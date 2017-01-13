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

import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import java.beans.PropertyChangeEvent;

/**
 * Represents a change of the form:
 * a column was updated
 * a set of rows within that column all changed from the given old value, to the given new value.
 *
 * This event therefore covers single cell changes, and sets of identicle cell changes in a column
 * this second case occurs rather frequently as a result of tables naturally denormalising shared objects
 * (e.g. changes to the name of an object that appears in many rows)
 *
 * I'm sorry about the whole "cause" thing. For some profiling and debugging stuff, I wanted to enable access to the event that caused the
 * cell change, I know this may have a detrimental effect on garbage collection etc..
 * Maybe I'll clean it up one day.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.3 $
 */
public class CellsInColumnUpdatedEvent extends TableModelEvent {
    private int[] multipleRows;
    private Object eventCause;
    private Object oldValue;
    private Object newValue;

    public CellsInColumnUpdatedEvent(TableModel source, int row, int column, Object oldValue, Object newValue) {
        super(source, row, row, column, UPDATE);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public CellsInColumnUpdatedEvent(TableModel source, int[] rows, int column, Object oldValue, Object newValue) {
        this(source, rows, column);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public CellsInColumnUpdatedEvent(TableModel source, int row, int column, PropertyChangeEvent cause) {
        super(source, row, row, column, UPDATE);
        this.eventCause = cause;
        this.oldValue = cause.getOldValue();
        this.newValue = cause.getNewValue();
    }

    public CellsInColumnUpdatedEvent(TableModel source, int row, int column, CellsInColumnUpdatedEvent cause) {
        super(source, row, row, column, UPDATE);
        this.eventCause = cause;
        this.oldValue = cause.getOldValue();
        this.newValue = cause.getNewValue();
    }

    public CellsInColumnUpdatedEvent(TableModel source, int[] rows, int column, PropertyChangeEvent cause) {
        this(source, rows, column);
        this.eventCause = cause;
        this.oldValue = cause.getOldValue();
        this.newValue = cause.getNewValue();
    }

    public CellsInColumnUpdatedEvent(TableModel source, int[] rows, int column, CellsInColumnUpdatedEvent cause) {
        this(source, rows, column);
        this.eventCause = cause;
        this.oldValue = cause.getOldValue();
        this.newValue = cause.getNewValue();
    }

    protected CellsInColumnUpdatedEvent(TableModel source, int[] rows, int column) {
        super(source, (rows.length == 1) ? rows[0] : 0, (rows.length == 1) ? rows[0] : Integer.MAX_VALUE, column, UPDATE);
        assert (rows.length != 0) : "should not create events with noo rows";
        if (rows.length > 1) {
            this.multipleRows = rows;
        }
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public int getRowCount() {
        return multipleRows == null ? 1 : multipleRows.length;
    }

    public int getRowIndex(int i) {
        if (multipleRows==null) {
            if (i!=0) throw new IndexOutOfBoundsException("Only have 1 row, but index: "+i+" was asked for");
            return getFirstRow();
        }
        return multipleRows[i];
    }

    public int[] getRows() {
        if (multipleRows != null) {
            return multipleRows;
        }else{
            return new int[]{getFirstRow()};
        }
    }

    /**
     * could be a propertyChangeEvent or a CellsInColumnUpdatedEvent
     */
    public Object getEventCause() {
        return eventCause;
    }
}
