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

import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.beans.Encoder;
import java.beans.Statement;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


/**
 * The main purpose of this class was to override the equals and hashCode,
 * so that they dont create a new Iterator each invocation.
 * This allows many EfficientArrayLists to be used as keys in large Maps without creating lots of garbage.
 * <p/>
 * Really this class wants to be an immutable ArrayList but its main usage is where you know the size required
 * on construction of EfficientArrayList, but you need to add elements later by calling add().  Hence add()
 * is still supported.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.13 $
 */
public class EfficientArrayList<E> implements List<E> {
    private E elementData[];
    private int size;

    static {
        BeanUtilities.addPersistenceDelegate(EfficientArrayList.class, new BhavayaPersistenceDelegate(new String[]{"size"}) {
            protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
                List<?> oldList = (List<?>) oldInstance;
                for (Object o : oldList) {
                    out.writeStatement(new Statement(oldInstance, "add", new Object[]{o}));
                }
            }

            /**
             * Adding this stopped a StackOverFlow when persisting.
             */
            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return (newInstance != null && oldInstance.getClass() == newInstance.getClass());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public EfficientArrayList(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        this.elementData = (E[])new Object[initialCapacity];
    }

    @SuppressWarnings("unchecked")
    public EfficientArrayList(Object... objects) {
        elementData = (E[]) objects.clone();
        size = elementData.length;
    }

    public boolean add(E o) {
        elementData[size++] = o;
        return true;
    }

    public E set(int index, E element) {
        E oldElement = elementData[index];
        elementData[index] = element;
        return oldElement;
    }

    public E get(int index) {
        return elementData[index];
    }

    public int size() {
        return size;
    }

    //Added to fix bug caused by change to DefaultPersistenceDelegate in Java 6
    public int getSize() {
        return size();
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            elementData[i] = null;
        }

        size = 0;
    }

    public Iterator<E> iterator() {
        return new Itr<E>(this);
    }

    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size()) throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size());
        return new ListItr<E>(this, index);
    }

    public boolean contains(Object elem) {
        return indexOf(elem) >= 0;
    }

    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    public int indexOf(Object elem) {
        if (elem == null) {
            for (int i = 0; i < size; i++) {
                if (elementData[i] == null) return i;
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (elem.equals(elementData[i])) return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(Object elem) {
        if (elem == null) {
            for (int i = size - 1; i >= 0; i--) {
                if (elementData[i] == null) return i;
            }
        } else {
            for (int i = size - 1; i >= 0; i--) {
                if (elem.equals(elementData[i])) return i;
            }
        }
        return -1;
    }

    public Object[] toArray() {
        Object[] result = new Object[size];
        System.arraycopy(elementData, 0, result, 0, size);
        return result;
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T a[]) {
        if (a.length < size) a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size) a[size] = null;
        return a;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[");

        for (int i = 0; i < size; i++) {
            if (i > 0) buf.append(", ");
            Object o = elementData[i];
            buf.append(o == this ? "(this Collection)" : String.valueOf(o));
        }

        buf.append("]");
        return buf.toString();
    }

    public final int hashCode() {
        int hashCode = 1;
        for (E o : elementData) {
            hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
        }
        return hashCode;
    }

    public final boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof List)) return false;

        List<?> otherList = ((List<?>) o);
        int otherSize = otherList.size();

        if (size != otherSize) return false;

        for (int i = 0; i < size; i++) {
            Object o1 = elementData[i];
            Object o2 = otherList.get(i);
            if (!(o1 == null ? o2 == null : o1.equals(o2))) return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            EfficientArrayList<E> clone = (EfficientArrayList<E>) super.clone();
            clone.elementData = (E[]) toArray();
            return clone;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    private static class Itr<E> implements Iterator<E> {
        protected List<E> list;
        protected int cursor;

        public Itr(List<E> list) {
            this.list = list;
            cursor = 0;
        }

        public boolean hasNext() {
            return cursor < list.size();
        }

        public E next() {
            E next = list.get(cursor);
            cursor++;
            return next;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ListItr<E> extends Itr<E> implements ListIterator<E> {
        ListItr(List<E> list, int index) {
            super(list);
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public E previous() {
            E previous = list.get(cursor - 1);
            cursor--;
            return previous;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        public void set(Object o) {
            throw new UnsupportedOperationException();
        }

        public void add(Object o) {
            throw new UnsupportedOperationException();
        }
    }
}
