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

import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import javax.swing.table.TableModel;
import java.util.*;

/**
 * A yucky class. Those adverse to event handling should look away now
 * Basically a class to manage grouped table rows and tell you how the grouped rows have changed for underlying table changes
 * <p/>
 * it sits ontop of a table model, and is told which rows belong to which groups.
 * It starts off with a "ColumnClassBucketFactory" (which basically builds aggregate buckets based on the column class -
 * e.g. IntegerSumBucket for all Integer columns, etc). You can change the bucket factory to your own via setBucketFactory.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.13 $
 */
public class GroupedRowManager {
    private static final Log log = Log.getCategory(GroupedRowManager.class);

    private CellChangeListener listener;
    private KeyedColumnTableModel tableModel;

    private Map sourceRowIndexToGroupedRow = new HashMap();
    private List sourceRowIndexes = new ArrayList();    //array of SourceRowIndex objects to easily maintain row indexing
    private boolean sourceRowIndexesEmpty = true;
    //after inserting or deleting source rows

    private ArrayList groupedRows = new ArrayList();
    private BucketFactory bucketFactory;


    public GroupedRowManager(KeyedColumnTableModel tableModel, CellChangeListener listener) {
        this.tableModel = tableModel;
        this.listener = listener;
        this.bucketFactory = new ColumnClassBucketFactory();
    }

    public KeyedColumnTableModel getSourceModel() {
        return tableModel;
    }

    public void setTableModel(KeyedColumnTableModel tableModel) {
        this.tableModel = tableModel;
        clearAll();
    }

    public int getGroupedRowCount() {
        return groupedRows.size();
    }

    public GroupedRow getGroupedRow(int i) {
        return (GroupedRow) groupedRows.get(i);
    }

    public void columnInserted(int insertColumn) {
        for (Iterator iterator = groupedRows.iterator(); iterator.hasNext();) {
            GroupedRow groupedRow = (GroupedRow) iterator.next();
            groupedRow.columnInserted(insertColumn);
        }
    }

    public void columnRemoved(int removeColumn) {
        for (Iterator iterator = groupedRows.iterator(); iterator.hasNext();) {
            GroupedRow groupedRow = (GroupedRow) iterator.next();
            groupedRow.columnRemoved(removeColumn);
        }
    }

    public void columnUpdated(int updateColumn) {
        for (Iterator iterator = groupedRows.iterator(); iterator.hasNext();) {
            GroupedRow groupedRow = (GroupedRow) iterator.next();
            groupedRow.recalcBucket(updateColumn, false);
        }
    }

    /**
     * call this to notify the groupedRowManager that it needs to increment sourceRowIndexes from the given point
     * don't like this paradigm (in that someone has the responsibility of telling grouped row manager that this has occured),
     * but my brain is fried at the moment and I can't think of anything better
     * <p/>
     * The main problem is that I can't add listener to the table model to do this, because the event handling is usually very
     * tightly coupled with the thing that is using the grouped row manager and wants very specific control over when the
     * grouped row manager updates its models.
     * <p/>
     * the answer is that I need to make a little message passing interface to force the user of the groupedRowManger to
     * think about the event handling. Just need to get round to it
     *
     * @param insertPoint
     */
    public void sourceRowInserted(int insertPoint) {
        List sourceRowIndexes = getSourceRowIndexes();
        if (insertPoint < sourceRowIndexes.size()) {
            for (int i = insertPoint; i < sourceRowIndexes.size(); i++) {
                SourceRowIndex sourceRowIndex = (SourceRowIndex) sourceRowIndexes.get(i);
                sourceRowIndex.index = i + 1;
            }
        }
        SourceRowIndex sourceRowIndex = new SourceRowIndex(insertPoint);
        sourceRowIndexes.add(insertPoint, sourceRowIndex);
    }

