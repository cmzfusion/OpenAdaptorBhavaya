package org.bhavaya.ui.table;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.4 $
 */
public class TableUtilities {
    public static boolean isFullStructureChange(TableModelEvent e) {
        if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
            if (e instanceof MultipleColumnChangeEvent) return false;
            return (e.getFirstRow() == TableModelEvent.HEADER_ROW);
        }
        return false;
    }

    /**
     * was all the data blown away (this includes a structure change)
     */
    public static boolean isAllDataChanged(TableModelEvent e) {
        if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
            if (e instanceof MultipleColumnChangeEvent) return false;

            int firstRow = e.getFirstRow();
            return (firstRow == TableModelEvent.HEADER_ROW ||
                    (firstRow == 0 && e.getLastRow() == Integer.MAX_VALUE));
        }
        return false;
    }

    public static boolean isColumnDefinitionChange(TableModelEvent e) {
        return (e.getFirstRow() == TableModelEvent.HEADER_ROW);
    }

    public static boolean isColumnCountChange(TableModelEvent e) {
        if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            return (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE);
        }
        return false;
    }

    public static boolean isRowInsert(TableModelEvent e) {
        return (e.getType() == TableModelEvent.INSERT && e.getFirstRow() >= 0);
    }

    public static boolean isRowDelete(TableModelEvent e) {
        return (e.getType() == TableModelEvent.DELETE && e.getFirstRow() >= 0);
    }

    /**
     * values changed, but the dimentions did not
     */
    public static boolean isDimensionUnchanged(TableModelEvent e) {
        if (e instanceof CellsInColumnUpdatedEvent || e instanceof MultipleColumnChangeEvent) {
            return true;
        } else {
            return (e.getFirstRow() >= 0 && e.getLastRow() != Integer.MAX_VALUE && e.getType() == TableModelEvent.UPDATE);
        }
    }

    public static boolean isCellInColumnUpdated(TableModelEvent e, int columnId) {
        if (e.getType() != TableModelEvent.UPDATE) return false;
        if (e.getColumn() == columnId) return true;
        if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
            if (e instanceof MultipleColumnChangeEvent) {
                CellsInColumnUpdatedEvent[] columnChanges = ((MultipleColumnChangeEvent) e).getColumnChanges();
                for (int i = 0; i < columnChanges.length; i++) {
                    CellsInColumnUpdatedEvent columnChange = columnChanges[i];
                    if (columnChange.getColumn() == columnId) return true;
                }
            }
        }
        return false;
    }

    public static boolean isRowCountChanged(TableModelEvent e) {
        if (e.getColumn() == TableModelEvent.ALL_COLUMNS && !(e instanceof MultipleColumnChangeEvent)) {
            if (e.getType() == TableModelEvent.UPDATE) {
                return (e.getLastRow() == Integer.MAX_VALUE);
            }
            return true;
        } else {
            return false;
        }
    }

    public static TableModelEvent remapCoordinates(TableTransform tableTransform, TableModelEvent e) {
        assert (isDimensionUnchanged(e)) : "Cannot remap events when dimension has changed";
        TableModelEvent newEvent;
        if (e instanceof MultipleColumnChangeEvent) {
            MultipleColumnChangeEvent multiEvent = (MultipleColumnChangeEvent) e;
            CellsInColumnUpdatedEvent[] columnChanges = multiEvent.getColumnChanges();
            CellsInColumnUpdatedEvent[] newColumnChanges = new CellsInColumnUpdatedEvent[columnChanges.length];

            for (int i = 0; i < columnChanges.length; i++) {
                CellsInColumnUpdatedEvent columnChange = columnChanges[i];
                newColumnChanges[i] = remapCoordinates(tableTransform, columnChange);
            }

            newEvent = new MultipleColumnChangeEvent(tableTransform.getModel(), columnChanges, multiEvent.isChangedRowsSameForEachColumn());
        } else if (e instanceof CellsInColumnUpdatedEvent) {
            newEvent = remapCoordinates(tableTransform, (CellsInColumnUpdatedEvent) e);
        } else {
            throw new UnsupportedOperationException("Dan, implement me!");
        }
        return newEvent;
    }

    public static CellsInColumnUpdatedEvent remapCoordinates(TableTransform tableTransform, CellsInColumnUpdatedEvent e) {
        int rowCount = e.getRowCount();
        int[] newRows = new int[rowCount];
        boolean madeChange = false;
        int deleteCount = 0;

        for (int i = 0; i < rowCount; i++) {
            int row = e.getRowIndex(i);
            int newRow = tableTransform.mapSourceRowToRow(row);
            if (newRow != row) {
                madeChange = true;
                if (newRow < 0) deleteCount++;
            }
            newRows[i] = newRow;
        }
        //remove all rows that were -1
        if (deleteCount > 0) {
            if (deleteCount == rowCount) return null;

            int[] tmpRows = new int[newRows.length - deleteCount];
            int index = 0;
            for (int i = 0; i < newRows.length; i++) {
                if (newRows[i] >= 0) {
                    tmpRows[index++] = newRows[i];
                    if (index == deleteCount) break; //break for loop
                }
            }
            newRows = tmpRows;
        }

        int column = e.getColumn();
        int newColumn = tableTransform.mapSourceColumnToColumn(column);
        if (column != newColumn || madeChange) {
            return new CellsInColumnUpdatedEvent(tableTransform.getModel(), newRows, newColumn, e);
        } else {
            return e;
        }
    }

    public static String convertToString(TableModelEvent e) {
        StringBuffer buf = new StringBuffer();
        String type = "";
        switch (e.getType()) {
            case TableModelEvent.INSERT:
                type = "INSERT";
                break;
            case TableModelEvent.DELETE:
                type = "DELETE";
                break;
            case TableModelEvent.UPDATE:
                type = "UPDATE";
                break;
        }
        buf.append("Type: ").append(type);
        buf.append(" firstRow: ").append(e.getFirstRow());
        buf.append(" lastRow: ").append(e.getLastRow());
        buf.append(" Col: ").append(e.getColumn());
        if (e instanceof CellsInColumnUpdatedEvent) {
            CellsInColumnUpdatedEvent event = (CellsInColumnUpdatedEvent) e;
            buf.append(" Oldvalue: ").append(event.getOldValue());
            buf.append(" Newvalue: ").append(event.getNewValue());
        }
        return buf.toString();
    }

    /**
     * Sets the width of the table columns to fit in the table data.
     * It takes into account preffered width of the columns and data in row within given range.
     */
    public static void initColumnSizes(JTable table, int rowStart, int rowEnd) {
        boolean containsData = table.getRowCount() > 0;
        Component cellRendererComponent;
        int headerWidth;
        int cellWidth;
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

        for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
            TableColumn column = table.getColumnModel().getColumn(columnIndex);

            cellRendererComponent = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, 0);
            headerWidth = cellRendererComponent.getPreferredSize().width;

            cellWidth = 0;
            if (containsData) {
                for (int row = rowStart; row < rowEnd; row++) {
                    try {
                        TableCellRenderer tableCellRenderer = table.getCellRenderer(row, columnIndex);
                        cellRendererComponent = tableCellRenderer.getTableCellRendererComponent(
                                table,
                                table.getValueAt(row, columnIndex),
                                false, false, row, columnIndex);
                        cellWidth = Math.max(cellWidth, cellRendererComponent.getPreferredSize().width);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }

            int width = Math.max(headerWidth, cellWidth) + 7;
            column.setPreferredWidth(width);
            column.setWidth(width);
        }
    }
}
