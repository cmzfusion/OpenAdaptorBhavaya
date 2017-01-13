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
import org.bhavaya.util.Log;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public class SybaseDatabaseDialect extends DefaultDatabaseDialect {
    private static final Log log = Log.getCategory(SybaseDatabaseDialect.class);

    private static int tableCount = 4;
    private int temporaryTableIndex = 0;
    private static boolean jtc = true;

    public void configure(Connection connection) throws SQLException {
        super.configure(connection);
        Statement statement = null;
        try {
            statement = connection.createStatement();
            // Makes the query parse better at determining which indexes to use when a query contains many tables
            // as Bhavaya can end up with many tables in a query, through auto-joining, this can make a big difference
            log.info("set table count " + tableCount);
            statement.execute("set table count " + tableCount);

            // Join transitive closure option makes the Sybase query optimiser consider all possible join orders.
            // Normall, given tables a,b and c Sybase will try abc, bca and cab.  JTC causes bac and acb and cba to
            // be tested.
            if (jtc) {
                log.info("set jtc on");
                statement.execute("set jtc on");
            }
        } finally {
            DBUtilities.closeResultSetAndStatement(null, statement);
        }
    }

    public String getConnectionId(Connection connection) {
        ResultSet resultSet = null;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT @@spid");
            resultSet.next();
            return resultSet.getString(1);
        } catch (SQLException e) {
            log.error(e);
            return "Error";
        } finally {
            DBUtilities.closeResultSetAndStatement(resultSet, statement);
        }
    }

    public static void setTableCount(int tableCount) {
        SybaseDatabaseDialect.tableCount = tableCount;
    }

    public static int getTableCount() {
        return tableCount;
    }

    public static boolean isJtc() {
        return jtc;
    }

    public static void setJtc(boolean jtc) {
        SybaseDatabaseDialect.jtc = jtc;
    }

    public String createTempTable(Connection connection, Column[] columns, boolean createWithPrimaryKey) throws Exception {
        temporaryTableIndex++;
        String tableName = "#t" + temporaryTableIndex;
        log.info("Creating temporary table: " + tableName);
        String createStatementString = createTableString(tableName, columns, createWithPrimaryKey);
        DBUtilities.executeUpdate(connection, createStatementString);
        return tableName;
    }

    public boolean containsPrivateTempTable(SQL sqlToExecute) {
        CatalogSchemaTable[] tables = sqlToExecute.getTables();
        for (int i = 0; i < tables.length; i++) {
            CatalogSchemaTable table = tables[i];
            if (table.getTableName().startsWith("#")) return true;
        }
        return false;
    }

    private static String createTableString(String tableName, Column[] columns, boolean createWithPrimaryKey) throws Exception {
        StringBuffer createStatement = new StringBuffer("CREATE TABLE ").append(tableName).append(" (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) createStatement.append(", ");
            TableColumn column = (TableColumn) columns[i];
            createStatement.append(column.getName()).append(" ").append(column.getNativeDescription());
        }

        if (createWithPrimaryKey) {
            createStatement.append(", CONSTRAINT PK_").append(tableName.substring(1)).append(" PRIMARY KEY (");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) createStatement.append(", ");
                TableColumn column = (TableColumn) columns[i];
                createStatement.append(column.getName());
            }
            createStatement.append(")");
        }

        createStatement.append(")");

        return createStatement.toString();
    }
}
