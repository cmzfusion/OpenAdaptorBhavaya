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

import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
class SQLTabularData implements TabularData {
    private SQL sql;
    private int rowType;
    private int row;

    public SQLTabularData(SQL sql) {
        row = -1;
        this.sql = sql;
        rowType = getRowType(sql);
    }

    public String toString() {
        return sql.toString();
    }

    public TabularData.Row next() {
        if (row == -1) {
            row++;
            return new Row();
        } else {
            return null;
        }
    }

    public int getColumnCount() {
        return sql.getColumnCount();
    }

    public Column getColumn(int columnIndex) {
        return sql.getColumn(columnIndex);
    }

    public Column[] getColumns() {
        List columns = sql.getColumns();
        return (Column[]) columns.toArray(new Column[columns.size()]);
    }

    public void close() {
    }

    public static int getRowType(SQL sql) {
        int rowType = 0;
        if (sql.getOperationType() == SQL.SELECT) {
            rowType = ROW_TYPE_SELECT;
        } else if (sql.getOperationType() == SQL.INSERT) {
            rowType = ROW_TYPE_INSERT;
        } else if (sql.getOperationType() == SQL.DELETE) {
            rowType = ROW_TYPE_DELETE;
        } else if (sql.getOperationType() == SQL.UPDATE) {
            rowType = ROW_TYPE_UPDATE;
        }
        return rowType;
    }

    private class Row implements TabularData.Row {
        public Object getColumnValue(Column column) {
            return getColumnValue(column, null);
        }

        public Object getColumnValue(Column column, Class expectedType) {
            if (row != 0) throw new RuntimeException("Invalid row");

            String columnValue = sql.getColumnValue(column);
            if (columnValue == null || columnValue.equalsIgnoreCase("null")) return null;

            if (expectedType == null) {
                int columnSqlType = ((TableColumn) column).getJdbcType();
                int columnPrecision = ((TableColumn) column).getDecimalDigits();

                // cant find column on view
                if (columnSqlType == Integer.MIN_VALUE || columnPrecision == Integer.MIN_VALUE) return null;
                expectedType = DBUtilities.getClassByJdbcType(columnSqlType, columnPrecision);
            }

            return DBUtilities.getObjectFromString(columnValue, expectedType, sql.getDataSourceName());
        }

        public Object getColumnValue(int columnIndex) {
            return getColumnValue(columnIndex);
        }

        public Object getColumnValue(int columnIndex, Class expectedType) {
            return getColumnValue(getColumn(columnIndex), expectedType);
        }

        public int getRowType() {
            return rowType;
        }

        public TabularData getTabularData() {
            return SQLTabularData.this;
        }

        public boolean isModified(Column column) {
            return sql.isModified(column);
        }
    }
}