    /**
     * call this to notify the groupedRowManager that the given source row has been removed from the source model
     * Note: I don't like this reliance upon someone to call this, but users of GroupedRowManager must have control over
     * *when* this processing occurs, so I can't make grouped row manager listen to the source and handle automatically.
     *
     * @param removedRow
     */
    public void sourceRowRemoved(int removedRow) {
        List sourceRowIndexes = getSourceRowIndexes();
        SourceRowIndex removedSourceRowIndex = (SourceRowIndex) sourceRowIndexes.get(removedRow);

        if (removedRow < sourceRowIndexes.size()) {
            for (int i = removedRow; i < sourceRowIndexes.size(); i++) {
                SourceRowIndex sourceRowIndex = (SourceRowIndex) sourceRowIndexes.get(i);
                sourceRowIndex.index = i - 1;
            }
        }
        sourceRowIndexes.remove(removedRow);

        GroupedRow groupedRow = (GroupedRow) sourceRowIndexToGroupedRow.remove(removedSourceRowIndex);
        if (groupedRow != null) {
            groupedRow.removeRow(removedSourceRowIndex, null, true);
        }
    }

    public void addSourceRowToGroup(GroupedRow row, int sourceRow, boolean sendEvents) {
        SourceRowIndex sourceRowIndex = getSourceRowIndex(sourceRow);
        row.addRow(sourceRowIndex, sendEvents);
        sourceRowIndexToGroupedRow.put(sourceRowIndex, row);
    }

    public void removeSourceRowFromGroup(GroupedRow groupedRow, int sourceRow, OldValuesAccessor oldValues, boolean sendEvents) {
        SourceRowIndex sourceRowIndex = getSourceRowIndex(sourceRow);
        groupedRow.removeRow(sourceRowIndex, oldValues, sendEvents);
        sourceRowIndexToGroupedRow.remove(sourceRowIndex);
    }

    public GroupedRow getGroupedRowForSourceRow(int sourceRow) {
        SourceRowIndex sourceRowIndex = getSourceRowIndex(sourceRow);
        return (GroupedRow) sourceRowIndexToGroupedRow.get(sourceRowIndex);
    }

    private SourceRowIndex getSourceRowIndex(int sourceRow) {
        return (SourceRowIndex) getSourceRowIndexes().get(sourceRow);
    }

    protected GroupedRowManager.ValueBucket createValueBucket(int column) {
        Object columnKey = getSourceModel().getColumnKey(column);
        return getBucketFactory().createBucketForKey(columnKey);
    }

    /**
     * use setBucketFactory if you want to override behaviour
     */
    public final BucketFactory getBucketFactory() {
        return bucketFactory;
    }

    public void setBucketFactory(BucketFactory bucketFactory) {
        this.bucketFactory = bucketFactory;
    }

    public GroupedRow createGroupedRow() {
        GroupedRow groupedRow = new GroupedRow(groupedRows.size());
        groupedRows.add(groupedRow);
        return groupedRow;
    }

    public void deleteGroupedRow(GroupedRow groupedRow) {
        for (int i = 0; i < groupedRow.getSourceRowCount(); i++) {
            int rowNum = groupedRow.getSourceRowIndex(i);
            SourceRowIndex sourceRowIndex = getSourceRowIndex(rowNum);
            sourceRowIndexToGroupedRow.remove(sourceRowIndex);
        }
        groupedRows.remove(groupedRow);

        //update row indexes
        for (int i = groupedRow.getRowNumber(); i < groupedRows.size(); i++) {
            GroupedRow followingRow = (GroupedRow) groupedRows.get(i);
            followingRow.setRowNumber(i);
        }
    }

    public void clearAll() {
        sourceRowIndexToGroupedRow.clear();
        sourceRowIndexes.clear();
        sourceRowIndexesEmpty = true;
        groupedRows.clear();
    }

    private List getSourceRowIndexes() {
        if (sourceRowIndexesEmpty) {
            sourceRowIndexesEmpty = false;
            TableModel sourceModel = getSourceModel();
            if (sourceModel != null) {
                for (int row = 0; row < sourceModel.getRowCount(); row++) {
                    sourceRowInserted(row);
                }
            }
        }
        return sourceRowIndexes;
    }

