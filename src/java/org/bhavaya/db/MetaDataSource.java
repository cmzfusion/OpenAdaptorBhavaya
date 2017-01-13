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

import org.bhavaya.util.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.15.6.1 $
 */
public abstract class MetaDataSource {
    private static final Log log = Log.getCategory(MetaDataSource.class);
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final ColumnData[] EMPTY_COLUMN_DATA_ARRAY = new ColumnData[0];
    private static final IndexData[] EMPTY_INDEX_DATA_ARRAY = new IndexData[0];
    private static final ForeignKeyData[] EMPTY_FOREIGN_KEY_DATA_ARRAY = new ForeignKeyData[0];

    private static Map instances = new HashMap();
    private static Map xmlMetaDataSourcesByDataSourceName;
    private static String version;

    private String dataSourceName;

    static {
        BeanUtilities.addPersistenceDelegate(IndexData.class, new BhavayaPersistenceDelegate(new String[]{"name", "unique", "columns"}));
        BeanUtilities.addPersistenceDelegate(ColumnData.class, new BhavayaPersistenceDelegate(new String[]{"name", "jdbcType", "decimalDigits", "columnSize"}));
        BeanUtilities.addPersistenceDelegate(ForeignKeyData.class, new BhavayaPersistenceDelegate(new String[]{"name", "otherCatalogName", "otherSchemaName", "otherTableName", "otherColumnNames", "thisColumnNames"}));
        BeanUtilities.addPersistenceDelegate(XmlMetaDataSource.class, new BhavayaPersistenceDelegate(new String[]{"dataSourceName", "catalogSupported", "schemaSupported", "nativeColumnTypes", "validColumnNamesBySqlString", "catalogSchemaMetaDataSources"}));
        BeanUtilities.addPersistenceDelegate(XmlCatalogSchemaMetaDataSource.class, new BhavayaPersistenceDelegate(new String[]{"tableNames", "tableMetaDataSources"}));
        BeanUtilities.addPersistenceDelegate(XmlTableMetaDataSource.class, new BhavayaPersistenceDelegate(new String[]{"primaryKeyColumnNames", "indexDatas", "columnDatas", "importedForeignKeys", "exportedForeignKeys"}));
        BeanUtilities.addPersistenceDelegate(JdbcDelegateTableMetaDataSource.class, new BhavayaPersistenceDelegate(new String[]{"dataSourceName", "catalogSchemaTable"}));
    }

    protected MetaDataSource(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    protected static boolean hasBeenRequested(String dataSourceName) {
        dataSourceName = DataSourceFactory.getDatabaseMetadataDatasource(dataSourceName);
        return instances.containsKey(dataSourceName);
    }

    public static synchronized MetaDataSource getInstance(String dataSourceName) {
        dataSourceName = DataSourceFactory.getDatabaseMetadataDatasource(dataSourceName);

        MetaDataSource instance = (MetaDataSource) instances.get(dataSourceName);
        if (instance == null) {
            MetaDataSource underlyingMetaDataSource = new JdbcMetaDataSource(dataSourceName);
            XmlMetaDataSource xmlMetaDataSource = getXmlMetaDataSource(dataSourceName);
            if (xmlMetaDataSource != null) {
                xmlMetaDataSource.setUnderlyingMetaDataSource(underlyingMetaDataSource);
                instance = xmlMetaDataSource;
            } else {
                instance = underlyingMetaDataSource;
            }
            instances.put(dataSourceName, instance);
        }
        return instance;
    }

    private static synchronized XmlMetaDataSource getXmlMetaDataSource(String dataSourceName) {
        if (xmlMetaDataSourcesByDataSourceName == null) {
            xmlMetaDataSourcesByDataSourceName = new HashMap();
            XmlMetaDataSource[] xmlMetaDataSources = readMetaDataFromFile();
            if (xmlMetaDataSources != null) {
                for (int i = 0; i < xmlMetaDataSources.length; i++) {
                    XmlMetaDataSource xmlMetaDataSource = xmlMetaDataSources[i];
                    xmlMetaDataSourcesByDataSourceName.put(xmlMetaDataSource.getDataSourceName(), xmlMetaDataSource);
                }
            }
        }
        return (XmlMetaDataSource) xmlMetaDataSourcesByDataSourceName.get(dataSourceName);
    }

    public static String getVersion() {
        return version;
    }

    public static void setVersion(String version) {
        MetaDataSource.version = version;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public static boolean isUsingXmlMetaDataSources() {
        return xmlMetaDataSourcesByDataSourceName != null && xmlMetaDataSourcesByDataSourceName.size() > 0;
    }

    public abstract CatalogSchemaMetaDataSource getCatalogSchemaMetaDataSource(CatalogSchema catalogSchema);

    protected abstract CatalogSchema[] getRequestedCatalogSchemas();

    public abstract boolean isCatalogSupported() throws Exception;

    public abstract boolean isSchemaSupported() throws Exception;

    public abstract Map getNativeColumnTypes() throws Exception;

    public abstract String[] getValidColumnNames(String sqlString) throws Exception;

    protected abstract Map getValidColumnNamesBySqlString();

    public static abstract class CatalogSchemaMetaDataSource {
        public abstract TableMetaDataSource getTableMetaDataSource(String tableName);

        public abstract String[] getTableNames() throws Exception;

        protected abstract boolean hasBeenRequested(String tableName);
    }

    public static abstract class TableMetaDataSource {
        public abstract String[] getPrimaryKeyColumnNames() throws Exception;

        public abstract IndexData[] getIndexDatas() throws Exception;

        public abstract ColumnData[] getColumnDatas() throws Exception;

        public abstract ForeignKeyData[] getImportedForeignKeys() throws Exception;

        public abstract ForeignKeyData[] getExportedForeignKeys() throws Exception;
    }

    public static class IndexData {
        private String name;
        private boolean unique;
        private String[] columns;

        public IndexData(String name, boolean unique) {
            this(name, unique, null);
        }

        public IndexData(String name, boolean unique, String[] columns) {
            this.name = name;
            this.unique = unique;
            this.columns = columns;
        }

        public String getName() {
            return name;
        }

        public boolean isUnique() {
            return unique;
        }

        public String[] getColumns() {
            return columns;
        }

        public void setColumns(String[] columns) {
            this.columns = columns;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IndexData)) return false;

            final IndexData indexData = (IndexData) o;

            if (unique != indexData.unique) return false;
            if (!Arrays.equals(columns, indexData.columns)) return false;
            if (name != null ? !name.equals(indexData.name) : indexData.name != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (name != null ? name.hashCode() : 0);
            result = 29 * result + (unique ? 1 : 0);
            for (int i = 0; i < columns.length; i++) {
                Object o = columns[i];
                result = 31 * result + (o == null ? 0 : o.hashCode());
            }
            return result;
        }
    }

    public static class ColumnData {
        private String name;
        private int jdbcType;
        private int decimalDigits;
        private int columnSize;

        public ColumnData(String name, int jdbcType, int decimalDigits, int columnSize) {
            this.name = name;
            this.jdbcType = jdbcType;
            this.decimalDigits = decimalDigits;
            this.columnSize = columnSize;
        }

        public String getName() {
            return name;
        }

        public int getJdbcType() {
            return jdbcType;
        }

        public int getDecimalDigits() {
            return decimalDigits;
        }

        public int getColumnSize() {
            return columnSize;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ColumnData)) return false;

            final ColumnData columnData = (ColumnData) o;

            if (columnSize != columnData.columnSize) return false;
            if (decimalDigits != columnData.decimalDigits) return false;
            if (jdbcType != columnData.jdbcType) return false;
            if (name != null ? !name.equals(columnData.name) : columnData.name != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (name != null ? name.hashCode() : 0);
            result = 29 * result + jdbcType;
            result = 29 * result + decimalDigits;
            result = 29 * result + columnSize;
            return result;
        }
    }

