package org.bhavaya.ui.table;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 22-Apr-2008
 * Time: 10:18:45
 *
 * A utility class to help pin down problems with analytics table processing column changes
 */
public class AnalyticsTableColumnChangeTest {
    private static ColumnChangeModel columnChangeModel;


    public static void main(String[] args ) {

        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                columnChangeModel = new ColumnChangeModel();
                columnChangeModel.randomize();

                AnalyticsTableModel analyticsModel = new AnalyticsTableModel(columnChangeModel);
                AnalyticsTable analyticsTable = new AnalyticsTable(analyticsModel, false);
                JFrame frame = new JFrame();
                frame.getContentPane().add(analyticsTable);
                frame.pack();
                frame.setVisible(true);

                JTable table = new JTable(analyticsModel);
                JFrame frame2 = new JFrame();
                frame2.getContentPane().add(new JScrollPane(table));
                frame2.pack();
                frame2.setVisible(true);
            }
        });

        Timer t = new Timer(2000, new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                columnChangeModel.randomize();
            }
        });
        t.start();

    }


    public static class ColumnChangeModel extends AbstractTableModel implements KeyedColumnTableModel {

        private Object[] colData = new Object[0];
        private String[] colNames = new String[0];
        private String[] AVAIL_NAMES = new String[] {"Col1", "Col2", "Col3", "Col4", "Col5", "Col6", "Col7", "Col8", "Col9", "Col10"};

        public int getRowCount() {
            return 1;
        }

        public int getColumnCount() {
            return colData.length;
        }

        public String getColumnName(int columnIndex) {
            return colNames[columnIndex];
        }

        public Class<?> getColumnClass(int columnIndex) {
            return colData[columnIndex].getClass();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return colData[columnIndex];
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        private void randomize() {
            int colNameStart = (int)(Math.random() * 10);
            int colCount =(int)(Math.random() * 10);
            colData = new Object[colCount];
            colNames = new String[colCount];
            for ( int col = 0; col < colCount; col ++ ) {
                int dataType = (int)(Math.random() * 2);
                if ( dataType == 0 ) {
                    colData[col] = new Double(Math.random() * 10);
                } else {
                    colData[col] = "String Data";
                }
                int mod = colNameStart++ % AVAIL_NAMES.length;
                colNames[col] = AVAIL_NAMES[mod];
            }

            printColNames();
            fireTableStructureChanged();
        }

        private void printColNames() {
            StringBuilder colNamesBuilder = new StringBuilder();
            for ( String colName : colNames) {
                colNamesBuilder.append(colName + ";");
            }
            System.out.println(colNamesBuilder);
        }

        public int getColumnIndex(Object columnKey) {
            int result = -1;
            for ( int loop=0; loop < colNames.length; loop ++ ) {
                if ( colNames[loop].equals(columnKey)) {
                    result = loop;
                    break;
                }
            }
            return result;
        }

        public Object getColumnKey(int index) {
            return getColumnName(index);
        }
    }
}
