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

import org.bhavaya.beans.Column;
import org.bhavaya.beans.TabularData;
import org.bhavaya.collection.EfficientArrayList;
import org.bhavaya.ui.diagnostics.Profiler;
import org.bhavaya.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.42.22.1 $
 */
public class DBUtilities {
    private static final Log log = Log.getCategory(DBUtilities.class);
    private static final CalendarThreadLocal gmtCalendar = new CalendarThreadLocal(TimeZone.getTimeZone("GMT"));
    private static final Map drivers = new HashMap();

    public static final int MAX_ELEMENTS_FOR_IN_STATEMENT = 50;
    public static final String SQL_STATE_TIMEOUT = "JZ0T3";
    public static final String SQL_STATE_IOERROR = "JZ006";
    public static final String SQL_STATE_DEADLOCK = "40001";
    public static final String SQL_STATE_UNKNOWN = "ZZZZZ";


    public static void closeResultSetAndStatement(ResultSet resultSet, Statement statement) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (Throwable e) {
            log.error("Error closing resultSet", e);
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (Throwable e) {
            log.error("Error closing statement", e);
        }
    }


    public static void commit(Connection connection) throws SQLException {
        if (connection != null) {
            if (log.isDebug()) log.debug("Committing on connection: " + connection);
            connection.commit();
        }
    }

    public static void rollback(Connection connection) {
        try {
            if (connection != null) {
                if (log.isDebug()) log.debug("Rolling back on connection: " + connection);
                connection.rollback();
            }
        } catch (Throwable e) {
            log.error("Error rolling back", e);
        }
    }

    public static void close(Connection connection) {
        try {
            if (connection != null) {
                if (log.isDebug()) log.debug("Closing connection: " + connection);
                connection.close();
            }
        } catch (Throwable e) {
            log.error("Error closing", e);
        }
    }


    public static void setValueOnPreparedStatement(PreparedStatement preparedStatement, int index, int jdbcType, Object propertyValue) throws SQLException {
        if (propertyValue == null) {
            preparedStatement.setNull(index, jdbcType);
            return;
        }

        Class propertyType = propertyValue.getClass();

        if (propertyType == String.class) {
            preparedStatement.setString(index, (String) propertyValue);
        } else if (propertyType == Integer.class) {
            preparedStatement.setObject(index, propertyValue);
        } else if (propertyType == Long.class) {
            preparedStatement.setObject(index, propertyValue);
        } else if (propertyType == Float.class) {
            if (((Float) propertyValue).isNaN()) {
                preparedStatement.setNull(index, jdbcType);
            } else {
                preparedStatement.setObject(index, propertyValue);
            }
        } else if (propertyType == Double.class) {
            if (((Double) propertyValue).isNaN()) {
                preparedStatement.setNull(index, jdbcType);
            } else {
                preparedStatement.setObject(index, propertyValue);
            }
        } else if (propertyType == BigDecimal.class) {
            preparedStatement.setObject(index, propertyValue);
        } else if (propertyType == BigInteger.class) {
            preparedStatement.setObject(index, propertyValue);
        } else if (java.sql.Date.class.isAssignableFrom(propertyType)) {
            preparedStatement.setDate(index, (java.sql.Date) propertyValue, (Calendar) gmtCalendar.get());
        } else if (java.util.Date.class.isAssignableFrom(propertyType)) {
            java.util.Date date = (java.util.Date) propertyValue;
            preparedStatement.setTimestamp(index, new Timestamp(date.getTime()), (Calendar) gmtCalendar.get());
        } else if (propertyType == Boolean.class) {
            preparedStatement.setObject(index, propertyValue);
        } else if (propertyType == Short.class) {
            preparedStatement.setObject(index, propertyValue);
        } else if (propertyType == Byte.class) {
            preparedStatement.setObject(index, propertyValue);
        } else if (propertyType == Character.class) {
            preparedStatement.setString(index, "" + propertyValue);
        } else {
            log.error("Could not persist property of type: " + propertyType.getName());
            throw new RuntimeException("Could not persist property of type: " + propertyType.getName());
        }
    }

