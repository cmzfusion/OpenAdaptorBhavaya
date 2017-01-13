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

package org.bhavaya.db.broadcaster;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;
import com.sun.org.apache.xml.internal.utils.FastStringBuffer;
import org.bhavaya.coms.NotificationException;
import org.bhavaya.coms.NotificationPublisher;
import org.bhavaya.coms.NotificationServer;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.DataSourceFactory;
import org.bhavaya.util.*;

import javax.management.ObjectName;
import java.io.IOException;
import java.net.*;
import java.sql.*;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.22 $
 */
public class SybaseAuditTableSqlBroadcaster implements SybaseAuditTableSqlBroadcasterMBean {
    private static final Log log = Log.getCategory(SybaseAuditTableSqlBroadcaster.class);
    private static final Log sqlLog = Log.getCategory("sql");

    public static int SOCKET_CONNECTION_WAIT = 5000;

    private static final String auditTablePrefix = "sybsecurity..sysaudits_0";
    private static final String currentAuditTableSql = "sp_configure 'current audit table'";
    private static final String setAuditTableSql = "sp_configure 'current audit table', ?";
    private static final String truncateTableSql = "truncate table ?";
    private static final String recordsSql = "SELECT * FROM ?";
    private static final String auditTableRowCountSql = "select count(event) from ?";
    private static final String numberOfAuditTablesSql = "SELECT COUNT(*) AS NUMBEROFTABLES from sybsecurity..sysobjects WHERE name LIKE 'sysaudits_%'";

    private static final String datasource = "sqlbroadcasterDatabase";
    private static final String SQLBROADCASTER_PROPERTIES_KEY = "sqlbroadcaster";

