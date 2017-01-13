package org.bhavaya.db;

import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.11.2.1 $
 */
public class ConnectionPoolDataSource extends AbstractBhavayaDataSource {

    private static final Log log = Log.getCategory(ConnectionPoolDataSource.class);

    // All thread-safe
    private String driverName;
    private String url;
    private int transactionIsolation;
    private boolean autoCommit;
    private String username;
    private String password;

    // All thread-safe
    private int maxConnections;
    private int minConnections;
    private long maxIdleTime;

    private final ReentrantReadWriteLock connectionsLock = new ReentrantReadWriteLock();
    private final Lock readLock = connectionsLock.readLock();
    private final Lock writeLock = connectionsLock.writeLock();

    // Not-thread safe
    private LinkedHashSet<Connection> connections = new LinkedHashSet<Connection>();
    private LinkedHashSet<Connection> idleConnections = new LinkedHashSet<Connection>();
    private Map<Connection, Long> connectionToLastUsedMap = new HashMap<Connection, Long>();

    // Not-thread safe
    private Map<Connection, ConnectionInvocationHandler> connectionToInvocationHandlerMap = new HashMap<Connection, ConnectionInvocationHandler>();

    private Semaphore idleSemaphore = new Semaphore(0);

    // Thread-safe.  Only writes during configure which is before it can be used.
    private Properties connectionProperties = new Properties();


    public ConnectionPoolDataSource() {
    }

