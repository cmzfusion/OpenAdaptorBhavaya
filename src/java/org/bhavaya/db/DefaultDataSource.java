/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

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
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.14.24.1.2.1 $
 */
public final class DefaultDataSource extends AbstractBhavayaDataSource {

    private static final Log log = Log.getCategory(DefaultDataSource.class);

    private String driverName;
    private String url;
    private int transactionIsolation;
    private boolean autoCommit;
    private String username;
    private String password;

    private Properties connectionProperties = new Properties();

    private Connection connection;
    private ConnectionInvocationHandler connectionInvocationHandler;


    public static void setAppName(String appName) {
        DefaultDataSource.appName = appName;
    }
    
    DefaultDataSource() {
    }

    public void configure(PropertyGroup propertyGroup) {
        super.configure(propertyGroup);

        driverName = propertyGroup.getProperty("driverName");
        url = propertyGroup.getProperty("url");
        username = propertyGroup.getProperty("user");
        password = propertyGroup.getProperty("password");

        autoCommit = propertyGroup.getProperty("autoCommit").equalsIgnoreCase("true");
        transactionIsolation = DBUtilities.getTransactionIsolation(propertyGroup.getProperty("transactionIsolation"));

        String[] properties = propertyGroup.getPropertyNames();
        for (int i = 0; i < properties.length; i++) {
            connectionProperties.setProperty(properties[i], propertyGroup.getProperty(properties[i]));
        }

        try {
            String description = InetAddress.getLocalHost().toString() + ":" + getDataSourceName();
            connectionProperties.put("hostname", description);
            if (appName != null) connectionProperties.put("applicationname", appName.substring(0, Math.min(30, appName.length())));
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

    public synchronized Connection getConnection() throws SQLException {
        return getConnection(username, password);
    }

    public synchronized Connection getConnection(String username, String password) throws SQLException {
        if (connection == null) {
            connection = newProxyConnection(username, password);
        }
        return connection;
    }

    public synchronized String getConnectionId() {
        return connection == null ? "Not connected" : connectionInvocationHandler.getConnectionId();
    }


    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
    }

    public void setLoginTimeout(int seconds) throws SQLException {
    }

    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return (T) connection;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return connection.getClass().isAssignableFrom(iface);
    }

    private synchronized Connection newProxyConnection(String username, String password) throws SQLException {
        Connection underlyingConnection = DBUtilities.newConnection(url, username, password, driverName, connectionProperties, autoCommit, transactionIsolation, getDialect());
        connectionInvocationHandler = new ConnectionInvocationHandler(this, underlyingConnection);
        log.info("Connected to '" + getDataSourceName() + "' with connection ID: " + connectionInvocationHandler.getConnectionId());
        return (Connection) Proxy.newProxyInstance(underlyingConnection.getClass().getClassLoader(), new Class[]{java.sql.Connection.class}, connectionInvocationHandler);
    }

    public synchronized void close() {
        log.info("Closing connection for dataSource: " + getDataSourceName());
        if (connection != null) {
            DBUtilities.close(connectionInvocationHandler.getUnderlyingConnection());
            connection = null;
            log.info("Connection for dataSource: " + getDataSourceName() + " closed.");
        } else {
            log.info("No connection for dataSource: " + getDataSourceName() + " to close.");
        }
    }

    private class ConnectionInvocationHandler implements InvocationHandler {
        private Connection underlyingConnection;
        private DefaultDataSource dataSource;
        private String connectionId;

        public ConnectionInvocationHandler(DefaultDataSource dataSource, Connection underlyingConnection) {
            this.underlyingConnection = underlyingConnection;
            this.connectionId = dataSource.getDialect().getConnectionId(underlyingConnection);
            this.dataSource = dataSource;
        }

        public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.equals(Object.class.getMethod("equals", Object.class))) {
                return new Boolean(proxy == args[0]);
            } else if (method.getDeclaringClass().equals(Object.class)) {
                return method.invoke(this, args);
            }

            if (method.getName().equals("close")) {
                return null;
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

            log.warn("Resetting connection");
            try {
                DBUtilities.close(underlyingConnection); // this doesnt fail otherwise we would need to wrap it in a try/catch block, dont call underlyingConnection.isClosed() as it could itself fail
                underlyingConnection = DBUtilities.newConnection(dataSource.url, dataSource.username, dataSource.password, dataSource.driverName, dataSource.connectionProperties, dataSource.autoCommit, dataSource.transactionIsolation, dataSource.getDialect());
                connectionId = dataSource.getDialect().getConnectionId(underlyingConnection);
                log.info("Connected to '" + getDataSourceName() + "' with connection ID: " + getConnectionId());
                if (log.isDebug()) log.debug("Reset connection successful");
                return true;
            } catch (Exception e) {
                log.warn("Reset connection failed");
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

    public static void main(String[] args) {
        try {
            DataSourceFactory.getInstance("database").getConnection();
            DataSourceFactory.getInstance("sqlbroadcasterDatabase").getConnection();
        } catch (SQLException e) {
            log.error(e);
        }
    }

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
}