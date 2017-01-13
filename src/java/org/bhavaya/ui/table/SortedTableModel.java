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

import org.bhavaya.ui.series.DateSeriesNew;
import org.bhavaya.ui.table.formula.FormulaResult;
import org.bhavaya.ui.table.formula.FormulaSumBucket;
import org.bhavaya.ui.table.formula.FormulaSumTotalBucket;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;

/**
 * A sorter for TableModels. The sorter has a model (conforming to TableModel)
 * and itself implements TableModel. SortedTableModel does not store or copy
 * the data in the TableModel, instead it maintains an array of
 * integers which it keeps the same size as the number of modelToRows in its
 * model. When the model changes it notifies the sorter that something
 * has changed so that its internal array of integers can be reallocated
 * and the modelToRows can be resorted. As requests are made of the sorter (like
 * getValueAt(row, col) it redirects them to its model via the mapping
 * array. That way the SortedTableModel appears to hold another copy of the table
 * with the modelToRows in a different order.
 *
 * @author Philip Milne
 * @version $Revision: 1.21 $
 */
public class SortedTableModel extends AbstractTableModel implements TabularBeanAssociation, ChainableTableModel, FilterFindTableModel {
    private static final Log log = Log.getCategory(SortedTableModel.class);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static final int SORT_ON_SINGLE_CLICK = 0;
    public static final int SORT_ON_DOUBLE_CLICK = 2;
    public static final int SORT_ON_DISABLED_CLICK = 3;

    private static int sortingChangeOption;
    private TableHeaderMouseHandler tableHeaderMouseHandler;
    private static ExclusionColumnCalculator exclusionColumnCalculator = ExclusionColumnCalculator.getInstance();

    public static void setSortingChangeOption(int sortingChangeOption) {
        SortedTableModel.sortingChangeOption = sortingChangeOption;
    }

    public static int getSortingChangeOption() {
        return sortingChangeOption;
    }

    protected KeyedColumnTableModel tableModel;
    //the same as tableModel or null if our wrapped tableModel doesn't implements FilterFindTableModel, to prevent multiple instanceof/casts
    private FilterFindTableModel filterFindTableModel;

    private boolean modelValid = false;
    private ArrayList modelToRows = new ArrayList();
    private ArrayList underlyingToRows = new ArrayList();

    private TableModelListener tableModelListener;
    private LinkedHashMap sortingColumns = new LinkedHashMap();
    private ArrayList exclusionColumns = new ArrayList();

    private boolean rowTotallingEnabled = false;
    private GroupedRowManager totalRowManager;
    private GroupedRowManager.GroupedRow totalRow;

    private HashSet tableHeaders = new HashSet();

    public SortedTableModel() {
        //this.mouseListener = new HeaderMouseHandler();
        this.tableModelListener = new TableUpdateHandler();
        totalRowManager = new GroupedRowManager(null, new GroupedRowManager.CellChangeListener() {
            public void cellChanged(GroupedRowManager.GroupedRow row, int column, Object oldValue, Object newValue) {
                if (isRowTotallingEnabled()) {
                    fireTableChanged(new CellsInColumnUpdatedEvent(SortedTableModel.this, getSourceModel().getRowCount(), column, oldValue, newValue));
                }
            }
        });

        GroupedRowManager.BucketFactory bucketFactory = getTotalRowBucketFactory();
        if (bucketFactory instanceof GroupedRowManager.ColumnClassBucketFactory) {
            GroupedRowManager.ColumnClassBucketFactory columnClassBucketFactory = (GroupedRowManager.ColumnClassBucketFactory) bucketFactory;
            columnClassBucketFactory.setColumnClassToBucket(Object.class, TotalBucket.class);
            columnClassBucketFactory.setColumnClassToBucket(String.class, TotalBucket.class);
            columnClassBucketFactory.setColumnClassToBucket(DateFunctionInterval.class, DateTotalBucket.class);
            columnClassBucketFactory.setColumnClassToBucket(Integer.class, IntegerSumBucket.class);
            columnClassBucketFactory.setColumnClassToBucket(Double.class, DoubleSumBucket.class);
            columnClassBucketFactory.setColumnClassToBucket(Quantity.class, QuantitySumBucket.class);
            columnClassBucketFactory.setColumnClassToBucket(FormulaResult.class, FormulaSumTotalBucket.class);
        }

    }

    private GroupedRowManager.BucketFactory getTotalRowBucketFactory() {
        return totalRowManager.getBucketFactory();
    }

