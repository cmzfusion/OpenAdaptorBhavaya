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

package org.bhavaya.ui;

import org.bhavaya.util.Generic;
import org.bhavaya.ui.table.TableToolTipManager;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.EventHandler;
import java.util.List;

/**
 * Why this class?  Because not only do JLists suck in general, they also look terrible.  This incarnation of a
 * table is meant as a sort of a quasi-list aiming at emulating KDE.
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */

public class ListTable extends JTable {
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private double[] columnWidthRatios;
    private String[] columnTooltips = EMPTY_STRING_ARRAY;
    private boolean columnWidthsSet = false;

    private TableToolTipManager toolTipManager;

    public ListTable(TableModel tableModel) {
        this(tableModel, null);
    }

    public ListTable(TableModel tableModel, double[] columnWidthRatios) {
        super(tableModel);
        toolTipManager = new TableToolTipManager(this);

        if (columnWidthRatios == null) {
            int columnCount = tableModel.getColumnCount();
            columnWidthRatios = new double[columnCount];
            for (int i = 0; i < columnWidthRatios.length; i++) {
                columnWidthRatios[i] = 1.0 / columnCount;
            }
        }
        this.columnWidthRatios = columnWidthRatios;

        setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setDefaultRenderer(Object.class, getDefaultRenderer(Object.class));

        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setDefaultRenderer(new HeaderRenderer(header.getDefaultRenderer()));
    }

    public String getToolTipText(MouseEvent event) {
        return toolTipManager.getToolTipText(event);
    }

    public Point getToolTipLocation(MouseEvent event) {
        return toolTipManager.getToolTipLocation(event);
    }

    public JToolTip createToolTip() {
        return toolTipManager.createToolTip();
    }

    public void setDefaultRenderer(Class columnClass, TableCellRenderer renderer) {
        super.setDefaultRenderer(columnClass, new CellRenderer(renderer));
    }

    public void setColumnTooltips(String[] tooltips) {
        this.columnTooltips = tooltips;
    }

    public void doLayout() {
        if (!columnWidthsSet) {
            TableColumnModel columnModel = getColumnModel();
            double totalRatio = 0;
            int totalSize = getWidth();
            for (int i = 0; i < columnModel.getColumnCount() - 1; i++) {
                TableColumn column = columnModel.getColumn(i);
                column.setPreferredWidth((int) (columnWidthRatios[i] * totalSize));
                totalRatio += columnWidthRatios[i];
            }
            columnModel.getColumn(columnModel.getColumnCount() - 1).setPreferredWidth((int) (totalSize * (1 - totalRatio)));
            columnWidthsSet = true;
        }

        super.doLayout();
    }

    public static abstract class ListTableModel extends AbstractTableModel {
        public static final int EVENT_UPDATE = 0;
        public static final int EVENT_INSERT = 1;
        public static final int EVENT_DELETE = 2;

        protected String[] properties;
        protected String[] columnNames;
        protected Class[] columnsTypes;

        public ListTableModel(String[] properties, String[] columnNames, Class[] columnsTypes) {
            this.properties = properties;
            this.columnNames = columnNames;
            this.columnsTypes = columnsTypes;
        }

        public ListTableModel(String[] properties, String[] columnNames) {
            this(properties, columnNames, null);
        }

        public int getColumnCount() {
            return properties.length;
        }

        public String getColumnName(int column) {
            return (columnNames != null) ? columnNames[column] : "";
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Object objectAtRow = getObjectAtRow(rowIndex);
            if (objectAtRow == null) return null;
            return Generic.get(objectAtRow, Generic.beanPathStringToArray(properties[columnIndex]));
        }

        public Class getColumnClass(int columnIndex) {
            if (columnsTypes != null) return columnsTypes[columnIndex];
            return super.getColumnClass(columnIndex);
        }

        protected abstract Object getObjectAtRow(int rowIndex);

        public <T> T getEventBinding(Class<T> triggerClass) {
            return EventHandler.create(triggerClass, this, "fireTableDataChanged");
        }
    }

    public static class CollectionListTableModel extends ListTableModel {
        private List list;

        public CollectionListTableModel(List list, String[] properties, String[] columnNames) {
            this(list, properties, columnNames, null);
        }

        public CollectionListTableModel(List list, String[] properties, String[] columnNames, Class[] columnsTypes) {
            super(properties, columnNames, columnsTypes);
            this.list = list;
        }

        protected Object getObjectAtRow(int rowIndex) {
            return list.get(rowIndex);
        }

        public int getRowCount() {
            return list.size();
        }
    }

    public static class ListModelTableModel extends ListTableModel {
        private ListModel listModel;

        public ListModelTableModel(ListModel listModel, String[] properties, String[] columnNames) {
            this(listModel, properties, columnNames, null);
        }

        public ListModelTableModel(ListModel listModel, String[] properties, String[] columnNames, Class[] columnsTypes) {
            super(properties, columnNames, columnsTypes);
            this.listModel = listModel;
            listModel.addListDataListener(new ListDataListener() {
                public void intervalAdded(ListDataEvent e) {
                    fireTableDataChanged();
                }

                public void intervalRemoved(ListDataEvent e) {
                    fireTableDataChanged();
                }

                public void contentsChanged(ListDataEvent e) {
                    fireTableDataChanged();
                }
            });
        }

        protected Object getObjectAtRow(int rowIndex) {
            return listModel.getElementAt(rowIndex);
        }

        public int getRowCount() {
            return listModel.getSize();
        }
    }

    private class HeaderRenderer implements TableCellRenderer {
        private TableCellRenderer orig;

        public HeaderRenderer(TableCellRenderer orig) {
            this.orig = orig;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = orig.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setText(" " + l.getText());
                l.setHorizontalAlignment(JLabel.LEFT);
                if (columnTooltips.length > column) {
                    l.setToolTipText(columnTooltips[column]);
                }
            }
            return c;
        }
    }

    private class CellRenderer implements TableCellRenderer {
        private TableCellRenderer orig;

        public CellRenderer(TableCellRenderer orig) {
            this.orig = orig;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = orig.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (hasFocus) {
                ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
            }
            return c;
        }
    }
}
