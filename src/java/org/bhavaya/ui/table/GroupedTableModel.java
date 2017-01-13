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

import org.bhavaya.collection.IndexedSet;
import org.bhavaya.collection.SingleItemSet;
import org.bhavaya.ui.table.formula.FormulaResult;
import org.bhavaya.ui.table.formula.FormulaSumBucket;
import org.bhavaya.util.Log;
import org.bhavaya.util.Quantity;
import org.bhavaya.util.Utilities;
import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.*;

/**
 * Dan's complete re-write of "A brazen hussy of a class that groups, pivots and makes you coffee when you're feeling down"
 * Now only groups, but I thought the comment block was one of brendon's more creative ones and didn't have the heart to
 * "refactor" it.
 *
 * @author Philip Milne
 * @version $Revision: 1.11 $
 * @commentedby Brendon McLean
 * @bestboy Dan Van
 * @guywithgoldedchain Parwy Sekon
 * @pointyhairedboss Malcolm Dick
 * @alsostarring Pamela Anderson as a divorced mother of ten struggling to come to terms with the loss of her dog 'Boris'
 * in a touching story of family, betrayal and angst in medieval Russia.
 */
public class GroupedTableModel extends AbstractTableModel implements TabularBeanAssociation, ChainableTableModel {
    private static final Log log = Log.getCategory(GroupedTableModel.class);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private TableModelListener tableModelListener;

    private KeyedColumnTableModel sourceTableModel;

    private HashMap keyToGroupedRow = new HashMap();
    private List groupedRows = new IndexedSet();

    private GroupedRowManager groupedRowManager;
    private GroupedKeyDefinition groupedKeyDefinition = new NonSumableColumns();

    private Object[][] beansForLocationCache;

    private boolean uncalculated;   //is the grouper currently uncalcuated

    public GroupedTableModel() {
        this.tableModelListener = new TableModelEventHandler();
        setUncalculated();
    }

    /**
     * bit dodgy - so I can change this in an override
     */
    protected GroupedRowManager createGroupedRowManager() {
        return new GroupedRowManager(null, new GroupedRowCellChangeFirer(this));
    }

    public GroupedTableModel(KeyedColumnTableModel underlyingTableModel) {
        this();
        setSourceModel(underlyingTableModel);
    }

    /**
     * Summable data (Quantity for now) is added together in rows which share the same
     * set of unsummable data.  This class generates a <b>key that represents the set
     * of unsummable columns that comprise a row</b>.  This will then be used to identify
     * rows that share the same set of unsummable columns to allow the summable columns
     * to summed.  Apologies for the hopeless explanation.
     *
     * @return a key representing non-summable, non-pivoted columns.
     */
    private Object createKeyForSourceRow(int sourceRow) {
        KeyedColumnTableModel tableModel = getSourceModel();
        List key = new ArrayList();

        for (int column = 0; column < tableModel.getColumnCount(); column++) {
            Object columnKey = tableModel.getColumnKey(column);
            if (getGroupedKeyDefinition().isGroupKeyColumn(columnKey)) {
                Object value = tableModel.getValueAt(sourceRow, column);
                key.add(value);
            }
        }
        return key;
    }

