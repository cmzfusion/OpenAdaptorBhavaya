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

package org.bhavaya.db;

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.Schema;
import org.bhavaya.beans.TabularData;
import org.bhavaya.beans.TabularDataBeanFactory;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.Association;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.Log;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.lang.reflect.Array;
import java.util.Collection;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.10 $
 */
public class StoredProcedureBeanFactory extends TabularDataBeanFactory {
    static {
        BeanUtilities.addPersistenceDelegate(StoredProcedureBeanFactory.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                BeanFactory oldBeanFactory = (BeanFactory) oldInstance;
                return new Expression(oldInstance, BeanFactory.class, "getInstance", new Object[]{oldBeanFactory.getType(), oldBeanFactory.getDataSourceName()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    private static final Log log = Log.getCategory(StoredProcedureBeanFactory.class);
    private StoredProcedure storedProcedure;

    public StoredProcedureBeanFactory(Class type, String dataSourceName) {
        super(type, dataSourceName);
        if (dataSourceName == null) {
            throw new RuntimeException("DataSource is null");
        }
        this.storedProcedure = getSchema().getStoredProcedure();
    }

    public Object[] getObjects(CriterionGroup criterionGroup) {
        throw new UnsupportedOperationException();
    }

    public Object[] getObjects(String storedProcedureToExecute) {
        TabularDataToBeanFactoryTransformer transformer = load(storedProcedureToExecute);
        Collection beans = transformer.getBeans();
        Class type = Schema.getInstance(this.getType()).getType(); // can't get the generated type, because beans contains subclass instances which do not extends the generated type
        Object[] array = (Object[]) Array.newInstance(type, beans.size());
        return beans.toArray(array);
    }

    protected Object load(Object key, String indexName) {
        String storedProcedureToExecute = getStoredProcedureToExecute(key, indexName);
        load(storedProcedureToExecute);
        Association store = getStore(indexName);
        Object bean = store.get(key);
        return bean;
    }

    private TabularDataToBeanFactoryTransformer load(String storedProcedureToExecute) {
        TabularDataToBeanFactoryTransformer transformer = null;
        TabularData tabularData = null;

        pushBeanFactoryLoadStack(this);
        try {
            synchronized (getLock()) {
                try {
                    log.info(logPrefix + "executing sql: " + storedProcedureToExecute);
                    long startTime = System.currentTimeMillis();
                    tabularData = new ResultSetTabularData(getDataSourceName(), storedProcedureToExecute, TabularData.ROW_TYPE_SELECT);
                    transformer = new TabularDataToBeanFactoryTransformer(this, tabularData, null);
                    transformer.firstPassInflate();
                    log.info(logPrefix + "finished load for sql: " + storedProcedureToExecute + " in " + (System.currentTimeMillis() - startTime) + " millis");
                } finally {
                    if (tabularData != null) tabularData.close(); // TabularData may already be closed, but repeat in finally block in case an exception was thrown and it was not closed.
                }
            }
            transformer.inflateBeansToFindLater();
            transformer.secondPassInflate();
            transformer.fireLifeCycleCallbacks();

        } catch (Exception e) {
            log.error(logPrefix + "error executing sql: " + storedProcedureToExecute, e);
            throw new RuntimeException(e);
        } finally {
            if (transformer != null) transformer.removeFromPartiallyInflatedMap();
            popBeanFactoryLoadStack(this);
        }

        return transformer;
    }

    protected void clearImpl() {
    }

    private String getStoredProcedureToExecute(Object key, String indexName) {
        return storedProcedure.getStoredProcedureForKey(getKeyColumns(indexName), key);
    }
}
