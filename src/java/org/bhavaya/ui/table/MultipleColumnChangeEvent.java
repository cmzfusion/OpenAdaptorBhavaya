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
import javax.swing.table.TableModel;

/**
 * Simply a set of CellsInColumnChangeEvents
 * This can be used for row changes or where multiple rows update in the same manner
 * Additionally there is a flag to indicate whether each CellsInColumnChangedEvent affects the same rows (this assists with faster event processing).
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public class MultipleColumnChangeEvent extends TableModelEvent {
    private static final Log log = Log.getCategory(MultipleColumnChangeEvent.class);

    private CellsInColumnUpdatedEvent[] columnChanges;
    private boolean changedRowsSameForEachColumn;

    public MultipleColumnChangeEvent(TableModel source, CellsInColumnUpdatedEvent[] columnChanges, boolean changedRowsSameForEachColumn) {
        super(source,
                (changedRowsSameForEachColumn && columnChanges[0].getRowCount() == 1) ? columnChanges[0].getRowIndex(0) : 0,
                (changedRowsSameForEachColumn && columnChanges[0].getRowCount() == 1) ? columnChanges[0].getRowIndex(0) : Integer.MAX_VALUE,
                ALL_COLUMNS, UPDATE);
        this.columnChanges = columnChanges;
        this.changedRowsSameForEachColumn = changedRowsSameForEachColumn;
    }

    public CellsInColumnUpdatedEvent[] getColumnChanges() {
        return columnChanges;
    }

    public boolean isChangedRowsSameForEachColumn() {
        return changedRowsSameForEachColumn;
    }
}
