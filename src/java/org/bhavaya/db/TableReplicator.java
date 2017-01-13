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

import org.bhavaya.collection.EfficientArrayList;
import org.bhavaya.coms.NotificationException;
import org.bhavaya.coms.NotificationPublisher;
import org.bhavaya.util.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.22 $
 */
public class TableReplicator {
    private static final Log log = Log.getCategory(TableReplicator.class);

    private String sourceDatasource;
    private NotificationPublisher notificationPublisher;
    private String destinationSql;
    private String sourceSql;
    private String destinationDatasource;
    private Table destinationTable;
    private Mapping[] mappings;
    private Mapping[] keyMappings;
    private String preparedInsertSqlString;
    private String preparedUpdateSqlString;
    private String preparedDeleteSqlString;
    private int maxBatchSize;

    public static Mapping[] getMappings(Table destinationTable) {
        TableColumn[] keyColumns = destinationTable.getPrimaryKey();
        return getMappings(destinationTable, keyColumns);
    }

    public static Mapping[] getMappings(Table destinationTable, String indexName) {
        final TableIndex index = destinationTable.getIndex(indexName);
        if (index == null) throw new RuntimeException("Cannot find index: " + indexName + " for table: " + destinationTable);
        TableColumn[] keyColumns = index.getTableColumns();
        return getMappings(destinationTable, keyColumns);
    }

    private static Mapping[] getMappings(Table destinationTable, TableColumn[] keyColumns) {
        Set primaryKey = new HashSet(Arrays.asList(keyColumns));

        TableColumn[] tableColumns = destinationTable.getColumns();
        Mapping[] mappings = new Mapping[tableColumns.length];
        for (int i = 0; i < tableColumns.length; i++) {
            TableColumn tableColumn = tableColumns[i];
            mappings[i] = new ColumnMapping(tableColumn, tableColumn, primaryKey.contains(tableColumn));
        }
        return mappings;
    }

    public static void replicate(final String sourceDatasource, final String sourceSql, final String destinationDatasource, final String destinationSql, Table destinationTable, final TableReplicator.Mapping[] mappings, final NotificationPublisher publisher, int maxBatchSize, final String[] groupComponentNames, final String selectGroupsSql) {
        /**
         * TODO this method might fail when groupValues contain question marks.
         * As I haven't found any usages of this method I'm just leaving this message here as a warning for its future users.
         * There is a problem with SQLFormatter when the values being used in place of question marks contain question marks.
         * Problem with this method is that if groupValues contain question marks - tableReplicator.replicate(); call fails
         * as there is another call to the SQLFormatter.replace method inside of this call.
         */
        SQLFormatter sourceSqlFormatter = SQLFormatter.getInstance(sourceDatasource);
        SQLFormatter destinationSqlFormatter = SQLFormatter.getInstance(destinationDatasource);
        List groups = DBUtilities.execute(sourceDatasource, selectGroupsSql);
        log.info("Split data into " + groups.size() + " groups");

        for (int j = 0; j < groups.size(); j++) {
            Object group = groups.get(j);

            Object[] groupComponentValues = new Object[groupComponentNames.length];
            for (int i = 0; i < groupComponentValues.length; i++) {
                groupComponentValues[i] = Generic.get(group, groupComponentNames[i]);
            }

            log.info("Replicating group: " + j + " of " + groups.size() + ": " + Utilities.asString(groupComponentValues, ", "));

            String sourceSqlToRun = sourceSql;
            String destinationSqlToRun = destinationSql;
            sourceSqlToRun = sourceSqlFormatter.replace(sourceSqlToRun, groupComponentValues).toString();
            destinationSqlToRun = destinationSqlFormatter.replace(destinationSqlToRun, groupComponentValues).toString();

            TableReplicator tableReplicator = new TableReplicator(sourceDatasource,
                    sourceSqlToRun,
                    destinationDatasource,
                    destinationSqlToRun,
                    destinationTable,
                    mappings,
                    publisher,
                    maxBatchSize);

            tableReplicator.replicate();
        }
    }

