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
import org.bhavaya.beans.Schema;
import org.bhavaya.util.Attribute;
import org.bhavaya.util.ClassUtilities;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public class TableColumn extends Column implements Attribute {
    private CatalogSchemaTable catalogSchemaTable;
    private int jdbcType;
    private int decimalDigits;
    private int columnSize;
    private boolean foreignKey;
    private String idFactoryName;
    private IdFactory idFactory;
    private String dataSourceName;
    private String representation;
    private Class type;

    public static TableColumn getInstance(String name, String tableName, String dataSourceName) {
        if (tableName == null) return null;
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(tableName, dataSourceName);
        return getInstance(name, catalogSchemaTable, dataSourceName);
    }

    public static TableColumn getInstance(String name, CatalogSchemaTable catalogSchemaTable, String dataSourceName) {
        if (Schema.isGenerationMode()) return new TableColumn(name, catalogSchemaTable, dataSourceName, null);
        if (catalogSchemaTable == null) return null;
        Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
        if (table == null) return null;
        return table.getColumn(name);
    }

    private TableColumn(String name, CatalogSchemaTable catalogSchemaTable, String dataSourceName, IdFactory idFactory) {
        this(name, catalogSchemaTable, dataSourceName, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, false, idFactory);
    }

    protected TableColumn(String name, CatalogSchemaTable catalogSchemaTable, String dataSourceName, int jdbcType, int columnSize, int decimalDigits, boolean foreignKey, IdFactory idFactory) {
        super(name);
        this.catalogSchemaTable = catalogSchemaTable;
        if (dataSourceName == null) {
            dataSourceName = DataSourceFactory.getDefaultDataSourceName();
        }
        this.dataSourceName = dataSourceName;
        this.jdbcType = jdbcType;
        this.columnSize = columnSize;
        this.decimalDigits = decimalDigits;
        this.foreignKey = foreignKey;
        this.idFactory = idFactory;
    }

    public CatalogSchemaTable getCatalogSchemaTable() {
        return catalogSchemaTable;
    }

    public int getJdbcType() {
        if (jdbcType == Integer.MIN_VALUE) updateColumn();
        return jdbcType;
    }

    public void setJdbcType(int jdbcType) {
        this.jdbcType = jdbcType;
    }

    public int getDecimalDigits() {
        if (decimalDigits == Integer.MIN_VALUE) updateColumn();
        return decimalDigits;
    }

    public void setDecimalDigits(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    public int getColumnSize() {
        if (columnSize == Integer.MIN_VALUE) updateColumn();
        return columnSize;
    }

    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }

    public Class getType() {
        if (type == null) type = DBUtilities.getClassByJdbcType(getJdbcType(), getDecimalDigits());
        return type;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isWritable() {
        return true;
    }

    public IdFactory getIdFactory() {
        if (idFactory == null && idFactoryName != null) {
            try {
                idFactory = (IdFactory) ClassUtilities.getClass(idFactoryName, true, false).newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return idFactory;
    }

    public String getIdFactoryName() {
        return idFactoryName;
    }

    public void setIdFactoryName(String idFactoryName) {
        this.idFactoryName = idFactoryName;
    }

    public boolean isForeignKey() {
        return foreignKey;
    }

    public void setForeignKey(boolean foreignKey) {
        this.foreignKey = foreignKey;
    }

    private void updateColumn() {
        // will update all columns from database metadata
        Table table = Table.getInstance(catalogSchemaTable, dataSourceName);
        if (table != null) table.getColumn(getName());
    }

    public String getNativeDescription() throws Exception {
        BhavayaDataSource dataSource = DataSourceFactory.getInstance(dataSourceName);
        return dataSource.getNativeColumnDescription(getJdbcType(), getColumnSize(), getDecimalDigits());
    }

    /**
     * TODO: a hack to ensure columns are equal even when table name differs, rely on super.equals
     * e.g. a primary key of BOND_TRADE.trade_id can be represent as TABLE.trade_id where there is the concept of inheritance
     * therefore just use the super.equals
     * @param o
     * @return
     */
//     public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof TableColumn)) return false;
//        if (!super.equals(o)) return false;
//        final TableColumn tableColumn = (TableColumn) o;
//        if (catalogSchemaTable != null ? !catalogSchemaTable.equals(tableColumn.catalogSchemaTable) : tableColumn.catalogSchemaTable != null) return false;
//        return true;
//    }

    /**
     * TODO: a hack to ensure columns are equal even when table name differs, rely on super.hashCode
     * e.g. a primary key of BOND_TRADE.trade_id can be represent as TABLE.trade_id where there is the concept of inheritance
     * therefore just use the super.hashCode
     *
     * @return
     */
//    public int hashCode() {
//        int result = super.hashCode();
//        result = 29 * result + (catalogSchemaTable != null ? catalogSchemaTable.hashCode() : 0);
//        return result;
//    }

    public String getRepresentation() {
        if (catalogSchemaTable == null) return super.getRepresentation();
        if (representation == null) {
            String tableRepresentation = catalogSchemaTable.getRepresentation();
            if (tableRepresentation.length() > 0) {
                representation = tableRepresentation + "." + getName();
            } else {
                representation = getName();
            }
        }
        return representation;
    }

    public String toString() {
        return getRepresentation();
    }
}
