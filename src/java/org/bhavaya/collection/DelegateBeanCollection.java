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
 * Description
 *
 * @author Either Parwy or Dan?
 * @version $Revision: 1.2.26.1 $
 */
public class DelegateBeanCollection implements BeanCollection, RandomAccess, Cloneable {
    private BeanCollection sourceBeanCollection;
    private ArrayList listeners = new ArrayList();

    public DelegateBeanCollection() {
    }

    public DelegateBeanCollection(BeanCollection sourceBeanCollection) {
        setSourceBeanCollection(sourceBeanCollection);
    }

    protected void setSourceBeanCollection(BeanCollection sourceBeanCollection) {
        assert this.sourceBeanCollection == null;
        this.sourceBeanCollection = sourceBeanCollection;
        sourceBeanCollection.addCollectionListener(new SourceBeanCollectionListener(sourceBeanCollection, this));
    }

    protected void readAccess() {
    }

    protected void writeAccess() {
    }

    public BeanCollection[] getBeanCollections() {
        readAccess();
        return sourceBeanCollection.getBeanCollections();
    }

    public Object set(int index, Object element) {
        writeAccess();
        return sourceBeanCollection.set(index, element);
    }

    public Object set(int index, Object element, boolean fireCommit) {
        writeAccess();
        return sourceBeanCollection.set(index, element, fireCommit);
    }

    public boolean add(Object value) {
        writeAccess();
        return sourceBeanCollection.add(value);
    }

    public boolean add(Object value, boolean fireCommit) {
        writeAccess();
        return sourceBeanCollection.add(value, fireCommit);
    }

    public void add(int index, Object value) {
        writeAccess();
        sourceBeanCollection.add(index, value);
    }

    public void add(int index, Object value, boolean fireCommit) {
        writeAccess();
        sourceBeanCollection.add(index, value, fireCommit);
    }

    public boolean addAll(Collection c) {
        writeAccess();
        return sourceBeanCollection.addAll(c);
    }

    public boolean addAll(Collection c, boolean fireCommit) {
        writeAccess();
        return sourceBeanCollection.addAll(c, fireCommit);
    }

    public boolean addAll(int index, Collection c) {
        writeAccess();
        return sourceBeanCollection.addAll(index, c);
    }

    public synchronized boolean addAll(int index, Collection c, boolean fireCommit) {
        writeAccess();
        return sourceBeanCollection.addAll(index, c, fireCommit);
    }

    public boolean remove(Object value) {
        writeAccess();
        return sourceBeanCollection.remove(value);
    }

    public boolean remove(Object value, boolean fireCommit) {
        writeAccess();
        return sourceBeanCollection.remove(value, fireCommit);
    }

    public synchronized Object remove(int index, boolean fireCommit) {
        writeAccess();
        return sourceBeanCollection.remove(index, fireCommit);
    }

    public Object remove(int index) {
        writeAccess();
        return sourceBeanCollection.remove(index);
    }

    public boolean removeAll(Collection c) {
        writeAccess();
        return sourceBeanCollection.removeAll(c);
    }

    public boolean removeAll(Collection c, boolean fireCommit) {
        writeAccess();
        return sourceBeanCollection.removeAll(c, fireCommit);
    }

    public Object getTransactionLock() {
        return this;
    }

    public void clear() {
        writeAccess();
        sourceBeanCollection.clear();
    }

    public void clear(boolean fireCommit) {
        writeAccess();
        sourceBeanCollection.clear(fireCommit);
    }

    public boolean retainAll(Collection c) {
        writeAccess();
        return sourceBeanCollection.retainAll(c);
    }

    public synchronized boolean retainAll(Collection c, boolean fireCommit) {
        writeAccess();
        return sourceBeanCollection.retainAll(c, fireCommit);
    }

    public synchronized int size() {
        readAccess();
        return sourceBeanCollection.size();
    }

    public synchronized boolean isEmpty() {
        readAccess();
        return sourceBeanCollection.isEmpty();
    }

    public synchronized boolean contains(Object o) {
        readAccess();
        return sourceBeanCollection.contains(o);
    }

    public synchronized Iterator iterator() {
        readAccess();
        return sourceBeanCollection.iterator();
    }

    public synchronized Object[] toArray() {
        readAccess();
        return sourceBeanCollection.toArray();
    }

    public synchronized Object[] toArray(Object a[]) {
        readAccess();
        return sourceBeanCollection.toArray(a);
    }

    public synchronized boolean containsAll(Collection c) {
        readAccess();
        return sourceBeanCollection.containsAll(c);
    }

    public synchronized Object get(int index) {
        readAccess();
        return sourceBeanCollection.get(index);
    }

    public synchronized int indexOf(Object o) {
        readAccess();
        return sourceBeanCollection.indexOf(o);
    }

    public synchronized int lastIndexOf(Object o) {
        readAccess();
        return sourceBeanCollection.lastIndexOf(o);
    }

    public synchronized ListIterator listIterator() {
        readAccess();
        return sourceBeanCollection.listIterator();
    }

    public synchronized ListIterator listIterator(int index) {
        readAccess();
        return sourceBeanCollection.listIterator(index);
    }

    public synchronized List subList(int fromIndex, int toIndex) {
        readAccess();
        return sourceBeanCollection.subList(fromIndex, toIndex);
    }

    public synchronized void addCollectionListener(CollectionListener l) {
        writeAccess();
        listeners.add(l);
    }

    public synchronized void removeCollectionListener(CollectionListener l) {
        writeAccess();
        listeners.remove(l);
    }

    public void fireCommit() {
        writeAccess();
        sourceBeanCollection.fireCommit();
    }

    public Class getType() {
        readAccess();
        return sourceBeanCollection.getType();
    }

    private static class SourceBeanCollectionListener extends WeakCollectionListener {
        public SourceBeanCollectionListener(ObservableCollection observableCollection, Object collectionListenerOwner) {
            super(observableCollection, collectionListenerOwner);
        }

        protected void collectionChanged(Object collectionListenerOwner, ListEvent sourceEvent) {
            DelegateBeanCollection beanCollection = (DelegateBeanCollection) collectionListenerOwner;
            ListEvent remappedEvent = new ListEvent(beanCollection, sourceEvent.getType(), sourceEvent.getValue(), sourceEvent.getRow());
            for (int i = 0; i < beanCollection.listeners.size(); i++) {
                CollectionListener listener = (CollectionListener) beanCollection.listeners.get(i);
                listener.collectionChanged(remappedEvent);
            }
        }
    }
}