    public TableReplicator(String sourceDatasource, String sourceSql, String destinationDatasource, String destinationSql, Table destinationTable, Mapping[] mappings, NotificationPublisher notificationPublisher, int maxBatchSize) {
        this.sourceDatasource = sourceDatasource;
        this.sourceSql = ApplicationProperties.substituteApplicationProperties(sourceSql);
        this.destinationDatasource = destinationDatasource;
        if (destinationSql == null) {
            destinationSql = "SELECT * FROM " + destinationTable.getCatalogSchemaTable().getRepresentation();
        }
        this.destinationSql = ApplicationProperties.substituteApplicationProperties(destinationSql);
        this.destinationTable = destinationTable;
        this.mappings = mappings;

        List keyMappingsList = new ArrayList();
        for (int i = 0; i < mappings.length; i++) {
            Mapping mapping = mappings[i];
            if (mapping.isPartOfKey()) keyMappingsList.add(mapping);
        }
        keyMappings = (Mapping[]) keyMappingsList.toArray(new Mapping[keyMappingsList.size()]);
        this.notificationPublisher = notificationPublisher;
        this.maxBatchSize = maxBatchSize;
    }

    public void replicate() {
        Connection destinationConnection = null;
        Connection sourceConnection = null;
        Statement statement = null;

        try {
            long startTime = System.currentTimeMillis();
            log.info("Replicating from " + sourceDatasource + " to " + destinationDatasource + "@" + destinationTable);
            destinationConnection = DataSourceFactory.getInstance(destinationDatasource).getConnection();
            sourceConnection = DataSourceFactory.getInstance(sourceDatasource).getConnection();
            if (notificationPublisher != null) notificationPublisher.connect();
            statement = destinationConnection.createStatement();
            initConnections(sourceConnection, destinationConnection, statement);
            replicate(sourceConnection, destinationConnection, statement);
            log.info("Committing");
            DBUtilities.commit(sourceConnection);
            DBUtilities.commit(destinationConnection);
            if (notificationPublisher != null) notificationPublisher.commit();
            log.info("Replicated from " + sourceDatasource + " to " + destinationDatasource + "@" + destinationTable + " in " + (System.currentTimeMillis() - startTime) + " millis");
        } catch (Exception e) {
            log.error(e);
        } finally {
            DBUtilities.closeResultSetAndStatement(null, statement);
            DBUtilities.rollback(sourceConnection);
            DBUtilities.close(sourceConnection);
            DBUtilities.rollback(destinationConnection);
            DBUtilities.close(destinationConnection);
            if (notificationPublisher != null) notificationPublisher.rollback();
        }
    }

    private void replicate(final Connection sourceConnection, final Connection destinationConnection, final Statement statement) throws SQLException, NotificationException {
        Map destinationValues = DBUtilities.execute(destinationConnection, destinationSql, new Utilities.KeyColumnsToKeyTransform(getDestinationKeyColumnNames()));

        Map sourceValues = DBUtilities.execute(sourceConnection, sourceSql, new Transform() {
            public Object execute(Object sourceData) {
                return createKey(sourceData);
            }
        });

        String insertStatementSql = getPreparedInsertSql();
        String updateStatementsql = getPreparedUpdateSql();
        String deleteStatementSql = getPreparedDeleteSql();

        Set insertedKeys = new HashSet(sourceValues.size() / 4);
        Set updatedKeys = new HashSet(sourceValues.size() / 4);
        Set ignoredKeys = new HashSet(sourceValues.size() / 4);
        Set deletedKeys = new HashSet(sourceValues.size() / 4);
        MutableInteger batchSize = new MutableInteger(0);

        for (Iterator iterator = sourceValues.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object key = entry.getKey();
            Object sourceRow = entry.getValue();

            Object destinationRow = destinationValues.get(key);

            if (key == null) {
                log.info("Ignoring null key for row: " + BeanUtilities.toString(sourceRow));
            } else if (destinationRow == null) {
                if (insertedKeys.contains(key)) {
                    log.warn("Found duplicate key: " + key + " for row: " + BeanUtilities.toString(sourceRow));
                } else {
                    insertedKeys.add(key);
                    final SqlIndexPair sqlIndexPair = new SqlIndexPair(insertStatementSql, 1);
                    setValueOnStatement(sourceRow, statement, sqlIndexPair);
                    saveToDatabase(sqlIndexPair, statement, destinationConnection, batchSize);
                    insert(key, sourceRow, destinationConnection, statement, batchSize);
                }
            } else {
                if (isDifferent(sourceRow, destinationRow)) {
                    updatedKeys.add(key);
                    final SqlIndexPair sqlIndexPair = new SqlIndexPair(updateStatementsql, 1);
                    setValuesAndKeyOnStatement(sourceRow, key, statement, sqlIndexPair);
                    saveToDatabase(sqlIndexPair, statement, destinationConnection, batchSize);
                    update(key, sourceRow, destinationConnection, statement, batchSize);
                } else {
//                    if (log.isDebug()) log.debug("Ignoring key: " + key + " for row: " + BeanUtilities.toString(sourceRow));
                    ignoredKeys.add(key);
                }
            }
        }


        for (Iterator iterator = destinationValues.keySet().iterator(); iterator.hasNext();) {
            Object destinationKey = iterator.next();
            if (!ignoredKeys.contains(destinationKey) && !updatedKeys.contains(destinationKey) && !insertedKeys.contains(destinationKey)) {
                deletedKeys.add(destinationKey);
            }
        }
        for (Iterator iterator = deletedKeys.iterator(); iterator.hasNext();) {
            Object keyToDelete = iterator.next();
            final SqlIndexPair sqlIndexPair = new SqlIndexPair(deleteStatementSql, 1);
            setKeyOnStatement(keyToDelete, sqlIndexPair);
            saveToDatabase(sqlIndexPair, statement, destinationConnection, batchSize);
            delete(keyToDelete, destinationConnection, statement, batchSize);
        }

        int rowsAffected = DBUtilities.executeBatch(statement);
        log.info(rowsAffected + " rows affected");

        log.info("Ignored " + ignoredKeys.size() + " records");
        log.info("Inserted " + insertedKeys.size() + " records");
        log.info("Updated " + updatedKeys.size() + " records");
        log.info("Deleted " + deletedKeys.size() + " records");
    }

