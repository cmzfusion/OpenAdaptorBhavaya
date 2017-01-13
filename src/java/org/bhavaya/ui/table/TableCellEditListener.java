package org.bhavaya.ui.table;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 30/01/14
 * Time: 17:34
 */

import javax.swing.JTable;
public interface TableCellEditListener {

    void editCellStarted(JTable table,
                         int row,
                         int column,
                         int selectedRows[],
                         Object oldValue);

    void editCellCompleted(JTable table,
                           int row,
                           int column,
                           int selectedRows[],
                           Object oldValue,
                           Object newValue);

    void editCancelled(JTable table);
}