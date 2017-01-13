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

package org.bhavaya.beans.criterion;

import org.bhavaya.beans.*;
import org.bhavaya.collection.*;
import org.bhavaya.db.CatalogSchemaTable;
import org.bhavaya.db.SQL;
import org.bhavaya.db.SQLFormatter;
import org.bhavaya.util.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 *
 * -----------------------------------
 * notes - Nick 14 April 08:
 * 'named criteria' are slightly confusing, they have nothing to do with the criterion 'name', they are actually criterion groups added with a String 'id' to identify them
 * They appear to be used mainly by view contexts which wish to further restrict the data in a view instance by adding extra criteria
 * Originally, each time a named criteria was added, this would trigger a collection refresh
 * I have added logic so that it is now possible to add several named criteria in one operation -
 * before a refresh takes place. This avoids triggering a refresh before all the criteria are in place
 * ----------------------------------------
 *
 * @author Parwy Sekhon
 * @version $Revision: 1.21 $
 */
public class CriteriaBeanCollection<E> extends AuditBeanCollection<E> implements Comparable, Describeable {
    private static Log log = Log.getCategory(CriteriaBeanCollection.class);

    private BeanFactory beanFactory;
    private SynchronisationWeakMapListener.Synchronisable synchronisable;
    private boolean inited;
    private CriterionGroup mergedCriteria;
    private CriterionGroup primaryCriteria;
    private Map<String, CriterionGroup> namedCriteriaById;
    private Set<CriterionGroup> nonPersistentCriteriaList;
    private PropertyChangeListener refresher;
    private LoadDecorator loadDecorator;
    private boolean firstLoad;
    private final String logPrefix;
    private Map<CriterionGroup, PropertyChangeListener> criterionGroupListeners = new HashMap<CriterionGroup, PropertyChangeListener>();
    private LoadRunnable commitLoadRunnable;
    private LoadRunnable noncommitLoadRunnable;
    private Map<MapListener, BeanFactory> registeredBeanFactories = new HashMap<MapListener, BeanFactory>();

    static {
        BeanUtilities.addPersistenceDelegate(CriteriaBeanCollection.class, new BhavayaPersistenceDelegate(new String[]{"beanFactory", "type", "primaryCriteria", "namedCriteriaById"}));
    }

    /**
     * Constructs a CriteriaBeanCollection that has the given criteria, but will contain no data until you call
     * one of its methods. Then it will be populated, and hooked into realtime beanFactory events.
     *
     * @param type
     * @param primaryCriteria
     */
    public CriteriaBeanCollection(Class<E> type, CriterionGroup primaryCriteria) {
        // use default BeanFactory
        this(BeanFactory.getInstance(type), type, primaryCriteria);
    }

    public CriteriaBeanCollection(BeanFactory beanFactory, Class<E> type, CriterionGroup primaryCriteria) {
        this(beanFactory, type, primaryCriteria, null);
    }

    public CriteriaBeanCollection(BeanFactory beanFactory, Class<E> type, CriterionGroup primaryCriteria, Map<String, CriterionGroup> namedCriteriaById) {
        super(type);
        this.beanFactory = beanFactory;
        this.primaryCriteria = primaryCriteria;

        logPrefix = getLogPrefix(this);
        inited = false;
        firstLoad = true;
        nonPersistentCriteriaList = new LinkedHashSet<CriterionGroup>();
        this.namedCriteriaById = new LinkedHashMap<String, CriterionGroup>();

        synchronisable = new CriteriaBeanCollectionSynchronisable(this);// keep this alive to prevent it being removed while collection is alive

        primaryCriteria.addPropertyChangeListener("criteria", new Merger<E>(primaryCriteria, this, "criteria"));

        // We are keeping instances of these runnables, so that if we have a loadDecorator that loads on a seperate thread
        // it can detect that it has been asked to run the runnable, when it is still running the previous one.
        commitLoadRunnable = new LoadRunnable() {
            public void run(LoadProgressMonitor loadProgressMonitor) {
                load(true, true, loadProgressMonitor);
            }
        };
        noncommitLoadRunnable = new LoadRunnable() {
            public void run(LoadProgressMonitor loadProgressMonitor) {
                load(false, false, loadProgressMonitor);
            }
        };

        mergedCriteria = createCriterionGroup("", null);
        refresher = new Refresher();
        mergedCriteria.addPropertyChangeListener("criteria", refresher);

        // add compulsory criteria
        IndexedSet<Criterion> compulsoryCriterionSet = CriterionFactory.getCompulsoryCriterion(getType());
        if (compulsoryCriterionSet != null && compulsoryCriterionSet.size() != 0) {
            Criterion[] criteria = new Criterion[compulsoryCriterionSet.size()];
            criteria = compulsoryCriterionSet.toArray(criteria);
            CriterionGroup criterionGroup = createCriterionGroup("Compulsory", criteria);
            addNonPersistentCriteria(criterionGroup);
        }

        if (namedCriteriaById != null) {
            for (Map.Entry<String, CriterionGroup> entry : namedCriteriaById.entrySet()) {
                String id = entry.getKey();
                CriterionGroup namedCriteria = entry.getValue();
                addNamedCriteria(id, namedCriteria);
            }
        }

        updateMergedCriterionGroup();
    }

