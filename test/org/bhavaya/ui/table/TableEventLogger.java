package org.bhavaya.ui.table;

import org.bhavaya.ui.table.CellsInColumnUpdatedEvent;
import org.bhavaya.ui.table.MultipleColumnChangeEvent;
import org.bhavaya.ui.table.TableUtilities;
import org.bhavaya.util.Log;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;

/**
 * Add this class as a listener to table model and it prints out all the incomming events.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
public class TableEventLogger implements TableModelListener {
    private static final Log log = Log.getCategory(TableEventLogger.class);
    private String name;

    public TableEventLogger(String name) {
        this.name = name;
    }

    public void tableChanged(TableModelEvent e) {
        StringBuffer msg = new StringBuffer("\n").append(name).append(" logger: ");
        if (TableUtilities.isFullStructureChange(e)) {
            msg.append("Struture change");
        } else if (TableUtilities.isAllDataChanged(e)) {
            msg.append("Data change");
        } else {
            TableModelEvent[] subEvents;
            if (e instanceof MultipleColumnChangeEvent) {
                msg.append("Multiple column change:");
                subEvents = ((MultipleColumnChangeEvent) e).getColumnChanges();
            } else {
                subEvents = new TableModelEvent[]{e};
            }
            msg.append("\n");

            for (int i = 0; i < subEvents.length; i++) {
                TableModelEvent event = subEvents[i];
                if (event.getFirstRow() == TableModelEvent.HEADER_ROW) {
                    handleColumnInsertOrDelete(event, msg);
                } else if (event.getColumn() == TableModelEvent.ALL_COLUMNS) {
                    handleRowInsertOrDelete(event, msg);
                } else {
                    assert (event.getType() == TableModelEvent.UPDATE) : "Expeced update event";
                    ArrayList rows = new ArrayList();
                    if (event instanceof CellsInColumnUpdatedEvent) {
                        CellsInColumnUpdatedEvent cellsEvent = ((CellsInColumnUpdatedEvent) event);
                        int[] primitiveRows = cellsEvent.getRows();
                        for (int j = 0; j < primitiveRows.length; j++) {
                            rows.add(new Integer(primitiveRows[j]));
                        }
                        Object newValue = cellsEvent.getNewValue();
                        if (newValue == null && primitiveRows[0] == 5) {
                            if (log.isDebug())log.debug("break");
                        }
                        msg.append("\tcells in column changed from " + cellsEvent.getOldValue() + " to " + newValue + " rows were: " + rows);
                    } else {
                        msg.append("\tcell change first row: " + event.getFirstRow() + " last row: " + event.getLastRow());
                    }
                    msg.append(" column: " + event.getColumn());
                }
                msg.append("\n");
            }
        }
        log.info(msg);
    }

    private void handleRowInsertOrDelete(TableModelEvent event, StringBuffer msg) {
        if (event.getType() == TableModelEvent.INSERT) {
            msg.append("row insert from " + event.getFirstRow() + " to " + event.getLastRow());
        } else if (event.getType() == TableModelEvent.DELETE) {
            msg.append("row delete from " + event.getFirstRow() + " to " + event.getLastRow());
        } else {
            msg.append("row update from " + event.getFirstRow() + " to " + event.getLastRow());
        }
    }

    private void handleColumnInsertOrDelete(TableModelEvent event, StringBuffer msg) {
        if (event.getType() == TableModelEvent.INSERT) {
            msg.append("column insert " + event.getColumn());
        } else if (event.getType() == TableModelEvent.DELETE) {
            msg.append("column delete " + event.getColumn());
        } else {
            assert (false) : "Expeced insert or delete! but got " + event.getType();
        }
    }
}
