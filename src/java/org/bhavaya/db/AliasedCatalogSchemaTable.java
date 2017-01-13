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

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class AliasedCatalogSchemaTable extends CatalogSchemaTable {
    private CatalogSchemaTable underlyingTable;
    private String alias;
    private String tableRepresentation;

    public AliasedCatalogSchemaTable(CatalogSchemaTable catalogSchemaTable, String alias) {
        super(catalogSchemaTable.getCatalogSchema(), catalogSchemaTable.getTableName(), catalogSchemaTable.getOriginalRepresentation());
        this.underlyingTable = catalogSchemaTable instanceof AliasedCatalogSchemaTable ? ((AliasedCatalogSchemaTable) catalogSchemaTable).getUnderlyingTable() : catalogSchemaTable;
        this.alias = alias;
    }

    public CatalogSchemaTable getUnderlyingTable() {
        return underlyingTable;
    }

    public String getAlias() {
        return alias;
    }

    public String getRepresentation() {
        return alias;
    }

    public String getTableRepresentation() {
        if (tableRepresentation == null) {
            String catalogSchemaRepresentation = getCatalogSchema() != null ? getCatalogSchema().getRepresentation() : null;
            if (catalogSchemaRepresentation != null && catalogSchemaRepresentation.length() > 0) {
                tableRepresentation = catalogSchemaRepresentation + "." + getTableName() + " " + alias;
            } else {
                tableRepresentation = getTableName() + " " + alias;
            }
        }
        return tableRepresentation;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AliasedCatalogSchemaTable)) return false;
        if (!super.equals(o)) return false;

        final AliasedCatalogSchemaTable aliasedCatalogSchemaTable = (AliasedCatalogSchemaTable) o;

        if (!alias.equals(aliasedCatalogSchemaTable.alias)) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + alias.hashCode();
        return result;
    }
}
