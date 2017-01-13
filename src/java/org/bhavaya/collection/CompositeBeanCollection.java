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
import org.bhavaya.util.Describeable;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public class CompositeBeanCollection extends AuditBeanCollection implements Describeable {
    private BeanCollection[] beanCollections;
    private SourceBeanCollectionListener sourceBeanCollectionListener;
    private boolean inited = false;

    static {
        BeanUtilities.addPersistenceDelegate(CompositeBeanCollection.class, new BhavayaPersistenceDelegate(new String[]{"type", "beanCollections"}));
    }

    protected CompositeBeanCollection(Class type) {
        super(type);
    }

    public CompositeBeanCollection(Class type, BeanCollection[] beanCollections) {
        this(type);
        this.beanCollections = beanCollections;
    }

    protected void setBeanCollections(BeanCollection[] beanCollections) {
        this.beanCollections = beanCollections;
    }

    public BeanCollection[] getBeanCollections() {
        List beanCollectionSources = new ArrayList();
        for (int i = 0; i < beanCollections.length; i++) {
            BeanCollection beanCollection = beanCollections[i];
            BeanCollection[] beanCollectionsForSource = beanCollection.getBeanCollections();
            for (int j = 0; j < beanCollectionsForSource.length; j++) {
                beanCollectionSources.add(beanCollectionsForSource[j]);
            }
        }
        return (BeanCollection[]) beanCollectionSources.toArray(new BeanCollection[beanCollectionSources.size()]);
    }

    public String getDescription() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < beanCollections.length; i++) {
            if (i > 0) buffer.append("\n");
            BeanCollection beanCollection = beanCollections[i];
            if (beanCollection instanceof Describeable) {
                Describeable describeable = (Describeable) beanCollection;
                buffer.append(describeable.getDescription());
            } else {
                buffer.append("Unknown");
            }
        }
        return buffer.toString();
    }

    protected void writeAccess() {
        init();
    }

    protected void readAccess() {
        init();
    }

    private void init() {
        synchronized (this) {
            if (inited) return;
            inited = true;
            initImpl();
        }
    }

    protected void initImpl() {
        sourceBeanCollectionListener = new SourceBeanCollectionListener(beanCollections, this);
        for (int i = 0; i < beanCollections.length; i++) {
            BeanCollection beanCollection = beanCollections[i];
            beanCollection.addCollectionListener(sourceBeanCollectionListener);
        }
        refresh();
    }

    private void refresh() {
        synchronized (this) {
            clear(false, false);
            for (int i = 0; i < beanCollections.length; i++) {
                BeanCollection beanCollection = beanCollections[i];
                synchronized (beanCollection) {
                    addAll(beanCollection, false, false);
                }
            }
        }
    }

    public synchronized Object clone() {
        return new CompositeBeanCollection(getType(), beanCollections);
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean add(Object value, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object element, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection c, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object value, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public Object remove(int index) {
        throw new UnsupportedOperationException();
    }

    public Object remove(int index, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public void clear(boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public Object set(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public Object set(int index, Object element, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    public ListIterator listIterator() {
        throw new UnsupportedOperationException();
    }

    public ListIterator listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    public List subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    private static class SourceBeanCollectionListener extends WeakCollectionListener {
        public SourceBeanCollectionListener(ObservableCollection[] observableCollections, Object collectionListenerOwner) {
            super(observableCollections, collectionListenerOwner);
        }

        protected void collectionChanged(Object collectionListenerOwner, ListEvent collectionEvent) {
            CompositeBeanCollection beanCollection = (CompositeBeanCollection) collectionListenerOwner;
            int eventType = collectionEvent.getType();
            Object bean = collectionEvent.getValue();

            if (eventType == ListEvent.INSERT) {
                beanCollection.add(bean, false, true);
            } else if (eventType == ListEvent.UPDATE) {
                int row;
                synchronized (beanCollection) {
                    row = beanCollection.indexOf(bean);
                }
                beanCollection.fireCollectionChanged(new ListEvent(beanCollection, ListEvent.UPDATE, bean, row));
            } else if (eventType == ListEvent.DELETE) {
                boolean stillContained = false;
                synchronized (beanCollection) {
                    for (int i = 0; i < beanCollection.beanCollections.length; i++) {
                        BeanCollection sourceBeanCollection = beanCollection.beanCollections[i];
                        if (sourceBeanCollection.contains(bean)) {
                            stillContained = true;
                            break;
                        }
                    }
                }
                if (!stillContained) {
                    beanCollection.remove(bean, false, true);
                }
            } else if (eventType == ListEvent.COMMIT) {
                beanCollection.fireCommit();
            } else if (eventType == ListEvent.ALL_ROWS) {
                beanCollection.refresh();
                beanCollection.fireCollectionChanged();
            } else {
                throw new RuntimeException("Do not know how to handle collection event of type: " + eventType);
            }
        }
    }
}
