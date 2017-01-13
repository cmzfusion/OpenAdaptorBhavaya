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

import org.bhavaya.beans.criterion.CriteriaBeanCollection;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.collection.*;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * BeanFactories are factories for individual beans and BeanCollections.  BeanCollections are sets of beans
 * defined by a CritierionGroup.
 * <p/>
 * Typically, there is a BeanFactory for each type of bean.  Where a client requires beans to be loaded in a transaction,
 * it must load the bean using a separate dataSource, for which autoCommit is set to false. At the end of the transaction,
 * as well as committing on the dataSource, it must call clearAllBeanFactories(Connection).  In the next transaction, the data is then
 * reloaded from the database.
 * <p/>
 * Each <code>type</code> is often a subclass of org.bhavaya.beans.Bean, although this is not enforced.
 * For each <code>type</code> there is a <code>generatedClass</code>.  The <code>generatedClass</code> is a subclass of <code>type</code>.
 * <code>generatedClass</code> implements many of the getter/setter methods, incorporating logic for firing property change events, lazy loading etc.
 * When a request is made for an instance of <code>type</code>, an instance of <code>generatedClass</code> is actually returned.
 * In fact, there is no requirement for a class representing <code>type</code> to exist,
 * <p/>
 * BeanFactories have relationshipa equivalent to the inheritance relationships between beans.
 * E.g. an Instrument BeanFactory relies on Bond BeanFactory and FxInstrument BeanFactory to actually instantiate beans.
 * A client may request beans from an "super" BeanFactory, which may return a mix of "sub" types.
 * E.g. requesting all instruments valid after a certain date, may return a CriteriaBeanCollection containing both Bonds and FxInstruments.
 * In this way, BeanFactory facilitates polymorphism, something EntityBeans in EJB have not yet managed to do.
 * <p/>
 * BeanFactory use an Associations to store beans, which means it is possible to get the bean for a key and also the key for a bean.
 * <p/>
 * Many public methods are synchronized as they can be called from different threads, leading to inconsistent state.
 * They are synchronized on the class rather than the instance because BeanFactories depend on each other,
 * based on the relationships between beans.  E.g. loading a Trade may load a TradeGroup, a Bond and a Rating.
 * Loading a Portfolio may load an AccountDate, loading an AccountDate may load a Portfolio.  If methods were
 * synchronized on instances deadlocks would arise, e.g if a TradeGroup BeanFactory locked itself waiting for a Trade
 * BeanFactory locked itself waiting for a TradeGroup.  The only way to lock on instances, is to use a single queue to process
 * all requests to all BeanFactories.  The queue could then order requests in a way that prevented deadlocks.  However this is
 * a very complicated undertaking, therefore synchronizing on class has been used.
 * <p/>
 * BeanFactory relies on Schema for meta-data about each type, such as how to generate beans of that type (e.g. a sql select statement,
 * or a stored procedure), the columns names which represent the key for those beans, whether the dataset is large and volatile...
 * <p/>
 * Common Usages:
 * 1) To obtain a single bean:
 * Instrument instrument = (Instrument)BeanFactory.getInstance(Instrument.class).get(new Integer(123464455));
 * <p/>
 * 2) To obtain a BeanCollection:
 * String instrumentsForPortfolio = ....;
 * BeanFactory instrumentFactory = BeanFactory.getInstance(Instrument.class);
 * CriterionGroup criterionGroup = ...
 * BeanCollection instruments = instrumentFactory.getBeanCollection(criterionGroup);
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.47 $
 */
public abstract class BeanFactory implements BeanMap, Association, CriterionBeanSource {
    private static final Log log = Log.getCategory(BeanFactory.class);

    private static final long PARTIAL_INFLATION_WAIT_TIMEOUT_MILLIS = 2 * 60 * 1000; // 2 minutes

    private static final Map instanceCache = new HashMap();
    private static volatile boolean postInited = true; // default this to true
    private static final Map instancesByBeanFactoryType = new HashMap();
    private static final Set initedBeanFactoryTypes = new HashSet();
    private static final List instanceList = new ArrayList();
    private static final ThreadLocal loadProgressMonitors = new ThreadLocal();
    protected static int loadStackSize = 0;
    protected static final Set loadStack = new LinkedHashSet();
    private static final Map beanInfoByBean = new WeakHashMap();
    private static final Set strongReferences = new HashSet();
    private static final Map lazyNullsByType = new HashMap();
    private static final Map locks = new HashMap();
    protected static final Map partiallyInflatedBeans = new HashMap();

    protected final MapEvent COMMIT_EVENT = new MapEvent(this, MapEvent.COMMIT, null, null);
    protected final MapEvent ALL_ROWS_EVENT = new MapEvent(this, MapEvent.ALL_ROWS, null, null);

    private boolean inited = false;
    private final Object initLock = new Object();
    private final Association primaryStore;
    private Map indexedStoresByName;
    private IndexedAssociation[] indexedStores;
    private final Set oneToManyIndexedValuesToCommit = new LinkedHashSet();
    private Class type;
    private String dataSourceName;
    private String referenceType;
    private Class superType;
    private Class ancestorType;
    private boolean hasSubClasses = false;
    private boolean beanLifeCycleable = false;
    private boolean indexable = false;
    private boolean classLoaded = false;
    private Schema schema;
    private final Object listenerLock = new Object();
    private List mapListeners = new ArrayList();
    protected final String logPrefix;
    protected final Association discardedBeans = new SynchronizedAssociation(ReferenceAssociation.newWeakInstance());
    private CriteriaBeanCollection allBeansCollection;
    private final ThreadLocal lockThreadLocal = new ThreadLocal();