    protected CellChangeListener getListener() {
        return listener;
    }

    public class GroupedRow {
        private ArrayList sourceRowIndexes = new ArrayList();
        private List rowBuckets;  //a list of ValueBuckets
        private int rowNumber;

        public GroupedRow(int rowNumber) {
            this.rowNumber = rowNumber;
            int columnCount = getSourceModel().getColumnCount();
            rowBuckets = new ArrayList(columnCount);
            for (int column = 0; column < columnCount; column++) {
                columnInserted(column);
            }
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public GroupedRowManager getManager() {
            return GroupedRowManager.this;
        }

        private void columnInserted(int column) {
            rowBuckets.add(column, null);
            recalcBucket(column, false);
        }

        public void recalcBucket(int column) {
            recalcBucket(column, true);
        }

        /**
         * get all values contributing to this cell and recalc the cell
         *
         * @param column
         * @param sendEvents
         */
        private void recalcBucket(int column, boolean sendEvents) {
            Object oldValue = null;
            if (sendEvents) {
                oldValue = getCellValue(column);
            }

            rowBuckets.set(column, createValueBucket(column));

            for (int i = 0; i < getSourceRowCount(); i++) {
                int sourceRow = getSourceRowIndex(i);
                Object value = getSourceModel().getValueAt(sourceRow, column);
                addValueToBucket(column, value);
            }

            if (sendEvents) {
                Object newValue = getCellValue(column);
                getListener().cellChanged(this, column, oldValue, newValue);
            }
        }

        private void columnRemoved(int column) {
            if (column >= rowBuckets.size()) {
                log.error("This is the bug Dan has been trying to catch", new RuntimeException());
                return;
            }
            rowBuckets.remove(column);
        }

        /**
         * include the sourceRowIndex in the set that this groupedRow represents
         *
         * @param sourceRowIndex
         * @param sendEvents
         */
        private void addRow(SourceRowIndex sourceRowIndex, boolean sendEvents) {
            sourceRowIndexes.add(sourceRowIndex);
            int sourceRow = sourceRowIndex.index;
            TableModel sourceModel = getSourceModel();
            //iterate new rows cells, and add them to our current cell buckets for this grouping
            for (int col = 0; col < sourceModel.getColumnCount(); col++) {
                Object additionalValue = sourceModel.getValueAt(sourceRow, col);

                Object oldValue = null;

                if (sendEvents) {
                    oldValue = getCellValue(col);
                }
                addValueToBucket(col, additionalValue);

                if (sendEvents) {
                    Object newValue = getCellValue(col);
                    getListener().cellChanged(this, col, oldValue, newValue);
                }
            }
        }

        private void removeRow(SourceRowIndex sourceRowIndex, OldValuesAccessor oldValues, boolean sendEvents) {
            sourceRowIndexes.remove(sourceRowIndex);
            for (int column = 0; column < rowBuckets.size(); column++) {
                if (oldValues == null) {
                    //don't know the value that has been removed, so the next best thing is to recalculate. Expensive.
                    recalcBucket(column, sendEvents);
                } else {
                    Object removedCellValue = oldValues.getOldValue(column);

                    Object oldValue = null;

                    if (sendEvents) {
                        oldValue = getCellValue(column);
                    }
                    boolean success = removeValueFromBucket(column, removedCellValue);
                    if (!success) {
                        recalcBucket(column);
                    }

                    if (sendEvents) {
                        Object newValue = getCellValue(column);
                        getListener().cellChanged(this, column, oldValue, newValue);
                    }
                }
            }
        }

        public boolean updateCellMemberValue(int columnIndex, Object oldValue, Object newValue) {
            return updateCellMemberValue(columnIndex, oldValue, newValue, 1);
        }

        /**
         * ummm, ok, this is a really sucky api, I can't think of any other contexts in which someone woulda want to call this method
         * instead of updateCellMemberValue(int columnIndex, Object oldValue, Object newValue)
         * But, GroupedTableModel uses it to avoid spewing lots of cell events if a number of rows in a column experienced the same value change
         *
         * @param columnIndex
         * @param oldValue
         * @param newValue
         * @param repeatCount
         */
        public boolean updateCellMemberValue(int columnIndex, Object oldValue, Object newValue, int repeatCount) {
            if (Utilities.equals(oldValue, newValue)) {
                return true;
            }
            Object oldBucketValue = null;
            Object newBucketValue;

            oldBucketValue = getCellValue(columnIndex);

            for (int i = 0; i < repeatCount; i++) {
                if (oldValue == CachedObjectGraph.DATA_NOT_READY) {
                    if (oldBucketValue != oldValue) {
                        ApplicationDiagnostics.getInstance().productionAssert(false, "GroupedRowManager: Unexpected old value for row: " + rowNumber + " col: " + columnIndex + " expOldVal: " + oldBucketValue + " oldVal: " + oldValue);
                    }
                    addValueToBucket(columnIndex, newValue);
                    changeUnreadyCount(columnIndex, -1);
                } else if (newValue == CachedObjectGraph.DATA_NOT_READY) {
                    boolean success = removeValueFromBucket(columnIndex, oldValue);
                    if (!success) return false;
                    changeUnreadyCount(columnIndex, +1);
                } else {
                    boolean success = getBucket(columnIndex).update(oldValue, newValue);
                    if (!success) return false;
                }
            }
            newBucketValue = getCellValue(columnIndex);
            getListener().cellChanged(this, columnIndex, oldBucketValue, newBucketValue);
            return true;
        }

        private void changeUnreadyCount(int columnIndex, int delta) {
            Object cell = rowBuckets.get(columnIndex);
            if (!(cell instanceof UnreadyData)) {
                if (delta < 0) {
                    ApplicationDiagnostics.getInstance().productionAssert(false, "Bug: Asked to reduce the unreadyCount of column " + columnIndex + " row: " + this + " by " + delta + " but cell thought it was ready! (value = " + cell);
                    return;
                } else {
                    cell = new UnreadyData(0);
                    ValueBucket bucket = (ValueBucket) rowBuckets.set(columnIndex, cell);
                    ((UnreadyData) cell).underlyingBucket = bucket;
                }
            }
            UnreadyData unreadyData = (UnreadyData) cell;
            unreadyData.changeUnreadyCount(delta);

            if (unreadyData.isReady()) {
                rowBuckets.set(columnIndex, unreadyData.underlyingBucket);
            }
        }

        private void addValueToBucket(int col, Object value) {
            if (value == CachedObjectGraph.DATA_NOT_READY) {
                changeUnreadyCount(col, +1);
            } else {
                ValueBucket currentValue = getBucket(col);
                currentValue.insert(value);
            }
        }

        private ValueBucket getBucket(int col) {
            Object cell = rowBuckets.get(col);
            if (cell instanceof UnreadyData) {
                return ((UnreadyData) cell).underlyingBucket;
            }
            return (ValueBucket) cell;
        }

        private boolean removeValueFromBucket(int col, Object value) {
            ValueBucket currentValue = getBucket(col);
            if (value == CachedObjectGraph.DATA_NOT_READY) {
                changeUnreadyCount(col, -1);
                return true;
            } else {
                return currentValue.delete(value);
            }
        }

        public Object getCellValue(int column) {
            Object obj = rowBuckets.get(column);
            if (obj instanceof UnreadyData) {
                return CachedObjectGraph.DATA_NOT_READY;
            } else {
                return ((ValueBucket) obj).getOutput();
            }
        }

        /**
         * to be used with getSourceRowCount in order to find the underlying index of rows in the group
         *
         * @param groupIndex ranges between 0 and getSourceRowCount()-1
         */
        public int getSourceRowIndex(int groupIndex) {
            SourceRowIndex sourceRowIndex = (SourceRowIndex) sourceRowIndexes.get(groupIndex);
            return sourceRowIndex.index;
        }

        public int getSourceRowCount() {
            return sourceRowIndexes.size();
        }

        public TableModel getSourceModel() {
            return GroupedRowManager.this.getSourceModel();
        }
    }

