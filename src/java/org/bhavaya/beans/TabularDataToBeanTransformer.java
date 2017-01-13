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

import org.bhavaya.util.Log;
import org.bhavaya.util.Type;

import java.util.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public class TabularDataToBeanTransformer {
    private static final Log log = Log.getCategory(TabularDataToBeanTransformer.class);

    private TabularData tabularData;
    private List beans;
    private Map cachedColumnValues;
    private Class beanClass;
    private Set excludeProperties;
    private Schema schema;
    private String logPrefix;
    private Type derivedPropertyColumnsType;

    public TabularDataToBeanTransformer(Class beanClass, TabularData tabularData, Set excludeProperties) {
        this.tabularData = tabularData;
        this.beanClass = beanClass;
        this.excludeProperties = excludeProperties;
        schema = Schema.getInstance(beanClass);
        logPrefix = "";
        derivedPropertyColumnsType = schema.getDerivedPropertyColumnsType();
    }

    public void process() throws Exception {
        firstPassInflate();
        secondPassInflate();
    }

    private void firstPassInflate() throws Exception {
        long startTime = System.currentTimeMillis();

        int numberOfRows = 0;
        TabularData.Row tabularDataRow = tabularData.next();
        while (tabularDataRow != null) {
            numberOfRows++;
            firstPassInflateRow(tabularDataRow);
            tabularDataRow = tabularData.next();
            if (numberOfRows > 0 && numberOfRows % 50 == 0) {
                if (log.isDebug()) log.debug(logPrefix + "done " + numberOfRows + " first pass inflations");
                Thread.yield();
            }
        }

        // Attempt to close the tabularData as early as possible to reduce resource lock contention.
        tabularData.close();
        if (log.isDebug()) log.debug(logPrefix + "first pass complete (" + numberOfRows + " rows) in " + (System.currentTimeMillis() - startTime) + " millis");
    }

    private void firstPassInflateRow(TabularData.Row tabularDataRow) throws Exception {
        createBean(tabularDataRow);
    }

    private void createBean(TabularData.Row tabularDataRow) throws InstantiationException, IllegalAccessException {
        Object bean = beanClass.newInstance();
        assignDefaultProperties(bean, tabularDataRow);
        getBeans().add(bean);
    }

    private void assignDefaultProperties(Object bean, TabularData.Row tabularDataRow) {
        int columnCount = tabularData.getColumnCount();
        Object cachedColumnValuesForBean = findOrCreateCacheColumnValuesForBean(bean);

        // set each property of bean
        for (int i = 0; i < columnCount; i++) {
            Column column = tabularData.getColumn(i);
            Object columnValue = tabularDataRow.getColumnValue(i);

            if (column != null) {
                DefaultProperty defaultProperty = schema.getDefaultPropertyByColumn(column);
                if (defaultProperty != null && (excludeProperties == null || !excludeProperties.contains(defaultProperty.getName()))) {
                    defaultProperty.setPropertyValue(bean, columnValue);
                }

                // Save column values which will be used to build derived properties, e.g. foreign keys,
                // amounts used to build Quantity objects...
                // we store these parameter values now, so that the tabularData can be closed before attempting to set derivedProperties.
                // Setting derivedProperties can lead to calls to other BeanFactories, which load more beans, therefore it is important
                // to close the tabularData as early as possible, to prevent resource lock contention, hence
                // we cache the values now, so the tabularData can be closed, and the cached values used afterwards.
                if (cachedColumnValuesForBean != null && derivedPropertyColumnsType.attributeExists(column.getName())) {
                    derivedPropertyColumnsType.set(cachedColumnValuesForBean, column.getName(), columnValue);
                }
            }
        }
    }

    private Object findOrCreateCacheColumnValuesForBean(Object bean) {
        Object cachedColumnValuesForBean = null;
        if (derivedPropertyColumnsType != null) {
            if (cachedColumnValues == null) cachedColumnValues = new HashMap();
            cachedColumnValuesForBean = cachedColumnValues.get(bean);
            if (cachedColumnValuesForBean == null) {
                cachedColumnValuesForBean = derivedPropertyColumnsType.newInstance();
                cachedColumnValues.put(bean, cachedColumnValuesForBean);
            }
        }
        return cachedColumnValuesForBean;
    }

    private void secondPassInflate() {
        long startTime = System.currentTimeMillis();
        // Set properties derived from primitive foreign keys or from multiple primitive values
        // This is done in a separate loop (second pass) from the loop that instantiates beans, to allow for circular relationships between beans.
        if (containsBeans()) assignDerivedProperties(getBeans());
        if (log.isDebug()) log.debug(logPrefix + "second pass complete (" + getNumberOfBeansAffected() + " beans) in " + (System.currentTimeMillis() - startTime) + " millis");
    }


    /**
     * Inflate property values from column values that are foreign keys to other beans, or that form compound values, e.g. Quantity values.
     * Inflates the property values, and set as properties of each bean.
     */
    private void assignDerivedProperties(Collection beans) {
        DerivedProperty[] derivedProperties = schema.getDerivedProperties();

        int n = 0;
        if (derivedProperties != null && beans != null && derivedProperties.length > 0) {
            for (int i = 0; i < derivedProperties.length; i++) {
                DerivedProperty derivedProperty = derivedProperties[i];
                if (excludeProperties == null || !excludeProperties.contains(derivedProperty.getName())) {
                    if (derivedProperty.isValid()) {
                        Map setPropertyState = new HashMap();
                        derivedProperty.initialiseSetPropertyState(setPropertyState);

                        for (Iterator iterator = beans.iterator(); iterator.hasNext();) {
                            Object bean = iterator.next();
                            Object cachedColumnValuesForBean = cachedColumnValues.get(bean);
                            derivedProperty.setPropertyValue(bean, cachedColumnValuesForBean, setPropertyState);

                            n++;
                            if (n % 50 == 0) {
                                if (log.isDebug()) log.debug(logPrefix + "done " + n + " seconds pass inflations");
                                Thread.yield();
                            }
                        }
                    }
                }
            }
        }
    }

    public List getBeans() {
        if (beans == null) beans = new ArrayList();
        return beans;
    }

    private int getNumberOfBeansAffected() {
        int numberOfBeansAffected = 0;
        numberOfBeansAffected += beans != null ? beans.size() : 0;
        return numberOfBeansAffected;
    }

    private boolean containsBeans() {
        return beans != null && beans.size() > 0;
    }
}
