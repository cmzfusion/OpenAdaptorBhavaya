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

package org.bhavaya.beans;

import org.bhavaya.util.Generic;
import org.bhavaya.util.Utilities;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class DefaultProperty extends Property {
    private static final Column[] EMPTY_COLUMN_ARRAY = new Column[]{};

    private Column column;
    private Column[] columns;

    public DefaultProperty(String parentTypeName, String name, String typeName, String cardinality) {
        this(parentTypeName, name, typeName, cardinality, null);
    }

    public DefaultProperty(String parentTypeName, String name, String typeName, String cardinality, Column column) {
        super(parentTypeName, name, typeName, cardinality);
        this.column = column;
        columns = column == null ? EMPTY_COLUMN_ARRAY : new Column[]{column};
    }

    public Column[] getColumns() {
        return columns;
    }

    public void setPropertyValue(Object bean, Object columnValue) {
        if (this.column == null) return;

        if (columnValue != null) {
            columnValue = Utilities.changeType(getType(), columnValue);
        } else {
            columnValue = Utilities.getNullValue(getType());
        }
        columnValue = applyPropertyValueTransform(bean, columnValue);
        Generic.set(bean, getName(), columnValue);
    }

    public Object getColumnValue(Object bean, Column column) {
        if (!Utilities.equals(this.column, column)) throw new RuntimeException("Invalid column");
        return getValue(bean);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DefaultProperty)) return false;
        DefaultProperty other = (DefaultProperty) obj;
        return super.equals(obj) && Utilities.equals(column, other.column);
    }

    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + (column == null ? 0 : column.hashCode());
        return hashCode;
    }
}
