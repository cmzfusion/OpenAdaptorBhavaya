package org.bhavaya.ui.table.column.dialog;

import org.bhavaya.ui.table.column.model.*;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 21-Sep-2009
 * Time: 12:53:29
 */
public class TestColumnEditingWindow {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(
	                    "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JFrame frame = new JFrame();

                ColumnHidingTestTableModel tableModel = new ColumnHidingTestTableModel(
                    new String[] {"A", "B", "C", "D", "E", "F"},
                    new Object[] {"A", "B", "C", "D", "E", "F"}
                );

                ColumnHidingColumnModel fixedColumnModel = new ColumnHidingColumnModel();
                fixedColumnModel.addColumn(HidableTableColumn.createColumnWithHeader("A", 0));
                fixedColumnModel.addColumn(HidableTableColumn.createColumnWithHeader("B", 1));
                fixedColumnModel.addColumn(HidableTableColumn.createColumnWithHeader("C", 2));

                ColumnHidingColumnModel scrollableColumnModel = new ColumnHidingColumnModel();
                scrollableColumnModel.addColumn(HidableTableColumn.createColumnWithHeader("D", 3));
                scrollableColumnModel.addColumn(HidableTableColumn.createColumnWithHeader("E", 4));
                scrollableColumnModel.addColumn(HidableTableColumn.createColumnWithHeader("F", 5));

                frame.getContentPane().setLayout(new GridLayout(1, 2));

                ColumnHidingJTable fixedTable = new ColumnHidingJTable(tableModel, fixedColumnModel);
                frame.getContentPane().add(new JScrollPane(fixedTable));

                ColumnHidingJTable scrollableTable = new ColumnHidingJTable(tableModel, scrollableColumnModel);
                frame.getContentPane().add(new JScrollPane(scrollableTable));

                FixedColumnTableColumns f = new FixedColumnTableColumns(
                    fixedColumnModel,
                    scrollableColumnModel
                );

                frame.setSize(new Dimension(800, 600));
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);

                ColumnManagementDialog optionsChooserDialog = new ColumnManagementDialog(null, new ColumnManagementDialogModel(f, null, null));
                optionsChooserDialog.setVisible(true);
            }
        });
    }

}