    public SortedTableModel(KeyedColumnTableModel tableModel) {
        this();
        setSourceModel(tableModel);
    }

    public Map getSortingColumns() {
        return copySortingColumns(sortingColumns);
    }

    public static Map copySortingColumns(Map sortingColumns) {
        Map newSortingColumns = new LinkedHashMap();
        for (Iterator iterator = sortingColumns.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object value = entry.getValue();
            value = migrateValue(value);
            newSortingColumns.put(entry.getKey(), value);
        }
        return newSortingColumns;
    }

    /**
     * note, the iteration order of the given map is significant.
     * The sorting priority will follow the iteration order of the map (so you probably want to use a linkedHashMap)
     * @param newSortingColumns
     */
    public void setSortingColumns(Map newSortingColumns) {
        this.sortingColumns.clear();

        for (Iterator iterator = newSortingColumns.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object columnKey = entry.getKey();

            //check it is a valid columnKey
            int column = getSourceModel().getColumnIndex(columnKey);
            if (column >= 0) {
                //get the identity equals key
                columnKey = getSourceModel().getColumnKey(column);

                // Make sure its not an exclusion column
                if ( isExclusionColumnKey(columnKey)) continue;

                if (log.isDebug()) log.debug("sortingColumnKey = " + columnKey);
                if (log.isDebug()) log.debug("sortingColumn = " + column);

                Object value = entry.getValue();
                value = migrateValue(value);
                this.sortingColumns.put(columnKey, value);
            }
        }

        fireTableDataChanged();
    }

    private static SortingModel migrateValue(Object value) {
        // a config migration on config version 76
        SortingModel sortingModel;
        if (value instanceof Boolean) {
            Boolean aBoolean = (Boolean) value;
            sortingModel = new SortingModel(aBoolean.booleanValue());
        } else {
            sortingModel = (SortingModel) ((SortingModel) value).clone();
        }
        return sortingModel;
    }

    @Deprecated //use the config mechanism to specify exclusion columns instead
    public java.util.List getExclusionColumns() {
        return exclusionColumns;
    }

    @Deprecated //use the config mechanism to specify exclusion columns instead
    public void setExclusionColumns(java.util.List exclusionColumns) {
        this.exclusionColumns.clear();

        for (Iterator iterator = exclusionColumns.iterator(); iterator.hasNext();) {
            this.exclusionColumns.add(iterator.next());
        }

        fireTableDataChanged();
    }

