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

import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.*;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.Generic;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public class DefaultBeanFactory extends BeanFactory {
    static {
        BeanUtilities.addPersistenceDelegate(DefaultBeanFactory.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                BeanFactory oldBeanFactory = (BeanFactory) oldInstance;
                return new Expression(oldInstance, BeanFactory.class, "getInstance", new Object[]{oldBeanFactory.getType(), oldBeanFactory.getDataSourceName()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    public DefaultBeanFactory(Class type, String dataSourceName) {
        super(type, dataSourceName);
    }

    public Object[] getObjects(CriterionGroup criterionGroup) {
        synchronized (getLock()) {
            List matchingValues = new ArrayList();
            for (Iterator iterator = values().iterator(); iterator.hasNext();) {
                Object o = iterator.next();
                if (o != null && criterionGroup.evaluate(o)) {
                    matchingValues.add(o);
                }
            }
            Object[] array = (Object[]) Array.newInstance(getSchema().getType(), matchingValues.size());
            return matchingValues.toArray(array);
        }
    }

    protected Object load(Object key, String indexName) {
        return null;
    }

    protected Object createKey(Object bean, String indexName) {
        Column[] keyColumns = getKeyColumns(indexName);
        int numberOfComponents = keyColumns.length;

        Object key = null;

        if (numberOfComponents > 1) {
            key = new EfficientArrayList(numberOfComponents);
        }

        if (bean instanceof BeanCollection) {
            BeanCollection beanCollection = (BeanCollection) bean;
            if (beanCollection.size() > 0) {
                bean = beanCollection.get(0);
            } else {
                return null;
            }
        }

        for (int i = 0; i < numberOfComponents; i++) {
            String keyColumnName = keyColumns[i].getName();
            Object keyValue = Generic.get(bean, keyColumnName);

            if (keyValue != null) {
                Class keyComponentClass = keyValue.getClass();
                if (Schema.hasInstance(keyComponentClass)) {
                    String dataSourceName = getDataSourceNameForType(keyComponentClass);
                    keyValue = BeanFactory.getInstance(keyComponentClass, dataSourceName).getKeyForValue(keyValue);
                }
            }

            if (numberOfComponents == 1) {
                key = keyValue;
            } else {
                ((List) key).add(keyValue);
            }
        }
        return key;
    }

    private String getDataSourceNameForType(Class keyComponentClass) {
        String dataSourceName;
        Schema schemaForKeyComponent = Schema.getInstance(keyComponentClass);
        if (schemaForKeyComponent.getBeanFactoryType() == this.getClass()) {
            dataSourceName = getDataSourceName();
        } else {
            dataSourceName = schemaForKeyComponent.getDefaultDataSourceName();
        }
        return dataSourceName;
    }

    protected void clearImpl() {
    }

    protected Column[] getKeyColumns(String indexName) {
        if (indexName == null) {
            return getSchema().getPrimaryKey();
        } else {
            return getSchema().getIndex(indexName).getColumns();
        }
    }

    protected Association getDefaultStore(String indexName) {
        if (getSchema().isDefaultReferenceType()) { // if not set then always use strong reference caches
            // overriding this prevents use of ReferenceAssociations, we dont want entries to be garbage collected
            return new SynchronizedAssociation(new DefaultAssociation(new LinkedHashMap()));
        } else {
            return super.getDefaultStore(indexName);
        }
    }

}
