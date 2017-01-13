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

import org.bhavaya.beans.*;
import org.bhavaya.coms.NotificationException;
import org.bhavaya.coms.NotificationPublisher;
import org.bhavaya.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Persists objects to the database using either updating or inserting a record into a specific
 * database table.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.12 $
 */
public class Persister {
    private static final Log log = Log.getCategory(Persister.class);
    private static final Log sqlLog = Log.getCategory("sql");

    private static Map insertStatementCache = new HashMap();
    private static Map updateStatementCache = new HashMap();
    private static Map deleteStatementCache = new HashMap();

    private String dataSourceName;
    private Connection connection;
    private NotificationPublisher broadcaster;
    private boolean updateLocalCache;
    private boolean clearCachesAtEndOfTransaction;
    private SQLFormatter sqlFormatter;

    public Persister(String dataSourceName, NotificationPublisher broadcaster) {
        this(dataSourceName, broadcaster, broadcaster == null, true);
    }

    public Persister(String dataSourceName, NotificationPublisher broadcaster, boolean updateLocalCache, boolean clearCachesAtEndOfTransaction) {
        this.dataSourceName = dataSourceName;
        this.broadcaster = broadcaster;
        this.updateLocalCache = updateLocalCache;
        this.clearCachesAtEndOfTransaction = clearCachesAtEndOfTransaction;
        sqlFormatter = SQLFormatter.getInstance(dataSourceName);
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public NotificationPublisher getBroadcaster() {
        return broadcaster;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isUpdateLocalCache() {
        return updateLocalCache;
    }

    public boolean isClearCachesAtEndOfTransaction() {
        return clearCachesAtEndOfTransaction;
    }

    private synchronized void initConnection() throws SQLException, NotificationException {
        if (broadcaster != null) broadcaster.connect();
        if (connection == null) {
            connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
        }
    }

    /**
     * Inserts bean as a record into the relevant database tables.  Gets all column names in tableName
     * and converts them into property names of bean.  Reads all the property values from bean which
     * match the converted columnNames and uses these property values to set the appropriate column
     * value in an insert sql statement.
     *
     * @param bean the bean that represents the record in a database table to persist.
     */
    public void insertObject(Object bean) throws SQLException, NotificationException {
        initConnection();
        Class typeClass = bean.getClass();
        Schema schema = Schema.getInstance(typeClass);
        CatalogSchemaTable[] catalogSchemaTables = schema.getSql().getTables();

        for (int t = 0; t < catalogSchemaTables.length; t++) {
            CatalogSchemaTable catalogSchemaTable = catalogSchemaTables[t];
            TableColumn[] columnsWithMatchingProperties = Table.getInstance(catalogSchemaTable, schema.getDefaultDataSourceName()).getColumns(typeClass);

            String preparedInsertSqlString = getPreparedInsertSql(typeClass, catalogSchemaTable, false);
            String broadcastSqlString = getPreparedInsertSql(typeClass, catalogSchemaTable, true);
            log.info("Executing sql: " + preparedInsertSqlString);
            sqlLog.info(preparedInsertSqlString);

            PreparedStatement insertStatement = null;
            try {
                insertStatement = connection.prepareStatement(preparedInsertSqlString);

                Object[] columnValues = new Object[columnsWithMatchingProperties.length];
                int index = 1;
                for (int i = 0; i < columnsWithMatchingProperties.length; i++) {
                    TableColumn column = columnsWithMatchingProperties[i];
                    if (column.getIdFactory() == null || column.getIdFactory().isGeneratedPreInsert()) {
                        Object columnValue;
                        if (column.getIdFactory() != null) {
                            columnValue = getColumnValueFromIdFactory(bean, column, schema);
                        } else {
                            columnValue = getColumnValue(bean, column);
                        }
                        columnValues[i] = columnValue;
                        int jdbcType = column.getJdbcType();
                        DBUtilities.setValueOnPreparedStatement(insertStatement, index, jdbcType, columnValue);
                        index++;
                    }
                }

                broadcastSqlString = sqlFormatter.replace(broadcastSqlString, columnValues).toString();
                log.info("Broadcast sql: " + broadcastSqlString);
                int numberOfRowsAffected = insertStatement.executeUpdate();
                log.info(numberOfRowsAffected + " affected");

                broadcastSqlString = processPostInsertGeneratedValues(bean, catalogSchemaTable, broadcastSqlString);

            } catch (SQLException e) {
                log.error("Failed to execute sql: " + broadcastSqlString, e);
                throw(e);
            } finally {
                DBUtilities.closeResultSetAndStatement(null, insertStatement);
            }

            // broadcast the insert sql
            if (broadcaster != null) {
                broadcaster.send(broadcastSqlString);
            }
            if (updateLocalCache) {
                BeanFactory.getInstance(typeClass).putValue(bean);
            }
        }
    }

    private static Object getColumnValue(Object bean, Column column) {
        return TabularDataBeanFactory.getColumnValueForBean(bean, column);
    }

    private Object getColumnValueFromIdFactory(Object bean, TableColumn column, Schema schema) throws SQLException {
        Object columnValue = column.getIdFactory().getInsertValue(bean, column, this);
        if (columnValue != null) {
            DefaultProperty property = schema.getDefaultPropertyByColumn(column);
            if (property != null) {
                property.setPropertyValue(bean, columnValue);
            }
        }
        return columnValue;
    }

    private String processPostInsertGeneratedValues(Object bean, CatalogSchemaTable catalogSchemaTable, String broadcastSqlString) throws SQLException {
        Class typeClass = bean.getClass();
        Schema schema = Schema.getInstance(typeClass);

        // 1) need to amend the broadcastSqlString to include the generated value(s)
        // 2) and set value(s) on bean if bean has the appropriate property(ies).
        TableColumn[] columns = Table.getInstance(catalogSchemaTable, schema.getDefaultDataSourceName()).getColumns(typeClass);

        for (int i = 0; i < columns.length; i++) {
            TableColumn column = columns[i];
            if (column.getIdFactory() != null && column.getIdFactory().isGeneratedPostInsert()) {
                Object columnValue = getColumnValueFromIdFactory(bean, column, schema);
                broadcastSqlString = sqlFormatter.replace(broadcastSqlString, columnValue);
                log.info("processPostInsertGeneratedValues: " + broadcastSqlString);
            }
        }
        return broadcastSqlString;
    }

    /**
     * Updates a record, represented by bean, into the relevant database tables.
     * All properties available on the bean will be written to the database, irrespective of
     * whether they've changed or not since originally being read.
     *
     * An optimised version of this method, where only changed properties are written, may be available
     * one day ...
     *
     * @param bean            the bean that represents the record in a database table to persist.
     */
    public void updateObject(Object bean) throws SQLException, NotificationException {
        Property[] properties = Schema.getInstance(bean.getClass()).getProperties();
        String[] propertyNames = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            propertyNames[i] = properties[i].getName();
        }
        updateObject(bean, propertyNames);
    }

    /**
     * Updates a record, represented by bean, into the relevant database tables.
     *
     * @param bean            the bean that represents the record in a database table to persist.
     * @param propertiesToSet the list of properties that are to be updated in the record in the database table.
     */
    public void updateObject(Object bean, String[] propertiesToSet) throws SQLException, NotificationException {
        if (propertiesToSet == null || propertiesToSet.length == 0) return;

        initConnection();
        Class typeClass = bean.getClass();
        Schema schema = Schema.getInstance(typeClass);
        Column[] columnsToKey = schema.getUnionOfKeyColumns();
        // Don't update without a primary key
        if (columnsToKey.length == 0) new SQLException("Can't update without primary key");
        CatalogSchemaTable[] catalogSchemaTables = schema.getSql().getTables();

        for (int t = 0; t < catalogSchemaTables.length; t++) {
            CatalogSchemaTable catalogSchemaTable = catalogSchemaTables[t];
            String preparedUpdateSqlString = getPreparedUpdateSql(typeClass, catalogSchemaTable, propertiesToSet);

            // if none of the properties to set apply to this table, skip to the next table
            if (preparedUpdateSqlString == null) continue;

            log.info("Executing sql: " + preparedUpdateSqlString);
            sqlLog.info(preparedUpdateSqlString);
            String broadcastSqlString = preparedUpdateSqlString;

            PreparedStatement updateStatement = null;
            try {
                updateStatement = connection.prepareStatement(preparedUpdateSqlString);
                int index = 1;
                List columnValues = new ArrayList(propertiesToSet.length);
                Set columnsSetForTable = new HashSet();

                for (int i = 0; i < propertiesToSet.length; i++) {
                    String propertyName = propertiesToSet[i];
                    Property property = schema.getProperty(propertyName);

                    if (property != null) {
                        // For a derived property set all columns which represent parameters
                        Column[] columns = property.getColumns();

                        for (int j = 0; j < columns.length; j++) {
                            TableColumn column = (TableColumn) columns[j];

                            if (columnsSetForTable.contains(column)) continue;
                            columnsSetForTable.add(column);

                            if (Table.getInstance(catalogSchemaTable, schema.getDefaultDataSourceName()).getColumn(column.getName()) != null) {
                                Object columnValue = getColumnValue(bean, column);
                                columnValues.add(columnValue);
                                int jdbcType = column.getJdbcType();
                                DBUtilities.setValueOnPreparedStatement(updateStatement, index, jdbcType, columnValue);
                                index++;
                            }
                        }
                    }
                }

                // Set the where clause, using the key for the bean,
                setWhereClauseValues(bean, catalogSchemaTable, columnsToKey, columnValues, updateStatement, index);

                broadcastSqlString = sqlFormatter.replace(broadcastSqlString, columnValues.toArray(new Object[columnValues.size()])).toString();

                log.info("Broadcast sql: " + broadcastSqlString);
                int numberOfRowsAffected = updateStatement.executeUpdate();
                log.info(numberOfRowsAffected + " affected");
            } catch (SQLException e) {
                log.error("Failed to execute sql: " + broadcastSqlString, e);
                throw(e);
            } finally {
                DBUtilities.closeResultSetAndStatement(null, updateStatement);
            }

            // broadcast the insert sql
            if (broadcaster != null) {
                broadcaster.send(broadcastSqlString);
            }
            if (updateLocalCache) {
                BeanFactory.getInstance(typeClass).updateValue(bean);
            }
        }
    }


    public void deleteObject(Object bean) throws SQLException, NotificationException {
        initConnection();
        Class typeClass = bean.getClass();
        Schema schema = Schema.getInstance(typeClass);
        Column[] columnsToKey = schema.getUnionOfKeyColumns();
        // Don't delete without a primary key
        if (columnsToKey.length == 0) new SQLException("Can't delete without primary key");
        CatalogSchemaTable[] catalogSchemaTables = schema.getSql().getTables();

        for (int i = catalogSchemaTables.length - 1; i >= 0; i--) {
            CatalogSchemaTable catalogSchemaTable = catalogSchemaTables[i];

            String preparedDeleteSqlString = getPreparedDeleteSql(typeClass, catalogSchemaTable);

            // if none of the properties to set apply to this table, skip to the next table
            if (preparedDeleteSqlString == null) continue;

            log.info("Executing sql: " + preparedDeleteSqlString);
            sqlLog.info(preparedDeleteSqlString);
            String broadcastSqlString = preparedDeleteSqlString;

            PreparedStatement deleteStatement = null;
            try {
                deleteStatement = connection.prepareStatement(preparedDeleteSqlString);
                List columnValues = new ArrayList();
                setWhereClauseValues(bean, catalogSchemaTable, columnsToKey, columnValues, deleteStatement, 1);
                broadcastSqlString = sqlFormatter.replace(broadcastSqlString, columnValues.toArray(new Object[columnValues.size()])).toString();
                
                log.info("Broadcast sql: " + broadcastSqlString);
                int numberOfRowsAffected = deleteStatement.executeUpdate();
                log.info(numberOfRowsAffected + " affected");
            } catch (SQLException e) {
                log.error("Failed to execute sql: " + broadcastSqlString, e);
                throw(e);
            } finally {
                DBUtilities.closeResultSetAndStatement(null, deleteStatement);
            }

            // broadcast the insert sql
            if (broadcaster != null) {
                broadcaster.send(broadcastSqlString);
            }
            if (updateLocalCache) {
                BeanFactory.getInstance(typeClass).removeValue(bean);
            }
        }
    }

    public void commit() throws SQLException, NotificationException {
        if (log.isDebug()) log.debug("Committing");
        try {
            DBUtilities.commit(connection);
            if (broadcaster != null) broadcaster.commit(); //only if commit on DB succeeds
        } finally {
            cleanUp();
        }
    }

    public void rollback() {
        if (log.isDebug()) log.debug("Rolling back");
        cleanUp();
    }

    private void cleanUp() {
        // at end of Transaction, clear all beans loaded during transaction, do not fire events
        if (clearCachesAtEndOfTransaction) {
            BeanFactory.clearAllBeanFactories(dataSourceName, SqlBeanFactory.class, false, false);
        }
        DBUtilities.rollback(connection);
        DBUtilities.close(connection);
        connection = null;
        if (broadcaster != null) broadcaster.rollback();
    }

    private void setWhereClauseValues(Object bean, CatalogSchemaTable catalogSchemaTable, Column[] columnsToKey, List columnValues, PreparedStatement statement, int index) throws SQLException {
        for (int i = 0; i < columnsToKey.length; i++) {
            String columnToKey = columnsToKey[i].getName();
            TableColumn column = TableColumn.getInstance(columnToKey, catalogSchemaTable, dataSourceName);
            if (column != null) {
                Object columnValue = getColumnValue(bean, columnsToKey[i]);
                columnValues.add(columnValue);
                int jdbcType = ((TableColumn) columnsToKey[i]).getJdbcType();
                DBUtilities.setValueOnPreparedStatement(statement, index, jdbcType, columnValue);
                index++;
            }
        }
        return;
    }

    private static String getPreparedInsertSql(Class typeClass, CatalogSchemaTable catalogSchemaTable, boolean addGeneratedColumns) {
        Object statementKey = new CatalogSchemaTableAndTypeKey(typeClass, catalogSchemaTable, addGeneratedColumns);
        String preparedInsertSqlString = (String) insertStatementCache.get(statementKey);
        if (preparedInsertSqlString != null) return preparedInsertSqlString;

        // preparedInsertSqlString used to do the insert into the databse
        StringBuffer preparedInsertSqlStringBuffer = new StringBuffer();
        preparedInsertSqlStringBuffer.append("INSERT INTO ");
        preparedInsertSqlStringBuffer.append(catalogSchemaTable.getRepresentation());
        preparedInsertSqlStringBuffer.append(" (");

        Schema schema = Schema.getInstance(typeClass);
        TableColumn[] columns = Table.getInstance(catalogSchemaTable, schema.getDefaultDataSourceName()).getColumns(typeClass);

        int columnsAdded = 0;
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].getIdFactory() == null || columns[i].getIdFactory().isGeneratedPreInsert()) {
                if (columnsAdded > 0) preparedInsertSqlStringBuffer.append(", ");
                preparedInsertSqlStringBuffer.append(columns[i].getName());
                columnsAdded++;
            }
        }

        if (addGeneratedColumns) {
            // append post insert generated columns to end
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].getIdFactory() != null && columns[i].getIdFactory().isGeneratedPostInsert()) {
                    if (columnsAdded > 0) preparedInsertSqlStringBuffer.append(", ");
                    preparedInsertSqlStringBuffer.append(columns[i].getName());
                    columnsAdded++;
                }
            }
        }

        preparedInsertSqlStringBuffer.append(") VALUES (");
        columnsAdded = 0;
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].getIdFactory() == null || columns[i].getIdFactory().isGeneratedPreInsert()) {
                if (columnsAdded > 0) preparedInsertSqlStringBuffer.append(", ");
                preparedInsertSqlStringBuffer.append("?");
                columnsAdded++;
            }
        }

        if (addGeneratedColumns) {
            // append post insert generated columns to end
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].getIdFactory() != null && columns[i].getIdFactory().isGeneratedPostInsert()) {
                    if (columnsAdded > 0) preparedInsertSqlStringBuffer.append(", ");
                    preparedInsertSqlStringBuffer.append("?");
                    columnsAdded++;
                }
            }
        }
        preparedInsertSqlStringBuffer.append(") ");

        preparedInsertSqlString = preparedInsertSqlStringBuffer.toString();
        insertStatementCache.put(statementKey, preparedInsertSqlString);
        return preparedInsertSqlString;
    }

    private String getPreparedUpdateSql(Class typeClass, CatalogSchemaTable catalogSchemaTable, String[] propertyNames) {
        Object statementKey = new CatalogSchemaTableAndTypeAndPropertiesKey(typeClass, catalogSchemaTable, false, propertyNames);
        String preparedUpdateSqlString = (String) updateStatementCache.get(statementKey);
        if (updateStatementCache.containsKey(statementKey)) return preparedUpdateSqlString;

        Schema schema = Schema.getInstance(typeClass);
        StringBuffer preparedUpdateSqlStringBuffer = new StringBuffer();
        preparedUpdateSqlStringBuffer.append("UPDATE ");
        preparedUpdateSqlStringBuffer.append(catalogSchemaTable.getRepresentation());
        preparedUpdateSqlStringBuffer.append(" SET ");

        Set columnsSetForTable = new HashSet();

        int index = 0;
        for (int i = 0; i < propertyNames.length; i++) {
            String propertyName = propertyNames[i];
            Property property = schema.getProperty(propertyName);

            if (property != null) {
                Column[] columns = property.getColumns();
                for (int j = 0; j < columns.length; j++) {
                    TableColumn column = (TableColumn) columns[j];

                    if (columnsSetForTable.contains(column)) continue;
                    columnsSetForTable.add(column);

                    if (Table.getInstance(catalogSchemaTable, schema.getDefaultDataSourceName()).getColumn(column.getName()) != null) {
                        if (index > 0) preparedUpdateSqlStringBuffer.append(", ");
                        preparedUpdateSqlStringBuffer.append(column.getName());
                        preparedUpdateSqlStringBuffer.append("= ?");
                        index++;
                    }
                }
            }
        }

        // if no properties to set for this table, there is no point carrying on building a where clause
        if (index == 0) {
            updateStatementCache.put(statementKey, null);
            return null;
        }

        Column[] columnsToKey = schema.getUnionOfKeyColumns();
        createPreparedWhereClause(catalogSchemaTable, columnsToKey, preparedUpdateSqlStringBuffer);

        preparedUpdateSqlString = preparedUpdateSqlStringBuffer.toString();
        updateStatementCache.put(statementKey, preparedUpdateSqlString);
        return preparedUpdateSqlString;
    }

    private String getPreparedDeleteSql(Class typeClass, CatalogSchemaTable catalogSchemaTable) {
        Object statementKey = new CatalogSchemaTableAndTypeKey(typeClass, catalogSchemaTable, false);
        String preparedDeleteSqlString = (String) deleteStatementCache.get(statementKey);
        if (preparedDeleteSqlString != null) return preparedDeleteSqlString;

        StringBuffer preparedDeleteSqlStringBuffer = new StringBuffer();
        preparedDeleteSqlStringBuffer.append("DELETE FROM ");
        preparedDeleteSqlStringBuffer.append(catalogSchemaTable.getRepresentation());

        Schema schema = Schema.getInstance(typeClass);
        Column[] columnsToKey = schema.getUnionOfKeyColumns();
        createPreparedWhereClause(catalogSchemaTable, columnsToKey, preparedDeleteSqlStringBuffer);

        preparedDeleteSqlString = preparedDeleteSqlStringBuffer.toString();
        deleteStatementCache.put(statementKey, preparedDeleteSqlString);
        return preparedDeleteSqlString;
    }

    private void createPreparedWhereClause(CatalogSchemaTable catalogSchemaTable, Column[] columnsToKey, StringBuffer sqlStringBuffer) {
        for (int i = 0; i < columnsToKey.length; i++) {
            String columnToKey = columnsToKey[i].getName();
            TableColumn column = TableColumn.getInstance(columnToKey, catalogSchemaTable, dataSourceName);
            if (column != null) {
                if (i == 0) sqlStringBuffer.append(" WHERE ");
                if (i != 0) sqlStringBuffer.append(" AND ");
                sqlStringBuffer.append(column.getRepresentation());
                sqlStringBuffer.append("=?");
            }
        }
    }

    private static class CatalogSchemaTableAndTypeKey {
        private Class type;
        private CatalogSchemaTable catalogSchemaTable;
        private boolean generatedColumns;

        public CatalogSchemaTableAndTypeKey(Class type, CatalogSchemaTable catalogSchemaTable, boolean generatedColumns) {
            this.type = type;
            this.catalogSchemaTable = catalogSchemaTable;
            this.generatedColumns = generatedColumns;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CatalogSchemaTableAndTypeKey)) return false;

            final CatalogSchemaTableAndTypeKey catalogSchemaTableAndTypeKey = (CatalogSchemaTableAndTypeKey) o;

            if (generatedColumns != catalogSchemaTableAndTypeKey.generatedColumns) return false;
            if (!catalogSchemaTable.equals(catalogSchemaTableAndTypeKey.catalogSchemaTable)) return false;
            if (!type.equals(catalogSchemaTableAndTypeKey.type)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = type.hashCode();
            result = 29 * result + catalogSchemaTable.hashCode();
            result = 29 * result + (generatedColumns ? 1 : 0);
            return result;
        }
    }

    private static class CatalogSchemaTableAndTypeAndPropertiesKey extends CatalogSchemaTableAndTypeKey {
        private String[] properties;

        public CatalogSchemaTableAndTypeAndPropertiesKey(Class type, CatalogSchemaTable catalogSchemaTable, boolean generatedColumns, String[] properties) {
            super(type, catalogSchemaTable, generatedColumns);
            this.properties = properties;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CatalogSchemaTableAndTypeAndPropertiesKey)) return false;
            if (!super.equals(o)) return false;

            final CatalogSchemaTableAndTypeAndPropertiesKey catalogSchemaTableAndTypeAndPropertiesKey = (CatalogSchemaTableAndTypeAndPropertiesKey) o;

            if (!Arrays.equals(properties, catalogSchemaTableAndTypeAndPropertiesKey.properties)) return false;

            return true;
        }

        public int hashCode() {
            int result = super.hashCode();
            for (int i = 0; i < properties.length; i++) {
                String property = properties[i];
                result = 29 * result + property.hashCode();
            }
            return result;
        }
    }
}