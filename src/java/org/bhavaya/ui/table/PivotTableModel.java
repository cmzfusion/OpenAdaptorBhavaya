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

import org.bhavaya.util.*;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.19 $
 */
public class PivotTableModel extends AbstractTableModel implements TabularBeanAssociation, ChainableTableModel {
    private static final Log log = Log.getCategory(PivotTableModel.class);

    static {
        BeanUtilities.addPersistenceDelegate(GeneratedColumnKey.class, new BhavayaPersistenceDelegate(new String[]{"pivotValue"}));
        BeanUtilities.addPersistenceDelegate(SpecialPivotValue.class, new BhavayaPersistenceDelegate(new String[]{"pivotValue"}));
    }

    public static final GeneratedColumnKey TOTAL_COLUMN_KEY = new GeneratedColumnKey(new SpecialPivotValue("Total"));
    public static final GeneratedColumnKey UNDEFINED_COLUMN_KEY = new GeneratedColumnKey(PivotValuesCollection.UNDEFINED_PIVOT_COLUMN_KEY);
    public static final GeneratedColumnKey DATA_NOT_READY_KEY = new GeneratedColumnKey(CachedObjectGraph.DATA_NOT_READY.toString());

    public static final List<GeneratedColumnKey> NOT_LOCKABLE_PIVOT_KEYS = new ArrayList<GeneratedColumnKey>(Arrays.asList(
            TOTAL_COLUMN_KEY,
            UNDEFINED_COLUMN_KEY,
            DATA_NOT_READY_KEY
    ));

    private static final Object INTERMEDIATE_STATE = new Object();
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private KeyedColumnTableModel sourceModel;

    private boolean modelInvalid = true;
    private boolean columnTotallingEnabled = false;
    private TableModelEventHandler eventHandler;

    private Object pivotColumnKey = null;
    private Object dataColumnKey = null;
    private int pivotColumnIndex = -1;
    private int dataColumnIndex = -1;

    private ArrayList totalColumn = new ArrayList();
    private PivotValuesCollection pivotValues = new PivotValuesCollection();
    private ArrayList<GeneratedColumnKey> generatedColumnKeys = new ArrayList<GeneratedColumnKey>();
    private ArrayList pivotValuePerRow = new ArrayList();

    private DataColumnFilter suitableDataColumnFilter = new DefaultDataColumnFilter();
    private PivotColumnFilter suitablePivotColumnFilter = new DefaultPivotColumnFilter();


    public PivotTableModel() {
        eventHandler = new TableModelEventHandler();
    }

    public PivotTableModel(KeyedColumnTableModel sourceModel) {
        this();
        setSourceModel(sourceModel);
    }

    public void setSourceModel(KeyedColumnTableModel sourceModel) {
        if (this.sourceModel != null) {
            this.sourceModel.removeTableModelListener(eventHandler);
        }
        this.sourceModel = sourceModel;
        if (this.sourceModel != null) {
            this.sourceModel.addTableModelListener(eventHandler);
        }
        setModelInvalid();
        fireTableStructureChanged();
    }

    public DataColumnFilter getSuitableDataColumnFilter() {
        return suitableDataColumnFilter;
    }

    public void setSuitableDataColumnFilter(DataColumnFilter suitableDataColumnFilter) {
        this.suitableDataColumnFilter = suitableDataColumnFilter;
        setModelInvalid();
        fireTableStructureChanged();
    }

    public PivotColumnFilter getSuitablePivotColumnFilter() {
        return suitablePivotColumnFilter;
    }

    public void setSuitablePivotColumnFilter(PivotColumnFilter suitablePivotColumnFilter) {
        this.suitablePivotColumnFilter = suitablePivotColumnFilter;
        setModelInvalid();
        fireTableStructureChanged();
    }


    private boolean isModelInvalid() {
        return modelInvalid;
    }

    private void setModelInvalid() {
        modelInvalid = true;
        invalidatePivotColumnIndex();
        invalidateDataColumnIndex();
    }


    public int getColumnCount() {
        recalculateIfInvalid();
        if (sourceModel == null) return 0;
        if (! hasValidPivotColumn()) return getSourceModel().getColumnCount();

        int count = getNonGeneratedColumnCount();
        count += pivotValues.size();
        return count + (isColumnTotallingEnabled() ? 1 : 0);
    }

    /**
     * @return the index of the column that is used to generate the data columns seem during a data.
     */
    private int getDataColumnIndex() {
        if (dataColumnIndex < 0) {
            int columnIndex = getSourceModel().getColumnIndex(getDataColumnKey());
            if (hasValidPivotColumn()) {    //no pivot column means no data column index is valid
                dataColumnIndex = columnIndex;
            }
        }
        return dataColumnIndex;
    }

    private void invalidateDataColumnIndex() {
        dataColumnIndex = -1;
    }

