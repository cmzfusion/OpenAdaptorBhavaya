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


import org.bhavaya.util.Attribute;
import org.bhavaya.util.ClassUtilities;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class Column implements Attribute {
    private String name;
    private String typeName;
    private Class type;

    public Column(String name, Class type) {
        this(name);
        this.type = type;
        this.typeName = type != null ? type.getName() : null;
    }

    public Column(String name, String typeName) {
        this(name);
        this.typeName = typeName; // this delays class loading until later, allows for schema generation
    }

    public Column(String name) {
        if (name == null) throw new IllegalArgumentException("Attribute name is null");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        if (type == null && typeName != null) type = ClassUtilities.getClass(typeName, true, false);
        return type;
    }

    public static String[] columnsToNames(Column[] columns) {
        String[] names = new String[columns.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = columns[i].getName();

        }
        return names;
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isWritable() {
        return true;
    }

    //TODO: a hack to ensure columns are equal even when type name differs, e.g. in one column it may be blank
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Column)) return false;

        final Column column = (Column) o;
        if (!name.equals(column.name)) return false;
        return true;
    }

    public int hashCode() {
        int result = name.hashCode();
        return result;
    }

    public int compareTo(Object object) {
        Column other = (Column) object;
        return name.compareTo(other.name);
    }

    public String toString() {
        return getRepresentation();
    }

    public String getRepresentation() {
        return getName();
    }
}