    /**
     * This funky method takes rows with similar keys and groups mergable data.
     */
    protected void groupAll() {
        beansForLocationCache = null;
        keyToGroupedRow.clear();
        groupedRows.clear();
        TableModel tableModel = getSourceModel();

        getGroupedRowManager().clearAll();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            addRowToGroup(row, false);
        }
        uncalculated = false;
    }

    private void addRowToGroup(int row, boolean fireEvents) {
        boolean createdNewRow = false;
        // Gets a key for the set of unsummable data
        Object sourceRowKey = createKeyForSourceRow(row);

        // Checks if there is a row that has the same key
        GroupedRowManager.GroupedRow groupedRow;
        groupedRow = (GroupedRowManager.GroupedRow) keyToGroupedRow.get(sourceRowKey);
        if (groupedRow == null) {
            // No, so add one
            groupedRow = getGroupedRowManager().createGroupedRow();
            beansForLocationCache = null;
            groupedRows.add(groupedRow);
            keyToGroupedRow.put(sourceRowKey, groupedRow);
            createdNewRow = true;
        }

        boolean fireCellChanges = !createdNewRow && fireEvents;
        getGroupedRowManager().addSourceRowToGroup(groupedRow, row, fireCellChanges);

        if (createdNewRow && fireEvents) {
            int groupedRowIndex = groupedRows.size() - 1;
            fireTableRowsInserted(groupedRowIndex, groupedRowIndex);
        }
    }

    public KeyedColumnTableModel getSourceModel() {
        return sourceTableModel;
    }

    public void setSourceModel(KeyedColumnTableModel sourceTableModel) {
        if (this.sourceTableModel != null) {
            this.sourceTableModel.removeTableModelListener(tableModelListener);
        }
        this.sourceTableModel = sourceTableModel;
        if (this.sourceTableModel != null) {
            this.sourceTableModel.addTableModelListener(tableModelListener);
        }
        getGroupedRowManager().setTableModel(sourceTableModel);

        setUncalculated();
        fireTableStructureChanged();
    }

    private boolean isUncalculated() {
        return uncalculated;
    }

    private void setUncalculated() {
        uncalculated = true;
        beansForLocationCache = null;
        keyToGroupedRow.clear();
        groupedRows.clear();
        if (groupedRowManager != null) {
            getGroupedRowManager().clearAll();
        }
    }

    public GroupedKeyDefinition getGroupedKeyDefinition() {
        return groupedKeyDefinition;
    }

    public void setGroupedKeyDefinition(GroupedKeyDefinition groupedKeyDefinition) {
        this.groupedKeyDefinition = groupedKeyDefinition;
        recalcBuckets();
    }

    public void recalcBuckets() {
        setUncalculated();
        fireTableDataChanged();
    }

    public void setBucketFactory(GroupedRowManager.BucketFactory factory) {
        GroupedRowManager.BucketFactory bucketFactory = getGroupedRowManager().getBucketFactory();
        assert (bucketFactory instanceof GroupKeyAwareBucketFactory) : "Somehow the grouped row manager does not have a group key aware bucket factory!!";
        ((GroupKeyAwareBucketFactory) bucketFactory).setActualFactory(factory);
        recalcBuckets();
    }

    /**
     * use setBucketFactory if you want to override behaviour
     */
    public final GroupedRowManager.BucketFactory getBucketFactory() {
        GroupedRowManager.BucketFactory bucketFactory = getGroupedRowManager().getBucketFactory();
        assert (bucketFactory instanceof GroupKeyAwareBucketFactory) : "Somehow the grouped row manager does not have a group key aware bucket factory!!";
        return ((GroupKeyAwareBucketFactory) bucketFactory).getActualFactory();
    }
