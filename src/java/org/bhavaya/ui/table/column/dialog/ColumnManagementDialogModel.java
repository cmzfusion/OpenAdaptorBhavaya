package org.bhavaya.ui.table.column.dialog;

import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.table.column.model.FixedColumnTableColumns;
import org.bhavaya.ui.table.column.model.HidableTableColumn;
import org.bhavaya.ui.table.column.model.ColumnGroup;
import org.bhavaya.ui.table.formula.conditionalhighlight.ConditionalHighlighter;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: 364045
 * Date: Aug 31, 2006
 * Time: 9:35:42 AM
 *
 * The ui model classes and operations required by the column management dialog and panels
 */
public class ColumnManagementDialogModel {

    private ListSelectionModel columnListSelectionModel = new DefaultListSelectionModel();
    private ColumnDialogTableModel columnDialogTableModel;
    private FixedColumnTableColumns fixedColumnTableColumns;
    private BeanCollectionTableModel beanCollectionTableModel;

    public ColumnManagementDialogModel(FixedColumnTableColumns fixedColumnTableColumns, ConditionalHighlighter conditionalHighlighter, BeanCollectionTableModel beanCollectionTableModel) {
        this.fixedColumnTableColumns = fixedColumnTableColumns;
        this.beanCollectionTableModel = beanCollectionTableModel;
        columnDialogTableModel = new ColumnDialogTableModel(fixedColumnTableColumns, conditionalHighlighter);
    }

    public void moveSelectionsUp() {
        List<HidableTableColumn> selections = getSelectedColumns();
        columnDialogTableModel.moveSelectionsUp(selections);
        restoreSelectionsAfterMove(selections);
    }

    public void moveSelectionsDown() {
        List<HidableTableColumn> selections = getSelectedColumns();
        columnDialogTableModel.moveSelectionsDown(selections);
        restoreSelectionsAfterMove(selections);
    }

    private void restoreSelectionsAfterMove(List<HidableTableColumn> selectedValues) {
        columnListSelectionModel.clearSelection();
        for (TableColumn selectedValue : selectedValues) {
            int index = columnDialogTableModel.getRowForColumn(selectedValue);
            columnListSelectionModel.addSelectionInterval(index, index);
        }
    }

    public void removeSelections() {
        List<HidableTableColumn> selections = getSelectedColumns();
        List<Integer> selectedIndexes = convertColumnsToIndexes(selections);
        for(HidableTableColumn column : selections) {
            beanCollectionTableModel.removeColumnLocator(column.getIdentifier().toString());
        }
        restoreSelectionsAfterRemove(selectedIndexes);
    }

    private List<Integer> convertColumnsToIndexes(List<HidableTableColumn> selections) {
        List<Integer> result = new ArrayList<Integer>(selections.size());
        for(HidableTableColumn column : selections) {
            int row = columnDialogTableModel.getRowForColumn(column);
            if(row >= 0) {
                result.add(row);
            }
        }
        return result;
    }

    private void restoreSelectionsAfterRemove(List<Integer> selectedIndexes) {
        columnListSelectionModel.clearSelection();
        for (Integer index : selectedIndexes) {
            if(index < columnDialogTableModel.getRowCount()) {
                columnListSelectionModel.addSelectionInterval(index, index);
            }
        }
    }

    public List<HidableTableColumn> getSelectedColumns() {
        List<HidableTableColumn> selectedValues = new ArrayList<HidableTableColumn>();
        int count = columnDialogTableModel.getRowCount();
        for (int i = 0; i < count; i++) {
            if (columnListSelectionModel.isSelectedIndex(i)) {
                selectedValues.add(columnDialogTableModel.getColumnAtRow(i));
            }
        }
        return selectedValues;
    }

    public void selectRowsWithGroup(ColumnGroup g) {
        for ( int row = 0; row < columnDialogTableModel.getRowCount(); row ++) {
            if ( g.equals(columnDialogTableModel.getColumnAtRow(row).getColumnGroup())) {
                columnListSelectionModel.addSelectionInterval(row, row);
            }
        }
    }

    public TableModel getColumnTableModel() {
        return columnDialogTableModel;
    }

    public ListSelectionModel getColumnListSelectionModel() {
        return columnListSelectionModel;
    }

    public FixedColumnTableColumns getFixedColumnTableColumns() {
        return fixedColumnTableColumns;
    }
}