    protected CriterionGroup createCriterionGroup(String name, Criterion[] criteria) {
        return new CriterionGroup(name, criteria);
    }

    protected CriterionGroup[] createCriterionGroups(int size) {
        return new CriterionGroup[size];
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
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

    protected boolean isInited() {
        return inited;
    }

    protected void initImpl() {
        beanFactory.addMapListener(new SynchronisationWeakMapListener(beanFactory, synchronisable));
        refresh(true);
    }

    private void registerWithDerivedPropertyBeanFactories() {
        Schema schema = Schema.getInstance(getType());
        DerivedProperty[] derivedProperties = schema.getDerivedProperties();
        if (derivedProperties != null) {
            for (DerivedProperty derivedProperty : derivedProperties) {
                if (derivedProperty instanceof ForeignKeyProperty) {
                    Class<?> type = derivedProperty.getType();
                    if (beanTypeIsAffectedByCriteria(type)) {
                        BeanFactory beanFactory = BeanFactory.getInstance(type);
                        MapListener mapListener = createDerivedPropertyMapListener(beanFactory, derivedProperty);
                        registeredBeanFactories.put(mapListener, beanFactory);
                        beanFactory.addMapListener(mapListener);
                    }
                }
            }
        }
    }

    protected WeakMapListener createDerivedPropertyMapListener(BeanFactory beanFactory, DerivedProperty derivedProperty) {
        return new DerivedPropertyMapListener(beanFactory, this, derivedProperty);
    }

    private boolean beanTypeIsAffectedByCriteria(Class<?> beanType) {
        Criterion[] criteria = mergedCriteria.getCriteria();
        if (criteria == null) {
            return false;
        }

        Schema propertySchema = Schema.getInstance(beanType);

        for (Criterion criterion : criteria) {
            if (criterion instanceof SqlCriterion) {
                Column[] criteriaColumns = null;
                try {
                    criteriaColumns = ((SqlCriterion) criterion).getDirectLeftOperandColumns(getType());
                } catch (RuntimeException e) {
                    //This is here because the above method can inexplicably throw a RuntimeException if the beanType doesn't apply
                }
                if (criteriaColumns == null) {
                    continue;
                }
                for (Column criteriaColumn : criteriaColumns) {
                    Property[] propertiesForColumn = propertySchema.getPropertiesByColumn(criteriaColumn);
                    if (propertiesForColumn != null) {
                        for (Property propertyForColumn : propertiesForColumn) {
                            if (!(propertyForColumn instanceof ForeignKeyProperty)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return the CriterionGroup that this collection is currently using (this is the intersection of the
     *         persistent and non-persistent criterionGroups)
     */
    public CriterionGroup getMergedCriteria() {
        return mergedCriteria;
    }

    public CriterionGroup getPrimaryCriteria() {
        return primaryCriteria;
    }

    public Map<String, CriterionGroup> getNamedCriteriaById() {
        return namedCriteriaById;
    }


    //kept for backwards compatibility
    public synchronized void addNamedCriteria(String id, CriterionGroup criteria) {
        doAddNamedCriteria(new NamedCriteria(id, criteria));
        updateMergedCriterionGroup();
    }

    public synchronized void removeNamedCriteria(String id) {
        doRemoveNamedCriteria(id);
        updateMergedCriterionGroup();
    }

    //kept for backwards compatibility, consider using setNamedCriteria to remove existing named criteria
    //and add new ones in a single operation
    public synchronized void removeNamedCriteriaNotInIds(String[] ids) {
        Set<String> idSet = new HashSet<String>(Arrays.asList(ids));
        Set<String> currentKeys = namedCriteriaById.keySet();
        currentKeys.removeAll(idSet);
        doRemoveNamedCriteria(currentKeys.toArray(new String[currentKeys.size()]));
        updateMergedCriterionGroup();
    }

    /**
     * Remove all existing named criteria, set the new criteria and update the collection
     * @param namedCriteria
     */
    public synchronized void setNamedCriteria(NamedCriteria... namedCriteria) {
        doRemoveNamedCriteria(namedCriteriaById.keySet().toArray(new String[namedCriteriaById.size()]));
        doAddNamedCriteria(namedCriteria);
        updateMergedCriterionGroup();
    }

    public synchronized void addNamedCriteria(NamedCriteria... namedCriterias) {
        doAddNamedCriteria(namedCriterias);
        updateMergedCriterionGroup();
    }

    //add criteria without calling updateMergedCriterionGroup
    private void doAddNamedCriteria(NamedCriteria... namedCriterias) {
        for (NamedCriteria c : namedCriterias) {
            namedCriteriaById.put(c.getId(), c.getCriterionGroup());
            Merger<E> merger = new Merger<E>(c.getCriterionGroup(), this, "criteria");
            c.getCriterionGroup().addPropertyChangeListener("criteria", merger);
            criterionGroupListeners.put(c.getCriterionGroup(), merger);
        }
    }

    //remove criteria without calling updateMergedCriterionGroup
    private void doRemoveNamedCriteria(String... ids) {
        for (String id : ids) {
            CriterionGroup criteria = namedCriteriaById.remove(id);
            if (criteria != null) {
                PropertyChangeListener merger = criterionGroupListeners.get(criteria);
                criteria.removePropertyChangeListener("criteria", merger);
            }
        }
    }

    public synchronized CriterionGroup getNamedCriteria(String id) {
        return namedCriteriaById.get(id);
    }

    public synchronized void addNonPersistentCriteria(CriterionGroup nonPersistentCriteria) {
        nonPersistentCriteriaList.add(nonPersistentCriteria);
        Merger<E> merger = new Merger<E>(nonPersistentCriteria, this, "criteria");
        nonPersistentCriteria.addPropertyChangeListener("criteria", merger);
        criterionGroupListeners.put(nonPersistentCriteria, merger);
        updateMergedCriterionGroup();
    }

    public synchronized void removeNonPersistentCriteria(CriterionGroup nonPersistentCriteria) {
        boolean removed = nonPersistentCriteriaList.remove(nonPersistentCriteria);
        if (removed) {
            PropertyChangeListener merger = criterionGroupListeners.get(nonPersistentCriteria);
            nonPersistentCriteria.removePropertyChangeListener("criteria", merger);
            updateMergedCriterionGroup();
        }
    }

    private void updateMergedCriterionGroup() {
        CriterionGroup newCriteria = mergeCriterionGroups(null, primaryCriteria);

        CriterionGroup[] namedCriteriaArray = namedCriteriaById.values().toArray(createCriterionGroups(namedCriteriaById.values().size()));
        for (CriterionGroup namedCriteria : namedCriteriaArray) {
            newCriteria = mergeCriterionGroups(newCriteria, namedCriteria);
        }

        CriterionGroup[] nonPersistentCriteriaArray = nonPersistentCriteriaList.toArray(createCriterionGroups(nonPersistentCriteriaList.size()));
        for (CriterionGroup nonPersistentCriteria : nonPersistentCriteriaArray) {
            newCriteria = mergeCriterionGroups(newCriteria, nonPersistentCriteria);
        }

        //now update the criterion group that CriteriaBeanCollection is using
        if (newCriteria != null) {
            mergedCriteria.setName(newCriteria.getName());
            mergedCriteria.setCriteria(newCriteria.getCriteria());
            unRegisterFromBeanFactories();
            registerWithDerivedPropertyBeanFactories();
        } else {
            mergedCriteria.setCriteria(null);
        }
    }

    protected CriterionGroup mergeCriterionGroups(CriterionGroup firstGroup, CriterionGroup secondGroup) {
        return CriterionGroup.mergeCriterionGroups(firstGroup, secondGroup);
    }

    private void unRegisterFromBeanFactories() {
        for (Map.Entry<MapListener, BeanFactory> entry : registeredBeanFactories.entrySet()) {
            BeanFactory beanFactory = entry.getValue();
            MapListener<?, ?> mapListener = entry.getKey();
            beanFactory.removeMapListener(mapListener);
        }
        registeredBeanFactories.clear();
    }

    public void setLoadDecorator(LoadDecorator loadDecorator) {
        this.loadDecorator = loadDecorator;
    }

    private void refresh(final boolean commit) {
        init();
        logRefreshInfo();

        // Ideally the load here would never take place on the swing thread
        // However we currently have too many components which call beanFactory.getAllBeanCollection().toArray()
        // and rely on the load being synchronous - so I will defer this change until we have time to properly
        // handle the implications of changing the behaviour here
        LoadRunnable loadRunnable = commit ? commitLoadRunnable : noncommitLoadRunnable;

        if (!commit || loadDecorator == null) {
            loadRunnable.run(null);
        } else {
            boolean firstLoad = isFirstLoad();
            loadDecorator.run(loadRunnable, firstLoad);
        }
    }

    private void logRefreshInfo() {
        int criteriaCount = mergedCriteria.getCriteria() == null ? 0 : mergedCriteria.getCriteria().length;
        log.info("Refreshing criteria bean collection " + this + " with " + criteriaCount + " criteria");
        if (log.isDebug()) {
            //for the time being add a stack track to the debug.
            //this is because it can be very hard to determine what triggered refresh to be called, without a stack dump
            //we can remove this once confident there are no problems with load triggering
            try {
                throw new Exception();
            } catch (Exception e) {
                log.debug(mergedCriteria.getDescription());
                log.debug("Stack trace generated for debug purposes:");
                log.debug(e, e);
            }
        }
    }

    private boolean isFirstLoad() {
        boolean firstLoad;
        synchronized (this) {
            firstLoad = this.firstLoad;
            this.firstLoad = false;
        }
        return firstLoad;
    }

    private void load(boolean commit, boolean allRows, LoadProgressMonitor loadProgressMonitor) {
        if (log.isDebug()) log.debug(logPrefix + "loading data for criteria: " + mergedCriteria.getName());

        synchronized (this) {
            Object[] beans = beanFactory.getObjects(mergedCriteria, loadProgressMonitor);

            if (allRows) {
                clear(false, false); // clearing after getting beans can help prevent beans being garbage collected when they are going to be reused
                for (Object bean1 : beans) {
                    E bean = (E) bean1;
                    this.add(bean, false, false);
                }
            } else {
                Set<E> oldBeans = new HashSet<E>(this);

                for (Object bean : beans) {
                    E newBean = (E) bean;
                    if (!oldBeans.contains(newBean)) {
                        this.add(newBean, false, true);
                    }
                }

                Set<?> newBeans = new HashSet<Object>(Arrays.asList(beans));

                for (E oldBean : oldBeans) {
                    if (!newBeans.contains(oldBean)) {
                        this.remove(oldBean, false, true);
                    }
                }
            }
        }

        if (allRows) fireCollectionChanged();
        if (commit) fireCommit();
    }

    public int getLoadRowCount() {
        return beanFactory.getLoadRowCount(mergedCriteria);
    }

    public String toString() {
        return primaryCriteria.getName();
    }

    public String getDescription() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(primaryCriteria.getDescription());

        for (CriterionGroup criterionGroup : namedCriteriaById.values()) {
            buffer.append("\n");
            buffer.append(criterionGroup.getDescription());
        }
        return buffer.toString();
    }

    public int compareTo(Object o) {
        if (o instanceof CriteriaBeanCollection) {
            CriteriaBeanCollection<?> criteriaBeanCollection = (CriteriaBeanCollection<?>) o;
            return ToStringComparator.CASE_INSENSITIVE_COMPARATOR.compare(this.primaryCriteria.getName(), criteriaBeanCollection.primaryCriteria.getName());
        }
        return -1;
    }

    private static String getLogPrefix(CriteriaBeanCollection<?> beanCollection) {
        return beanCollection.getType().getName() + "@" + System.identityHashCode(beanCollection) + " (" + beanCollection.primaryCriteria.getName() + "): ";
    }

    public class CriteriaBeanCollectionSynchronisable implements SynchronisationWeakMapListener.Synchronisable {
        private CriteriaBeanCollection beanCollection;

        public CriteriaBeanCollectionSynchronisable(CriteriaBeanCollection beanCollection) {
            this.beanCollection = beanCollection;
        }

        public void onAllRows() {
            beanCollection.refresh(false);
        }

        public void onCommit() {
            beanCollection.fireCommit();
        }

        public void onInsert(Object key, Object bean) {
            beanCollection.add(bean, false);
        }

        public void onUpdate(Object key, Object bean) {
            int rowIndex = beanCollection.indexOf(bean);
            beanCollection.fireCollectionChanged(new ListEvent(beanCollection, ListEvent.UPDATE, bean, rowIndex));
        }

        public boolean onDelete(Object key, Object bean) {
            return beanCollection.remove(bean, false);
        }

        public boolean contains(Object key, Object bean) {
            return beanCollection.contains(bean);
        }

        public boolean evaluate(Object key, Object bean) {
            return beanCollection.mergedCriteria.evaluate(bean);
        }

        public String getLogPrefix() {
            return CriteriaBeanCollection.getLogPrefix(beanCollection);
        }
    }

    private class Refresher implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (requiresRefresh()) refresh(true);
        }

        private boolean requiresRefresh() {
            boolean refresh;
            synchronized (listenerLock) {
                refresh = getCollectionListeners().size() > 0;
            }
            return refresh;
        }
    }

    private static class Merger<E> extends WeakPropertyChangeListener {
        public Merger(CriterionGroup criterionGroup, CriteriaBeanCollection<E> criteriaBeanCollection, String propertyName) {
            super(criterionGroup, criteriaBeanCollection, propertyName);
        }

        protected void propertyChange(Object listenerOwner, PropertyChangeEvent evt) {
            CriteriaBeanCollection<E> criteriaBeanCollection = (CriteriaBeanCollection<E>) listenerOwner;
            criteriaBeanCollection.updateMergedCriterionGroup();
        }
    }

    public static interface LoadDecorator {
        public void run(LoadRunnable loadRunnable, boolean firstLoad);
    }

    public static interface LoadRunnable {
        public void run(LoadProgressMonitor loadProgressMonitor);
    }

    protected static class DerivedPropertyMapListener extends WeakMapListener {
        private CatalogSchemaTable[] tables;
        private Class type;
        private BeanFactory beanFactory;
        private Column[] columns;
        private boolean usePrimaryKey = false;
        private boolean compositePrimaryKey = false;

        public DerivedPropertyMapListener(ObservableMap observableMap, Object listenerOwner, DerivedProperty derivedProperty) {
            super(observableMap, listenerOwner);
            CriteriaBeanCollection collection = (CriteriaBeanCollection) listenerOwner; // we dont keep a reference to this as a field, it is done by WeakMapListener

            type = collection.getType();
            beanFactory = collection.beanFactory;
            columns = derivedProperty.getColumns();
            tables = getTables();
            Schema schema = Schema.getInstance(type);
            Column[] primaryKeyColumns = schema.getPrimaryKey();
            usePrimaryKey = doColumnsMatchPrimaryKey(columns, primaryKeyColumns);
            compositePrimaryKey = (primaryKeyColumns.length != 1);
        }

        protected BeanFactory getBeanFactory() {
            return beanFactory;
        }

        protected CatalogSchemaTable[] getTables() {
            Schema schema = Schema.getInstance(type);
            return schema.getSql().getTables();
        }

        private static boolean doColumnsMatchPrimaryKey(Column[] derivedPropertyColumns, Column[] primaryKeyColumns) {
            if (derivedPropertyColumns == null || primaryKeyColumns == null || derivedPropertyColumns.length != primaryKeyColumns.length) {
                return false;
            }
            for (int i = 0; i < derivedPropertyColumns.length; i++) {
                boolean found = false;
                for (int j = 0; j < primaryKeyColumns.length; j++) {
                    if (derivedPropertyColumns[i].equals(primaryKeyColumns[j])) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }

        protected void mapChanged(Object mapListenerOwner, MapEvent e) {
            CriteriaBeanCollection collection = (CriteriaBeanCollection) mapListenerOwner;

            if (!collection.inited) return;

            Object object = e.getValue();

            if (object == null) {
                return;
            }

            Object[] values = getValuesForColumns(object);
            Object[] objects = getObjectsForColumnsAndValues(values);

            if (objects != null) {
                for (int j = 0; j < objects.length; j++) {
                    if (!collection.contains(objects[j]) && collection.mergedCriteria.evaluate(objects[j])) {
                        collection.add(objects[j]);
                        if (log.isDebug()) log.debug("Found an update to " + object + " and updated " + objects[j]);
                    }
                }
            }
        }

        private Object[] getObjectsForColumnsAndValues(Object[] values) {
            Object[] objects;
            if (usePrimaryKey) {
                if (compositePrimaryKey) {
                    Object[] arrayOfListsOfKeys = new Object[]{Arrays.asList(values)};
                    objects = beanFactory.getObjects(arrayOfListsOfKeys);
                } else {
                    objects = beanFactory.getObjects(values);
                }
            } else {
                SqlCriterionImpl criterion = new SqlCriterionImpl(beanFactory.getDataSourceName(), type, tables, columns, values);
                CriterionGroup criterionGroup = new CriterionGroup("", new Criterion[]{criterion});
                objects = beanFactory.getObjects(criterionGroup);
            }
            return objects;
        }

        private Object[] getValuesForColumns(Object object) {
            Object[] values = new Object[columns.length];
            for (int j = 0; j < columns.length; j++) {
                values[j] = TabularDataBeanFactory.getColumnValueForBean(object, columns[j]);
            }
            return values;
        }

        private static class SqlCriterionImpl implements SqlCriterion {
            private Column[] leftOperandColumns;
            private CatalogSchemaTable[] joinTables;
            private String whereClause;
            private Class<?> type;
            private SQLFormatter formatter;

            /**
             * Creates a new SqlCriterionImpl that takes an array of tables that are affected, an array of
             * columns and an array of values for those columns.  Values in the array should correspond to the
             * column in the same index into the array.
             *
             * @param tables
             * @param columns
             * @param values
             */
            public SqlCriterionImpl(String dataSourceName, Class<?> type, CatalogSchemaTable[] tables, Column[] columns, Object[] values) {
                assert columns != null && values != null && columns.length == values.length : "Illegal arguments passed to SqlCriterionImpl constructor";
                this.joinTables = tables;
                this.leftOperandColumns = columns;
                formatter = SQLFormatter.getInstance(dataSourceName);
                this.type = type;
                createWhereClause(values);
            }

            private void createWhereClause(Object[] values) {
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < leftOperandColumns.length; i++) {
                    buf.append(leftOperandColumns[i]);
                    buf.append(" = ");
                    buf.append(formatter.format(values[i]));
                    if (i < leftOperandColumns.length - 1) {
                        buf.append(" AND ");
                    }
                }
                whereClause = buf.toString();
            }

            public SQL joinSql(Class beanType, SQL sql) {
                return sql.joinWhereClause(getWhereClause(), joinTables);
            }

            private String getWhereClause() {
                return whereClause;
            }

            public Column[] getDirectLeftOperandColumns(Class beanType) {
                return leftOperandColumns;
            }

            public String getName() {
                return "Unnamed criterion";
            }

            public boolean evaluate(Object bean) {
                throw new UnsupportedOperationException("SqlCriterionImpl.evaluate is not implemented at this time");
            }

            public String getDescription() {
                return null;
            }

            public boolean isValidForBeanType(Class beanType) {
                return type.isAssignableFrom(beanType);
            }
        }
    }


    /**
     * A CriteriaGroup identified by a String id
     */
    public static class NamedCriteria {
        String id;
        CriterionGroup criterionGroup;

        public NamedCriteria(String id, CriterionGroup criterionGroup) {
            this.id = id;
            this.criterionGroup = criterionGroup;
        }

        public CriterionGroup getCriterionGroup() {
            return criterionGroup;
        }

        public String getId() {
            return id;
        }
    }

}
