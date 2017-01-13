package org.bhavaya.ui.table.column.dialog;

import org.bhavaya.ui.table.column.model.FixedColumnTableColumns;
import org.bhavaya.ui.table.column.model.HidableTableColumn;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlighter;

import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * @author ebbuttn
 *
 * Adapts the columns in FixedColumnTableColumns column models to the TableModel interface
 */
public class ColumnDialogTableModel extends AbstractTableModel {

    public static final int FIXED_OR_SCROLLABLE_COLUMN_INDEX = 0;
    public static final int HIGHLIGHT_COLUMN_INDEX = 1;
    public static final int WIDTH_COLUMN_INDEX = 2;
    public static final int COLUMN_NAME_COLUMN_INDEX = 3;
    public static final int GROUP_COLUMN_INDEX = 4;
    public static final int COLUMN_PATH_COLUMN_INDEX = 5;
    private static final int COLUMN_COUNT = 6;

    private FixedColumnTableColumns fixedTableColumns;
    private ConditionalHighlighter conditionalHighlighter;

    public ColumnDialogTableModel(FixedColumnTableColumns fixedTableColumns, ConditionalHighlighter conditionalHighlighter) {
        this.fixedTableColumns = fixedTableColumns;
        this.conditionalHighlighter = conditionalHighlighter;
        TableUpdatingColumnModelListener tableUpdatingColumnModelListener = new TableUpdatingColumnModelListener();
        fixedTableColumns.getFixedColumnModel().addColumnModelListener(tableUpdatingColumnModelListener);
        fixedTableColumns.getScrollableColumnModel().addColumnModelListener(tableUpdatingColumnModelListener);
    }

    public int getRowCount() {
        return fixedTableColumns.getColumnCount();
    }

    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {
         switch(column) {
            case FIXED_OR_SCROLLABLE_COLUMN_INDEX:
                return "";
            case COLUMN_NAME_COLUMN_INDEX:
                return "Column Name";
            case GROUP_COLUMN_INDEX:
                return "Group";
            case COLUMN_PATH_COLUMN_INDEX:
                return "Column Path";
            case WIDTH_COLUMN_INDEX:
                return "Width";
            case HIGHLIGHT_COLUMN_INDEX:
                 return "";
        }
        throw new IllegalArgumentException("Cannot return a value for column " + column);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        HidableTableColumn column = getColumnAtRow(rowIndex);
        switch(columnIndex) {
            case FIXED_OR_SCROLLABLE_COLUMN_INDEX :
                return fixedTableColumns.isColIndexInFixedTable(rowIndex);
            case COLUMN_NAME_COLUMN_INDEX :
                return column.toString();
            case GROUP_COLUMN_INDEX:
                return column.getColumnGroup();
            case COLUMN_PATH_COLUMN_INDEX:
                return column.getIdentifier();
            case WIDTH_COLUMN_INDEX:
                return column.getPreferredWidth();
            case HIGHLIGHT_COLUMN_INDEX:
                return conditionalHighlighter == null || conditionalHighlighter.hasEnabledHighlightConditionsForColumn(column.getIdentifier().toString());
        }
        throw new IllegalArgumentException("Cannot return a value for column " + columnIndex);
    }

    public HidableTableColumn getColumnAtRow(int index) {
        if ((index < 0) || index >= getRowCount()) {
            throw new IllegalArgumentException("Index of the element (index : " + index + ") should be between 0 to " + (getRowCount() - 1));
        } else {
            return fixedTableColumns.getColumn(index);
        }
    }

    public int getRowForColumn(TableColumn selectedValue) {
        //rows in the column table/list map to columns in the fixedTableColumn
        return fixedTableColumns.getCombinedColumnIndex(selectedValue);
    }

    public void moveSelectionsUp(List<HidableTableColumn> selections) {
        //start from the first in the list which will fail first if cannot move up aborting the move
        doMove(selections, -1);
    }

    public void moveSelectionsDown(List<HidableTableColumn> selections) {
        //start from the last in the list which will fail first if cannot move down aborting the move
        Collections.reverse(selections);
        doMove(selections, 1);
    }

    //here we stop at the first failure, caused by trying to move a column up when it is already
    //at the top, down when already at the bottom, or between the fixed and scrollable models
    private void doMove(List<HidableTableColumn> selections, int increment) {
        for (TableColumn c : selections) {
            int index = getRowForColumn(c);

            boolean success = fixedTableColumns.moveColumn(index, index + increment);
            if ( ! success ) {
                break;
            }
        }
    }

    private class TableUpdatingColumnModelListener implements TableColumnModelListener {

        public void columnAdded(TableColumnModelEvent e) {
            fireTableDataChanged();
        }

        public void columnRemoved(TableColumnModelEvent e) {
            fireTableDataChanged();
        }

        public void columnMoved(TableColumnModelEvent e) {
            fireTableDataChanged();
        }

        public void columnMarginChanged(ChangeEvent e) {}

        public void columnSelectionChanged(ListSelectionEvent e) {}
    }
}
