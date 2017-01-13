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

package org.bhavaya.ui.table;

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.CollectionListener;
import org.bhavaya.collection.IndexedSet;
import org.bhavaya.collection.ListEvent;
import org.bhavaya.ui.SwingTask;
import org.bhavaya.ui.table.diagnostics.TableUpdateDiagnostics;
import org.bhavaya.ui.table.formula.FormulaEnabledObjectGraph;
import org.bhavaya.ui.table.formula.FormulaManager;
import org.bhavaya.ui.table.formula.FormulaResult;
import org.bhavaya.ui.table.formula.FormulaUtils;
import org.bhavaya.util.*;
import org.bhavaya.ui.UIUtilities;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Uses cached object graph to present a thread safe table model adaption of a bean collection.
 * Columns show the value at a given property path, and can be added or removed dynamically
 *
 * @author Philip Milne
 * @author Daniel van Enckevort
 * @author Parwy Sekhon
 * @author Brendon McLean
 * @author John Smith
 * @version $Revision: 1.39.4.1 $
 */
public class BeanCollectionTableModel implements KeyedColumnTableModel, TabularBeanAssociation, CustomColumnNameModel {
    private static final Log log = Log.getCategory(BeanCollectionTableModel.class);
    private static final String NEW_LINE = "\n";

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};

    private IndexedSet rows = new IndexedSet();
    private FormulaEnabledObjectGraph objectCache;
    private ReschedulingChangeQueue reschedulingChangeQueue = new ReschedulingChangeQueue();
    private static ScheduledExecutorService changeExecutor = NamedExecutors.newSingleThreadScheduledExecutor("BeanCollectionTableModel_ChangeExecutor");

    private BeanCollection beanCollecton = null;    //this is the bean collection that we are displaying data from.

    /**
     * An indexed set of beanpaths of the form "trade.instrument.spRating"
     */
    private IndexedSet<String> columnLocators = new IndexedSet<String>();
    private Map locatorToListener = new HashMap();
    private Map locatorToClass = new HashMap();
    private Map locatorToColumnName = new HashMap();
    private Map locatorToColumnNameDepth = new HashMap();

    private List collectionEventsToFire = new ArrayList();
    private ArrayList tableModelListeners = new ArrayList();
    private CollectionListener beanCollectionSynchroniser;

    private Class beanClass;

    private Map<String,SetStatement> locatorToSetStatement = new HashMap<String,SetStatement>();
    private List<PatternMatchingSetStatement> patternMatchingSetStatements = new LinkedList<PatternMatchingSetStatement>();

    private PropertyModel.PropertyChangeListener propertyDisplayNameChangeListener;
    private ObjectGraphMultipleChangeListener multipleChangeListener;
    private boolean deferCollectionChangesUntilCellsReady;  //indicates whether a collection "insert" should appear immediately, or wait for values to be loaded

    private TransactionQueue transactionQueue = new TransactionQueue();
    private Object changeLock = new ChangeLock();

    private Map customColumnNames = new HashMap();

    private String name = null;
    private ScheduledFuture taskFuture = null;

    public BeanCollectionTableModel(Class beanClass, boolean asynchronous) {
        this(beanClass, asynchronous, null);
    }

    public BeanCollectionTableModel(Class beanClass, boolean asynchronous, String name) {
        super();
        this.name = name;
        this.beanClass = beanClass;
        this.deferCollectionChangesUntilCellsReady = asynchronous;

        beanCollectionSynchroniser = new CollectionListener() {
            public void collectionChanged(final ListEvent collectionEvent) {
                synchronized (beanCollectionSynchroniser) {
                    handleListEvent(collectionEvent);
                }
            }
        };
        objectCache = new FormulaEnabledObjectGraph(beanClass, asynchronous, name, this);
        multipleChangeListener = new ObjectGraphMultipleChangeListener();

        objectCache.addPathListener("", multipleChangeListener);
        objectCache.addRowReadyListener(transactionQueue);

        scheduleTask(50);
    }

    /**
     * This constructor should only be called on the EventThread. Do not call
     * it from any other thread unless you really know what you are doing!
     *
     * This constructor ends up calling allRowsChanged. The only other place allRowsChanged is called is in response
     * to a list event, and the resulting update is processed on the EventThread.
     *
     */
    public BeanCollectionTableModel(BeanCollection beanCollection, boolean asynchronous) {
        this(beanCollection.getType(), asynchronous);
        setBeanCollection(beanCollection);
    }

    public FormulaManager getFormulaManager() {
        return objectCache.getFormulaManager();
    }

    public void setFormulaManager(FormulaManager formulaManager ) {
        objectCache.setFormulaManager(formulaManager);
    }

    public BeanCollection getBeanCollecton() {
        return beanCollecton;
    }

    public CachedObjectGraph getCachedObjectGraph() {
        return objectCache;
    }

    public void setBeanCollection(BeanCollection beanCollection) {
        if (log.isDebug()) log.debug("Setting bean collection");
        if (this.beanCollecton != null) {
            this.beanCollecton.removeCollectionListener(beanCollectionSynchroniser);
        }
        this.beanCollecton = beanCollection;

        Object[] collectionSnapshot = EMPTY_OBJECT_ARRAY;
        if (beanCollection != null) {
            synchronized (beanCollection.getTransactionLock()) {
                beanCollection.addCollectionListener(beanCollectionSynchroniser);
                collectionSnapshot = beanCollection.toArray();
            }
        }
        allRowsChanged(collectionSnapshot);
    }

    public void setDeferCollectionChangesUntilCellsReady(boolean defer) {
        deferCollectionChangesUntilCellsReady = defer;
    }

    public boolean isDeferCollectionChangesUntilCellsReady() {
        return deferCollectionChangesUntilCellsReady;
    }

    private void stopTask() {
        if(taskFuture != null) {
            taskFuture.cancel(false);
        }
    }

    private void scheduleTask(int delayMs) {
        // Remeber folks, the actionPerformed of timer is on the Swing event thread
        taskFuture = changeExecutor.schedule(getChangeQueue(), delayMs, TimeUnit.MILLISECONDS);
    }


    public void dispose() {
        stopTask();

        if (this.beanCollecton != null) {
            this.beanCollecton.removeCollectionListener(beanCollectionSynchroniser);
            this.beanCollecton = null;
        }
        objectCache.dispose();
        rows.clear();
        PropertyModel.removePropertyDisplayNameChangeListener(propertyDisplayNameChangeListener);
    }

