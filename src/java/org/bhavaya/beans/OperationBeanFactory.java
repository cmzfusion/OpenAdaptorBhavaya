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

import org.bhavaya.beans.criterion.BasicCriterion;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.beans.criterion.EnumerationCriterion;
import org.bhavaya.collection.Association;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.Transform;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This beanFactory loads data by executing an operation on a target.
 * The operation is determined by the index and the key forms the arguments.
 * Subclasses of OperationBeanFactory implement getTarget.
 * If a transform is specified for the operation, this is applied to the value returned by executing the operation on the target.
 * If a transform is specified for the type, this is then applied also applied.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public abstract class OperationBeanFactory extends DefaultBeanFactory {
    private static final Log log = Log.getCategory(OperationBeanFactory.class);

    static {
        BeanUtilities.addPersistenceDelegate(OperationBeanFactory.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                BeanFactory oldBeanFactory = (BeanFactory) oldInstance;
                return new Expression(oldInstance, BeanFactory.class, "getInstance", new Object[]{oldBeanFactory.getType(), oldBeanFactory.getDataSourceName()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    private Transform transform;
    private Object[] emptyBeanArray;

    protected abstract Object getTarget();

    public OperationBeanFactory(Class type, String dataSourceName) throws Exception {
        super(type, dataSourceName);
        transform = getSchema().getTransform();
        emptyBeanArray = (Object[]) Array.newInstance(getSchema().getType(), 0);
    }

    protected Object load(Object key, String indexName) {
        Operation operation = getOperationByIndex(indexName);
        if (operation == null) {
            log.info(logPrefix + "no operation for index: " + indexName);
            return super.load(key, indexName);
        }

        synchronized (getLock()) {
            Association store = getStore(indexName);
            Object bean = store.get(key);
            if (bean != null || store.containsKey(key)) return bean;

            Object object = load(key, operation);
            object = put(key, indexName, object, operation);
            return object;
        }
    }

    protected Operation getOperationByIndex(String indexName) {
        return getSchema().getOperationGroup().getOperationByIndex(indexName); //one-to-one relationship between the operation and an index
    }

    private Object load(Object key, Operation operation) {
        log.info(logPrefix + "executing: " + operation.getName() + " for key: " + key);

        Object object = null;
        try {
            Expression expression = new Expression(getTarget(), operation.getName(), operation.getArguments(key));
            object = expression.getValue();
        } catch (Exception e) {
            log.error(e);
        }
        if (operation.getTransform() != null) object = operation.getTransform().execute(object);
        log.info(logPrefix + "returned: " + object);
        return object;
    }


    private Object put(Object key, String indexName, Object object, Operation operation) {
        Index index = operation.getIndex();
        Association primaryStore = getStore(null);

        Object insertedValue;

        if (index != null && index.isUnique()) {
            Object bean = object;
            if (transform != null) bean = transform.execute(bean);

            if (bean != null) {
                Object primaryKey = createKey(bean, null);

                // Replace the bean with one that have already been added to the beanFactory, to prevent duplicate beans.
                Object existingBean = primaryStore.get(primaryKey);
                if (existingBean != null) {
                    bean = existingBean;
                } else {
                    primaryStore.put(primaryKey, bean);
                }
            }

            // Do this after inserting into primary store, as the bean may be modified such as removing duplicate beans.
            Association store = getStore(indexName);
            store.put(key, bean);

            if (bean != null && isBeanLifeCycleable()) {
                ((LifeCycle) bean).init();
            }

            insertedValue = bean;
        } else if (index != null && !index.isUnique()) {
            Object[] beans = (Object[]) object;
            BeanCollection beanCollection = new DefaultBeanCollection(getType());

            if (beans != null) {
                if (transform != null) {
                    Object[] transformedBeans = new Object[beans.length];
                    for (int i = 0; i < beans.length; i++) {
                        transformedBeans[i] = transform.execute(beans[i]);
                    }
                    beans = transformedBeans;
                }

                for (int i = 0; i < beans.length; i++) {
                    Object primaryKey = createKey(beans[i], null);
                    // Replace the elements with ones that have already been added to the beanFactory, to prevent duplicate beans.
                    Object existingBeanArrayElement = primaryStore.get(primaryKey);
                    if (existingBeanArrayElement != null) {
                        beans[i] = existingBeanArrayElement;
                    } else {
                        primaryStore.put(primaryKey, beans[i]);
                    }

                    beanCollection.add(beans[i]);
                }
            }

            Association store = getStore(indexName);
            store.put(key, beanCollection);

            if (isBeanLifeCycleable()) {
                for (Iterator iterator = beanCollection.iterator(); iterator.hasNext();) {
                    LifeCycle bean = (LifeCycle) iterator.next();
                    if (bean != null) bean.init();
                }
            }

            insertedValue = beanCollection;
        } else {
            Object bean = object;
            if (transform != null) bean = transform.execute(bean);
            primaryStore.put(key, bean);

            if (bean != null && isBeanLifeCycleable()) {
                ((LifeCycle) bean).init();
            }

            insertedValue = bean;
        }

        return insertedValue;
    }

    public Object[] getObjects(CriterionGroup criterionGroup) {
        Criterion[] criteria = criterionGroup.getCriteria();

        if (criteria.length != 1) {
            String message = logPrefix + "can only handle criterionGroups with one criterion";
            log.error(message);
            return emptyBeanArray;
        }

        BasicCriterion criterion = (BasicCriterion) criteria[0];
        String operator = criterion.getOperator();

        if (operator.equals(Criterion.OPERATOR_EQUALS)) {
            String indexName = criterion.getId();  // making assumption that the criterionId maps to an indexName
            Object key = criterion.getRightOperand();
            return getObjects(key, indexName);
        } else if (operator.equals(EnumerationCriterion.ENUM_IN_OPERATION)) {
            String indexName = criterion.getId();  // making assumption that the criterionId maps to an indexName
            EnumerationCriterion.EnumElement[] enumElements = (EnumerationCriterion.EnumElement[]) criterion.getRightOperand();
            List objects = new ArrayList();
            for (int i = 0; i < enumElements.length; i++) {
                EnumerationCriterion.EnumElement enumElement = enumElements[i];
                Object key = enumElement.getId();
                objects.addAll(Arrays.asList(getObjects(key, indexName)));
            }
            Object[] array = (Object[]) Array.newInstance(getSchema().getType(), objects.size());
            return objects.toArray(array);
        } else {
            String message = logPrefix + "cannot only handle criterion using operator: " + operator;
            log.error(message);
            return emptyBeanArray;
        }
    }

    private Object[] getObjects(Object key, String indexName) {
        Object object = get(key, indexName);

        if (object == null) {
            return emptyBeanArray;
        } else if (object instanceof BeanCollection) {
            BeanCollection beanCollection = (BeanCollection) object;
            Object[] array = (Object[]) Array.newInstance(getSchema().getType(), beanCollection.size());
            return beanCollection.toArray(array);
        } else {
            Object[] array = (Object[]) Array.newInstance(getSchema().getType(), 1);
            array[0] = object;
            return array;
        }
    }
}
