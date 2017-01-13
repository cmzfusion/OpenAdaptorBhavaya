package org.bhavaya.ui.table.column.model;

import org.bhavaya.ui.table.column.model.ColumnHidingColumnModel;
import org.bhavaya.ui.table.column.model.HidableTableColumn;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumn;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 19-Sep-2009
 * Time: 12:26:36
 * To change this template use File | Settings | File Templates.
 */
public class ColumnHidingJTable extends JTable {
    public ColumnHidingJTable(ColumnHidingTestTableModel t) {
        super(t);
    }

    public ColumnHidingJTable(ColumnHidingTestTableModel columnHidingTestTableModel, ColumnHidingColumnModel columnHidingColumnModel) {
        super(columnHidingTestTableModel, columnHidingColumnModel);
    }

    protected TableColumnModel createDefaultColumnModel() {
        return new ColumnHidingColumnModel();
    }

    //Override the default to create HidableTableColumn instead
    public void createDefaultColumnsFromModel() {
        TableModel m = getModel();
        if (m != null) {
            // Remove any current columns
            TableColumnModel cm = getColumnModel();
            while (cm.getColumnCount() > 0) {
                cm.removeColumn(cm.getColumn(0));
	        }

            // Create new columns from the data model info
            for (int i = 0; i < m.getColumnCount(); i++) {
                TableColumn newColumn = new HidableTableColumn(i);
                addColumn(newColumn);
            }
        }
    }
}