//    protected List getList() {
//        return data;
//    }

    public Class getBeanType() {
        return beanClass;
    }

    private void beanAdded(Object bean, boolean fireEvent) {
        objectCache.addRootObject(bean);
        int row = rows.size();
        boolean newItem = rows.add(bean);
        assert (newItem) : "Oh dear, row was already in the bean table model, and we tried to add it again!";
        if (fireEvent) fireTableChanged(new TableModelEvent(BeanCollectionTableModel.this, row, row, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }


    /**
     * same as calling "setBeanCollection(null)"
     */
    public void clear() {
        assert EventQueue.isDispatchThread() : "modification to beanCollectionTableModel is not on EventQueue Thread";
        setBeanCollection(null);
    }

    private void beanRemoved(Object bean) {
//        assert EventQueue.isDispatchThread() : "modification to beanCollectionTableModel is not on EventQueue Thread";
//
        int rowIndex = rows.indexOf(bean);
        if (rowIndex < 0 && !objectCache.isWaitingForProperties(bean)) {
            log.warn("beanCollectionTableModel is out of sync with beanCollection, maybe we lost an event?, beanRemoved: " + bean, new RuntimeException());
        }


        rows.remove(bean);
        objectCache.removeRootObject(bean);

        if (rowIndex >= 0) fireTableChanged(new TableModelEvent(this, rowIndex, rowIndex, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
    }


    /**
     * This is a hack for the JTable which is confused by primitive types and interfaces.
     */
    private Class displayableType(Class c) {
        if (c == null || c.isInterface()) {
            return Object.class;
        }
        return ClassUtilities.typeToClass(c);
    }

    private void handleListEvent(final ListEvent listEvent) {

        if (listEvent.getType() == ListEvent.ALL_ROWS) {
            //if we get an all rows, then previous events are irrelevant as we are doing a full re-sync
            collectionEventsToFire.clear();

            //take a snapshot of the beanCollection
            Collection collection = (Collection) listEvent.getSource();
            Object[] snapshot = collection.toArray();
            collectionEventsToFire.add(new ListEvent((Collection) listEvent.getSource(), ListEvent.ALL_ROWS, snapshot));

        } else if (listEvent.getType() == ListEvent.COMMIT) {
            if (collectionEventsToFire.size() == 0) return;

            final ListEvent[] collectionEventsToFireSnapShot = (ListEvent[]) collectionEventsToFire.toArray(new ListEvent[collectionEventsToFire.size()]);
            collectionEventsToFire.clear();

            //if this map event came from an external notification, we may not be on the swing thread
            //check for this and handle
            UIUtilities.runInDispatchThread(new Runnable() {
                public void run() {
                    processEvents(collectionEventsToFireSnapShot);
                }
            });

        } else {
            collectionEventsToFire.add(listEvent);
        }
    }

    private void processEvents(final ListEvent[] listEventsToFireSnapShot) {

        synchronized (objectCache.getChangeLock()) {
            ArrayList transaction = new ArrayList(4);

            for (int i = 0; i < listEventsToFireSnapShot.length; i++) {
                ListEvent currentListEvent = listEventsToFireSnapShot[i];


                int listEventType = currentListEvent.getType();
                switch (listEventType) {
                    case ListEvent.ALL_ROWS: {
                        Object value = currentListEvent.getValue();
                        assert (value != null && value.getClass().isArray()) : "Value property of ALL_ROWS list change event should be an array holding a snapshot of the collection. Instead it is: " + value;
                        allRowsChanged((Object[]) value);
                    }
                    break;
                    case ListEvent.INSERT: {
                        Object value = currentListEvent.getValue();
                        if (value != null) {
                            if (isDeferCollectionChangesUntilCellsReady()) {
                                transaction.add(currentListEvent);
                            } else {
                                beanAdded(value, true);
                            }
                        } else {
                            log.warn("got currentListEvent INSERT but null value");
                        }
                    }
                    break;
                    case ListEvent.UPDATE:
                        // do not fire any events, this will be handled through propertyChangeEvents on the value
                        break;
                    case ListEvent.DELETE: {
                        Object value = currentListEvent.getValue();
                        if (value != null) {
                            if (isDeferCollectionChangesUntilCellsReady()) {
                                transaction.add(currentListEvent);
                            } else {
                                beanRemoved(value);
                            }
                        } else {
                            log.warn("got currentListEvent DELETE but null value");
                        }
                    }
                    break;
                }
            }
            if (transaction.size() > 0) transactionQueue.add(transaction);
        }
    }

    protected void allRowsChanged(Object[] newRowsSnapshot) {
        if (log.isDebug()) log.debug("all rows changed");
        rows.clear();
        objectCache.clear();
        transactionQueue.clear();

        for (int i = 0; i < newRowsSnapshot.length; i++) {
            Object bean = newRowsSnapshot[i];
            beanAdded(bean, false);
        }

        fireTableChanged(new TableModelEvent(this));
        if (log.isDebug()) log.debug("handled all rows event");
    }

    public int getColumnCount() {
        return columnLocators.size();
    }

    public Class getColumnClass(int column) {
        String columnLocatorString = columnLocators.get(column);
        if (columnLocatorString != null) {
            Class columnClass = (Class) locatorToClass.get(columnLocatorString);
            if (columnClass == null) {
                try {
                    if(isFormulaLocator(columnLocatorString)) {
                        columnClass = FormulaResult.class;
                    } else {
                        String[] locator = Generic.beanPathStringToArray(columnLocatorString);
                        columnClass = PropertyModel.getInstance(beanClass).getAttribute(locator).getType();
                        columnClass = displayableType(columnClass);
                    }
                    locatorToClass.put(columnLocatorString, columnClass);
                } catch (RuntimeException nsfe) {
                    log.error("could not find field for column specifer:" + columnLocatorString, nsfe);
                }
            }
            return columnClass;
        } else {
            log.error("beanCollectionTableModel asked for non-existant column " + column);
        }
        return Object.class;
    }

    public String getColumnName(int column) {
        String columnLocatorString = null;
        if (column < columnLocators.size()) {
            columnLocatorString = columnLocators.get(column);
        }
        if (columnLocatorString == null) {
            return null;
        }
        //names are added to this when columns are added
        return (String) locatorToColumnName.get(columnLocatorString);
    }

     public String getCustomColumnName(int column) {
        String columnName = null;
        String columnLocatorString = getColumnLocator(column);
        columnName = getColumnName(column);
        if (columnLocatorString != null) {
            String customColumnName = getCustomColumnName(columnLocatorString);
            if (customColumnName != null) {
                int index = columnName.lastIndexOf("\n");
               columnName = columnName.substring(0, index + 1) + " " + customColumnName;
            }
        }
        return columnName;
    }
    
    public String getColumnLocator(int column) {
        if (column < 0 || column > columnLocators.size() - 1) {
            return null;
        } else {
            return columnLocators.get(column);
        }
    }

    public List<String> getColumnLocators() {
        return new ArrayList<String>(columnLocators);
    }

    public void setColumnLocators(List<String> columnList) {
        removeAllColumnLocators();
        if (columnList != null) {
            addColumnLocators(columnList);
        }
    }

    public Map getCustomColumnNames() {
        return customColumnNames;
    }

    public void setCustomColumnNames(Map customColumnNames) {
        this.customColumnNames = customColumnNames;
        fireTableChanged(new TableModelEvent(BeanCollectionTableModel.this, TableModelEvent.HEADER_ROW));
    }

    public String getCustomColumnName(Object key) {
        return (String) customColumnNames.get(key);
    }

    public String getFormatedCustomColumnName(Object key) {
        String customName = (String) customColumnNames.get(key);
        if (customName != null) {
            customName = "[" + customName + "]";
        }
        return customName;
    }

    public void addCustomColumnName(Object key, String customName){
        customColumnNames.put(key, customName);
        fireColumnNameUpdated(key);
     }

    public void removeCustomColumnName(Object key){
        customColumnNames.remove(key);
        fireColumnNameUpdated(key);
    }

    private void fireColumnNameUpdated(Object key) {
        int columnId = getColumnIndex(key);
        if(columnId >= 0 && columnId < getColumnCount()) {
            fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, columnId, TableModelEvent.UPDATE));
        }
    }

    public Map getLocatorToDepthMap() {
        return locatorToColumnNameDepth;
    }

    public void setLocatorToDepthMap(Map locatorToDepthMap) {
        if (locatorToDepthMap != null) {
            for (Iterator iterator = locatorToDepthMap.keySet().iterator(); iterator.hasNext();) {
                String locator = (String) iterator.next();
                setColumnNameDepth(locator, ((Integer) locatorToDepthMap.get(locator)).intValue());
            }
        }
    }

    public int getRowCount() {
        return rows.size();
    }

    /**
     * note could return CachedObjectGraph.DATA_NOT_READY if this model is asynchronous
     */
    public Object getValueAt(int r, int c) {
        String locatorString = columnLocators.get(c);
        Object root = getBeanForRow(r);
        return objectCache.get(root, locatorString);
    }

    public void addTableModelListener(TableModelListener l) {
        if (propertyDisplayNameChangeListener == null) {
            propertyDisplayNameChangeListener = new TabularListPropertyChangeListener(this);
            PropertyModel.addPropertyDisplayNameChangeListener(propertyDisplayNameChangeListener);
        }

        if (log.isDebug()) log.debug("beanCollectionTableModel: " + this + " added listener: " + l + " count: " + tableModelListeners.size());
        tableModelListeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        if (log.isDebug()) log.debug("beanCollectionTableModel: " + this + " removed listener: " + l + " count: " + tableModelListeners.size());
        tableModelListeners.remove(l);
    }

    protected void fireTableChanged(TableModelEvent e) {
        //not using an iterator here because we can be calling this many tens of thousands of times per second, and each
        //iterator costs 24 bytes, meaning frequent garbage collection
        for (int i = 0; i < tableModelListeners.size(); i++) {
//            if (log.isDebug()) log.debug("Notifying " + i + " of table change event (" + e.getFirstRow() + ", " + e.getColumn() + ")");
            ((TableModelListener) tableModelListeners.get(i)).tableChanged(e);
        }
    }

    public void removeAllColumnLocators() {
        // iterates over columns in reverse order, removing them one by one (as this is more efficient for an indexedSet

        int size = columnLocators.size();
        for (int i = size - 1; i >= 0; i--) {
            String lastLocator = columnLocators.get(i);
            removeColumnLocator(lastLocator, false);
        }
        fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    public void removeColumnLocator(String locator) {
        removeColumnLocator(locator, true);
    }

    protected void removeColumnLocator(String locator, boolean fireEvent) {
        if (!EventQueue.isDispatchThread()) {
            log.warn("Probably should not try to remove columns without being on event thread");
        }

        int colIdx = getColumnIndex(locator);

        if (colIdx >= 0) {
            columnLocators.remove(locator);
            locatorToColumnNameDepth.remove(locator);
            locatorToColumnName.remove(locator);
            ColumnChangeListener listener = (ColumnChangeListener) locatorToListener.remove(locator);
            objectCache.removePathListener(locator, listener);

            if (fireEvent) {
                if (columnLocators.size() == 0) {
                    //structure change because we now have no rows (due to the fact that we have no columns)
                    fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
                } else {
                    fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, colIdx, TableModelEvent.DELETE));
                }
            }
        }
    }

    public void changeColumnLocator(String oldLocator, String newLocator) {
        changeColumnLocator(oldLocator, newLocator, true);
    }

    public void changeColumnLocator(String oldLocator, String newLocator, boolean fireTableChange) {
        final String intOldLocator = oldLocator.intern();
        final String intNewLocator = newLocator.intern();

        int columnId = getColumnIndex(intOldLocator);
        if(columnId >= 0) {
            try {
                columnLocators.set(columnId, intNewLocator);

                locatorToColumnNameDepth.put(intNewLocator, locatorToColumnNameDepth.remove(intOldLocator));
                String columnName = createDisplayName(intNewLocator);
                locatorToColumnName.remove(intOldLocator);
                locatorToColumnName.put(intNewLocator, columnName);

                ColumnChangeListener listener = (ColumnChangeListener) locatorToListener.remove(intOldLocator);
                objectCache.removePathListener(intOldLocator, listener);
                listener = new ColumnChangeListener(intNewLocator);
                locatorToListener.put(intNewLocator, listener);
                objectCache.addPathListener(intNewLocator, listener);

                if (fireTableChange) {
                    fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, columnId, TableModelEvent.UPDATE));
                }
            } catch (Exception e) {
                log.warn("Cannot change locator: " + newLocator, e);
                removeColumnLocator(newLocator);
                addColumnLocator(oldLocator);
            }
        }
    }

    public void addColumnLocator(String locator) {
        addColumnLocator(locator, true);
    }

    /**
     * expects a collection of strings
     */
    public void addColumnLocators(Collection locators) {
        for (Iterator iterator = locators.iterator(); iterator.hasNext();) {
            String locator = (String) iterator.next();
            addColumnLocator(locator, false);
        }
        //multiple columns changed. currently no way of indicating which ones. TODO: fix this
        fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    /**
     * common entry point for adding columns.
     * i.e. addColumnLocators(...) and addColumnLocator(String) both end up calling this method
     */
    protected void addColumnLocator(final String locator, final boolean fireTableChange) {
        assert locator != null;
        final String internedLocator = locator.intern();

        if (log.isDebug()) log.debug("Adding locator: " + internedLocator);
        if (!columnLocators.contains(internedLocator)) {

            try {
                String columnName = createDisplayName(internedLocator);
                ColumnChangeListener listener = new ColumnChangeListener(internedLocator);
                columnLocators.add(internedLocator);
                locatorToColumnName.put(internedLocator, columnName);
                locatorToListener.put(internedLocator, listener);
                objectCache.addPathListener(internedLocator, listener);

                if (fireTableChange) {
                    if (columnLocators.size() == 1) {
                        //structure change because we now have rows (previously we had no columns => no rows)
                        fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
                    } else {
                        int columnId = getColumnIndex(internedLocator);
                        fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, columnId, TableModelEvent.INSERT));
                    }
                }
            } catch (Exception e) {
                log.warn("Cannot add locator: " + internedLocator, e);
                removeColumnLocator(internedLocator);
            }
        }
    }

    public boolean isColumnVisible(String locator) {
        return columnLocators.contains(locator);
    }

    public void setColumnVisibleByLocator(String locator, boolean visible) {
        int modelColumIndex = getColumnIndex(locator);
        if (visible) {
            if (modelColumIndex < 0) {
                addColumnLocator(locator);
            }
        } else {
            if (modelColumIndex >= 0) {
                removeColumnLocator(locator);
            }
        }
    }

    /**
     * Changes the header name of an existing column in the table. If you remove and then add the column again, you will
     * have to reset the column name
     *
     * @param locator
     * @param columnName
     * @param fireTableChange
     */
    public void setColumnName(String locator, String columnName, boolean fireTableChange) {
        locatorToColumnName.put(locator, columnName);

        if (fireTableChange) {
            int columnId = getColumnIndex(locator);
            if (columnId >= 0) {
                fireTableChanged(new TableModelEvent(BeanCollectionTableModel.this, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, columnId));
            }
        }
    }

    public void setColumnNameDepth(String locator, int depth) {
        int columnIdx = getColumnIndex(locator);
        if (columnIdx >= 0) {
            if (depth == 1) {
                locatorToColumnNameDepth.remove(locator);
            } else {
                locatorToColumnNameDepth.put(locator, new Integer(depth));
            }
            try {
                locatorToColumnName.put(locator, createDisplayName(locator));
                fireTableChanged(new TableModelEvent(BeanCollectionTableModel.this, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, columnIdx, TableModelEvent.UPDATE));
            } catch (Exception e) {
                log.warn("Cannot set column name depth for locator: " + locator, e);
                locatorToColumnNameDepth.put(locator, new Integer(1));
            }
        }
    }

    /**
     * converts a property locator into a display name taking into account the chosen display name depth
     *
     * @param locator
     * @return the display name for the given column
     * @throws Exception
     */
    private String createDisplayName(String locator) throws Exception {
        if ("".equals(locator)) return "Root";
        StringBuffer columnNameBuffer = new StringBuffer();
        Integer preferredDepth = (Integer) locatorToColumnNameDepth.get(locator);
        int depth = preferredDepth != null ? preferredDepth.intValue() : 1;
        for (int i = 0; i < depth; i++) {
            if (i > 0) columnNameBuffer.insert(0, NEW_LINE);
            String attributeDisplayName = getDisplayNameForProperty(locator, i);
            columnNameBuffer.insert(0, attributeDisplayName);
        }

        return columnNameBuffer.toString();
    }

    private boolean isFormulaLocator(String locator) {
        return FormulaUtils.formulasEnabled() && FormulaUtils.isFormulaPath(locator);
    }

    /**
     * gets the display name for the indicated component of the property path by asking the static PropertyModel
     *
     * @param locator
     * @param depth
     * @return
     * @throws Exception
     */
    private String getDisplayNameForProperty(String locator, int depth) throws Exception {
        String[] locatorPath = Generic.beanPathStringToArray(locator);
        if(isFormulaLocator(locator)) {
            return FormulaUtils.getFormulaNameFromLocator(locatorPath);
        }

        Attribute attribute = PropertyModel.getInstance(beanClass).getAttribute((String[]) Utilities.subSection(locatorPath, 0, locatorPath.length - depth));
        if (attribute == null) throw new Exception("Cannot find display name for locator: " + locator + this.beanClass.getName());

        Class parentClass;
        if (locatorPath.length - depth > 1) {
            Attribute parentAttribute = PropertyModel.getInstance(beanClass).getAttribute((String[]) Utilities.subSection(locatorPath, 0, locatorPath.length - depth - 1));
            parentClass = parentAttribute.getType();
        } else {
            parentClass = beanClass;
        }

        if (parentClass != null) {
            parentClass = PropertyModel.getInstance(parentClass).findMatchingSubclass(attribute.getName());
        }

        if (parentClass == null) throw new Exception("Cannot find display name for locator: " + locator);
        return PropertyModel.getInstance(parentClass).getDisplayName(attribute.getName());
    }

    public Object[] getBeansForLocation(int row, int column) {
        return new Object[]{getBeanForRow(row)};
    }

    public boolean isSingleBean(int row, int column) {
        return true;
    }

    public Object getBeanForRow(int row) {
        return rows.get(row);
    }

    public int getRowForBean(Object bean) {
        return rows.indexOf(bean);
    }

    public int getColumnIndex(Object columnKey) {
        return columnLocators.indexOf(columnKey);
    }

    public Object getColumnKey(int column) {
        return getColumnLocator(column);
    }

    public void addSetStatement(SetStatement setStatement) {
        //add a mapping for any fixed bean paths
        String[] validBeanPaths = setStatement.getValidBeanPaths();
        for (int i = 0; i < validBeanPaths.length; i++) {
            String validBeanPath = validBeanPaths[i];
            locatorToSetStatement.put(validBeanPath, setStatement);
        }

        //Check for and handle the new PatternMatchingSetStatement interface
        //n.b. PatternMatchingSetStatement can also define fixed valid paths
        if ( setStatement instanceof PatternMatchingSetStatement ) {
            patternMatchingSetStatements.add((PatternMatchingSetStatement)setStatement);
        }
    }

    public boolean isColumnSettable(Object columnKey){
        //is there a valid set statement for this bean path?
        SetStatement s = getSetStatement(columnKey);
        return s != null;
    }

    public void setValueAt(Object value, int r, int c) {
        String beanPath = getColumnLocator(c);
        Object bean = getBeansForLocation(r, c)[0];
        try {
            SetConstraintHandler setConstraintHandler = SetConstraintHandler.getInstance(getBeanType());
            if (!setConstraintHandler.validate(bean, beanPath, value)) return;
            
            SetStatement setStatement = getSetStatement(beanPath);
            setStatement.execute(bean, beanPath, value);
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow(), t.getMessage(), "Unable to execute table edit", JOptionPane.ERROR_MESSAGE);
            log.error("Error executing set statement, edit aborted.  This SHOULD be caught in setStatement", t);
        }
    }

    public boolean isCellEditable(int row, int column) {
        String beanPath = getColumnLocator(column);
        if (getValueAt(row, column) == CachedObjectGraph.DATA_NOT_READY) return false;

        SetStatement setStatement = getSetStatement(beanPath);
        if (setStatement == null) return false;
        Object bean = getBeansForLocation(row, column)[0];
        return setStatement.isSettable(bean, beanPath);
    }

    private SetStatement getSetStatement(Object columnKey) {
        SetStatement s = null;
        //set statements can either be mapped to a fixed bean path or match a path pattern - check the explicit paths first
        if ( columnKey instanceof String) {
            s = getFixedPathSetStatement((String)columnKey);
            if ( s == null ) {
                s = getPatternMatchingSetStatement((String)columnKey);
            }
        }
        return s;
    }

    private SetStatement getFixedPathSetStatement(String columnKey) {
        return locatorToSetStatement.get(columnKey);
    }

    private SetStatement getPatternMatchingSetStatement(String columnKey) {
        for ( PatternMatchingSetStatement s : patternMatchingSetStatements ) {
            for (java.util.regex.Pattern p : s.getBeanPathPatterns()) {
                if ( p.matcher(columnKey).matches()) {
                    return s;
                }
            }
        }
        return null;
    }

