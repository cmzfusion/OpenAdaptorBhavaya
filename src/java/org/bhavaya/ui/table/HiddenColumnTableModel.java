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

import org.bhavaya.util.Log;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

/**
 * Not meant to be used to allow a user to hide columns. This is used to mask the fact that a column is present in a table
 * Probably only useful in
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public class HiddenColumnTableModel extends AbstractTableModel implements TabularBeanAssociation, KeyedColumnTableModel {
    private static final Log log = Log.getCategory(HiddenColumnTableModel.class);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private KeyedColumnTableModel sourceModel;
    private Object hiddenColumnKey;
    private int hiddenColumnIndex = -1;


    public HiddenColumnTableModel(KeyedColumnTableModel primaryModel) {
        this.sourceModel = primaryModel;
        primaryModel.addTableModelListener(new TableModelUpdateHandler());
    }

    public int getColumnCount() {
        return sourceModel.getColumnCount()-getHiddenColumnCount();
    }

    private int getHiddenColumnCount() {
        if (hiddenColumnIndex >= 0) return 1;
        return 0;
    }

    public int getRowCount() {
        return sourceModel.getRowCount();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return sourceModel.getValueAt(rowIndex, columnIndex);
    }

    public Object[] getBeansForLocation(int rowIndex, int columnIndex) {
        if (sourceModel instanceof TabularBeanAssociation) {
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) sourceModel;
            tabularBeanAssociation.getBeansForLocation(rowIndex, columnIndex);
        }
        return EMPTY_OBJECT_ARRAY;
    }

    public boolean isSingleBean(int rowIndex, int columnIndex) {
        if (sourceModel instanceof TabularBeanAssociation) {
            TabularBeanAssociation tabularBeanAssociation = (TabularBeanAssociation) sourceModel;
            return tabularBeanAssociation.isSingleBean(rowIndex, columnIndex);
        }
        return false;
    }

    public int getColumnIndex(Object columnKey) {
        return sourceModel.getColumnIndex(columnKey);
    }

    public Object getColumnKey(int columnIndex) {
        return sourceModel.getColumnKey(columnIndex);
    }

    public String getColumnName(int columnIndex) {
        return sourceModel.getColumnName(columnIndex);
    }

    public Class getColumnClass(int columnIndex) {
        return sourceModel.getColumnClass(columnIndex);
    }


    private class TableModelUpdateHandler implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            TableModelEvent repackaged;
            if (e.getColumn() >= 0 && e.getColumn() >= sourceModel.getColumnCount()) {
                if (e instanceof CellsInColumnUpdatedEvent) {
                    repackaged = repackageColumnEvent(e);
                }else if (e instanceof MultipleColumnChangeEvent) {
                    MultipleColumnChangeEvent multiEvent = (MultipleColumnChangeEvent) e;
                    CellsInColumnUpdatedEvent[] columnChanges = multiEvent.getColumnChanges();
                    CellsInColumnUpdatedEvent[] newChanges = new CellsInColumnUpdatedEvent[columnChanges.length];
                    for (int i = 0; i < columnChanges.length; i++) {
                        CellsInColumnUpdatedEvent change = columnChanges[i];
                        newChanges[i] = repackageColumnEvent(change);
                    }
                    repackaged = new MultipleColumnChangeEvent(HiddenColumnTableModel.this, newChanges, multiEvent.isChangedRowsSameForEachColumn());
                } else if (e.getType() == TableModelEvent.DELETE && e.getColumn() == sourceModel.getColumnCount()) {
                    repackaged = e;
                } else {
                    int newColumn = e.getColumn()+sourceModel.getColumnCount();
                    repackaged = new TableModelEvent(HiddenColumnTableModel.this, e.getFirstRow(), e.getLastRow(), newColumn, e.getType());
                }
            } else {
                repackaged = e;
            }
            fireTableChanged(repackaged);
        }

        private CellsInColumnUpdatedEvent repackageColumnEvent(TableModelEvent e) {
            CellsInColumnUpdatedEvent repackaged;
            CellsInColumnUpdatedEvent event = (CellsInColumnUpdatedEvent) e;
            int newColumn = event.getColumn() + sourceModel.getColumnCount();
            repackaged = new CellsInColumnUpdatedEvent(HiddenColumnTableModel.this, event.getRows(), newColumn, event.getOldValue(), event.getNewValue());
            return repackaged;
        }
    }
}
