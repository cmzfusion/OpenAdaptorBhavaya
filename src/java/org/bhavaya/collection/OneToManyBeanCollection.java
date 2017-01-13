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
import org.bhavaya.util.Generic;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Description
 *
 * @author
 * @version $Revision: 1.9 $
 */
public class OneToManyBeanCollection extends AuditBeanCollection implements Describeable {
    private String collectionProperty;
    private BeanCollection sourceBeanCollection;
    private List childBeanCollections;
    private ChildBeanCollectionListener childBeanCollectionListener;
    private SourceBeanCollectionListener sourceBeanCollectionListener;
    private boolean inited = false;

    static {
        BeanUtilities.addPersistenceDelegate(OneToManyBeanCollection.class, new BhavayaPersistenceDelegate(new String[]{"type", "sourceBeanCollection", "collectionProperty"}));
    }

    public OneToManyBeanCollection(Class type, BeanCollection sourceBeanCollection, String collectionProperty) {
        super(type);
        this.sourceBeanCollection = sourceBeanCollection;
        this.collectionProperty = collectionProperty;
        childBeanCollections = new ArrayList();
        childBeanCollectionListener = new ChildBeanCollectionListener(childBeanCollections, this);
        sourceBeanCollectionListener = new SourceBeanCollectionListener(sourceBeanCollection, this);
    }

    public BeanCollection getSourceBeanCollection() {
        return sourceBeanCollection;
    }