    private static class SourceRowIndex {
        public int index;

        public SourceRowIndex(int index) {
            this.index = index;
        }
    }


    public interface ValueBucket {
        public void insert(Object value);

        /**
         * @return true if the update could be applied to the bucket. False means that the bucket could not use this information
         *         to update its output correctly.
         *         for example, a summation bucket cannot determine the correct value if we change a value from NaN to 0.0
         */
        public boolean update(Object oldValue, Object newValue);

        /**
         * @param value
         * @return true if the delete could be applied to the bucket. False means that the bucket could not use this information
         *         to update its output correctly.
         *         for example, a summation bucket cannot determine the correct value if we remove NaN from the total.
         */
        public boolean delete(Object value);

        public Object getOutput();
    }

    public interface CellChangeListener {
        public void cellChanged(GroupedRow row, int column, Object oldValue, Object newValue);
    }


    /**
     * just shows the first value that was added to it.
     * I use this bucket for non-mergable data (i.e. where you expect all the values added to the bucket to be the same)
     */
    public static class UnmergeableDataBucket implements ValueBucket {
        public static final String NA = "N/A";
        private int dataCount = 0;
        private Object mergeValue = null;

        public boolean delete(Object value) {
            dataCount--;
            if (dataCount == 0) {
                mergeValue = null;
            }
            return true;
        }