    public Object getDataColumnKey() {
        if (getSourceModel() == null) return null;
        if (dataColumnKey == INTERMEDIATE_STATE) return null;
        int index = getSourceModel().getColumnIndex(dataColumnKey);
        if (index < 0) {
            dataColumnKey = INTERMEDIATE_STATE;
            //watch out for infinite recursion, a search strategy could indirectly cause an attempt to get it again.
            dataColumnKey = findBestDataColumnKey();
        }

        return dataColumnKey;
    }

    public void setDataColumnKey(Object dataColumnKey) {
        if ( ! dataColumnKey.equals(this.dataColumnKey)) {
            if (getSourceModel().getColumnIndex(dataColumnKey) >= 0) {
                Object oldKey = this.dataColumnKey;
                this.dataColumnKey = dataColumnKey;
                if (getSuitableDataColumnFilter().isValidDataColumn(dataColumnKey)) {
                    setModelInvalid();
                    fireTableStructureChanged();
                } else {
                    this.dataColumnKey = oldKey;
                    Exception e = new IllegalArgumentException("Asked to set data column to " + dataColumnKey + " but it is not a permissable data column");
                    log.warn(e.getMessage(), e);
                }
            }
        }
    }

    public KeyedColumnTableModel getSourceModel() {
        return sourceModel;
    }


    /**
     * @return the index of the column that is used to generate the data columns seem during a pivot.
     */
    private int getPivotColumnIndex() {
        if (pivotColumnIndex < 0) {
            pivotColumnIndex = getSourceModel().getColumnIndex(getPivotColumnKey());
        }
        return pivotColumnIndex;
    }

    private void invalidatePivotColumnIndex() {
        pivotColumnIndex = -1;
    }

    public Object getPivotColumnKey() {
        if (getSourceModel() == null) return null;
        if (pivotColumnKey == INTERMEDIATE_STATE) return null;

        int index = getSourceModel().getColumnIndex(pivotColumnKey);
        if (index < 0) {
            pivotColumnKey = INTERMEDIATE_STATE;
            if (dataColumnKey == null) {
                dataColumnKey = findBestDataColumnKey();
            }
            //watch out for infinite recursion, a search strategy could indirectly cause an attempt to get it again.
            pivotColumnKey = findBestPivotColumnKey();
        }

        return pivotColumnKey;
    }

    public void setPivotColumnKey(Object pivotColumnKey) {
        if ( ! pivotColumnKey.equals(this.pivotColumnKey)) {
            Object oldKey = this.pivotColumnKey;
            this.pivotColumnKey = pivotColumnKey;
            if (getSuitablePivotColumnFilter().isValidPivotColumn(pivotColumnKey)) {
                setModelInvalid();
                fireTableStructureChanged();
            } else {
                this.pivotColumnKey = oldKey;
                Exception e = new IllegalArgumentException("Asked to set pivot column to " + pivotColumnKey + " but it is not a permissable pivot column");
                log.warn(e.getMessage(), e);
            }
        }
    }

    public void addLockedPivotColumn(String pivotValue) {
        addLockedPivotColumn(new GeneratedColumnKey(pivotValue));
    }

    /**
     * Add a pivot column which will be displayed whether or not the pivotColumnValue
     * actually exists in the source model
     */
    public void addLockedPivotColumn(GeneratedColumnKey columnKey) {
        recalculateIfInvalid();
        String pivotValue = columnKey.getPivotValue();
        if (isLockablePivotColumn(columnKey)) {
            boolean newLocked = pivotValues.addLockedColumnPivotValue(pivotValue);
            if ( newLocked ) {
                if (! pivotValues.contains(pivotValue)) {
                    addPivotColumnForValue(pivotValue);
                }
                fireTableStructureChanged();
            }
        }
    }

    public void removeLockedPivotColumn(String pivotValue) {
        removeLockedPivotColumn(new GeneratedColumnKey(pivotValue));
    }

    public void removeLockedPivotColumn(GeneratedColumnKey columnKey) {
        recalculateIfInvalid();
        String pivotValue = columnKey.getPivotValue();
        if (isLockablePivotColumn(columnKey)) {
            boolean removed = pivotValues.removeLockedColumnPivotValue(pivotValue);
            if ( removed ) {
                //even if we 'unfix' a column, we shouldn't delete it if there are still valid data values displayed
                if ( pivotValues.getUsageCount(pivotValue).value <= 0) {
                    List<String> columnsToDelete = new ArrayList<String>();
                    columnsToDelete.add(pivotValue);
                    removeUnlockedColumns(columnsToDelete);
                }
                //must fire a structure change since unlocking a column may
                //trigger reordering of table column model
                fireTableStructureChanged();
            }
        }
    }

    public void clearLockedPivotColumns() {
        recalculateIfInvalid();
        Collection<String> lockedPivotValues = pivotValues.getLockedPivotValues();
        String[] lockedCols = lockedPivotValues.toArray(new String[lockedPivotValues.size()]);
        for ( String pivotValue : lockedCols ) {
            removeLockedPivotColumn(pivotValue);
        }
    }