    protected void saveToDatabase(final SqlIndexPair sqlIndexPair, Statement statement, Connection destinationConnection, MutableInteger batchSize) throws SQLException, NotificationException {
        if (log.isDebug()) log.debug(sqlIndexPair.sql);
        statement.addBatch(sqlIndexPair.sql);
        batchSize.value++;
        if (maxBatchSize > 0 && batchSize.value >= maxBatchSize) {
            log.info("Executing batch and committing at limit: " + maxBatchSize);
            int rowsAffected = DBUtilities.executeBatch(statement);
            log.info(rowsAffected + " rows affected");
            DBUtilities.commit(destinationConnection);
            batchSize.value = 0;
        }
        if (notificationPublisher != null) notificationPublisher.send(sqlIndexPair.sql);
    }

    // for subclass hooks
    protected void insert(Object key, Object sourceRow, Connection destinationConnection, Statement statement, MutableInteger batchSize) throws SQLException, NotificationException {
    }

    // for subclass hooks
    protected void update(Object key, Object sourceRow, Connection destinationConnection, Statement statement, MutableInteger batchSize) throws SQLException, NotificationException {
    }

    // for subclass hooks
    protected void delete(Object key, Connection destinationConnection, Statement statement, MutableInteger batchSize) throws SQLException, NotificationException {
    }

    // for subclass hooks
    protected void commit() {
    }

    // for subclass hooks
    protected void rollback() {
    }

    // for subclass hooks
    public void initConnections(Connection sourceConnection, Connection destinationConnection, Statement statement) throws SQLException {
    }

    // for subclass hooks
    public void closeDestinationConnection(Connection connection) throws SQLException {
    }

    private List getDestinationValues(Object sourceRow) {
        List destinationValues = new ArrayList(mappings.length);
        for (int i = 0; i < mappings.length; i++) {
            Mapping mapping = mappings[i];
            destinationValues.add(mapping.getDestinationValue(sourceRow));
        }
        return destinationValues;
    }

    protected void setValueOnStatement(Object sourceRow, Statement statement, SqlIndexPair sqlIndexPair) throws SQLException {
        SQLFormatter sqlFormatter = SQLFormatter.getInstance(destinationDatasource);
        List destinationValues = getDestinationValues(sourceRow);
        sqlIndexPair.sql = sqlFormatter.replace(sqlIndexPair.sql, destinationValues.toArray()).toString();
        sqlIndexPair.index += destinationValues.size();
    }

