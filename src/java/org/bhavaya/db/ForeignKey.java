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

import org.bhavaya.util.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class ForeignKey {
    private String name;
    private CatalogSchemaTable thisCatalogSchemaTable;
    private CatalogSchemaTable otherCatalogSchemaTable;
    private List foreignKeyColumns;
    private List otherForeignKeyColumns;
    private String join;

    public ForeignKey(String name, CatalogSchemaTable thisCatalogSchemaTable, CatalogSchemaTable otherCatalogSchemaTable) {
        this.name = name;
        this.thisCatalogSchemaTable = thisCatalogSchemaTable;
        this.otherCatalogSchemaTable = otherCatalogSchemaTable;
    }

    public void addForeignKeyColumn(String thisColumnName, String otherColumnName, String dataSourceName) {
        if (foreignKeyColumns == null) foreignKeyColumns = new ArrayList();
        if (otherForeignKeyColumns == null) otherForeignKeyColumns = new ArrayList();
        foreignKeyColumns.add(TableColumn.getInstance(thisColumnName, thisCatalogSchemaTable, dataSourceName));
        otherForeignKeyColumns.add(TableColumn.getInstance(otherColumnName, otherCatalogSchemaTable, dataSourceName));
        Utilities.sort(foreignKeyColumns);
        Utilities.sort(otherForeignKeyColumns);
        if (join != null) {
            join = join + " AND ";
        } else {
            join = "";
        }
        join = join + thisCatalogSchemaTable.getRepresentation() + "." + thisColumnName + " = " + otherCatalogSchemaTable.getRepresentation() + "." + otherColumnName;
    }

    public String getName() {
        return name;
    }

    public CatalogSchemaTable getThisCatalogSchemaTable() {
        return thisCatalogSchemaTable;
    }

    public CatalogSchemaTable getOtherCatalogSchemaTable() {
        return otherCatalogSchemaTable;
    }

    public String getJoin() {
        return join;
    }

    public TableColumn[] getThisColumns() {
        return (TableColumn[]) foreignKeyColumns.toArray(new TableColumn[foreignKeyColumns.size()]);
    }

    public TableColumn[] getOtherColumns() {
        return (TableColumn[]) otherForeignKeyColumns.toArray(new TableColumn[otherForeignKeyColumns.size()]);
    }

    public String createReport() {
        StringBuffer reportBuffer = new StringBuffer();

        reportBuffer.append(name).append(" (").append(thisCatalogSchemaTable.getRepresentation()).append(" -> ").append(otherCatalogSchemaTable.getRepresentation()).append("): ");

        for (int i = 0; i < foreignKeyColumns.size(); i++) {
            TableColumn tableColumn = (TableColumn) foreignKeyColumns.get(i);
            TableColumn otherTableColumn = (TableColumn) otherForeignKeyColumns.get(i);
            if (i > 0) reportBuffer.append(", ");
            reportBuffer.append(tableColumn.getName()).append(" -> ").append(otherTableColumn.getName());
        }
        return reportBuffer.toString();
    }
}