        public Object getOutput() {
            return mergeValue;
        }

        public void insert(Object value) {
            if (dataCount == 0) {
                mergeValue = value;
            } else {
                if (!Utilities.equals(mergeValue, value)) {
                    mergeValue = NA;
                }
            }
            dataCount++;
        }

        public boolean update(Object oldValue, Object newValue) {
            mergeValue = newValue;
            return true;
        }

        public String toString() {
            return "No grouping";
        }
    }

    private static class UnreadyData {
        private ValueBucket underlyingBucket;
        private int unreadyCount;

        public UnreadyData(int initialUnready) {
            unreadyCount = initialUnready;
        }

        public void changeUnreadyCount(int delta) {
            unreadyCount += delta;
        }

        public boolean isReady() {
            return unreadyCount == 0;
        }
    }

    public interface OldValuesAccessor {
        public Object getOldValue(int column);
    }

    public interface BucketFactory {
        public ValueBucket createBucketForKey(Object columnKey);
    }

    public class ColumnClassBucketFactory implements BucketFactory {
        private HashMap columnClassToBucketClass = new HashMap(8);

        public ColumnClassBucketFactory() {
            columnClassToBucketClass.put(Object.class, UnmergeableDataBucket.class);
        }

        public void clear() {
            columnClassToBucketClass.clear();
        }

        public void setColumnClassToBucket(Class columnClass, Class bucketType) {
            columnClassToBucketClass.put(columnClass, bucketType);
        }

        public Class getBucketForClass(Class columnClass) {
            return (Class) columnClassToBucketClass.get(columnClass);
        }

        public Set getDefinedColumnClasses() {
            return columnClassToBucketClass.keySet();
        }

        public ValueBucket createBucketForKey(Object columnKey) {
            int columnIndex = -1;
            if (getSourceModel() != null) columnIndex = getSourceModel().getColumnIndex(columnKey);
            if (columnIndex >= 0) {
                Class columnClass = getSourceModel().getColumnClass(columnIndex);
                Class bucketClass = (Class) columnClassToBucketClass.get(columnClass);
                if (bucketClass == null) {
                    bucketClass = (Class) columnClassToBucketClass.get(Object.class);
                }
                GroupedRowManager.ValueBucket newBucket = null;
                try {
                    newBucket = (GroupedRowManager.ValueBucket) bucketClass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Could not create bucket of type: " + bucketClass, e);
                }
                return newBucket;
            }
            return null;
        }
    }
}
