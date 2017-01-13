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

import java.lang.reflect.Array;
import java.util.*;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.10.26.1 $
 */
public class DefaultBeanCollection<E> implements BeanCollection<E>, RandomAccess, Cloneable {
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    protected final Object listenerLock = new Object();

    private Class<E> type;
    private List<CollectionListener> collectionListeners;
    private IndexedSet<E> indexedSet;

    public DefaultBeanCollection(Class<E> type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public BeanCollection<E>[] getBeanCollections() {
        return new BeanCollection[]{this};
    }

    public E set(int index, E element) {
        return set(index, element, true);
    }

    public E set(int index, E element, boolean fireCommit) {
        E currentObject = remove(index, false);
        add(index, element, fireCommit);
        return currentObject;
    }

    public boolean add(E value) {
        return add(value, true, true);
    }

    public boolean add(E value, boolean fireCommit) {
        return add(value, fireCommit, true);
    }

    protected synchronized boolean add(E value, boolean fireCommit, boolean fireCollectionChanged) {
        int addedIndex = getIndexedSet().size();
        boolean added = getIndexedSet().add(value);
        if (added && fireCollectionChanged) fireCollectionChanged(new ListEvent<E>(this, ListEvent.INSERT, value, addedIndex));
        if (fireCommit) fireCommit();
        return added;
    }

    public void add(int index, E value) {
        add(index, value, true, true);
    }

    public void add(int index, E value, boolean fireCommit) {
        add(index, value, fireCommit, true);
    }

    public void ensureCapacity(int minCapacity) {
        getIndexedSet().ensureCapacity(minCapacity);
    }

    protected synchronized void add(int index, E value, boolean fireCommit, boolean fireCollectionChanged) {
        int oldIndex = getIndexedSet().indexOf(value);

        if (oldIndex != index) {
            getIndexedSet().add(index, value);
            int eventType = ListEvent.INSERT;

            if (oldIndex >= 0) {
                value = null;
                eventType = ListEvent.ALL_ROWS;
            }

            if (fireCollectionChanged) fireCollectionChanged(new ListEvent<E>(this, eventType, value, index));
            if (fireCommit) fireCommit();
        }
    }

    public boolean addAll(Collection<? extends E> c) {
        return addAll(c, true, true);
    }

    public boolean addAll(Collection<? extends E> c, boolean fireCommit) {
        return addAll(c, fireCommit, true);
    }

    protected synchronized boolean addAll(Collection<? extends E> c, boolean fireCommit, boolean fireCollectionChanged) {
        boolean added = getIndexedSet().addAll(c);
        if (added && fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();
        return added;
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public synchronized boolean addAll(int index, Collection<? extends E> c, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object value) {
        return remove(value, true, true);
    }

    public boolean remove(Object value, boolean fireCommit) {
        return remove(value, fireCommit, true);
    }

    @SuppressWarnings("unchecked")
    protected synchronized boolean remove(Object value, boolean fireCommit, boolean fireCollectionChanged) {
        if (indexedSet == null) return false;
        int oldIndex = getIndexedSet().indexOf(value);
        boolean removedValue = getIndexedSet().remove(value);
        if (removedValue && fireCollectionChanged) fireCollectionChanged(new ListEvent<E>(this, ListEvent.DELETE, (E) value, oldIndex));
        if (fireCommit) fireCommit();
        return removedValue;
    }

    public synchronized E remove(int index, boolean fireCommit) {
        if (indexedSet == null) return null;
        E removedObject = getIndexedSet().remove(index);
        fireCollectionChanged(new ListEvent<E>(this, ListEvent.DELETE, removedObject, index));
        if (fireCommit) fireCommit();
        return removedObject;
    }

    public E remove(int index) {
        if (indexedSet == null) return null;
        return remove(index, true);
    }

    public boolean removeAll(Collection<?> c) {
        return removeAll(c, true, true);
    }

    public boolean removeAll(Collection<?> c, boolean fireCommit) {
        return removeAll(c, fireCommit, true);
    }

    protected synchronized boolean removeAll(Collection<?> c, boolean fireCommit, boolean fireCollectionChanged) {
        if (indexedSet == null) return false;
        boolean removed = getIndexedSet().removeAll(c);
        if (removed && fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();
        return removed;
    }

    public Object getTransactionLock() {
        return this;
    }

    public void clear() {
        clear(true, true);
    }

    public void clear(boolean fireCommit) {
        clear(fireCommit, true);
    }

    protected synchronized void clear(boolean fireCommit, boolean fireCollectionChanged) {
        if (indexedSet == null) return;
        getIndexedSet().clear();
        if (fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public synchronized boolean retainAll(Collection<?> c, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public synchronized int size() {
        if (indexedSet == null) return 0;
        return getIndexedSet().size();
    }

    public synchronized boolean isEmpty() {
        if (indexedSet == null) return true;
        return getIndexedSet().isEmpty();
    }

    public synchronized boolean contains(Object o) {
        if (indexedSet == null) return false;
        return getIndexedSet().contains(o);
    }

    @SuppressWarnings("unchecked")
    public synchronized Iterator<E> iterator() {
        if (indexedSet == null) return Utilities.EMPTY_ITERATOR;
        return getIndexedSet().iterator();
    }

    public synchronized Object[] toArray() {
        if (indexedSet == null) return EMPTY_OBJECT_ARRAY;
        return getIndexedSet().toArray();
    }

    public synchronized <T> T[] toArray(T a[]) {
        if (indexedSet == null) return (T[]) Array.newInstance(a.getClass().getComponentType(), 0);
        return getIndexedSet().toArray(a);
    }

    public synchronized boolean containsAll(Collection<?> c) {
        if (indexedSet == null) return false;
        return getIndexedSet().containsAll(c);
    }

    public synchronized E get(int index) {
        if (indexedSet == null) return null;
        return getIndexedSet().get(index);
    }

    public synchronized int indexOf(Object o) {
        if (indexedSet == null) return -1;
        return getIndexedSet().indexOf(o);
    }

    public synchronized int lastIndexOf(Object o) {
        if (indexedSet == null) return -1;
        return getIndexedSet().lastIndexOf(o);
    }

    public synchronized ListIterator<E> listIterator() {
        return getIndexedSet().listIterator();
    }

    public synchronized ListIterator<E> listIterator(int index) {
        return getIndexedSet().listIterator(index);
    }

    public synchronized List<E> subList(int fromIndex, int toIndex) {
        return getIndexedSet().subList(fromIndex, toIndex);
    }

//----------------Listener stuff------------------------
    public void addCollectionListener(CollectionListener<E> l) {
        synchronized (listenerLock) {
            collectionListeners = Utilities.add(l, getCollectionListeners());
        }
    }

    public synchronized void removeCollectionListener(CollectionListener<E> l) {
        synchronized (listenerLock) {
            collectionListeners = Utilities.remove(l, getCollectionListeners());
        }
    }

    protected void fireCollectionChanged(ListEvent<E> event) {
        // firingCollectionChanged can actually cause side-effect where other
        // objects add/remove listeners, using an iterator would cause ConcurrentModificationExceptions.
        List<CollectionListener> listenersSnapShot;
        synchronized (listenerLock) {
            listenersSnapShot = getCollectionListeners();
        }

        for (CollectionListener<E> collectionListener : listenersSnapShot) {
            collectionListener.collectionChanged(event);
        }
    }

    protected void fireCollectionChanged() {
        fireCollectionChanged(new ListEvent<E>(this, ListEvent.ALL_ROWS, null));
    }

    public void fireCommit() {
        fireCollectionChanged(new ListEvent<E>(this, ListEvent.COMMIT, null));
    }

    public Class<E> getType() {
        return type;
    }

    public synchronized Object clone() {
        DefaultBeanCollection<E> copy = new DefaultBeanCollection<E>(type);
        if (indexedSet != null) {
            for (E e : this) {
                copy.add(e);
            }
        }
        return copy;
    }

    /**
     * Encapsulate access as many DefaultBeanCollections can be constructed that are never actually accessed
     */
    protected List<CollectionListener> getCollectionListeners() {
        if (collectionListeners == null) collectionListeners = new ArrayList<CollectionListener>();
        return collectionListeners;
    }

    /**
     * Encapsulate access as many DefaultBeanCollections can be constructed that are never actually accessed
     */
    private IndexedSet<E> getIndexedSet() {
        if (indexedSet == null) indexedSet = new IndexedSet<E>();
        return indexedSet;
    }

}
