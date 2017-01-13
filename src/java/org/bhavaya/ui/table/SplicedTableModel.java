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
import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.15 $
 */
public class SplicedTableModel extends AbstractTableModel implements TabularBeanAssociation, KeyedColumnTableModel {
    private static final Log log = Log.getCategory(SplicedTableModel.class);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private KeyedColumnTableModel primaryModel;
    private KeyedColumnTableModel secondaryModel;
    private Object leftJoinColumn = null;
    private Object rightJoinColumn = null;
    private int hiddenLeftJoinColumnIndex = -1;
    private int hiddenRightJoinColumnIndex = -1;

    private IndexedSet keysAndIndicies = new IndexedSet();
    private boolean indiciesValid = false;

    public SplicedTableModel(KeyedColumnTableModel leftModel, Object leftJoinColumnKey, KeyedColumnTableModel rightModel, Object rightJoinColumnKey) {
        this.primaryModel = leftModel;
        this.secondaryModel = rightModel;

        primaryModel.addTableModelListener(new TableModelUpdateHandler(primaryModel));
        secondaryModel.addTableModelListener(new TableModelUpdateHandler(secondaryModel));

        this.leftJoinColumn = leftJoinColumnKey;
        this.rightJoinColumn = rightJoinColumnKey;
        updateHiddenColumnIndexes();
    }

    private void updateHiddenColumnIndexes() {
        hiddenLeftJoinColumnIndex = -1;
        hiddenRightJoinColumnIndex = -1;
        hiddenLeftJoinColumnIndex = primaryModel.getColumnIndex(leftJoinColumn);
        hiddenRightJoinColumnIndex = secondaryModel.getColumnIndex(rightJoinColumn);
        if (hiddenRightJoinColumnIndex >= 0) {
            hiddenRightJoinColumnIndex += primaryModel.getColumnCount();
        }
    }

    private void invalidateRowIndicies() {
        indiciesValid = false;
    }

    private void validateRowIndexes() {
        if (!indiciesValid) {
            int rightJoinIndex = hiddenRightJoinColumnIndex - primaryModel.getColumnCount();
            keysAndIndicies.clear();
            for (int row = 0; row < secondaryModel.getRowCount(); row++) {
                Object rowKey = secondaryModel.getValueAt(row, rightJoinIndex);
                ApplicationDiagnostics.getInstance().productionAssert(!keysAndIndicies.contains(rowKey), "Arrrgh, bug. Row key was not unique: " + rowKey);
                keysAndIndicies.add(row, rowKey);
            }
            indiciesValid = true;
        }
    }

    private int getSecondaryRowIndexForKey(Object key) {
        return keysAndIndicies.indexOf(key);
    }

    public int getColumnCount() {
        int count = primaryModel.getColumnCount() + secondaryModel.getColumnCount();
        if (hiddenLeftJoinColumnIndex >= 0) count -= 1;
        if (hiddenRightJoinColumnIndex >= 0) count -= 1;
        return count;
    }

    public int getRowCount() {
        return primaryModel.getRowCount();
    }

    public Object getValueAt(int primaryRowIndex, int columnIndex) {
        validateRowIndexes();
        columnIndex = mapHiddenToNonHiddenIndex(columnIndex);
        if (columnIndex < primaryModel.getColumnCount()) {
            return primaryModel.getValueAt(primaryRowIndex, columnIndex);
        } else {
            columnIndex -= primaryModel.getColumnCount();

            Object primaryJoinKey = primaryModel.getValueAt(primaryRowIndex, hiddenLeftJoinColumnIndex);
            int index = getSecondaryRowIndexForKey(primaryJoinKey);
            if (index < 0) {
                return null;
            } else {
                return secondaryModel.getValueAt(index, columnIndex);
            }
        }
    }

    private int mapHiddenToNonHiddenIndex(int columnIndex) {
        if (hiddenLeftJoinColumnIndex >= 0 && columnIndex >= hiddenLeftJoinColumnIndex) {
            columnIndex += 1;
        }
        if (hiddenRightJoinColumnIndex >= 0 && columnIndex >= primaryModel.getColumnCount() && columnIndex >= hiddenRightJoinColumnIndex) {
            columnIndex += 1;
        }
        return columnIndex;
    }