    public void configure(PropertyGroup propertyGroup) {
        super.configure(propertyGroup);

        driverName = propertyGroup.getProperty("driverName");
        url = propertyGroup.getProperty("url");
        username = propertyGroup.getProperty("user");
        password = propertyGroup.getProperty("password");

        autoCommit = propertyGroup.getProperty("autoCommit").equalsIgnoreCase("true");
        transactionIsolation = DBUtilities.getTransactionIsolation(propertyGroup.getProperty("transactionIsolation"));

        maxConnections = propertyGroup.getNumericProperty("maxConnections").intValue();
        minConnections = propertyGroup.getNumericProperty("minConnections").intValue();
        maxIdleTime = propertyGroup.getNumericProperty("maxIdleTime").longValue();

        String[] properties = propertyGroup.getPropertyNames();
        for (String property : properties) {
            connectionProperties.setProperty(property, propertyGroup.getProperty(property));
        }

        try {
            String description = InetAddress.getLocalHost().toString() + ":" + getDataSourceName();
            connectionProperties.put("hostname", description);
            if (appName != null)
                connectionProperties.put("applicationname", appName.substring(0, Math.min(30, appName.length())));
        } catch (UnknownHostException e) {
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getUrl() {
        return url;
    }

    public int getTransactionIsolation() {
        return transactionIsolation;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public int getConnectionCount() {
        readLock.lock();
        try {
            return connections.size();
        } finally {
            readLock.unlock();
        }
    }

    public int getIdleCount() {
        readLock.lock();
        try {
            return idleConnections.size();
        } finally {
            readLock.unlock();
        }
    }

    public Connection getConnection() throws SQLException {
        return getConnection(username, password);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        ensureMinimumConnections();
        return getNextFromPool();
    }


    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    private Connection getNextFromPool() throws SQLException {
        // If you don't understand the locking going on down below, then don't touch it.  If you think it looks strange
        // then you simply have yet to understand it.  It is evil I know.  Closure support should tidy this up -> JSE7?
        readLock.lock();
        try {
            while (true) {
                if (idleConnections.size() == 0) {
                    if (connections.size() == maxConnections) {
                        readLock.unlock();  // Temporarily release lock while blocking on another
                        blockUntilConnectionFree();
                        readLock.lock();
                    } else {
                        // Upgrade to a write lock and re-test this branch path logic.
                        readLock.unlock();
                        writeLock.lock();
                        try {
                            if (idleConnections.size() == 0 && connections.size() != maxConnections) {
                                addConnectionToPool();
                            }
                        } finally {
                            writeLock.unlock();
                            readLock.lock();
                        }
                    }
                }

                // Upgrade to write lock
                readLock.unlock();
                writeLock.lock();
                try {
                    // Try again as thread may have been added.
                    if (idleConnections.size() != 0) {
                        return removeIdleConnection();
                    }
                } finally {
                    writeLock.unlock();
                    readLock.lock();
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    private void blockUntilConnectionFree() {
        idleSemaphore.acquireUninterruptibly();
    }

    // All callers must have the writeLock
    private Connection addConnectionToPool() throws SQLException {
        Connection connection = newProxyConnection(username, password);

        connections.add(connection);
        idleConnections.add(connection);
        connectionToLastUsedMap.put(connection, System.currentTimeMillis());

        return connection;
    }

    // All callers must have the writeLock
    private Connection removeIdleConnection() throws SQLException, Error {
        Connection connection = idleConnections.iterator().next();

        idleConnections.remove(connection);
        connectionToLastUsedMap.remove(connection);

        assert connection != null;
        return connection;
    }

    // All callers must NOT have a read lock
    private void ensureMinimumConnections() throws SQLException {
        writeLock.lock();
        try {
            int connectionsToCreate = Math.max(minConnections - connections.size(), 0);
            for (int i = 0; i < connectionsToCreate; i++) {
                addConnectionToPool();
            }
        } finally {
            writeLock.unlock();
        }
    }

    // All callers must have the writeLock
    private void makeConnectionIdle(Connection connection) {
        idleConnections.add(connection);
        connectionToLastUsedMap.put(connection, System.currentTimeMillis());
        idleSemaphore.release();
    }

    // All callers must have the writeLock
    private void cullOldIdleConnections() {
        Connection idleConnectionsSnapshot[] = idleConnections.toArray(new Connection[idleConnections.size()]);
        for (Connection idleConnection : idleConnectionsSnapshot) {
            if (connections.size() == minConnections) return;

            long idleTimeStart = connectionToLastUsedMap.get(idleConnection);
            if (System.currentTimeMillis() - idleTimeStart > maxIdleTime) {
                log.debug("Culling idle connection: " + idleConnection);
                removeConnection(idleConnection);
            }
        }
    }

    // All callers must have the writeLock
    private void removeConnection(Connection connection) {
        idleConnections.remove(connection);
        connections.remove(connection);
        connectionToLastUsedMap.remove(connection);
        ConnectionInvocationHandler connectionInvocationHandler = connectionToInvocationHandlerMap.remove(connection);
        DBUtilities.close(connectionInvocationHandler.getUnderlyingConnection());
    }

    public String getConnectionId() {
        readLock.lock();
        try {
            StringBuilder buffer = new StringBuilder();
            for (Connection connection : connections) {
                ConnectionInvocationHandler connectionInvocationHandler = connectionToInvocationHandlerMap.get(connection);
                buffer.append(connectionInvocationHandler.getConnectionId() + ", ");
            }
            buffer.setLength(buffer.length() - 2);
            return "[" + buffer + "]";
        } finally {
            readLock.unlock();
        }
    }


    public PrintWriter getLogWriter() {
        return null;
    }

    public void setLogWriter(PrintWriter out) {
    }

    public void setLoginTimeout(int seconds) {
    }

    public int getLoginTimeout() {
        return 0;
    }

    // All callers must have the writeLock
    private Connection newProxyConnection(String username, String password) throws SQLException {
        Connection underlyingConnection = createUnderlyingConnection(username, password);
        ConnectionInvocationHandler connectionInvocationHandler = new ConnectionInvocationHandler(this, underlyingConnection);
        log.info("Connected to '" + getDataSourceName() + "' with connection ID: " + connectionInvocationHandler.getConnectionId());
        Connection connection = (Connection) Proxy.newProxyInstance(underlyingConnection.getClass().getClassLoader(), new Class[]{Connection.class}, connectionInvocationHandler);
        connectionToInvocationHandlerMap.put(connection, connectionInvocationHandler);
        return connection;
    }

    protected Connection createUnderlyingConnection(String username, String password) throws SQLException {
        return DBUtilities.newConnection(url, username, password, driverName, connectionProperties, autoCommit, transactionIsolation, getDialect());
    }

    public void close() {
        writeLock.lock();
        try {
            ConnectionPoolDataSource.log.info("Closing connections for dataSource: " + getDataSourceName());
            for (Connection connection : connections) {
                removeConnection(connection);
            }
            log.info("Connections for dataSource: " + getDataSourceName() + " closed.");
        } finally {
            writeLock.unlock();
        }
    }

    private class ConnectionInvocationHandler implements InvocationHandler {
        private Connection underlyingConnection;
        private ConnectionPoolDataSource dataSource;
        private String connectionId;

        public ConnectionInvocationHandler(ConnectionPoolDataSource dataSource, Connection underlyingConnection) {
            this.underlyingConnection = underlyingConnection;
            this.connectionId = dataSource.getDialect().getConnectionId(underlyingConnection);
            this.dataSource = dataSource;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.equals(Object.class.getMethod("equals", Object.class))) {
                return proxy == args[0];
            } else if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(this, args);
            }

            if (method.getName().equals("close")) {
                writeLock.lock();
                try {
                    makeConnectionIdle((Connection) proxy);
                    cullOldIdleConnections();
                    return null;
                } finally {
                    writeLock.unlock();
                }
            }

            try {
                return method.invoke(underlyingConnection, args);
            } catch (Exception e) {
                if (resetConnection()) {
                    return method.invoke(underlyingConnection, args);
                } else {
                    throw e;
                }
            }
        }

        private boolean isClosed(Connection underlyingCollection) {
            try {
                return underlyingCollection.isClosed();
            } catch (Exception e) {
                return true;
            }
        }

        private boolean resetConnection() {
            if (!isClosed(underlyingConnection)) return false;

            ConnectionPoolDataSource.log.warn("Resetting connection");
            try {
                DBUtilities.close(underlyingConnection); // this doesnt fail otherwise we would need to wrap it in a try/catch block, dont call underlyingConnection.isClosed() as it could itself fail
                underlyingConnection = dataSource.createUnderlyingConnection(dataSource.username, dataSource.password);
                connectionId = dataSource.getDialect().getConnectionId(underlyingConnection);
                ConnectionPoolDataSource.log.info("Connected to '" + getDataSourceName() + "' with connection ID: " + getConnectionId());
                if (ConnectionPoolDataSource.log.isDebug())
                    ConnectionPoolDataSource.log.debug("Reset connection successful");
                return true;
            } catch (Exception e) {
                ConnectionPoolDataSource.log.warn("Reset connection failed");
                return false;
            }
        }

        public Connection getUnderlyingConnection() {
            return underlyingConnection;
        }

        public String getConnectionId() {
            return connectionId;
        }
    }

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
}
