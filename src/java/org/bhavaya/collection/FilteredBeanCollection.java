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
import org.bhavaya.util.Filter;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.util.Collection;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.9 $
 */
public class FilteredBeanCollection<E> extends AuditBeanCollection<E> implements Describeable {
    private BeanCollection<E> sourceBeanCollection;
    private SourceBeanCollectionListener sourceBeanCollectionListener;
    private Filter filter;
    protected volatile boolean inited = false;

    static {
        BeanUtilities.addPersistenceDelegate(FilteredBeanCollection.class, new BhavayaPersistenceDelegate(new String[]{"type", "filter", "sourceBeanCollection"}));
    }

    protected FilteredBeanCollection(Class<E> type) {
        super(type);
    }

    public FilteredBeanCollection(Class<E> type, Filter filter, BeanCollection<E> sourceBeanCollection) {
        this(type);
        this.filter = filter;
        this.sourceBeanCollection = sourceBeanCollection;
    }

    public BeanCollection<E>[] getBeanCollections() {
        return sourceBeanCollection.getBeanCollections();
    }

    protected void setSourceBeanCollection(BeanCollection<E> sourceBeanCollection) {
        this.sourceBeanCollection = sourceBeanCollection;
    }

    public BeanCollection<E> getSourceBeanCollection() {
        return sourceBeanCollection;
    }

    public String getDescription() {
        StringBuffer buffer = new StringBuffer();
        if (sourceBeanCollection instanceof Describeable) {
            Describeable describeable = (Describeable) sourceBeanCollection;
            buffer.append(describeable.getDescription());
        } else {
            buffer.append("Unknown");
        }
        return buffer.toString();
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
        refresh();
    }

    protected void setFilterInternal(Filter filter) {
        this.filter = filter;
    }

    protected void writeAccess() {
        init();
    }

    protected void readAccess() {
        init();
    }

    private void init() {
        // Double checked locking.  According to my research, this is now okay in Java 5+.
        if (inited) return;

        synchronized (sourceBeanCollection) {
            synchronized (this) {
                if (inited) return;
                inited = true;
                initImpl();
            }
        }
    }

    protected void initImpl() {
        sourceBeanCollectionListener = new SourceBeanCollectionListener(sourceBeanCollection, this);
        sourceBeanCollection.addCollectionListener(sourceBeanCollectionListener);
        refresh();
    }

    private void refresh() {
        synchronized (this) {
            clear(false, false);
            addAll(sourceBeanCollection, true, true);
        }
    }

    public synchronized Object clone() {
        return new FilteredBeanCollection<E>(getType(), getFilter(), getSourceBeanCollection());
    }

    protected boolean add(E value, boolean fireCommit, boolean fireCollectionChanged) {
        if (filter.evaluate(value)) {
            return super.add(value, fireCommit, fireCollectionChanged);
        }
        return false;
    }

    protected void add(int index, E value, boolean fireCommit, boolean fireCollectionChanged) {
        if (filter.evaluate(value)) {
            super.add(index, value, fireCommit, fireCollectionChanged);
        }
    }

    public E set(int index, E value, boolean fireCommit) {
        if (filter.evaluate(value)) {
            return super.set(index, value, fireCommit);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected boolean addAll(Collection<? extends E> c, boolean fireCommit, boolean fireCollectionChanged) {
        boolean added = false;

        Object[] snapshot;
        synchronized (c) { // to prevent concurrent modifications
            snapshot = c.toArray(new Object[c.size()]);
        }

        ensureCapacity(snapshot.length);

        for (Object value : snapshot) {
            if (add((E) value, false, false)) {
                added = true;
            }
        }

        if (fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();

        return added;
    }

    private static class SourceBeanCollectionListener extends WeakCollectionListener {
        public SourceBeanCollectionListener(ObservableCollection observableCollection, Object collectionListenerOwner) {
            super(observableCollection, collectionListenerOwner);
        }

        protected void collectionChanged(Object collectionListenerOwner, ListEvent collectionEvent) {
            FilteredBeanCollection beanCollection = (FilteredBeanCollection) collectionListenerOwner;

            int eventType = collectionEvent.getType();
            Object bean = collectionEvent.getValue();

            if (eventType == MapEvent.ALL_ROWS) {
                beanCollection.setFilter(beanCollection.filter);
            } else if (eventType == MapEvent.INSERT) {
                if (beanCollection.filter.evaluate(bean)) {
                    beanCollection.add(bean, false, true);
                }
            } else if (eventType == MapEvent.UPDATE) {
                if (beanCollection.filter.evaluate(bean)) {
                    if (!beanCollection.contains(bean)) {
                        beanCollection.add(bean, false, true);
                    }
                } else {
                    if (beanCollection.contains(bean)) {
                        beanCollection.remove(bean, false, true);
                    }
                }
            } else if (eventType == MapEvent.DELETE) {
                beanCollection.remove(bean, false, true);
            } else if (eventType == MapEvent.COMMIT) {
                beanCollection.fireCollectionChanged(new ListEvent(beanCollection, ListEvent.COMMIT, bean));
            } else {
                throw new RuntimeException("Do not know how to handle collection event of type: " + eventType);
            }
        }
    }

}
