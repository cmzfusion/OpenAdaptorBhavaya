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

import org.bhavaya.beans.*;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.beans.criterion.SqlCriterion;
import org.bhavaya.beans.criterion.CriterionGroupSqlOptimiser;
import org.bhavaya.collection.Association;
import org.bhavaya.collection.IndexedSet;
import org.bhavaya.collection.MapEvent;
import org.bhavaya.collection.WeakHashSet;
import org.bhavaya.coms.NotificationException;
import org.bhavaya.coms.NotificationSubscriber;
import org.bhavaya.ui.diagnostics.Profiler;
import org.bhavaya.util.*;
import org.bhavaya.util.TaskQueue;

import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Loads beans from the database.
 * Joins sql to optimise the loading of beans.
 * Listends for sql notifications (e.g. insert, update or delete statements) broadcast to communicate changes in the state of the database.
 * BeanFactories keep beans and the set of beans in each CriteriaBeanCollection up-to-date by processing the sql notifications,
 * and modifying the state of beans that have already been loaded and sending map events to BeanCollections indicating changes
 * in the state of a bean or set of beans.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.51.4.2 $
 */
public class SqlBeanFactory extends TabularDataBeanFactory {
    private static final Log log = Log.getCategory(SqlBeanFactory.class);

    static {
        BeanUtilities.addPersistenceDelegate(SqlBeanFactory.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                BeanFactory oldBeanFactory = (BeanFactory) oldInstance;
                return new Expression(oldInstance, BeanFactory.class, "getInstance", new Object[]{oldBeanFactory.getType(), oldBeanFactory.getDataSourceName()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    private static NotificationSubjectGroup[] notificationSubjectGroups;

    private SQL beanSelectStatement;
    private Set sqlExecuted = new HashSet();
    private Set loadedCriterionGroups = new WeakHashSet();
    private long lastNotificationsCount;

    private CriterionGroupSqlOptimiser criterionGroupSqlOptimiser;

    private static class NotificationSubjectGroup {
        private NotificationSubscriber notificationSubscriber;
        private String dataSourceName;
        private String notificationSubject;
        private Exception initNotificationException;
        private NotificationExceptionListener exceptionListener;
        private TaskQueue notificationTaskQueue;
    }

    public static void initBeanFactoryType() {
        if (notificationSubjectGroups == null) {
            BhavayaDataSource[] dataSources = DataSourceFactory.getInstances();
            ArrayList notificationSubjectGroupList = new ArrayList();

            for (int i = 0; i < dataSources.length; i++) {
                BhavayaDataSource dataSource = dataSources[i];
                String sqlNotificationSubject = dataSource.getSqlNotificationSubject();
                String dataSourceName = dataSource.getDataSourceName();

                if (sqlNotificationSubject != null) {
                    NotificationSubscriber notificationSubscriber = NotificationSubscriber.getInstance(sqlNotificationSubject);

                    if (notificationSubscriber != null) {
                        NotificationSubjectGroup notificationSubjectGroup = new NotificationSubjectGroup();
                        notificationSubjectGroup.notificationSubscriber = notificationSubscriber;
                        notificationSubjectGroup.dataSourceName = dataSourceName;
                        notificationSubjectGroup.notificationSubject = dataSource.getSqlNotificationSubject();
                        notificationSubjectGroup.notificationTaskQueue = new TaskQueue("Notifications(" + sqlNotificationSubject + ")");
                        notificationSubjectGroup.exceptionListener = new NotificationExceptionListener(notificationSubjectGroup);

                        notificationSubjectGroup.notificationTaskQueue.start();

                        if (log.isDebug()) log.debug("Adding exception listener to the NotificationSubscriber(" + sqlNotificationSubject + ")");
                        notificationSubscriber.addExceptionListener(notificationSubjectGroup.exceptionListener);
                        notificationSubscriber.addSlowConsumerListener(notificationSubjectGroup.exceptionListener);

                        log.info("Adding listener to the NotificationSubscriber, will not process notifications until NotificationSubscriber started");
                        notificationSubscriber.addNotificationListener(new NotificationListener(notificationSubjectGroup));

                        try {
                            notificationSubscriber.connect();
                        } catch (NotificationException e) {
                            log.error(e);
                            notificationSubjectGroup.initNotificationException = e;
                        }
                        notificationSubjectGroupList.add(notificationSubjectGroup);
                    }
                }
            }
            notificationSubjectGroups = (NotificationSubjectGroup[]) notificationSubjectGroupList.toArray(
                    new NotificationSubjectGroup[notificationSubjectGroupList.size()]);
        }
    }

    public static void postInitBeanFactoryType() {
        if (notificationSubjectGroups != null) {
            for (int i = 0; i < notificationSubjectGroups.length; i++) {
                NotificationSubjectGroup notificationSubjectGroup = notificationSubjectGroups[i];

                // only start processing notification if we have had no notification exception OR slow consumer events since startup
                // if these have occured then the error handlers will call startProcessing the notification subscriber.
                notificationSubjectGroup.notificationSubscriber.startProcessingIfNoErrors();

                if (notificationSubjectGroup.initNotificationException != null) {
                    notificationSubjectGroup.exceptionListener.exceptionThrown(notificationSubjectGroup.initNotificationException);
                }
            }
        }
    }

    public static void forceReconnectToNotificationServer() {
        if (notificationSubjectGroups != null) {
            for (NotificationSubjectGroup notificationSubjectGroup : notificationSubjectGroups) {
                notificationSubjectGroup.exceptionListener.forceReconnect();
            }
        }
    }

    public SqlBeanFactory(Class type, String dataSourceName) {
        super(type, dataSourceName);
        if (dataSourceName == null) {
            throw new RuntimeException("DataSource is null");
        }
        beanSelectStatement = getSchema().getSql();
    }

    protected void removeReference(Object key, String indexName) {
        super.removeReference(key, indexName);
        if (indexName == null) clearSqlExecuted();
    }

    public int getLoadRowCount(CriterionGroup criterionGroup) {
        SQL countSql = criterionGroup.getSQL(getType()).getSelectCountStatement(primaryKeyColumns[0]);
        Object record = DBUtilities.execute(getDataSourceName(), countSql.getStatementString()).iterator().next();
        int rowCount = ((Number) Generic.get(record, "NUMBEROFROWS")).intValue();
        return rowCount;
    }

    protected void clearImpl() {
        clearSqlExecuted();
    }

    protected Object remove(Object key, boolean fireCommit, boolean fireMapChanged) {
        clearSqlExecuted();
        return super.remove(key, fireCommit, fireMapChanged);
    }

    protected void touch() {
        super.touch();
        synchronized (loadedCriterionGroups) {
            loadedCriterionGroups.size();// loadedCriterionGroups is a WeakHashSet, ensure its reference queue is polled
        }
    }

    public CriterionGroupSqlOptimiser getCriterionGroupSqlOptimiser() {
        return criterionGroupSqlOptimiser;
    }

    public void setCriterionGroupSqlOptimiser(CriterionGroupSqlOptimiser criterionGroupSqlOptimiser) {
        this.criterionGroupSqlOptimiser = criterionGroupSqlOptimiser;
    }

    protected void resetLoadStack() {
        super.resetLoadStack();
        // on clear sqlExecuted, if we dont get realtime notifications, or if we only get realtime notifications for some (not all) tables
        // for now , assume we dont get notifications for all tables (just to be safe), therefore if we get a notification for one table
        // assume that the other tables may have changed to.
        long tempNotificationCount = 0;
        boolean connected = true;
        boolean hasSubscribers = notificationSubjectGroups.length > 0;

        for (int i = 0; i < notificationSubjectGroups.length; i++) {
            NotificationSubjectGroup notificationSubjectGroup = notificationSubjectGroups[i];
            connected = connected & notificationSubjectGroup.notificationSubscriber.isConnected();
            tempNotificationCount += notificationSubjectGroup.notificationSubscriber.getReceivedNotificationsCount();
        }
        if (!hasSubscribers || !connected || tempNotificationCount != lastNotificationsCount) {
            if (hasSubscribers) lastNotificationsCount = tempNotificationCount;
            clearSqlExecuted();
        }
    }

    public Object[] getObjects(CriterionGroup criteria) {
        synchronized (loadedCriterionGroups) {
            loadedCriterionGroups.add(criteria);
        }

        if (criterionGroupSqlOptimiser != null) {
            criteria.setCriterionGroupSQLOptimiser(criterionGroupSqlOptimiser);
        }

        return getObjects(criteria.getSQL(getType()));
    }

    public Object[] getObjects(SQL selectStatement) {
        TabularDataToBeanFactoryTransformer transformer = load(selectStatement, true, null, null);
        Collection beans = transformer.getBeans();
        Class type = Schema.getInstance(this.getType()).getType(); // can't get the generated type, because beans contains subclass instances which do not extends the generated type
        Object[] array = (Object[]) Array.newInstance(type, beans.size());
        return beans.toArray(array);
    }

    public Object[] getObjects(Object[] keys, String indexName) {
        if (keys == null) return null;
        Column[] keyColumns = getKeyColumns(indexName);
        if (keyColumns.length != 1) return super.getObjects(keys, indexName);

        List objects = new ArrayList(keys.length);
        SQL sql = getSchema().getSql();

        if (keyColumns.length == 1 && keys.length < DBUtilities.MAX_ELEMENTS_FOR_IN_STATEMENT) {
            sql = sql.getSelectStatementForKeys(keyColumns[0], keys);
        } else {
            String temporaryTable;
            try {
                temporaryTable = DBUtilities.createTemporaryTable(getDataSourceName(), getPrimaryKeyColumns(), keys, true);
            } catch (Exception e) {
                log.error(e);
                throw new RuntimeException(e);
            }
            StringBuffer whereClauseBuffer = new StringBuffer();
            for (int i = 0; i < keyColumns.length; i++) {
                if (i > 0) whereClauseBuffer.append(" AND ");
                whereClauseBuffer.append(keyColumns[i].getRepresentation()).append(" = ").append(temporaryTable).append(".").append(keyColumns[i].getName());
            }
            sql = sql.joinWhereClause(whereClauseBuffer.toString(), new CatalogSchemaTable[]{new CatalogSchemaTable(null, temporaryTable, temporaryTable)});
        }

        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            Object object = get(key, indexName, sql);
            if (object != null) {
                if (object instanceof Collection) {
                    objects.addAll((Collection) object);
                } else {
                    objects.add(object);
                }
            }
        }

        Object[] array = (Object[]) Array.newInstance(getSchema().getType(), // this returns the non-generated type
                objects.size());
        return objects.toArray(array);
    }

    /**
     * Returns the bean with the given key.  Care must be taken to ensure that the key is of an instance with
     * compatible equals and hashCode as the key that will be generated by BeanFactory.  For example, if a bean is
     * identified by the number "1233353", it is important whether this is a Integer, Double, BigDecimal, String etc.
     * BeanFactory decides the type of the key based on the type of the column in the database table.
     * <p/>
     * Where a bean is identified by a compound key, the key must be an ArrayList of key component values.  The values
     * must be ordered alphabetically by column name.
     * <p/>
     * sql identifies how to load the bean, or set of beans.  Where sql loads a set of beans, only the
     * bean identified by key will be returned.  If sql is null, and a bean identified by key has not already been
     * loaded then the sql is generated to only load the bean, unless the number of beans is considered to small, in that case
     * all beans are loaded.
     */
    protected Object get(Object key, SQL sql) {
        return get(key, null, sql, null, null);
    }

    protected Object get(Object key, String indexName, SQL sql) {
        return get(key, indexName, sql, null, null);
    }

    protected Object get(Object key, SQL sql, SQL parentSQL, Join[] joinsToParent) {
        return get(key, null, sql, parentSQL, joinsToParent);
    }

    protected Object get(Object key, String indexName, SQL sql, SQL parentSQL, Join[] joinsToParent) {
        if (key == null) return null;
        Association store = getStore(indexName);

        Object bean = store.get(key);
        if (bean == null && !store.containsKey(key)) {
            // can't find bean in cache, run latest sql to get further values in cache
            bean = load(key, indexName, sql, parentSQL, joinsToParent);
            if (bean == null) {
                bean = putNullKey(key, indexName, store);
            }
        }
        waitForBeanToInflate(bean);
        return bean;
    }

    protected Object load(Object key, String indexName) {
        return load(key, indexName, null, null, null);
    }

    private Object load(Object key, String indexName, SQL sql, SQL parentSQL, Join[] joinsToParent) {
        if (sql == null) sql = getDefaultSql(key, indexName);
        load(sql, false, parentSQL, joinsToParent);
        Association store = getStore(indexName);
        return store.get(key);
    }

    protected SqlTabularDataToBeanFactoryTransformer createTransformer(TabularData tabularData, Map cachedColumnValues, SQL selectStatement, SQL tabularDataSQL, SQL parentSQL, Join[] joinsToParent) {
        return new SqlTabularDataToBeanFactoryTransformer(this, tabularData, cachedColumnValues, selectStatement, tabularDataSQL, parentSQL, joinsToParent);
    }

    /**
     * Populates the BeanFactory with the set of beans identified by selectStatement.
     * If beanCollection is not null, it is also populated with the set of beans identified by selectStatement.
     */
    private TabularDataToBeanFactoryTransformer load(SQL selectStatement, boolean returnExactRows, SQL parentSQL, Join[] joinsToParent) {
//        ApplicationContext.productionAssert( ! SwingUtilities.isEventDispatchThread(), "Trying to track down blocking on AWT queue");
        TabularDataToBeanFactoryTransformer transformer = null;
        TabularData tabularData = null;

        pushBeanFactoryLoadStack(this);
        Profiler.Task sqlTask = null;
        try {
            SQL sqlToExecute = optimiseSql(selectStatement, returnExactRows);
            if (sqlToExecute == null) return null;
            String statementString = sqlToExecute.getStatementString();

            sqlTask = Profiler.taskStarted(Profiler.SQL, statementString);

            synchronized (getLock()) {
                Profiler.Task firstPassInflate = null;
                try {
                    log.info(logPrefix + "executing sql: " + statementString);
                    long startTime = System.currentTimeMillis();
                    Connection connection = getConnectionForSql();
                    tabularData = new ResultSetTabularData(getDataSourceName(), connection, sqlToExecute.getStatementString(), TabularData.ROW_TYPE_SELECT, null);
                    transformer = createTransformer(tabularData, null, selectStatement, sqlToExecute, parentSQL, joinsToParent);
                    firstPassInflate = Profiler.taskStarted(Profiler.SQL_INFLATE, "firstPassInflate");
                    transformer.firstPassInflate();
                    log.info(logPrefix + "finished load for sql: " + sqlToExecute.getStatementString() + " in " + (System.currentTimeMillis() - startTime) + " millis");
                    SqlMonitorFactory.getSqlMonitorInstance().sqlExecuted();
                } finally {
                    if (tabularData != null) tabularData.close(); // TabularData may already be closed, but repeat in finally block in case an exception was thrown and it was not closed.
                    if (firstPassInflate != null) Profiler.taskStopped(firstPassInflate);
                }
            }

            Profiler.Task inflateBeansToFindLater = Profiler.taskStarted(Profiler.MINOR_METHOD, "inflateBeansToFindLater");
            try {
                transformer.inflateBeansToFindLater();
            } finally {
                Profiler.taskStopped(inflateBeansToFindLater);
            }

            addSqlExecuted(sqlToExecute);

            Profiler.Task secondPassInflate = Profiler.taskStarted(Profiler.MINOR_METHOD, "secondPassInflate");
            try {
                transformer.secondPassInflate();
            } finally {
                Profiler.taskStopped(secondPassInflate);
            }

            transformer.fireLifeCycleCallbacks();
        } catch (Exception e) {
            log.error(logPrefix + "error during load for sql: " + selectStatement, e);
            throw new RuntimeException(e);
        } finally {
            if (transformer != null) transformer.removeFromPartiallyInflatedMap();
            popBeanFactoryLoadStack(this);
            if (sqlTask != null) Profiler.taskStopped(sqlTask);
        }

        return transformer;
    }

    private Connection getConnectionForSql() throws SQLException {
        return DataSourceFactory.getInstance(getDataSourceName()).getConnection();
    }

    private void addSqlExecuted(SQL sqlToExecute) {
        synchronized (sqlExecuted) {
            sqlExecuted.add(sqlToExecute.getStatementString());
        }
    }

    private SQL getDefaultSql(Object key, String indexName) {
        SQL sql;
        if (getSchema().getDataQuantity().equals(Schema.HIGH)) {
            // get sql to retrieve a single bean from database
            sql = getSchema().getSql().getSelectStatementForKey(getKeyColumns(indexName), key);
        } else {
            // get sql to retrieve all beans from database
            sql = getSchema().getSql();
        }
        return sql;
    }

    /**
     * Returns the sql that should be executed, or null if no sql should be executed.
     */
    private SQL optimiseSql(SQL sql, boolean returnExactRows) {
        if (!returnExactRows) {
            // if the data is non-volatile and is a small amount, then just get it all
            if (getSchema().getDataVolatility().equals(Schema.LOW) && getSchema().getDataQuantity().equals(Schema.LOW)) {
                sql = beanSelectStatement;
            }

            synchronized (sqlExecuted) {
                // we have already run this sql, so no point running it again
                if (sqlExecuted.contains(sql.getStatementString())) return null;
            }
        }

        return sql;
    }

    private void clearSqlExecuted() {
        synchronized (sqlExecuted) {
            sqlExecuted.clear();
        }
    }

    public static void clearNotificationBeanFactories() {
        if (notificationSubjectGroups != null) {
            for (int i = 0; i < notificationSubjectGroups.length; i++) {
                NotificationSubjectGroup notificationSubjectGroup = notificationSubjectGroups[i];
                clearNotificationBeanFactories(notificationSubjectGroup.dataSourceName);
            }
        }
    }

    public static void clearNotificationBeanFactories(String datasourceName) {
        clearAllBeanFactories(datasourceName, SqlBeanFactory.class, false, true);
    }

    protected static class SqlTabularDataToBeanFactoryTransformer extends TabularDataToBeanFactoryTransformer {
        private SqlBeanFactory beanFactory;
        private SQL selectStatement;
        private SQL tabularDataSQL;
        private SQL parentSQL;
        private Join[] joinsToParent;
        private Map selectStatementsByClazz;
        private Boolean includesAllColumns;
        private Set interestingLeftOperands;

        public SqlTabularDataToBeanFactoryTransformer(SqlBeanFactory beanFactory, TabularData tabularData, Map cachedColumnValues, SQL selectStatement, SQL tabularDataSQL, SQL parentSQL, Join[] joinsToParent) {
            super(beanFactory, tabularData, cachedColumnValues);
            this.beanFactory = beanFactory;
            this.selectStatement = selectStatement;
            this.tabularDataSQL = tabularDataSQL;
            this.parentSQL = parentSQL;
            this.joinsToParent = joinsToParent;
        }

        protected Object getBean(Object key, Class clazz) {
            BeanFactory factory = BeanFactory.getInstance(clazz, beanFactory.getDataSourceName());
            if (factory instanceof SqlBeanFactory) {
                SQL selectStatementForClazz = getSelectStatementForClazz(clazz);
                return ((SqlBeanFactory) factory).get(key, selectStatementForClazz, parentSQL, joinsToParent);
            } else {
                return factory.get(key);
            }
        }

        protected Class getSubClass(TabularData.Row tabularDataRow) {
            if (tabularDataSQL.getOperationType() == SQL.SELECT) {
                return schema.getSubClass(tabularDataRow);
            } else {
                return schema.getSubClass(tabularDataRow, tabularDataSQL.getTables()[0]);
            }
        }

        protected boolean includesAllColumns(TabularData.Row tabularDataRow) {
            if (!super.includesAllColumns(tabularDataRow)) return false;

            if (includesAllColumns == null) {
                // cache the value as this is called many times in a loop, and the value never changes
                if (tabularDataRow.getRowType() == TabularData.ROW_TYPE_UPDATE) {
                    includesAllColumns = new Boolean(schema.getSql().getTables().length == 1 && includesAllColumns(tabularDataRow.getTabularData()));
                } else {
                    includesAllColumns = Boolean.TRUE;
                }
            }
            return includesAllColumns.booleanValue();
        }

        private boolean includesAllColumns(TabularData tabularData) {
            Property[] properties = schema.getProperties();

            Set sqlColumns = new HashSet(Arrays.asList(tabularData.getColumns()));

            for (int i = 0; i < properties.length; i++) {
                Property property = properties[i];
                if (property.isValid()) {
                    Column[] columnsForProperty = property.getColumns();
                    for (int j = 0; j < columnsForProperty.length; j++) {
                        Column columnForProperty = columnsForProperty[j];
                        if (!sqlColumns.contains(columnForProperty)) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }

        protected boolean isIgnoreableUpdate(TabularData.Row tabularDataRow) {
            Set interestingLeftOperands = getInterestingLeftOperands();

            for (Iterator iterator = interestingLeftOperands.iterator(); iterator.hasNext();) {
                Column interestingColumn = (Column) iterator.next();
                if (tabularDataRow.isModified(interestingColumn)) {
                    return false;
                }
            }

            // no interesting left operands
            return true;
        }

        private Set getInterestingLeftOperands() {
            if (interestingLeftOperands == null) {
                interestingLeftOperands = new HashSet();

                for (int i = 0; i < beanFactory.unionOfKeyColumns.length; i++) {
                    Column column = beanFactory.unionOfKeyColumns[i];
                    interestingLeftOperands.add(column);
                }

                BeanFactory beanFactory = this.beanFactory;
                while (beanFactory != null) {
                    addInterestingLeftOperands(interestingLeftOperands, beanFactory.getType());
                    beanFactory = (BeanFactory) beanFactory.getSuperBeanFactory();
                }
            }
            return interestingLeftOperands;
        }

        private void addInterestingLeftOperands(Set interestingLeftOperands, Class beanType) {
            synchronized (beanFactory.loadedCriterionGroups) {
                for (Iterator iterator = beanFactory.loadedCriterionGroups.iterator(); iterator.hasNext();) {
                    CriterionGroup criterionGroup = (CriterionGroup) iterator.next();
                    Criterion[] criterions = criterionGroup.getCriteria();

                    if (criterions != null) {
                        for (int i = 0; i < criterions.length; i++) {
                            SqlCriterion criterion = (SqlCriterion) criterions[i];
                            Column[] directLeftOperandColumns = criterion.getDirectLeftOperandColumns(beanType);

                            if (directLeftOperandColumns != null) {
                                for (int j = 0; j < directLeftOperandColumns.length; j++) {
                                    Column column = directLeftOperandColumns[j];
                                    interestingLeftOperands.add(column);
                                }
                            }
                        }
                    }
                }
            }
        }

        protected Map getPropertyInitialisationValues() {
            Map map = super.getPropertyInitialisationValues();
            if (tabularDataSQL.getOperationType() == SQL.SELECT) {
                map.put(SqlForeignKeyProperty.SELECT_STATEMENT_PROPERTY, selectStatement);
            }
            return map;
        }

        private SQL getSelectStatementForClazz(Class clazz) {
            if (selectStatement == null) return null;
            if (clazz == beanFactory.getType()) return selectStatement;
            if (clazz == beanFactory.getAncestorType()) return selectStatement;

            // get sql to load data for a subclass, need to join in where clause of select statement
            // joining sql can be expensive, especially in a loop creating many beans therefore cache the joined sql for a tabularData
            if (selectStatementsByClazz == null) selectStatementsByClazz = new HashMap();
            SQL selectStatementForClazz = (SQL) selectStatementsByClazz.get(clazz);
            if (selectStatementForClazz == null) {
                SQL selectStatementForAllRows = Schema.getInstance(clazz).getSql();
                if (parentSQL != null && joinsToParent != null) {
                    selectStatementForClazz = selectStatementForAllRows.joinStatement(parentSQL, joinsToParent, null, null, true); // join the subclass sql
                } else {
                    selectStatementForClazz = selectStatementForAllRows.joinStatement(selectStatement, null, null, null, false); // join the subclass sql
                }
                selectStatementsByClazz.put(clazz, selectStatementForClazz);
            }
            return selectStatementForClazz;
        }
    }

    private static class NotificationListener implements org.bhavaya.coms.NotificationListener {
        private Map beanFactoriesByTable;
        private SqlBeanFactory[] beanFactories;
        private Task refreshDataTask;
        private NotificationSubjectGroup notificationSubjectGroup;

        public NotificationListener(NotificationSubjectGroup notificationSubjectGroup) {
            this.notificationSubjectGroup = notificationSubjectGroup;
            beanFactoriesByTable = new HashMap();
            refreshDataTask = new RefreshDataTask(notificationSubjectGroup.dataSourceName);
            Collection instancesForNonTxConnection = getInstances(notificationSubjectGroup.dataSourceName, SqlBeanFactory.class).values();
            beanFactories = (SqlBeanFactory[]) instancesForNonTxConnection.toArray(new SqlBeanFactory[instancesForNonTxConnection.size()]);
        }

        public final void receive(String incomingSqlStatements) {
            if (incomingSqlStatements == null) {
                log.error("Failed to parse null string");
                return;
            }

            if (incomingSqlStatements.equals("REFRESH\n")) {
                log.info("Received REFRESH event");
                notificationSubjectGroup.notificationTaskQueue.addTask(refreshDataTask); // this is going to run on the default queue
                return;
            } else if (incomingSqlStatements.equals("STOP\n")) {
                log.info("Received STOP event");
                System.exit(0);
                return;
            }

            SQL[] sqlStatements;
            try {
                sqlStatements = SQL.parseTransaction(incomingSqlStatements, notificationSubjectGroup.dataSourceName);
            } catch (Throwable e) {
                log.error("Failed to parse: " + incomingSqlStatements, e);
                return;
            }

            try {
                processNotification(sqlStatements);
            } catch (Throwable e) {
                log.error("Failed to process: " + incomingSqlStatements, e);
            }
        }

        private static class RefreshDataTask extends Task {
            private String datasourceName;

            public RefreshDataTask(String datasourceName) {
                super("Refresh data");
                this.datasourceName = datasourceName;
            }

            public void run() {
                try {
                    clearNotificationBeanFactories(datasourceName);
                } catch (Exception e) {
                    log.error("Could not refresh data", e);
                }
            }
        }

        /**
         * Pass on sqlNotification to all BeanFactories.
         * <p/>
         * First clear the sqlExecuted by each BeanFactory (cache), this is a sqlNotification indicated the state of the
         * database has changed.  Therefore we cannot rely on sqlExecuted to prove that we have all data in the BeanFactory.
         * If we can't find a bean in the cache, we must go to the database again, to see if it exists, even if we have
         * already checked before the sqlNotification.  A complex example would be
         * "select BOND.* from BOND, POSITION where BOND.instrumentId = POSITION.instrumentId and POSITION.amount > 1000000",
         * after an insert or update the Bond BeanFactory could not say whether it has all Bonds for the above sql select statement.
         * Therefore it must clear its sqlExecuted.
         * <p/>
         * Only BeanFactories, with autoCommit == true, process notifications, other
         * BeanFactories that use transaction database connections (with autoCommit == false), do not process sql
         * notifications, but rather clear the BeanFactories at the end of each transaction.
         */
        private final void processNotification(SQL[] sqlNotifications) throws Exception {
            List processableBeanFactorySqlPairs = new ArrayList();

            for (int n = 0; n < sqlNotifications.length; n++) {
                SQL sqlNotification = sqlNotifications[n];
                Set beanFactoriesForTable = getBeanFactoriesForTable(sqlNotification.getTables()[0]);
                for (Iterator iterator = beanFactoriesForTable.iterator(); iterator.hasNext();) {
                    SqlBeanFactory beanFactory = (SqlBeanFactory) iterator.next();
                    processableBeanFactorySqlPairs.add(new BeanFactorySQLPair(beanFactory, sqlNotification));
                }
            }
            if (processableBeanFactorySqlPairs.size() == 0) return; // SQL is not relevant

            ArrayList pushedBeanFactories = new ArrayList();
            IndexedSet events = new IndexedSet(processableBeanFactorySqlPairs.size());

            try {
                Map cachedColumnValues = new HashMap();
                TransformersByBean transformersByBean = new TransformersByBean();
                Set beanFactoriesAffected = new LinkedHashSet();

                for (int i = 0; i < processableBeanFactorySqlPairs.size(); i++) {
                    BeanFactorySQLPair beanFactorySQLPair = (BeanFactorySQLPair) processableBeanFactorySqlPairs.get(i);
                    pushedBeanFactories.add(beanFactorySQLPair.beanFactory);
                    pushBeanFactoryLoadStack(beanFactorySQLPair.beanFactory);
                    boolean beansAffected = processNotification(beanFactorySQLPair.beanFactory, cachedColumnValues, beanFactorySQLPair.sql, events, transformersByBean);
                    if (beansAffected) beanFactoriesAffected.add(beanFactorySQLPair.beanFactory);
                }

                List transformers = transformersByBean.getTransformers();
                for (int i = transformers.size() - 1; i >= 0; i--) { // reverse iterate for second pass
                    List transformersForBean = (List) transformers.get(i);
                    for (int j = 0; j < transformersForBean.size(); j++) { // forward iterate if pass for same bean
                        TabularDataToBeanFactoryTransformer transformer = (TabularDataToBeanFactoryTransformer) transformersForBean.get(j);
                        transformer.secondPassInflate();
                    }
                }

                // accumulate commit events
                if (beanFactoriesAffected.size() > 0) {
                    for (Iterator iterator = beanFactoriesAffected.iterator(); iterator.hasNext();) {
                        SqlBeanFactory beanFactory = (SqlBeanFactory) iterator.next();
                        beanFactory.fireCommit(events);
                    }
                }

                for (int i = transformers.size() - 1; i >= 0; i--) { // reverse iterate for second pass
                    List transformersForBean = (List) transformers.get(i);
                    for (int j = 0; j < transformersForBean.size(); j++) { // forward iterate if pass for same bean
                        TabularDataToBeanFactoryTransformer transformer = (TabularDataToBeanFactoryTransformer) transformersForBean.get(j);
                        transformer.fireLifeCycleCallbacks();
                        transformer.removeFromPartiallyInflatedMap();
                    }
                }
            } finally {
                for (ListIterator iterator = pushedBeanFactories.listIterator(pushedBeanFactories.size()); iterator.hasPrevious();) {
                    BeanFactory beanFactory = (BeanFactory) iterator.previous();
                    popBeanFactoryLoadStack(beanFactory);
                }
            }

            // now fire all accumulated events
            for (int i = 0; i < events.size(); i++) {
                MapEvent event = (MapEvent) events.get(i);
                BeanFactory beanFactory = (BeanFactory) event.getSource();
                beanFactory.fireMapChanged(event);
            }
        }

        private final Set getBeanFactoriesForTable(CatalogSchemaTable table) {
            Set beanFactoriesForTable = (Set) beanFactoriesByTable.get(table);

            if (beanFactoriesForTable == null) {
                beanFactoriesForTable = new LinkedHashSet();

                for (int i = 0; i < beanFactories.length; i++) {
                    SqlBeanFactory beanFactory = beanFactories[i];
                    if (Utilities.contains(beanFactory.getSchema().getSql().getTables(), table)) {
                        beanFactoriesForTable.add(beanFactory);
                    }
                }

                beanFactoriesByTable.put(table, beanFactoriesForTable);
            }

            return beanFactoriesForTable;
        }

        /**
         * There are five types of sql update notification:
         * (1) update tableX set randomColumn1 = xxxx, randomColumn2 = xxxx where keyColumn1 = xxxx and keyColumn2 = xxxx
         * (2) update tableX set randomColumn1 = randomColumn1 + xxxx where keyColumn1 = xxxx and keyColumn2 = xxxx*
         * (3) update tableX set randomColumn1 = xxxx, randomColumn2 = xxxx
         * (4) update tableX set randomColumn1 = xxxx, randomColumn2 = xxxx where randomColumn3 = xxxx
         * (5) update tableX set randomColumn1 = xxxx, randomColumn2 = xxxx where randomColumn3 = xxxx and keyColumn1 = xxxx
         * (6) update tableX set randomColumn1 = xxxx, randomColumn2 = xxxx where randomColumn3 = xxxx and keyColumn1 = xxxx and keyColumn2 = xxxx
         * <p/>
         * In case 1, you can determine the key for the bean to be inserted or updated.
         * In case 2, although the key can be determined, the value for randomColumn1 cannot.
         * <p/>
         * There are three types of sql insert notification:
         * (1) insert into tableX (randomColumn1, randomColumn2, keyColumn1, keyColumn2) values (xxxx, xxxx, xxxx, xxxx)
         * (2) insert into tableX values (xxxx, xxxx, xxxx, xxxx)
         * (3) insert into tableX (randomColumn1, randomColumn2, keyColumn1, keyColumn2) select ... from tableY where...
         * <p/>
         * In case 1, you can determine the key for the bean to be inserted or updated.
         * An insert may lead to an update, e.g. in the case of a BondTrade, the "INSERT INTO TRADE" inserts the BondTrade
         * into the cache, while the "INSERT INTO BOND_TRADE", updates the value inserted previously.
         * <p/>
         * Note an insert can cause beans to be updated.  E.g. where a BondTrade bean is made up of data from the TRADE table
         * and the BOND_TRADE table.  An 'insert into TRADE' causes a new bean to be inserted into
         * the cache.  The following 'insert into BOND_TRADE' causes that bean to be updated with the new data.
         * <p/>
         * There are four types of sql delete notification:
         * (1) delete tableX where keyColumn1 = xxxx and keyColumn2 = xxxx
         * (2) delete tableX
         * (3) delete tableX where randomColumn1 = xxxx
         * (4) delete tableX where randomColumn1 = xxxx and keyColumn1 = xxxx
         * <p/>
         * In case 1, you can determine the key for the bean to be deleted.
         */
        private final boolean processNotification(SqlBeanFactory beanFactory, Map cachedColumnValues, SQL sqlNotification, IndexedSet events, TransformersByBean transformersByBean) throws Exception {
            if (log.isDebug()) log.debug(getLogPrefix(beanFactory) + "processing: " + sqlNotification.getStatementString());
            int incomingSqlOperationType = sqlNotification.getOperationType();
            boolean sqlNotificationContainsPrimaryKey = containsPrimaryKeyValues(sqlNotification, beanFactory.primaryKeyColumns);

            if ((incomingSqlOperationType == SQL.INSERT) || (incomingSqlOperationType == SQL.UPDATE) || (incomingSqlOperationType == SQL.DELETE && sqlNotificationContainsPrimaryKey)) {
                TabularData tabularData = null;
                TabularDataToBeanFactoryTransformer transformer;

                SQL selectStatement = beanFactory.beanSelectStatement.joinStatement(sqlNotification);
                synchronized (beanFactory.getLock()) {
                    try {
                        tabularData = getTabularData(beanFactory, sqlNotification, selectStatement, sqlNotificationContainsPrimaryKey);
                        transformer = beanFactory.createTransformer(tabularData, cachedColumnValues, selectStatement, sqlNotification, null, null);
                        transformer.firstPassInflate();
                    } finally {
                        // TabularData may already be closed, but repeat in finally block in case an exception was thrown and it was not closed.
                        if (tabularData != null) tabularData.close();
                    }
                }

                boolean containsBeans = transformer.containsBeans();
                if (containsBeans) {
                    transformersByBean.addTransformer(transformer.getBeans().iterator().next(), transformer);
                }

                transformer.inflateBeansToFindLater();
                transformer.fireMapChangedEvents(events);

                return containsBeans;

            } else if ((incomingSqlOperationType == SQL.DELETE && !sqlNotificationContainsPrimaryKey) || incomingSqlOperationType == SQL.TRUNCATE || incomingSqlOperationType == SQL.DROP_TABLE) {
                // refresh all data, listeners will ask BeanFactory to reload data, when they receive an ALL_ROWS event
                beanFactory.clear(events, false, true);
                return true;
            } else {
                log.error(getLogPrefix(beanFactory) + "unknown sql operation: " + incomingSqlOperationType);
                return false;
            }
        }

        public static boolean containsPrimaryKeyValues(SQL sql, Column[] keyColumns) {
            for (int i = 0; i < keyColumns.length; i++) {
                if (!sql.hasColumnValue(keyColumns[i])) return false;
            }
            return true;
        }


        private final TabularData getTabularData(SqlBeanFactory beanFactory, SQL sqlNotification, SQL selectStatement, boolean sqlNotificationContainsPrimaryKey) throws SQLException {
            TabularData tabularData;

            if (sqlNotificationContainsPrimaryKey) {
                // avoid hitting the database
                tabularData = new SQLTabularData(sqlNotification);
            } else {
                log.info(getLogPrefix(beanFactory) + "executing sql: " + selectStatement.getStatementString() + "\nFor: " + sqlNotification.getStatementString());
                tabularData = new ResultSetTabularData(beanFactory.getDataSourceName(), selectStatement, SQLTabularData.getRowType(sqlNotification));
                SqlMonitorFactory.getSqlMonitorInstance().sqlExecuted();
            }
            return tabularData;
        }

        private static final class BeanFactorySQLPair {
            private final SqlBeanFactory beanFactory;
            private final SQL sql;

            public BeanFactorySQLPair(SqlBeanFactory beanFactory, SQL sql) {
                this.beanFactory = beanFactory;
                this.sql = sql;
            }
        }

    }

    /**
     * The reason transformers are grouped by Ancestor is that in a transaction we want to second pass in reverse order
     * except where we are refering to the same bean, then we need to second pass in the originial order
     */
    private static class TransformersByBean {
        private List transformers = new ArrayList();
        private List beanIndicies = new ArrayList();

        public void addTransformer(Object bean, TabularDataToBeanFactoryTransformer transformer) {
            int ancestorIndex = beanIndicies.lastIndexOf(bean);
            List transformersForBean;
            if (ancestorIndex == -1) {
                beanIndicies.add(bean);
                transformersForBean = new ArrayList();
                transformers.add(transformersForBean);
            } else {
                transformersForBean = (List) transformers.get(ancestorIndex);
            }
            transformersForBean.add(transformer);
        }

        public List getTransformers() {
            return transformers;
        }
    }


    private static class NotificationExceptionListener implements ExceptionListener, NotificationSubscriber.SlowConsumerListener {
        private Task refreshDataAndProcessNotificationsTask;
        private Task refreshDataAndReconnectTask;
        private Task reconnectTask;
        private Task closeConnectionAndReconnect;
        private NotificationSubjectGroup notificationSubjectGroup;

        public NotificationExceptionListener(NotificationSubjectGroup notificationSubjectGroup) {
            this.notificationSubjectGroup = notificationSubjectGroup;
            refreshDataAndProcessNotificationsTask = new RefreshDataAndProcessNotificationsTask();
            refreshDataAndReconnectTask = new RefreshDataAndReconnectTask();
            reconnectTask = new ReconnectTask();
            closeConnectionAndReconnect = new CloseAndReconnectTask();
        }

        public String toString() {
            return "NotificationReconnectQueue";
        }

        public void exceptionThrown(Exception e) {
            Log.getUserCategory().error("The connection to the notification server has been lost.  " +
                    "The data is no longer realtime.  Data will refresh every " +
                    (notificationSubjectGroup.notificationSubscriber.getReconnectionPeriod() / 1000) +
                    " seconds.  The application will reconnect to the notification server as soon as possible.");
            notificationSubjectGroup.notificationTaskQueue.addTask(reconnectTask);
        }

        public void slowConsumer() {
            notificationSubjectGroup.notificationTaskQueue.addTask(refreshDataAndProcessNotificationsTask);
        }

        public void forceReconnect() {
            Log.getUserCategory().warn("Received a request to reconnect to the notification server");
            notificationSubjectGroup.notificationTaskQueue.addTask(closeConnectionAndReconnect);
        }

        private class RefreshDataAndProcessNotificationsTask extends Task {
            public RefreshDataAndProcessNotificationsTask() {
                super("Refresh data and process notifications");
            }

            public void run() {
                try {
                    clearNotificationBeanFactories(notificationSubjectGroup.dataSourceName);
                } finally {
                    notificationSubjectGroup.notificationSubscriber.startProcessing();

                    // Protect clients hammering the database, by sleeping in the TaskQueue,
                    // in case repeated slow consumer while responding to previous slow consumer.
                    try {
                        Thread.sleep(notificationSubjectGroup.notificationSubscriber.getReconnectionPeriod());
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
            }
        }

        private class RefreshDataAndReconnectTask extends Task {
            public RefreshDataAndReconnectTask() {
                super("Refresh data and reconnect");
            }

            public void run() {
                try {
                    clearNotificationBeanFactories(notificationSubjectGroup.dataSourceName);
                } catch (Exception e) {
                    log.error("Could not refresh data", e);
                }

                try {
                    Thread.sleep(notificationSubjectGroup.notificationSubscriber.getReconnectionPeriod());
                } catch (InterruptedException e) {
                    log.error(e);
                }

                notificationSubjectGroup.notificationTaskQueue.addTask(reconnectTask);
            }
        }

        private class ReconnectTask extends Task {
            public ReconnectTask() {
                super("Reconnect");
            }

            public void run() {
                try {
                    log.warn("Attempting reconnection to NotificationServer");
                    notificationSubjectGroup.notificationSubscriber.connect();
                    log.warn("Reconnection successful");
                    Log.getUserCategory().error("The connection to the notification server has been re-established.  The data is now realtime.");
                    notificationSubjectGroup.notificationTaskQueue.addTask(refreshDataAndProcessNotificationsTask);
                } catch (NotificationException ex) {
                    log.warn("Reconnection was not successful");
                    notificationSubjectGroup.notificationTaskQueue.addTask(refreshDataAndReconnectTask);
                }
            }
        }

        private class CloseAndReconnectTask extends Task {
            public CloseAndReconnectTask() {
                super("Close And Reconnect");
            }

            public void run() {
                log.warn("Attempting to close the connection to the notification server");
                notificationSubjectGroup.notificationSubscriber.closeAndStopNotificationProcessor();
                log.warn("Connection to notification server is closed");
                notificationSubjectGroup.notificationTaskQueue.addTask(reconnectTask);
            }
        }
    }
}
