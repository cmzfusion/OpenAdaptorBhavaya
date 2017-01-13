package org.bhavaya.db;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import org.bhavaya.util.PropertyGroup;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Apr 7, 2006
 * Time: 11:22:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionPoolDataSourceTest extends MockObjectTestCase {
    private ConnectionPoolDataSource datasource;
    private Mock mockConnection;
    public static final int MIN_CONNECTIONS = 2;

    protected void setUp() throws Exception {
        mockConnection = mock(Connection.class);
        datasource = new ConnectionPoolDataSource() {
            protected Connection createUnderlyingConnection(String username, String password) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                return (Connection) mockConnection.proxy();
            }
        };
        datasource.configure(getConfiguration());
    }

    protected void tearDown() throws Exception {
        mockConnection.reset();
    }

    private PropertyGroup getConfiguration() {
        PropertyGroup config = new PropertyGroup(null, "Config");

        config.addProperty("name", "test");
        config.addProperty("defaultCatalog", "defaultCatalog");
        config.addProperty("defaultSchema", "defaultSchema");
        config.addProperty("sqlNotificationSubject", "sqlNotificationSubject");

        config.addProperty("driverName", "driver");
        config.addProperty("url", "url");
        config.addProperty("user", "user");
        config.addProperty("password", "password");
        config.addProperty("autoCommit", "true");
        config.addProperty("transactionIsolation", "NONE");
        config.addProperty("maxConnections", "4");
        config.addProperty("minConnections", "2");
        config.addProperty("maxIdleTime", "5000");
        return config;
    }

    public void testCreatesMinConnections() throws SQLException {
        Connection connection1 = datasource.getConnection();

        assertEquals("Number of connections should be equal to the minimum number of connections", 2, datasource.getConnectionCount());
        assertEquals("Incorrect idle connection count", 1, datasource.getIdleCount());

        Connection connection2 = datasource.getConnection();
        assertEquals("Number of connections should be equal to the minimum number of connections", 2, datasource.getConnectionCount());
        assertEquals("Incorrect idle connection count", 0, datasource.getIdleCount());

        connection1.close();
        assertEquals("Connection should have been returned to idle pool", 1, datasource.getIdleCount());
        assertEquals("Connections should not have shrunk", 2, datasource.getConnectionCount());

        connection2.close();
        assertEquals("Connection should have been returned to idle pool", 2, datasource.getIdleCount());
        assertEquals("Connections should not have shrunk", 2, datasource.getConnectionCount());
    }

    public void testConnectionsGrow() throws SQLException {
        final CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        datasource.getConnection();
                        latch.countDown();
                    } catch (SQLException e) {
                        fail("Exception");
                    }
                }
            }).start();
        }

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail();
        }

        assertEquals("Connections should have grown to 3", 3, datasource.getConnectionCount());
        assertEquals("Incorrect idle connection count", 0, datasource.getIdleCount());

        datasource.getConnection();
        assertEquals("Connections should be 4", 4, datasource.getConnectionCount());
        assertEquals("There should be no more idle connections", 0, datasource.getIdleCount());
    }

    public void testBlockWhenMaxReached() throws SQLException, InterruptedException {
        datasource.getConnection();
        datasource.getConnection();
        datasource.getConnection();
        final Connection connection = datasource.getConnection();

        final Latch connectionAcquiredLatch = new Latch();
        final Latch waitForBlockThread = new Latch();

        Thread shouldBlockThread = new Thread(new Runnable() {
            public void run() {
                try {
                    waitForBlockThread.release();
                    datasource.getConnection();
                    connectionAcquiredLatch.release();
                } catch (SQLException e) {
                    throwException(e);
                }
            }
        });
        shouldBlockThread.start();

        waitForBlockThread.acquire();
        boolean connectionAcquired = connectionAcquiredLatch.attempt(600);
        assertTrue("Connection should have blocked", !connectionAcquired);

        Thread closeConnectionThread = new Thread(new Runnable() {
            public void run() {
                try {
                    connection.close();
                } catch (Exception e) {
                    throwException(e);
                }
            }
        });
        closeConnectionThread.start();

        connectionAcquired = connectionAcquiredLatch.attempt(1000);
        assertTrue("Thread never unblocked on connection close", connectionAcquired);
    }

    public void testIdleThreadsDie() throws SQLException, InterruptedException {
        Connection connection1 = datasource.getConnection();
        Connection connection2 = datasource.getConnection();
        Connection connection3 = datasource.getConnection();
        Connection connection4 = datasource.getConnection();

        mockConnection.expects(never()).method("close");
        connection1.close();
        connection2.close();
        connection3.close();
        mockConnection.verify();
        mockConnection.reset();

        assertEquals("Connection count should be 4", 4, datasource.getConnectionCount());
        assertEquals("There should be 3 idle threads", 3, datasource.getIdleCount());

        mockConnection.expects(exactly(2)).method("close");
        Thread.sleep(5500);
        connection4.close();
        mockConnection.verify();

        assertEquals("Connection count should be 2", 2, datasource.getConnectionCount());
        assertEquals("There should be 2 idle threads", 2, datasource.getIdleCount());
    }
}

