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

import java.sql.SQLException;
import java.util.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.8 $
 */
public class Database {
    private static final Log log = Log.getCategory(Database.class);
    private static Map instancesByDataSource = new HashMap();

    private String dataSourceName;
    private CatalogSchema catalogSchema;
    private Map tablesByName;

    public static synchronized Database getInstance(CatalogSchema catalogSchema, String dataSourceName) throws Exception {
        dataSourceName = DataSourceFactory.getDatabaseMetadataDatasource(dataSourceName);

        Map instancesByCatalogSchema = (Map) instancesByDataSource.get(dataSourceName);

        if (instancesByCatalogSchema == null) {
            instancesByCatalogSchema = new IdentityHashMap();// CatalogSchema has a strange equals that allows null to match a non-null String, so use instance
            instancesByDataSource.put(dataSourceName, instancesByCatalogSchema);
        }

        Database database = (Database) instancesByCatalogSchema.get(catalogSchema);
        if (database == null) {
            database = new Database(catalogSchema, dataSourceName);
            instancesByCatalogSchema.put(catalogSchema, database);
        }

        return database;
    }

    private Database(CatalogSchema catalogSchema, String dataSourceName) throws Exception {
        this.catalogSchema = catalogSchema;
        this.dataSourceName = dataSourceName;
        findTables();
    }

    private void findTables() throws Exception {
        if (log.isDebug()) log.debug("Finding tables for: " + catalogSchema + " in dataSourceName: " + dataSourceName);
        long startTime = System.currentTimeMillis();
        MetaDataSource.CatalogSchemaMetaDataSource catalogSchemaMetaDataSource = MetaDataSource.getInstance(dataSourceName).getCatalogSchemaMetaDataSource(catalogSchema);
        String[] tableNames = catalogSchemaMetaDataSource.getTableNames();

        tablesByName = new LinkedHashMap();
        for (int i = 0; i < tableNames.length; i++) {
            String tableName = tableNames[i];
            if (log.isDebug()) log.debug("Found table: " + tableName);
            CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(catalogSchema, tableName, null);
            Table table = new Table(catalogSchemaTable, this);
            tablesByName.put(catalogSchemaTable, table);
        }
        if (log.isDebug())log.debug("Finding tables for: " + catalogSchema + " in dataSourceName: " + dataSourceName + " took: " + (System.currentTimeMillis() - startTime) + " millis");
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public synchronized Map getTables() throws SQLException {
        return tablesByName;
    }


    public String createReport() throws SQLException {
        StringBuffer reportBuffer = new StringBuffer(5000);

        reportBuffer.append("Database: ").append(catalogSchema).append(" on ").append(((DefaultDataSource) DataSourceFactory.getInstance(dataSourceName)).getUrl()).append('\n');
        reportBuffer.append('\n');

        Map tables = getTables();
        for (Iterator iterator = tables.values().iterator(); iterator.hasNext();) {
            Table table = (Table) iterator.next();
            reportBuffer.append(table.createReport()).append("\n");
        }

        return reportBuffer.toString();
    }
}
