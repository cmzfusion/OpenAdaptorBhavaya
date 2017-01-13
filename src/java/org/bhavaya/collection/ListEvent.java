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

package org.bhavaya.collection;

import java.util.Collection;

/**
 * @author Parwinder sekhon
 * @version $Revision: 1.4 $
 */
public class ListEvent<E> extends java.util.EventObject {
    public static final int INSERT = 1;
    public static final int UPDATE = 0;
    public static final int DELETE = -1;
    public static final int COMMIT = 2;
    public static final int ALL_ROWS = -3;

    private int type;
    private E value;

    private int row = -1;

    /**
     * @param source
     * @param type   either INSERT, UPDATE, DELETE, COMMIT or ALL_ROWS
     * @param value  the value that was inserted, updated or deleted, for COMMIT and ALL_ROWS value is null.
     * @param row    indicates the row index for the value that was inserted, removed, or updated, for COMMIT and ALL_ROWS value is -1.
     */
    public ListEvent(Collection<E> source, int type, E value, int row) {
        this(source, type, value);
        this.row = row;
    }

    public ListEvent(Collection<E> source, int type, E value) {
        super(source);
        this.type = type;
        this.value = value;
    }

    public int getRow() {
        return row;
    }

    public E getValue() {
        return value;
    }

    public int getType() {
        return type;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListEvent)) return false;

        final ListEvent<E> event = (ListEvent<E>) o;

        if (getSource() != event.getSource()) return false;// use identity
        if (row != event.row) return false;
        if (type != event.type) return false;
        return value == event.value;
    }

    public int hashCode() {
        int result;
        result = type;
        result = 29 * result + (getSource() != null ? System.identityHashCode(getSource()) : 0);// use identity
        result = 29 * result + (value != null ? System.identityHashCode(value) : 0); // use identity
        result = 29 * result + row;
        return result;
    }

    public String toString() {
        return getClass().getName() + "[type = " + type + "], " + "[value = " + value + "]";
    }
}
