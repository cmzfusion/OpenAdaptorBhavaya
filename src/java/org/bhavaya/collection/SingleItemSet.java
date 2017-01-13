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

import org.bhavaya.util.Utilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public class SingleItemSet implements Set {
    private static final Object NOT_SET = new Object() {
        public String toString() {
            return "NO_DATA";
        }
    };

    private Object object = NOT_SET;

    public SingleItemSet(Object object) {
        this.object = object;
    }

    public SingleItemSet() {
    }

    public boolean add(Object o) {
        if (object != NOT_SET) {
            if (!Utilities.equals(object, o)) {
                throw new IllegalArgumentException("Single set already contains a value: " + object);
            }
            return false;
        }
        object = o;
        return true;
    }

    public boolean addAll(Collection c) {
        if (c == this) return false;

        if (!c.isEmpty()) {
            int totalSize = size() + c.size();
            if (totalSize > 2) throw new IllegalArgumentException("Adding collection would mean storing " + totalSize + " objects.");
            return add(c.iterator().next());
        }
        return false;
    }

    public void clear() {
        object = NOT_SET;
    }

    public boolean contains(Object o) {
        return Utilities.equals(object, o);
    }

    public boolean containsAll(Collection c) {
        if (c instanceof SingleItemSet) {
            return Utilities.equals(object, ((SingleItemSet) c).object);
        }

        int size = c.size();
        switch (size) {
            case 0:
                return true;
            case 1:
                return c.contains(object);
            default:
                return false;
        }
    }

    public boolean isEmpty() {
        return object == NOT_SET;
    }

    public Iterator iterator() {
        return new SingleItemIterator();
    }

    public boolean remove(Object o) {
        if (contains(o)) {
            clear();
            return true;
        }
        return false;
    }

    public boolean removeAll(Collection c) {
        if (isEmpty()) return false;
        if (c.contains(object)) {
            clear();
            return true;
        }
        return false;
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Too lazy");
    }

    public int size() {
        return object == NOT_SET ? 0 : 1;
    }

    public Object[] toArray() {
        return new Object[]{object};
    }

    public Object[] toArray(Object a[]) {
        throw new UnsupportedOperationException("Too lazy");
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Set)) return false;
        Set other = (Set) o;
        if (size() != other.size()) return false;
        return other.contains(object);
    }

    public int hashCode() {
        return (object != null ? object.hashCode() : 0);
    }

    /**
     * if the given set is a singleItemSet, returns a new HashSet containing the union of both collections
     * else just adds the additional items to the given set and returns set
     *
     * @param set
     * @param additionalItems
     * @return
     */
    public static Set mergeSets(Set set, Collection additionalItems) {
        if (set instanceof SingleItemSet) {
            if (set.containsAll(additionalItems)) {
                return set;
            } else {
                HashSet newSet = new HashSet(1 + additionalItems.size());
                newSet.addAll(set);
                newSet.addAll(additionalItems);
                return newSet;
            }
        } else {
            set.addAll(additionalItems);
            return set;
        }
    }

    /**
     * used when you want to minimise memory usage for a set that will mostly contain 1 item
     * but may occasionally contain more.
     * If set == null, the result will be a SingleItemSet
     * if set instanceof SingleItemSet the result will be a HashSet
     */
    public static Set addToSet(Set set, Object item) {
        if (set == null) {
            return new SingleItemSet(item);
        } else if (set instanceof SingleItemSet) {
            if (set.contains(item)) {
                return set;
            } else {
                HashSet newSet = new HashSet(2);
                newSet.addAll(set);
                newSet.add(item);
                return newSet;
            }
        } else {
            set.add(item);
            return set;
        }
    }

    private class SingleItemIterator implements Iterator {
        private boolean hasNext = size() > 0;

        public boolean hasNext() {
            return hasNext;
        }

        public Object next() {
            hasNext = false;
            return object;
        }

        public void remove() {
            clear();
        }
    }

    public String toString() {
        if (isEmpty()) return "[]";
        return "[" + object + "]";
    }
}
