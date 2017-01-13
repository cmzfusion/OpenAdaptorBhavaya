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

import java.util.*;

/**
 * @author Brendon McLean
 * @version $Revision: 1.10.26.1 $
 */
public class IndexedSet<E> implements List<E>, RandomAccess, Cloneable {
    private ArrayList<E> list;
    private NullableKeyTObjectIntHashMap objectsToIndex;
    private transient int modCount = 0;

    public IndexedSet(int initialSize) {
        list = new ArrayList<E>(initialSize);
        objectsToIndex = new NullableKeyTObjectIntHashMap(initialSize);
    }

    public IndexedSet() {
        this(16);
    }

    public IndexedSet(Collection<? extends E> collection) {
        this(collection.size());
        addAll(collection);
    }

    public void ensureCapacity(int minCapacity) {
        list.ensureCapacity(minCapacity);
        objectsToIndex.ensureCapacity(minCapacity);
    }

    /**
     * for all indexes after the given one (non-inclusive), apply delta to the recordedindex value in our lookup
     */
    private void adjustIndexesAfter(int index, int delta) {
        //fix the indicies of the objects after the insert point
        for (int i = index + 1; i < size(); i++) {
            Object o = get(i);
            int oldIndex = objectsToIndex.get(o);
            objectsToIndex.put(o, oldIndex + delta);
        }
    }

    /**
     * if the set already contains the given object, this method has no effect (i.e. no indexes are updated)
     * if you expect the index of the given object to be updated to be size()-1 (whether it is already present or not),
     * then please use add(index, object)
     */
    public boolean add(E o) {
        return addInternal(o);
    }

    private boolean addInternal(E o) {
        if (contains(o)) {
            return false;
        } else {
            addInternal(size(), o);
            return true;
        }
    }

    public void add(int index, E element) {
        removeInternal(element);    //since we are adding at a specified index, we should update the index of the object if it already exists
        addInternal(index, element);
    }

    private void addInternal(int index, E o) {
        list.add(index, o);
        modCount++;
        objectsToIndex.put(o, index);
        adjustIndexesAfter(index, 1);
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean added = false;
        ensureCapacity(size() + c.size());
        for (E o : c) {
            if (addInternal(o)) {
                added = true;
            }
        }
        return added;
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        return removeInternal(o);
    }

    private boolean removeInternal(Object o) {
        boolean containsValue = objectsToIndex.containsKey(o);
        if (containsValue) {
            int idx = objectsToIndex.get(o);
            objectsToIndex.remove(o);
            adjustIndexesAfter(idx, -1);
            list.remove(idx);
            modCount++;
            return true;
        } else {
            return false;
        }
    }

    public E remove(int index) {
        E obj = get(index);
        removeInternal(obj);
        return obj;
    }

    public boolean removeAll(Collection<?> c) {
        boolean removed = false;
        for (Object o : c) {
            if (removeInternal(o)) {
                removed = true;
            }
        }
        return removed;
    }

    public E set(int index, E o) {
        boolean containsValue = objectsToIndex.containsKey(o);
        int oldIndex = objectsToIndex.get(o);

        E oldObject;
        if (containsValue && oldIndex == index) {
            // the new element ".equals" the old element and is at the same index, just replace the old one with the new one, as it doesnt affect the position
            // we still change the instance, as the identity of o may be different to oldObject, even though they are .equals
            objectsToIndex.remove(o);
            objectsToIndex.put(o, index);
            list.set(index, o);
            oldObject = o;
        } else if (containsValue && oldIndex != index) {
            // the element is already in the Set, but in a different position to index
            // if index > list.indexOf(element), you end up replacing a different oldObject than you expected
            // if index == list.size(), you end up with an ArrayIndexOutOfBoundsException
            throw new UnsupportedOperationException("Invalid set: index: " + index + ", oldIndex: " + oldIndex + ", size: " + size() + ", object: " + o);
        } else {
            // the element is not already in the Set
            objectsToIndex.put(o, index);
            oldObject = list.set(index, o);
            objectsToIndex.remove(oldObject);
        }

        modCount++;
        return oldObject;
    }

    public boolean contains(Object o) {
        return objectsToIndex.containsKey(o);
    }

    public int indexOf(Object o) {
        boolean containsValue = objectsToIndex.containsKey(o);

        if (containsValue) {
            return objectsToIndex.get(o);
        } else {
            return -1;
        }
    }

    public void clear() {
        list.clear();
        modCount++;
        objectsToIndex.clear();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Iterator<E> iterator() {
        return new Itr<E>(this);
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <T> T[] toArray(T a[]) {
        return list.toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public E get(int index) {
        return list.get(index);
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size()) throw new IndexOutOfBoundsException("Index: " + index + ", size: " + size());
        return new ListItr<E>(this, index);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    public Object clone() throws CloneNotSupportedException {
        IndexedSet<E> clone = (IndexedSet<E>) super.clone();
        clone.objectsToIndex = (NullableKeyTObjectIntHashMap) objectsToIndex.clone();
        clone.list = (ArrayList<E>) list.clone();
        return clone;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexedSet)) return false;

        final IndexedSet<E> indexedSet = (IndexedSet<E>) o;

        return list.equals(indexedSet.list);
    }

    public int hashCode() {
        return list.hashCode();
    }

    protected void removeRange(int fromIndex, int toIndex) {
        ListIterator<E> it = listIterator(fromIndex);
        for (int i = 0, n = toIndex - fromIndex; i < n; i++) {
            it.next();
            it.remove();
        }
    }


    private static class Itr<E> implements Iterator<E> {
        protected IndexedSet<E> indexedSet;
        protected int cursor;
        protected int lastReturnedIndex;
        protected int expectedModCount;

        public Itr(IndexedSet<E> indexedSet) {
            this.indexedSet = indexedSet;
            expectedModCount = indexedSet.modCount;
            cursor = 0;
            lastReturnedIndex = -1;
        }

        public boolean hasNext() {
            return cursor < indexedSet.size();
        }

        public E next() {
            checkForComodification();
            try {
                E next = indexedSet.get(cursor);
                lastReturnedIndex = cursor;
                cursor++;
                return next;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            if (lastReturnedIndex == -1) throw new IllegalStateException();
            checkForComodification();

            try {
                indexedSet.remove(lastReturnedIndex);
                if (lastReturnedIndex < cursor) cursor--;
                lastReturnedIndex = -1;
                expectedModCount = indexedSet.modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        final void checkForComodification() {
            if (indexedSet.modCount != expectedModCount) throw new ConcurrentModificationException();
        }
    }

    private static class ListItr<E> extends Itr<E> implements ListIterator<E> {
        ListItr(IndexedSet<E> indexedSet, int index) {
            super(indexedSet);
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public E previous() {
            checkForComodification();
            try {
                E previous = indexedSet.get(cursor - 1);
                cursor--;
                return previous;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        public void set(E o) {
            throw new UnsupportedOperationException();
        }

        public void add(E o) {
            throw new UnsupportedOperationException();
        }
    }
}