//---------------- TableModel interface

    protected List getRows() {
        if (isUncalculated()) {
            groupAll();
        }
        return groupedRows;
    }

    public int getRowCount() {
        if (sourceTableModel == null) {
            return 0;
        }
        return getRows().size();
    }

    public int getColumnCount() {
        KeyedColumnTableModel sourceModel = getSourceModel();
        if (sourceModel == null) return 0;
        return sourceModel.getColumnCount();
    }

    public String getColumnName(int column) {
        return getSourceModel().getColumnName(column);
    }

    public Class getColumnClass(int column) {
        return getSourceModel().getColumnClass(column);
    }

    public Object getValueAt(int row, int column) {
        int rowCount = getRowCount();
        int columnCount = getColumnCount();
        if (row >= rowCount || column >= columnCount) {
            Exception e = new IllegalArgumentException("Grouper asked for row: " + row + " col: " + column + ". But table dimensions are: " + rowCount + " by " + columnCount);
            log.error(e);
            return null;    //this is more robust than throwing an exception on the event thread (which would mess up painting)
        }
        GroupedRowManager.GroupedRow groupedRow = (GroupedRowManager.GroupedRow) getRows().get(row);
        return groupedRow.getCellValue(column);
    }

    public int getColumnIndex(Object columnKey) {
        return getSourceModel().getColumnIndex(columnKey);
    }

    public Object getColumnKey(int column) {
        return getSourceModel().getColumnKey(column);
    }

    public Object[] getBeansForLocation(int row, int column) {
        if (beansForLocationCache == null) {
            beansForLocationCache = new Object[getRowCount()][getColumnCount()];
        } else {
            ApplicationDiagnostics.getInstance().productionAssert(row < beansForLocationCache.length, "GroupedTableModel: Row out of bounds");
            ApplicationDiagnostics.getInstance().productionAssert(column < beansForLocationCache[0].length, "GroupedTableModel: Column out of bounds");

            if (row >= beansForLocationCache.length || column >= beansForLocationCache[0].length) {
                // this is a safety net here that can be removed when we don't receive any emails from code above 
                beansForLocationCache = new Object[getRowCount()][getColumnCount()];
            }

            if (beansForLocationCache[row][column] != null) {
                return (Object[]) beansForLocationCache[row][column];
            }
        }

        // Ensure that rows have been allocated
        TableModel tableModel = getSourceModel();

        Object[] beansForLocation = EMPTY_OBJECT_ARRAY;
        if (tableModel instanceof TabularBeanAssociation) {
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) tableModel;
            GroupedRowManager.GroupedRow groupedRow = (GroupedRowManager.GroupedRow) getRows().get(row);

            ArrayList returnedBeans = new ArrayList();
            for (int i = 0; i < groupedRow.getSourceRowCount(); i++) {
                int sourceRowIndex = groupedRow.getSourceRowIndex(i);
                returnedBeans.addAll(Arrays.asList(tabularBeanAssociation.getBeansForLocation(sourceRowIndex, column)));
            }
            beansForLocation = returnedBeans.toArray();
        }

        beansForLocationCache[row][column] = beansForLocation;
        return beansForLocation;
    }

    public boolean isSingleBean(int row, int column) {
        return false;
    }

    public int getUnderlyingRowIndex(int row, int groupIndex) {
        GroupedRowManager.GroupedRow groupedRow = (GroupedRowManager.GroupedRow) getRows().get(row);
        return groupedRow.getSourceRowIndex(groupIndex);
    }

    public int getUnderlyingRowCount(int row) {
        GroupedRowManager.GroupedRow groupedRow = (GroupedRowManager.GroupedRow) getRows().get(row);
        return groupedRow.getSourceRowCount();
    }

    public int getRowContainingUnderlyingRow(int underlyingRow) {
        GroupedRowManager.GroupedRow groupedRow = getGroupedRowManager().getGroupedRowForSourceRow(underlyingRow);
        return groupedRows.indexOf(groupedRow);
    }

    protected GroupedRowManager getGroupedRowManager() {
        if (groupedRowManager == null) {
            groupedRowManager = createGroupedRowManager();

            //hook in an invisible bucket factory that means that callers of GroupedRowManger.getbucketFactory will always
            //get a factory that returns GroupedRowManager.UnmergableDataBucket if the column is unmergable.
            GroupedRowManager.BucketFactory underlyingBucketFactory = groupedRowManager.getBucketFactory();
            if (underlyingBucketFactory instanceof GroupedRowManager.ColumnClassBucketFactory) {
                GroupedRowManager.ColumnClassBucketFactory columnClassBucketFactory = (GroupedRowManager.ColumnClassBucketFactory) underlyingBucketFactory;
                columnClassBucketFactory.setColumnClassToBucket(Integer.class, IntegerSumBucket.class);
                columnClassBucketFactory.setColumnClassToBucket(Double.class, DoubleSumBucket.class);
                columnClassBucketFactory.setColumnClassToBucket(Quantity.class, QuantitySumBucket.class);
                columnClassBucketFactory.setColumnClassToBucket(FormulaResult.class, FormulaSumBucket.class);
            }
            GroupKeyAwareBucketFactory groupKeyAwareBucketFactory = new GroupKeyAwareBucketFactory(underlyingBucketFactory);
            groupedRowManager.setBucketFactory(groupKeyAwareBucketFactory);
        }

        return groupedRowManager;
    }

    private class TableModelEventHandler implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            int firstRow = e.getFirstRow();
            int lastRow = e.getLastRow();
            int eventColumn = e.getColumn();
            int type = e.getType();

            // If table columns changed, need to regroup
            if (firstRow == TableModelEvent.HEADER_ROW) {
                //todo: would be quite trivial to handle this more specifically, but do not want to cause any bugs at the moment
                //todo: will do this if people end up complaining about how long it takes to add or remove columns while grouped.
                setUncalculated();
                fireTableStructureChanged();
            } else { //not the header row
                if (isUncalculated()) {
                    beansForLocationCache = null;
                    fireTableDataChanged();
                } else {
                    //data changed
                    if (type == TableModelEvent.INSERT || type == TableModelEvent.DELETE) {
                        if (firstRow == lastRow) {
                            beansForLocationCache = null;
                            switch (type) {
                                case TableModelEvent.INSERT:
                                    handleInsert(firstRow);
                                    break;
                                case TableModelEvent.DELETE:
                                    handleRemove(firstRow);
                                    break;
                            }
                        } else {
                            recalcBuckets();
                        }
                    } else {
                        //an update
                        if (!(e instanceof MultipleColumnChangeEvent) && eventColumn == TableModelEvent.ALL_COLUMNS && firstRow == 0 && lastRow == Integer.MAX_VALUE) {
                            recalcBuckets();
                        }
                        //possible actions:
                        //update affects column that is part of the group key => remove affected rows from the current group, add them to new
                        //else => update values of affected buckets
                        if (columnKeyAffected(e)) {
                            beansForLocationCache = null;
                            regroupRows(e);
                        } else {
                            updateBucketValues(e);
                        }
                    }
                }
            }
        }

        private void handleRemove(int firstRow) {
            GroupedRowManager.GroupedRow groupedRow = getGroupedRowManager().getGroupedRowForSourceRow(firstRow);
            getGroupedRowManager().sourceRowRemoved(firstRow);
            if (groupedRow.getSourceRowCount() == 0) {
                removeGroupedRow(groupedRow);
            }
        }

        private void handleInsert(int firstRow) {
            getGroupedRowManager().sourceRowInserted(firstRow);
            addRowToGroup(firstRow, true);
        }

        private void updateBucketValues(TableModelEvent e) {
            int[] columns;
            TableModelEvent[] columnEvents;
            if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                if (e instanceof MultipleColumnChangeEvent) {
                    columnEvents = ((MultipleColumnChangeEvent) e).getColumnChanges();
                    columns = new int[columnEvents.length];
                    for (int i = 0; i < columnEvents.length; i++) {
                        columns[i] = columnEvents[i].getColumn();
                    }
                } else {
                    columns = new int[getColumnCount()];
                    columnEvents = new TableModelEvent[getColumnCount()];
                    for (int i = 0; i < getColumnCount(); i++) {
                        //todo: could filter for only columns that are not part of the key, but i do not expect this to occur often
                        columns[i] = i;
                        columnEvents[i] = e;
                    }
                }
            } else {
                columns = new int[]{e.getColumn()};
                columnEvents = new TableModelEvent[]{e};
            }

            //now iterate all changed columns
            for (int c = 0; c < columnEvents.length; c++) {
                TableModelEvent columnEvent = columnEvents[c];
                int column = columns[c];
                if (columnEvent instanceof CellsInColumnUpdatedEvent) {
                    CellsInColumnUpdatedEvent cellsEvent = (CellsInColumnUpdatedEvent) columnEvent;
                    Set rowsNeedingRecalc = null;
                    for (int r = 0; r < cellsEvent.getRowCount(); r++) {
                        int index = cellsEvent.getRowIndex(r);
                        GroupedRowManager.GroupedRow groupedRow = getGroupedRowManager().getGroupedRowForSourceRow(index);
                        boolean success = groupedRow.updateCellMemberValue(column, cellsEvent.getOldValue(), cellsEvent.getNewValue());
                        if (!success) {
                            rowsNeedingRecalc = SingleItemSet.addToSet(rowsNeedingRecalc, groupedRow);
                        }
                    }
                    if (rowsNeedingRecalc != null) {
                        for (Iterator iterator = rowsNeedingRecalc.iterator(); iterator.hasNext();) {
                            GroupedRowManager.GroupedRow row = (GroupedRowManager.GroupedRow) iterator.next();
                            row.recalcBucket(column);
                        }
                    }
                } else {
                    if (e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE) {
                        //all rows changed
                        int count = getGroupedRowManager().getGroupedRowCount();
                        for (int i = 0; i < count; i++) {
                            GroupedRowManager.GroupedRow groupedRow = getGroupedRowManager().getGroupedRow(i);
                            groupedRow.recalcBucket(column);
                        }
                    } else {
                        for (int row = e.getFirstRow(); row <= e.getLastRow(); row++) {
                            GroupedRowManager.GroupedRow groupedRow = getGroupedRowManager().getGroupedRowForSourceRow(row);
                            groupedRow.recalcBucket(column);
                        }
                    }
                }
            }
        }

        private boolean columnKeyAffected(TableModelEvent e) {
            int column = e.getColumn();
            if (column >= 0) {
                return getGroupedKeyDefinition().isGroupKeyColumn(getColumnKey(column));
            } else if (e instanceof MultipleColumnChangeEvent) {
                CellsInColumnUpdatedEvent[] columnChanges = ((MultipleColumnChangeEvent) e).getColumnChanges();
                for (int i = 0; i < columnChanges.length; i++) {
                    column = columnChanges[i].getColumn();
                    if (getGroupedKeyDefinition().isGroupKeyColumn(getColumnKey(column))) {
                        return true;
                    }
                }
                return false;
            }
            return true; // when column == -1 - all columns have changed including group key columns
        }

        /**
         * the key of some rows changed, reallocate to the correct group
         */
        private void regroupRows(TableModelEvent evt) {
            int[] rows = null;

            if (evt.getFirstRow() != evt.getLastRow()) {
                if (evt instanceof CellsInColumnUpdatedEvent) {
                    rows = ((CellsInColumnUpdatedEvent) evt).getRows();
                } else if (evt instanceof MultipleColumnChangeEvent) {
                    MultipleColumnChangeEvent event = ((MultipleColumnChangeEvent) evt);
                    if (event.isChangedRowsSameForEachColumn()) {
                        rows = event.getColumnChanges()[0].getRows();
                    }
                }
            } else {
                rows = new int[]{evt.getFirstRow()};
            }

            if (rows != null) {
                for (int i = 0; i < rows.length; i++) {
                    int rowIndex = rows[i];

                    GroupedRowManager.GroupedRow groupedRow = getGroupedRowManager().getGroupedRowForSourceRow(rowIndex);
                    int sourceRowCount = groupedRow.getSourceRowCount();
                    //if the iterator only has one element which is the row we are removing, then delete the groupedRow
                    if (sourceRowCount == 1 && groupedRow.getSourceRowIndex(0) == rowIndex) {
                        removeGroupedRow(groupedRow);
                    } else {
                        GroupedRowManager.OldValuesAccessor oldValuesAccessor = null;
                        if (evt instanceof CellsInColumnUpdatedEvent) {
                            oldValuesAccessor = new OldValuesAccessorColumnEvent(rowIndex, (CellsInColumnUpdatedEvent) evt);
                        } else if (evt instanceof MultipleColumnChangeEvent) {
                            oldValuesAccessor = new OldValuesAccessorMultiEvent(rowIndex, (MultipleColumnChangeEvent) evt);
                        }
                        getGroupedRowManager().removeSourceRowFromGroup(groupedRow, rowIndex, oldValuesAccessor, true);
                    }
                    addRowToGroup(rowIndex, true);
                }
            } else {
                recalcBuckets();
            }
        }

        private void removeGroupedRow(GroupedRowManager.GroupedRow groupedRow) {
            int rowIndex = groupedRows.indexOf(groupedRow);

            getGroupedRowManager().deleteGroupedRow(groupedRow);
            beansForLocationCache = null;
            groupedRows.remove(groupedRow);
            Iterator iterator = keyToGroupedRow.entrySet().iterator();
            Object sourceRowKey = null;
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                if (entry.getValue() == groupedRow) {
                    sourceRowKey = entry.getKey();
                    break;
                }
            }
            keyToGroupedRow.remove(sourceRowKey);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    protected static class GroupedRowCellChangeFirer implements GroupedRowManager.CellChangeListener {
        private GroupedTableModel groupedTable;

        public GroupedRowCellChangeFirer(GroupedTableModel groupedTable) {
            this.groupedTable = groupedTable;
        }

        public void cellChanged(GroupedRowManager.GroupedRow row, int column, Object oldValue, Object newValue) {
            if (!Utilities.equals(oldValue, newValue)) {
                int rowIndex = groupedTable.groupedRows.indexOf(row);
                groupedTable.fireTableChanged(new CellsInColumnUpdatedEvent(groupedTable, rowIndex, column, oldValue, newValue));
            }
        }
    }

    private class OldValuesAccessorColumnEvent implements GroupedRowManager.OldValuesAccessor {
        private int row;
        private CellsInColumnUpdatedEvent event;

        public OldValuesAccessorColumnEvent(int row, CellsInColumnUpdatedEvent event) {
            this.row = row;
            this.event = event;
        }

        public Object getOldValue(int column) {
            if (column == event.getColumn()) {
                return event.getOldValue();
            } else {
                return getSourceModel().getValueAt(row, column);
            }
        }
    }

    private class OldValuesAccessorMultiEvent implements GroupedRowManager.OldValuesAccessor {
        private int row;
        private CellsInColumnUpdatedEvent[] columnEvents;

        public OldValuesAccessorMultiEvent(int row, MultipleColumnChangeEvent event) {
            this.row = row;
            columnEvents = new CellsInColumnUpdatedEvent[getColumnCount()];
            CellsInColumnUpdatedEvent[] columnChanges = event.getColumnChanges();
            for (int i = 0; i < columnChanges.length; i++) {
                CellsInColumnUpdatedEvent change = columnChanges[i];
                int columnIndex = change.getColumn();
                columnEvents[columnIndex] = change;
            }
        }

        public Object getOldValue(int column) {
            CellsInColumnUpdatedEvent columnEvent = columnEvents[column];
            if (columnEvent != null) {
                return columnEvent.getOldValue();
            } else {
                return getSourceModel().getValueAt(row, column);
            }
        }
    }

    private class NonSumableColumns implements GroupedKeyDefinition {
        public boolean isGroupKeyColumn(Object columnKey) {
            if (getSourceModel() == null) return false;

            int columnIndex = getColumnIndex(columnKey);
            Class columnClass = getColumnClass(columnIndex);
            return !(Double.class.isAssignableFrom(columnClass) ||
                    Integer.class.isAssignableFrom(columnClass) ||
                    Quantity.class.isAssignableFrom(columnClass));
        }
    }

    private class GroupKeyAwareBucketFactory implements GroupedRowManager.BucketFactory {
        private GroupedRowManager.BucketFactory actualFactory;

        public GroupKeyAwareBucketFactory(GroupedRowManager.BucketFactory actualFactory) {
            this.actualFactory = actualFactory;
        }

        public GroupedRowManager.ValueBucket createBucketForKey(Object columnKey) {
            if (getGroupedKeyDefinition().isGroupKeyColumn(columnKey)) {
                //keys should always use the unmergeable data bucket because they are unique
                return new GroupedRowManager.UnmergeableDataBucket();
            } else {
                return actualFactory.createBucketForKey(columnKey);
            }
        }

        public GroupedRowManager.BucketFactory getActualFactory() {
            return actualFactory;
        }

        public void setActualFactory(GroupedRowManager.BucketFactory factory) {
            actualFactory = factory;
        }
    }

    public static interface GroupedKeyDefinition {
        /**
         * @param columnKey
         * @return true if the values from column "columnKey" should be used in the group primary key
         */
        public boolean isGroupKeyColumn(Object columnKey);
    }
}
