package org.bhavaya.ui.table.column.model;

import org.bhavaya.ui.table.column.model.ColumnHidingColumnModel;

import javax.swing.*;
import javax.swing.table.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 18-Sep-2009
 * Time: 18:24:35
 * To change this template use File | Settings | File Templates.
 */
public class ColumnHidingJTableTest {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        final JTable[] table = new JTable[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(
	                    "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                JFrame frame = new JFrame();

                ColumnHidingTestTableModel t = new ColumnHidingTestTableModel();
                table[0] = new ColumnHidingJTable(t);

                JScrollPane j = new JScrollPane(table[0]);

                frame.getContentPane().add(j);
                frame.setSize(500,500);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });

        Thread.sleep(1000);

        final TableColumn[] c = new TableColumn[1];
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ColumnHidingColumnModel h = (ColumnHidingColumnModel) table[0].getColumnModel();
                c[0] = h.getColumn(1);
                h.hideColumn(c[0]);

            }
        });

        Thread.sleep(1000);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ColumnHidingColumnModel h = (ColumnHidingColumnModel) table[0].getColumnModel();
                h.showColumn(c[0]);
            }
        });
    }
}