    private static final Pattern updatePattern = Pattern.compile("^[\\s]*update[\\s]+[\\w]+\\.", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern insertIntoPattern = Pattern.compile("^[\\s]*insert[\\s]+into[\\s]+[\\w]+\\.", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    //private static final Pattern insertPattern = Pattern.compile("^[\\s]*insert ", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern deletePattern = Pattern.compile("^[\\s]*delete[\\s]+from[\\s]+[\\w]+\\.", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    //private static final Pattern deletePattern = Pattern.compile("^[\\s]*delete[\\s]+[\\w]+\\.", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern truncatePattern = Pattern.compile("^[\\s]*truncate[\\s]+table[\\s]+[\\w]+\\.", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern replacePattern = Pattern.compile("\\?");
    private static final Pattern removeIdentityPattern = Pattern.compile("[\\w]*set[\\s]identity_insert[\\s]dbo+\\.[\\w]+[\\s](on|off)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private String databaseName;
    private int numberOfAuditTables;

    private Connection connection;
    private PreparedStatementAndSql getAuditTableStatement;
    private PreparedStatementAndSql[] setAuditTableStatements;
    private PreparedStatementAndSql[] recordsStatements;
    private PreparedStatementAndSql[] auditTableRowCountStatements;
    private PreparedStatementAndSql[] trunacteTableStatements;
    private Map<SpidToDBNameKey, FastStringBuffer> sqlBuffersBySpid;
    private NotificationPublisher broadcaster;

    private long retriesAttempted = 0;
    private long reconnectionPeriod = 0;
    private long auditTableRowCountCheckPeriod = 0;
    private long lastAuditTableRowCountCheckPeriod = 0;
    private long auditTableCheckPeriod = 0;
    private int maximumAuditTableRowCount = 0;
    private NotificationServer notificationServer;
    private int currentAuditTableNumber;
    private int nextAuditTableNumber;
    private boolean closed;
    private final DateFormat dateFormat;
    private long lastScanTimeInMillis;
    private long startTime;
    private String databaseServer;
    private int multicastPort;
    private String sqlNotificationSubject;
    private boolean removeIdentity;

    public SybaseAuditTableSqlBroadcaster(boolean daemon) throws Exception {
        startTime = System.currentTimeMillis();

        PropertyGroup sqlBroadcasterProperties = ApplicationProperties.getApplicationProperties().getGroup(SQLBROADCASTER_PROPERTIES_KEY);
        this.databaseName = sqlBroadcasterProperties.getMandatoryProperty("databaseName");
        this.auditTableRowCountCheckPeriod = Integer.parseInt(sqlBroadcasterProperties.getMandatoryProperty("auditTableRowCountCheckPeriod"));
        this.auditTableCheckPeriod = Integer.parseInt(sqlBroadcasterProperties.getMandatoryProperty("auditTableCheckPeriod"));
        this.maximumAuditTableRowCount = Integer.parseInt(sqlBroadcasterProperties.getMandatoryProperty("maximumAuditTableRowCount"));
        this.databaseServer = sqlBroadcasterProperties.getMandatoryProperty("databaseServer");
        this.multicastPort = Integer.parseInt(sqlBroadcasterProperties.getMandatoryProperty("multicastPort"));
        this.sqlNotificationSubject = sqlBroadcasterProperties.getProperty("sqlNotificationSubject");
        this.removeIdentity = Boolean.valueOf(sqlBroadcasterProperties.getProperty("removeIdentity"));
        if (sqlNotificationSubject == null) sqlNotificationSubject = NotificationServer.DEFAULT_NOTIFICATION_SUBJECT;

        PropertyGroup notificationProperties = ApplicationProperties.getApplicationProperties().getGroup(NotificationServer.NOTIFICATION_PROPERTY_GROUP);
        PropertyGroup sqlSubject = getSqlSubjectProperties(notificationProperties, sqlNotificationSubject);
        String reconnectionPeriodString = sqlSubject.getMandatoryProperty(NotificationServer.RECONNECTION_PERIOD_PROPERTY);
        reconnectionPeriod = Long.parseLong(reconnectionPeriodString);

        dateFormat = DateUtilities.newGmtDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        sqlBuffersBySpid = new HashMap<SpidToDBNameKey, FastStringBuffer>();

        notificationServer = NotificationServer.newInstance(sqlNotificationSubject, daemon);
        broadcaster = NotificationPublisher.getInstance(sqlNotificationSubject);

        String objectName = Utilities.MBEANSERVER_DOMAIN + ":type=" + ClassUtilities.getUnqualifiedClassName(getClass());
        log.info("Binding " + ClassUtilities.getUnqualifiedClassName(getClass()) + " to diagnostics");
        notificationServer.getMBeanServer().registerMBean(this, new ObjectName(objectName));

        setClosed(true);
        checkForOtherInstances();
    }

    private void checkForOtherInstances() throws Exception {
        InetAddress localHost = InetAddress.getLocalHost();
        final String requestMessage = "SqlBroadcaster|" + databaseName + "|" + databaseServer + "|" + localHost + "|" + startTime;
        final InetAddress group = InetAddress.getByName("224.0.0.1");
        final MulticastSocket socket = new MulticastSocket(multicastPort);
        socket.joinGroup(group);
        final DatagramPacket requestPacket = new DatagramPacket(requestMessage.getBytes(), requestMessage.length(), group, multicastPort);
        socket.send(requestPacket);

        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    while (true) {
                        byte[] bytes = new byte[1000];
                        DatagramPacket responsePacket = new DatagramPacket(bytes, bytes.length, group, multicastPort);
                        socket.receive(responsePacket);
                        String responseMessage = new String(responsePacket.getData()).trim();
                        if (responseMessage.startsWith("SqlBroadcaster|")) {
                            log.info("Received broadcast: " + responseMessage);
                            String[] tokens = responseMessage.split("\\|");
                            String responseDatabaseName = tokens[1];
                            String responseDatabaseServer = tokens[2];
                            String responseHost = tokens[3];
                            long responseStartTime = Long.parseLong(tokens[4]);

                            if (databaseName.equals(responseDatabaseName) && databaseServer.equals(responseDatabaseServer)) {
                                if (startTime > responseStartTime) {
                                    log.error("An instance of SqlBroadcaster is already running for " + databaseServer + " on " + responseHost);
                                    System.exit(1);
                                } else if (startTime < responseStartTime) {
                                    socket.send(requestPacket);
                                } else {
                                    // this is either a packet from this instance or an instance that started at the same time
                                    // in both cases ignore
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error(e);
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

    public String getDatabaseServer() {
        return databaseServer;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public long getAuditTableRowCountCheckPeriod() {
        return auditTableRowCountCheckPeriod;
    }

    public long getReconnectionPeriod() {
        return reconnectionPeriod;
    }

    public int getMaximumAuditTableRowCount() {
        return maximumAuditTableRowCount;
    }

    private void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isClosed() {
        return closed;
    }

    public double getSecondsSinceLastScan() {
        return (System.currentTimeMillis() - lastScanTimeInMillis) / 1000.0;
    }

    public void startWithFailover() {
        while (true) {
            try {
                start();
                if (isClosed()) return;
            } catch (Exception e) {
                log.error("SqlBroadcaster failed", e);
                if (isClosed()) return;
                try {
                    close();
                } catch (Throwable e1) {
                    log.error(e1);
                }
                retriesAttempted++;
                pauseBeforeRestart();
            }
        }
    }

    private void pauseBeforeRestart() {
        if (retriesAttempted == 1) {
            log.error("Restarting SqlBroadcaster (Attempt " + retriesAttempted + ")...");
        } else {
            log.error("Restarting SqlBroadcaster (Attempt " + retriesAttempted + ") in " + (reconnectionPeriod / 1000) + " seconds...");

            try {
                Thread.sleep(reconnectionPeriod);
            } catch (InterruptedException e) {
                log.error(e);
            }
        }
    }

    public void restart() {
        close();
        startWithFailover();
    }

    private synchronized void connect() throws Exception {
        if (!isClosed()) {
            log.error("Attempting to connect when already connected");
            return;
        }
        setClosed(false);

        log.info("Connecting to database for datasource: " + datasource);
        connection = DataSourceFactory.getInstance(datasource).getConnection();
        connection.setCatalog("sybsecurity");

        createStatements();

        currentAuditTableNumber = getCurrentAuditTableNumberFromDatabase();
        log.info("Current audit table is: " + currentAuditTableNumber);
        nextAuditTableNumber = getNextAuditTableNumber(currentAuditTableNumber);
        log.info("Next audit table is: " + currentAuditTableNumber);
        truncateAllAuditTables();

        log.info("Starting notification server");
        notificationServer.start();

        log.info("Connecting to notification server");
        broadcaster = NotificationPublisher.getInstance(sqlNotificationSubject);
        broadcaster.connect();

        clearSqlBuffers();
        lastAuditTableRowCountCheckPeriod = System.currentTimeMillis();

        log.info("Started broadcasting");
    }

    private void createStatements() throws SQLException {
        numberOfAuditTables = calculateNumberOfAuditTables();
        log.info("Number of audit tables: " + numberOfAuditTables);
        if (numberOfAuditTables == 0) throw new RuntimeException("No audit tables");

        getAuditTableStatement = new PreparedStatementAndSql(connection.prepareCall(currentAuditTableSql), currentAuditTableSql);
        recordsStatements = new PreparedStatementAndSql[numberOfAuditTables];
        setAuditTableStatements = new PreparedStatementAndSql[numberOfAuditTables];
        auditTableRowCountStatements = new PreparedStatementAndSql[numberOfAuditTables];
        trunacteTableStatements = new PreparedStatementAndSql[numberOfAuditTables];

        for (int i = 0; i < numberOfAuditTables; i++) {
            int auditTableNumber = i + 1;
            String auditTableName = getAuditTableName(auditTableNumber);

            String sql = recordsSql;
            sql = replace(sql, auditTableName);
            recordsStatements[i] = new PreparedStatementAndSql(connection.prepareStatement(sql), sql);

            sql = setAuditTableSql;
            sql = replace(sql, auditTableNumber);
            setAuditTableStatements[i] = new PreparedStatementAndSql(connection.prepareCall(sql), sql);

            sql = auditTableRowCountSql;
            sql = replace(sql, auditTableName);
            auditTableRowCountStatements[i] = new PreparedStatementAndSql(connection.prepareStatement(sql), sql);

            sql = truncateTableSql;
            sql = replace(sql, auditTableName);
            trunacteTableStatements[i] = new PreparedStatementAndSql(connection.prepareStatement(sql), sql);
        }
    }

    public int getNumberOfAuditTables() {
        return numberOfAuditTables;
    }

    private int calculateNumberOfAuditTables() throws SQLException {
        log.info("Executing sql: " + numberOfAuditTablesSql);

        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(numberOfAuditTablesSql);
            resultSet.next();
            return resultSet.getInt("NUMBEROFTABLES");
        } finally {
            DBUtilities.closeResultSetAndStatement(resultSet, statement);
        }
    }

    public void exit() {
        System.exit(0);
    }


    public synchronized void close() {
        if (isClosed()) {
            log.error("Attempting to connect when already closed");
            return;
        }
        setClosed(true);

        if (broadcaster != null) {
            broadcaster.close();
            broadcaster = null;
        }

        if (notificationServer != null) {
            try {
                notificationServer.close();
            } catch (Exception e) {
                log.error(e);
            }
        }

        if (connection != null) {
            DataSourceFactory.closeAll();
            connection = null;
        }
    }

    public void start() throws Exception {
        log.info("Attempting to start broadcast");
        log.info("Database name: " + databaseName);
        log.info("Maximum audit table row count: " + maximumAuditTableRowCount);
        log.info("Audit table check period: " + auditTableCheckPeriod / 1000.0 + " seconds");
        log.info("Audit table row count check period: " + auditTableRowCountCheckPeriod / 1000.0 + " seconds");
        connect();

        while (true) {
            if (isClosed()) break;
            broadcastOutstandingRecords();
            if (isClosed()) break;
            Thread.sleep(auditTableCheckPeriod);
        }
    }

    private synchronized void broadcastOutstandingRecords() throws Exception {
        try {
            // process audit data in current table
            int totalRecords = processOutstandingRecords(currentAuditTableNumber);
            truncateTable(currentAuditTableNumber, totalRecords);

            if (totalRecords == 0) {
                // broadcast previous buffers as there are no more records to concatanate to them
                broadcastSqlBuffers();
            }

            // switch to next audit table
            currentAuditTableNumber = nextAuditTableNumber;
            nextAuditTableNumber = getNextAuditTableNumber(currentAuditTableNumber);
            setAuditTable(nextAuditTableNumber);
        } catch (Exception e) {
            if (isClosed()) {
                return;
            } else {
                throw e;
            }
        }
    }

    private FastStringBuffer getSqlBuffer(Integer spid, String dbName) {
        SpidToDBNameKey key = new SpidToDBNameKey(spid, dbName);
        FastStringBuffer sqlBuffer = sqlBuffersBySpid.get(key);
        if (sqlBuffer == null) {
            sqlBuffer = new FastStringBuffer();
            sqlBuffersBySpid.put(key, sqlBuffer);
        }
        return sqlBuffer;
    }

    private synchronized void broadcastSqlBuffers() throws NotificationException {
        for (Object obj : sqlBuffersBySpid.entrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            SpidToDBNameKey key = (SpidToDBNameKey) entry.getKey();
            FastStringBuffer sqlBuffer = (FastStringBuffer) entry.getValue();
            broadcast(sqlBuffer, key.getDBName());
        }
    }

    private synchronized void clearSqlBuffers() {
        sqlBuffersBySpid.clear();
    }

    private synchronized int processOutstandingRecords(int currentAuditTableNumber) throws Exception {
        ResultSet recordsResultSet = null;
        try {
            if (log.isDebug()) log.debug("Processing table: " + currentAuditTableNumber);
            if (checkInterrupt()) {
                return 0;
            }
            PreparedStatementAndSql recordsStatement = recordsStatements[currentAuditTableNumber - 1];
            recordsResultSet = recordsStatement.executeQuery("processOutstandingRecords");
            lastScanTimeInMillis = System.currentTimeMillis();
            if (checkInterrupt()) {
                return 0;
            }

            int totalRecords = 0;

            while (recordsResultSet.next()) {
                if (checkInterrupt()) {
                    return 0;
                }
                int sequence = recordsResultSet.getInt("sequence");
                String extrainfo = recordsResultSet.getString("extrainfo");
                Integer spid = (Integer) recordsResultSet.getObject("spid");
                String dbname = recordsResultSet.getString("dbname");

                if (log.isDebug()) {
                    Integer event = (Integer) recordsResultSet.getObject("event");
                    Integer eventMod = (Integer) recordsResultSet.getObject("eventmod");
                    java.util.Date eventtime = recordsResultSet.getTimestamp("eventtime");
                    Integer suid = (Integer) recordsResultSet.getObject("suid");
                    Integer dbid = (Integer) recordsResultSet.getObject("dbid");
                    Integer objid = (Integer) recordsResultSet.getObject("dbid");
                    String loginname = recordsResultSet.getString("loginname");

                    if (log.isDebug()) log.debug("Add record: " + event + "/" + eventMod + "/" + spid + "/" + sequence + "/" + suid + "/" + dbid + "/" + objid + "/" + loginname + "/" + dbname + "/" + dateFormat.format(eventtime) + ":\n" + extrainfo);
                }

                FastStringBuffer sqlBuffer = getSqlBuffer(spid, dbname);

                if (sequence == 1) {
                    // broadcast previous contents of buffer
                    broadcast(sqlBuffer, dbname);
                }

                if (extrainfo != null) {
                    sqlBuffer.append(extrainfo);
                }
                totalRecords++;
            }

            return totalRecords;

        } catch (Exception e) {
            log.error(e);
            throw e;
        } finally {
            DBUtilities.closeResultSetAndStatement(recordsResultSet, null);
        }
    }

    private synchronized int getCurrentAuditTableNumberFromDatabase() throws SQLException {
        ResultSet getAuditTableResultSet = null;
        try {
            getAuditTableResultSet = getAuditTableStatement.executeQuery("getCurrentAuditTableNumberFromDatabase");
            getAuditTableResultSet.next();
            return getAuditTableResultSet.getInt(5);
        } finally {
            DBUtilities.closeResultSetAndStatement(getAuditTableResultSet, null);
        }
    }

    private int getNextAuditTableNumber(int auditTableNumber) {
        int nextAuditTableNumber = auditTableNumber + 1;
        if (nextAuditTableNumber > numberOfAuditTables) {
            nextAuditTableNumber = 1;
        }
        return nextAuditTableNumber;
    }

    private static String getAuditTableName(int auditTableNumber) {
        return auditTablePrefix + auditTableNumber;
    }

    private synchronized void setAuditTable(int auditTableNumber) throws SQLException {
        boolean changed = false;
        int noOfAttempts = 0;
        while (!changed) {
            noOfAttempts++;

            if (noOfAttempts == 50) {
                throw new RuntimeException("Failed to change audit table after: " + noOfAttempts);
            }

            PreparedStatementAndSql statement = setAuditTableStatements[auditTableNumber - 1];
            statement.execute("setAuditTable");
            changed = checkValidAuditTableNumber(auditTableNumber);
            if (!changed) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
        }
    }

    private boolean checkValidAuditTableNumber(int auditTableNumber) throws SQLException {
        int currentAuditTableNumber = getCurrentAuditTableNumberFromDatabase();
        if (currentAuditTableNumber != auditTableNumber) {
            log.error("Audit table is: " + currentAuditTableNumber + " but expected: " + auditTableNumber);
            return false;
        }
        return true;
    }


    private synchronized void truncateAllAuditTables() throws SQLException {
        // clear all unused audit tables
        for (int i = 1; i <= numberOfAuditTables; i++) {
            if (i != currentAuditTableNumber) {
                String truncateTableName = getAuditTableName(i);
                log.info("Truncating table: " + truncateTableName);
                truncateTable(i, -1);
            }
        }

        // make audit data go to next audit table
        int nextAuditTableNumber = getNextAuditTableNumber(currentAuditTableNumber);
        log.info("Setting audit table to: " + getAuditTableName(nextAuditTableNumber));
        setAuditTable(nextAuditTableNumber);

        // clear the previous audit table
        String truncateTableName = getAuditTableName(currentAuditTableNumber);
        log.info("Truncating table: " + truncateTableName);
        truncateTable(currentAuditTableNumber, -1);
    }

    private synchronized void truncateTable(int auditTableNumber, int expectedRowCount) throws SQLException {
        PreparedStatementAndSql statement = trunacteTableStatements[auditTableNumber - 1];
        checkValidRowCount(auditTableNumber, expectedRowCount);
        statement.execute("truncateTable");
    }

    private void checkValidRowCount(int auditTableNumber, final int expectedRowCount) throws SQLException {
        if (expectedRowCount >= 0) {
            PreparedStatementAndSql statement = auditTableRowCountStatements[auditTableNumber - 1];

            ResultSet resultSet = null;
            try {
                resultSet = statement.executeQuery("checkValidRowCount");
                resultSet.next();
                int rowCount = resultSet.getInt(1);
                if (rowCount != expectedRowCount) {
                    int auditTableNumberFromDatabase = getCurrentAuditTableNumberFromDatabase();
                    if (auditTableNumber != auditTableNumberFromDatabase) log.error("Database indicates audit table is: " + auditTableNumberFromDatabase + " but process expects it to be: " + auditTableNumber);
                    log.error(new Exception("Row count is: " + rowCount + " but expected: " + expectedRowCount));
                    logInvalidRows(auditTableNumber);
                }
            } finally {
                DBUtilities.closeResultSetAndStatement(resultSet, null);
            }
        }
    }

    private void logInvalidRows(int auditTableNumber) throws SQLException {
        PreparedStatementAndSql statement = recordsStatements[auditTableNumber - 1];

        ResultSet resultSet = null;
        try {
            resultSet = statement.executeQuery("logInvalidRows");
            while (resultSet.next()) {
                log.error("======================");
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    Object columnValue = resultSet.getObject(i);
                    String columnName = metaData.getColumnName(i);
                    log.error(columnName + ": " + columnValue);
                }
            }
        } finally {
            DBUtilities.closeResultSetAndStatement(resultSet, null);
        }
    }

    private synchronized void broadcast(FastStringBuffer sqlBuffer, String dbName) throws NotificationException {
        if (sqlBuffer.length() == 0) return;
        String sqlString = sqlBuffer.toString();
        if (sqlLog.isDebug()) sqlLog.debug("Preparing for boardcast, database = " + dbName + " sql "+ sqlString);
        sqlBuffer.setLength(0);
        sqlString = processString(sqlString, dbName);
        broadcaster.send(sqlString);
        broadcaster.commit();
        if (sqlLog.isDebug()) sqlLog.debug("Broadcasted: " + sqlString);
    }

    private String processString(String record, String dbName) {
        // remove identity statements if configured
        if (removeIdentity) {
            Matcher removeIdentity = removeIdentityPattern.matcher(record);
            record = removeIdentity.replaceAll("");
        }
        // prefix ownername.tablename with correct databasename
        Matcher updateMatcher = updatePattern.matcher(record);
        record = updateMatcher.replaceAll("update " + dbName + "..");

        Matcher insertIntoMatcher = insertIntoPattern.matcher(record);
        record = insertIntoMatcher.replaceAll("insert into " + dbName + "..");

        Matcher deleteMatcher = deletePattern.matcher(record);
        record = deleteMatcher.replaceAll("delete from " + dbName + "..");

        Matcher truncateMatcher = truncatePattern.matcher(record);
        record = truncateMatcher.replaceAll("truncate table " + dbName + "..");

        return record;
    }


    private boolean checkInterrupt() {
        boolean interrupt = false;

        if (System.currentTimeMillis() - lastAuditTableRowCountCheckPeriod > auditTableRowCountCheckPeriod) {
            try {
                for (int i = 1; i <= numberOfAuditTables; i++) {
                    ResultSet resultSet = null;

                    try {
                        String auditTableName = getAuditTableName(i);
                        PreparedStatementAndSql statement = auditTableRowCountStatements[i - 1];

                        resultSet = statement.executeQuery("checkInterrupt");
                        resultSet.next();
                        int rowCount = resultSet.getInt(1);
                        if (log.isDebug()) log.debug("Audit table: " + auditTableName + " has row count: " + rowCount);
                        if (rowCount > maximumAuditTableRowCount) {
                            log.warn("Interrupting execution for refresh, as row count in audit table: " + auditTableName + " is: " + rowCount);
                            broadcastRefresh();
                            interrupt = true;
                            break;
                        }
                    } finally {
                        DBUtilities.closeResultSetAndStatement(resultSet, null);
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }
            lastAuditTableRowCountCheckPeriod = System.currentTimeMillis(); // running the row count sql can take quite a bit of time;
        }

        return interrupt;
    }

    public synchronized void broadcastRefresh() throws NotificationException, SQLException {
        DBUtilities.rollback(connection);
        truncateAllAuditTables();
        clearSqlBuffers();
        broadcaster.send("REFRESH");
        broadcaster.commit();
    }

    public static String replace(CharSequence sql, String value) {
        return replacePattern.matcher(sql).replaceFirst(value);
    }

    public static String replace(CharSequence sql, int value) {
        return replacePattern.matcher(sql).replaceFirst(Integer.toString(value));
    }

    private static class PreparedStatementAndSql {
        private PreparedStatement statement;
        private String sql;

        public PreparedStatementAndSql(PreparedStatement statement, String sql) {
            log.info("Creating statement: " + sql);
            this.statement = statement;
            this.sql = sql;
        }

        public ResultSet executeQuery(String methodName) throws SQLException {
            if (log.isDebug()) log.debug("Executing sql for " + methodName + ": " + sql);
            return statement.executeQuery();
        }

        public void execute(String methodName) throws SQLException {
            if (log.isDebug()) log.debug("Executing sql for " + methodName + ": " + sql);
            statement.execute();
        }
    }

    public static boolean isProcessAlive(String[] environment) {
        PropertyGroup subjectPropertyGroup = getSqlSubjectProperties(environment);
        String portString = subjectPropertyGroup.getMandatoryProperty(NotificationServer.DIAGNOSTIC_PORT_PROPERTY);
        String host = subjectPropertyGroup.getMandatoryProperty("host");
        int port = Integer.parseInt(portString);

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), SOCKET_CONNECTION_WAIT);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void createProcess(String[] environment) throws IOException {
        log.info("createProcess for environment: " + Utilities.asString(environment, ", "));

        PropertyGroup sqlBroadcasterProperties = ApplicationProperties.getInstance(environment).getGroup(SQLBROADCASTER_PROPERTIES_KEY);
        PropertyGroup subjectPropertyGroup = getSqlSubjectProperties(environment);
        String host = subjectPropertyGroup.getMandatoryProperty("host");
        String username = sqlBroadcasterProperties.getMandatoryProperty("hostUsername");
        String password = sqlBroadcasterProperties.getMandatoryProperty("hostPassword");
        String home = sqlBroadcasterProperties.getMandatoryProperty("hostHome");
        String java = sqlBroadcasterProperties.getMandatoryProperty("hostJava");
        String fileSeparator = sqlBroadcasterProperties.getMandatoryProperty("hostFileSeparator");
        String pathSeparator = sqlBroadcasterProperties.getMandatoryProperty("hostPathSeparator");
        String classpathJars = sqlBroadcasterProperties.getMandatoryProperty("classpath");
        String hostLogConfigFile = sqlBroadcasterProperties.getMandatoryProperty("hostLogConfigFile");

        String[] jars = classpathJars.split(",");
        String mainClass = SybaseAuditTableSqlBroadcaster.class.getName();

        StringBuffer classpathBuffer = new StringBuffer("-cp ");
        classpathBuffer.append(home).append(pathSeparator);
        for (int i = 0; i < jars.length; i++) {
            if (i > 0) classpathBuffer.append(pathSeparator);
            String jar = jars[i];
            classpathBuffer.append(home + fileSeparator + jar);
        }
        String classpath = classpathBuffer.toString();

        String maxMemory = "-mx256m";
        String environmentPath = "-DENVIRONMENT_PATH=" + Utilities.asString(environment, ",");
        String logConfigFile = "-Dlog4j.configuration=" + hostLogConfigFile;
        String overrideResourceDir = "-DOVERRIDE_RESOURCE_DIR=" + home;
        String command = "nohup " + java + " " + environmentPath + " " + overrideResourceDir + " " + logConfigFile + " " + maxMemory + " " + classpath + " " + mainClass + " &";
        if (log.isDebug())log.debug("command: " + command);

        if (isProcessAlive(environment)) {
            throw new RuntimeException("Process already alive");
        }
        executeCommand(host, username, password, command);
    }

    private static PropertyGroup getSqlSubjectProperties(String[] environment) {
        PropertyGroup applicationProperties = ApplicationProperties.getInstance(environment[0], environment, 1);
        PropertyGroup notificationProperties = applicationProperties.getGroup(NotificationServer.NOTIFICATION_PROPERTY_GROUP);
        PropertyGroup sqlBroadcasterProperties = applicationProperties.getGroup(SQLBROADCASTER_PROPERTIES_KEY);
        String sqlNotificationSubject = sqlBroadcasterProperties.getProperty("sqlNotificationSubject");
        if (sqlNotificationSubject == null) sqlNotificationSubject = NotificationServer.DEFAULT_NOTIFICATION_SUBJECT;
        return getSqlSubjectProperties(notificationProperties, sqlNotificationSubject);

    }

    private static PropertyGroup getSqlSubjectProperties(PropertyGroup notificationProperties, String sqlNotificationSubject) {
        PropertyGroup[] subjectsProperties = notificationProperties.getGroups("subject");

        if (subjectsProperties != null) {
            for (int i = 0; i < subjectsProperties.length; i++) {
                PropertyGroup subjectProperties = subjectsProperties[i];
                if (subjectProperties.getProperty("name").equals(sqlNotificationSubject)) {
                    return subjectProperties;
                }
            }
        }
        throw new RuntimeException("Cannot find notification properties for " + sqlNotificationSubject + " subject");
    }

    private static void executeCommand(String host, String username, String password, String command) throws IOException {
        ConfigurationLoader.initialize(false);

        SshClient ssh = new SshClient();
        ssh.setSocketTimeout(30000);
        SshConnectionProperties properties = new SshConnectionProperties();
        properties.setHost(host);
        properties.setPrefPublicKey("ssh-dss");
        ssh.connect(properties, new HostKeyVerification() {
            public boolean verifyHost(String s, SshPublicKey sshPublicKey) {
                return true;
            }
        });

        PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
        pwd.setUsername(username);
        pwd.setPassword(password);
        int result = ssh.authenticate(pwd);

        if (result != AuthenticationProtocolState.COMPLETE) {
            throw new IOException("Authentication failed, status: " + result);
        }

        log.info("Executing: " + command);
        SessionChannelClient session = ssh.openSessionChannel();
        session.executeCommand(command);
        ssh.disconnect();
    }

    public static void main(String[] args) {
        try {
            SybaseAuditTableSqlBroadcaster sqlBroadcaster = new SybaseAuditTableSqlBroadcaster(false);
            sqlBroadcaster.startWithFailover();
        } catch (Exception e) {
            log.error(e);
        }
    }
    
    private class SpidToDBNameKey{
        private int spid;
        private String dBName;

        private SpidToDBNameKey(int spid, String dBName) {
            this.spid = spid;
            this.dBName = dBName;
        }

        public int getSpid() {
            return spid;
        }

        public String getDBName() {
            return dBName;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SpidToDBNameKey that = (SpidToDBNameKey) o;

            if (spid != that.spid) return false;
            if (dBName != null ? !dBName.equals(that.dBName) : that.dBName != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = spid;
            result = 31 * result + (dBName != null ? dBName.hashCode() : 0);
            return result;
        }
    }
}

