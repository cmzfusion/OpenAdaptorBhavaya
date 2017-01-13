/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui.table;

import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * Provides toString method to get all the data from debugger and a GUI to visualize these data.
 *
 * To use, extend your table model from this class instead of AbstractTableModel.
 * Then when debugging tables (particularly chains of table models) toString() can be called for individual
 * models along the chain, to see the state of the table so far.
 *
 * Copy paste result of toString() into this class's GUI for nice output.
 *
 * @author vladimir hrmo
 * @version $Revision: 1.1 $
 */
public abstract class AbstractTestTableModel extends javax.swing.table.AbstractTableModel {

    public String toString() {
        return toString(this);
    }

    public static String toString(TableModel tableModel) {
        StringBuffer sb = new StringBuffer();
        sb.append(tableModel.getClass().getName() + ";");
        int colCount = tableModel.getColumnCount();
        for (int i = 0; i < colCount; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(tableModel.getColumnName(i));
        }
        sb.append(";");
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            if (i > 0) {
                sb.append(";");
            }
            for (int j = 0; j < colCount; j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append(tableModel.getValueAt(i, j));
            }
        }
        return sb.toString();
    }

    /**
     * Displays a GUI where you can paste the result of toString and see the content in a table.
     * @param args
     */
    public static void main(String[] args) {
        final JFrame frame = new JFrame();
        Container container = frame.getContentPane();
        container.setLayout(new BorderLayout());

        final GUITableModel tableModel = new GUITableModel();

        final JTextField textField = new JTextField();
        container.add(textField, BorderLayout.NORTH);
        textField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                tableModel.setData(textField.getText());
                frame.setTitle(tableModel.className);
            }
        });

        JTable table = new JTable(tableModel);
        container.add(new JScrollPane(table), BorderLayout.CENTER);

        container.add(new JLabel("Paste the data into the text box and press enter."), BorderLayout.SOUTH);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static class GUITableModel extends AbstractTableModel {

        String className;
        ArrayList columnNames = new ArrayList();
        ArrayList tableData = new ArrayList();

        private void setData(String data) {

            columnNames.clear();
            tableData.clear();

            // parse the data
            StringTokenizer st = new StringTokenizer(data, ";");
            if (st.hasMoreTokens()) {
                // class name
                className = st.nextToken();
            }
            if (st.hasMoreTokens()) {
                // header row
                String row = st.nextToken();
                StringTokenizer st2 = new StringTokenizer(row, ",");
                while (st2.hasMoreTokens()) {
                    String s =  st2.nextToken();
                    columnNames.add(s);
                }
            }
            while (st.hasMoreTokens()) {
                String row = st.nextToken();
                StringTokenizer st2 = new StringTokenizer(row, ",");
                ArrayList rowData = new ArrayList();
                tableData.add(rowData);
                while (st2.hasMoreTokens()) {
                    String s =  st2.nextToken();
                    rowData.add(s);
                }
            }
            fireTableStructureChanged();
        }

        public int getColumnCount() {
            return columnNames.size() + 1;
        }

        public String getColumnName(int column) {
            if (column == 0) return "Row";
            return "(" + (column-1) + ")" +(String)columnNames.get(column-1);
        }

        public int getRowCount() {
            return tableData.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex ==0) return new Integer(rowIndex);
            ArrayList row = (ArrayList) tableData.get(rowIndex);
            Object value = row.get(columnIndex-1);
            if ("null".equals(value)) return null;
            return value;
        }
    }
}