    public static class ForeignKeyData {
        private String name;
        private String otherCatalogName;
        private String otherSchemaName;
        private String otherTableName;
        private String[] otherColumnNames;
        private String[] thisColumnNames;

        public ForeignKeyData(String name, String otherCatalogName, String otherSchemaName, String otherTableName) {
            this(name, otherCatalogName, otherSchemaName, otherTableName, null, null);
        }

        public ForeignKeyData(String name, String otherCatalogName, String otherSchemaName, String otherTableName, String[] otherColumnNames, String[] thisColumnNames) {
            this.name = name;
            this.otherCatalogName = otherCatalogName;
            this.otherSchemaName = otherSchemaName;
            this.otherTableName = otherTableName;
            this.otherColumnNames = otherColumnNames;
            this.thisColumnNames = thisColumnNames;
        }

        public String getName() {
            return name;
        }

        public String getOtherCatalogName() {
            return otherCatalogName;
        }

        public String getOtherSchemaName() {
            return otherSchemaName;
        }

        public String getOtherTableName() {
            return otherTableName;
        }

        public String[] getOtherColumnNames() {
            return otherColumnNames;
        }

        public void setOtherColumnNames(String[] otherColumnNames) {
            this.otherColumnNames = otherColumnNames;
        }

        public String[] getThisColumnNames() {
            return thisColumnNames;
        }

        public void setThisColumnNames(String[] thisColumnNames) {
            this.thisColumnNames = thisColumnNames;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ForeignKeyData)) return false;

            final ForeignKeyData foreignKeyData = (ForeignKeyData) o;