    protected void setKeyOnStatement(Object key, SqlIndexPair sqlIndexPair) throws SQLException {
        SQLFormatter sqlFormatter = SQLFormatter.getInstance(destinationDatasource);

        if (keyMappings.length > 1) {
            List compoundKey = (List) key;
            Object[] compoudKeyObjects = compoundKey.toArray();
            sqlIndexPair.sql = sqlFormatter.replace(sqlIndexPair.sql, compoudKeyObjects).toString();
            sqlIndexPair.index += compoudKeyObjects.length;
        } else {
            sqlIndexPair.sql = sqlFormatter.replace(sqlIndexPair.sql, key);
            sqlIndexPair.index++;
        }
    }

    protected void setValuesAndKeyOnStatement(Object sourceRow, Object key, Statement statement, SqlIndexPair sqlIndexPair) throws SQLException {
        SQLFormatter sqlFormatter = SQLFormatter.getInstance(destinationDatasource);
        List values = getDestinationValues(sourceRow);
        if (keyMappings.length > 1) {
            values.addAll((List) key);
        } else {
            values.add(key);
        }
        sqlIndexPair.sql = sqlFormatter.replace(sqlIndexPair.sql, values.toArray()).toString();
        sqlIndexPair.index += values.size();
    }

    private boolean isDifferent(Object sourceRow, Object destinationRow) {
        for (int i = 0; i < mappings.length; i++) {
            Mapping mapping = mappings[i];
            if (mapping.isDifferent(sourceRow, destinationRow)) {
                return true;
            }
        }
        return false;
    }

    private String[] getDestinationKeyColumnNames() {
        String[] keyColumnNames = new String[keyMappings.length];
        for (int i = 0; i < keyColumnNames.length; i++) {
            keyColumnNames[i] = keyMappings[i].getDestinationColumn().getName();
        }
        return keyColumnNames;
    }

    private String getPreparedInsertSql() {
        if (preparedInsertSqlString != null) return preparedInsertSqlString;

        StringBuffer preparedSqlStringBuffer = new StringBuffer();
        preparedSqlStringBuffer.append("INSERT INTO ");
        preparedSqlStringBuffer.append(destinationTable.getCatalogSchemaTable().getRepresentation());

        preparedSqlStringBuffer.append(" ( ");
        for (int i = 0; i < mappings.length; i++) {
            Mapping mapping = mappings[i];
            TableColumn destinationColumn = mapping.getDestinationColumn();
            if (i > 0) preparedSqlStringBuffer.append(", ");
            preparedSqlStringBuffer.append(destinationColumn.getName());
        }

        preparedSqlStringBuffer.append(" ) ");
        preparedSqlStringBuffer.append(" VALUES ");
        preparedSqlStringBuffer.append(" ( ");

        for (int i = 0; i < mappings.length; i++) {
            if (i > 0) preparedSqlStringBuffer.append(", ");
            preparedSqlStringBuffer.append('?');
        }
        preparedSqlStringBuffer.append(" ) ");

        preparedInsertSqlString = preparedSqlStringBuffer.toString();
        return preparedInsertSqlString;
    }

    private String getPreparedUpdateSql() {
        if (preparedUpdateSqlString != null) return preparedUpdateSqlString;

        StringBuffer preparedSqlStringBuffer = new StringBuffer();
        preparedSqlStringBuffer.append("UPDATE ");
        preparedSqlStringBuffer.append(destinationTable.getCatalogSchemaTable().getRepresentation());

        preparedSqlStringBuffer.append(" SET ");

        for (int i = 0; i < mappings.length; i++) {
            Mapping mapping = mappings[i];
            TableColumn destinationColumn = mapping.getDestinationColumn();
            if (i > 0) preparedSqlStringBuffer.append(", ");
            preparedSqlStringBuffer.append(destinationColumn.getName()).append(" = ?");
        }

        appendWhereClause(preparedSqlStringBuffer);

        preparedUpdateSqlString = preparedSqlStringBuffer.toString();
        return preparedUpdateSqlString;
    }

    private String getPreparedDeleteSql() {
        if (preparedDeleteSqlString != null) return preparedDeleteSqlString;

        StringBuffer preparedSqlStringBuffer = new StringBuffer();
        preparedSqlStringBuffer.append("DELETE ");
        preparedSqlStringBuffer.append(destinationTable.getCatalogSchemaTable().getRepresentation());

        appendWhereClause(preparedSqlStringBuffer);

        preparedDeleteSqlString = preparedSqlStringBuffer.toString();
        return preparedDeleteSqlString;
    }

