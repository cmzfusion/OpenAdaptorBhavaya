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

import org.bhavaya.beans.DefaultValue;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
class DefaultValueToTableJoin implements Join {
    private DefaultValue defaultValue;
    private CatalogSchemaTable foreignTable;
    private String foreignColumn;
    private String dataSourceName;

    public DefaultValueToTableJoin(DefaultValue defaultValue, CatalogSchemaTable foreignCatalogSchemaTable, String foreignColumn, String dataSourceName) {
        this.defaultValue = defaultValue;
        this.foreignTable = foreignCatalogSchemaTable;
        this.foreignColumn = foreignColumn;
        this.dataSourceName = dataSourceName;
    }

    public Join aliasForeignTable(AliasedCatalogSchemaTable alias) {
        if (foreignTable.equals(alias.getUnderlyingTable())) {
            return new DefaultValueToTableJoin(defaultValue, alias, foreignColumn, dataSourceName);
        }
        return this;
    }

    public Join aliasParentTable(AliasedCatalogSchemaTable alias) {
        // doesnt do anything
        return this;
    }

    public String getRepresentation() {
        StringBuffer buffer = new StringBuffer(150);
        buffer.append(foreignTable.getRepresentation()).append(".").append(foreignColumn);
        if (defaultValue.getValue() != null) {
            buffer.append(" = ").append(SQLFormatter.getInstance(dataSourceName).format(defaultValue.getValue()));
        } else {
            buffer.append(" IS NULL");
        }
        return buffer.toString();
    }

    public String toString() {
        return getRepresentation();
    }
}