    public Object[] getBeansForLocation(int row, int column) {
        if (tableModel instanceof TabularBeanAssociation) {
            sort(); //ensure model is valid
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) tableModel;

            if (isRowTotallingEnabled() && row == tableModel.getRowCount()) {
                ArrayList returnedBeans = new ArrayList();
                for (int i = 0; i < totalRow.getSourceRowCount(); i++) {
                    int sourceRowIndex = totalRow.getSourceRowIndex(i);
                    returnedBeans.addAll(Arrays.asList(tabularBeanAssociation.getBeansForLocation(sourceRowIndex, column)));
                }
                return returnedBeans.toArray();
            }
            return tabularBeanAssociation.getBeansForLocation(mapModelToUnderlying(row), column);
        } else {
            return EMPTY_OBJECT_ARRAY;
        }
    }

    public boolean isSingleBean(int row, int column) {
        if (tableModel instanceof TabularBeanAssociation) {
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) tableModel;
            if (isRowTotallingEnabled() && row == tableModel.getRowCount()) {
                return false;
            } else {
                return tabularBeanAssociation.isSingleBean(row, column);
            }
        }
        return false;
    }

    public int getColumnIndex(Object columnKey) {
        KeyedColumnTableModel sourceModel = getSourceModel();
        if (sourceModel == null) return -1;
        return sourceModel.getColumnIndex(columnKey);
    }

    public Object getColumnKey(int column) {
        return getSourceModel().getColumnKey(column);
    }

    public void setSourceModel(KeyedColumnTableModel tableModel) {
        if (this.tableModel != null) {
            this.tableModel.removeTableModelListener(tableModelListener);
        }

        this.tableModel = tableModel;
        this.filterFindTableModel = tableModel instanceof FilterFindTableModel ? (FilterFindTableModel)tableModel : null;

        totalRowManager.setTableModel(tableModel);
        totalRow = null;

        if (this.tableModel != null) {
            this.tableModel.addTableModelListener(tableModelListener);
            if (isRowTotallingEnabled()) {
                totalRow = totalRowManager.createGroupedRow();
            }
        }

        invalidateModel();
        fireTableStructureChanged();
    }

    private void invalidateModel() {
        modelValid = false;
        modelToRows.clear();
        underlyingToRows.clear();
        totalRowManager.clearAll();
    }

    public boolean isExclusionColumnKey(Object columnKey) {
        return columnKey instanceof String &&
                ( exclusionColumns.contains(columnKey) ||
                  exclusionColumnCalculator.isExcluded((String)columnKey) );
    }

    public KeyedColumnTableModel getSourceModel() {
        return tableModel;
    }

    public TableCellRenderer getTableHeaderRenderer(JTableHeader tableHeader) {
        tableHeaders.add(tableHeader);
        return new SortableHeaderRenderer(tableHeader.getDefaultRenderer());
    }

    public MouseListener getClickSortMouseHandler() {
        if (tableHeaderMouseHandler == null) {
            tableHeaderMouseHandler = new TableHeaderMouseHandler();
        }
        return tableHeaderMouseHandler;
    }

    private final void reallocateRows() {
        int tableModelRowCount = tableModel.getRowCount();

        modelToRows.clear();
        modelToRows.ensureCapacity(tableModelRowCount);
        totalRowManager.clearAll();

        totalRow = totalRowManager.createGroupedRow();
        for (int row = 0; row < tableModelRowCount; row++) {
            modelToRows.add(new Row(row));
            if (isRowTotallingEnabled()) {
                totalRowManager.addSourceRowToGroup(totalRow, row, false);
            }
        }
    }

    private final void sort() {
        if (!isModelValid()) {
            modelValid = true;

            if (tableModel == null) {
                return;
            }
            reallocateRows();

            if (isSorting()) {
                Utilities.sort(modelToRows);
            }

            Object[] underlyingRowsArray = new Row[modelToRows.size()];
            for (int i = 0; i < modelToRows.size(); i++) {
                Row row = (Row) modelToRows.get(i);
                row.setModelIndex(i);
                underlyingRowsArray[row.getUnderlyingIndex()] = row;
            }
            underlyingToRows.clear();
            underlyingToRows.addAll(Arrays.asList(underlyingRowsArray));
//        fireTableRowsUpdated(0, modelToRows.size() - 1);
        }
    }

    private boolean isModelValid() {
        return modelValid;
    }


    private ArrayList getModelToRows() {
        sort();
        return modelToRows;
    }

    private ArrayList getUnderlyingToRows() {
        assert (isModelValid()) : "It should not be possible tot call getInverse modelToRows while the model is invalid";
        return underlyingToRows;
    }

    public void addSortingColumn(Object columnKey, boolean descending) {
        addSortingColumn(columnKey, descending, null);
    }

    public void addSortingColumn(Object columnKey, boolean descending, Comparator comparator) {
        if (isExclusionColumnKey(columnKey)) return;
        if (getSourceModel().getColumnIndex(columnKey) >= 0) {
            if (log.isDebug()) log.debug("Adding sorting to column: " + columnKey);
            sortingColumns.put(columnKey, new SortingModel(descending, comparator));
            forceUpdate();
        }
    }

    public void removeSortingColumn(Object columnKey) {
        if (log.isDebug()) log.debug("Removing sorting from column: " + columnKey);
        sortingColumns.remove(columnKey);
        forceUpdate();
    }

    public boolean isSorting() {
        return sortingColumns.size() != 0;
    }

    public boolean isSortingColumn(Object columnKey) {
        return sortingColumns.containsKey(columnKey);
    }

    public void cancelSorting() {
        if (log.isDebug()) log.debug("Cancel all sorting");
        sortingColumns.clear();
        forceUpdate();
    }

    public void toggleSortingDirection(Object columnKey) {
        if (!isSortingColumn(columnKey)) {
            addSortingColumn(columnKey, true);
        } else {
            SortingModel sortingModel = (SortingModel) sortingColumns.get(columnKey);
            sortingModel.setDescending(!sortingModel.isDescending());
        }
        forceUpdate();
    }

    public boolean isSortDescendingColumn(Object columnKey) {
        return ((SortingModel) sortingColumns.get(columnKey)).isDescending();
    }

    private void forceUpdate() {
        invalidateModel();
        fireTableDataChanged();
        for (Iterator iterator = tableHeaders.iterator(); iterator.hasNext();) {
            JTableHeader tableHeader = (JTableHeader) iterator.next();
            tableHeader.repaint();
        }
    }