    private void appendWhereClause(StringBuffer preparedSqlStringBuffer) {
        preparedSqlStringBuffer.append(" WHERE ");

        for (int i = 0; i < keyMappings.length; i++) {
            Mapping keyMapping = keyMappings[i];
            TableColumn destinationKeyColumn = keyMapping.getDestinationColumn();
            if (i > 0) preparedSqlStringBuffer.append(" AND ");
            preparedSqlStringBuffer.append(destinationKeyColumn.getName()).append(" = ?");
        }
    }


    private Object createKey(Object row) {
        Object key;

        if (keyMappings.length == 1) {
            key = keyMappings[0].getDestinationValue(row);
        } else {
            key = new EfficientArrayList(keyMappings.length);

            for (int i = 0; i < keyMappings.length; i++) {
                Mapping keyMapping = keyMappings[i];
                final Object keyComponent = keyMapping.getDestinationValue(row);
                ((List) key).add(keyComponent);
            }
        }

        return key;
    }

    protected static class SqlIndexPair {
        public String sql;
        public int index;

        public SqlIndexPair(String sql, int index) {
            this.sql = sql;
            this.index = index;
        }
    }

    public static interface Mapping {
        public boolean isPartOfKey();

        public Object getDestinationValue(Object sourceRow);

        public TableColumn getDestinationColumn();

        public boolean isDifferent(Object sourceRow, Object destinationRow);
    }

    public static abstract class AbstractMapping implements Mapping {
        protected TableColumn destinationColumn;
        protected boolean partOfKey;

        public AbstractMapping(TableColumn destinationColumn, boolean partOfKey) {
            this.destinationColumn = destinationColumn;
            this.partOfKey = partOfKey;
        }

        public TableColumn getDestinationColumn() {
            return destinationColumn;
        }

        public boolean isPartOfKey() {
            return partOfKey;
        }

        public boolean isDifferent(Object sourceRow, Object destinationRow) {
            Object sourceValue = getDestinationValue(sourceRow);
            Object destinationValue = Generic.get(destinationRow, destinationColumn.getName());
            if (sourceValue != null && destinationValue != null) {
                if (!destinationValue.getClass().isAssignableFrom(sourceValue.getClass())) {
                    log.error("Trying to compare: " + sourceValue.getClass().getName() + " with " + destinationValue.getClass().getName() + " for destinationColumn: " + destinationColumn);
                } else if (sourceValue instanceof String) {
                    return isDifferent((String) sourceValue, (String) destinationValue);
                } else if (sourceValue instanceof java.util.Date) {
                    return isDifferent((java.util.Date) sourceValue, (java.util.Date) destinationValue);
                } else if (sourceValue instanceof Number) {
                    return isDifferent((Number) sourceValue, (Number) destinationValue);
                }
            }

            return !Utilities.equals(sourceValue, destinationValue);
        }

        private boolean isDifferent(java.util.Date date1, java.util.Date date2) {
            if (Math.abs(date1.getTime() - date2.getTime()) < 2) { //2 millis
                return false;
            }
            return true;
        }

        private boolean isDifferent(Number number1, Number number2) {
            if (Math.abs(number1.doubleValue() - number2.doubleValue()) < 0.000001) {
                return false;
            }
            return true;
        }

        private boolean isDifferent(String string1, String string2) {
            if (destinationColumn.getColumnSize() < string1.length()) {
                string1 = string1.substring(0, destinationColumn.getColumnSize());
            }
            return !string1.equals(string2);
        }
    }

    public static class ColumnMapping extends AbstractMapping {
        private TableColumn sourceColumn;
        private Transform sourceTransform;

        public ColumnMapping(TableColumn sourceColumn, TableColumn destinationColumn, boolean partOfKey) {
            this(sourceColumn, destinationColumn, partOfKey, null);
        }

        public ColumnMapping(TableColumn sourceColumn, TableColumn destinationColumn, boolean partOfKey, Transform sourceTransform) {
            super(destinationColumn, partOfKey);
            this.sourceColumn = sourceColumn;
            this.sourceTransform = sourceTransform;
        }

        public Object getDestinationValue(Object sourceRow) {
            Object destinationValue = Generic.get(sourceRow, sourceColumn.getName());
            if (sourceTransform != null) destinationValue = sourceTransform.execute(destinationValue);
            destinationValue = Utilities.changeType(destinationColumn.getType(), destinationValue);
            return destinationValue;
        }
    }
}