    public BeanCollection[] getBeanCollections() {
        return sourceBeanCollection.getBeanCollections();
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
            sourceBeanCollection.addCollectionListener(sourceBeanCollectionListener);
            refresh();
        }
    }

    private void refresh() {
        synchronized (sourceBeanCollection) {
            clear(false, false);
            addAll(sourceBeanCollection, false, false);
        }
    }

    public String getCollectionProperty() {
        return collectionProperty;
    }

    protected boolean add(Object value, boolean fireCommit, boolean fireCollectionChanged) {
        BeanCollection childBeanCollection = (BeanCollection) Generic.get(value, collectionProperty);
        return addChildBeanCollection(childBeanCollection, fireCommit, fireCollectionChanged);
    }

    protected void add(int index, Object value, boolean fireCommit, boolean fireCollectionChanged) {
        throw new UnsupportedOperationException();
    }

    public Object set(int index, Object value, boolean fireCommit) {
        throw new UnsupportedOperationException();
    }

    private boolean addChildBeanCollection(BeanCollection childBeanCollection, boolean fireCommit, boolean fireCollectionChanged) {
        if (childBeanCollection == null) return false;
        boolean added;

        synchronized (childBeanCollection) {
            childBeanCollection.addCollectionListener(childBeanCollectionListener);
            childBeanCollections.add(childBeanCollection);
            added = addChildMemberBeans(childBeanCollection, false, fireCollectionChanged);
        }

        if (fireCommit) fireCommit();

        return added;
    }

    private boolean addChildMemberBeans(BeanCollection childBeanCollection, boolean fireCommit, boolean fireCollectionChanged) {
        boolean added = false;

        synchronized (childBeanCollection) {
            ensureCapacity(childBeanCollection.size());
            for (Iterator iterator = childBeanCollection.iterator(); iterator.hasNext();) {
                Object childMemberBean = iterator.next();
                if (addChildMemberBean(childMemberBean, fireCommit, fireCollectionChanged)) {
                    added = true;
                }
            }
        }
        return added;
    }

    private boolean addChildMemberBean(Object childMemberBean, boolean fireCommit, boolean fireCollectionChanged) {
        return super.add(childMemberBean, fireCommit, fireCollectionChanged);
    }

    protected boolean addAll(Collection c, boolean fireCommit, boolean fireCollectionChanged) {
        boolean added = false;

        synchronized (c) {
            for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                Object value = iterator.next();
                if (add(value, false, false)) {
                    added = true;
                }
            }
        }

        if (added && fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();

        return added;
    }

    protected boolean remove(Object value, boolean fireCommit, boolean fireCollectionChanged) {
        BeanCollection childBeanCollection = (BeanCollection) Generic.get(value, collectionProperty);
        return removeChildBeanCollection(childBeanCollection, fireCommit, fireCollectionChanged);
    }

    private boolean removeChildBeanCollection(BeanCollection childBeanCollection, boolean fireCommit, boolean fireCollectionChanged) {
        if (childBeanCollection == null) return false;
        boolean removed;

        synchronized (childBeanCollection) {
            childBeanCollection.removeCollectionListener(childBeanCollectionListener);
            childBeanCollections.remove(childBeanCollection);
            removed = removeChildMemberBeans(childBeanCollection, false, fireCollectionChanged);
        }

        if (fireCommit) fireCommit();

        return removed;
    }

    private boolean removeChildMemberBeans(BeanCollection childBeanCollection, boolean fireCommit, boolean fireCollectionChanged) {
        boolean removed = false;

        synchronized (childBeanCollection) {
            for (Iterator iterator = childBeanCollection.iterator(); iterator.hasNext();) {
                Object childMemberBean = iterator.next();
                if (removeChildMemberBean(childMemberBean, fireCommit, fireCollectionChanged)) {
                    removed = true;
                }
            }
        }
        return removed;
    }

    private boolean removeChildMemberBean(Object childMemberBean, boolean fireCommit, boolean fireCollectionChanged) {
        return super.remove(childMemberBean, fireCommit, fireCollectionChanged);
    }

    protected boolean removeAll(Collection c, boolean fireCommit, boolean fireCollectionChanged) {
        boolean removed = false;

        synchronized (c) {
            for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                Object value = iterator.next();
                if (remove(value, false, false)) {
                    removed = true;
                }
            }
        }

        if (removed && fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();

        return removed;
    }

    protected void clear(boolean fireCommit, boolean fireCollectionChanged) {
        synchronized (childBeanCollections) {
            for (Iterator iterator = childBeanCollections.iterator(); iterator.hasNext();) {
                BeanCollection childBeanCollection = (BeanCollection) iterator.next();
                childBeanCollection.removeCollectionListener(childBeanCollectionListener);
                iterator.remove();
            }
        }

        clearSuper(fireCommit, fireCollectionChanged);
    }

    private void clearSuper(boolean fireCommit, boolean fireCollectionChanged) {
        super.clear(fireCommit, fireCollectionChanged);
    }

    private static class ChildBeanCollectionListener extends WeakCollectionListener {
        public ChildBeanCollectionListener(List observableCollections, Object collectionListenerOwner) {
            super(observableCollections, collectionListenerOwner);
        }

        protected void collectionChanged(Object collectionListenerOwner, ListEvent collectionEvent) {
            OneToManyBeanCollection beanCollection = (OneToManyBeanCollection) collectionListenerOwner;

            int eventType = collectionEvent.getType();
            Object childMemberBean = collectionEvent.getValue();

            if (eventType == ListEvent.INSERT) {
                beanCollection.addChildMemberBean(childMemberBean, false, true);
            } else if (eventType == ListEvent.UPDATE) {
                throw new UnsupportedOperationException("Bean collection update not implemented yet");
            } else if (eventType == ListEvent.DELETE) {
                beanCollection.removeChildMemberBean(childMemberBean, false, true);
            } else if (eventType == ListEvent.COMMIT) {
                beanCollection.fireCommit();
            } else if (eventType == ListEvent.ALL_ROWS) {
                // This does not use refresh, as we do not want to add/remove listeners onto the childBeanCollections
                // as they have already been added.
                synchronized (beanCollection.sourceBeanCollection) {
                    beanCollection.clearSuper(false, false);
                    for (Iterator iterator = beanCollection.sourceBeanCollection.iterator(); iterator.hasNext();) {
                        Object o = iterator.next();
                        BeanCollection childBeanCollection = (BeanCollection) Generic.get(o, beanCollection.collectionProperty);
                        beanCollection.addChildMemberBeans(childBeanCollection, false, false);
                    }
                }
                beanCollection.fireCollectionChanged();
            } else {
                throw new RuntimeException("Do not know how to handle collection event of type: " + eventType);
            }
        }
    }

    private static class SourceBeanCollectionListener extends WeakCollectionListener {
        public SourceBeanCollectionListener(ObservableCollection observableCollection, Object collectionListenerOwner) {
            super(observableCollection, collectionListenerOwner);
        }

        protected void collectionChanged(Object collectionListenerOwner, ListEvent collectionEvent) {
            OneToManyBeanCollection beanCollection = (OneToManyBeanCollection) collectionListenerOwner;
            int eventType = collectionEvent.getType();
            Object bean = collectionEvent.getValue();

            if (eventType == ListEvent.INSERT) {
                beanCollection.add(bean, false, true);
            } else if (eventType == ListEvent.UPDATE) {
                // not handled, assume that you do not get replace the childProperty of bean with another beanCollection
            } else if (eventType == ListEvent.DELETE) {
                beanCollection.remove(bean, false, true);
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
