package org.bhavaya.ui.table;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Sep-2009
 * Time: 14:57:48
 *
 * Assits with saving and restoring user selections in the table during a sort/rearrangement
 * of the table data
 */
public class AnalyticsTableSelection implements ListSelectionListener {

    private AnalyticsTable table;
    private AnalyticsTableModel analyticsTableModel;
    private Set<Object> selectedBeans = new HashSet<Object>();
    private int selectedColumn;

    public AnalyticsTableSelection(AnalyticsTable table, AnalyticsTableModel analyticsTableModel) {
        this.table = table;
        this.analyticsTableModel = analyticsTableModel;
        table.getSelectionModel().addListSelectionListener(this);
    }

    public void valueChanged(ListSelectionEvent e) {
        selectedBeans.clear();
        if ( ! e.getValueIsAdjusting() && e.getFirstIndex() != -1 && canRestoreSelections()) {
            int[] selectedRows = table.getSelectedRows();
            Object[] selectedObjects;
            for ( int selectedRow : selectedRows) {
                selectedObjects = analyticsTableModel.getBeansForLocation(selectedRow, 0);
                if ( selectedObjects.length == 1 ) {
                    selectedBeans.add(selectedObjects[0]);
                }
            }
            selectedColumn = Math.max(0, table.getSelectedColumnIndex());
        }
    }

    public int getSelectedColumn() {
        return selectedColumn;
    }

    public HashSet<Object> getSelectionSnapshot() {
        return new HashSet<Object>(selectedBeans);
    }

    public void restoreSelections(Set<Object> selectionSnapshot, int selectedColumn) {
        final ArrayList<Integer> rowsToRestore = new ArrayList<Integer>();
        if ( canRestoreSelections() ) {
            for ( int row = 0; row < analyticsTableModel.getRowCount(); row ++) {
                Object[] o = analyticsTableModel.getBeansForLocation(row, 0);
                if ( o.length == 1 && selectionSnapshot.contains(o[0])) {
                    rowsToRestore.add(row);
                }
            }
        }

        if ( rowsToRestore.size() > 0) {
            table.setSelectedRows(rowsToRestore);
            table.setSelectedColumn(selectedColumn);
        }
    }

    private boolean canRestoreSelections() {
        return table.getColumnCount() > 0 && ! analyticsTableModel.isPivoted() && ! analyticsTableModel.isGrouped();
    }
}
