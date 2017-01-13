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
import org.bhavaya.util.Transform;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.util.HashMap;
import java.util.Map;

/**
 * This TransformerBeanCollection routes the events back to the source collection to keep them
 * in synchronization. This syncronization only works, if the transformer works in both ways without loosing information.
 *
 * @author
 * @version $Revision: 1.3 $
 */
public class SynchronizedTransformerBeanCollection extends TransformerBeanCollection {
    private Transform valueRetriever;
    private Map transformedValues2Values = new HashMap();
    private SourceBeanCollectionListener sourceBeanCollectionListener;

    static {
        BeanUtilities.addPersistenceDelegate(SynchronizedTransformerBeanCollection.class, new BhavayaPersistenceDelegate(new String[]{"type", "sourceBeanCollection", "valueTransformer", "valueRetriever"}));
    }

    public SynchronizedTransformerBeanCollection(Class type, BeanCollection sourceBeanCollection, final Transform valueTransformer, final Transform valueRetriever) {
        this(type);
        init(sourceBeanCollection, valueTransformer, valueRetriever);
    }

    protected SynchronizedTransformerBeanCollection(Class type) {
        super(type);
    }

    protected void init(BeanCollection sourceBeanCollection, final Transform valueTransformer, final Transform valueRetriever) {
        super.init(sourceBeanCollection,valueTransformer);
        this.valueRetriever = valueRetriever;
        this.sourceBeanCollectionListener = new SourceBeanCollectionListener(sourceBeanCollection, this);
    }

    protected void initImpl() {
        getSourceBeanCollection().addCollectionListener(sourceBeanCollectionListener);
        super.refresh();
    }

    public Transform getValueRetriever() {
        return valueRetriever;
    }

    public void clear() {
        transformedValues2Values.clear();
        super.clear();
    }

    protected boolean remove(Object value, boolean fireCommit, boolean fireCollectionChanged) {
        Object transformedValue = transformValue(value);
        transformedValues2Values.remove(transformedValue);
        return super.remove(value, fireCommit, fireCollectionChanged);
    }

    protected void fireCommit(boolean own) {
        super.fireCommit();
    }

    public boolean add(Object transformedValue, boolean fireCommit) {
        Object value = retrieveValue(transformedValue);
        return getSourceBeanCollection().add(value, true);
    }

    public boolean remove(Object transformedValue, boolean fireCommit) {
        Object value = retrieveValue(transformedValue);
        return getSourceBeanCollection().remove(value, true);
    }

    public boolean remove(Object transformedValue) {
        Object value = retrieveValue(transformedValue);
        return getSourceBeanCollection().remove(value, true);
    }

    public void fireCommit() {
        getSourceBeanCollection().fireCommit();
    }

    private Object retrieveValue(Object transformedValue) {
        Object value = transformedValues2Values.get(transformedValue);
        if (value == null) {
            value = valueRetriever.execute(transformedValue);
            getTransformedValues().put(value, transformedValue);
            transformedValues2Values.put(transformedValue, value);
        }
        return value;
    }

    private static class SourceBeanCollectionListener extends TransformerBeanCollection.SourceBeanCollectionListener {
        public SourceBeanCollectionListener(ObservableCollection observableCollection, Object collectionListenerOwner) {
            super(observableCollection, collectionListenerOwner);
        }

        protected void collectionChanged(Object collectionListenerOwner, ListEvent collectionEvent) {
            SynchronizedTransformerBeanCollection beanCollection = (SynchronizedTransformerBeanCollection) collectionListenerOwner;
            int eventType = collectionEvent.getType();

            if (eventType == ListEvent.COMMIT) {
                beanCollection.fireCommit(true);
            } else {
                super.collectionChanged(collectionListenerOwner,collectionEvent);
            }
        }
    }
}
