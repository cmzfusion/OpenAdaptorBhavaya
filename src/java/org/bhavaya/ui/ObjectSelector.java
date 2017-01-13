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

import org.bhavaya.collection.IndexedSet;
import org.bhavaya.util.ImageIconCache;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Description
 *
 * @author
 * @version $Revision: 1.3 $
 */
public class ObjectSelector extends JPanel {
    private Class beanType;
    private String[] tableDisplayProperties;
    private String[] tableHeaderNames;
    private double[] columnWidthRatios;

    private IndexedSet availableObjects;
    private ListTable availableObjectsTable;
    private IndexedSet selectedObjects;
    private ListTable selectedObjectsTable;
    private Runnable selectedObjectsUpdateTrigger;

    public ObjectSelector(Class beanType,
                          Object[] availableObjects,
                          Object[] selectedObjects,
                          String[] tableDisplayProperties,
                          String[] tableHeaderNames,
                          double[] columnWidthRatios) {
        this.beanType = beanType;
        this.tableDisplayProperties = tableDisplayProperties;
        this.tableHeaderNames = tableHeaderNames;
        this.columnWidthRatios = columnWidthRatios;
        this.availableObjects = availableObjects != null ? new IndexedSet(Arrays.asList(availableObjects)) : new IndexedSet();
        this.selectedObjects = selectedObjects != null ? new IndexedSet(Arrays.asList(selectedObjects)) : new IndexedSet();
        initMainPanel();
    }

    private void initMainPanel() {
        final int BORDER_DISTANCE = 10;

        this.setLayout(new SpringLayout());
        SpringLayout springLayout = (SpringLayout) this.getLayout();

        ListTable.ListTableModel availableObjectsModel = new ListTable.CollectionListTableModel(availableObjects, tableDisplayProperties, tableHeaderNames);
        availableObjectsTable = new ListTable(availableObjectsModel, columnWidthRatios);
        availableObjectsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availableObjectsTable.setDefaultRenderer(Object.class, new SelectedObjectsCellRenderer());
        if (tableHeaderNames == null) availableObjectsTable.setTableHeader(null);
        JScrollPane availableObjectsScrollPane = new JScrollPane(availableObjectsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        LabelledComponent availableObjectsLabelledTable = new LabelledComponent(new JLabel("Available"), availableObjectsScrollPane);
        this.add(availableObjectsLabelledTable);

        ListTable.CollectionListTableModel selectedObjectsModel = new ListTable.CollectionListTableModel(selectedObjects, tableDisplayProperties, tableHeaderNames);
        selectedObjectsTable = new ListTable(selectedObjectsModel, columnWidthRatios);
        selectedObjectsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        if (tableHeaderNames == null) selectedObjectsTable.setTableHeader(null);
        selectedObjectsUpdateTrigger = (Runnable) selectedObjectsModel.getEventBinding(Runnable.class);
        JScrollPane selectedObjectsScrollPane = new JScrollPane(selectedObjectsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        LabelledComponent selectedObjectsLabelledTable = new LabelledComponent(new JLabel("Selected"), selectedObjectsScrollPane);
        this.add(selectedObjectsLabelledTable);

        final AddAction addAction = new AddAction();
        JButton addButton = new BhavayaButton(addAction, ImageIconCache.getImageIcon("right_arrow.gif"));
        addButton.setToolTipText("Add selection");
        availableObjectsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, ""));
                }
            }
        });

        final RemoveAction removeAction = new RemoveAction();
        JButton removeButton = new BhavayaButton(removeAction, ImageIconCache.getImageIcon("left_arrow.gif"));
        removeButton.setToolTipText("Remove selection");
        selectedObjectsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    removeAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, ""));
                }
            }
        });

        JPanel buttonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, BORDER_DISTANCE));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        this.add(buttonPanel);

        springLayout.putConstraint(SpringLayout.EAST, availableObjectsLabelledTable, Spring.constant(80, 400, 800), SpringLayout.WEST, availableObjectsLabelledTable);
        springLayout.putConstraint(SpringLayout.WEST, availableObjectsLabelledTable, BORDER_DISTANCE, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.NORTH, availableObjectsLabelledTable, BORDER_DISTANCE, SpringLayout.NORTH, this);

        springLayout.putConstraint(SpringLayout.WEST, buttonPanel, BORDER_DISTANCE / 2, SpringLayout.EAST, availableObjectsLabelledTable);
        springLayout.putConstraint(SpringLayout.SOUTH, buttonPanel, 0, SpringLayout.SOUTH, availableObjectsLabelledTable);
        springLayout.putConstraint(SpringLayout.NORTH, buttonPanel, 0, SpringLayout.NORTH, availableObjectsLabelledTable);

        springLayout.putConstraint(SpringLayout.EAST, selectedObjectsLabelledTable, Spring.constant(80, 400, 800), SpringLayout.WEST, selectedObjectsLabelledTable);
        springLayout.putConstraint(SpringLayout.WEST, selectedObjectsLabelledTable, BORDER_DISTANCE / 2, SpringLayout.EAST, buttonPanel);
        springLayout.putConstraint(SpringLayout.SOUTH, selectedObjectsLabelledTable, 0, SpringLayout.SOUTH, buttonPanel);
        springLayout.putConstraint(SpringLayout.NORTH, selectedObjectsLabelledTable, 0, SpringLayout.NORTH, buttonPanel);
        springLayout.putConstraint(SpringLayout.SOUTH, this, BORDER_DISTANCE, SpringLayout.SOUTH, selectedObjectsLabelledTable);
        springLayout.putConstraint(SpringLayout.EAST, this, BORDER_DISTANCE, SpringLayout.EAST, selectedObjectsLabelledTable);

        this.setLayout(springLayout);
    }

    private class AddAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            int[] selectedRows = availableObjectsTable.getSelectedRows();
            for (int i = 0; i < selectedRows.length; i++) {
                Object selectedObject = availableObjects.get(selectedRows[i]);
                selectedObjects.add(selectedObject);
                availableObjectsTable.getSelectionModel().removeSelectionInterval(selectedRows[i], selectedRows[i]);
            }
            selectedObjectsUpdateTrigger.run();
            availableObjectsTable.repaint();
        }
    }

    private class RemoveAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            int[] selectedRows = selectedObjectsTable.getSelectedRows();

            Object[] selectedObjects = new Object[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                selectedObjects[i] = ObjectSelector.this.selectedObjects.get(selectedRows[i]);
            }
            for (int i = 0; i < selectedObjects.length; i++) {
                ObjectSelector.this.selectedObjects.remove(selectedObjects[i]);
            }

            selectedObjectsUpdateTrigger.run();
            availableObjectsTable.repaint();
        }
    }


    private class SelectedObjectsCellRenderer extends DefaultTableCellRenderer {
        private Color normalColor;
        private Color alreadySelectedColor;

        public SelectedObjectsCellRenderer() {
            super();
            normalColor = Color.black;
            alreadySelectedColor = Color.lightGray;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component;
            if (selectedObjects.contains(availableObjects.get(row))) {
                component = super.getTableCellRendererComponent(table, value, false, hasFocus, row, column);
                component.setForeground(alreadySelectedColor);
            } else {
                component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                component.setForeground(normalColor);
            }

            return component;
        }
    }

    public Object[] getSelectedObjects() {
        Object[] objects = (Object[]) Array.newInstance(beanType, selectedObjects.size());
        return selectedObjects.toArray(objects);
    }

}
