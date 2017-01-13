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

import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.VerticalFlowLayout;
import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class contains the test GUI used during tree-table development.   
 */
public class TreeTableTest {
    private static final Log log = Log.getCategory(TreeTableTest.class);

    private static Object[] data = {new TestBean("Bond", "USD", 1, 1),
                                    new TestBean("Bond", "USD", 1, 1),
                                    new TestBean("Bond", "GBP", 1, 1),
                                    new TestBean("Bond", "GBP", 1, 1),
                                    new TestBean("Future", "GBP", 1, 1)};


    public static void main(String[] args) {

        final DefaultBeanCollection beanCollection = new DefaultBeanCollection(TestBean.class);
        beanCollection.addAll(Arrays.asList(data));

        final BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, false);
        beanCollectionTableModel.addColumnLocator("string1");
        beanCollectionTableModel.addColumnLocator("string2");
        beanCollectionTableModel.addColumnLocator("value1");
        beanCollectionTableModel.addColumnLocator("value2");


        final JTable sourceTable = new JTable(beanCollectionTableModel);

//        GroupedTableModel grouper = new GroupedTableModel(beanCollectionTableModel);
//        Filter groupedKeyTest = new Filter() {
//            public boolean evaluate(Object key) {
//                return key.equals("string1");
//            }
//        };
//        grouper.setGroupedKeyDefinition(groupedKeyTest);

        TreeTableModel grouper = new TreeTableModel(beanCollectionTableModel);
        ArrayList hierarchy = new ArrayList();
        hierarchy.add("string1");
        hierarchy.add("string2");
        grouper.setTreeHierarchy(hierarchy);


        FixedColumnTest table = new FixedColumnTest();
        table.setModel(grouper);
        table.getColumnModel().getColumn(0).setCellRenderer(new TreeTableCellRenderer());
//        TableCellRenderer depth1Renderer = table.getColumnModel().getColumn(1).getCellRenderer();
//        TableCellRenderer depth2Renderer = table.getColumnModel().getColumn(1).getCellRenderer();
//        table.getColumnModel().getColumn(1).setCellRenderer(new TreeDepthCellRenderer(depth1Renderer));

        JScrollPane groupedScroll = new JScrollPane(table);

        final JTextArea events = new JTextArea(15, 60);
        grouper.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                events.append(TableUtilities.convertToString(e) + "\n");
            }
        });
        JPanel groupedPanel = new JPanel();
        groupedPanel.add(groupedScroll);
        groupedPanel.add(table.getFixedView());
        groupedPanel.add(new JScrollPane(grouper.getUnderlyingTree()));

//        groupedPanel.add(new JScrollPane(events));

        JPanel cellChangePanel = new JPanel();
        final JTextField string1Field = new JTextField(5);
        final JTextField string2Field = new JTextField(5);
        final JTextField value1Field = new JTextField(5);
        final JTextField value2Field = new JTextField(5);

        JButton commitChange = new JButton(new AbstractAction("InsertRow") {
            public void actionPerformed(ActionEvent e) {
                try {
                    double value1 = new Double(value1Field.getText().toString()).doubleValue();
                    double value2 = new Double(value2Field.getText().toString()).doubleValue();
                    TestBean bean = new TestBean(string1Field.getText(), string2Field.getText(), value1, value2);
                    beanCollection.add(bean);
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                }
            }
        });

        JButton deleteRow = new JButton(new AbstractAction("Delete") {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = sourceTable.getSelectedRow();
                Object bean = beanCollectionTableModel.getBeanForRow(selectedRow);
                beanCollection.remove(bean);
            }
        });

        cellChangePanel.add(UIUtilities.createLabelledComponent("String1", string1Field));
        cellChangePanel.add(UIUtilities.createLabelledComponent("String2", string2Field));
        cellChangePanel.add(UIUtilities.createLabelledComponent("value1", value1Field));
        cellChangePanel.add(UIUtilities.createLabelledComponent("value2", value2Field));
        cellChangePanel.add(commitChange);
        cellChangePanel.add(deleteRow);

        JPanel mainPanel = new JPanel(new VerticalFlowLayout());
        mainPanel.add(new JLabel("SourceModel"));
        mainPanel.add(new JScrollPane(sourceTable));
        mainPanel.add(new JLabel("Grouped model"));
        mainPanel.add(groupedPanel);
        mainPanel.add(cellChangePanel);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.show();
    }


    public static class TestBean extends org.bhavaya.util.DefaultObservable {
        private String string1;
        private String string2;
        private double value1;
        private double value2;


        public TestBean(String string1, String string2, double value1, double value2) {
            this.string1 = string1;
            this.string2 = string2;
            this.value1 = value1;
            this.value2 = value2;
        }

        public String getString1() {
            return string1;
        }

        public void setString1(String string1) {
            Object oldValue = this.string1;
            this.string1 = string1;
            firePropertyChange("string1", oldValue, string1);
        }

        public String getString2() {
            return string2;
        }

        public void setString2(String string2) {
            Object oldValue = this.string2;
            this.string2 = string2;
            firePropertyChange("string2", oldValue, string2);
        }

        public double getValue1() {
            return value1;
        }

        public void setValue1(double value1) {
            double oldValue = this.value1;
            this.value1 = value1;
            firePropertyChange("value1", oldValue, value1);
        }

        public double getValue2() {
            return value2;
        }

        public void setValue2(double value2) {
            double oldValue = this.value2;
            this.value2 = value2;
            firePropertyChange("value2", oldValue, value2);
        }
    }


    private static class TreeDepthCellRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TreeTableModel model = (TreeTableModel) table.getModel();
            TreePath pathForRow = model.getUnderlyingTree().getPathForRow(row);
            int depth = pathForRow.getPathCount();
            return null;
        }
    }
}