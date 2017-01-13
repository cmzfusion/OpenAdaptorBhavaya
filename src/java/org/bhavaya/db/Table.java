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

import org.bhavaya.beans.Index;
import org.bhavaya.beans.Property;
import org.bhavaya.beans.Schema;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import java.sql.SQLException;
import java.util.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.11 $
 */
public class Table {
    private static final Log log = Log.getCategory(Table.class);
    private static final ForeignKey[] EMPTY_FOREIGN_KEY_ARRAY = new ForeignKey[]{};
    private static final TableColumn[] EMPTY_TABLECOLUMN_ARRAY = new TableColumn[]{};

    private CatalogSchemaTable catalogSchemaTable;
    private TableColumn[] primaryKey;
    private TableColumn[] columns;
    private boolean loadedColumnsFromMetadata;
    private Map columnsByName;
    private Map columnsWithMatchingPropertiesByType = new HashMap();
    private ForeignKey[] foreignKeys;
    private Map indicies;
    private ForeignKey[] exportedForeignKeys;
    private String dataSourceName;
    private Database database;

    public static synchronized Table getInstance(CatalogSchemaTable catalogSchemaTable, String dataSourceName) {
        if (dataSourceName == null) dataSourceName = DataSourceFactory.getDefaultDataSourceName();
        dataSourceName = DataSourceFactory.getDatabaseMetadataDatasource(dataSourceName);

        try {
            Database database = Database.getInstance(catalogSchemaTable.getCatalogSchema(), dataSourceName);
            if (database == null) return null;
            CatalogSchemaTable underlyingTable = catalogSchemaTable instanceof AliasedCatalogSchemaTable ? ((AliasedCatalogSchemaTable) catalogSchemaTable).getUnderlyingTable() : catalogSchemaTable;
            Table table = (Table) database.getTables().get(underlyingTable);
            if (table == null) {
                log.error("Cannot find table for: " + catalogSchemaTable + "@" + dataSourceName);
            }
            return table;
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    protected Table(CatalogSchemaTable catalogSchemaTable, Database database) {
        this.database = database;
        dataSourceName = database.getDataSourceName();
        this.catalogSchemaTable = catalogSchemaTable;
    }

    public CatalogSchemaTable getCatalogSchemaTable() {
        return catalogSchemaTable;
    }

    public Database getDatabase() {
        return database;
    }

    public TableColumn[] getPrimaryKey() {
        findPrimaryKey();
        return primaryKey;
    }

    private synchronized void findPrimaryKey() {
        if (primaryKey != null) return;
        if (log.isDebug()) log.debug("Finding primary key for: " + catalogSchemaTable);
        long startTime = System.currentTimeMillis();

        try {
            MetaDataSource.CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = MetaDataSource.getInstance(dataSourceName).getCatalogSchemaMetaDataSource(catalogSchemaTable.getCatalogSchema());
            MetaDataSource.TableMetaDataSource tableMetaDataSource = catalogSchemaMetaDataSource.getTableMetaDataSource(catalogSchemaTable.getTableName());
            String[] primaryKeyColumnNames = tableMetaDataSource.getPrimaryKeyColumnNames();

            List primaryKeyList = new ArrayList();
            for (int i = 0; i < primaryKeyColumnNames.length; i++) {
                String primaryKeyColumnName = primaryKeyColumnNames[i];
                TableColumn primaryKeyColumn = getColumn(primaryKeyColumnName);
                primaryKeyList.add(primaryKeyColumn);
                if (log.isDebug()) log.debug("Found primary key column: " + primaryKeyColumnName);
            }

            if (primaryKeyList.size() == 0) {
                log.warn("No primary key for: " + catalogSchemaTable);
                if (log.isDebug()) log.debug("Finding unique index for: " + catalogSchemaTable);
                TableIndex[] indicies = getIndicies();
                for (int i = 0; i < indicies.length && primaryKey == null; i++) {
                    TableIndex index = indicies[i];
                    if (index.isUnique()) {
                        log.warn("Setting primary key to unique index: " + index.getName());
                        primaryKeyList.addAll(Arrays.asList(index.getTableColumns()));
                    }
                }
                if (primaryKeyList.size() == 0) log.warn("No unique index for: " + catalogSchemaTable);
            }
            Utilities.sort(primaryKeyList);
            setPrimaryKey((TableColumn[]) primaryKeyList.toArray(new TableColumn[primaryKeyList.size()]));
            if (log.isDebug())log.debug("Finding primary key for: " + catalogSchemaTable + " took: " + (System.currentTimeMillis() - startTime) + " millis");
        } catch (Exception e) {
            log.error("Cannot determine primary key for table: " + catalogSchemaTable, e);
            this.primaryKey = EMPTY_TABLECOLUMN_ARRAY;
        }
    }

    public TableIndex[] getIndicies() {
        findIndicies();
        return (TableIndex[]) indicies.values().toArray(new TableIndex[indicies.values().size()]);
    }

    public TableIndex getIndex(String indexNameToFind) {
        findIndicies();
        return (TableIndex) indicies.get(indexNameToFind);
    }

    private synchronized void findIndicies() {
        if (indicies != null) return;
        if (log.isDebug()) log.debug("Finding indicies for: " + catalogSchemaTable);
        long startTime = System.currentTimeMillis();

        indicies = new HashMap();

        try {
            MetaDataSource.CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = MetaDataSource.getInstance(dataSourceName).getCatalogSchemaMetaDataSource(catalogSchemaTable.getCatalogSchema());
            MetaDataSource.TableMetaDataSource tableMetaDataSource = catalogSchemaMetaDataSource.getTableMetaDataSource(catalogSchemaTable.getTableName());
            MetaDataSource.IndexData[] indexDatas = tableMetaDataSource.getIndexDatas();

            for (int i = 0; i < indexDatas.length; i++) {
                MetaDataSource.IndexData indexData = indexDatas[i];
                String indexName = indexData.getName();

                String[] columnNames = indexData.getColumns();
                TableColumn[] tableColumns = new TableColumn[columnNames.length];
                for (int j = 0; j < columnNames.length; j++) {
                    String columnName = columnNames[j];
                    tableColumns[j] = getColumn(columnName);
                    if (log.isDebug())log.debug("Found column: " + columnName + " for index: " + indexName);
                }

                Arrays.sort(tableColumns);
                Index index = new TableIndex(indexName, indexData.isUnique(), tableColumns);
                indicies.put(indexName, index);
            }

            if (log.isDebug())log.debug("Finding indicies for: " + catalogSchemaTable + " took: " + (System.currentTimeMillis() - startTime) + " millis");
        } catch (Exception e) {
            log.error("Cannot determine indicies for table: " + catalogSchemaTable, e);
        }
    }

    public void setPrimaryKey(TableColumn[] primaryKey) {
        this.primaryKey = primaryKey;
    }

    public synchronized void putColumn(TableColumn column) {
        if (columnsByName == null) columnsByName = new LinkedHashMap();
        if (columnsByName.containsKey(column.getName())) {
            return;
        }
        columnsByName.put(column.getName(), column);
        if (columns != null) updateColumns();
    }

    private void updateColumns() {
        columns = (TableColumn[]) getColumnsByName().values().toArray(new TableColumn[columnsByName.size()]);
    }

    public TableColumn getColumn(String columnName) {
        TableColumn column = (TableColumn) getColumnsByName().get(columnName);
        return column;
    }

    /**
     * Returns the columns for a database table as Attribute[].
     */
    public TableColumn[] getColumns() {
        if (columns == null) updateColumns();
        return columns;
    }

    private Map getColumnsByName() {
        findColumns();
        return columnsByName;
    }

    private synchronized void findColumns() {
        if (loadedColumnsFromMetadata) return;
        if (log.isDebug()) log.debug("Finding columns for: " + catalogSchemaTable + " in dataSourceName: " + dataSourceName);
        loadedColumnsFromMetadata = true;

        if (columnsByName == null) columnsByName = new LinkedHashMap();
        try {
            long startTime = System.currentTimeMillis();
            MetaDataSource.CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = MetaDataSource.getInstance(dataSourceName).getCatalogSchemaMetaDataSource(catalogSchemaTable.getCatalogSchema());
            MetaDataSource.TableMetaDataSource tableMetaDataSource = catalogSchemaMetaDataSource.getTableMetaDataSource(catalogSchemaTable.getTableName());
            MetaDataSource.ColumnData[] columnDatas = tableMetaDataSource.getColumnDatas();

            for (int i = 0; i < columnDatas.length; i++) {
                MetaDataSource.ColumnData columnData = columnDatas[i];
                String columnName = columnData.getName();
                TableColumn column = (TableColumn) columnsByName.get(columnName);
                if (column == null) {
                    column = new TableColumn(columnName, catalogSchemaTable, dataSourceName, columnData.getJdbcType(), columnData.getColumnSize(), columnData.getDecimalDigits(), false, null);
                    columnsByName.put(columnName, column);
                } else {
                    column.setJdbcType(columnData.getJdbcType());
                    column.setColumnSize(columnData.getColumnSize());
                    column.setDecimalDigits(columnData.getDecimalDigits());
                }
            }
            if (log.isDebug())log.debug("Finding columns for: " + catalogSchemaTable + " took: " + (System.currentTimeMillis() - startTime) + " millis");
        } catch (Exception e) {
            log.error("Cannot determine columns for table: " + catalogSchemaTable + " in dataSourceName: " + dataSourceName, e);
            throw new RuntimeException(e);
        }

        if (Schema.isGenerationMode()) {
            try {
                long startTime = System.currentTimeMillis();
                MetaDataSource.CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = MetaDataSource.getInstance(dataSourceName).getCatalogSchemaMetaDataSource(catalogSchemaTable.getCatalogSchema());
                MetaDataSource.TableMetaDataSource tableMetaDataSource = catalogSchemaMetaDataSource.getTableMetaDataSource(catalogSchemaTable.getTableName());
                MetaDataSource.ForeignKeyData[] foreignKeyDatas = tableMetaDataSource.getImportedForeignKeys();
                for (int i = 0; i < foreignKeyDatas.length; i++) {
                    MetaDataSource.ForeignKeyData foreignKeyData = foreignKeyDatas[i];
                    String[] foreignKeyColumnNames = foreignKeyData.getThisColumnNames();

                    for (int j = 0; j < foreignKeyColumnNames.length; j++) {
                        String foreignKeyColumnName = foreignKeyColumnNames[j];
                        TableColumn column = (TableColumn) columnsByName.get(foreignKeyColumnName);
                        column.setForeignKey(true);
                    }
                }
                if (log.isDebug())log.debug("Finding imported foreign keys for: " + catalogSchemaTable + " took: " + (System.currentTimeMillis() - startTime) + " millis");
            } catch (Exception e) {
                log.error("Cannot determine imported foreign keys for table: " + catalogSchemaTable + " in dataSourceName: " + dataSourceName, e);
                throw new RuntimeException(e);
            }
        }
    }

    public ForeignKey[] getImportedForeignKeys() {
        findImportedForeignKeys();
        return foreignKeys;
    }

    private synchronized void findImportedForeignKeys() {
        if (foreignKeys != null) return;
        if (log.isDebug()) log.debug("Finding foreignKeys for: " + catalogSchemaTable + " in dataSourceName: " + dataSourceName);
        long startTime = System.currentTimeMillis();

        try {
            MetaDataSource.CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = MetaDataSource.getInstance(dataSourceName).getCatalogSchemaMetaDataSource(catalogSchemaTable.getCatalogSchema());
            MetaDataSource.TableMetaDataSource tableMetaDataSource = catalogSchemaMetaDataSource.getTableMetaDataSource(catalogSchemaTable.getTableName());
            MetaDataSource.ForeignKeyData[] foreignKeyDatas = tableMetaDataSource.getImportedForeignKeys();
            foreignKeys = new ForeignKey[foreignKeyDatas.length];
            for (int i = 0; i < foreignKeyDatas.length; i++) {
                MetaDataSource.ForeignKeyData foreignKeyData = foreignKeyDatas[i];
                CatalogSchemaTable otherCatalogSchemaTable = CatalogSchemaTable.getInstance(foreignKeyData.getOtherCatalogName(), foreignKeyData.getOtherSchemaName(), foreignKeyData.getOtherTableName(), null);
                ForeignKey foreignKey = new ForeignKey(foreignKeyData.getName(), catalogSchemaTable, otherCatalogSchemaTable);
                foreignKeys[i] = foreignKey;
                addColumns(foreignKey, foreignKeyData);
            }

            if (log.isDebug())log.debug("Finding imported foreign keys for: " + catalogSchemaTable + " took: " + (System.currentTimeMillis() - startTime) + " millis");
        } catch (Exception e) {
            log.error("Cannot determine imported foreign keys for table: " + catalogSchemaTable + " in dataSourceName: " + dataSourceName);
            foreignKeys = EMPTY_FOREIGN_KEY_ARRAY;
        }
    }

    private void addColumns(ForeignKey foreignKey, MetaDataSource.ForeignKeyData foreignKeyData) {
        String[] thisColumnNames = foreignKeyData.getThisColumnNames();
        String[] otherColumnNames = foreignKeyData.getOtherColumnNames();
        for (int j = 0; j < thisColumnNames.length; j++) {
            String thisColumnName = thisColumnNames[j];
            String otherColumnName = otherColumnNames[j];
            foreignKey.addForeignKeyColumn(thisColumnName, otherColumnName, dataSourceName);
        }
    }

    public ForeignKey[] getExportedForeignKeys() {
        findExportedForeignKeys();
        return exportedForeignKeys;
    }

    private synchronized void findExportedForeignKeys() {
        if (exportedForeignKeys != null) return;
        if (log.isDebug()) log.debug("Finding exported foreignKeys for: " + catalogSchemaTable + " in dataSourceName: " + dataSourceName);
        long startTime = System.currentTimeMillis();

        try {
            MetaDataSource.CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = MetaDataSource.getInstance(dataSourceName).getCatalogSchemaMetaDataSource(catalogSchemaTable.getCatalogSchema());
            MetaDataSource.TableMetaDataSource tableMetaDataSource = catalogSchemaMetaDataSource.getTableMetaDataSource(catalogSchemaTable.getTableName());
            MetaDataSource.ForeignKeyData[] foreignKeyDatas = tableMetaDataSource.getExportedForeignKeys();
            exportedForeignKeys = new ForeignKey[foreignKeyDatas.length];
            for (int i = 0; i < foreignKeyDatas.length; i++) {
                MetaDataSource.ForeignKeyData foreignKeyData = foreignKeyDatas[i];
                CatalogSchemaTable otherCatalogSchemaTable = CatalogSchemaTable.getInstance(foreignKeyData.getOtherCatalogName(), foreignKeyData.getOtherSchemaName(), foreignKeyData.getOtherTableName(), null);
                ForeignKey foreignKey = new ForeignKey(foreignKeyData.getName(), catalogSchemaTable, otherCatalogSchemaTable);
                exportedForeignKeys[i] = foreignKey;
                addColumns(foreignKey, foreignKeyData);
            }

            if (log.isDebug())log.debug("Finding exported foreign keys for: " + catalogSchemaTable + " took: " + (System.currentTimeMillis() - startTime) + " millis");
        } catch (Exception e) {
            log.error("Cannot determine exported foreign keys for table: " + catalogSchemaTable + " in dataSourceName: " + dataSourceName);
            exportedForeignKeys = EMPTY_FOREIGN_KEY_ARRAY;
        }
    }

    /**
     * Like getColumns, except it only returns those columns which have matching property names in type.
     */
    public synchronized TableColumn[] getColumns(Class type) {
        TableColumn[] columnsWithMatchingProperties = (TableColumn[]) columnsWithMatchingPropertiesByType.get(type);

        if (columnsWithMatchingProperties == null) {
            if (log.isDebug()) log.debug("Finding columns for: " + catalogSchemaTable + " in dataSourceName: " + dataSourceName + " matching type: " + type.getName());
            TableColumn[] columns = getColumns();

            Set columnsWithMatchingPropertiesList = new HashSet(columns.length);
            Schema schema = Schema.getInstance(type);
            for (int i = 0; i < columns.length; i++) {
                TableColumn column = columns[i];
                Property[] properties = schema.getPropertiesByColumn(column);
                if (properties != null && properties.length > 0) {
                    columnsWithMatchingPropertiesList.add(column);
                }
            }
            columnsWithMatchingProperties = (TableColumn[]) columnsWithMatchingPropertiesList.toArray(new TableColumn[columnsWithMatchingPropertiesList.size()]);
            columnsWithMatchingPropertiesByType.put(type, columnsWithMatchingProperties);
        }

        return columnsWithMatchingProperties;
    }

    public String toString() {
        return catalogSchemaTable.toString() + "@" + dataSourceName;
    }

    public String createReport() throws SQLException {
        StringBuffer reportBuffer = new StringBuffer(5000);

        reportBuffer.append("View: ").append(catalogSchemaTable).append('\n');

        reportBuffer.append("Primary Key/Unique Index: ");
        TableColumn[] pkColumns = getPrimaryKey();
        for (int i = 0; i < pkColumns.length; i++) {
            if (i > 0) reportBuffer.append(", ");
            TableColumn pkColumn = pkColumns[i];
            reportBuffer.append(pkColumn.getName());
        }
        reportBuffer.append('\n');

        ForeignKey[] importedKeys = getImportedForeignKeys();
        reportBuffer.append("Imported foreign keys (").append(importedKeys.length).append("):").append('\n');
        for (int i = 0; i < importedKeys.length; i++) {
            ForeignKey importedKey = importedKeys[i];
            reportBuffer.append("    ").append(importedKey.createReport()).append('\n');
        }

        return reportBuffer.toString();
    }
}