    public void setLockedPivotColumnValues(Set<String> pivotColumnValues) {
        recalculateIfInvalid();
        clearLockedPivotColumns();
        for ( String pivotValue : pivotColumnValues ) {
            addLockedPivotColumn(pivotValue);
        }
    }

    public Set<String> getLockedPivotColumnValues() {
        return new HashSet<String>(pivotValues.getLockedPivotValues()); //return a defensive copy.
    }

    public boolean isLockedPivotColumn(GeneratedColumnKey pivotColumnKey) {
        boolean result = false;
        if ( isLockablePivotColumn(pivotColumnKey)) {
            result = pivotValues.isLockedPivotValue(pivotColumnKey.getPivotValue());
        }
        return result;
    }

    public boolean isLockablePivotColumn(GeneratedColumnKey columnKey) {
        return ! NOT_LOCKABLE_PIVOT_KEYS.contains(columnKey);
    }

    private Object findBestPivotColumnKey() {
        KeyedColumnTableModel sourceModel = getSourceModel();
        if (sourceModel != null) {
            for (int column = sourceModel.getColumnCount() - 1; column >= 0; column--) {
                Object columnKey = sourceModel.getColumnKey(column);
                if (!Utilities.equals(columnKey, dataColumnKey)) {  //important to use fieled access to prevent inf loop
                    if (getSuitablePivotColumnFilter().isValidPivotColumn(columnKey)) {
                        return columnKey;
                    }
                }
            }
        }
        //no suitable pivot column
        return null;
    }

    private Object findBestDataColumnKey() {
        KeyedColumnTableModel sourceModel = getSourceModel();
        int columnCount = sourceModel.getColumnCount();
        for (int column = columnCount - 1; column >= 0; column--) {
            Object columnKey = sourceModel.getColumnKey(column);
            if (!Utilities.equals(columnKey, pivotColumnKey)) {
                //important to use fieled access to prevent inf loop
                if (getSuitableDataColumnFilter().isValidDataColumn(columnKey)) {
                    return columnKey;
                }
            }
        }
        //no suitable data column
        return null;
    }

    /**
     * @return true if the given column index is a column generated by the pivot operation
     */
    public boolean isGeneratedColumn(Object columnKey) {
        return (columnKey instanceof GeneratedColumnKey);
    }

    /**
     * @param columnIdx
     * @return true if the given column index is a column generated by the pivot operation
     */
    public boolean isGeneratedColumn(int columnIdx) {
        recalculateIfInvalid();
        int c = mapColumnToUnderlying(columnIdx);
        return c < 0;
    }

    /**
     * @return the underlying models column index for a column in this model.
     */
    private int mapColumnToUnderlying(int column) {
        int pivotColumnIndex = getPivotColumnIndex();
        if (pivotColumnIndex < 0) return column;   //no valid pivot column so no change

        if (column < getNonGeneratedColumnCount()) {
            int dataColumnIndex = hasValidDataColumn() ? getDataColumnIndex() : Integer.MAX_VALUE;
            if (!hasValidPivotColumn()) pivotColumnIndex = Integer.MAX_VALUE;

            if (column >= Math.min(dataColumnIndex, pivotColumnIndex)) {
                column = column + 1;
            }
            if (column >= Math.max(dataColumnIndex, pivotColumnIndex)) {
                column = column + 1;
            }
            return column;
        }
        return -1;
    }

    private int getNonGeneratedColumnCount() {
        int nonGeneratedColumnCount = (getSourceModel().getColumnCount());
        if (hasValidPivotColumn()) nonGeneratedColumnCount--;    //we have a valid pivot column
        if (hasValidDataColumn()) nonGeneratedColumnCount--;    //we have a valid data column
        return nonGeneratedColumnCount;
    }

    /**
     * don't call this method if the underlyingColumn might be the pivot or the data column
     *
     * @param underlyingColumn
     */
    private int mapUnderlyingToModel(int underlyingColumn) {
        int mappedColumn = underlyingColumn;
        if (hasValidPivotColumn() && mappedColumn >= getPivotColumnIndex()) {
            mappedColumn--;
        }
        if (hasValidDataColumn() && mappedColumn >= getDataColumnIndex()) {
            mappedColumn--;
        }
        return mappedColumn;
    }


    private int getColumnIndexForPivotKey(Object pivotKey) {
        recalculateIfInvalid();
        int pivotIndex = pivotValues.indexOf(pivotKey);
        if (pivotIndex < 0) {
            return -1;
        } else {
            return getNonGeneratedColumnCount() + pivotIndex;
        }
    }

