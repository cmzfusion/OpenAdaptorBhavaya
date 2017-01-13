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

import java.util.Map;

/**
 * @author Philip Milne
 * @version $Revision: 1.2 $
 */
public class MapEvent<K, V> extends java.util.EventObject {
    public static final int INSERT = 1;
    public static final int UPDATE = 0;
    public static final int DELETE = -1;
    public static final int COMMIT = 2;
    public static final int ALL_ROWS = -3;

    private int type;
    private K key;
    private V value;

    public MapEvent(Map<K, V> source, int type, K key, V value) {
        super(source);
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    public K getKey() {
        return key;
    }

    public int getType() {
        return type;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapEvent)) return false;

        final MapEvent event = (MapEvent) o;

        if (getSource() != event.getSource()) return false;// use identity
        if (type != event.type) return false;
        if (key != null ? !key.equals(event.key) : event.key != null) return false;
        if (value != event.value) return false; // use identity

        return true;
    }

    public int hashCode() {
        int result;
        result = type;
        result = 29 * result + (getSource() != null ? System.identityHashCode(getSource()) : 0);// use identity
        result = 29 * result + (key != null ? key.hashCode() : 0);
        result = 29 * result + (value != null ? System.identityHashCode(value) : 0); // use identity
        return result;
    }


    public String toString() {
        return getClass().getName() +
                "[type = " + type + "], " +
                "[key = " + key + "]";
    }

    public static String getName(int eventType) {
        switch (eventType) {
            case INSERT:
                return "INSERT";
            case UPDATE:
                return "UPDATE";
            case DELETE:
                return "DELETE";
            case COMMIT:
                return "COMMIT";
            case ALL_ROWS:
                return "ALL_ROWS";
            default:
                return "UNKNOWN";
        }
    }
}
