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

import gnu.trove.TObjectIntHashMap;
import org.bhavaya.beans.Column;
import org.bhavaya.beans.TabularData;
import org.bhavaya.util.Log;
import org.bhavaya.ui.diagnostics.Profiler;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.19 $
 */
public class ResultSetTabularData implements TabularData {
    private static final Log log = Log.getCategory(ResultSetTabularData.class);
    private static final Log sqlLog = Log.getCategory("sql");

    private String dataSourceName;
    private Column[] columns;
    private TObjectIntHashMap columnToLastIndex;
    private Connection connection;
    private ResultSet resultSet;
    private Statement statement;
    private ResultSetMetaData metaData;
    private String sql;
    private int rowType;
    private int rowIndex;
    private Row row;

    public ResultSetTabularData(String dataSourceName, String sql, int rowType) throws SQLException {
        init(dataSourceName, DataSourceFactory.getInstance(dataSourceName).getConnection(), sql, rowType, null);
        createColumnMetaData();
    }

    public ResultSetTabularData(String dataSourceName, SQL sql, int rowType) throws SQLException {
        String sqlString = sql.getStatementString();
        init(dataSourceName, DataSourceFactory.getInstance(dataSourceName).getConnection(), sqlString, rowType, null);
        createColumnMetaData(sql, dataSourceName);
    }

    public ResultSetTabularData(String dataSourceName, Connection connection, String sql, int rowType, Statement statement) throws SQLException {
        init(dataSourceName, connection, sql, rowType, statement);
        createColumnMetaData();
    }

    private void init(String dataSourceName, Connection connection, String sqlString, int rowType, Statement statement) throws SQLException {
        this.dataSourceName = dataSourceName;
        this.connection = connection;
        this.sql = sqlString;
        this.rowType = rowType;
        this.rowIndex = -1;
        if (statement == null) {
            createResultSet(connection, sqlString);
        } else {
            this.resultSet = statement.getResultSet();
            this.statement = statement;
        }

        this.metaData = this.resultSet.getMetaData();
        row = new Row();
    }

    private void createColumnMetaData() throws SQLException {
        int columnCount = metaData.getColumnCount();
        columns = new Column[columnCount];
        columnToLastIndex = new TObjectIntHashMap(columnCount);
        for (int i = 0; i < columnCount; i++) {
// TODO: the commented out code does not work with all jdbc drivers, maybe one day it can be supported on all
//                String catalogName = metaData.getCatalogName(i + 1);
//                String schemaName = metaData.getSchemaName(i + 1);
//                String tableName = metaData.getTableName(i + 1);
//                CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(catalogName, schemaName, tableName, null);
//                TableColumn column = Table.getInstance(catalogSchemaTable, dataSourceName).getColumn(metaData.getColumnName(i + 1));
            Class columnType = DBUtilities.getClassByJdbcType(metaData.getColumnType(i + 1), metaData.getScale(i + 1));
            Column column = new Column(metaData.getColumnLabel(i + 1), columnType);
            columns[i] = column;
            if (!columnToLastIndex.contains(column)) columnToLastIndex.put(column, i); // columns can be repeated more than once in a sql statement, e.g. where there is two tables in the statement, there may be a column called "id" in both tables which is used to join
        }
    }

    private void createColumnMetaData(SQL sql, String dataSourceName) throws SQLException {
        int columnCount = metaData.getColumnCount();
        columnToLastIndex = new TObjectIntHashMap(columnCount);
        List columnList = new ArrayList(columnCount);
        for (int i = 0; i < columnCount; i++) {
            String columnName = metaData.getColumnName(i + 1);
            TableColumn column = sql.getColumn(columnName, dataSourceName);
            if (column == null) {
                log.error("Null column for:" + columnName + " in: " + sql);
            } else {
                columnList.add(column);
                if (!columnToLastIndex.contains(column)) columnToLastIndex.put(column, i); // columns can be repeated more than once in a sql statement, e.g. where there is two tables in the statement, there may be a column called "id" in both tables which is used to join
            }
        }
        columns = (Column[]) columnList.toArray(new Column[columnList.size()]);
    }

    private void createResultSet(Connection connection, String sql) throws SQLException {
        if (resultSet != null) DBUtilities.closeResultSetAndStatement(resultSet, statement);
        statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        sql = DataSourceFactory.getInstance(dataSourceName).getDialect().transformSelectSql(sql);
        sqlLog.info(sql);

        Profiler.Task executionTask = Profiler.taskStarted(Profiler.SQL_EXECUTE, sql);
        try {
            resultSet = statement.executeQuery(sql);
        } finally {
            Profiler.taskStopped(executionTask);
        }
    }

    public String toString() {
        return sql;
    }

    public TabularData.Row next() {
        boolean hasNext;
        try {
            hasNext = resultSet.next();
            rowIndex++;
        } catch (SQLException e) {
            if (DBUtilities.isRecoverableException(e)) {
                recreateResultSet(e);
                return next();
            } else {
                throw new RuntimeException(e);
            }
        }

        if (hasNext) {
            return row;
        } else {
            return null;
        }
    }

    private void recreateResultSet(SQLException e) {
        while (!DBUtilities.isRecoverableException(e)) {
            e = e.getNextException();
        }

        String sqlState = e.getSQLState();
        log.warn("Encountered sqlState: " + sqlState + ", reexecuting sql: " + sql);

        try {
            createResultSet(connection, sql);
            moveToCorrectResultSetRow();
        } catch (SQLException e1) {
            log.warn("Error reexecuting sql: " + sql);
            throw new RuntimeException(e);
        }
    }

    private void moveToCorrectResultSetRow() throws SQLException {
        try {
            int currentRow = resultSet.getRow();
            while (currentRow <= rowIndex) {
                resultSet.next();
                currentRow = resultSet.getRow();
            }
        } catch (SQLException e) {
            if (DBUtilities.isRecoverableException(e)) {
                recreateResultSet(e);
            } else {
                throw e;
            }
        }
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Column getColumn(int columnIndex) {
        return columns[columnIndex];
    }

    public Column[] getColumns() {
        return columns;
    }

    public void close() {
        if (resultSet != null) {
            DBUtilities.closeResultSetAndStatement(resultSet, statement);
            DBUtilities.close(connection);
            resultSet = null;
            statement = null;
        }
    }


    private class Row implements TabularData.Row {
        public Object getColumnValue(Column column) {
            return getColumnValue(column, null);
        }

        public Object getColumnValue(Column column, Class expectedType) {
            return getColumnValue(columnToLastIndex.get(column), expectedType);
        }

        public Object getColumnValue(int columnIndex) {
            return getColumnValue(columnIndex, null);
        }

        public Object getColumnValue(int columnIndex, Class expectedType) {
            try {
                return DBUtilities.getObjectFromResultSet(columnIndex + 1, resultSet, expectedType); // resultSet use a 1-based index
            } catch (SQLException e) {
                if (DBUtilities.isRecoverableException(e)) {
                    recreateResultSet(e);
                    return getColumnValue(columnIndex);
                } else {
                    throw new RuntimeException(e);
                }
            }
        }

        public int getRowType() {
            return rowType;
        }

        public TabularData getTabularData() {
            return ResultSetTabularData.this;
        }

        public boolean isModified(Column column) {
            return true;
        }
    }
}