            if (name != null ? !name.equals(foreignKeyData.name) : foreignKeyData.name != null) return false;
            if (otherCatalogName != null ? !otherCatalogName.equals(foreignKeyData.otherCatalogName) : foreignKeyData.otherCatalogName != null) return false;
            if (!Arrays.equals(otherColumnNames, foreignKeyData.otherColumnNames)) return false;
            if (otherSchemaName != null ? !otherSchemaName.equals(foreignKeyData.otherSchemaName) : foreignKeyData.otherSchemaName != null) return false;
            if (otherTableName != null ? !otherTableName.equals(foreignKeyData.otherTableName) : foreignKeyData.otherTableName != null) return false;
            if (!Arrays.equals(thisColumnNames, foreignKeyData.thisColumnNames)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (name != null ? name.hashCode() : 0);
            result = 29 * result + (otherCatalogName != null ? otherCatalogName.hashCode() : 0);
            result = 29 * result + (otherSchemaName != null ? otherSchemaName.hashCode() : 0);
            result = 29 * result + (otherTableName != null ? otherTableName.hashCode() : 0);
            for (int i = 0; i < otherColumnNames.length; i++) {
                Object o = otherColumnNames[i];
                result = 31 * result + (o == null ? 0 : o.hashCode());
            }
            for (int i = 0; i < thisColumnNames.length; i++) {
                Object o = thisColumnNames[i];
                result = 31 * result + (o == null ? 0 : o.hashCode());
            }

            return result;
        }
    }

    public static class XmlMetaDataSource extends MetaDataSource {
        private MetaDataSource underlyingMetaDataSource;
        private boolean catalogSupported;
        private boolean schemaSupported;
        private Map nativeColumnTypes;
        private Map validColumnNamesBySqlString;
        private Map catalogSchemaMetaDataSources;

        public XmlMetaDataSource(String dataSourceName) {
            super(dataSourceName);
        }

        public XmlMetaDataSource(String dataSourceName, boolean catalogSupported, boolean schemaSupported, Map nativeColumnTypes, Map validColumnNamesBySqlString, Map catalogSchemaMetaDataSources) {
            super(dataSourceName);
            this.catalogSupported = catalogSupported;
            this.schemaSupported = schemaSupported;
            this.nativeColumnTypes = nativeColumnTypes;
            this.validColumnNamesBySqlString = validColumnNamesBySqlString;
            this.catalogSchemaMetaDataSources = catalogSchemaMetaDataSources;
        }

        public void setUnderlyingMetaDataSource(MetaDataSource underlyingMetaDataSource) {
            this.underlyingMetaDataSource = underlyingMetaDataSource;
        }

        protected CatalogSchema[] getRequestedCatalogSchemas() {
            Set catalogSchemaSet = catalogSchemaMetaDataSources.keySet();
            return (CatalogSchema[]) catalogSchemaSet.toArray(new CatalogSchema[catalogSchemaSet.size()]);
        }

        public CatalogSchemaMetaDataSource getCatalogSchemaMetaDataSource(CatalogSchema catalogSchema) {
            if (catalogSchema == null) return null;
            CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = (CatalogSchemaMetaDataSource) catalogSchemaMetaDataSources.get(catalogSchema);
            if (catalogSchemaMetaDataSource == null) catalogSchemaMetaDataSource = underlyingMetaDataSource.getCatalogSchemaMetaDataSource(catalogSchema);
            return catalogSchemaMetaDataSource;
        }

        public boolean isCatalogSupported() throws Exception {
            return catalogSupported;
        }

        public boolean isSchemaSupported() throws Exception {
            return schemaSupported;
        }

        public Map getNativeColumnTypes() throws Exception {
            return nativeColumnTypes;
        }

        //
        // For persistence delegate
        //
        public Map getValidColumnNamesBySqlString() {
            return validColumnNamesBySqlString;
        }

        public String[] getValidColumnNames(String sqlString) throws Exception {
            if (sqlString == null) return EMPTY_STRING_ARRAY;
            if (validColumnNamesBySqlString == null) return EMPTY_STRING_ARRAY;
            String[] validColumnNames = (String[]) validColumnNamesBySqlString.get(sqlString);
            if (validColumnNames == null) return EMPTY_STRING_ARRAY;
            return validColumnNames;
        }

        //
        // For persistence delegate
        //
        public Map getCatalogSchemaMetaDataSources() {
            return catalogSchemaMetaDataSources;
        }
    }


    public static class XmlCatalogSchemaMetaDataSource extends CatalogSchemaMetaDataSource {
        private String[] tableNames;
        private Map tableMetaDataSources;

        public XmlCatalogSchemaMetaDataSource(String[] tableNames, Map tableMetaDataSources) {
            this.tableNames = tableNames;
            this.tableMetaDataSources = tableMetaDataSources;
        }

        public TableMetaDataSource getTableMetaDataSource(String tableName) {
            if (tableName == null) return null;
            return (TableMetaDataSource) tableMetaDataSources.get(tableName);
        }

        public String[] getTableNames() throws Exception {
            if (tableNames == null) return EMPTY_STRING_ARRAY;
            return tableNames;
        }

        protected boolean hasBeenRequested(String tableName) {
            return tableMetaDataSources.containsKey(tableName);
        }

        //
        // For persistence delegate
        //
        public Map getTableMetaDataSources() {
            return tableMetaDataSources;
        }
    }

    public static class XmlTableMetaDataSource extends TableMetaDataSource {
        private String[] primaryKeyColumnNames;
        private IndexData[] indexDatas;
        private ColumnData[] columnDatas;
        private ForeignKeyData[] importedForeignKeyDatas;
        private ForeignKeyData[] exportedForeignKeyDatas;

        public XmlTableMetaDataSource(String[] primaryKeyColumnNames,
                                      IndexData[] indexDatas,
                                      ColumnData[] columnDatas,
                                      ForeignKeyData[] importedForeignKeyDatas,
                                      ForeignKeyData[] exportedForeignKeyDatas) {
            this.primaryKeyColumnNames = primaryKeyColumnNames;
            this.indexDatas = indexDatas;
            this.columnDatas = columnDatas;
            this.importedForeignKeyDatas = importedForeignKeyDatas;
            this.exportedForeignKeyDatas = exportedForeignKeyDatas;
        }

        public XmlTableMetaDataSource(String[] primaryKeyColumnNames,
                                      IndexData[] indexDatas,
                                      ColumnData[] columnDatas,
                                      ForeignKeyData[] importedForeignKeyDatas) {
            this.primaryKeyColumnNames = primaryKeyColumnNames;
            this.indexDatas = indexDatas;
            this.columnDatas = columnDatas;
            this.importedForeignKeyDatas = importedForeignKeyDatas;
            this.exportedForeignKeyDatas = null;
        }

        public String[] getPrimaryKeyColumnNames() throws Exception {
            if (primaryKeyColumnNames == null) return EMPTY_STRING_ARRAY;
            return primaryKeyColumnNames;
        }

        public IndexData[] getIndexDatas() throws Exception {
            if (indexDatas == null) return EMPTY_INDEX_DATA_ARRAY;
            return indexDatas;
        }

        public ColumnData[] getColumnDatas() throws Exception {
            if (columnDatas == null) return EMPTY_COLUMN_DATA_ARRAY;
            return columnDatas;
        }

        public ForeignKeyData[] getImportedForeignKeys() throws Exception {
            if (importedForeignKeyDatas == null) return EMPTY_FOREIGN_KEY_DATA_ARRAY;
            return importedForeignKeyDatas;
        }

        public ForeignKeyData[] getExportedForeignKeys() throws Exception {
            if (exportedForeignKeyDatas == null) return EMPTY_FOREIGN_KEY_DATA_ARRAY;
            return exportedForeignKeyDatas;
        }
    }

    public static class JdbcDelegateTableMetaDataSource extends TableMetaDataSource {
        private TableMetaDataSource delegate;
        private String dataSourceName;
        private CatalogSchemaTable catalogSchemaTable;

        public JdbcDelegateTableMetaDataSource(String dataSourceName, CatalogSchemaTable catalogSchemaTable) {
            this.dataSourceName = dataSourceName;
            this.catalogSchemaTable = catalogSchemaTable;
            delegate = new JdbcTableMetaDataSource(dataSourceName, catalogSchemaTable);
        }

        public String[] getPrimaryKeyColumnNames() throws Exception {
            return delegate.getPrimaryKeyColumnNames();
        }

        public IndexData[] getIndexDatas() throws Exception {
            return delegate.getIndexDatas();
        }

        public ColumnData[] getColumnDatas() throws Exception {
            return delegate.getColumnDatas();
        }

        public ForeignKeyData[] getImportedForeignKeys() throws Exception {
            return delegate.getImportedForeignKeys();
        }

        public ForeignKeyData[] getExportedForeignKeys() throws Exception {
            return delegate.getExportedForeignKeys();
        }

        //
        // For persistence delegate
        //
        public String getDataSourceName() {
            return dataSourceName;
        }

        //
        // For persistence delegate
        //
        public CatalogSchemaTable getCatalogSchemaTable() {
            return catalogSchemaTable;
        }
    }

    public static class JdbcMetaDataSource extends MetaDataSource {
        private Boolean catalogSupported;
        private Boolean schemaSupported;
        private Map nativeColumnTypes;
        private Map validColumnNamesBySqlString;
        private Map catalogSchemaMetaDataSources;

        public JdbcMetaDataSource(String dataSourceName) {
            super(dataSourceName);
            catalogSchemaMetaDataSources = new HashMap();
            validColumnNamesBySqlString = new HashMap();
        }

        protected CatalogSchema[] getRequestedCatalogSchemas() {
            Set catalogSchemaSet = catalogSchemaMetaDataSources.keySet();
            return (CatalogSchema[]) catalogSchemaSet.toArray(new CatalogSchema[catalogSchemaSet.size()]);
        }

        public CatalogSchemaMetaDataSource getCatalogSchemaMetaDataSource(CatalogSchema catalogSchema) {
            CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = (CatalogSchemaMetaDataSource) catalogSchemaMetaDataSources.get(catalogSchema);
            if (catalogSchemaMetaDataSource == null) {
                catalogSchemaMetaDataSource = new JdbcCatalogSchemaMetaDataSource(getDataSourceName(), catalogSchema);
                catalogSchemaMetaDataSources.put(catalogSchema, catalogSchemaMetaDataSource);
            }
            return catalogSchemaMetaDataSource;
        }

        public boolean isCatalogSupported() throws Exception {
            if (catalogSupported == null) {
                Connection connection = null;
                try {
                    connection = DataSourceFactory.getInstance(getDataSourceName()).getConnection();
                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + getDataSourceName()));
                    }

                    DatabaseMetaData metaData = connection.getMetaData();
                    catalogSupported = new Boolean(metaData.supportsCatalogsInDataManipulation());
                } finally {
                    DBUtilities.close(connection);
                }
            }
            return catalogSupported.booleanValue();
        }

        public boolean isSchemaSupported() throws Exception {
            if (schemaSupported == null) {
                Connection connection = null;
                try {
                    connection = DataSourceFactory.getInstance(getDataSourceName()).getConnection();
                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + getDataSourceName()));
                    }

                    DatabaseMetaData metaData = connection.getMetaData();
                    schemaSupported = new Boolean(metaData.supportsSchemasInDataManipulation());
                } finally {
                    DBUtilities.close(connection);
                }
            }
            return schemaSupported.booleanValue();
        }

        public Map getNativeColumnTypes() throws Exception {
            if (nativeColumnTypes == null) {
                nativeColumnTypes = new HashMap();

                Connection connection = null;
                ResultSet resultSet = null;
                try {
                    connection = DataSourceFactory.getInstance(getDataSourceName()).getConnection();
                    DatabaseMetaData metaData = connection.getMetaData();
                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + getDataSourceName()));
                    }

                    resultSet = metaData.getTypeInfo();
                    while (resultSet.next()) {
                        int jdbcType = resultSet.getInt("DATA_TYPE");
                        String nativeType = resultSet.getString("LOCAL_TYPE_NAME");
                        Integer key = new Integer(jdbcType);
                        if (!nativeColumnTypes.containsKey(key)) nativeColumnTypes.put(key, nativeType);
                    }
                } finally {
                    DBUtilities.closeResultSetAndStatement(resultSet, null);
                    DBUtilities.close(connection);
                }
            }
            return nativeColumnTypes;
        }

        public Map getValidColumnNamesBySqlString() {
            return validColumnNamesBySqlString;
        }

        public String[] getValidColumnNames(String sqlString) throws Exception {
            if (sqlString == null) return EMPTY_STRING_ARRAY;
            String[] validColumnNames = (String[]) validColumnNamesBySqlString.get(sqlString);
            if (validColumnNames != null) return validColumnNames;

            SQL sql = new SQL(ApplicationProperties.substituteApplicationProperties(sqlString), getDataSourceName());

            PreparedStatement statement = null;
            ResultSet emptyResultSet = null;
            Connection connection = null;

            try {
                long startTime = System.currentTimeMillis();
                SQL returnNothingSql = sql.joinWhereClause("1=2");
                connection = DataSourceFactory.getInstance(getDataSourceName()).getConnection();
                if (log.isDebug()) log.debug("Executing: " + returnNothingSql.getStatementString());
                statement = connection.prepareStatement(returnNothingSql.toString());
                emptyResultSet = statement.executeQuery();
                ResultSetMetaData resultSetMetaData = emptyResultSet.getMetaData();
                int columnCount = resultSetMetaData.getColumnCount();
                validColumnNames = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    validColumnNames[i - 1] = resultSetMetaData.getColumnName(i);
                }
                validColumnNamesBySqlString.put(sqlString, validColumnNames);
                if (log.isDebug()) log.debug("Sql took: " + (System.currentTimeMillis() - startTime) + " millis");
                return validColumnNames;
            } finally {
                DBUtilities.closeResultSetAndStatement(emptyResultSet, statement);
                DBUtilities.close(connection);
                if (validColumnNames == null) validColumnNamesBySqlString.put(sqlString, EMPTY_STRING_ARRAY);
            }
        }
    }

    public static class JdbcCatalogSchemaMetaDataSource extends CatalogSchemaMetaDataSource {
        private String dataSourceName;
        private CatalogSchema catalogSchema;
        private String[] tableNames;
        private Map tableMetaDataSources;

        public JdbcCatalogSchemaMetaDataSource(String dataSourceName, CatalogSchema catalogSchema) {
            this.dataSourceName = dataSourceName;
            this.catalogSchema = catalogSchema;
            tableMetaDataSources = new HashMap();
        }

        public TableMetaDataSource getTableMetaDataSource(String tableName) {
            TableMetaDataSource tableMetaDataSource = (TableMetaDataSource) tableMetaDataSources.get(tableName);
            if (tableMetaDataSource == null) {
                tableMetaDataSource = new JdbcTableMetaDataSource(dataSourceName, CatalogSchemaTable.getInstance(catalogSchema, tableName, null));
                tableMetaDataSources.put(tableName, tableMetaDataSource);
            }
            return tableMetaDataSource;
        }

        public String[] getTableNames() throws Exception {
            if (tableNames == null) {
                Connection connection = null;
                ResultSet rs = null;
                try {
                    connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
                    DatabaseMetaData metaData = connection.getMetaData();
                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + dataSourceName));
                        if (metaData.supportsCatalogsInDataManipulation()) connection.setCatalog(catalogSchema.getCatalogName()); // fix for opta2000.jar, need to set catalog name first
                        rs = metaData.getTables(catalogSchema.getCatalogName(), catalogSchema.getSchemaName(), null, new String[]{"TABLE", "VIEW"});
                    }

                    Set tableNameList = new LinkedHashSet();

                    while (rs.next()) {
                        String tableName = rs.getString(3);
                        tableNameList.add(tableName);
                    }

                    tableNames = (String[]) tableNameList.toArray(new String[tableNameList.size()]);
                } finally {
                    DBUtilities.closeResultSetAndStatement(rs, null);
                    DBUtilities.close(connection);
                    if (tableNames == null) tableNames = EMPTY_STRING_ARRAY;
                }
            }
            return tableNames;
        }

        protected boolean hasBeenRequested(String tableName) {
            return tableMetaDataSources.containsKey(tableName);
        }
    }

    public static class JdbcTableMetaDataSource extends TableMetaDataSource {
        private String dataSourceName;
        private CatalogSchemaTable catalogSchemaTable;
        private String[] primaryKeyColumnNames;
        private IndexData[] indexDatas;
        private ColumnData[] columnDatas;
        private ForeignKeyData[] importedForeignKeyDatas;
        private ForeignKeyData[] exportedForeignKeyDatas;

        public JdbcTableMetaDataSource(String dataSourceName, CatalogSchemaTable catalogSchemaTable) {
            this.dataSourceName = dataSourceName;
            this.catalogSchemaTable = catalogSchemaTable;
        }

        public String[] getPrimaryKeyColumnNames() throws Exception {
            if (primaryKeyColumnNames == null) {
                Connection connection = null;
                ResultSet resultSet = null;
                try {
                    connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
                    DatabaseMetaData metaData = connection.getMetaData();
                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + dataSourceName));
                        if (metaData.supportsCatalogsInDataManipulation()) connection.setCatalog(catalogSchemaTable.getCatalogSchema().getCatalogName()); // fix for opta2000.jar, need to set catalog name first
                        resultSet = metaData.getPrimaryKeys(catalogSchemaTable.getCatalogSchema().getCatalogName(),
                                catalogSchemaTable.getCatalogSchema().getSchemaName(),
                                catalogSchemaTable.getTableName());
                    }

                    Set primaryKeyColumnNameSet = new LinkedHashSet();
                    while (resultSet.next()) {
                        String primaryKeyColumnName = resultSet.getString(4);
                        primaryKeyColumnNameSet.add(primaryKeyColumnName);
                    }

                    primaryKeyColumnNames = (String[]) primaryKeyColumnNameSet.toArray(new String[primaryKeyColumnNameSet.size()]);
                } finally {
                    DBUtilities.closeResultSetAndStatement(resultSet, null);
                    DBUtilities.close(connection);
                    if (primaryKeyColumnNames == null) primaryKeyColumnNames = EMPTY_STRING_ARRAY;
                }
            }
            return primaryKeyColumnNames;
        }

        public IndexData[] getIndexDatas() throws Exception {
            if (indexDatas == null) {
                Connection connection = null;
                ResultSet resultSet = null;

                try {
                    connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
                    DatabaseMetaData metaData = connection.getMetaData();

                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + dataSourceName));
                        if (metaData.supportsCatalogsInDataManipulation()) connection.setCatalog(catalogSchemaTable.getCatalogSchema().getCatalogName()); // fix for opta2000.jar, need to set catalog name first
                        resultSet = metaData.getIndexInfo(catalogSchemaTable.getCatalogSchema().getCatalogName(),
                                catalogSchemaTable.getCatalogSchema().getSchemaName(),
                                catalogSchemaTable.getTableName(),
                                true, true);
                    }

                    Map indexColumnsByName = new HashMap();
                    Map indexDataByName = new HashMap();

                    while (resultSet.next()) {
                        int indexType = resultSet.getInt(7);
                        if (indexType != DatabaseMetaData.tableIndexStatistic) {
                            String indexName = resultSet.getString(6);
                            if (indexName != null) indexName = indexName.trim();

                            IndexData indexData = (IndexData) indexDataByName.get(indexName);
                            if (indexData == null) {
                                boolean unique = !resultSet.getBoolean(4);
                                indexData = new IndexData(indexName, unique);
                                indexDataByName.put(indexName, indexData);
                            }

                            String columnName = resultSet.getString(9);
                            addElementToMapOfLists(indexColumnsByName, indexName, columnName);
                        }
                    }

                    for (Iterator iterator = indexColumnsByName.entrySet().iterator(); iterator.hasNext();) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        String indexName = (String) entry.getKey();
                        List columnList = (List) entry.getValue();
                        String[] columns = (String[]) columnList.toArray(new String[columnList.size()]);
                        IndexData indexData = (IndexData) indexDataByName.get(indexName);
                        indexData.setColumns(columns);
                    }

                    indexDatas = (IndexData[]) indexDataByName.values().toArray(new IndexData[indexDataByName.values().size()]);
                } finally {
                    DBUtilities.closeResultSetAndStatement(resultSet, null);
                    DBUtilities.close(connection);
                    if (indexDatas == null) indexDatas = EMPTY_INDEX_DATA_ARRAY;
                }
            }
            return indexDatas;
        }

        public ColumnData[] getColumnDatas() throws Exception {
            if (columnDatas == null) {
                Connection connection = null;
                ResultSet rs = null;

                try {
                    connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
                    DatabaseMetaData metaData = connection.getMetaData();

                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + dataSourceName));
                        if (metaData.supportsCatalogsInDataManipulation()) connection.setCatalog(catalogSchemaTable.getCatalogSchema().getCatalogName()); // fix for opta2000.jar, need to set catalog name first
                        rs = metaData.getColumns(catalogSchemaTable.getCatalogSchema().getCatalogName(),
                                catalogSchemaTable.getCatalogSchema().getSchemaName(),
                                catalogSchemaTable.getTableName(), null);
                    }

                    List columns = new ArrayList();
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        int jdbcType = rs.getInt("DATA_TYPE");
                        int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                        int columnSize = rs.getInt("COLUMN_SIZE");
                        ColumnData columnData = new ColumnData(columnName, jdbcType, decimalDigits, columnSize);
                        columns.add(columnData);
                    }

                    columnDatas = (ColumnData[]) columns.toArray(new ColumnData[columns.size()]);
                } finally {
                    DBUtilities.closeResultSetAndStatement(rs, null);
                    DBUtilities.close(connection);
                    if (columnDatas == null) columnDatas = EMPTY_COLUMN_DATA_ARRAY;
                }
            }
            return columnDatas;
        }

        public ForeignKeyData[] getImportedForeignKeys() throws Exception {
            if (importedForeignKeyDatas == null) {
                Connection connection = null;
                ResultSet rs = null;
                try {
                    connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
                    DatabaseMetaData metaData = connection.getMetaData();
                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + dataSourceName));
                        if (metaData.supportsCatalogsInDataManipulation()) connection.setCatalog(catalogSchemaTable.getCatalogSchema().getCatalogName()); // fix for opta2000.jar, need to set catalog name first
                        rs = metaData.getImportedKeys(catalogSchemaTable.getCatalogSchema().getCatalogName(),
                                catalogSchemaTable.getCatalogSchema().getSchemaName(),
                                catalogSchemaTable.getTableName());
                    }
                    importedForeignKeyDatas = getForeignKeysFromResultSet(rs, true);
                } finally {
                    DBUtilities.closeResultSetAndStatement(rs, null);
                    DBUtilities.close(connection);
                    if (importedForeignKeyDatas == null) importedForeignKeyDatas = EMPTY_FOREIGN_KEY_DATA_ARRAY;
                }
            }
            return importedForeignKeyDatas;
        }

        public ForeignKeyData[] getExportedForeignKeys() throws Exception {
            if (exportedForeignKeyDatas == null) {
                Connection connection = null;
                ResultSet rs = null;
                try {
                    connection = DataSourceFactory.getInstance(dataSourceName).getConnection();
                    DatabaseMetaData metaData = connection.getMetaData();
                    synchronized (connection) {
                        if (!connection.getAutoCommit()) log.error(new Exception("Using transactional connection to get meta-data: " + dataSourceName));
                        if (metaData.supportsCatalogsInDataManipulation()) connection.setCatalog(catalogSchemaTable.getCatalogSchema().getCatalogName()); // fix for opta2000.jar, need to set catalog name first
                        rs = metaData.getExportedKeys(catalogSchemaTable.getCatalogSchema().getCatalogName(),
                                catalogSchemaTable.getCatalogSchema().getSchemaName(),
                                catalogSchemaTable.getTableName());
                    }
                    exportedForeignKeyDatas = getForeignKeysFromResultSet(rs, false);
                } finally {
                    DBUtilities.closeResultSetAndStatement(rs, null);
                    DBUtilities.close(connection);
                    if (exportedForeignKeyDatas == null) exportedForeignKeyDatas = EMPTY_FOREIGN_KEY_DATA_ARRAY;
                }
            }
            return exportedForeignKeyDatas;
        }

        private static ForeignKeyData[] getForeignKeysFromResultSet(ResultSet rs, boolean importedKey) throws SQLException {
            Map foreignKeysById = new LinkedHashMap();
            Map foreignKeyThisColumnsById = new HashMap();
            Map foreignKeyOtherColumnsById = new HashMap();

            while (rs.next()) {
                String otherCatalogName = importedKey ? rs.getString(1) : rs.getString(5);
                String otherSchemaName = importedKey ? rs.getString(2) : rs.getString(6);
                String otherTableName = importedKey ? rs.getString(3) : rs.getString(7);
                String otherColumnName = importedKey ? rs.getString(4) : rs.getString(8);
                String thisColumnName = importedKey ? rs.getString(8) : rs.getString(4);
                String fkName = rs.getString(12);
                assert(fkName != null) : "Foreign key name is null, this prevents grouping foreign key columns";
                assert(otherTableName != null) : "Foreign key other table name is null, this prevents grouping foreign key columns";

                String id = fkName + otherCatalogName + otherSchemaName + otherTableName;
                ForeignKeyData foreignKey = (ForeignKeyData) foreignKeysById.get(id);
                if (foreignKey == null) {
                    foreignKey = new ForeignKeyData(fkName, otherCatalogName, otherSchemaName, otherTableName);
                    foreignKeysById.put(id, foreignKey);
                }

                addElementToMapOfLists(foreignKeyThisColumnsById, id, thisColumnName);
                addElementToMapOfLists(foreignKeyOtherColumnsById, id, otherColumnName);
            }

            for (Iterator iterator = foreignKeyThisColumnsById.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String id = (String) entry.getKey();
                List columnList = (List) entry.getValue();
                String[] columns = (String[]) columnList.toArray(new String[columnList.size()]);
                ForeignKeyData foreignKeyData = (ForeignKeyData) foreignKeysById.get(id);
                foreignKeyData.setThisColumnNames(columns);
            }

            for (Iterator iterator = foreignKeyOtherColumnsById.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String id = (String) entry.getKey();
                List columnList = (List) entry.getValue();
                String[] columns = (String[]) columnList.toArray(new String[columnList.size()]);
                ForeignKeyData foreignKeyData = (ForeignKeyData) foreignKeysById.get(id);
                foreignKeyData.setOtherColumnNames(columns);
            }

            return (ForeignKeyData[]) foreignKeysById.values().toArray(new ForeignKeyData[foreignKeysById.values().size()]);
        }
    }

    private static void addElementToMapOfLists(Map mapOfLists, Object key, Object element) {
        List list = (List) mapOfLists.get(key);
        if (list == null) {
            list = new ArrayList();
            mapOfLists.put(key, list);
        }
        list.add(element);
    }

    public static void main(String[] args) {
        writeMetaDataToFile();
    }

    public static void writeMetaDataToFile() {
        if (isUsingXmlMetaDataSources()) return;

        long startTime = System.currentTimeMillis();
        log.info("Generating metadata");

        String[] dataSourceNames = DataSourceFactory.getDataSourceNames();
        Set scannedDataSourceNames = new HashSet();
        List xmlMetaDataSources = new ArrayList();

        for (int i = 0; i < dataSourceNames.length; i++) {
            String dataSourceName = dataSourceNames[i];
            dataSourceName = DataSourceFactory.getDatabaseMetadataDatasource(dataSourceName);
            if (hasBeenRequested(dataSourceName)) {
                if (!scannedDataSourceNames.contains(dataSourceName)) {
                    scannedDataSourceNames.add(dataSourceName);
                    MetaDataSource metaDataSource = createMetaDataSource(dataSourceName);
                    if (metaDataSource != null) xmlMetaDataSources.add(metaDataSource);
                }
            } else {
                log.info("Ignoring metadata for dataSource: " + dataSourceName + ", it has not yet been requested");
            }
        }

        writeMetaDataToFile((XmlMetaDataSource[]) xmlMetaDataSources.toArray(new XmlMetaDataSource[xmlMetaDataSources.size()]));
        log.info("Generated metadata in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
    }

    private static MetaDataSource createMetaDataSource(String dataSourceName) {
        long startTime = System.currentTimeMillis();
        log.info("Generating metadata for dataSource: " + dataSourceName);

        try {
            MetaDataSource underlyingMetaDataSource = getInstance(dataSourceName);
            assert underlyingMetaDataSource instanceof JdbcMetaDataSource : "Can only create XmlMetaDataSource from JdbcMetaDataSource";

            boolean catalogSupported = underlyingMetaDataSource.isCatalogSupported();
            boolean schemaSupported = underlyingMetaDataSource.isSchemaSupported();
            Map nativeColumnTypes = underlyingMetaDataSource.getNativeColumnTypes();
            Map validColumnNamesBySqlString = underlyingMetaDataSource.getValidColumnNamesBySqlString();
            Map catalogSchemaMetaDataSources = new HashMap();

            CatalogSchema[] catalogSchemas = underlyingMetaDataSource.getRequestedCatalogSchemas(); // do not do all databases on server, as this can take ages, so use this little optimisation
            if (catalogSchemas.length == 0) {
                log.info("Ignoring metadata for dataSource: " + dataSourceName);
                return null;
            }

            for (int j = 0; j < catalogSchemas.length; j++) {
                CatalogSchema catalogSchema = catalogSchemas[j];
                CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = createCatalogSchemaMetaDataSource(catalogSchema, underlyingMetaDataSource);
                catalogSchemaMetaDataSources.put(catalogSchema, catalogSchemaMetaDataSource);
            }

            MetaDataSource metaDataSource = new XmlMetaDataSource(dataSourceName, catalogSupported, schemaSupported, nativeColumnTypes, validColumnNamesBySqlString, catalogSchemaMetaDataSources);
            log.info("Generated metadata for dataSource: " + dataSourceName + " in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
            return metaDataSource;
        } catch (Exception e) {
            log.error("Error generating metadata for dataSource: " + dataSourceName, e);
            return null;
        }
    }

    private static CatalogSchemaMetaDataSource createCatalogSchemaMetaDataSource(CatalogSchema catalogSchema, MetaDataSource underlyingMetaDataSource) {
        long startTime = System.currentTimeMillis();
        log.info("Generating metadata for database: " + catalogSchema);

        try {
            CatalogSchemaMetaDataSource underlyingCatalogSchemaMetaDataSource = underlyingMetaDataSource.getCatalogSchemaMetaDataSource(catalogSchema);
            String[] tableNames = underlyingCatalogSchemaMetaDataSource.getTableNames();

            Map tableMetaDataSources = new HashMap();

            for (int k = 0; k < tableNames.length; k++) {
                String tableName = tableNames[k];
                if (underlyingCatalogSchemaMetaDataSource.hasBeenRequested(tableName)) {
                    TableMetaDataSource tableMetaDataSource = createTableMetaDataSource(tableName, underlyingCatalogSchemaMetaDataSource);
                    if (tableMetaDataSource != null) tableMetaDataSources.put(tableName, tableMetaDataSource);
                } else {
                    if (log.isDebug()) log.debug("Ignoring metadata for table: " + tableName + ", it has not yet been requested");
                    tableMetaDataSources.put(tableName, new JdbcDelegateTableMetaDataSource(underlyingMetaDataSource.getDataSourceName(), CatalogSchemaTable.getInstance(catalogSchema, tableName, null)));
                }
            }

            CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = new XmlCatalogSchemaMetaDataSource(tableNames, tableMetaDataSources);
            log.info("Generated metadata for database: " + catalogSchema + " in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
            return catalogSchemaMetaDataSource;
        } catch (Exception e) {
            log.error("Error generating metadata for database: " + catalogSchema);
            return null;
        }
    }

    private static TableMetaDataSource createTableMetaDataSource(String tableName, CatalogSchemaMetaDataSource underlyingCatalogSchemaMetaDataSource) {
        long startTime = System.currentTimeMillis();
        if (log.isDebug()) log.debug("Generating metadata for table: " + tableName);

        try {
            TableMetaDataSource underlyingTableMetaDataSource = underlyingCatalogSchemaMetaDataSource.getTableMetaDataSource(tableName);
            String[] primaryKeyColumnNames = underlyingTableMetaDataSource.getPrimaryKeyColumnNames();
            IndexData[] indexDatas = underlyingTableMetaDataSource.getIndexDatas();
            ColumnData[] columnDatas = underlyingTableMetaDataSource.getColumnDatas();
            ForeignKeyData[] importedForeignKeys = null; // only need this in generation mode
            ForeignKeyData[] exportedForeignKeys = null; // only need this in generation mode
//                ForeignKeyData[] importedForeignKeys = jdbcTableMetaDataSource.getImportedForeignKeys();
//                ForeignKeyData[] exportedForeignKeys = jdbcTableMetaDataSource.getExportedForeignKeys();
            TableMetaDataSource tableMetaDataSource = new XmlTableMetaDataSource(primaryKeyColumnNames, indexDatas, columnDatas, importedForeignKeys, exportedForeignKeys);
            if (log.isDebug()) log.debug("Generated metadata for table: " + tableName + " in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
            return tableMetaDataSource;
        } catch (Exception e) {
            log.error("Error generating metadata for table: " + tableName);
            return null;
        }
    }

    private static String getZipFileName() {
        return IOUtilities.getUserCacheDirectory() + "/" + "databasemetadata" + "." + ApplicationInfo.getInstance().getEnvironmentId() + ".zip";
    }

    private static void writeMetaDataToFile(MetaDataSource[] metaDataSources) {
        if (metaDataSources.length == 0) return;
        try {
            long startTime = System.currentTimeMillis();
            String zipFileName = getZipFileName();
            log.info("Writing database metadata file: " + zipFileName);
            File file = new File(zipFileName);
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }

            ZipOutputStream stream = null;
            try {
                stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                stream.setLevel(9);
                for (int i = 0; i < metaDataSources.length; i++) {
                    MetaDataSource metaDataSource = metaDataSources[i];
                    ZipEntry zipEntry = new ZipEntry(metaDataSource.getDataSourceName() + ".xml");
                    zipEntry.setComment(version);
                    log.info("Writing database metadata file entry: " + zipEntry.getName());
                    stream.putNextEntry(zipEntry);
                    ByteArrayOutputStream entryStream = new ByteArrayOutputStream(4096);
                    BeanUtilities.writeObjectToStream(metaDataSource, entryStream);
                    stream.write(entryStream.toByteArray());
                    stream.closeEntry();
                }
            } finally {
                if (stream != null) stream.close();
            }
            log.info("Wrote database metadata file: " + zipFileName + " in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public static XmlMetaDataSource[] readMetaDataFromFile() {
        try {
            long startTime = System.currentTimeMillis();
            String zipFileName = getZipFileName();
            File file = new File(zipFileName);
            if (!file.exists()) {
                log.debug("Could not find database metadata file: " + zipFileName);
                return null;
            }
            log.info("Reading database metadata file: " + zipFileName);
            ZipFile zipFile = new ZipFile(file);
            List xmlMetaDataSources = new ArrayList();

            for (Enumeration entries = zipFile.entries(); entries.hasMoreElements();) {
                ZipEntry zipEntry = (ZipEntry) entries.nextElement();
                String entryName = zipEntry.getName();
                String entryVersion = zipEntry.getComment();
                long entryTime = zipEntry.getTime();

                log.info("Reading database metadata file entry: " + entryName + ", version: " + entryVersion + ", timestamp: " + new java.util.Date(entryTime));

//                if (Utilities.equals(version, entryVersion) && (System.currentTimeMillis() - entryTime < DateUtilities.MILLIS_PER_DAY)) {
                if (Utilities.equals(version, entryVersion)) {
                    InputStream stream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                    XmlMetaDataSource xmlMetaDataSource = (XmlMetaDataSource) BeanUtilities.readObjectFromStream(stream);
                    xmlMetaDataSources.add(xmlMetaDataSource);
                } else {
                    log.info("Ignoring database metadata file entry: " + entryName + ", it is a different version");
                }
            }
            log.info("Read database metadata file: " + zipFileName + " in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");

            return (XmlMetaDataSource[]) xmlMetaDataSources.toArray(new XmlMetaDataSource[xmlMetaDataSources.size()]);
        } catch (Throwable e) {
            log.error(e);
            return null;
        }
    }
}