    static {
        BeanUtilities.addPersistenceDelegate(BeanFactory.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                BeanFactory oldBeanFactory = (BeanFactory) oldInstance;
                return new Expression(oldInstance, BeanFactory.class, "getInstance", new Object[]{oldBeanFactory.getType(), oldBeanFactory.getDataSourceName()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    public abstract Object[] getObjects(CriterionGroup criterionGroup);

    protected abstract Object load(Object key, String indexName);

    protected abstract Object createKey(Object bean, String indexName);

    protected abstract void clearImpl();

    /**
     * Returns an instance of a BeanFactory for a given type and default dataSource.
     * All BeanFactories are created by the appropriate definitions in schema.xml.
     */
    public static BeanFactory getInstance(Class type) {
        return getInstance(type, null);
    }

    /**
     * Returns an instance of a BeanFactory for a given type and dataSource.
     * All BeanFactories are created by the appropriate definitions in schema.xml.
     */
    public static BeanFactory getInstance(Class type, String dataSourceName) {
        BeanFactory instance;
        Map instancesForDataSource;

        synchronized (instanceCache) {
            // getInstance can get called many times, they makes it faster
            instancesForDataSource = (Map) instanceCache.get(dataSourceName);
            if (instancesForDataSource == null) {
                instancesForDataSource = new HashMap();
                instanceCache.put(dataSourceName, instancesForDataSource);
            }
            instance = (BeanFactory) instancesForDataSource.get(type);
        }
        if (instance != null) return instance;

        instance = getInstanceSlow(type, dataSourceName);

        synchronized (instanceCache) {
            instancesForDataSource.put(type, instance);
        }

        return instance;
    }

    public static Class[] getBeanFactoryClasses() {
        return getBeanFactoryClasses(null);
    }

    public static Class[] getBeanFactoryClasses(String dataSourceName) {
        Class[] result = new Class[0];
        synchronized (instanceCache) {
            // getInstance can get called many times, they makes it faster
            Map instancesForDataSource = (Map) instanceCache.get(dataSourceName);
            if (instancesForDataSource != null) {
                result = new Class[instancesForDataSource.size()];
                Iterator keyIterator = instancesForDataSource.keySet().iterator();
                for(int i=0; i<result.length; i++) {
                    result[i] = (Class) keyIterator.next();
                }
            }
        }
        return result;
    }

    private static BeanFactory getInstanceSlow(Class type, String dataSourceName) {
        Schema schema = Schema.getInstance(type);
        type = schema.getGeneratedClass(); //type must always refer to generatedClass
        if (dataSourceName == null) dataSourceName = schema.getDefaultDataSourceName();

        Map instancesByTypeForDataSource = getInstances(dataSourceName, schema.getBeanFactoryType());
        BeanFactory instance = (BeanFactory) instancesByTypeForDataSource.get(type);
        if (instance == null) throw new RuntimeException("No BeanFactory for type: " + type.getName() + ", datasource: " + dataSourceName);
        instance.init();
        return instance;
    }

    private void loadClass() {
        // Initialise class to prevent any class load order effects later on.
        // Schema loads the class without initing its static variables, these variables may reference BeanFactory for loading their value
        try {
            if (classLoaded) return;
            Class.forName(type.getName(), true, type.getClassLoader());
            classLoaded = true;
        } catch (ClassNotFoundException e) {
            log.error(e);
        }
    }

    public static BeanFactory[] getInstances() {
        synchronized (instanceList) {
            return (BeanFactory[]) instanceList.toArray(new BeanFactory[instanceList.size()]);
        }
    }

    protected static Map getInstances(String dataSourceName, Class beanFactoryType) {
        assert beanFactoryType != null : "beanFactoryType cannot be null";

        synchronized (instancesByBeanFactoryType) {
            Object key = new BeanFactoryTypeKey(beanFactoryType, dataSourceName);
            Map instancesByTypeForBeanFactoryType = (Map) instancesByBeanFactoryType.get(key);
            if (instancesByTypeForBeanFactoryType == null) {
                if (log.isDebug()) log.debug("Adding BeanFactories for: " + beanFactoryType.getName() + " and dataSource: '" + dataSourceName + "'");
                instancesByTypeForBeanFactoryType = new LinkedHashMap(); // instances need to be created in the order the appear in Schema.getInstances()

                Schema[] schemas = Schema.getInstances();
                for (int i = 0; i < schemas.length; i++) {
                    Schema schema = schemas[i];
                    if (schema.getBeanFactoryType() == beanFactoryType) {
                        BeanFactory instance = newBeanFactory(beanFactoryType, dataSourceName, schema);
                        instancesByTypeForBeanFactoryType.put(instance.getType(), instance);
                        synchronized (instanceList) {
                            instanceList.add(instance);
                        }
                    }
                }
                if (instancesByBeanFactoryType.containsKey(key)) {
                    log.fatal("Error in initialisation of beanfactories, constructing duplicate factories");
                    System.exit(1);
                }
                instancesByBeanFactoryType.put(key, instancesByTypeForBeanFactoryType);

                // init after it has been placed into instanceList and instancesByBeanFactoryType
                initBeanFactoryType(beanFactoryType);
            }

            return instancesByTypeForBeanFactoryType;
        }
    }

    private static BeanFactory newBeanFactory(Class beanFactoryType, String dataSourceName, Schema schema) {
        try {
            Constructor constructor = beanFactoryType.getConstructor(new Class[]{Class.class, String.class});
            return (BeanFactory) constructor.newInstance(new Object[]{schema.getGeneratedClass(), dataSourceName});
        } catch (Exception e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    public static void setPostInited(boolean postInited) {
        BeanFactory.postInited = postInited;
    }

    private static void initBeanFactoryType(Class beanFactoryType) {
        // Run once per BeanFactory Type
        synchronized (initedBeanFactoryTypes) {
            if (initedBeanFactoryTypes.contains(beanFactoryType)) return; //only once regardless of datasource
            initedBeanFactoryTypes.add(beanFactoryType);
            try {
                Method initMethod = beanFactoryType.getMethod("initBeanFactoryType");
                log.info("Calling: " + initMethod);
                initMethod.invoke(beanFactoryType);
                log.info("Initialised: " + beanFactoryType.getName());
            } catch (NoSuchMethodException e) {
                log.info("Not initialising: " + beanFactoryType.getName() + " as no static initBeanFactoryType method provided.");
            } catch (Exception e) {
                log.error("Error initialising: " + beanFactoryType.getName(), e);
            }

            if (postInited) postInitBeanFactoryType(beanFactoryType);

        }
    }

    public static void postInitAllBeanFactoryTypes() {
        Class[] initedBeanFactoryTypesSnapShot;
        synchronized (initedBeanFactoryTypes) {
            initedBeanFactoryTypesSnapShot = (Class[]) initedBeanFactoryTypes.toArray(new Class[initedBeanFactoryTypes.size()]);
            postInited = true;
        }

        for (int i = 0; i < initedBeanFactoryTypesSnapShot.length; i++) {
            Class beanFactoryType = initedBeanFactoryTypesSnapShot[i];
            postInitBeanFactoryType(beanFactoryType);
        }

    }

    private static void postInitBeanFactoryType(Class beanFactoryType) {
        try {
            Method initMethod = beanFactoryType.getMethod("postInitBeanFactoryType");
            log.info("Calling: " + initMethod);
            initMethod.invoke(beanFactoryType);
            log.info("Post initialised: " + beanFactoryType.getName());
        } catch (NoSuchMethodException e) {
            log.info("Not post initialising: " + beanFactoryType.getName() + " as no static postInitBeanFactoryType method provided.");
        } catch (Exception e) {
            log.error("Error post initialising: " + beanFactoryType.getName(), e);
        }
    }

    public static BeanFactory getBeanFactoryForValue(Object value) {
        BeanFactory beanFactory;
        synchronized (beanInfoByBean) {
            beanFactory = (BeanFactory) beanInfoByBean.get(value);
        }
        if (beanFactory == null) beanFactory = BeanFactory.getInstance(value.getClass());
        return beanFactory;
    }

    public static String getDatasource(Object value) {
        return getBeanFactoryForValue(value).getDataSourceName();
    }

    private static Object createLock(Class type) {
        if (type == null) throw new RuntimeException("Cannot create a lock for a null type");
        synchronized (locks) {
            Object lock = locks.get(type);
            if (lock == null) {
                lock = new Object();
                locks.put(type, lock);
            }
            return lock;
        }
    }

    public Object getLock() {
        Object lock = lockThreadLocal.get();
        if (lock == null) {
            Class lockUnderlying = ancestorType == null ? type : ancestorType; // one lock per class hierarchy
            lock = createLock(lockUnderlying);
            lockThreadLocal.set(lock);
        }
        return lock;
    }

    /**
     * Returns whether this BeanFactory's type (class) has been loaded by this beanfactory yet.  This can be used to
     * ascertain whether any data reads or rights have occured yet.
     */
    public boolean isClassLoaded() {
        return classLoaded;
    }

    protected BeanFactory(Class type, String dataSourceName) {
        this.dataSourceName = dataSourceName;
        this.type = type;
        logPrefix = getLogPrefix(this);

        if (log.isDebug()) log.debug(logPrefix + "constructing");
        this.schema = Schema.getInstance(type);

        if (schema.getSuperType() != null && Schema.hasInstance(schema.getSuperType()) && Schema.getInstance(schema.getSuperType()).hasBeanFactory()) {
            superType = Schema.getInstance(schema.getSuperType()).getGeneratedClass();

            Class ancestorTypeTemp = superType;
            while (ancestorTypeTemp != null && Schema.getInstance(ancestorTypeTemp).hasBeanFactory()) {
                ancestorType = Schema.getInstance(ancestorTypeTemp).getGeneratedClass();
                ancestorTypeTemp = Schema.getInstance(ancestorTypeTemp).getSuperType();
            }
        }

        referenceType = schema.getReferenceType();
        beanLifeCycleable = LifeCycle.class.isAssignableFrom(type);
        indexable = Indexable.class.isAssignableFrom(type);
        hasSubClasses = schema.hasSubClasses();

        primaryStore = getDefaultStore(null);

        Index[] indices = schema.getIndicies();
        if (indices != null) {
            this.indexedStoresByName = new HashMap(indices.length);
            for (int i = 0; i < indices.length; i++) {
                Index index = indices[i];
                if (index.isUnique()) {
                    indexedStoresByName.put(index.getName(), new IndexedAssociation(this, type, index));
                } else {
                    indexedStoresByName.put(index.getName(), new OneToManyIndexedAssociation(this, type, index));
                    if (!indexable && referenceType.equalsIgnoreCase(Schema.STRONG_REFERENCE)) log.error(logPrefix + "has non-unique index: " + index.getName() + " but " + type.getName() + " does not implement " + Indexable.class.getName() + ", even though BeanFactory is using " + referenceType + " references.");
                }
            }
            indexedStores = (IndexedAssociation[]) indexedStoresByName.values().toArray(new IndexedAssociation[indexedStoresByName.values().size()]);
        }
    }

    protected static final String getLogPrefix(BeanFactory beanFactory) {
        return beanFactory.getType().getName() + "/" + beanFactory.getDataSourceName() + "@" + System.identityHashCode(beanFactory) + ": ";
    }

    protected void removeReference(Object key, String indexName) {
        if (log.isDebug()) log.debug(logPrefix + "removing reference: " + type.getName() + (indexName != null ? ":" + indexName : "") + ": " + key);
    }

    public int getLoadRowCount(CriterionGroup criterionGroup) {
        return -1;
    }

    private void init() {
        // Run once per BeanFactory instance
        synchronized (initLock) {
            if (inited) return;
            inited = true;
            initImpl();
        }
    }

    protected void initImpl() {
    }

    public final Class getType() {
        return type;
    }

    public final Class getSuperType() {
        return superType;
    }

    protected final Schema getSchema() {
        return schema;
    }

    protected final boolean isBeanLifeCycleable() {
        return beanLifeCycleable;
    }

    protected final boolean hasSubClasses() {
        return hasSubClasses;
    }

    public final String getDataSourceName() {
        return dataSourceName;
    }

    protected String getReferenceType() {
        return referenceType;
    }

    protected Association getDefaultStore(String indexName) {
        if (!referenceType.equalsIgnoreCase(Schema.STRONG_REFERENCE)) {
            Schema schema = Schema.getInstance(type);
            if (schema.getDataQuantity().equals(Schema.LOW) && schema.getDataVolatility().equals(Schema.LOW)) {
                return new SynchronizedAssociation(new DefaultAssociation(new LinkedHashMap()));
            } else {
                ReferenceAssociation referenceAssociation;
                if (referenceType.equalsIgnoreCase(Schema.WEAK_REFERENCE)) {
                    referenceAssociation = ReferenceAssociation.newWeakInstance(new LinkedHashMap());
                } else if (referenceType.equalsIgnoreCase(Schema.SOFT_REFERENCE)) {
                    referenceAssociation = ReferenceAssociation.newSoftInstance(new LinkedHashMap());
                } else {
                    throw new IllegalStateException("Unknown reference type.");
                }
                referenceAssociation.addReferenceRemovedListener(new ReferenceRemovedListener(this, indexName));
                return new SynchronizedAssociation(referenceAssociation);
            }
        } else {
            return new SynchronizedAssociation(new DefaultAssociation(new LinkedHashMap()));
        }
    }

    protected Association getPrimaryStore() {
        loadClass();
        return primaryStore;
    }

    protected Association getStore(String indexName) {
        if (indexName == null) {
            return getPrimaryStore();
        } else {
            return (Association) indexedStoresByName.get(indexName);
        }
    }

    protected IndexedAssociation[] getIndexedStores() {
        return indexedStores;
    }

    protected boolean isUnique(String indexName) {
        if (indexName == null) {
            return true;
        } else {
            IndexedAssociation indexedStore = (IndexedAssociation) indexedStoresByName.get(indexName);
            return indexedStore.isUnique();
        }
    }


    public BeanFactory getSuperBeanFactory() {
        if (superType == null) return null;
        return BeanFactory.getInstance(superType, dataSourceName);
    }

    public BeanFactory getAncestorBeanFactory() {
        if (ancestorType == null) return null;
        return BeanFactory.getInstance(ancestorType, dataSourceName);
    }

    protected Class getAncestorType() {
        return ancestorType;
    }

    /**
     * Creates a new instance of a bean for a given type.  This ensures that the instance created
     * is actually an instance of generatedClass.
     */
    public static Object newBeanInstance(Class type) {
        try {
            Class generatedClass = Schema.getInstance(type).getGeneratedClass();
            return generatedClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate generated type for type: " + type, e);
        }
    }

    public Object newBeanInstance() {
        return newBeanInstance(type);
    }

    /**
     * Compares the keys of value1 and value2, returns true if they are equal.
     *
     * @param value1
     * @param value2
     * @return true if the key of both value1 and value2 are equal.
     */
    public static boolean equalsBean(Object value1, Object value2) {
        Object key1 = getKeyForBean(value1);
        Object key2 = getKeyForBean(value2);
        return Utilities.equals(key1, key2);
    }

    public boolean equals(Object value1, Object value2) {
        Object key1 = getKeyForValue(value1);
        Object key2 = getKeyForValue(value2);
        return Utilities.equals(key1, key2);
    }

    public void putValue(Object value) {
        Object key = getKeyForValue(value);
        put(key, value);
    }

    public void removeValue(Object value) {
        Object key = getKeyForValue(value);
        remove(key);
    }

    public void updateValue(Object value) {
        Object key = getKeyForValue(value);
        fireMapChangedThroughHierarchy(MapEvent.UPDATE, key, value);
        fireCommit();
    }

    public CriteriaBeanCollection getAllBeanCollection() {
        if (allBeansCollection == null) {
            // note: schema.getType is different to type, schema.getType is likely to be a superclass of type
            allBeansCollection = createCriteriaBeanCollection(schema.getType(), new CriterionGroup("All", Criterion.ALL_CRITERION));
        }
        return allBeansCollection;
    }

    public CriteriaBeanCollection getBeanCollection(CriterionGroup criterionGroup) {
        // note: schema.getType is different to type, schema.getType is likely to be a superclass of type
        return createCriteriaBeanCollection(schema.getType(), criterionGroup);
    }

    protected CriteriaBeanCollection createCriteriaBeanCollection(Class type, CriterionGroup primaryCriteria) {
        return new CriteriaBeanCollection(this, type, primaryCriteria);
    }

    public Object[] getAllObjects() {
        return getObjects(new CriterionGroup("All", Criterion.ALL_CRITERION));
    }

    public Object[] getObjects(Object[] keys) {
        return getObjects(keys, null);
    }

    public Object[] getObjects(CriterionGroup criterionGroup, LoadProgressMonitor loadProgressMonitor) {
        BeanFactory.loadProgressMonitors.set(loadProgressMonitor);
        try {
            return getObjects(criterionGroup);
        } finally {
            BeanFactory.loadProgressMonitors.set(null);
        }
    }

    /**
     * Returns the bean with the given key.  Care must be taken to ensure that the key is of an instance with
     * compatible equals and hashCode as the key that will be generated by BeanFactory.  For example, if a bean is
     * identified by the number "1233353", it is important whether this is a Integer, Double, BigDecimal, String etc.
     * BeanFactory decides the type of the key based on the type of the column in the database table.
     * <p/>
     * Where a bean is identified by a compound key, the key must be an ArrayList of key component values.  The values
     * must be ordered alphabetically by column name.
     */
    public Object get(Object key) {
        return get(key, null);
    }

    public Object get(Object key, String indexName) {
        if (key == null) return null;
        Association store = getStore(indexName);
        if (store == null) throw new RuntimeException(logPrefix + "Could not find store for index: " + indexName);

        Object bean = store.get(key);
        if (bean == null && !store.containsKey(key)) {
            // can't find bean in cache, load the bean
            bean = load(key, indexName);
            if (bean == null) {
                bean = putNullKey(key, indexName, store);
            }
        }
        waitForBeanToInflate(bean);
        return bean;
    }

    protected static void addPartiallyInflatedBean(Object bean) {
        if (bean == null) return;
        synchronized (partiallyInflatedBeans) {
            partiallyInflatedBeans.put(bean, Thread.currentThread());
        }
    }

    protected static boolean isPartiallyInflated(Object bean) {
        if (bean == null) return false;
        synchronized (partiallyInflatedBeans) {
            return partiallyInflatedBeans.containsKey(bean);
        }
    }

    public boolean isFullyInflatedKey(Object key, String indexName) {
        Association store = getStore(indexName);
        if (store == null) throw new RuntimeException(logPrefix + "Could not find store for index: " + indexName);
        Object bean = store.get(key);
        if (bean == null) return false;
        return !isPartiallyInflated(bean);
    }

    protected static void removePartiallyInflatedBeans(Set beans) {
        synchronized (partiallyInflatedBeans) {
            for (Iterator iterator = beans.iterator(); iterator.hasNext();) {
                partiallyInflatedBeans.remove(iterator.next());
            }
            partiallyInflatedBeans.notifyAll();
        }
    }

    private static void clearPartiallyInflatedBeans() {
        synchronized (partiallyInflatedBeans) {
            partiallyInflatedBeans.clear();
        }
    }

    protected void waitForBeansToInflate(Set beans) {
        synchronized (partiallyInflatedBeans) {
            for (Iterator iterator = beans.iterator(); iterator.hasNext();) {
                waitForBeanToInflate(iterator.next());
            }
        }
    }

    protected void waitForBeanToInflate(Object bean) {
        if (bean == null) return;
        try {
            synchronized (partiallyInflatedBeans) {
                Thread inflatingThread = (Thread) partiallyInflatedBeans.get(bean);
                Thread originalInflatingThread = inflatingThread;
                Thread currentThread = Thread.currentThread();
                long startTime = System.currentTimeMillis();
                boolean waited = false;

                while (inflatingThread != null && inflatingThread != currentThread) {
                    // some other thread still hasnt fully inflated this bean yet so wait
                    if ((System.currentTimeMillis() - startTime) > PARTIAL_INFLATION_WAIT_TIMEOUT_MILLIS) {
                        log.warn(getLogPrefix(this) + "'" + currentThread.getName() + "' timed-out waiting for full inflation of: " + systemToString(bean) + " by '" + originalInflatingThread.getName() + "'");
                        break;
                    } else {
                        if (!waited) log.info(getLogPrefix(this) + "'" + currentThread.getName() + "' waiting for full inflation of: " + systemToString(bean) + " by '" + originalInflatingThread.getName() + "'");
                        partiallyInflatedBeans.wait(PARTIAL_INFLATION_WAIT_TIMEOUT_MILLIS);
                        inflatingThread = (Thread) partiallyInflatedBeans.get(bean);
                        waited = true;
                    }
                }

                if (waited) {
                    log.info(getLogPrefix(this) + "'" + currentThread.getName() + "' finished wait for full inflation of: " + systemToString(bean) + " by '" + originalInflatingThread.getName() + "' for " + (System.currentTimeMillis() - startTime) + " millis");
                }
            }
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    private static String systemToString(Object o) {
        return o.getClass().getName() + "@" + System.identityHashCode(o);
    }

    protected Object putNullKey(Object key, String indexName, Association store) {
        synchronized (getLock()) {
            if (!isUnique(indexName)) {
                DefaultBeanCollection emptyBeanCollection = new DefaultBeanCollection(type);
                store.put(key, emptyBeanCollection);
                return emptyBeanCollection;
            } else {
                store.put(key, null);
                return null;
            }
        }
    }

    /**
     * An unoptimised implementation.  It is expected that subclasses override this implementation.
     *
     * @param keys
     * @param indexName
     */
    public Object[] getObjects(Object[] keys, String indexName) {
        if (keys == null) return null;
        List objects = new ArrayList(keys.length);
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            Object object = get(key, indexName);
            if (object != null) objects.add(object);
        }
        Class returnType = schema.getType(); // can't get the generated type, because BeanFactories contain subclass instances which do not extend the generated type
        Object[] array = (Object[]) Array.newInstance(returnType, objects.size());
        return objects.toArray(array);
    }

    public Object put(Object key, Object value) {
        return put(key, value, true, true);
    }

    public Object put(Object key, Object value, boolean fireCommit) {
        return put(key, value, fireCommit, true);
    }

    private Object put(Object key, Object value, boolean fireCommit, boolean fireMapChanged) {
        boolean canPut;
        Object existingValue = null;

        synchronized (getLock()) {
            canPut = canPut(key, value);
            if (canPut) {
                existingValue = putInPrimaryStore(key, value);
                putIntoIndexedStores(value);
            }
        }

        if (getSuperBeanFactory() != null) {
            getSuperBeanFactory().put(key, value, fireCommit, fireMapChanged);
        }

        if (canPut) {
            if (fireMapChanged) fireMapChanged(MapEvent.INSERT, key, value);
            if (fireCommit) fireMapChanged(COMMIT_EVENT);
        }

        return existingValue;
    }

    protected Object putInPrimaryStore(Object key, Object value) {
        Object existingValue = getPrimaryStore().put(key, value);
        addStrongReference(value);
        synchronized (beanInfoByBean) {
            if (!beanInfoByBean.containsKey(value)) {
                assert value == null || value.getClass().equals(this.getType()) : "Invalid BeanFactory.putInPrimaryStore, beanFactory type is: " + getType().getName() + ", value type is: " + value.getClass().getName() + ", key is: " + key;
                beanInfoByBean.put(value, this);
            }
        }
        return existingValue;
    }

    protected static void addStrongReference(Object value) {
        synchronized (strongReferences) {
            strongReferences.add(value);
        }
    }

    private void putIntoIndexedStores(Object value) {
        if (indexedStores != null) {
            for (int i = 0; i < indexedStores.length; i++) {
                IndexedAssociation indexedAssociation = indexedStores[i];
                Object indexKey = createKey(value, indexedAssociation.getIndexName());
                putIntoIndexedStore(indexedAssociation, indexKey, value);
            }
        }
    }

    protected Object putIntoIndexedStore(IndexedAssociation indexedStore, Object indexKey, Object value) {
        Object existingValue = indexedStore.put(indexKey, value);
        return existingValue;
    }

    private boolean canPut(Object key, Object value) {
        // do not insert new value if it already exists
        // allow null values to be replaced with non-null values
        // and, allow non-null values to be replaced with null values
        return !containsKey(key) || (getPrimaryStore().get(key) == null && value != null) || (getPrimaryStore().get(key) != null && value == null);
    }

    public void putAll(Map t) {
        putAll(t, true, true);
    }

    public void putAll(Map t, boolean fireCommit) {
        putAll(t, fireCommit, true);
    }

    private void putAll(Map t, boolean fireCommit, boolean fireMapChanged) {
        for (Iterator iterator = t.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            put(key, value, false, false);
        }

        if (getSuperBeanFactory() != null) {
            if (fireMapChanged) getSuperBeanFactory().fireMapChanged(ALL_ROWS_EVENT);
            if (fireCommit) getSuperBeanFactory().fireMapChanged(COMMIT_EVENT);

        }
        if (fireMapChanged) fireMapChanged(ALL_ROWS_EVENT);
        if (fireCommit) fireMapChanged(COMMIT_EVENT);
    }

    public Object remove(Object key) {
        return remove(key, true, true);
    }

    public Object remove(Object key, boolean fireCommit) {
        return remove(key, fireCommit, true);
    }

    protected Object remove(Object key, boolean fireCommit, boolean fireMapChanged) {
        Object removedValue;

        synchronized (getLock()) {
            removedValue = getPrimaryStore().remove(key);
            if (removedValue != null) {
                discardedBeans.put(key, removedValue);
                removeFromIndexedStores(removedValue);
            }
        }

        if (getSuperBeanFactory() != null) {
            getSuperBeanFactory().remove(key, fireCommit, fireMapChanged);
        }

        if (fireMapChanged) fireMapChanged(MapEvent.DELETE, key, removedValue);
        if (fireCommit) fireMapChanged(COMMIT_EVENT);

        touch();
        return removedValue;
    }

    protected void touch() {
        loadClass();
        getPrimaryStore().size();
        discardedBeans.size(); // discardedBeans is a weak Association, ensure its reference queue is polled;
    }

    private void removeFromIndexedStores(Object removedValue) {
        if (indexedStores != null) {
            for (int i = 0; i < indexedStores.length; i++) {
                IndexedAssociation indexedAssociation = indexedStores[i];
                indexedAssociation.removeValue(removedValue);
            }
        }
    }

    /**
     * Clear all BeanFactories that use dataSource.  Typically, this is called by clients using a transactional
     * dataSource, i.e. a dataSource for which autoCommit is false.
     */
    public static void clearAllBeanFactories(String dataSourceName, Class beanFactoryType, boolean clearLowVolatilityBeanFactories, boolean fireEvents) {
        if (log.isDebug()) log.debug("Clearing BeanFactories for: " + beanFactoryType.getName() + " and dataSource: '" + dataSourceName + "'");

        BeanFactory[] instancesSnapShot;
        Object key = new BeanFactoryTypeKey(beanFactoryType, dataSourceName);

        synchronized (instancesByBeanFactoryType) {
            Map instancesByTypeForBeanFactoryType = (Map) instancesByBeanFactoryType.get(key);
            if (instancesByTypeForBeanFactoryType != null) {
                instancesSnapShot = (BeanFactory[]) instancesByTypeForBeanFactoryType.values().toArray(new BeanFactory[instancesByTypeForBeanFactoryType.values().size()]);
            } else {
                return;
            }
        }

        List beanFactoriesAffected = null;
        if (fireEvents) {
            beanFactoriesAffected = new ArrayList();
        }

        for (int i = 0; i < instancesSnapShot.length; i++) {
            BeanFactory beanFactory = instancesSnapShot[i];
            if (clearLowVolatilityBeanFactories || beanFactory.schema.getDataVolatility().equals(Schema.HIGH)) {
                beanFactory.clear(null, false, false);
                if (fireEvents) beanFactoriesAffected.add(beanFactory);
            }
        }

        if (fireEvents) {
            for (int i = 0; i < beanFactoriesAffected.size(); i++) {
                BeanFactory beanFactory = (BeanFactory) beanFactoriesAffected.get(i);
                beanFactory.fireMapChanged(beanFactory.ALL_ROWS_EVENT);
                beanFactory.fireMapChanged(beanFactory.COMMIT_EVENT);
            }
        }
    }

    /**
     * Clear all BeanFactories.
     */
    public static void clearAllBeanFactories(boolean clearLowVolatilityBeanFactories, boolean fireEvents) {
        Map snapShotOfAllInstances = new LinkedHashMap();
        synchronized (instancesByBeanFactoryType) {
            snapShotOfAllInstances.putAll(instancesByBeanFactoryType);
        }
        for (Iterator iterator = snapShotOfAllInstances.keySet().iterator(); iterator.hasNext();) {
            BeanFactoryTypeKey beanFactoryTypeKey = (BeanFactoryTypeKey) iterator.next();
            String dataSourceName = beanFactoryTypeKey.getDataSourceName();
            Class beanFactoryType = beanFactoryTypeKey.getBeanFactoryType();
            clearAllBeanFactories(dataSourceName, beanFactoryType, clearLowVolatilityBeanFactories, fireEvents);
        }
    }

    public void clear() {
        clear(null, true, true);
    }

    public void clear(boolean fireCommit) {
        clear(null, fireCommit, true);
    }

    /**
     * Clears all the data contained in store from the bean factory. Does not fire events.
     * Used to propagate bean factory clearing up in the bean factory tree.
     *
     * When clearing a child bean factory, all the child bean factory's data must also be
     * removed from the parent bean factories, in order to keep the data consistent.
     *
     * @param store containing the beans to be removed
     * @param events
     */
    private void clearSuperBeanFactory(Association store, List events) {
        synchronized (getLock()) {
            if (getSuperBeanFactory() != null) {
                getSuperBeanFactory().clearSuperBeanFactory(store, events);
            }

            discardedBeans.putAll(store);
            removeAllByKey(getPrimaryStore(), store);
            clearImpl();

            if (this.indexedStores != null) {
                for (int i = 0; i < indexedStores.length; i++) {
                    IndexedAssociation indexedStore = indexedStores[i];
                    // removing by value is pretty slow but as the indexed store might not have unique keys
                    // this is the safest way to do it.
                    removeAllByValue(indexedStore, store);
                }
            }
        }
        touch();
    }

    private void removeAllByValue(IndexedAssociation store, Association toRemove) {
        if (store.size() == 0 || toRemove.size() == 0) return;
        Iterator iterator = toRemove.values().iterator();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            store.removeValue(value);
        }
    }

    private void removeAllByKey(Association store, Association toRemove) {
        Iterator iterator = toRemove.keySet().iterator();
        while (iterator.hasNext()) {
            Object key = iterator.next();
            store.remove(key);
        }
    }

    /**
     * Clears this (child) factory and all the child factories of this (child) factory.
     * Used to propagate bean factory clearing down the bean factory tree.
     * @param events
     */
    private void clearChildBeanFactory(List events, List childBeanFactories) {
        synchronized (getLock()) {

            childBeanFactories.add(this);
            discardedBeans.putAll(getPrimaryStore());
            getPrimaryStore().clear();
            clearImpl();

            if (indexedStores != null) {
                for (int i = 0; i < indexedStores.length; i++) {
                    IndexedAssociation indexedAssociation = indexedStores[i];
                    indexedAssociation.clear();
                }
            }

            Class[] subClasses = getSchema().getSubClasses();
            for (int i = 0; i < subClasses.length; i++) {
                Class subClass = subClasses[i];
                BeanFactory subClassBeanFactory = BeanFactory.getInstance(subClass);
                subClassBeanFactory.clearChildBeanFactory(events, childBeanFactories);
            }
        }

        touch();
    }

    protected void clear(List events, boolean fireCommit, boolean fireMapChanged) {
        LinkedList childBeanFactories = new LinkedList();
        synchronized (getLock()) {
            if (getSuperBeanFactory() != null) {
                getSuperBeanFactory().clearSuperBeanFactory(getPrimaryStore(), events);
            }

            discardedBeans.putAll(getPrimaryStore());
            getPrimaryStore().clear();
            clearImpl();

            if (indexedStores != null) {
                for (int i = 0; i < indexedStores.length; i++) {
                    IndexedAssociation indexedAssociation = indexedStores[i];
                    indexedAssociation.clear();
                }
            }

            // clear also all child bean factories
            Class[] subClasses = getSchema().getSubClasses();
            for (int i = 0; i < subClasses.length; i++) {
                Class subClass = subClasses[i];
                BeanFactory subClassBeanFactory = BeanFactory.getInstance(subClass);
                subClassBeanFactory.clearChildBeanFactory(events, childBeanFactories);
            }
        }

        if (getSuperBeanFactory() != null) {
            if (fireMapChanged) getSuperBeanFactory().fireMapChanged(events, ALL_ROWS_EVENT);
            if (fireCommit) getSuperBeanFactory().fireMapChanged(events, COMMIT_EVENT);
        }

        if (fireMapChanged) fireMapChanged(events, ALL_ROWS_EVENT);
        if (fireCommit) fireMapChanged(events, COMMIT_EVENT);

        Iterator it = childBeanFactories.iterator();
        while (it.hasNext()) {
            BeanFactory beanFactory = (BeanFactory) it.next();
            if (fireMapChanged) beanFactory.fireMapChanged(events, ALL_ROWS_EVENT);
            if (fireCommit) beanFactory.fireMapChanged(events, COMMIT_EVENT);
        }

        touch();
    }

    public int size() {
        return getPrimaryStore().size();
    }

    public boolean isEmpty() {
        return getPrimaryStore().isEmpty();
    }

    public boolean containsKey(Object key) {
        return getPrimaryStore().containsKey(key);
    }

    public boolean containsKey(Object key, String indexName) {
        if (indexName == null) return containsKey(key);
        IndexedAssociation indexedStore = (IndexedAssociation) indexedStoresByName.get(indexName);
        return indexedStore.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return getPrimaryStore().containsValue(value);
    }

    public boolean containsValue(Object value, String indexName) {
        if (indexName == null) return containsValue(value);
        IndexedAssociation indexedStore = (IndexedAssociation) indexedStoresByName.get(indexName);
        return indexedStore.containsValue(value);
    }

    public Set keySet() {
        return getPrimaryStore().keySet();
    }

    public Collection values() {
        return getPrimaryStore().values();
    }

    public Set entrySet() {
        return getPrimaryStore().entrySet();
    }

    public void addMapListener(MapListener l) {
        if (log.isDebug()) log.debug(logPrefix + "addMapListener: " + l);
        synchronized (listenerLock) {
            mapListeners = Utilities.add(l, mapListeners);
        }
    }

    public void removeMapListener(MapListener l) {
        if (log.isDebug()) log.debug(logPrefix + "removeMapListener: " + l);
        synchronized (listenerLock) {
            mapListeners = Utilities.remove(l, mapListeners);
        }
    }

    public int getMapListenerCount() {
        synchronized (listenerLock) {
            return mapListeners.size();
        }
    }

    public void fireCommit() {
        fireCommit(null);
    }

    protected void fireCommit(List events) {
        fireMapChangedThroughHierarchy(events, MapEvent.COMMIT, null, null);
    }

    protected void fireMapChangedThroughHierarchy(int eventType, Object key, Object value) {
        fireMapChanged(null, eventType, key, value);
    }

    protected void fireMapChangedThroughHierarchy(List events, int eventType, Object key, Object value) {
        if (getSuperBeanFactory() != null) {
            getSuperBeanFactory().fireMapChangedThroughHierarchy(events, eventType, key, value);
        }

        fireMapChanged(events, eventType, key, value);
    }

    protected void fireMapChanged(int eventType, Object key, Object value) {
        fireMapChanged(null, eventType, key, value);
    }

    protected void fireMapChanged(List events, int eventType, Object key, Object value) {
        MapEvent e = new MapEvent(this, eventType, key, value);
        fireMapChanged(events, e);
    }

    public void fireMapChanged(MapEvent e) {
        fireMapChanged(null, e);
    }

    protected void fireMapChanged(List events, MapEvent e) {
        if (events == null) {
            if (log.isDebug()) log.debug(logPrefix + "fire " + MapEvent.getName(e.getType()) + " for value with key: " + e.getKey());

            List mapListenersSnapShot;
            synchronized (listenerLock) {
                mapListenersSnapShot = mapListeners;
            }

            for (int i = 0; i < mapListenersSnapShot.size(); i++) {
                MapListener mapListener = (MapListener) mapListenersSnapShot.get(i);
                try {
                    mapListener.mapChanged(e);
                } catch (Exception ex) {
                    log.error(logPrefix + "error firing: " + e, ex);
                }
            }

            fireOneToManyIndexesValuesCommit();
        } else {
            events.add(e);
        }
    }

    private void fireOneToManyIndexesValuesCommit() {
        BeanCollection[] beanCollections;
        synchronized (oneToManyIndexedValuesToCommit) {
            beanCollections = (BeanCollection[]) oneToManyIndexedValuesToCommit.toArray(new BeanCollection[oneToManyIndexedValuesToCommit.size()]);
            oneToManyIndexedValuesToCommit.clear();
        }

        for (int i = 0; i < beanCollections.length; i++) {
            BeanCollection beanCollection = beanCollections[i];
            beanCollection.fireCommit();
        }
    }

    /**
     * Returns the key for value.  The key may be a single value, e.g. a
     * String, Long, Integer, BigDecimal, Date, or it may be a compound value which is a List of single values.
     * <p/>
     * If the value is not contained in the BeanFactory, the BeanFactory will attempt to create a key based on the
     * properties of the value.
     */
    public static Object getKeyForBean(Object value) {
        return getKeyForBean(value, null);
    }

    public static Object getKeyForBean(Object value, String indexName) {
        if (value == null) return null;
        BeanFactory beanFactory = getBeanFactoryForValue(value);
        return beanFactory.getKeyForValue(value, indexName);
    }

    public Object getKeyForValue(Object value) {
        return getKeyForValue(value, null);
    }

    public Object getKeyForValue(Object value, String indexName) {
        Association store = getStore(indexName);
        Object key = store.getKeyForValue(value);
        if (key == null) key = createKey(value, indexName);
        return key;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeanFactory)) return false;

        final BeanFactory beanFactory = (BeanFactory) o;

        if (dataSourceName != null ? !dataSourceName.equals(beanFactory.dataSourceName) : beanFactory.dataSourceName != null) return false;
        if (!type.equals(beanFactory.type)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = type.hashCode();
        result = 29 * result + (dataSourceName != null ? dataSourceName.hashCode() : 0);
        return result;
    }

    public static boolean isLazy(Object value) {
        return value != null && (value instanceof Lazy) && (((Lazy) value).isLazy());
    }

    public static boolean isLazy(Object value, String property) {
        if (value == null) return false;
        if (isLazy(value)) return true;
        Field field = ClassUtilities.getField(value.getClass(), property);
        try {
            Object fieldValue = field.get(value);
            return isLazy(fieldValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object createLazyObject(Class type, Object key, String index) {
        Lazy lazyValue = (Lazy) BeanFactory.newBeanInstance(type);
        lazyValue.setLoad(new BeanFactoryLoad(BeanFactory.getInstance(type), key, index));
        return lazyValue;
    }

    /**
     * This is a hack, but I dont want to pollute the Lazy interface for now with some sort of
     * reset method.  So use field access.
     * <p/>
     * Searches through fields of value, if it finds any that implement Lazy, it then searches ech of those
     * fields for fields that are BeanFactoryLoads, and calls reset on the BeanFactoryLoad.
     *
     * @param value
     */
    public static void resetLazyObject(Object value) {
        resetLazyObject(value, new IdentityHashMap());
    }

    private static void resetLazyObject(Object value, Map checkedObjects) {
        if (value == null) return;
        if (checkedObjects.containsKey(value)) return;
        checkedObjects.put(value, null);

        Field[] fields = ClassUtilities.getFields(value.getClass());
        try {
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                Object fieldValue = field.get(value);
                if (fieldValue instanceof BeanFactoryLoad) {
                    BeanFactoryLoad beanFactoryLoad = (BeanFactoryLoad) fieldValue;
                    beanFactoryLoad.reset();
                } else if (fieldValue instanceof Lazy) {
                    resetLazyObject(fieldValue, checkedObjects);
                }
            }
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public Object getLazyNull() {
        return getLazyNull(type);
    }

    public static Object getLazyNull(Class type) {
        synchronized (lazyNullsByType) {
            Lazy lazyNull = (Lazy) lazyNullsByType.get(type);
            if (lazyNull == null) {
                try {
                    lazyNull = (Lazy) type.newInstance();
                    lazyNull.setThisInstanceAsLazyNull();
                    lazyNullsByType.put(type, lazyNull);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return lazyNull;
        }
    }

    public static boolean isLazyNull(Object value) {
        boolean result = false;
        if ( value instanceof Lazy ) {
            result = ((Lazy)value).isLazyNullInstance();
        }
        return result;
    }

    protected void resetLoadStack() {
    }

    protected static void pushBeanFactoryLoadStack(BeanFactory beanFactory) {
        LoadProgressMonitor loadProgressMonitor = (LoadProgressMonitor) loadProgressMonitors.get();
        if (loadProgressMonitor != null) loadProgressMonitor.loadingStackPush(beanFactory);
        synchronized (loadStack) {
            boolean reset = (loadStackSize == 0);
            if (reset) {
                synchronized (strongReferences) {
                    strongReferences.clear();
                }
                for (Iterator iterator = loadStack.iterator(); iterator.hasNext();) {
                    BeanFactory aBeanFactory = (BeanFactory) iterator.next();
                    aBeanFactory.resetLoadStack();
                }
                loadStack.clear();
            }
            loadStack.add(beanFactory);
            loadStackSize++;
            if (log.isDebug()) log.debug("beanFactoryLoadStackDepth push: " + (loadStackSize - 1) + " > " + loadStackSize);
        }
        beanFactory.touch();
    }

    protected static void popBeanFactoryLoadStack(BeanFactory beanFactory) {
        LoadProgressMonitor loadProgressMonitor = (LoadProgressMonitor) loadProgressMonitors.get();
        if (loadProgressMonitor != null) loadProgressMonitor.loadingStackPop();

        synchronized (loadStack) {
            loadStackSize--;
            if (log.isDebug()) log.debug("beanFactoryLoadStackDepth pop: " + (loadStackSize + 1) + " > " + loadStackSize);
            if (loadStackSize == 0) {
                clearPartiallyInflatedBeans(); // a safety measure
            }
        }
    }

    protected static void setCurrentLoadProgress(int numberOfRows) {
        LoadProgressMonitor loadProgressMonitor = (LoadProgressMonitor) BeanFactory.loadProgressMonitors.get();
        if (loadProgressMonitor != null) loadProgressMonitor.setValue(numberOfRows);
    }

    private static class ReferenceRemovedListener implements MapListener {
        private BeanFactory beanFactory;
        private String indexName;

        public ReferenceRemovedListener(BeanFactory beanFactory, String indexName) {
            this.beanFactory = beanFactory;
            this.indexName = indexName;
        }

        public void mapChanged(MapEvent e) {
            beanFactory.removeReference(e.getKey(), indexName);
        }
    }

    protected static class IndexedAssociation implements Association {
        protected BeanFactory beanFactory;
        protected Association delegate;
        protected Class type;
        private boolean unique;
        private Index index;
        protected String indexName;

        public IndexedAssociation(BeanFactory beanFactory, Class type, Index index) {
            this.beanFactory = beanFactory;
            this.type = type;
            this.index = index;
            this.indexName = index.getName();
            delegate = beanFactory.getDefaultStore(indexName);
            this.unique = index.isUnique();
        }

        public Index getIndex() {
            return index;
        }

        public String getIndexName() {
            return indexName;
        }

        public boolean isUnique() {
            return unique;
        }

        public Object put(Object key, Object value) {
            synchronized (beanInfoByBean) {
                if (!beanInfoByBean.containsKey(value)) {
                    assert value == null || value.getClass().equals(beanFactory.getType()) : "Invalid beanFactory";
                    beanInfoByBean.put(value, beanFactory);
                }
            }
            return delegate.put(key, value);
        }

        public Object get(Object key) {
            return delegate.get(key);
        }

        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        public Object getKeyForValue(Object value) {
            return delegate.getKeyForValue(value);
        }

        public void clear() {
            delegate.clear();
        }

        public int size() {
            return delegate.size();
        }

        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        public Object remove(Object key) {
            return delegate.remove(key);
        }

        public void putAll(Map t) {
            delegate.putAll(t);
        }

        public Set keySet() {
            return delegate.keySet();
        }

        public Collection values() {
            return delegate.values();
        }

        public Set entrySet() {
            return delegate.entrySet();
        }

        public void removeValue(Object value) {
            Object key = getKeyForValue(value);
            remove(key);
        }
    }

    private static class OneToManyIndexedAssociation extends IndexedAssociation {

        public OneToManyIndexedAssociation(BeanFactory beanFactory, Class componentType, Index index) {
            super(beanFactory, componentType, index);
        }

        public Object put(Object indexKey, Object value) {
            BeanCollection beanCollection = (BeanCollection) delegate.get(indexKey);

            if (beanCollection == null) {
                if (value instanceof BeanCollection) {
                    beanCollection = (BeanCollection) value;
                    delegate.put(indexKey, beanCollection);
                    addStrongReference(beanCollection);
                    synchronized (beanInfoByBean) {
                        if (!beanInfoByBean.containsKey(beanCollection)) {
                            beanInfoByBean.put(beanCollection, beanFactory);
                        }
                    }

                    for (int i = 0; i < beanCollection.size(); i++) {
                        Object o = beanCollection.get(i);
                        if (o != null && beanFactory.indexable) {
                            Indexable indexable = (Indexable) o;
                            indexable.addIndexedValue(this, beanCollection);
                        }
                    }
                } else {
                    beanCollection = new DefaultBeanCollection(type);
                    delegate.put(indexKey, beanCollection);
                    addStrongReference(beanCollection);
                    synchronized (beanInfoByBean) {
                        beanInfoByBean.put(beanCollection, beanFactory);
                    }
                    beanCollection.add(value, false);
                    synchronized (beanFactory.oneToManyIndexedValuesToCommit) {
                        beanFactory.oneToManyIndexedValuesToCommit.add(beanCollection);
                    }
                    if (value != null && beanFactory.indexable) {
                        Indexable indexable = (Indexable) value;
                        indexable.addIndexedValue(this, beanCollection);
                    }
                }
            } else {
                if (value instanceof BeanCollection) throw new RuntimeException("Cannot replace an existing indexed entry with a new indexed entry");
                boolean added = beanCollection.add(value, false);
                if (added) {
                    synchronized (beanFactory.oneToManyIndexedValuesToCommit) {
                        beanFactory.oneToManyIndexedValuesToCommit.add(beanCollection);
                    }
                    if (value != null && beanFactory.indexable) {
                        Indexable indexable = (Indexable) value;
                        indexable.addIndexedValue(this, beanCollection);
                    }
                }
            }

            return null;
        }

        public void removeValue(Object value) {
            final Indexable indexable = (Indexable) value;
            BeanCollection manyValue = (BeanCollection) indexable.getIndexedValue(this);
            if (manyValue == null) return;
            manyValue.remove(value);
            indexable.addIndexedValue(this, null);

        }

        public void clear() {
            super.clear();
        }
    }

    private static final class BeanFactoryTypeKey {
        private Class beanFactoryType;
        private String dataSourceName;

        public BeanFactoryTypeKey(Class beanFactoryType, String dataSourceName) {
            this.beanFactoryType = beanFactoryType;
            this.dataSourceName = dataSourceName;
        }

        public Class getBeanFactoryType() {
            return beanFactoryType;
        }

        public String getDataSourceName() {
            return dataSourceName;
        }

        public boolean equals(Object o) {
            final BeanFactoryTypeKey beanFactoryTypeKey = (BeanFactoryTypeKey) o;
            if (beanFactoryType != null ? !beanFactoryType.equals(beanFactoryTypeKey.beanFactoryType) : beanFactoryTypeKey.beanFactoryType != null) return false;
            if (dataSourceName != null ? !dataSourceName.equals(beanFactoryTypeKey.dataSourceName) : beanFactoryTypeKey.dataSourceName != null) return false;
            return true;
        }

        public int hashCode() {
            int result;
            result = (beanFactoryType != null ? beanFactoryType.hashCode() : 0);
            result = 29 * result + (dataSourceName != null ? dataSourceName.hashCode() : 0);
            return result;
        }
    }
}