//--------------------------------Inner Classes-----------------------------------------------

    /**
     * this will only get fired when the CachedObjectGraph fires a property change.
     * since we control when that is (via our ReschedulingChangeQueue, which is always on the swing thread)
     * we don't need to do an invoke later before firing an event.
     */
    private class ColumnChangeListener implements GraphChangeListener {
        private String columnKey;

        private ColumnChangeListener(String columnKey) {
            this.columnKey = columnKey;
        }

        public void multipleChange(Collection changes, boolean allAffectSameRoots) {
            //this means that lots of paths changed. We can leave it up to our instance of ObjectGraphMultipleChangeListener to sort that out
        }

        public void graphChanged(PathPropertyChangeEvent event) {
            Set rootBeans = event.getRoots();

            BeanCollectionTableModel myTabularList = BeanCollectionTableModel.this;
            final int columnIndex = myTabularList.getColumnIndex(columnKey);
            if(name != null) {
                TableUpdateDiagnostics.getInstance().addUpdate(name, columnKey);
            }

            if (columnIndex != -1) {
                CellsInColumnUpdatedEvent newEvent = null;
                if (rootBeans.size() == 1) {
                    Object rootBean = rootBeans.iterator().next();
                    int rowNum = myTabularList.getRowForBean(rootBean);
                    if (rowNum != -1) {
                        newEvent = new CellsInColumnUpdatedEvent(myTabularList, rowNum, columnIndex, event);
                    }
                } else {
                    int[] rows = new int[rootBeans.size()];
                    int i = 0;
                    Iterator iterator = rootBeans.iterator();
                    while (iterator.hasNext()) {
                        Object root = iterator.next();
                        int rowForBean = getRowForBean(root);
                        if (rowForBean >= 0) {
                            rows[i++] = rowForBean;
                        }
                    }
                    if (i < rows.length) {
                        int[] newRows = new int[i];
                        System.arraycopy(rows, 0, newRows, 0, i);
                        rows = newRows;
                    }

                    if (i > 0) {
                        newEvent = new CellsInColumnUpdatedEvent(myTabularList, rows, columnIndex, event);
                    }
                }
                if (newEvent != null) {
                    fireTableChanged(newEvent);
                }
            }
            // else { ooo look, it was an event for a bean or column that has subsequently been removed. How sad.}
        }
    }


    private static class TabularListPropertyChangeListener extends PropertyModel.WeakPropertyChangeListener {
        public TabularListPropertyChangeListener(Object listenerOwner) {
            super(listenerOwner);
        }

        /**
         * Called when the display name for a specific propertyType / property name changes
         */
        public void displayNameChanged(final Object listenerOwner, final Class parentType, final String propertyName, String newDisplayName) {
            if (log.isDebug()) log.debug("beanCollectionTableModel got property rename " + parentType.getName() + "." + propertyName + " = " + newDisplayName);

            final BeanCollectionTableModel beanCollectionTableModel = (BeanCollectionTableModel) listenerOwner;

            Runnable runnable = new Runnable() {
                public void run() {
                    List changedIndexes = new ArrayList();

                    int i = 0;
                    for (Iterator iter = beanCollectionTableModel.columnLocators.iterator(); iter.hasNext(); i++) {
                        String locator = (String) iter.next();
                        if(FormulaUtils.isFormulaPath(locator)) {
                            continue;
                        }
                        String[] locatorPath = Generic.beanPathStringToArray(locator);
                        for (int n = 0; n < locatorPath.length; n++) {
                            Attribute subAttribute = PropertyModel.getInstance(beanCollectionTableModel.beanClass).getAttribute((String[]) Utilities.subSection(locatorPath, 0, locatorPath.length - n));

                            Class parentClass;
                            if (locatorPath.length - n > 1) {
                                Attribute parentAttribute = PropertyModel.getInstance(beanCollectionTableModel.beanClass).getAttribute((String[]) Utilities.subSection(locatorPath, 0, locatorPath.length - n - 1));
                                parentClass = parentAttribute.getType();
                            } else {
                                parentClass = beanCollectionTableModel.beanClass;
                            }

                            if (parentClass.equals(parentType) && subAttribute.getName().equals(propertyName)) {
                                try {
                                    beanCollectionTableModel.locatorToColumnName.put(locator, beanCollectionTableModel.createDisplayName(locator));
                                    changedIndexes.add(new Integer(i));
                                } catch (Exception e) {
                                    log.error("Cannot change display name for locator: " + locator, e);
                                }
                                break;
                            }
                        }
                    }

                    for (Iterator iterator = changedIndexes.iterator(); iterator.hasNext();) {
                        Integer idx = (Integer) iterator.next();
                        beanCollectionTableModel.fireTableChanged(new TableModelEvent(beanCollectionTableModel, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW, idx.intValue()));
                    }
                }
            };

            UIUtilities.runInDispatchThread(runnable);
        }

        public void hiddenStatusChanged(Object listenerOwner, Class parentType, String propertyName, boolean hidden) {
            // don't do anything - we are not going to remove column from the view 
        }
    }

    protected abstract class ChangeQueue extends SwingTask {
        abstract void addChange(final CachedObjectGraph.CachedProperties cachedProperties, final PropertyChangeEvent event);
    }


    /**
     * attempt to shove as many changes through as possible without taking more than 150 milliseconds.
     */
    private class ReschedulingChangeQueue extends ChangeQueue {
        //ArrayList used here previously tended to end up holding onto sparse arrays with 99% null elements - this was inefficient in terms of memory
        //this is because the internal array for ArrayList never shrinks, unless you call trimToSize() - and so would remain sized for the worst case scenario
        private LinkedList propertyQueue = new LinkedList(); 
        private LinkedList propertyChangeQueue = new LinkedList();
        private Object queueChangeLock = new Object();
        private boolean isWaitingForService = false;

        public void addChange(final CachedObjectGraph.CachedProperties cachedProperties, final PropertyChangeEvent event) {
            boolean start = false;
            //else add to queue
            synchronized (queueChangeLock) {
                propertyQueue.add(cachedProperties);
                propertyChangeQueue.add(event);

                //if changed from empty, put in a service request
                if (!isWaitingForService) {
                    start = true;
                    isWaitingForService = true;
                }
                if (start) {
                    scheduleTask(50);
                }
            }
        }


        @Override
        protected void runOnEventThread() {
            try {
                Object[] properties;
                Object[] propertyChanges;
                synchronized (queueChangeLock) {
                    propertyChanges = propertyChangeQueue.toArray();
                    propertyChangeQueue.clear();
                    properties = propertyQueue.toArray();
                    propertyQueue.clear();
                }


                int i = 0;
                synchronized (objectCache.getChangeLock()) {
                    long time = System.currentTimeMillis();
                    while (i < propertyChanges.length && (System.currentTimeMillis() - time < 50)) {
                        CachedObjectGraph.CachedProperties property = (CachedObjectGraph.CachedProperties) properties[i];
                        PropertyChangeEvent event = (PropertyChangeEvent) propertyChanges[i];
                        try {
                            objectCache.processPropertyChange(property, event);
                        } catch (Exception e) {
                            log.error("Object cache could not process property change event due to exception, event: " + event, e);
                        }
                        i++;
                    }
                }
                synchronized (queueChangeLock) {
                    if (i < propertyChanges.length) {
                        //oops, we have blocked the awt thread for 1/5 seconds, reschedule the rest for later
                        requeueChanges(i, propertyChanges, properties);
                    }

                    if (!propertyChangeQueue.isEmpty()) {
                        scheduleTask(50);
                    } else {
                        isWaitingForService = false;
                        // If anyone is waiting for all pending events to end, then notify
                        queueChangeLock.notify();
                    }
                }
            } catch (Exception e) {
                log.error("Exception while dispatching bundled cell change events", e);
            }
        }

        private void requeueChanges(int i, Object[] propertyChanges, Object[] properties) {
            int pendingSize = propertyChanges.length - i;
            if (log.isDebug()) log.debug("Took too long processing events, rescheduling " + pendingSize + " for later");

            Object[] pendingChanges = new Object[pendingSize];
            Object[] pendingProperties = new Object[pendingSize];
            System.arraycopy(propertyChanges, i, pendingChanges, 0, pendingSize);
            System.arraycopy(properties, i, pendingProperties, 0, pendingSize);

            propertyChangeQueue.addAll(0, Arrays.asList(pendingChanges));
            propertyQueue.addAll(0, Arrays.asList(pendingProperties));
        }
    }

    public Object getChangeLock() {
        return changeLock;
    }

    public void setCachedObjectGraphExecutionController(CachedObjectGraph.ExecutionController executionController) {
        objectCache.setExecutionController(executionController);
    }

    public final void waitForEventsToClear() throws InterruptedException {
            synchronized (reschedulingChangeQueue.queueChangeLock) {
                if (reschedulingChangeQueue.propertyQueue.size() > 0) {
                    reschedulingChangeQueue.queueChangeLock.wait();
                }
        }
    }

    ChangeQueue getChangeQueue() {
        return reschedulingChangeQueue;
    }

