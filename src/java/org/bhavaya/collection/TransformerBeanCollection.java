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
import org.bhavaya.util.Transform;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A BeanCollection that transforms one type to another type.  Note that this class is currently partly broken!
 * <br><br>
 * Currently, transformation is using a Transformer is done inside the add method.  This makes no sense and breaks
 * the contract of what you put in is what you get out.  For example, you can currently do the following.
 * <code><pre>
 *      TransformerBeanCollection tbc = new TransformerBeanCollection(...) // Integer->String
 *      tbc.add(3)
 *      String value = tbc.get(0);    // returns "3"
 * </pre></code>
 * <br>
 * This definitely needs to be addressed.  Until this is addressed, this class cannot be made generic as the syntax
 * immediately highlights the problem.  I imagine that this problem has not been discovered because all usages of this
 * class so far have been to transform events on a source beancollection.  i.e.  All write operations have thus far
 * been derived from the source collection synchroniser.
 *
 * @author
 * @version $Revision: 1.9 $
 */
public class TransformerBeanCollection extends AuditBeanCollection implements Describeable {
    private BeanCollection sourceBeanCollection;
    private Transform valueTransformer;
    private Map transformedValues = new HashMap();
    private SourceBeanCollectionListener sourceBeanCollectionListener;
    private boolean inited = false;

    static {
        BeanUtilities.addPersistenceDelegate(TransformerBeanCollection.class, new BhavayaPersistenceDelegate(new String[]{"type", "sourceBeanCollection", "valueTransformer"}));
    }

    public TransformerBeanCollection(Class type, BeanCollection sourceBeanCollection, final Transform valueTransformer) {
        this(type);
        init(sourceBeanCollection, valueTransformer);
    }

    protected TransformerBeanCollection(Class type) {
        super(type);
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

    protected void init(BeanCollection sourceBeanCollection, final Transform valueTransformer) {
        this.sourceBeanCollection = sourceBeanCollection;
        this.valueTransformer = valueTransformer;
        this.sourceBeanCollectionListener = new SourceBeanCollectionListener(sourceBeanCollection, this);
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
        sourceBeanCollection.addCollectionListener(sourceBeanCollectionListener);
        refresh();
    }

    protected void refresh() {
        synchronized (sourceBeanCollection) {
            clear(false, false);
            addAll(sourceBeanCollection, false, false);
        }
    }

    public BeanCollection getSourceBeanCollection() {
        return sourceBeanCollection;
    }

    public BeanCollection[] getBeanCollections() {
        return sourceBeanCollection.getBeanCollections();
    }

    public Transform getValueTransformer() {
        return valueTransformer;
    }

    protected Map getTransformedValues() {
        return transformedValues;
    }

    public void clear() {
        transformedValues.clear();
        super.clear();
    }

    protected boolean add(Object value, boolean fireCommit, boolean fireCollectionChanged) {
        Object transformedValue = transformValue(value);
        return super.add(transformedValue, fireCommit, fireCollectionChanged);
    }

    protected void add(int index, Object value, boolean fireCommit, boolean fireCollectionChanged) {
        Object transformedValue = transformValue(value);
        super.add(index, transformedValue, fireCommit, fireCollectionChanged);
    }

    public Object set(int index, Object value, boolean fireCommit) {
        Object transformedValue = transformValue(value);
        return super.set(index, transformedValue, fireCommit);
    }

    protected boolean addAll(Collection c, boolean fireCommit, boolean fireCollectionChanged) {
        boolean added = false;

        Object[] snapshot;
        synchronized (c) { // to prevent concurrent modifications
            snapshot = c.toArray(new Object[c.size()]);
        }

        ensureCapacity(snapshot.length);

        for (int i = 0; i < snapshot.length; i++) {
            Object value = snapshot[i];
            if (add(value, false, false)) {
                added = true;
            }
        }

        if (fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();

        return added;
    }

    protected boolean remove(Object value, boolean fireCommit, boolean fireCollectionChanged) {
        Object transformedValue = transformValue(value);
        transformedValues.remove(value);
        return super.remove(transformedValue, fireCommit, fireCollectionChanged);
    }

    protected boolean removeAll(Collection c, boolean fireCommit, boolean fireCollectionChanged) {
        boolean removed = false;

        Object[] snapshot;
        synchronized (c) { // to prevent concurrent modifications
            snapshot = c.toArray(new Object[c.size()]);
        }

        for (int i = 0; i < snapshot.length; i++) {
            Object value = snapshot[i];
            if (remove(value, false, false)) {
                removed = true;
            }
        }

        if (fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();

        return removed;
    }

    protected Object transformValue(Object value) {
        Object transformedValue = transformedValues.get(value);
        if (transformedValue == null) {
            transformedValue = valueTransformer.execute(value);
            transformedValues.put(value, transformedValue);
        }
        return transformedValue;
    }

    protected static class SourceBeanCollectionListener extends WeakCollectionListener {
        public SourceBeanCollectionListener(ObservableCollection observableCollection, Object collectionListenerOwner) {
            super(observableCollection, collectionListenerOwner);
        }

        protected void collectionChanged(Object collectionListenerOwner, ListEvent collectionEvent) {
            TransformerBeanCollection beanCollection = (TransformerBeanCollection) collectionListenerOwner;
            int eventType = collectionEvent.getType();
            Object bean = collectionEvent.getValue();

            if (eventType == ListEvent.INSERT) {
                beanCollection.add(bean, false, true);
            } else if (eventType == ListEvent.UPDATE) {
                Object transformedValue = bean != null ? beanCollection.transformValue(bean) : null;
                int index = beanCollection.indexOf(transformedValue);
                beanCollection.fireCollectionChanged(new ListEvent(beanCollection, ListEvent.UPDATE, transformedValue, index));
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