    private void recalculateIfInvalid() {
        if (isModelInvalid()) {
            modelInvalid = false;
            int pivot = getPivotColumnIndex();
            pivotValues.clear();
            generatedColumnKeys.clear();
            pivotValuePerRow.clear();
            totalColumn.clear();

            for ( String pivotValue : pivotValues.getLockedPivotValues() ) {
                addPivotColumnForValue(pivotValue);
            }

            TableModel tableModel = getSourceModel();
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                Object value = tableModel.getValueAt(row, pivot);
                if (isColumnTotallingEnabled()) {
                    Object rowTotal = null;
                    if (hasValidDataColumn()) rowTotal = tableModel.getValueAt(row, getDataColumnIndex());
                    totalColumn.add(rowTotal);
                }

                if (!pivotValues.contains(value)) {
                    addPivotColumnForValue(value);
                }
                changeUsageCount(value, +1);
                pivotValuePerRow.add(value);
            }

            if (isColumnTotallingEnabled()) {
                addGeneratedColumnKey(TOTAL_COLUMN_KEY);
            }
        }
    }

    private void addPivotColumnForValue(Object value) {
        int newColIndex = registerPivotValue(value);
        String pivotValue = PivotValuesCollection.convertPivotValueToString(value);
        insertGeneratedColumnKey(newColIndex, pivotValue);
    }

    /**
     * @return index of the removed column
     */
    private int removePivotColumnForValue(Object value) {
        int index = unregisterPivotValue(value);
        removeGeneratedColumnKey(index);
        return index;
    }

    private int registerPivotValue(Object value) {
        int insertPoint = pivotValues.getPivotColumnIndex(value);
        if (insertPoint < 0) insertPoint = (-insertPoint) - 1;
        pivotValues.add(insertPoint, value);
        pivotValues.createUsageCount(value, new MutableInteger(0));
        return insertPoint;
    }

    /**
     * @return index of the removed value (from what position it's been removed)
     */
    private int unregisterPivotValue(Object value) {
        int idx = pivotValues.indexOf(value);
        if (idx < 0) return -1;
        pivotValues.remove(value);
        pivotValues.removeUsageCount(value);
        return idx;
    }

    private int changeUsageCount(Object value, int delta) {
        MutableInteger mutableInteger = pivotValues.getUsageCount(value);
        mutableInteger.value += delta;
        return mutableInteger.value;
    }

    private void addGeneratedColumnKey(GeneratedColumnKey key) {
        generatedColumnKeys.add(key);
    }

    /**
     * @param insertColIndex index within generated column
     */
    private void insertGeneratedColumnKey(int insertColIndex, String pivotValue) {
        generatedColumnKeys.add(insertColIndex, new GeneratedColumnKey(pivotValue));
    }

    private void removeGeneratedColumnKey(int index) {
        generatedColumnKeys.remove(index);
    }

    private List getGeneratedColumnKeys() {
        recalculateIfInvalid();
        return generatedColumnKeys;
    }


    public boolean isColumnTotallingEnabled() {
        return columnTotallingEnabled;
    }

    public void setColumnTotallingEnabled(boolean columnTotallingEnabled) {
        this.columnTotallingEnabled = columnTotallingEnabled;
        setModelInvalid();
        fireTableStructureChanged();
    }

    public int getRowCount() {
        if (sourceModel == null) return 0;
        return getSourceModel().getRowCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        recalculateIfInvalid();
        int underlyingCol = mapColumnToUnderlying(columnIndex);
        KeyedColumnTableModel sourceModel = getSourceModel();
        if (underlyingCol < 0) {   //generated column
            if (!hasValidDataColumn()) return null;  //no valid data column, so return null

            if (isColumnTotallingEnabled() && columnIndex == getTotalColumnIndex()) {
                return totalColumn.get(rowIndex);
            }
            Object rowPivotKey = pivotValuePerRow.get(rowIndex);
            int pivotIndex = getColumnIndexForPivotKey(rowPivotKey);

            if (pivotIndex == columnIndex) {
                underlyingCol = getDataColumnIndex();
            } else {
                return null;
            }
        }
        return sourceModel.getValueAt(rowIndex, underlyingCol);
    }

    private int getTotalColumnIndex() {
        return (isColumnTotallingEnabled() && hasValidPivotColumn()) ? getColumnCount() - 1 : -1;
    }

    private boolean hasValidPivotColumn() {
        return getPivotColumnIndex() >= 0;
    }

    private boolean hasValidDataColumn() {
        return getDataColumnIndex() >= 0;
    }

    public String getColumnName(int column) {
        recalculateIfInvalid();
        TableModel sourceModel = getSourceModel();
        if (sourceModel == null) {
            log.warn("PivotTableModel does not have a source tableModel");
            return "";
        }
        int c = mapColumnToUnderlying(column);
        if (c >= 0) {
            return sourceModel.getColumnName(c);
        }
        GeneratedColumnKey columnKey = (GeneratedColumnKey) getColumnKey(column);
        return columnKey.getPivotValue();
    }

    public Class getColumnClass(int columnIndex) {
        recalculateIfInvalid();
        TableModel tableModel = getSourceModel();

        int c = mapColumnToUnderlying(columnIndex);
        Class result;
        if (c >= 0) {
            result = tableModel.getColumnClass(c);
        } else {         //column is one of the generated columns
            int dataColumn = getDataColumnIndex();
            result = (dataColumn == -1) ? Object.class : tableModel.getColumnClass(dataColumn);
        }
        return result;
    }

    /**
     * never let a "data changed" go through. It is always a structure change to cover for added or removed columns
     */
    public void fireTableDataChanged() {
        fireTableStructureChanged();
    }

    public Object[] getBeansForLocation(int row, int column) {
        recalculateIfInvalid();
        KeyedColumnTableModel tableModel = getSourceModel();
        if (tableModel instanceof TabularBeanAssociation) {
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) tableModel;

            int underlyingColumn;
            if (!isGeneratedColumn(column)) {
                underlyingColumn = mapColumnToUnderlying(column);
            } else {
                if (column == getTotalColumnIndex()) {
                    underlyingColumn = getDataColumnIndex();
                } else {
                    Object rowPivotKey = getSourceModel().getValueAt(row, getPivotColumnIndex());
                    int pivotIndex = getColumnIndexForPivotKey(rowPivotKey);
                    if (pivotIndex == column) {
                        underlyingColumn = getDataColumnIndex();
                    } else {
                        underlyingColumn = -1;   //wrong pivot cell for this row
                    }
                }
            }
            if (underlyingColumn < 0) return EMPTY_OBJECT_ARRAY;
            return tabularBeanAssociation.getBeansForLocation(row, underlyingColumn);
        }
        return EMPTY_OBJECT_ARRAY;
    }

    public boolean isSingleBean(int row, int column) {
        return false;
    }

    public Object getColumnKey(int column) {
        recalculateIfInvalid();
        if (column < 0) return null;

        int c = mapColumnToUnderlying(column);
        if (c >= 0) {
            return getSourceModel().getColumnKey(c);
        } else {
            // it is a generated column
            int generatedColumnIndex = column - getNonGeneratedColumnCount();
            if (generatedColumnIndex < 0 || generatedColumnIndex > getGeneratedColumnKeys().size()) {
                return null;
            } else {
                return getGeneratedColumnKeys().get(generatedColumnIndex);
            }
        }
    }

    public int getColumnIndex(Object columnKey) {
        recalculateIfInvalid();
        if (columnKey instanceof GeneratedColumnKey) {
            int listIndex = generatedColumnKeys.indexOf((GeneratedColumnKey)columnKey);
            return listIndex >= 0 ? getNonGeneratedColumnCount() + listIndex : -1;
        } else if ( columnKey.equals(pivotColumnKey) || columnKey.equals(dataColumnKey)) {
            return -1;
        } else {
            KeyedColumnTableModel sourceModel = getSourceModel();
            if (sourceModel == null) return -1;
            int underlyingColumn = sourceModel.getColumnIndex(columnKey);
            return mapUnderlyingToModel(underlyingColumn);
        }
    }

    public void invertPivot() {
        recalculateIfInvalid();
        Object pivotColumnKey = getPivotColumnKey();
        Object dataColumnKey = getDataColumnKey();
        Object newPivotColumnKey = pivotColumnKey;

        KeyedColumnTableModel sourceModel = getSourceModel();
        for (int i = 0; i < sourceModel.getColumnCount(); i++) {
            Object columnKey = sourceModel.getColumnKey(i);
            if (!Utilities.equals(columnKey, pivotColumnKey) && !Utilities.equals(columnKey, dataColumnKey) && getSuitablePivotColumnFilter().isValidPivotColumn(columnKey)) {
                newPivotColumnKey = columnKey;
                break;
            }
        }

        if (!Utilities.equals(newPivotColumnKey, pivotColumnKey)) {
            setPivotColumnKey(newPivotColumnKey);
        }
    }

    private class TableModelEventHandler implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            int firstRow = e.getFirstRow();
            int type = e.getType();

            // If table structure changed,
            if (firstRow == TableModelEvent.HEADER_ROW) {
                setModelInvalid();
                fireTableStructureChanged();
            } else { //not the header row
                if (isModelInvalid()) {
                    fireTableStructureChanged();
                    return;
                }
                if (type == TableModelEvent.INSERT || type == TableModelEvent.DELETE) {
                    handleRowChange(e);
                } else {
                    //an update
                    //possible actions:
                    //update affects pivot column => either fire some cell changes, or maybe a column addition or removal
                    //else => map column indexes and refire

                    //if plain event
                    if (!(e instanceof CellsInColumnUpdatedEvent) && !(e instanceof MultipleColumnChangeEvent)) {
                        //
                        if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                            setModelInvalid();
                            fireTableStructureChanged();
                        } else if (e.getColumn() == getPivotColumnIndex()) {
                            setModelInvalid();
                            fireTableStructureChanged();
                        } else if (e.getColumn() == getDataColumnIndex()) {
                            updateTotalColumn(e);
                            fireTableDataChanged();
                        } else {
                            int newColIndex = mapUnderlyingToModel(e.getColumn());
                            TableModelEvent newEvent = new TableModelEvent(PivotTableModel.this, e.getFirstRow(), e.getLastRow(), newColIndex);
                            fireTableChanged(newEvent);
                        }
                        return;
                    }
                    boolean sameRowsForEachColumn = true;
                    if (e instanceof MultipleColumnChangeEvent) {
                        sameRowsForEachColumn = ((MultipleColumnChangeEvent) e).isChangedRowsSameForEachColumn();
                    }

                    ColumnEventIterator iterator = new ColumnEventIterator(e);

                    boolean updatedDataColumn = false;
                    boolean updatedPivotColumn = false;

                    ArrayList dataChangeEvents = new ArrayList(iterator.size() + 10);
                    ArrayList columnsToAdd = new ArrayList();
                    ArrayList columnsToRemove = new ArrayList();
                    while (iterator.hasNext()) {
                        CellsInColumnUpdatedEvent cellsEvent = (CellsInColumnUpdatedEvent) iterator.next();
                        if (cellsEvent.getColumn() == getPivotColumnIndex()) {
                            //if there is only one CellsInColumnChangedEvent, then it is safe to simply
                            //add the new column and fire a column insert event - otherwise recalculate
                            recalculateIfInvalid();
                            if (iterator.size() > 1 && ! pivotValues.contains(cellsEvent.getNewValue())) {
                                if (log.isDebug())log.debug("firing structure change due to new column");
                                setModelInvalid();
                                fireTableStructureChanged();
                                return;
                            } else if (hasValidDataColumn()) {
                                if (updatedDataColumn) {
                                    //both data and pivot changed, abort
                                    setModelInvalid();
                                    fireTableStructureChanged();
                                    return;
                                }
                                updatedPivotColumn = true;
                                boolean success = generateEventsForPivotChange(cellsEvent, dataChangeEvents, columnsToAdd, columnsToRemove);
                                if (!success) {
//could not represent as simple CellsChangedEvent per column, do all data changed
                                    setModelInvalid();
                                    fireTableStructureChanged();
                                    return;
                                }
                            }
                        } else if (cellsEvent.getColumn() == getDataColumnIndex()) {
                            if (hasValidPivotColumn()) {
                                updateTotalColumn(cellsEvent);
                                if (updatedPivotColumn) {
                                    //both data and pivot changed, fire data change because we will not be able to represent as single CellsChangedEvent per column
                                    fireTableDataChanged();
                                    return;
                                }
                                updatedDataColumn = true;
                                boolean success = generateEventsForDataChange(cellsEvent, dataChangeEvents);
                                if (!success) {//could not represent as simple CellsChangedEvents, do full monty
                                    fireTableDataChanged();
                                    return;
                                }
                            }
                        } else {
                            int mappedIndex = mapUnderlyingToModel(cellsEvent.getColumn());
                            dataChangeEvents.add(new CellsInColumnUpdatedEvent(PivotTableModel.this, cellsEvent.getRows(), mappedIndex, cellsEvent));
                        }
                    }
                    fireDataChangeEvents(dataChangeEvents, sameRowsForEachColumn);
                    removeColsAndFireEvent(columnsToRemove);
                }
            }
        }

        private void fireDataChangeEvents(ArrayList dataChangeEvents, boolean sameRowsForEachColumn) {
            TableModelEvent newEvent;
            if (dataChangeEvents.size() == 0) {
                return;
            } else if (dataChangeEvents.size() == 1) {
                newEvent = (TableModelEvent) dataChangeEvents.get(0);
            } else {
                CellsInColumnUpdatedEvent[] newEvents = new CellsInColumnUpdatedEvent[dataChangeEvents.size()];
                dataChangeEvents.toArray(newEvents);
                newEvent = new MultipleColumnChangeEvent(PivotTableModel.this, newEvents, sameRowsForEachColumn);
            }
            fireTableChanged(newEvent);
        }

        private boolean generateEventsForDataChange(CellsInColumnUpdatedEvent cellsEvent, List generatedEvents) {
            if (cellsEvent.getRowCount() > 1) {
                return false;
            } else {
                int rowIndex = cellsEvent.getRowIndex(0);
                Object pivotKey = getSourceModel().getValueAt(rowIndex, getPivotColumnIndex());
                int mappedColumnIndex = getColumnIndexForPivotKey(pivotKey);
                generatedEvents.add(new CellsInColumnUpdatedEvent(PivotTableModel.this, rowIndex, mappedColumnIndex, cellsEvent));
                int totalColumnIndex = getTotalColumnIndex();
                if (totalColumnIndex >= 0) {
                    generatedEvents.add(new CellsInColumnUpdatedEvent(PivotTableModel.this, rowIndex, totalColumnIndex, cellsEvent));
                }
            }
            return true;
        }

        /**
         * Handles change of the pivoted column's data.
         * First applies changes to the model and only at the end fires the events.
         *
         * @param cellsEvent
         * @return null if cannot represent as simple CellsChangedEvents, otherwise
         */
        private boolean generateEventsForPivotChange(CellsInColumnUpdatedEvent cellsEvent, List dataChangeEvents, List columnsToAdd, List columnsToRemove) {
            if (hasValidDataColumn()) {
                //fire a value change to null in the old pivot column, and a value change from null in the new pivot column
                int rowIndex = cellsEvent.getRowIndex(0);
                int rowCount = cellsEvent.getRowCount();
                Object oldPivot = cellsEvent.getOldValue();
                Object newPivot = cellsEvent.getNewValue();
                int[] rows = cellsEvent.getRows();

                // add missing pivot column value
                recalculateIfInvalid();
                if (!pivotValues.contains(newPivot)) {
                    if (dataChangeEvents.size() > 0 || columnsToAdd.size() > 0) {
                        // adding a column would cause these events in dataChangeEvents to have invalid column indices
                        // currently only adding single column is supported through this method
                        return false;
                    }
                    columnsToAdd.add(newPivot);
                    addColsAndFireEvent(columnsToAdd);
                }
                changeUsageCount(newPivot, rowCount);

                pivotValuePerRow.set(rowIndex, newPivot); // first set only the first row value - rest is set in the loop bellow

                // remove unused pivot column value
                int oldUsageCount = changeUsageCount(oldPivot, -rowCount);
                if (oldUsageCount <= 0) {
                    columnsToRemove.add(oldPivot);
                }

                Object dataPoint = getSourceModel().getValueAt(rowIndex, getDataColumnIndex());
                //check that all other rows experiencing this change have the same data values, otherwise the resulting events would be
                //too complex to represent with our existing event structures.
                boolean sharedChanges = true;
                for (int i = 1; i < rows.length; i++) {
                    rowIndex = rows[i];
                    Object oldStoredPivot = pivotValuePerRow.set(rowIndex, newPivot);
                    assert (Utilities.equals(oldStoredPivot, oldPivot)) : "Bug, old stored pivot value did not match value given by event";

                    Object value = getSourceModel().getValueAt(rowIndex, getDataColumnIndex());
                    if (sharedChanges && !Utilities.equals(value, dataPoint)) {
                        sharedChanges = false;
                    }
                }
                if (!sharedChanges) return false;

                int oldColumnIndex = getColumnIndexForPivotKey(oldPivot);
                int newColumnIndex = getColumnIndexForPivotKey(newPivot);
                dataChangeEvents.add(new CellsInColumnUpdatedEvent(PivotTableModel.this, rows, oldColumnIndex, dataPoint, null));
                dataChangeEvents.add(new CellsInColumnUpdatedEvent(PivotTableModel.this, rows, newColumnIndex, null, dataPoint));
            }

            return true;
        }

        private void updateTotalColumn(TableModelEvent e) {
            if (isColumnTotallingEnabled()) {
                int dataColumnIndex = getDataColumnIndex();
                assert (e.getColumn() == dataColumnIndex) : "Illegal argument, event " + e + " was for column " + e.getColumn() + ", expected: " + dataColumnIndex;

                if (e instanceof CellsInColumnUpdatedEvent) {
                    CellsInColumnUpdatedEvent event = ((CellsInColumnUpdatedEvent) e);
                    int count = event.getRowCount();
                    for (int i = 0; i < count; i++) {
                        int index = event.getRowIndex(i);
                        Object value = getSourceModel().getValueAt(index, dataColumnIndex);
                        totalColumn.set(index, value);
                    }
                } else {
                    int last = Math.min(e.getLastRow(), getRowCount());
                    for (int i = e.getFirstRow(); i < last; i++) {
                        Object value = getSourceModel().getValueAt(i, dataColumnIndex);
                        totalColumn.set(i, value);
                    }
                }
            }
        }

        private void handleRowChange(TableModelEvent evt) {
            int eventRow = evt.getFirstRow();
            if (evt.getLastRow() != eventRow) {
                //not a one-row change, could have multiple new generated columns
                setModelInvalid();
                fireTableStructureChanged();
            } else {
                recalculateIfInvalid();
                if (evt.getType() == TableModelEvent.INSERT) {
                    Object pivotKey = getSourceModel().getValueAt(eventRow, getPivotColumnIndex());
                    if (!pivotValues.contains(pivotKey)) {
                        //new pivot column, therefore fire structure changed
                        setModelInvalid();
                        fireTableStructureChanged();
                        return;
                    }
                    changeUsageCount(pivotKey, +1);
                    pivotValuePerRow.add(eventRow, pivotKey);
                } else if (evt.getType() == TableModelEvent.DELETE) {
                    Object oldKey = pivotValuePerRow.remove(eventRow);
                    int usageCount = changeUsageCount(oldKey, -1);
                    if (isColumnTotallingEnabled()) {
                        totalColumn.remove(eventRow);
                    }
                    fireTableChanged(evt);

                    if (usageCount <= 0) {
                        //deleted a row and a column, fire stucture change. Can make this better if required
                        int nonGeneratedColumnCount = getNonGeneratedColumnCount();
                        int columnToRemoveIndex = removePivotColumnForValue(oldKey);
                        int unusedColumnIdx = nonGeneratedColumnCount + columnToRemoveIndex;
                        TableModelEvent event = new TableModelEvent(PivotTableModel.this, -1, -1, unusedColumnIdx, TableModelEvent.DELETE);
                        fireTableChanged(event);
                    }
                    return;
                }

                //update the total column and pass the event on
                if (isColumnTotallingEnabled()) {
                    if (evt.getType() == TableModelEvent.DELETE) {
                        totalColumn.remove(eventRow);
                    } else {
                        Object newColumnTotalValue = null;
                        if (hasValidDataColumn())   {
                            newColumnTotalValue = getSourceModel().getValueAt(eventRow, getDataColumnIndex());
                        }
                        totalColumn.add(eventRow, newColumnTotalValue);
                    }
                }
                fireTableChanged(evt);
            }
        }
    }


    private boolean removeUnlockedColumns(List columnsToRemove) {
        boolean structureChanged = false;
        for (Object columnKey : columnsToRemove) {
            if (!pivotValues.isLockedPivotValue(columnKey)) {
                removePivotColumnForValue(columnKey);
                structureChanged = true;
            }
        }
        return structureChanged;
    }

    private void addColsAndFireEvent(List columnsToAdd) {
        if (columnsToAdd.size() == 0) return;
        for ( Object value : columnsToAdd) {
            addPivotColumnForValue(value);
        }
        recalculateIfInvalid();
        fireTableStructureChanged();
    }

    private void removeColsAndFireEvent(List columnsToRemove) {
         if (columnsToRemove.size() == 0) return;
         boolean structureChanged = removeUnlockedColumns(columnsToRemove);
         if ( structureChanged ) {
             recalculateIfInvalid();
             fireTableStructureChanged();
         }
    }

    /**
     * mainly used as a "tag" so that we can distinguish keys for generated columns from keys provided by the underlying model
     * this has to be public so that the xml persistence layer can build them
     */
    public static class GeneratedColumnKey {
        private Object pivotValue;

        public GeneratedColumnKey(Object pivotValue) {
            this.pivotValue = pivotValue;
        }

        /**
         *@Deprecated this is here to support legacy table view configs
         */
        public GeneratedColumnKey(Object underlyingKey, String pivotValue, Integer index) {
            this.pivotValue = pivotValue;
        }

        public String getPivotValue() {
            return pivotValue.toString();
        }

        public String toString() {
            return "GeneratedColumn for pivot value " + pivotValue;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GeneratedColumnKey)) return false;

            final GeneratedColumnKey generatedColumnKey = (GeneratedColumnKey) o;
            return Utilities.equals(pivotValue, generatedColumnKey.pivotValue);
        }

        public int hashCode() {
            int result;
            result = 29 * (pivotValue != null ? pivotValue.hashCode() : 0);
            return result;
        }
    }

    private static class ColumnEventIterator implements Iterator {
        private TableModelEvent event;
        int multiIndex = 0;

        public ColumnEventIterator(TableModelEvent event) {
            this.event = event;
        }

        public boolean hasNext() {
            return multiIndex < size();
        }

        public Object next() {
            if (event instanceof MultipleColumnChangeEvent) {
                return ((MultipleColumnChangeEvent) event).getColumnChanges()[multiIndex++];
            }
            if (multiIndex > 0) throw new NoSuchElementException();
            multiIndex += 1;
            return event;
        }

        public int size() {
            if (event instanceof MultipleColumnChangeEvent) {
                return ((MultipleColumnChangeEvent) event).getColumnChanges().length;
            }
            return 1;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class DefaultDataColumnFilter implements DataColumnFilter {
        public boolean isValidDataColumn(Object columnKey) {
            if (getSourceModel() == null) return false;
            int columnIndex = getSourceModel().getColumnIndex(columnKey);
            if (columnIndex < 0) {
                return true;
            }
            Class columnClass = getSourceModel().getColumnClass(columnIndex);
            return Number.class.isAssignableFrom(columnClass) ||
                    Quantity.class.isAssignableFrom(columnClass);
        }
    }

    private class DefaultPivotColumnFilter implements PivotColumnFilter {
        public boolean isValidPivotColumn(Object columnKey) {
            return true;    //anything will do
        }
    }

    public static interface DataColumnFilter {
        public boolean isValidDataColumn(Object columnKey);
    }

    public static interface PivotColumnFilter {
        public boolean isValidPivotColumn(Object columnKey);
    }


    /**
     * This is passed to a GeneratedColumnKey to distinguish special pivot columns from auto generated pivot columns
     * e.g. so the special 'Total' column can be distinguished from an autogenerated 'Total' column, although
     * both have the same String representation
     */
    public static class SpecialPivotValue implements Serializable {

        private String pivotValue;

        public SpecialPivotValue(String pivotValue) {
            this.pivotValue = pivotValue;
        }

        public String getPivotValue() {
            return pivotValue;
        }

        public String toString() {
            return pivotValue;
        }

        public int hashCode() {
            return pivotValue.hashCode();
        }

        public boolean equals(Object o) {
            if ( o == this ) return true;
            return o instanceof SpecialPivotValue && pivotValue.equals(((SpecialPivotValue) o).pivotValue);
        }
    }


}
