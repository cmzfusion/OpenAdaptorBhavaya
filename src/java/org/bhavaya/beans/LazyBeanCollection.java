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

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.CollectionListener;
import org.bhavaya.util.LoadClosure;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class LazyBeanCollection implements Lazy, BeanCollection {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    public static final BeanCollection NULL_COLLECTION = new NullBeanCollection();
    private static LazyBeanCollection lazyNullInstance;

    private LoadClosure load;

    LazyBeanCollection() {
    }

    public Object load() {
        return load.load();
    }

    public void setLoad(LoadClosure load) {
        this.load = load;
    }

    public boolean isLazy() {
        return load != null;
    }

    public String toString() {
        return load != null ? load.toString() : "null";
    }

    public BeanCollection[] getBeanCollections() {
        return null;
    }

    public Class getType() {
        return null;
    }

    public void add(int index, Object element, boolean fireCommit) {
    }

    public boolean add(Object value, boolean fireCommit) {
        return false;
    }

    public void addCollectionListener(CollectionListener l) {
    }

    public int size() {
        return 0;
    }

    public boolean addAll(int index, Collection c) {
        return false;
    }

    public boolean addAll(int index, Collection c, boolean fireCommit) {
        return false;
    }

    public boolean remove(Object value, boolean fireCommit) {
        return false;
    }

    public void removeCollectionListener(CollectionListener l) {
    }

    public boolean isEmpty() {
        return false;
    }

    public Object get(int index) {
        return null;
    }

    public Object remove(int index, boolean fireCommit) {
        return null;
    }

    public boolean addAll(Collection c, boolean fireCommit) {
        return false;
    }

    public boolean contains(Object o) {
        return false;
    }

    public Object set(int index, Object element) {
        return null;
    }

    public Object set(int index, Object element, boolean fireCommit) {
        return null;
    }

    public boolean removeAll(Collection c, boolean fireCommit) {
        return false;
    }

    public Iterator iterator() {
        return null;
    }

    public void add(int index, Object element) {
    }

    public boolean retainAll(Collection c, boolean fireCommit) {
        return false;
    }

    public Object[] toArray() {
        return EMPTY_ARRAY;
    }

    public Object remove(int index) {
        return null;
    }

    public void clear(boolean fireCommit) {
    }

    public Object getTransactionLock() {
        return this;
    }

    public Object[] toArray(Object a[]) {
        return EMPTY_ARRAY;
    }

    public int indexOf(Object o) {
        return 0;
    }

    public void fireCommit() {
    }

    public boolean add(Object o) {
        return false;
    }

    public int lastIndexOf(Object o) {
        return 0;
    }

    public boolean remove(Object o) {
        return false;
    }

    public ListIterator listIterator() {
        return null;
    }

    public boolean containsAll(Collection c) {
        return false;
    }

    public ListIterator listIterator(int index) {
        return null;
    }

    public boolean addAll(Collection c) {
        return false;
    }

    public List subList(int fromIndex, int toIndex) {
        return null;
    }

    public boolean removeAll(Collection c) {
        return false;
    }

    public boolean retainAll(Collection c) {
        return false;
    }

    public void clear() {
    }

    private static class NullBeanCollection implements BeanCollection {
        public Class getType() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public BeanCollection[] getBeanCollections() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Object getTransactionLock() {
            return this;
        }

        public void add(int index, Object element, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean add(Object value, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean addAll(Collection c, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean addAll(int index, Collection c, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Object remove(int index, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean remove(Object value, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean removeAll(Collection c, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean retainAll(Collection c, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Object set(int index, Object element, boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public void clear(boolean fireCommit) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public void fireCommit() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public void addCollectionListener(CollectionListener l) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public void removeCollectionListener(CollectionListener l) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public int size() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean isEmpty() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean contains(Object o) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Iterator iterator() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Object[] toArray() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Object[] toArray(Object a[]) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean add(Object o) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean containsAll(Collection c) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean addAll(int index, Collection c) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public void clear() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Object get(int index) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Object set(int index, Object element) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public void add(int index, Object element) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public Object remove(int index) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public int indexOf(Object o) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public ListIterator listIterator() {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public ListIterator listIterator(int index) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }

        public List subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException("it should be impossible to call this method");
        }
    }

    public boolean isLazyNullInstance() {
        return lazyNullInstance == this;
    }

    public void setThisInstanceAsLazyNull() {
        lazyNullInstance = this;
    }
}
