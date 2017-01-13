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

import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.VerticalFlowLayout;
import org.bhavaya.ui.series.DateSeriesNew;
import org.bhavaya.util.DateUtilities;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.util.Date;


public class CategoriesTableModelTest {
    private static Object[][] data = {{DateUtilities.newDate(2002, 3, 10), "a", new Double(1)},
                                      {DateUtilities.newDate(2003, 4, 0),  "b", new Double(1)},
                                      {DateUtilities.newDate(2002, 4, 10), "c", new Double(1)},
                                      {DateUtilities.newDate(2003, 4, 0),  "d", new Double(1)},
                                      {DateUtilities.newDate(2003, 4, 0),  "e", new Double(1)}};


    private static class TestTableModel extends AbstractTableModel implements TabularBeanAssociation {
        String[] names = {"Foo", "Bar", "Baz"};
        Class[] types = {Date.class, String.class, Double.class};

        public int getRowCount() {
            return data.length;
        }

        public int getColumnCount() {
            return types.length;
        }

        public String getColumnName(int column) {
            return names[column];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= getRowCount() || columnIndex >= getColumnCount()) {
                return null;
            }
            if (rowIndex < 0 || columnIndex < 0) {
                return null;
            }
            return data[rowIndex][columnIndex];
        }

        public Class getColumnClass(int columnIndex) {
            if (columnIndex >= getColumnCount()) {
                return null;
            }
            return types[columnIndex];
        }

        public Object[] getBeansForLocation(int row, int column) {
            return new Object[]{data[row]};
        }

        public boolean isSingleBean(int rowIndex, int columnIndex) {
            return true;
        }

        public Object getColumnKey(int column) {
            return null;
        }

        public int getColumnIndex(Object columnKey) {
            return -1;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        // A hack to generate cell events
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Object oldValue = data[rowIndex][columnIndex];
            data[rowIndex][columnIndex] = aValue;
//            fireTableChanged(new TableModelCellEvent(this, rowIndex, columnIndex, oldValue, aValue));
            fireTableStructureChanged();
        }
    }


    public static void main(String[] args) {
        final TableModel dataModel = new TestTableModel();
        final SeriesTableModel categoriesModel = new SeriesTableModel(new KeyedColumnTableModelAdapter(dataModel));
        categoriesModel.setSeries(DateSeriesNew.getDefaultInstance());
        categoriesModel.setSeriesColumnKey(categoriesModel.getSourceModel().getColumnKey(0));

        JTable table = new HighlightedTable(categoriesModel, true) {
            public void createDefaultColumnsFromModel() {
                super.createDefaultColumnsFromModel();
            }
        };

        JScrollPane groupedScroll = new JScrollPane(table);

        final JTextArea events = new JTextArea(15, 60);
        categoriesModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                events.append(TableUtilities.convertToString(e) + "\n");
            }
        });
        JPanel groupedPanel = new JPanel();
        groupedPanel.add(groupedScroll);
        groupedPanel.add(new JScrollPane(events));

        JPanel cellChangePanel = new JPanel();
        final JTextField rowField = new JTextField(2);
        final JTextField colField = new JTextField(2);
        final JTextField valueField = new JTextField(10);

        JButton commitChange = new JButton(new AbstractAction("Update cell") {
            public void actionPerformed(ActionEvent e) {
                try {
                    int row = Integer.parseInt(rowField.getText());
                    int col = Integer.parseInt(colField.getText());
                    Object value = valueField.getText();
                    if (dataModel.getColumnClass(col) == Double.class) {
                        value = new Double(value.toString());
                    }

                    dataModel.setValueAt(value, row, col);
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                }
            }
        });

        cellChangePanel.add( UIUtilities.createLabelledComponent("row", rowField));
        cellChangePanel.add( UIUtilities.createLabelledComponent("col", colField));
        cellChangePanel.add( UIUtilities.createLabelledComponent("value", valueField));
        cellChangePanel.add( commitChange );

        JPanel mainPanel = new JPanel(new VerticalFlowLayout());
        mainPanel.add(new JLabel("SourceModel"));
        mainPanel.add(new JScrollPane( new JTable(dataModel) ));
        mainPanel.add(new JLabel("Grouped model"));
        mainPanel.add(groupedPanel);
//        mainPanel.add(cellChangePanel);
        mainPanel.add(new JButton(new AbstractAction("Structure"){
            public void actionPerformed(ActionEvent e) {
                categoriesModel.fireTableStructureChanged();
            }
        }));

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.show();
    }
}