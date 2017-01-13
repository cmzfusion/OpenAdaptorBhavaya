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

import org.bhavaya.collection.Association;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.EfficientArrayList;
import org.bhavaya.collection.MapEvent;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.Type;
import org.bhavaya.util.Utilities;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.util.*;

/**
 * Extends BeanFactory, adding the concept of TabularData, which is any data that can be represents by
 * rows and columns. Beans have keys made up of one-or-more columns. Each bean is created from a TabularData.Row.
 * Properties of the bean are determined from column values of the TabularData.Row.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.30.26.1 $
 * @see org.bhavaya.beans.TabularData
 */
public abstract class TabularDataBeanFactory extends BeanFactory {
    private static final Log log = Log.getCategory(TabularDataBeanFactory.class);

    static {
        BeanUtilities.addPersistenceDelegate(TabularDataBeanFactory.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                BeanFactory oldBeanFactory = (BeanFactory) oldInstance;
                return new Expression(oldInstance, BeanFactory.class, "getInstance", new Object[]{oldBeanFactory.getType(), oldBeanFactory.getDataSourceName()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    protected Column[] primaryKeyColumns;
    protected Column[] unionOfKeyColumns;

    protected TabularDataBeanFactory(Class type, String dataSourceName) {
        super(type, dataSourceName);
        this.primaryKeyColumns = getSchema().getPrimaryKey();
        this.unionOfKeyColumns = getSchema().getUnionOfKeyColumns();
    }


    protected Column[] getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    protected Column[] getKeyColumns(String indexName) {
        if (indexName == null) {
            return primaryKeyColumns;
        } else {
            return getSchema().getIndex(indexName).getColumns();
        }
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
            Object keyValue = getColumnValueForValue(bean, keyColumns[i], false);
            if (numberOfComponents == 1) {
                key = keyValue;
            } else {
                ((List) key).add(keyValue);
            }
        }
        return key;
    }

    public static Object getColumnValueForBean(Object value, Column column) {
        if (value == null) return null;
        BeanFactory beanFactory = getBeanFactoryForValue(value);
        if (beanFactory instanceof TabularDataBeanFactory) {
            TabularDataBeanFactory tabularDataBeanFactory = (TabularDataBeanFactory)beanFactory;
            return tabularDataBeanFactory.getColumnValueForValue(value, column);
        }
        return null;
    }

    protected Object getColumnValueForValue(Object value, Column column) {
        return getColumnValueForValue(value, column, true);
    }

    private Object getColumnValueForValue(Object value, Column column, boolean lookInKey) {
        if (value == null) return null;

        Property[] properties = getSchema().getPropertiesByColumn(column);

        if (properties != null) {
            Object columnValue = properties[0].getColumnValue(value, column);
            if (columnValue != null) {
                return columnValue;
            }
            // else lookInKey if possible
        }

        if (lookInKey) {
            return getKeyComponentForValue(value, column);
        }

        return null;
    }

    /**
     * Returns the key for value. If the key is a compound value,
     * i.e. a list of single values.  The single value to return is identified by the columnName.
     * <p/>
     * If the value is not contained in the BeanFactory and cannot get determined, null is returned.
     */
    private Object getKeyComponentForValue(Object value, Column column) {
        return getKeyComponentForValue(value, column, null);
    }

    private Object getKeyComponentForValue(Object value, Column column, String indexName) {
        if (value == null) return null;

        Column[] keyColumns = getKeyColumns(indexName);
        int numberOfcomponents = keyColumns.length;
        assert(numberOfcomponents > 0) : "number of key components greater than zero";

        Object key = getKeyForValue(value, indexName);
        if (key == null) return null;

        int keyIndex = -1;
        for (int i = 0; i < numberOfcomponents && keyIndex == -1; i++) {
            if (column.equals(keyColumns[i])) {
                keyIndex = i;
            }
        }

        if (keyIndex == -1) {
            return null;
        }

        if (numberOfcomponents == 1) {
            return key;
        } else {
            List compoundKey = (List) key;
            key = compoundKey.get(keyIndex);
            return key;
        }
    }

    /**
     * TODO: I want this class to reuse code in TabularDataToBeanTransformer, but have not yet done so.
     */
    protected static class TabularDataToBeanFactoryTransformer {
        protected TabularDataBeanFactory beanFactory;
        private KeysBeansAndEvents keysBeansAndEvents;
        private Map beansToFindLater;
        private TabularData tabularData;
        private Map cachedColumnValues;
        private Class beanClass;
        protected Schema schema;
        protected Column[] primaryKeyColumns;
        private Map keyColumnsExistInTabularData;
        protected final String logPrefix;
        private Type derivedPropertyColumnsType;

        public TabularDataToBeanFactoryTransformer(TabularDataBeanFactory beanFactory, TabularData tabularData, Map cachedColumnValues) {
            this.beanFactory = beanFactory;
            this.tabularData = tabularData;
            this.cachedColumnValues = cachedColumnValues;
            primaryKeyColumns = beanFactory.getSchema().getPrimaryKey();
            beanClass = beanFactory.getType();
            schema = beanFactory.getSchema();
            logPrefix = beanFactory.logPrefix;
            keysBeansAndEvents = new KeysBeansAndEvents();
            keyColumnsExistInTabularData = new HashMap();
            derivedPropertyColumnsType = schema.getDerivedPropertyColumnsType();
        }

        protected Object getBean(Object key, Class clazz) {
            return BeanFactory.getInstance(clazz, beanFactory.getDataSourceName()).get(key);
        }

        protected Class getSubClass(TabularData.Row tabularDataRow) {
            return schema.getSubClass(tabularDataRow);
        }

        protected boolean includesAllColumns(TabularData.Row tabularDataRow) {
            return containsKeyColumns(tabularData, beanFactory.unionOfKeyColumns);
        }

        public void firstPassInflate() throws Exception {
            try {
                long startTime = System.currentTimeMillis();
                int numberOfRows = 0;
                TabularData.Row tabularDataRow = tabularData.next();
                while (tabularDataRow != null) {
                    numberOfRows++;
                    firstPassInflateRow(tabularDataRow);
                    tabularDataRow = tabularData.next();
                    if (numberOfRows > 0 && numberOfRows % 50 == 0) {
                        if (log.isDebug()) log.debug(logPrefix + "done " + numberOfRows + " first pass inflations");
                        BeanFactory.setCurrentLoadProgress(numberOfRows);
                        Thread.yield();
                    }
                }
                BeanFactory.setCurrentLoadProgress(Integer.MAX_VALUE);
                if (log.isDebug()) log.debug(logPrefix + "first pass complete (" + numberOfRows + " rows) in " + (System.currentTimeMillis() - startTime) + " millis");
            } finally {
                // Attempt to close the tabularData as early as possible to reduce resource lock contention.
                tabularData.close();
            }
        }

        private void firstPassInflateRow(TabularData.Row tabularDataRow) throws Exception {
            final Object key = createKey(tabularDataRow, primaryKeyColumns);
            final Object existingBean = beanFactory.getPrimaryStore().get(key); // keep this reference, with weak/soft references it could dissappear

            if (isIgnoreable(key, tabularDataRow, existingBean)) return;

            final boolean beanNotInCache = existingBean == null;

            if (tabularDataRow.getRowType() == TabularData.ROW_TYPE_SELECT) {
                if (beanNotInCache) {
                    if (beanFactory.hasSubClasses()) {
                        Class subClass = getSubClass(tabularDataRow);
                        if (subClass != beanClass) {
                            if (beansToFindLater == null) beansToFindLater = new LinkedHashMap();
                            beansToFindLater.put(key, subClass); // will load these later, after we have finished with this TabularData
                            return;
                        }
                    }
                    createBean(key, tabularDataRow, false);
                } else {
                    keysBeansAndEvents.addFoundBean(key, existingBean, false);
                }
            } else if (tabularDataRow.getRowType() == TabularData.ROW_TYPE_INSERT || tabularDataRow.getRowType() == TabularData.ROW_TYPE_UPDATE) {
                if (beanNotInCache) {
                    if (tabularDataRow.getRowType() == TabularData.ROW_TYPE_UPDATE && !includesAllColumns(tabularDataRow)) {
                        // an update statement, for which we do not have enough data to instantiate a bean, therefore load the bean.
                        if (log.isDebugEnabled()) log.debug(logPrefix + "possible load data for: " + tabularData);

                        if (beansToFindLater == null) beansToFindLater = new LinkedHashMap();
                        if (beanFactory.getAncestorType() != null) {
                            // Even though the table of the insert/update is one of the tables that contains data for beans of this type,
                            // it still not be relevant to this type, as a single table may contain data for multiple
                            // bean types. e.g. FxTrade BeanFactory should not process "UPDATE TRADE" if the data represents a BondTrade.
                            beansToFindLater.put(key, beanFactory.getAncestorType()); // will load these later, after we have finished with this TabularData
                        } else {
                            beansToFindLater.put(key, beanFactory.getType()); // will load these later, after we have finished with this TabularData
                        }
                    } else {
                        if (beanFactory.getSuperType() != null || beanFactory.hasSubClasses()) {
                            Class subClass = getSubClass(tabularDataRow);
                            if (subClass != beanClass) return;
                        }

                        createBean(key, tabularDataRow, true);
                    }
                } else {
                    // Processing an INSERT for a bean that is all ready in the beanFactory may seem strange, but beans can have more than one INSERT statement.
                    // e.g as a result of an "INSERT INTO BOND_TRADE..." which updates a BondTrade bean that was first created by an "INSERT INTO TRADE..."
                    if (existingBean.getClass() != beanClass) return;
                    assignDefaultProperties(existingBean, tabularDataRow);
                    keysBeansAndEvents.addUpdatedBean(key, existingBean, true);
                }
            } else if (tabularDataRow.getRowType() == TabularData.ROW_TYPE_DELETE) {
                if (beanNotInCache) {
                    // do nothing
                } else {
                    if (existingBean.getClass() != beanClass) return;
                    beanFactory.remove(key, false, false);
                    keysBeansAndEvents.addDeletedBean(key, existingBean, true);
                    if (cachedColumnValues != null) cachedColumnValues.remove(existingBean);
                }
            } else {
                throw new IllegalArgumentException();
            }
        }

        private boolean isIgnoreable(Object key, TabularData.Row tabularDataRow, Object existingBean) {
            boolean beanNotInCache = existingBean == null;

            if (tabularDataRow.getRowType() == TabularData.ROW_TYPE_SELECT && keysBeansAndEvents.containsKey(key)) {
                // already handled bean in this transaction
                return true;
            } else if (beanNotInCache && tabularDataRow.getRowType() == TabularData.ROW_TYPE_UPDATE && isIgnoreableUpdate(tabularDataRow)) {
                if (log.isDebug()) log.debug(logPrefix + "ignore: " + tabularData);
                return true;
            }

            return false;
        }

        protected boolean isIgnoreableUpdate(TabularData.Row tabularDataRow) {
            return false;
        }

        private void createBean(Object key, TabularData.Row tabularDataRow, boolean insert) throws InstantiationException, IllegalAccessException {
            Object bean = beanFactory.discardedBeans.get(key);
            if (bean == null) bean = beanClass.newInstance();
            assignDefaultProperties(bean, tabularDataRow);
            if(shouldAddBean(bean)) {
                keysBeansAndEvents.addCreatedBean(key, bean, insert);
                addPartiallyInflatedBean(bean);
                put(beanFactory, key, bean, tabularDataRow);
            }
        }

        /**
         * Override this method to filter out addition of certain beans
         * @param bean Bean to add
         * @return True if bean should be added
         */
        protected boolean shouldAddBean(Object bean) {
            return true;
        }

        private void assignDefaultProperties(Object bean, TabularData.Row tabularDataRow) {
            int columnCount = tabularData.getColumnCount();
            Object cachedColumnValuesForBean = findOrCreateCacheColumnValuesForBean(bean);

            // set each property of bean
            for (int i = 0; i < columnCount; i++) {
                Column column = tabularData.getColumn(i);

                if (column != null) {
                    Object columnValue = tabularDataRow.getColumnValue(i, schema.getType(column));

                    DefaultProperty defaultProperty = schema.getDefaultPropertyByColumn(column);
                    if (defaultProperty != null) {
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

        private void put(BeanFactory beanFactory, Object key, Object value, TabularData.Row tabularDataRow) {
            beanFactory.putInPrimaryStore(key, value);
            putIntoIndexedStores(beanFactory, value, tabularDataRow);
            if (beanFactory.getSuperBeanFactory() != null) put(beanFactory.getSuperBeanFactory(), key, value, tabularDataRow);
        }

        private void putIntoIndexedStores(BeanFactory beanFactory, Object value, TabularData.Row tabularDataRow) {
            BeanFactory.IndexedAssociation[] indexedStores = beanFactory.getIndexedStores();
            if (indexedStores != null) {
                for (int i = 0; i < indexedStores.length; i++) {
                    IndexedAssociation indexedAssociation = indexedStores[i];
                    putIntoIndexedStore(indexedAssociation, value, tabularDataRow);
                }
            }
        }

        private void putIntoIndexedStore(BeanFactory.IndexedAssociation indexedStore, Object value, TabularData.Row tabularDataRow) {
            Object indexKey = createKey(tabularDataRow, indexedStore.getIndex().getColumns());
            beanFactory.putIntoIndexedStore(indexedStore, indexKey, value);
        }

        public void secondPassInflate() {
            long startTime = System.currentTimeMillis();

            // Set properties derived from primitive foreign keys or from multiple primitive values
            // This is done in a separate loop (second pass) from the loop that instantiates beans, to allow for circular relationships between beans,
            // and to allow the closing of tabularData as early as possible.
            if (keysBeansAndEvents.containsCreatedBeans()) assignDerivedProperties(keysBeansAndEvents.getCreatedBeans(), false);
            if (keysBeansAndEvents.containsUpdatedBeans()) assignDerivedProperties(keysBeansAndEvents.getUpdatedBeans(), true);

            if (log.isDebug()) log.debug(logPrefix + "second pass complete (" + keysBeansAndEvents.size() + " beans) in " + (System.currentTimeMillis() - startTime) + " millis");
        }


        public void inflateBeansToFindLater() {
            if (beansToFindLater == null) return;

            // do this after closing previous tabularData as this may cause new tabularData to be opened
            int numberOfRows = 0;
            for (Iterator iterator = beansToFindLater.entrySet().iterator(); iterator.hasNext();) {
                numberOfRows++;
                Map.Entry entry = (Map.Entry) iterator.next();
                Object key = entry.getKey();
                Class clazz = (Class) entry.getValue();
                Object bean = getBean(key, clazz);
                if (bean != null) keysBeansAndEvents.addFoundBean(key, bean, false); // nb. there may not be a subClass instance corresponding to the superType instance
                if (numberOfRows % 100 == 0) Thread.yield();
            }
        }

        /**
         * Inflate property values from column values that are foreign keys to other beans, or that form compound values, e.g. Quantity values.
         * Inflates the property values, and set as properties of each bean.
         */
        private void assignDerivedProperties(Collection beans, boolean updateOnly) {
            DerivedProperty[] derivedProperties = schema.getDerivedProperties();

            int n = 0;
            if (derivedProperties != null && beans != null && derivedProperties.length > 0) {
                for (int i = 0; i < derivedProperties.length; i++) {
                    DerivedProperty derivedProperty = derivedProperties[i];
                    if (derivedProperty.isValid() && (!updateOnly || derivedColumnInTabularData(derivedProperty.getColumns()))) {
                        Map setPropertyState = getPropertyInitialisationValues();
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

        private boolean derivedColumnInTabularData(Column[] derivedPropertyColumns) {
            for (Column tabularDataColumn : tabularData.getColumns()) {
                for (Column derivedColumn : derivedPropertyColumns) {
                    if (derivedColumn.equals(tabularDataColumn)) return true;
                }
            }
            return false;
        }

        protected Map getPropertyInitialisationValues() {
            Map map = new HashMap();
            map.put(ForeignKeyProperty.DATA_SOURCE_NAME_PROPERTY, beanFactory.getDataSourceName());
            return map;
        }

        protected Column[] getKeyColumns(String indexName) {
            if (indexName == null) {
                return primaryKeyColumns;
            } else {
                return schema.getIndex(indexName).getColumns();
            }
        }

        private Object createKey(TabularData.Row tabularDataRow, Column[] keyColumns) {
            if (!containsKeyColumns(tabularData, keyColumns)) throw new RuntimeException("Primary key or indicies not part of statement: " + tabularData);

            Object key = null;

            int numberOfComponents = keyColumns.length;
            if (numberOfComponents > 1) key = new EfficientArrayList(keyColumns.length);

            for (int i = 0; i < numberOfComponents; i++) {
                Column keyColumn = keyColumns[i];

                // need to ensure we map the keyValue type if it exists as a property on the bean
                // e.g this may convert a java.sql.Timestamp to a java.sql.Date or a BigDecimal to a Long
                Object keyValue = tabularDataRow.getColumnValue(keyColumn, schema.getType(keyColumn));

                if (numberOfComponents == 1) {
                    key = keyValue;
                } else {
                    ((List) key).add(keyValue);
                }
            }

            return key;
        }

        private boolean containsKeyColumns(TabularData tabularData, Column[] keyColumns) {
            Boolean keyColumnsExist = (Boolean) keyColumnsExistInTabularData.get(keyColumns);
            if (keyColumnsExist == null) {
                keyColumnsExist = new Boolean(containsKeyColumnsInternal(tabularData, keyColumns));
                keyColumnsExistInTabularData.put(keyColumns, keyColumnsExist);
                if (!keyColumnsExist.booleanValue()) {
                    log.info(logPrefix + "Could not find all columns: " + Utilities.asString(keyColumns, ",") + " in: " + Utilities.asString(tabularData.getColumns(), ","));
                }
            }
            return keyColumnsExist.booleanValue();
        }

        private boolean containsKeyColumnsInternal(TabularData tabularData, Column[] keyColumns) {
            if (keyColumns.length == 0) return false;
            int columnCount = tabularData.getColumnCount();

            for (int k = 0; k < keyColumns.length; k++) {
                boolean keyColumnExists = false;
                Column keyColumn = keyColumns[k];

                for (int i = 0; i < columnCount && !keyColumnExists; i++) {
                    Column column = tabularData.getColumn(i);
                    if (column.equals(keyColumn)) {
                        keyColumnExists = true;
                    }
                }

                if (!keyColumnExists) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Guarantee to fire callbacks on any Lifecyle at end of transaction.
         */
        public void fireLifeCycleCallbacks() {
            if (beanFactory.isBeanLifeCycleable()) {
                keysBeansAndEvents.fireLifeCycleCallbacks(beanFactory.discardedBeans);
            }
        }

        public void removeFromPartiallyInflatedMap() {
            removePartiallyInflatedBeans(keysBeansAndEvents.getCreatedBeans());
            beanFactory.waitForBeansToInflate(keysBeansAndEvents.getFoundBeans());
        }

        public Collection getBeans() {
            return keysBeansAndEvents.getBeans();
        }

        public boolean containsBeans() {
            return keysBeansAndEvents.containsBeans();
        }

        public void fireMapChangedEvents(List events) {
            keysBeansAndEvents.fireMapChangedEvents(beanFactory, events);
        }

        private static class KeysBeansAndEvents {
            private List eventQueue;
            private Map keysAndBeans;
            private Set createdBeans;
            private Set insertedBeans;
            private Set updatedBeans;
            private Set foundBeans;
            private Set deletedBeans;

            public KeysBeansAndEvents() {
                keysAndBeans = new LinkedHashMap();
            }

            public void addCreatedBean(Object key, Object bean, boolean insert) {
                getCreatedBeans().add(bean);
                keysAndBeans.put(key, bean);
                if (insert) {
                    getInsertedBeans().add(bean);
                    addEvent(key, bean, MapEvent.INSERT);
                }
            }

            public Set getCreatedBeans() {
                if (createdBeans == null) createdBeans = new HashSet();
                return createdBeans;
            }

            public boolean containsCreatedBeans() {
                return (createdBeans != null && createdBeans.size() > 0);
            }

            public Set getInsertedBeans() {
                if (insertedBeans == null) insertedBeans = new HashSet();
                return insertedBeans;
            }

            public boolean containsInsertedBeans() {
                return (insertedBeans != null && insertedBeans.size() > 0);
            }

            public void addUpdatedBean(Object key, Object bean, boolean addEvent) {
                getUpdatedBeans().add(bean);
                keysAndBeans.put(key, bean);
                addStrongReference(bean);
                if (addEvent) addEvent(key, bean, MapEvent.UPDATE);
            }

            public Set getUpdatedBeans() {
                if (updatedBeans == null) updatedBeans = new HashSet();
                return updatedBeans;
            }

            public boolean containsUpdatedBeans() {
                return (updatedBeans != null && updatedBeans.size() > 0);
            }

            public void addFoundBean(Object key, Object bean, boolean addEvent) {
                if (createdBeans != null && createdBeans.contains(bean)) return; // may have already been created
                getFoundBeans().add(bean);
                keysAndBeans.put(key, bean);
                addStrongReference(bean);
                if (addEvent) addEvent(key, bean, MapEvent.INSERT);
            }

            public Set getFoundBeans() {
                if (foundBeans == null) foundBeans = new HashSet();
                return foundBeans;
            }

            public boolean containsFoundBeans() {
                return (foundBeans != null && foundBeans.size() > 0);
            }

            public void addDeletedBean(Object key, Object bean, boolean addEvent) {
                getDeletedBeans().add(bean);
                keysAndBeans.put(key, bean);
                if (addEvent) addEvent(key, bean, MapEvent.DELETE);
            }

            public Set getDeletedBeans() {
                if (deletedBeans == null) deletedBeans = new HashSet();
                return deletedBeans;
            }

            public boolean containsDeletedBeans() {
                return (deletedBeans != null && deletedBeans.size() > 0);
            }

            private void addEvent(Object key, Object bean, int eventType) {
                if (eventQueue == null) eventQueue = new ArrayList();
                eventQueue.add(new MapEventData(key, bean, eventType));
            }

            public int size() {
                return keysAndBeans.size();
            }

            public boolean containsKey(Object key) {
                return keysAndBeans.containsKey(key);
            }

            public Collection getBeans() {
                return keysAndBeans.values();
            }

            public boolean containsBeans() {
                return keysAndBeans.size() > 0;
            }

            public void fireMapChangedEvents(BeanFactory beanFactory, List events) {
                if (eventQueue == null) return;
                for (int i = 0; i < eventQueue.size(); i++) {
                    MapEventData event = (MapEventData) eventQueue.get(i);
                    beanFactory.fireMapChangedThroughHierarchy(events, event.eventType, event.key, event.value);
                }
            }

            public void fireLifeCycleCallbacks(Association discardedBeans) {
                int i = 0;
                for (Iterator iterator = keysAndBeans.values().iterator(); iterator.hasNext();) {
                    if (i > 0 && i % 50 == 0) Thread.yield(); // dont want to hog the processor
                    LifeCycle lifeCycle = (LifeCycle) iterator.next();
                    try {
                        if (createdBeans != null && createdBeans.contains(lifeCycle)) {
                            if (!discardedBeans.containsValue(lifeCycle)) {
                                lifeCycle.init();
                            } else {
                                lifeCycle.updated();
                            }
                        }

                        if (insertedBeans != null && insertedBeans.contains(lifeCycle)) {
                            lifeCycle.inserted();
                        } else if (updatedBeans != null && updatedBeans.contains(lifeCycle)) {
                            lifeCycle.updated();
                        } else if (deletedBeans != null && deletedBeans.contains(lifeCycle)) {
                            lifeCycle.deleted();
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                    i++;
                }
            }

            private static class MapEventData {
                public int eventType;
                public Object key;
                public Object value;

                public MapEventData(Object key, Object value, int eventType) {
                    this.key = key;
                    this.value = value;
                    this.eventType = eventType;
                }
            }
        }
    }
}
