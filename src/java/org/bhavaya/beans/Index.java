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

import org.bhavaya.util.Utilities;

import java.util.Arrays;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class Index {
    private boolean unique;
    private String name;
    private Column[] columns;

    public Index(String indexName, boolean unique, Column[] index) {
        this.name = indexName;
        this.unique = unique;
        this.columns = index;
    }

    public String getName() {
        return name;
    }

    public boolean isUnique() {
        return unique;
    }

    public Column[] getColumns() {
        return columns;
    }

    public boolean equals(Object obj) {
        Index other = (Index) obj;
        return Utilities.equals(name, other.name) && Arrays.equals(columns, other.columns) && unique == other.unique;
    }

    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + name.hashCode();
        hashCode = 31 * hashCode + (unique ? 0 : 1);
        for (int i = 0; i < columns.length; i++) {
            Column column = columns[i];
            hashCode = 31 * hashCode + (column == null ? 0 : column.hashCode());
        }
        return hashCode;
    }
}
