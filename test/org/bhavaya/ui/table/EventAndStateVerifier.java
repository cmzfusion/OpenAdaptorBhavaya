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

import junit.framework.TestCase;
import org.bhavaya.ui.table.CellsInColumnUpdatedEvent;
import org.bhavaya.ui.table.MultipleColumnChangeEvent;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;

/**
 * Register this class as a listener with your table model. It will keep a copy of the table model data
 * and verify events submitted by the table model. 
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
public class EventAndStateVerifier implements TableModelListener {

    private ArrayList rows = new ArrayList();
    private int colCount = 0;
    private TableModel source;

    public EventAndStateVerifier(TableModel source) {
        this.source = source;
        refresh();
    }

    private void refresh() {
        rows.clear();
        colCount = source.getColumnCount();
        for (int row = 0; row < source.getRowCount(); row++) {
            ArrayList rowValues = new ArrayList(colCount);
            for (int col = 0; col < colCount; col++) {
                rowValues.add(source.getValueAt(row, col));
            }
            rows.add(rowValues);
        }
    }

    private void compare() {
        TestCase.assertEquals("Row count incorrect", rows.size(), source.getRowCount());
        TestCase.assertEquals("Col count incorrect", colCount, source.getColumnCount());

        for (int row = 0; row < source.getRowCount(); row++) {
            ArrayList rowValues = (ArrayList) rows.get(row);
            for (int col = 0; col < colCount; col++) {
                Object expected = rowValues.get(col);
                Object actual = source.getValueAt(row, col);
                TestCase.assertEquals("Value mismatch at row: " + row + " col: " + col, expected, actual);
            }
        }
    }

    public void tableChanged(TableModelEvent e) {
        //update state from event
        if (e instanceof MultipleColumnChangeEvent) {
            processCellChanges(((MultipleColumnChangeEvent) e).getColumnChanges());
        } else if (e instanceof CellsInColumnUpdatedEvent) {
            processCellChanges(new CellsInColumnUpdatedEvent[]{(CellsInColumnUpdatedEvent) e});
        } else {
            int type = e.getType();
            if (type == TableModelEvent.INSERT) {
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                    //row insertion
                    int first = e.getFirstRow();
                    int last = e.getLastRow();
                    if (last == Integer.MAX_VALUE) {
                        last = source.getRowCount();
                    }
                    for (int row = first; row <= last; row++) {
                        ArrayList newRow = new ArrayList(colCount);
                        for (int col = 0; col < colCount; col++) {
                            newRow.add(source.getValueAt(row, col));
                        }
                        rows.add(row, newRow);
                    }
                } else {
                    //column insert
                    TestCase.assertEquals("Invalid column insertion event", TableModelEvent.HEADER_ROW, e.getLastRow());
                    for (int row = 0; row < rows.size(); row++) {
                        ArrayList rowValues = (ArrayList) rows.get(row);
                        rowValues.add(e.getColumn(), source.getValueAt(row, e.getColumn()));
                    }
                    colCount++;
                }
            } else if (type == TableModelEvent.DELETE) {
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                    //row deletion
                    int first = e.getFirstRow();
                    int last = e.getLastRow();
                    if (last == Integer.MAX_VALUE) {
                        last = source.getRowCount();
                    }
                    for (int row = last; row >= first; row--) {
                        rows.remove(row);
                    }
                } else {
                    //column delete
                    TestCase.assertEquals("Invalid column deletion event", TableModelEvent.HEADER_ROW, e.getLastRow());
                    for (int row = 0; row < rows.size(); row++) {
                        ArrayList rowValues = (ArrayList) rows.get(row);
                        rowValues.remove(e.getColumn());
                    }
                    colCount--;
                }

            } else { //update
                TestCase.assertEquals("Unknown event type", TableModelEvent.UPDATE, e.getType());

                int firstCol = e.getColumn();
                int lastCol = e.getColumn();
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
                    if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {//structure change
                        refresh();
                        return;
                    } else if (e.getLastRow() == Integer.MAX_VALUE) { //full data change
                        refresh();
                        return;
                    } else {
                        firstCol = 0;
                        lastCol = colCount;
                    }
                }
                //region update
                int firstRow = e.getFirstRow();
                int lastRow = e.getLastRow();
                for (int row = firstRow; row <= lastRow; row++) {
                    ArrayList rowValues = (ArrayList) rows.get(row);
                    for (int col = firstCol; col < lastCol; col++) {
                        rowValues.set(col, source.getValueAt(row, col));
                    }
                }
            }
            compare();
        }
        // then compare to source model
    }

    private void processCellChanges(CellsInColumnUpdatedEvent[] events) {
        for (int i = 0; i < events.length; i++) {
            CellsInColumnUpdatedEvent event = events[i];
            int col = event.getColumn();
            int[] rowsIndexes = event.getRows();
            for (int j = 0; j < rowsIndexes.length; j++) {
                int row = rowsIndexes[j];
                ArrayList rowValues = (ArrayList) rows.get(row);
                Object expectedNew = event.getNewValue();
                Object expectedOld = rowValues.set(col, expectedNew);

                TestCase.assertEquals("unexpected oldValue for event row: " + row + " col: " + col, expectedOld, event.getOldValue());
                TestCase.assertEquals("unexpected newValue for event row: " + row + " col: " + col, expectedNew, source.getValueAt(row, col));
            }
        }
    }
}
