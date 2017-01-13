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

package org.bhavaya.beans;

import org.bhavaya.db.SqlForeignKeyProperty;
import org.bhavaya.util.Generic;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;


/**
 * Description
 *
 * @author
 * @version $Revision: 1.3 $
 */
public class SchemaEditor {
    private static class AnotherGenericTableModel extends AbstractTableModel {
        private String[] propertyNames;
        private Class[] types;
        private List data;

        public AnotherGenericTableModel(String[] propertyNames, Class[] types, List data) {
            this.propertyNames = propertyNames;
            this.types = types;
            this.data = data;
        }

        public AnotherGenericTableModel(String[] propertyNames, Class[] types, Object[] data) {
            this.propertyNames = propertyNames;
            this.types = types;
            this.data = Arrays.asList(data);
        }

        public String getColumnName(int c) {
            return propertyNames[c];
        }

        public Class getColumnClass(int c) {
            return types[c];
        }

        public int getColumnCount() {
            return propertyNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public Object getValueAt(int r, int c) {
            try {
                return Generic.get(data.get(r), propertyNames[c]);
            } catch (Exception e) {
//                System.out.println("Could not retrieve " + r + " " + c);
                return null;
            }

        }

        public boolean isCellEditable(int r, int c) {
            return true;
        }

        public void setValueAt(Object o, int r, int c) {
            Generic.set(data.get(r), propertyNames[c], o);
        }

        public void setData(Object[] data) {
            this.data = Arrays.asList(data);
            fireTableDataChanged();
        }

        public List getData() {
            return data;
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        JPanel p = new JPanel();

        final Schema[] schemas = Schema.getInstances();
//        Object[] cls = new Object[schemas.length];
//        for(int i = 0; i < schemas.length; i++){
//            Schema schema = schemas[i];
//
//        }
        JList schemaList = new JList(schemas);

        final JTextField join = new JTextField();
        join.setPreferredSize(new Dimension(100, 20));
        final DefaultListModel columns = new DefaultListModel();
        JList jList = new JList(columns);
        jList.setPreferredSize(new Dimension(100, 200));
        {
            String[] propertyNames = {"name", "typeName", "columnName", "cardinality"};
            Class[] types = {String.class, String.class, String.class, Integer.class};
            final AnotherGenericTableModel tm = new AnotherGenericTableModel(propertyNames, types, new Object[]{});
            schemaList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    tm.setData(schemas[e.getFirstIndex()].getProperties());
                }
            });
            final JTable t = new JTable(tm);
            JScrollPane s = new JScrollPane(t);
            p.setLayout(new BorderLayout());
            p.add(new JScrollPane(schemaList), BorderLayout.NORTH);
            p.add(s, BorderLayout.CENTER);
            t.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    int r = t.getSelectedRow();
                    if (r != -1) {
                        Property p = (Property) tm.getData().get(r);
                        if (p instanceof SqlForeignKeyProperty) {
                            SqlForeignKeyProperty fkp = (SqlForeignKeyProperty) p;
//                            join.setText(fkp.getOriginalJoin());
                            columns.clear();
                            Object[] d = fkp.getColumns();
                            for (int i = 0; i < d.length; i++) {
                                columns.addElement(d[i]);
                            }
                        }
                    }
                }
            });
        }
        {
            JPanel detail = new JPanel();
            detail.setPreferredSize(new Dimension(200, 200));
            detail.setLayout(null);
            JLabel l1 = new JLabel("Join: ");
            l1.setBounds(10, 10, 100, 20);
            detail.add(l1);
            join.setBounds(100, 10, 100, 20);
            detail.add(join);
            JLabel l2 = new JLabel("Columns: ");
            detail.add(l2);
            l2.setBounds(10, 100, 100, 20);
            detail.add(jList);
            p.add(detail, BorderLayout.SOUTH);
            jList.setBounds(100, 100, 100, 100);
        }
        f.setContentPane(p);
        f.pack();
        f.setVisible(true);
    }
}
