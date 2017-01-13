package org.bhavaya.ui.table.column.model;

import javax.swing.table.DefaultTableModel;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 19-Sep-2009
 * Time: 12:23:47
 * To change this template use File | Settings | File Templates.
 */
public class ColumnHidingTestTableModel extends DefaultTableModel {

    public ColumnHidingTestTableModel() {
        this(new String[] { "A", "B", "C", "D"}, new Object[] { "val1", "val2", "val3", "val4"});
    }

    public ColumnHidingTestTableModel(String[] identifiers, Object[] rowData) {
        setColumnIdentifiers(identifiers);
        addRow(rowData);
    }
}