//------ TableModel interface -------

    public int getRowCount() {
        KeyedColumnTableModel sourceModel = getSourceModel();
        if (sourceModel == null) return 0;

        int rowCount = sourceModel.getRowCount();
        return isRowTotallingEnabled() ? rowCount + 1 : rowCount;
    }

    public int getColumnCount() {
        return (tableModel == null) ? 0 : tableModel.getColumnCount();
    }

    public String getColumnName(int column) {
        return tableModel.getColumnName(column);
    }

    public Class getColumnClass(int column) {
        return tableModel.getColumnClass(column);
    }

    public int mapModelToUnderlying(int rowIndex) {
        sort();
        return ((Row) getModelToRows().get(rowIndex)).getUnderlyingIndex();
    }

    public int mapUnderlyingToModel(int rowIndex) {
        sort();
        return ((Row) getUnderlyingToRows().get(rowIndex)).getModelIndex();
    }

    public boolean isCellEditable(int row, int column) {
        if (isRowTotallingEnabled() && row == tableModel.getRowCount()) {
            return false;
        }
        return tableModel.isCellEditable(mapModelToUnderlying(row), column);
    }

    public Object getValueAt(int row, int column) {
        if (isRowTotallingEnabled() && row == getModelToRows().size()) {
            return totalRow.getCellValue(column);
        }
        return tableModel.getValueAt(mapModelToUnderlying(row), column);
    }

    public void setValueAt(Object aValue, int row, int column) {
        tableModel.setValueAt(aValue, mapModelToUnderlying(row), column);
    }

    public void setRowTotallingEnabled(boolean rowTotallingEnabled) {
        this.rowTotallingEnabled = rowTotallingEnabled;
        forceUpdate();
    }

    public boolean isRowTotallingEnabled() {
        return rowTotallingEnabled;
    }

    public boolean isCellMatchingSearch(int rowIndex, int colIndex) {
        boolean isTotalRow = isRowTotallingEnabled() && rowIndex == tableModel.getRowCount();
        return ! isTotalRow && filterFindTableModel != null && filterFindTableModel.isCellMatchingSearch(
            mapModelToUnderlying(rowIndex), colIndex
        );
    }

    private class Row implements Comparable {
        private int underlyingIndex;
        private int modelIndex = -1;

        public int getUnderlyingIndex() {
            return underlyingIndex;
        }

        public void setUnderlyingIndex(int underlyingIndex) {
            this.underlyingIndex = underlyingIndex;
        }

        public int getModelIndex() {
            return modelIndex;
        }

        public void setModelIndex(int modelIndex) {
            this.modelIndex = modelIndex;
        }

        public Row(int underyingIndex) {
            this.underlyingIndex = underyingIndex;
        }

        public int compareTo(Object o) {
            int row1 = underlyingIndex;
            int row2 = ((Row) o).underlyingIndex;

            for (Iterator iterator = sortingColumns.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                int column = getSourceModel().getColumnIndex(entry.getKey());
                SortingModel sortingModel = (SortingModel) entry.getValue();

                int comparison = 0;

                if (column >= 0 && column < tableModel.getColumnCount()) {
                    Object o1 = tableModel.getValueAt(row1, column);
                    Object o2 = tableModel.getValueAt(row2, column);
                    try {
                        comparison = sortingModel.getComparator().compare(o1, o2);
                    } catch (ClassCastException e) {
                        log.error("Class cast while comparing: " + o1 + " with " + o2, e);
                    }
                }

                if (comparison != 0) {
                    return sortingModel.isDescending() ? comparison * -1 : comparison;
                }
            }

            return 0;
        }

    }

    private class TableUpdateHandler implements TableModelListener {
        private int[] ALL_ROWS = new int[0];
        private final ModelIndexRowComparator MODEL_INDEX_ROW_COMPARATOR = new ModelIndexRowComparator();

        public void tableChanged(TableModelEvent e) {
            int firstRow = e.getFirstRow();
            int lastRow = e.getLastRow();
            int column = e.getColumn();
            int type = e.getType();

            // We check to see if modelToRows are allocated to allow the batching up multiple single cell updates
            if (!isModelValid()) {
                //not possible to be specific about how this change affected us
                if (firstRow == TableModelEvent.HEADER_ROW) {
                    fireTableStructureChanged();
                } else {
                    fireTableDataChanged();
                }
                return;
            }


            // If the table changed structure (row == TableModelEvent.HEADER_ROW)
            // find out how, and behave appropriately
            if (firstRow == TableModelEvent.HEADER_ROW) {
                if (column == TableModelEvent.ALL_COLUMNS) {    //structure change
                    invalidateModel();
                } else if (type == TableModelEvent.DELETE) {
                    if (isRowTotallingEnabled()) {
                        totalRowManager.columnRemoved(column);
                    }
                    columnRemoved();
                } else if (type == TableModelEvent.UPDATE) {
                    //remove sorting columns that are invalid
                    Object columnKey = getSourceModel().getColumnKey(column);
                    if (sortingColumns.containsKey(columnKey)) {
                        sortingColumns.remove(columnKey);
                        invalidateModel();    //go for a re-sort next time someone does a getValueAt
                    } else {
                        if (isRowTotallingEnabled()) {
                            totalRowManager.columnUpdated(column);
                        }
                    }
                } else if (type == TableModelEvent.INSERT) {
                    if (isRowTotallingEnabled()) {
                        totalRowManager.columnInserted(column);
                    }
                }

                fireTableChanged(e);
                return;
            } else {
                //data changed
                if (type == TableModelEvent.INSERT || type == TableModelEvent.DELETE) {
                    if (firstRow == lastRow) {
                        switch (type) {
                            case TableModelEvent.INSERT:
                                handleInsert(firstRow);
                                break;
                            case TableModelEvent.DELETE:
                                handleDelete(firstRow);
                                break;
                        }
                    } else {
                        forceUpdate();
                    }
                } else {
                    //an update
                    //possible actions:
                    //update affects column that is sorted => remove affected rows + re-add them
                    //else => map new row indexes and refire
                    int[] rows = getRowsNeedingResort(e);
                    if (rows == ALL_ROWS) {
                        forceUpdate();
                        return;
                    } else if (rows == null) {
                        refireChange(e);
                    } else {
                        rowsNeedMoving(rows);
                    }
                    updateTotalRow(e);

                }
            }
        }

        private void updateTotalRow(TableModelEvent e) {
            //update total row
            if (isRowTotallingEnabled()) {
                if (e instanceof CellsInColumnUpdatedEvent) {
                    CellsInColumnUpdatedEvent columnEvent = (CellsInColumnUpdatedEvent) e;
                    boolean success = totalRow.updateCellMemberValue(columnEvent.getColumn(), columnEvent.getOldValue(), columnEvent.getNewValue(), columnEvent.getRowCount());
                    if (!success) {
                        totalRow.recalcBucket(e.getColumn());
                        return;
                    }
                    return;
                } else if (e instanceof MultipleColumnChangeEvent) {
                    MultipleColumnChangeEvent event = (MultipleColumnChangeEvent) e;
                    CellsInColumnUpdatedEvent[] columnChanges = event.getColumnChanges();
                    for (int i = 0; i < columnChanges.length; i++) {
                        CellsInColumnUpdatedEvent columnEvent = columnChanges[i];
                        boolean success = totalRow.updateCellMemberValue(columnEvent.getColumn(), columnEvent.getOldValue(), columnEvent.getNewValue(), columnEvent.getRowCount());
                        if (!success) {
                            totalRow.recalcBucket(columnEvent.getColumn());
                        }
                    }
                } else {
                    int column = e.getColumn();
                    if (column >= 0) {
                        totalRow.recalcBucket(column);
                    } else {
                        for (int i = 0; i < getColumnCount(); i++) {
                            totalRow.recalcBucket(i);
                        }
                    }
                }
            }
        }

        private int[] getRowsNeedingResort(TableModelEvent e) {
            if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                if (e instanceof MultipleColumnChangeEvent) {
                    MultipleColumnChangeEvent event = (MultipleColumnChangeEvent) e;
                    CellsInColumnUpdatedEvent[] columnChanges = event.getColumnChanges();
                    for (int i = 0; i < columnChanges.length; i++) {
                        if (isSortingColumn(getColumnKey(columnChanges[i].getColumn()))) {
                            //get rows
                            if (event.isChangedRowsSameForEachColumn() || sortingColumns.size() == 1) {
                                return columnChanges[0].getRows();
                            } else {
                                //todo: maybe we should efficiently find the set of rows that are effected?
                                return ALL_ROWS;
                            }
                        }
                    }
                } else {
                    return ALL_ROWS;
                }
            } else if (isSortingColumn(getColumnKey(e.getColumn()))) {
                if (e instanceof CellsInColumnUpdatedEvent) {
                    return ((CellsInColumnUpdatedEvent) e).getRows();
                } else if (e.getFirstRow() == e.getLastRow()) {
                    return new int[]{e.getFirstRow()};
                } else {
                    return ALL_ROWS;
                }
            }
            return null;
        }

        private int[] mapRows(int[] rows) {
            int[] mapped = new int[rows.length];
            for (int i = 0; i < rows.length; i++) {
                mapped[i] = mapUnderlyingToModel(rows[i]);
            }
            return mapped;
        }

        /**
         * remap the rows and refire the event
         */
        private void refireChange(TableModelEvent e) {
            TableModelEvent newEvent = e;
            if (isSorting()) {
                if (e instanceof CellsInColumnUpdatedEvent) {
                    newEvent = mapEvent(((CellsInColumnUpdatedEvent) e));
                } else if (e instanceof MultipleColumnChangeEvent) {
                    newEvent = mapEvent((MultipleColumnChangeEvent) e);
                } else {
                    int first = 0;
                    int last = Integer.MAX_VALUE;
                    if (e.getFirstRow() == e.getLastRow()) {
                        first = mapUnderlyingToModel(e.getFirstRow());
                        last = first;
                    }
                    newEvent = new TableModelEvent(SortedTableModel.this, first, last, e.getColumn());
                }
            }
            fireTableChanged(newEvent);
        }

        private MultipleColumnChangeEvent mapEvent(MultipleColumnChangeEvent event) {
            CellsInColumnUpdatedEvent[] columnChanges = event.getColumnChanges();
            CellsInColumnUpdatedEvent[] mappedColumnChanges = new CellsInColumnUpdatedEvent[columnChanges.length];
            boolean sharedRows = event.isChangedRowsSameForEachColumn();

//            int mappedRow = 0;
            int[] mappedRows = null;
            for (int i = 0; i < columnChanges.length; i++) {
                CellsInColumnUpdatedEvent columnChange = columnChanges[i];
                if (sharedRows || i == 0) {
                    mappedRows = mapRows(columnChange.getRows());
                }
                CellsInColumnUpdatedEvent newColumnChange;
                newColumnChange = new CellsInColumnUpdatedEvent(SortedTableModel.this, mappedRows, columnChange.getColumn(), columnChange);
                mappedColumnChanges[i] = newColumnChange;
            }

            return new MultipleColumnChangeEvent(SortedTableModel.this, mappedColumnChanges, sharedRows);
        }

        private CellsInColumnUpdatedEvent mapEvent(CellsInColumnUpdatedEvent event) {
            CellsInColumnUpdatedEvent newEvent;
            int[] mappedRows = mapRows(event.getRows());
            newEvent = new CellsInColumnUpdatedEvent(SortedTableModel.this, mappedRows, event.getColumn(), event);
            return newEvent;
        }

        private void handleDelete(int row) {
            ArrayList underlyingToRows1 = getUnderlyingToRows();
            Row deletedRow = (Row) underlyingToRows1.remove(row);
            for (int i = row; i < underlyingToRows1.size(); i++) {
                Row row1 = (Row) underlyingToRows1.get(i);
                row1.setUnderlyingIndex(i);
            }

            int modelIndex = deletedRow.getModelIndex();
            modelToRows.remove(modelIndex);
            for (int i = modelIndex; i < modelToRows.size(); i++) {
                Row row1 = (Row) modelToRows.get(i);
                row1.setModelIndex(i);
            }
            fireTableRowsDeleted(modelIndex, modelIndex);
            if (isRowTotallingEnabled()) {
                totalRowManager.sourceRowRemoved(row);
            }
        }

        private void handleInsert(int rowIndex) {
            if (isRowTotallingEnabled()) {
                totalRowManager.sourceRowInserted(rowIndex);
            }
            Row newRow = new Row(rowIndex);

            int insertPoint = rowIndex;
            if (isSorting()) {
                insertPoint = Collections.binarySearch(modelToRows, newRow);
                if (insertPoint < 0) insertPoint = (-insertPoint) - 1;
            }
            newRow.setModelIndex(insertPoint);

            if (insertPoint > modelToRows.size()) {
                log.error("This is parwy's phantom bug: Underlying column count:" + getSourceModel().getColumnCount() + " row count: " + getSourceModel().getRowCount() + " isSorted:" + isSorting() + " insertPoint " + insertPoint + " modelToRows: " + modelToRows.size(), new RuntimeException());
                forceUpdate();
                return;
            }
            modelToRows.add(insertPoint, newRow);
            for (int i = insertPoint + 1; i < modelToRows.size(); i++) {
                Row row = (Row) modelToRows.get(i);
                row.setModelIndex(i);
            }

            ArrayList underlyingToRows = getUnderlyingToRows();
            underlyingToRows.add(rowIndex, newRow);
            for (int i = rowIndex + 1; i < underlyingToRows.size(); i++) {
                Row row = (Row) underlyingToRows.get(i);
                row.setUnderlyingIndex(i);
            }

            fireTableRowsInserted(insertPoint, insertPoint);

            if (isRowTotallingEnabled()) {
                totalRowManager.addSourceRowToGroup(totalRow, rowIndex, true);
            }
        }

        private void rowsNeedMoving(int[] rows) {
            if (isSorting()) {
                Row[] changedRows = new Row[rows.length];
                for (int i = 0; i < rows.length; i++) {
                    int underlyingIndex = rows[i];
                    changedRows[i] = (Row) underlyingToRows.get(underlyingIndex);
                }

                Arrays.sort(changedRows, MODEL_INDEX_ROW_COMPARATOR);
                int firstChangedRow = changedRows[0].getModelIndex();
                int firstUntouchedRow = changedRows[changedRows.length - 1].getModelIndex() + 1 - changedRows.length;

                //remove all changed rows (must do this in reverse order to preserve validity of model indexes
                for (int i = changedRows.length - 1; i >= 0; i--) {
                    Row changedRow = changedRows[i];
                    modelToRows.remove(changedRow.getModelIndex());
                }

                //now add them back in the right place
                for (int i = 0; i < changedRows.length; i++) {
                    Row changedRow = changedRows[i];
                    int newIndex = Collections.binarySearch(modelToRows, changedRow);
                    if (newIndex < 0) newIndex = (-newIndex) - 1;

                    modelToRows.add(newIndex, changedRow);
                    firstChangedRow = Math.min(newIndex, firstChangedRow);
                    if (newIndex <= firstUntouchedRow) {
                        firstUntouchedRow++;
                    } else {
                        firstUntouchedRow = newIndex + 1;
                    }
                }

                //now upate the row indexes in the range that was changed
                for (int i = firstChangedRow; i < firstUntouchedRow; i++) {
                    Row row = (Row) modelToRows.get(i);
                    row.setModelIndex(i);
                }

                if ((firstUntouchedRow - firstChangedRow) > 0) fireTableDataChanged();
            }
        }

        private void columnRemoved() {
            Iterator sortingColumns = getSortingColumns().keySet().iterator();
            while (sortingColumns.hasNext()) {
                Object key = sortingColumns.next();
                if (getSourceModel().getColumnIndex(key) < 0) {
                    removeSortingColumn(key);
                    return;
                }
            }
        }

        private class ModelIndexRowComparator implements Comparator {
            public int compare(Object o1, Object o2) {
                return ((Row) o1).getModelIndex() - ((Row) o2).getModelIndex();
            }
        }
    }

    private class SortableHeaderRenderer extends DefaultTableCellRenderer {
        private Icon ascendingIcon = ImageIconCache.getImageIcon("sort_ascending.png");
        private Icon descendingIcon = ImageIconCache.getImageIcon("sort_descending.png");
        private JLabel ascendingIconLabel = new JLabel(ascendingIcon);
        private JLabel descendingIconLabel = new JLabel(descendingIcon);
        private TableCellRenderer tableCellRenderer;

        public SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
            this.tableCellRenderer = tableCellRenderer;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JComponent c = (JComponent) tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int columnIndex = table.convertColumnIndexToModel(column);
            Object columnKey = getSourceModel().getColumnKey(columnIndex);

            if (isSortingColumn(columnKey)) {
                boolean sortDescending = isSortDescendingColumn(columnKey);
                Icon icon = sortDescending ? descendingIcon : ascendingIcon;

                if (c instanceof MultiLineHeaderRenderer) {
                    MultiLineHeaderRenderer renderer = (MultiLineHeaderRenderer) c;
                    renderer.setIcon(icon);
                } else if (c instanceof JLabel) {
                    JLabel l = (JLabel) c;
                    l.setIcon(icon);
                } else if (c instanceof JPanel) {
                    log.error("Whoops");
                }
            } else {
                if (c instanceof MultiLineHeaderRenderer) {
                    MultiLineHeaderRenderer renderer = (MultiLineHeaderRenderer) c;
                    renderer.setIcon(null);
                } else if (c instanceof JLabel) {
                    JLabel l = (JLabel) c;
                    l.setIcon(null);
                } else if (c instanceof JPanel) {
                }
            }

            return c;
        }
    }

    private class TableHeaderMouseHandler extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            checkPopup(e);
        }

        private void checkPopup(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && sortingChangeOption == SORT_ON_SINGLE_CLICK
                    && ((KeyEvent.CTRL_DOWN_MASK & e.getModifiersEx()) == 0)) {
                sort(e);
            } else if (e.getClickCount() == 2 && !e.isPopupTrigger() && sortingChangeOption == SORT_ON_DOUBLE_CLICK) {
                sort(e);
            }
        }

        private void sort(MouseEvent e) {
            JTableHeader h = (JTableHeader) e.getSource();
            TableColumnModel columnModel = h.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            if (viewColumn >= 0) {
                int column = columnModel.getColumn(viewColumn).getModelIndex();
                if (column != -1) {
                    boolean controlPressed = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK;

                    Object columnKey = getSourceModel().getColumnKey(column);
                    if (isExclusionColumnKey(columnKey)) return;

                    if (!controlPressed && !isSortingColumn(columnKey)) {
                        cancelSorting();
                    }

                    if (isSortingColumn(columnKey)) {
                        toggleSortingDirection(columnKey);
                    } else {
                        addSortingColumn(columnKey, false);
                    }
                }
            }
        }
    }

    public static class TotalBucket implements GroupedRowManager.ValueBucket {
        public static final Object TOTAL = new Object() {
            public String toString() {
                return "Total";
            }
        };

        public boolean delete(Object value) {
            return true;
        }

        public Object getOutput() {
            return TOTAL;
        }

        public void insert(Object value) {
        }

        public boolean update(Object oldValue, Object newValue) {
            return true;
        }
    }

    public static class DateTotalBucket extends TotalBucket {
        public Object getOutput() {
            return DateSeriesNew.ALL_TIME_INTERVAL;
        }
    }

    static void setExclusionColumnCalculator(ExclusionColumnCalculator exclusionColumnCalculator) {
        SortedTableModel.exclusionColumnCalculator = exclusionColumnCalculator;
    }

    static ExclusionColumnCalculator getExclusionColumnCalculator() {
        return exclusionColumnCalculator;
    }


    public static class SortingModel implements Cloneable {
        private boolean descending;
        private Comparator underlyingComparator;
        private Comparator comparator;

        static {
            BeanUtilities.addPersistenceDelegate(SortingModel.class, new BhavayaPersistenceDelegate(new String[]{"descending", "underlyingComparator"}));
        }

        public SortingModel(boolean descending) {
            this(descending, null);
        }

        public SortingModel(boolean descending, Comparator underlyingComparator) {
            this.descending = descending;
            this.underlyingComparator = underlyingComparator;
            this.comparator = underlyingComparator;
            if (comparator == null) comparator = Utilities.COMPARATOR;
            if (!(comparator instanceof CachedObjectGraph.NotReadyAwareComparator)) comparator = new CachedObjectGraph.NotReadyAwareComparator(comparator);
            comparator = new PartialBucketValue.PartialBucketValueAwareComparator(comparator);
        }

        public boolean isDescending() {
            return descending;
        }

        public void setDescending(boolean descending) {
            this.descending = descending;
        }

        public Comparator getComparator() {
            return comparator;
        }

        public void setComparator(Comparator comparator) {
            this.comparator = comparator;
        }

        public Comparator getUnderlyingComparator() {
            return underlyingComparator;
        }

        public void setUnderlyingComparator(Comparator underlyingComparator) {
            this.underlyingComparator = underlyingComparator;
        }

        public Object clone() {
            return new SortingModel(descending, underlyingComparator);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SortingModel)) return false;

            final SortingModel sortingModel = (SortingModel) o;

            if (descending != sortingModel.descending) return false;
            if (underlyingComparator != null ? !underlyingComparator.equals(sortingModel.underlyingComparator) : sortingModel.underlyingComparator != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (descending ? 1 : 0);
            result = 29 * result + (underlyingComparator != null ? underlyingComparator.hashCode() : 0);
            return result;
        }
    }
}