    private int mapNonHiddenToHiddenIndex(int columnIndex) {
        int hiddenIndex = columnIndex;
        if (hiddenLeftJoinColumnIndex >= 0) {
            if (columnIndex == hiddenLeftJoinColumnIndex) return -1;
            if (columnIndex > hiddenLeftJoinColumnIndex) hiddenIndex -= 1;
        }
        if (hiddenRightJoinColumnIndex >= 0) {
            if (columnIndex == hiddenRightJoinColumnIndex) return -1;
            if (columnIndex > hiddenRightJoinColumnIndex) hiddenIndex -= 1;
        }
        return hiddenIndex;
    }

    public Object[] getBeansForLocation(int primaryRowIndex, int columnIndex) {
        validateRowIndexes();
        columnIndex = mapHiddenToNonHiddenIndex(columnIndex);
        KeyedColumnTableModel model;
        int rowIndex;
        if (columnIndex < primaryModel.getColumnCount()) {
            model = primaryModel;
            rowIndex = primaryRowIndex;
        } else {
            model = secondaryModel;
            columnIndex -= primaryModel.getColumnCount();

            Object primaryJoinKey = primaryModel.getValueAt(primaryRowIndex, hiddenLeftJoinColumnIndex);
            rowIndex = getSecondaryRowIndexForKey(primaryJoinKey);
            if (rowIndex < 0) {
                return EMPTY_OBJECT_ARRAY;
            }
        }

        if (model instanceof TabularBeanAssociation && primaryRowIndex < model.getRowCount()) {
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) model;
            return tabularBeanAssociation.getBeansForLocation(rowIndex, columnIndex);
        }
        return EMPTY_OBJECT_ARRAY;
    }

    public boolean isSingleBean(int primaryRowIndex, int columnIndex) {
        validateRowIndexes();
        TableModel model;
        columnIndex = mapHiddenToNonHiddenIndex(columnIndex);
        int rowIndex;
        if (columnIndex < primaryModel.getColumnCount()) {
            model = primaryModel;
            rowIndex = primaryRowIndex;
        } else {
            model = secondaryModel;
            columnIndex -= primaryModel.getColumnCount();

            Object primaryJoinKey = primaryModel.getValueAt(primaryRowIndex, hiddenLeftJoinColumnIndex);
            rowIndex = getSecondaryRowIndexForKey(primaryJoinKey);
            if (rowIndex < 0) {
                return true;
            }
        }

        if (model instanceof TabularBeanAssociation && rowIndex < model.getRowCount()) {
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) model;
            return tabularBeanAssociation.isSingleBean(rowIndex, columnIndex);
        }
        return false;
    }

    public int getColumnIndex(Object columnKey) {
        int columnIndex = primaryModel.getColumnIndex(columnKey);
        if (columnIndex < 0) {
            columnIndex = secondaryModel.getColumnIndex(columnKey);
            if (columnIndex >= 0) {
                columnIndex += primaryModel.getColumnCount();
            }
        }
        columnIndex = mapNonHiddenToHiddenIndex(columnIndex);
        return columnIndex;
    }

    public Object getColumnKey(int columnIndex) {
        KeyedColumnTableModel model;
        columnIndex = mapHiddenToNonHiddenIndex(columnIndex);
        if (columnIndex < primaryModel.getColumnCount()) {
            model = primaryModel;
        } else {
            model = secondaryModel;
            columnIndex -= primaryModel.getColumnCount();
        }

        return model.getColumnKey(columnIndex);
    }

    public String getColumnName(int columnIndex) {
        KeyedColumnTableModel model;
        columnIndex = mapHiddenToNonHiddenIndex(columnIndex);
        if (columnIndex < primaryModel.getColumnCount()) {
            model = primaryModel;
        } else {
            model = secondaryModel;
            columnIndex -= primaryModel.getColumnCount();
        }

        return model.getColumnName(columnIndex);
    }

    public Class getColumnClass(int columnIndex) {
        KeyedColumnTableModel model;
        columnIndex = mapHiddenToNonHiddenIndex(columnIndex);
        if (columnIndex < primaryModel.getColumnCount()) {
            model = primaryModel;
        } else {
            model = secondaryModel;
            columnIndex -= primaryModel.getColumnCount();
        }

        return model.getColumnClass(columnIndex);
    }

    public void fireTableDataChanged() {
        invalidateRowIndicies();
        fireTableDataChangedNoInvalidate();
    }

    private void fireTableDataChangedNoInvalidate() {
        super.fireTableDataChanged();
    }

    public void fireTableStructureChanged() {
        invalidateRowIndicies();
        super.fireTableStructureChanged();
    }

    private class TableModelUpdateHandler implements TableModelListener {
        private TableModel sourceModel;

        public TableModelUpdateHandler(TableModel sourceModel) {
            this.sourceModel = sourceModel;
        }

        public void tableChanged(TableModelEvent e) {
            if (!SwingUtilities.isEventDispatchThread()) {
                if (log.isDebug())log.debug("A breakpoint");
            }
            //an inserted or deleted row on one half of the table is complex to deal with. It might, or might not change the final row count.
            //for simplicity, I will simply fire a data changed event and address this if it is a performance problem
            if (e.getFirstRow() != TableModelEvent.HEADER_ROW
                    && (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE)) {
                fireTableDataChanged(); // hits: 326/1616 (profiling comment, 1616 total method calls, hit this bit 326 times)
                return;
            }
            if (!(e instanceof CellsInColumnUpdatedEvent) && !(e instanceof MultipleColumnChangeEvent)) {
                //if is a data or structure change, then just pass it on
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                    if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {    //structure change
                        updateHiddenColumnIndexes();
                        fireTableStructureChanged();
                        return;
                    } else if (e.getLastRow() == Integer.MAX_VALUE) {
                        // todo by avoiding table structure change in underlying table model we save many calls to invalidateRowIndicies which a bottleneck (actually the validateRowIndexes method)
                        // hits: 925/1616
                        fireTableDataChanged();
                        return;
                    }
                }
            }

            TableModelEvent repackaged;
            //at this point is is either cell updates or column insertion or removal
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                int columnIndex;
                if (e.getType() == TableModelEvent.DELETE) {
                    columnIndex = mapNonHiddenToHiddenIndex(e.getColumn());
                    updateHiddenColumnIndexes();
                } else if (e.getType() == TableModelEvent.INSERT) {
                    updateHiddenColumnIndexes();
                    columnIndex = mapNonHiddenToHiddenIndex(e.getColumn());
                } else {
                    assert (false) : "unknown event";
                    return;
                }
                if (columnIndex < 0) return;
                repackaged = new TableModelEvent(SplicedTableModel.this, e.getFirstRow(), e.getLastRow(), columnIndex, e.getType());
            } else {
                assert (e.getType() == TableModelEvent.UPDATE) : "By this point, the event should be an update";
                if (sourceModel == primaryModel && e.getColumn() == hiddenLeftJoinColumnIndex) {
                    // fire the row update - it will do the work as the getValueAt returns correct values
                    // hits: 366/1616
                    TableModelEvent event = new TableModelEvent(SplicedTableModel.this, e.getFirstRow(), e.getLastRow());
                    fireTableChanged(event);
                    return;
                } else if (sourceModel == secondaryModel && e.getColumn() == hiddenRightJoinColumnIndex - primaryModel.getColumnCount()) {
                    int[] out_minAndMax = new int[2];
                    updateKeysAndIndicies(e, out_minAndMax);
                    TableModelEvent rowsUpdated = new TableModelEvent(SplicedTableModel.this, out_minAndMax[0], out_minAndMax[1]);
                    fireTableChanged(rowsUpdated);
                    return;
                }

                if (e instanceof CellsInColumnUpdatedEvent) {
                    repackaged = repackageColumnEvent((CellsInColumnUpdatedEvent) e);
                } else if (e instanceof MultipleColumnChangeEvent) {
                    MultipleColumnChangeEvent multiEvent = (MultipleColumnChangeEvent) e;
                    CellsInColumnUpdatedEvent[] columnChanges = multiEvent.getColumnChanges();
                    CellsInColumnUpdatedEvent[] newChanges = new CellsInColumnUpdatedEvent[columnChanges.length];
                    for (int i = 0; i < columnChanges.length; i++) {
                        CellsInColumnUpdatedEvent change = columnChanges[i];
                        if (sourceModel == primaryModel && change.getColumn() == hiddenLeftJoinColumnIndex) {
                            fireTableDataChanged();
                            return;
                        } else if (sourceModel == secondaryModel && change.getColumn() == hiddenRightJoinColumnIndex - primaryModel.getColumnCount()) {
                            int[] out_minAndMax = new int[2];
                            updateKeysAndIndicies(e, out_minAndMax);
                            fireTableDataChangedNoInvalidate();
                            return;
                        } else {
                            newChanges[i] = repackageColumnEvent(change);
                        }
                    }
                    newChanges = (CellsInColumnUpdatedEvent[]) Utilities.stripNulls(newChanges);
                    repackaged = new MultipleColumnChangeEvent(SplicedTableModel.this, newChanges, multiEvent.isChangedRowsSameForEachColumn());
                } else {
                    int changedColumn = e.getColumn();
                    changedColumn = mapNonHiddenToHiddenIndex(changedColumn);
                    repackaged = new TableModelEvent(SplicedTableModel.this, e.getFirstRow(), e.getLastRow(), changedColumn, e.getType());
                }
            }
            if (repackaged != null) {
                fireTableChanged(repackaged);
            }
        }

        private void updateKeysAndIndicies(TableModelEvent e, int[] out_minAndMax) {
            if (indiciesValid) {
                int rightJoinIndex = hiddenRightJoinColumnIndex - primaryModel.getColumnCount();

                boolean useCellEvent = false;
                int first;
                int last;
                if (e instanceof CellsInColumnUpdatedEvent) {
                    first = 0;
                    last = ((CellsInColumnUpdatedEvent) e).getRowCount();
                    useCellEvent = true;
                } else {
                    first = e.getFirstRow();
                    last = Math.min(secondaryModel.getRowCount(), e.getLastRow() + 1);
                }
                for (int i = first; i < last; i++) {
                    int row = (useCellEvent) ? ((CellsInColumnUpdatedEvent) e).getRowIndex(i) : i;
                    Object rowKey = secondaryModel.getValueAt(row, rightJoinIndex);
                    keysAndIndicies.set(row, rowKey);

                    out_minAndMax[0] = Math.min(out_minAndMax[0], row);
                    out_minAndMax[1] = Math.max(out_minAndMax[1], row);
                }
            } else {
                validateRowIndexes();
                out_minAndMax[0] = 0;
                out_minAndMax[1] = secondaryModel.getRowCount();
            }
        }

        private CellsInColumnUpdatedEvent repackageColumnEvent(CellsInColumnUpdatedEvent event) {
            CellsInColumnUpdatedEvent repackaged;
            int newColumn = event.getColumn();
            int[] rows = event.getRows();
            if (event.getSource() == secondaryModel) {
                newColumn += primaryModel.getColumnCount();
                rows = getPrimaryModelRows(rows); // fix row indexes to those of the primary model
                if (rows == null) return null;
            }
            newColumn = mapNonHiddenToHiddenIndex(newColumn);
            assert (newColumn != -1) : "Should not try to repackage events for the hidden column";
            repackaged = new CellsInColumnUpdatedEvent(SplicedTableModel.this, rows, newColumn, event.getOldValue(), event.getNewValue());
            return repackaged;
        }

        /**
         * Returns the array with row numbers of the primary model for rows in the secondary model.
         * <p/>
         * Secondary model row can be joined with:
         * - no primary model rows (not displayed)
         * - single primary row (displayed just once)
         * - multiple primary rows (displayed multiple times)
         */
        private int[] getPrimaryModelRows(int[] rowsSecondaryModel) {
            validateRowIndexes();
            List secondaryJoinKeys = new ArrayList(rowsSecondaryModel.length);
            for (int i = 0; i < rowsSecondaryModel.length; i++) {
                int secondaryModelRow = rowsSecondaryModel[i];
                Object secondaryJoinKey = keysAndIndicies.get(secondaryModelRow);
                secondaryJoinKeys.add(secondaryJoinKey);
            }
            // go through the table and get the indexes of primary rows joined with given secondary rows
            List primaryRowNumbers = new ArrayList();
            for (int row = 0; row < getRowCount(); row++) {
                Object primaryJoinKey = primaryModel.getValueAt(row, hiddenLeftJoinColumnIndex);
                if (secondaryJoinKeys.contains(primaryJoinKey)) {
                    primaryRowNumbers.add(new Integer(row));
                }
            }
            if (primaryRowNumbers.size() == 0) {
                return null; // no primary rows joined to this sec. row
            }
            // copy to array
            int[] ret = new int[primaryRowNumbers.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = ((Integer) primaryRowNumbers.get(i)).intValue();
            }
            return ret;
        }
    }
}