    public static Class getClassByJdbcType(int type, int decimalDigits) {
        switch (type) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                return String.class;

            case Types.BIT:
            case Types.BOOLEAN:
                return Boolean.class;

            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
                return Integer.class;

            case Types.BIGINT:
                return Long.class;

            case Types.FLOAT:
            case Types.DOUBLE:
                return Double.class;

            case Types.DECIMAL:
            case Types.NUMERIC:
                if (decimalDigits == 0) {
                    return Long.class;
                } else {
                    return Double.class;
                }

            case Types.DATE:
                return java.sql.Date.class;

            case Types.TIME:
            case Types.TIMESTAMP:
                return java.util.Date.class;

            default:
                return String.class;
        }
    }

    public static Object getObjectFromString(String columnValue, Class expectedType, String dataSourceName) {
        Object propertyValue;
        if (expectedType == String.class || expectedType == Character.class || expectedType == char.class) {
            // remove quotes
            propertyValue = columnValue.substring(1, columnValue.length() - 1);
        } else if (expectedType == Long.class || expectedType == long.class) {
            propertyValue = new Long(columnValue);
        } else if (expectedType == Integer.class || expectedType == int.class) {
            propertyValue = new Integer(columnValue);
        } else if (expectedType == Short.class || expectedType == short.class) {
            propertyValue = new Short(columnValue);
        } else if (expectedType == Byte.class || expectedType == byte.class) {
            propertyValue = new Byte(columnValue);
        } else if (expectedType == Double.class || expectedType == double.class) {
            propertyValue = new Double(columnValue);
        } else if (expectedType == Float.class || expectedType == float.class) {
            propertyValue = new Float(columnValue);
        } else if (expectedType == BigDecimal.class) {
            propertyValue = new BigDecimal(columnValue);
        } else if (expectedType == BigInteger.class) {
            propertyValue = new BigInteger(columnValue);
        } else if (expectedType == Boolean.class || expectedType == boolean.class) {
            columnValue = columnValue.trim();
            boolean b = Utilities.booleanValue(columnValue);
            propertyValue = Boolean.valueOf(b);
        } else if (expectedType == java.sql.Date.class) {
            // String value is expected to be in GMT
            java.util.Date date = SQLFormatter.getInstance(dataSourceName).getGmtDateFromString(columnValue);
            propertyValue = DateUtilities.newDate(date);
        } else if (expectedType == java.util.Date.class) {
            propertyValue = SQLFormatter.getInstance(dataSourceName).getGmtDateFromString(columnValue);
        } else {
            throw new RuntimeException("Cannot type: " + expectedType.getName());
        }
        return propertyValue;
    }

    public static Object getObjectFromResultSet(int columnIndex, ResultSet resultSet) throws SQLException {
        return getObjectFromResultSet(columnIndex, resultSet, null);
    }

    public static Object getObjectFromResultSet(int columnIndex, ResultSet resultSet, Class expectedType) throws SQLException {
        if (expectedType == Boolean.class || expectedType == boolean.class) {
            Class actualType = getExpectedType(resultSet, columnIndex);
            if (actualType != Boolean.class) {
                String stringValue = (String) getObjectFromResultSetInternal(columnIndex, resultSet, String.class);
                return Boolean.valueOf(Utilities.booleanValue(stringValue));
            }
        }

        if (expectedType == null) {
            expectedType = getExpectedType(resultSet, columnIndex);
        }

        return getObjectFromResultSetInternal(columnIndex, resultSet, expectedType);
    }

    private static Object getObjectFromResultSetInternal(int columnIndex, ResultSet resultSet, Class expectedType) throws SQLException {
        Object propertyValue;
        if (expectedType == String.class || expectedType == Character.class || expectedType == char.class) {
            propertyValue = resultSet.getString(columnIndex);
            propertyValue = propertyValue != null ? ((String) propertyValue).trim() : null;
        } else if (expectedType == Long.class || expectedType == long.class) {
            long aLong = resultSet.getLong(columnIndex);
            propertyValue = resultSet.wasNull() ? null : new Long(aLong);
        } else if (expectedType == Integer.class || expectedType == int.class) {
            int anInt = resultSet.getInt(columnIndex);
            propertyValue = resultSet.wasNull() ? null : new Integer(anInt);
        } else if (expectedType == Short.class || expectedType == short.class) {
            short aShort = resultSet.getShort(columnIndex);
            propertyValue = resultSet.wasNull() ? null : new Short(aShort);
        } else if (expectedType == Byte.class || expectedType == byte.class) {
            byte aByte = resultSet.getByte(columnIndex);
            propertyValue = resultSet.wasNull() ? null : new Byte(aByte);
        } else if (expectedType == Double.class || expectedType == double.class) {
            double aDouble = resultSet.getDouble(columnIndex);
            propertyValue = resultSet.wasNull() ? null : new Double(aDouble);
        } else if (expectedType == Float.class || expectedType == float.class) {
            float aFloat = resultSet.getFloat(columnIndex);
            propertyValue = resultSet.wasNull() ? null : new Float(aFloat);
        } else if (expectedType == BigDecimal.class) {
            propertyValue = resultSet.getBigDecimal(columnIndex);
        } else if (expectedType == Boolean.class || expectedType == boolean.class) {
            boolean aBoolean = resultSet.getBoolean(columnIndex);
            propertyValue = resultSet.wasNull() ? null : Boolean.valueOf(aBoolean);
        } else if (expectedType == java.util.Date.class || expectedType == java.sql.Date.class) {
            // assume all dates/times are stored in GMT

            // cannot do "propertyValue = resultSet.getTimestamp(i,  (Calendar) gmtCalendar.get());"
            // as there is a bug in jConnect which does not return the time for the correct timezone,
            // which in our case is always GMT.
            // Therefore get the local time and convert to GMT by adding the GMT offset
            // this includes any default saving offset if we are in a daylight saving period.
            propertyValue = resultSet.getTimestamp(columnIndex);
            if (propertyValue != null) {
                long timeInMillis = DateUtilities.addGmtOffset(((java.util.Date) propertyValue).getTime());
                if (expectedType == java.sql.Date.class) {
                    propertyValue = DateUtilities.newDate(timeInMillis);
                } else {
                    propertyValue = new java.util.Date(timeInMillis);
                }
            }
        } else {
            throw new RuntimeException("Cannot type: " + expectedType.getName());
        }
        return propertyValue;
    }

    private static Class getExpectedType(ResultSet resultSet, int columnIndex) throws SQLException {
        Class expectedType;
        ResultSetMetaData metaData = resultSet.getMetaData();
        int sqlType = metaData.getColumnType(columnIndex);
        int decimalDigits = metaData.getScale(columnIndex);
        expectedType = getClassByJdbcType(sqlType, decimalDigits);
        return expectedType;
    }

    public static Object getObjectFromResultSet(String columnName, ResultSet resultSet) throws SQLException {
        return getObjectFromResultSet(columnName, resultSet, null);
    }

    public static Object getObjectFromResultSet(String columnName, ResultSet resultSet, Class expectedType) throws SQLException {
        int columnIndex = resultSet.findColumn(columnName);
        return getObjectFromResultSet(columnIndex, resultSet, expectedType);
    }

    public static int executeUpdate(String dataSourceName, String statementString) throws SQLException {
        Connection connection = null;
        try {
            connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
            return executeUpdate(connection, statementString);
        } finally {
            DBUtilities.close(connection);
        }
    }

    public static int executeUpdate(Connection connection, String statementString) throws SQLException {
        Statement statement = null;
        Profiler.Task task = Profiler.taskStarted(Profiler.SQL, statementString);
        try {
            long startTime = System.currentTimeMillis();
            statement = connection.createStatement();
            log.info("Executing sql: " + statementString);

            int noOfRows = statement.executeUpdate(statementString);

            log.info("Affected " + noOfRows + " rows in " + (System.currentTimeMillis() - startTime) + " millis");
            SqlMonitorFactory.getSqlMonitorInstance().sqlExecuted();
            return noOfRows;
        } finally {
            Profiler.taskStopped(task);
            DBUtilities.closeResultSetAndStatement(null, statement);
        }
    }

    public static List execute(String dataSourceName, String selectStatement) {
        log.info("Executing sql: " + selectStatement);
        TabularData tabularData = null;
        List result = null;
        try {
            tabularData = new ResultSetTabularData(dataSourceName, selectStatement, ResultSetTabularData.ROW_TYPE_SELECT);
            result = processResultSet(tabularData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tabularData != null) tabularData.close();
        }
        SqlMonitorFactory.getSqlMonitorInstance().sqlExecuted();
        return result;
    }

    public static List execute(String dataSourceName, String[] selectStatements) {
        List result = new ArrayList();

        try {
            for (int i = 0; i < selectStatements.length; i++) {
                String selectStatement = selectStatements[i];
                addDataToList(dataSourceName, selectStatement, result);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private static void addDataToList(String dataSourceName, String selectStatement, List result) throws SQLException {
        log.info("Executing sql: " + selectStatement);

        TabularData tabularData = null;
        Connection connection = null;
        Statement statement = null;

        Profiler.Task executionTask = Profiler.taskStarted(Profiler.SQL, selectStatement);
        try {
            connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
            statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            selectStatement = DataSourceFactory.getInstance(dataSourceName).getDialect().transformSelectSql(selectStatement);

            boolean hasResultSet = statement.execute(selectStatement);

            if (hasResultSet) {
                tabularData = new ResultSetTabularData(dataSourceName, connection, selectStatement, ResultSetTabularData.ROW_TYPE_SELECT, statement);
                result.addAll(processResultSet(tabularData));
            }
            SqlMonitorFactory.getSqlMonitorInstance().sqlExecuted();
        } finally {
            Profiler.taskStopped(executionTask);
            if (tabularData != null) {
                tabularData.close();
            } else if (statement != null) {
                DBUtilities.closeResultSetAndStatement(null, statement);
                DBUtilities.close(connection);
            }
        }
    }

    public static Map execute(String dataSourceName, String selectStatement, final String[] keyColumns) throws SQLException {
        return execute(dataSourceName, selectStatement, new Utilities.KeyColumnsToKeyTransform(keyColumns));
    }

    public static Map execute(Connection connection, String selectStatement, final String[] keyColumns) throws SQLException {
        return execute(connection, selectStatement, new Utilities.KeyColumnsToKeyTransform(keyColumns));
    }

    public static Map execute(String dataSourceName, String selectStatement, Transform keyTransform) throws SQLException {
        return execute(dataSourceName, selectStatement, keyTransform, null);
    }

    public static Map execute(Connection connection, String selectStatement, Transform keyTransform) throws SQLException {
        return execute(connection, selectStatement, keyTransform, null);
    }

    public static Map execute(String dataSourceName, String selectStatement, Transform keyTransform, ResultSetRowTransform resultSetRowTransform) throws SQLException {
        Connection connection = null;
        try {
            connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
            Map result = DBUtilities.execute(connection, selectStatement, keyTransform, resultSetRowTransform);
            return result;
        } finally {
            DBUtilities.close(connection);
        }
    }

    public static Map execute(Connection connection, String selectStatement, Transform keyTransform, ResultSetRowTransform resultSetRowTransform) throws SQLException {
        Map values = null;
        PreparedStatement selectValuesStatement = null;
        ResultSet selectValuesResultSet = null;

        Profiler.Task executionTask = Profiler.taskStarted(Profiler.SQL, selectStatement);
        try {
            log.info("Executing: " + selectStatement);

            selectValuesStatement = connection.prepareStatement(selectStatement);
            selectValuesResultSet = selectValuesStatement.executeQuery();

            values = DBUtilities.processResultSet(selectValuesResultSet, keyTransform, resultSetRowTransform);
            SqlMonitorFactory.getSqlMonitorInstance().sqlExecuted();
        } finally {
            Profiler.taskStopped(executionTask);
            DBUtilities.closeResultSetAndStatement(selectValuesResultSet, selectValuesStatement);
            DBUtilities.commit(connection);
        }

        return values;
    }

    public static Map execute(String dataSourceName, String selectStatement, ResultSetRowTransform keyTransform, ResultSetRowTransform resultSetRowTransform) throws SQLException {
        Connection connection = null;
        try {
            connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
            Map result = DBUtilities.execute(connection, selectStatement, keyTransform, resultSetRowTransform);
            return result;
        } finally {
            DBUtilities.close(connection);
        }
    }

    public static Map execute(Connection connection, String selectStatement, ResultSetRowTransform keyTransform, ResultSetRowTransform resultSetRowTransform) throws SQLException {
        Map values = null;
        PreparedStatement selectValuesStatement = null;
        ResultSet selectValuesResultSet = null;

        Profiler.Task executionTask = Profiler.taskStarted(Profiler.SQL, selectStatement);
        try {
            log.info("Executing: " + selectStatement);

            selectValuesStatement = connection.prepareStatement(selectStatement);
            selectValuesResultSet = selectValuesStatement.executeQuery();

            values = DBUtilities.processResultSet(selectValuesResultSet, keyTransform, resultSetRowTransform);
            SqlMonitorFactory.getSqlMonitorInstance().sqlExecuted();
        } finally {
            Profiler.taskStopped(executionTask);
            DBUtilities.closeResultSetAndStatement(selectValuesResultSet, selectValuesStatement);
            DBUtilities.commit(connection);
        }

        return values;
    }

    public static List processResultSet(TabularData tabularData) {
        long startTime = System.currentTimeMillis();
        List result = new ArrayList();

        int columnCount = tabularData.getColumnCount();
        Type type = Generic.getType(tabularData.getColumns());
        TabularData.Row tabularDataRow = tabularData.next();

        while (tabularDataRow != null) {
            Object value = type.newInstance();

            for (int i = 0; i < columnCount; i++) {
                Object propertyValue = tabularDataRow.getColumnValue(i);
                Column column = tabularData.getColumn(i);
                Generic.set(value, column.getName(), propertyValue);
            }

            result.add(value);
            tabularDataRow = tabularData.next();
        }

        log.info("Inflated: " + result.size() + " rows, " + columnCount + " columns in: " + (System.currentTimeMillis() - startTime) + " millis");
        return result;
    }

    public static Map processResultSet(ResultSet resultSet, final String[] keyColumns) throws SQLException {
        return processResultSet(resultSet, new Utilities.KeyColumnsToKeyTransform(keyColumns));
    }

    public static Map processResultSet(final ResultSet resultSet, Transform keyTransform) throws SQLException {
        return processResultSet(resultSet, keyTransform, null);
    }

    public static Map processResultSet(ResultSet resultSet, Transform keyTransform, ResultSetRowTransform resultSetRowTransform) throws SQLException {
        if (resultSetRowTransform == null) resultSetRowTransform = new ResultSetToRecordTransform(resultSet.getMetaData());
        long startTime = System.currentTimeMillis();
        Map result = new LinkedHashMap();
        while (resultSet.next()) {
            Object value = resultSetRowTransform.getRowValue(resultSet);
            Object key = keyTransform.execute(value);
            result.put(key, value);
        }
        log.info("Inflated: " + result.size() + " rows: " + (System.currentTimeMillis() - startTime) + " millis");
        return result;
    }

    public static Map processResultSet(ResultSet resultSet, ResultSetRowTransform keyTransform, ResultSetRowTransform resultSetRowTransform) throws SQLException {
        if (resultSetRowTransform == null) resultSetRowTransform = new ResultSetToRecordTransform(resultSet.getMetaData());
        long startTime = System.currentTimeMillis();
        Map result = new LinkedHashMap();
        while (resultSet.next()) {
            Object value = resultSetRowTransform.getRowValue(resultSet);
            Object key = keyTransform.getRowValue(resultSet);
            result.put(key, value);
        }
        log.info("Inflated: " + result.size() + " rows: " + (System.currentTimeMillis() - startTime) + " millis");
        return result;
    }

    public static CatalogSchema[] getCatalogSchemas(Connection connection) throws SQLException {
        Set catalogSchemas = new TreeSet();
        DatabaseMetaData metaData = connection.getMetaData();
        Set catalogNames = new HashSet();

        if (metaData.supportsCatalogsInDataManipulation()) {
            ResultSet catalogsResultSet = null;
            try {
                catalogsResultSet = metaData.getCatalogs();
                while (catalogsResultSet.next()) {
                    String catalogName = catalogsResultSet.getString(1);
                    catalogNames.add(catalogName);
                }
            } finally {
                DBUtilities.closeResultSetAndStatement(catalogsResultSet, null);
            }
        } else {
            catalogNames.add(null);
        }

        Set schemaNames = new HashSet();
        if (metaData.supportsSchemasInDataManipulation()) {
            ResultSet schemasResultSet = null;
            try {
                schemasResultSet = metaData.getSchemas();
                while (schemasResultSet.next()) {
                    String schemaName = schemasResultSet.getString(1);
                    schemaNames.add(schemaName);
                }
            } finally {
                DBUtilities.closeResultSetAndStatement(schemasResultSet, null);
            }
        } else {
            schemaNames.add(null);
        }

        for (Iterator iterator = catalogNames.iterator(); iterator.hasNext();) {
            String catalogName = (String) iterator.next();
            for (Iterator iterator2 = schemaNames.iterator(); iterator2.hasNext();) {
                String schemaName = (String) iterator2.next();
                catalogSchemas.add(CatalogSchema.getInstance(catalogName, schemaName));
            }
        }

        return (CatalogSchema[]) catalogSchemas.toArray(new CatalogSchema[catalogSchemas.size()]);
    }

    public static void executeUpdateScript(String dataSourceName, String filename, boolean strict) throws SQLException, IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(IOUtilities.getResourceAsStream(filename)));
            String line = reader.readLine();
            StringBuffer transaction = new StringBuffer();
            while (line != null) {
                if (!line.startsWith("--")) {
                    line = line.trim();
                    transaction.append(line);
                    if (line.endsWith(";")) {
                        try {
                            executeUpdate(dataSourceName, transaction.toString());
                        } catch (SQLException e) {
                            if (strict) {
                                log.error(e);
                                throw e;
                            } else {
                                log.warn("Failed to execute: " + transaction.toString());
                            }
                        }
                        transaction = new StringBuffer();
                    }
                }
                line = reader.readLine();
            }
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    public static Connection newConnection(String url, String username, String password, String driverName, Properties connectionProperties, boolean autoCommit, int transactionIsolation, DatabaseDialect dialect) throws SQLException {
        Properties connectionPropertiesCopy;
        if (connectionProperties != null) {
            connectionPropertiesCopy = (Properties) connectionProperties.clone();
        } else {
            connectionPropertiesCopy = new Properties();
        }
        connectionPropertiesCopy.put("user", username);
        connectionPropertiesCopy.put("password", password);

        log.info("Opening connection to: " + url + " for user: " + username);
        Connection connection = getDriver(driverName).connect(url, connectionPropertiesCopy);
        log.debug("DataSource version: " + connection.getMetaData().getDatabaseProductVersion());
        log.debug("Driver version: " + connection.getMetaData().getDriverVersion());

        if (transactionIsolation != -1) {
            connection.setTransactionIsolation(transactionIsolation);
        }
        // setAutoCommit after setting transaction isolation level as setTransactionIsolation may have side effects on autoCommit status
        connection.setAutoCommit(autoCommit);

        if (dialect != null) {
            log.info("Applying " + dialect.getClass().getName() + ".configure");
            dialect.configure(connection);
        }

        log.info("Set connection transaction isolation to: " + getTransactionIsolationName(connection.getTransactionIsolation()));
        log.info("Set connection autoCommit to: " + connection.getAutoCommit());

        return connection;
    }

    private static Driver getDriver(String driverName) throws SQLException {
        // We get the Driver directly to by-pass the DriverManager which does not allow
        // our driver to be loaded on a different classloader to the caller.
        // With Builder, the driver may be loaded by ClassUtilities.getApplicationClassLoader
        // while the caller was loaded by the System class loader.
        Driver driver = (Driver) drivers.get(driverName);
        if (driver == null) {
            try {
                log.info("Registering driver: " + driverName);
                driver = (java.sql.Driver) ClassUtilities.getClass(driverName).newInstance();
                drivers.put(driverName, driver);
            } catch (Exception e) {
                throw new SQLException("Could not register driver: " + driverName + " due to " + e.getMessage());
            }
        }
        return driver;
    }

    public static boolean isRecoverableException(SQLException e) {
        String sqlState = e.getSQLState();
        if (sqlState != null &&
                (
                sqlState.equals(SQL_STATE_DEADLOCK) ||
                sqlState.equals(SQL_STATE_TIMEOUT) ||
                sqlState.equals(SQL_STATE_IOERROR) ||
                sqlState.equals(SQL_STATE_UNKNOWN)
                )
        ) {
            return true;
        }

        SQLException nextException = e.getNextException();
        if (nextException != null && nextException != e) {
            return isRecoverableException(nextException);
        } else {
            return false;
        }
    }

    public static String getTransactionIsolationName(int transactionIsolationLevel) {
        switch (transactionIsolationLevel) {
            case -1:
                return "DEFAULT";
            case Connection.TRANSACTION_NONE:
                return "NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED:
                return "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ:
                return "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE:
                return "SERIALIZABLE";
            default:
                throw new RuntimeException("Invalid transactionIsolationLevel: " + transactionIsolationLevel);
        }
    }

    public static int getTransactionIsolation(String transactionIsolationName) {
        if (transactionIsolationName.equalsIgnoreCase("DEFAULT")) {
            return -1;
        } else if (transactionIsolationName.equalsIgnoreCase("NONE")) {
            return Connection.TRANSACTION_NONE;
        } else if (transactionIsolationName.equalsIgnoreCase("READ_UNCOMMITTED")) {
            return Connection.TRANSACTION_READ_UNCOMMITTED;
        } else if (transactionIsolationName.equalsIgnoreCase("READ_COMMITTED")) {
            return Connection.TRANSACTION_READ_COMMITTED;
        } else if (transactionIsolationName.equalsIgnoreCase("REPEATABLE_READ")) {
            return Connection.TRANSACTION_REPEATABLE_READ;
        } else if (transactionIsolationName.equalsIgnoreCase("SERIALIZABLE")) {
            return Connection.TRANSACTION_SERIALIZABLE;
        } else {
            throw new RuntimeException("Invalid transactionIsolationName: " + transactionIsolationName);
        }
    }

    public static String createTemporaryTable(String dataSourceName, Column[] columns, Object[] keys, boolean createWithPrimaryKey) throws Exception {
        Connection connection = null;

        try {
            BhavayaDataSource dataSource = DataSourceFactory.getInstance(dataSourceName);
            connection = dataSource.getConnection();

            String tempTable = dataSource.getDialect().createTempTable(connection, columns, createWithPrimaryKey);
            populateTableWithKeys(dataSourceName, connection, tempTable, columns, keys);
            return tempTable;
        } finally {
            DBUtilities.close(connection);
        }
    }

    public static boolean containsPrivateTempTable(SQL sqlToExecute, String dataSourceName) {
        return DataSourceFactory.getInstance(dataSourceName).getDialect().containsPrivateTempTable(sqlToExecute);
    }

    private static int populateTableWithKeys(String dataSourceName, Connection connection, String tableName, Column[] columns, Object[] keys) throws SQLException {
        String statementStringTemplate = createInsertStatement(tableName, columns);
        SQLFormatter formatter = SQLFormatter.getInstance(dataSourceName);
        Statement statement = null;

        try {
            statement = connection.createStatement();
            StringBuffer logMessage = new StringBuffer("Creating temp table: " + tableName + " with " + keys.length + " entries: {");
            for (int j = 0; j < keys.length; j++) {
                String statementString = statementStringTemplate;
                final Object key = keys[j];
                logMessage.append(key).append("; ");

                if (columns.length > 1) {
                    List compoundKey = (List) key;
                    for (int i = 0; i < compoundKey.size(); i++) {
                        Object o = compoundKey.get(i);
                        statementString = formatter.replace(statementString, o);
                    }
                } else {
                    statementString = formatter.replace(statementString, key);
                }

                statement.addBatch(statementString);
            }
            log.info(logMessage.toString() + "}");
            int totalAffected = executeBatch(statement);
            log.info("Executing batch for " + tableName + " resulted in " + totalAffected + " rows affected");
            SqlMonitorFactory.getSqlMonitorInstance().sqlExecuted();
            return totalAffected;
        } finally {
            DBUtilities.closeResultSetAndStatement(null, statement);
        }
    }

    private static String createInsertStatement(String tableName, Column[] columns) {
        StringBuffer buffer = new StringBuffer("INSERT INTO ").append(tableName).append("(");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) buffer.append(",");
            buffer.append(columns[i].getName());
        }
        buffer.append(") VALUES (");

        for (int i = 0; i < columns.length; i++) {
            if (i > 0) buffer.append(",");
            buffer.append("?");
        }

        buffer.append(")");
        return buffer.toString(); // didnt use PreparedStatement, as was getting wrong parameter type errors
    }

    public static int executeBatch(Statement statement) throws SQLException {
        Profiler.Task task = Profiler.taskStarted(Profiler.SQL, "[Batch DML]");
        try {
            int[] rowsAffectedArray = statement.executeBatch();
            return sum(rowsAffectedArray);
        } finally {
            Profiler.taskStopped(task);
        }
    }

    private static int sum(int[] intArray) {
        int sum = 0;
        for (int i = 0; i < intArray.length; i++) {
            sum = sum + intArray[i];
        }
        return sum;
    }

    public static class ResultSetToRecordTransform implements ResultSetRowTransform {
        private Type type;
        private int columnCount;
        private Column[] columns;

        public ResultSetToRecordTransform(ResultSetMetaData metaData) throws SQLException {
            this(createColumns(metaData));
        }

        public ResultSetToRecordTransform(Column[] columns) {
            this.columns = columns;
            columnCount = columns.length;
            type = Generic.getType(columns);
        }

        private static Column[] createColumns(ResultSetMetaData metaData) throws SQLException {
            int columnCount = metaData.getColumnCount();

            Column[] columns = new Column[columnCount];
            for (int i = 0; i < columnCount; i++) {
                Class columnType = DBUtilities.getClassByJdbcType(metaData.getColumnType(i + 1), metaData.getScale(i + 1));
                Column column = new Column(metaData.getColumnLabel(i + 1), columnType);
                columns[i] = column;
            }
            return columns;
        }

        public Object getRowValue(ResultSet resultSet) {
            try {
                Object value = type.newInstance();

                for (int i = 0; i < columnCount; i++) {
                    Column column = columns[i];
                    Object propertyValue = DBUtilities.getObjectFromResultSet(column.getName(), resultSet, columns[i].getType());
                    Generic.set(value, columns[i].getName(), propertyValue);
                }

                return value;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static interface ResultSetRowTransform {
        public Object getRowValue(ResultSet resultSet);
    }

    public static class ResultSetToSimpleOrListValueTransform implements ResultSetRowTransform {
        private String[] columns;

        public ResultSetToSimpleOrListValueTransform(TableColumn[] columns) {
            this.columns = Column.columnsToNames(columns);
        }

        public ResultSetToSimpleOrListValueTransform(String[] columns) {
            this.columns = columns;
        }

        public Object getRowValue(ResultSet resultSet) {
            try {
                Object value;

                if (columns.length == 1) {
                    value = DBUtilities.getObjectFromResultSet(columns[0], resultSet);
                } else {
                    value = new EfficientArrayList(columns.length);
                    for (int j = 0; j < columns.length; j++) {
                        ((List) value).add(DBUtilities.getObjectFromResultSet(columns[j], resultSet));
                    }
                }
                return value;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}