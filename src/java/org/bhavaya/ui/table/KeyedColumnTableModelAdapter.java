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

import org.bhavaya.util.Utilities;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;

/**
 * An implementation of KeyedColumnTableModel that uses the columnName and columnType of TableModel to make keys for columns.
 * This wraps a TableModel
 * This is the best that can be done if you need to use an arbitrary TableModel as the sourceModel to one of the ChainableTableModel implementations.
 *
 * Note that the "source" property of tableModelEvents does not become this table model, it remains unchanged as the wrapped TableModel.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public class KeyedColumnTableModelAdapter implements KeyedColumnTableModel {
    private TableModel delegateTableModel;
    private ArrayList columnKeys = new ArrayList();

    private ArrayList listeners = new ArrayList(2);


    public KeyedColumnTableModelAdapter(TableModel delegateTableModel) {
        this.delegateTableModel = delegateTableModel;
        recalcAllKeys();
        delegateTableModel.addTableModelListener(new ColumnKeyUpdater());
    }

    private void recalcAllKeys() {
        int columnCount = getColumnCount();
        ArrayList reorganisedColumnKeys = new ArrayList(columnCount);
        for (int i=0; i<columnCount; i++) {
            String columnName = getColumnName(i);
            Class columnType = getColumnClass(i);
            int matchingIndex = findFirstMatchingKey(columnType, columnName);

            if (matchingIndex >= 0) {
                NameTypeKey key = (NameTypeKey) columnKeys.remove(matchingIndex);
                key.columnIndex = i;
                reorganisedColumnKeys.add(key);
            } else {
                reorganisedColumnKeys.add(new NameTypeKey(columnName, columnType, i));
            }
        }

        //now invalidate the columnindexes of all remaining keys
        for (int i=0; i<columnKeys.size(); i++) {
            NameTypeKey key = (NameTypeKey) columnKeys.get(i);
            key.columnIndex = -1;
        }

        columnKeys = reorganisedColumnKeys;
    }

    private void columnInserted(int insertIndex) {
        String columnName = getColumnName(insertIndex);
        Class columnClass = getColumnClass(insertIndex);
        NameTypeKey newKey = new NameTypeKey(columnName, columnClass, insertIndex);
        columnKeys.add(insertIndex, newKey);

        for (int i=insertIndex+1; i<columnKeys.size(); i++) {
            NameTypeKey key = (NameTypeKey) columnKeys.get(i);
            key.columnIndex = i;
        }
    }

    private void columnUpdated(int updateIndex) {
        String columnName = getColumnName(updateIndex);
        Class columnClass = getColumnClass(updateIndex);
        NameTypeKey newKey = new NameTypeKey(columnName, columnClass, updateIndex);
        NameTypeKey oldKey = (NameTypeKey) columnKeys.set(updateIndex, newKey);
        oldKey.columnIndex = -1;
    }

    private void columnRemoved(int removeIndex) {
        NameTypeKey removedKey = (NameTypeKey) columnKeys.remove(removeIndex);
        removedKey.columnIndex = -1;

        for (int i=removeIndex; i<columnKeys.size(); i++) {
            NameTypeKey key = (NameTypeKey) columnKeys.get(i);
            key.columnIndex = i;
        }
    }

    private int findFirstMatchingKey(Class type, String columnName) {
        int keyCount = columnKeys.size();
        int firstClassMatch = -1;
        for (int i=0; i<keyCount; i++) {
            NameTypeKey key = (NameTypeKey) columnKeys.get(i);
            if (key.type == type) {
                firstClassMatch = i;
                if (Utilities.equals(key.name, columnName)) return i;
            }
        }
        return firstClassMatch;
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    protected void fireTableEvent(TableModelEvent e) {
        for (int i = 0; i < listeners.size(); i++) {
            TableModelListener l = (TableModelListener) listeners.get(i);
            l.tableChanged(e);
        }
    }

    public int getColumnIndex(Object columnKey) {
        return ((NameTypeKey)columnKey).columnIndex;
    }

    public Object getColumnKey(int index) {
        return columnKeys.get(index);
    }

    public int getRowCount() {
        return delegateTableModel.getRowCount();
    }

    public int getColumnCount() {
        return delegateTableModel.getColumnCount();
    }

    public String getColumnName(int columnIndex) {
        return delegateTableModel.getColumnName(columnIndex);
    }

    public Class getColumnClass(int columnIndex) {
        return delegateTableModel.getColumnClass(columnIndex);
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return delegateTableModel.isCellEditable(rowIndex, columnIndex);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return delegateTableModel.getValueAt(rowIndex, columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        delegateTableModel.setValueAt(aValue, rowIndex, columnIndex);
    }

    private static class NameTypeKey {
        private Class type;
        private String name;
        private int columnIndex;

        public NameTypeKey(String name, Class type, int columnIndex) {
            this.name = name;
            this.type = type;
            this.columnIndex = columnIndex;
        }
    }

    private class ColumnKeyUpdater implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                    //structure changed
                    recalcAllKeys();
                } else {
                    int columnIndex = e.getColumn();
                    switch (e.getType()) {
                        case TableModelEvent.INSERT:
                            columnInserted(columnIndex);
                            break;
                        case TableModelEvent.UPDATE:
                            columnUpdated(columnIndex);
                            break;
                        case TableModelEvent.DELETE:
                            columnRemoved(columnIndex);
                            break;
                    }
                }
            }

            //now pass on the event to our table model listeners
            fireTableEvent(e);
        }
    }
}