//    public SingleChangeQueue getChangeQueue() {
//        return testQueue;
//    }

    private SingleChangeQueue testQueue = new SingleChangeQueue();

    /**
     * used to test asynchronous behavious. a javax.swing.Timer calls the actionPerformed
     */
    private class SingleChangeQueue extends ChangeQueue {
        private LinkedList propertyQueue = new LinkedList();
        private LinkedList propertyChangeQueue = new LinkedList();
        private Object changeLock = new Object();

        public void addChange(CachedObjectGraph.CachedProperties cachedProperties, PropertyChangeEvent event) {
            scheduleTask(1000);
            synchronized (changeLock) {
                propertyQueue.add(cachedProperties);
                propertyChangeQueue.add(event);
            }
        }

        @Override
        protected void runOnEventThread() {
            try {
                if (propertyQueue.size() > 0) {
                    CachedObjectGraph.CachedProperties property;
                    PropertyChangeEvent event;
                    synchronized (changeLock) {
                        event = (PropertyChangeEvent) propertyChangeQueue.removeFirst();
                        property = (CachedObjectGraph.CachedProperties) propertyQueue.removeFirst();
                    }

                    synchronized (objectCache.getChangeLock()) {
                        objectCache.processPropertyChange(property, event);
                    }
                } else {
                    stopTask();
                }
            } catch (Exception e) {
                log.error("Exception while dispatching cell change event", e);
            }
        }
    }

    private class ObjectGraphMultipleChangeListener implements GraphChangeListener {
        public void graphChanged(PathPropertyChangeEvent event) {
        }

        public void multipleChange(Collection events, boolean allAffectSameRoots) {
            assert (events.size() > 1) : "This should be fired as a 'CellsInColumnUpdatedEvent'";

            CellsInColumnUpdatedEvent[] newEvents = new CellsInColumnUpdatedEvent[events.size()];
            int[] rows = null;

            int i = 0;
            Iterator iterator = events.iterator();
            while (iterator.hasNext()) {
                PathPropertyChangeEvent event = (PathPropertyChangeEvent) iterator.next();
                String columnKey = Generic.beanPathArrayToString(event.getPathFromRoot());
                if(name != null) {
                    TableUpdateDiagnostics.getInstance().addUpdate(name, columnKey);
                }
                if (i == 0 || !allAffectSameRoots) {
                    rows = getRowsFromEvent(event);
                }
                if (rows != null) {
                    int col = getColumnIndex(columnKey);
                    newEvents[i++] = new CellsInColumnUpdatedEvent(BeanCollectionTableModel.this, rows, col, event);
                } else {
                    // if those rows were null and all changes shared the same rows then there is nothing to do.
                    if (allAffectSameRoots) return;
                }
            }
            //if we pruned some of those events due to null rows, then we need to resize the array to fit.
            if (newEvents.length != i) {
                CellsInColumnUpdatedEvent[] tmp = new CellsInColumnUpdatedEvent[i];
                System.arraycopy(newEvents, 0, tmp, 0, i);
                newEvents = tmp;
            }
            fireTableChanged(new MultipleColumnChangeEvent(BeanCollectionTableModel.this, newEvents, allAffectSameRoots));
        }

        private int[] getRowsFromEvent(PathPropertyChangeEvent event) {
            int rowCount;
            Iterator iterator;
            Set roots = event.getRoots();
            int[] rows = new int[roots.size()];
            rowCount = 0;
            iterator = roots.iterator();
            while (iterator.hasNext()) {
                Object root = iterator.next();
                int rowForBean = getRowForBean(root);
                if (rowForBean >= 0) {
                    rows[rowCount++] = rowForBean;
                }
            }
            if (rowCount == 0) return null;

            if (rowCount < rows.length) {
                int[] newRows = new int[rowCount];
                System.arraycopy(rows, 0, newRows, 0, rowCount);
                rows = newRows;
            }
            return rows;
        }
    }

    private class TransactionQueue implements CachedObjectGraph.RowReadyListener {
        private ArrayList transactions = new ArrayList(4);
        private HashSet waitingFor;
        private int clearCount = 0;

        public TransactionQueue() {
            waitingFor = new HashSet(4);
        }

        public void rowReady(Object root) {
            synchronized (transactions) {
                waitingFor.remove(root);
                doAllReady();
            }
        }

        private void doAllReady() {
            while (waitingFor.isEmpty() && transactions.size() > 0) {
                applyNextTransaction();
                if (transactions.size() > 0) {
                    beginWaiting((ArrayList) transactions.get(0));
                }
            }
        }

        private void applyNextTransaction() {
            final ArrayList currentTransaction = ((ArrayList) transactions.remove(0));
            final int clearCountCopy = clearCount;
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    //process deferredInsertsAndDeletes
                    synchronized (objectCache.getChangeLock()) {
                        synchronized (transactions) {
                            assert (clearCountCopy >= clearCount) : "Bug, a work request that was subsequently invalidated by an allRowsChanged is occuring after the all rows event!";
                            for (Iterator iterator = currentTransaction.iterator(); iterator.hasNext();) {
                                ListEvent event = (ListEvent) iterator.next();
                                if (event.getType() == ListEvent.INSERT) {
                                    beanAdded(event.getValue(), true);
                                } else if (event.getType() == ListEvent.DELETE) {
                                    beanRemoved(event.getValue());
                                }
                            }
                        }
                    }
                }
            });
        }

        public void add(final ArrayList transaction) {
            synchronized (transactions) {
                transactions.add(transaction);
                if (transactions.size() == 1) {
                    beginWaiting(transaction);
                    doAllReady();
                }
            }
        }

        private void beginWaiting(final ArrayList transaction) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    //process deferredInsertsAndDeletes
                    synchronized (objectCache.getChangeLock()) {
                        Iterator iterator = transaction.iterator();
                        waitingFor.clear();
                        while (iterator.hasNext()) {
                            ListEvent event = (ListEvent) iterator.next();
                            if (event.getType() == ListEvent.INSERT) {
                                Object newRow = event.getValue();
                                waitingFor.add(newRow);
                                objectCache.addRootObject(newRow);
                            }
                        }
                    }
                }
            });
        }

        public void clear() {
            synchronized (transactions) {
                clearCount++;
                waitingFor.clear();
                transactions.clear();
            }
        }
    }

    private static class ChangeLock {
    };
}