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

package org.bhavaya.ui.dataset;

import org.bhavaya.beans.Column;
import org.bhavaya.beans.Property;
import org.bhavaya.beans.Schema;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.EnumerationCriterion;
import org.bhavaya.collection.IndexedSet;
import org.bhavaya.db.SQL;
import org.bhavaya.ui.*;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This is a redo of work of an anonymous author who left this message: 'Help!  This is mess!'
 * Attempted to clear the mess and put some more logic into data loading so as not all data
 * with high quantity are necessarily loaded.
 *
 *
 * @author Vladimir Hrmo (redo of a work of an anonymous author)
 * @version $Revision: 1.12.4.2 $
 */
public class EnumerationCriterionEditor extends CardPanel implements CriterionPanel.CriterionView {
    private static final Log log = Log.getCategory(EnumerationCriterionEditor.class);

    private static ImageIcon BLANK_ICON = ImageIconCache.getImageIcon("blank16.gif");
    private static ImageIcon BUSY_ICON = ImageIconCache.getImageIcon("hourglass.gif");
    private static final TaskQueue loadQueue = new TaskQueue("EnumCriterionPanel");

    static {
        loadQueue.start();
    }

    private LoadQueueTask lastLoadQueueTask;

    private LoadingPanel loadingPanel;
    private EnumerationCriterion initialCriterion;

    public EnumerationCriterionEditor() {
        createInitialGUI();
    }

    public EnumerationCriterionEditor(EnumerationCriterion initialCriterion,
                                      String searchProperty,
                                      String searchPropertyDisplayName,
                                      String[] tableDisplayProperties,
                                      String[] tableHeaderNames,
                                      double[] columnWidthRatios,
                                      String sqlFromSearchProperty,
                                      String sqlFromId,
                                      StringRenderer renderer) {
        createInitialGUI();
        runInLoadQueue(new LoadQueueTask(initialCriterion,
                searchProperty,
                searchPropertyDisplayName,
                tableDisplayProperties,
                tableHeaderNames,
                columnWidthRatios,
                sqlFromSearchProperty,
                ApplicationProperties.substituteApplicationProperties(sqlFromId),
                renderer));
    }

    private void createInitialGUI() {
        assert EventQueue.isDispatchThread() : "Must run on EDT.";
        if (loadingPanel == null) {
            loadingPanel = new LoadingPanel("Loading data...");
            addComponent(loadingPanel);
            setSelectedComponent(loadingPanel);
        }
    }

    public synchronized void setSelectedComponent(Component component) {
        super.setSelectedComponent(component);
    }

    public synchronized Component getSelectedComponent() {
        return super.getSelectedComponent();
    }

    private void displayCriteriaPanel(final EnumerationCriterionPanel criterionPanel) {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                Component previousSelectedComponent = getSelectedComponent();
                addComponent(criterionPanel);
                setSelectedComponent(criterionPanel);
                loadingPanel.dispose();
                if (previousSelectedComponent != null && previousSelectedComponent != loadingPanel) {
                    remove(previousSelectedComponent);
                }
            }
        });
    }

    private void displayLoadingPanel() {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                synchronized (EnumerationCriterionEditor.this) {
                    if (getSelectedComponent() != loadingPanel) {
                        loadingPanel.setProgressModel(null); // this will set it to Indeterminate
                        setSelectedComponent(loadingPanel);
                    }
                }
            }
        });
    }

    private void displayError() {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                if (loadingPanel != null) loadingPanel.dispose();
                JComponent errorView = UIUtilities.createErrorComponent("<html>There was a problem loading the data.<br>Please go to the Help/Diagnotics menu and send a report to support</html>");
                addComponent(errorView);
                setSelectedComponent(errorView);
            }
        });
    }

    public void setCriterion(Criterion criterion) {
        this.initialCriterion = (EnumerationCriterion) criterion;
        displayLoadingPanel();
        runInLoadQueue(new LoadQueueTask((EnumerationCriterion) criterion));
    }

    public Criterion createCriterion() throws CriterionPanel.ValidationException {
        Component selectedComponent;
        synchronized (this) {
            selectedComponent = getSelectedComponent();
        }
        if (selectedComponent == loadingPanel) {
            return initialCriterion;
        } else if (selectedComponent instanceof EnumerationCriterionPanel) {
            EnumerationCriterionPanel criterionPanel = (EnumerationCriterionPanel) selectedComponent;
            return criterionPanel.createCriterion();
        } else {
            return null;
        }
    }

    public void runInLoadQueue(final LoadQueueTask task) {
        synchronized (this) {
            if (lastLoadQueueTask != null) {
                lastLoadQueueTask.cancel();
            }
            lastLoadQueueTask = task;
        }
        if (EventQueue.isDispatchThread()) {
            loadQueue.addTask(task);
        } else {
            task.run();
        }
    }

    /**
     * Task that runs on a background thread and preloads data for the criterion editor
     */
    private class LoadQueueTask extends Task {

        private boolean cancelled = false;

        private EnumerationCriterion initialCriterion;
        private String searchProperty;
        private String searchPropertyDisplayName;
        private String[] tableDisplayProperties;
        private String[] tableHeaderNames;
        private double[] columnWidthRatios;
        private String sqlFromSearchProperty;
        private String sqlFromId;
        private StringRenderer renderer;
        private boolean showAll;

        public LoadQueueTask(EnumerationCriterion initialCriterion) {
            super("Loading enumeration data");
            this.initialCriterion = initialCriterion;
        }

        public LoadQueueTask(EnumerationCriterion initialCriterion,
                             String searchProperty,
                             String searchPropertyDisplayName,
                             String[] tableDisplayProperties,
                             String[] tableHeaderNames,
                             double[] columnWidthRatios,
                             String sqlFromSearchProperty,
                             String sqlFromId,
                             StringRenderer renderer) {

            super("Loading enumeration data");
            this.initialCriterion = initialCriterion;
            this.searchProperty = searchProperty;
            this.searchPropertyDisplayName = searchPropertyDisplayName;
            this.tableDisplayProperties = tableDisplayProperties;
            this.tableHeaderNames = tableHeaderNames;
            this.columnWidthRatios = columnWidthRatios;
            this.sqlFromSearchProperty = sqlFromSearchProperty;
            this.sqlFromId = sqlFromId;
            this.renderer = renderer;
        }

        public void run() {
            synchronized (this) {
                if (cancelled) return;
            }
            try {
                runTask();
            } catch (Throwable t) {
                log.error("Loading enumeration data task failed.", t);
                displayError();
            }
        }

        public synchronized void cancel() {
            cancelled = true;
        }

        public synchronized boolean isCancelled() {
            return cancelled;
        }

        public void runTask() {
            if (searchProperty == null) {
                initializeProperties(initialCriterion);
            }

            final EnumerationCriterionPanel criterionPanel = new EnumerationCriterionPanel(initialCriterion,
                    searchProperty,
                    searchPropertyDisplayName,
                    tableDisplayProperties,
                    tableHeaderNames,
                    columnWidthRatios,
                    sqlFromSearchProperty,
                    sqlFromId,
                    showAll,
                    renderer);

            if (!isCancelled()) {
                displayCriteriaPanel(criterionPanel);
            }
        }

        private void initializeProperties(EnumerationCriterion initialCriterion) {
            Column enumDescriptionColumn = initialCriterion.getEnumDescriptionColumn();

            searchProperty = enumDescriptionColumn.getName();
            searchPropertyDisplayName = initialCriterion.getName();
            tableDisplayProperties = new String[]{searchProperty};
            columnWidthRatios = new double[]{1};
            renderer = new GenericStringRenderer(searchProperty);
            showAll = true;

            if (initialCriterion.getEnumSql() != null) {
                Column[] enumKeyColumns = initialCriterion.getEnumKeyColumns();
                SQL enumSql = new SQL(initialCriterion.getEnumSql(), initialCriterion.getDatasource());

                if (enumDescriptionColumn.getType() == String.class) {
                    sqlFromSearchProperty = enumSql.joinWhereClause(enumDescriptionColumn.getRepresentation() + " LIKE  ?").getStatementString();

                    String enumWhereClause = "";
                    for (int i = 0; i < enumKeyColumns.length; i++) {
                        if (i > 0) enumWhereClause += " AND ";
                        enumWhereClause += enumKeyColumns[i].getRepresentation() + " = ?";
                    }

                    sqlFromId = enumSql.joinWhereClause(enumWhereClause).getStatementString();

                    if (Schema.hasInstance(initialCriterion.getToBeanType())) {
                        Schema schema = Schema.getInstance(initialCriterion.getToBeanType());
                        showAll = !Schema.HIGH.equals(schema.getDataQuantity());
                    }
                } else {
                    // cant load values using LIKE, will load all values, need change the search property to a bean property rather than a db column
                    if (Schema.hasInstance(initialCriterion.getToBeanType())) {
                        Property[] properties = Schema.getInstance(initialCriterion.getToBeanType()).getPropertiesByColumn(enumDescriptionColumn);
                        if (properties != null && properties.length > 0) {
                            searchProperty = properties[0].getName();
                        }
                    }
                }
            }
        }
    }

    /**
     * The criteria editor panel.
     */
    private class EnumerationCriterionPanel extends JPanel {

        private String searchProperty;

        private EnumerationCriterion initialCriterion;
        private NarrowableListModel availableRecords;
        private ListTable availableRecordsTable;
        private PlainDocument availableRecordsNarrowingDocument;
        private IndexedSet selectedRecords;
        private HashSet selectedKeys;
        private ListTable selectedRecordsTable;
        private Runnable selectedRecordsUpdateTrigger;
        private JComboBox operatorComboBox;
        private JLabel hourGlassLabel;

        private Transform keyToRecordTransform;
        private Transform recordToKeyTransform;

        public EnumerationCriterionPanel(EnumerationCriterion initialCriterion, String searchProperty, String searchPropertyDisplayName, String[] tableDisplayProperties, String[] tableHeaderNames, double[] columnWidthRatios, String sqlFromSearchProperty, String sqlFromId, boolean showAll, StringRenderer renderer) {
            this.initialCriterion = initialCriterion;
            this.searchProperty = searchProperty;
            initialize(sqlFromId, sqlFromSearchProperty, showAll, renderer);
            createGUI(searchPropertyDisplayName, tableDisplayProperties, tableHeaderNames, columnWidthRatios);
            updateSelectedFromCriterion();
        }

        private void initialize(String sqlFromId, String sqlFromSearchProperty, boolean showAll, StringRenderer renderer) {
            if (sqlFromId != null) {
                keyToRecordTransform = new Transform.KeyToRecord(initialCriterion.getDatasource(), ApplicationProperties.substituteApplicationProperties(sqlFromId));
                recordToKeyTransform = new Transform.RecordToKey(initialCriterion.getEnumKeyColumns());
            } else {
                keyToRecordTransform = new Transform.KeyToBean(initialCriterion.getToBeanType(), initialCriterion.getDatasource());
                recordToKeyTransform = new Transform.BeanToKey(initialCriterion.getToBeanType(), initialCriterion.getDatasource());
            }

            if (sqlFromSearchProperty != null) {
                availableRecords = new DBNarrowableListModel(initialCriterion.getDatasource(), ApplicationProperties.substituteApplicationProperties(sqlFromSearchProperty), showAll);
            } else {
                availableRecords = new NarrowableListModel(initialCriterion.getAllBeans(), showAll);
            }

            availableRecords.setRenderer(renderer);
            availableRecords.addListDataListener(new ListDataListener() {
                public void contentsChanged(ListDataEvent e) {
                    hourGlassLabel.setIcon(BLANK_ICON);
                }

                public void intervalAdded(ListDataEvent e) {
                }

                public void intervalRemoved(ListDataEvent e) {
                }
            });
            selectedRecords = new IndexedSet();
            selectedKeys = new HashSet();
        }

        private void createGUI(String searchPropertyDisplayName, String[] tableDisplayProperties, String[] tableHeaderNames, double[] columnWidthRatios) {
            final int BORDER_DISTANCE = 10;

            SpringLayout springLayout = new SpringLayout();
            setLayout(springLayout);

            availableRecordsNarrowingDocument = new PlainDocument();

            JTextField searchTextField = new JTextField(availableRecordsNarrowingDocument, "", 6);
            JLabel searchTextFieldLabel = new JLabel(searchPropertyDisplayName != null ? searchPropertyDisplayName : "");
            add(searchTextFieldLabel);
            add(searchTextField);

            operatorComboBox = new JComboBox(new String[]{EnumerationCriterion.ENUM_IN_OPERATION, EnumerationCriterion.ENUM_NOT_IN_OPERATION});
            operatorComboBox.setSelectedItem(initialCriterion.getOperator());
            add(operatorComboBox);

            hourGlassLabel = new JLabel(BLANK_ICON);
            add(hourGlassLabel);

            ListTable.ListTableModel availableRecordsModel = new ListTable.ListModelTableModel(availableRecords, tableDisplayProperties, tableHeaderNames);
            availableRecordsTable = new ListTable(availableRecordsModel, columnWidthRatios);
            availableRecordsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            availableRecordsTable.setDefaultRenderer(Object.class, new SelectedRecordsCellRenderer());
            if (tableHeaderNames == null) availableRecordsTable.setTableHeader(null);
            JScrollPane availableRecordsScrollPane = new JScrollPane(availableRecordsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            LabelledComponent availableRecordsLabelledTable = new LabelledComponent(new JLabel("Available"), availableRecordsScrollPane);
            add(availableRecordsLabelledTable);

            ListTable.CollectionListTableModel selectedRecordsModel = new ListTable.CollectionListTableModel(selectedRecords, tableDisplayProperties, tableHeaderNames);
            selectedRecordsTable = new ListTable(selectedRecordsModel, columnWidthRatios);
            selectedRecordsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            if (tableHeaderNames == null) selectedRecordsTable.setTableHeader(null);
            selectedRecordsUpdateTrigger = (Runnable) selectedRecordsModel.getEventBinding(Runnable.class);
            JScrollPane selectedRecordsScrollPane = new JScrollPane(selectedRecordsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            LabelledComponent selectedRecordsLabelledTable = new LabelledComponent(new JLabel("Selected"), selectedRecordsScrollPane);
            add(selectedRecordsLabelledTable);

            final AddAction addAction = new AddAction();
            JButton addButton = new BhavayaButton(addAction, ImageIconCache.getImageIcon("right_arrow.gif"));
            addButton.setToolTipText("Add selection");
            availableRecordsTable.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        addAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, ""));
                    }
                }
            });
            final RemoveAction removeAction = new RemoveAction();
            JButton removeButton = new BhavayaButton(removeAction, ImageIconCache.getImageIcon("left_arrow.gif"));
            removeButton.setToolTipText("Remove selection");
            selectedRecordsTable.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        removeAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, ""));
                    }
                }
            });

            JPanel buttonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, BORDER_DISTANCE));
            buttonPanel.add(addButton);
            buttonPanel.add(removeButton);
            add(buttonPanel);

            springLayout.putConstraint(SpringLayout.WEST, searchTextFieldLabel, 0, SpringLayout.WEST, this);
            springLayout.putConstraint(SpringLayout.SOUTH, searchTextFieldLabel, -1, SpringLayout.SOUTH, searchTextField);

            springLayout.putConstraint(SpringLayout.WEST, searchTextField, BORDER_DISTANCE, SpringLayout.EAST, searchTextFieldLabel);
            springLayout.putConstraint(SpringLayout.SOUTH, searchTextField, searchTextField.getPreferredSize().height, SpringLayout.NORTH, searchTextField);
            springLayout.putConstraint(SpringLayout.NORTH, searchTextField, 0, SpringLayout.NORTH, this);

            springLayout.putConstraint(SpringLayout.WEST, operatorComboBox, BORDER_DISTANCE, SpringLayout.EAST, searchTextField);
            springLayout.putConstraint(SpringLayout.SOUTH, operatorComboBox, 0, SpringLayout.SOUTH, searchTextField);
            springLayout.putConstraint(SpringLayout.NORTH, operatorComboBox, 0, SpringLayout.NORTH, searchTextField);

            springLayout.putConstraint(SpringLayout.WEST, hourGlassLabel, BORDER_DISTANCE, SpringLayout.EAST, operatorComboBox);
            springLayout.putConstraint(SpringLayout.SOUTH, hourGlassLabel, 0, SpringLayout.SOUTH, operatorComboBox);
            springLayout.putConstraint(SpringLayout.NORTH, hourGlassLabel, 0, SpringLayout.NORTH, operatorComboBox);

            springLayout.putConstraint(SpringLayout.EAST, availableRecordsLabelledTable, Spring.constant(80, 400, 800), SpringLayout.WEST, availableRecordsLabelledTable);
            springLayout.putConstraint(SpringLayout.WEST, availableRecordsLabelledTable, 0, SpringLayout.WEST, searchTextFieldLabel);
            springLayout.putConstraint(SpringLayout.NORTH, availableRecordsLabelledTable, BORDER_DISTANCE, SpringLayout.SOUTH, searchTextFieldLabel);

            springLayout.putConstraint(SpringLayout.WEST, buttonPanel, BORDER_DISTANCE / 2, SpringLayout.EAST, availableRecordsLabelledTable);
            springLayout.putConstraint(SpringLayout.SOUTH, buttonPanel, 0, SpringLayout.SOUTH, availableRecordsLabelledTable);
            springLayout.putConstraint(SpringLayout.NORTH, buttonPanel, 0, SpringLayout.NORTH, availableRecordsLabelledTable);

            springLayout.putConstraint(SpringLayout.EAST, selectedRecordsLabelledTable, Spring.constant(80, 400, 800), SpringLayout.WEST, selectedRecordsLabelledTable);
            springLayout.putConstraint(SpringLayout.WEST, selectedRecordsLabelledTable, BORDER_DISTANCE / 2, SpringLayout.EAST, buttonPanel);
            springLayout.putConstraint(SpringLayout.SOUTH, selectedRecordsLabelledTable, 0, SpringLayout.SOUTH, buttonPanel);
            springLayout.putConstraint(SpringLayout.NORTH, selectedRecordsLabelledTable, 0, SpringLayout.NORTH, buttonPanel);
            springLayout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, selectedRecordsLabelledTable);
            springLayout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, selectedRecordsLabelledTable);

            availableRecordsNarrowingDocument.addDocumentListener( new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    narrowSearch();
                }

                public void insertUpdate(DocumentEvent e) {
                    narrowSearch();
                }

                public void removeUpdate(DocumentEvent e) {
                    narrowSearch();
                }
            });
        }

        public Criterion createCriterion() {
            int size = selectedRecords.size();
            EnumerationCriterion.EnumElement[] elements = new EnumerationCriterion.EnumElement[size];
            int i = 0;
            for (Iterator iterator = selectedRecords.iterator(); iterator.hasNext(); i++) {
                Object selectedRow = iterator.next();
                Object key = recordToKeyTransform.execute(selectedRow);
                Object description = Generic.get(selectedRow, searchProperty);
                elements[i] = new EnumerationCriterion.EnumElement(key, description);
            }

            String operator = (String) operatorComboBox.getSelectedItem();
            return new EnumerationCriterion(initialCriterion.getId(), operator, elements);
        }

        private class AddAction extends AuditedAbstractAction {

            public void auditedActionPerformed(ActionEvent e) {
                int[] selectedRows = availableRecordsTable.getSelectedRows();
                for (int i = 0; i < selectedRows.length; i++) {
                    Object selectedObject = availableRecords.getElementAt(selectedRows[i]);
                    selectedRecords.add(selectedObject);
                    availableRecordsTable.getSelectionModel().removeSelectionInterval(selectedRows[i], selectedRows[i]);
                    Object key = recordToKeyTransform.execute(availableRecords.getElementAt(selectedRows[i]));
                    selectedKeys.add(key);
                }
                selectedRecordsUpdateTrigger.run();
                availableRecordsTable.repaint();
            }
        }

        private class RemoveAction extends AuditedAbstractAction {
            public void auditedActionPerformed(ActionEvent e) {
                int[] selectedRows = selectedRecordsTable.getSelectedRows();
                Object[] selectedObjects = new Object[selectedRows.length];

                for (int i = 0; i < selectedRows.length; i++) {
                    selectedObjects[i] = selectedRecords.get(selectedRows[i]);
                }

                for (int i = 0; i < selectedObjects.length; i++) {
                    selectedRecords.remove(selectedObjects[i]);
                    Object key = recordToKeyTransform.execute(selectedObjects[i]);
                    selectedKeys.remove(key);
                }

                selectedRecordsUpdateTrigger.run();
                availableRecordsTable.repaint();
            }
        }

        private class SelectedRecordsCellRenderer extends DefaultTableCellRenderer {
            private Color normalColor;
            private Color alreadySelectedColor;

            public SelectedRecordsCellRenderer() {
                super();
                normalColor = Color.black;
                alreadySelectedColor = Color.lightGray;
            }

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component;
                Object rowKey = recordToKeyTransform.execute(availableRecords.getElementAt(row));
                if (selectedKeys.contains(rowKey)) {
                    component = super.getTableCellRendererComponent(table, value, false, hasFocus, row, column);
                    component.setForeground(alreadySelectedColor);
                } else {
                    component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    component.setForeground(normalColor);
                }

                return component;
            }
        }

        public void narrowSearch() {
            try {
                hourGlassLabel.setIcon(BUSY_ICON);
                availableRecords.narrow(availableRecordsNarrowingDocument.getText(0, availableRecordsNarrowingDocument.getLength()), true);
            } catch (BadLocationException e) {
                EnumerationCriterionEditor.this.log.error(e);
            }
        }

        private void updateSelectedFromCriterion() {
            selectedRecords.clear();
            selectedKeys.clear();

            Object[] elements = (Object[]) initialCriterion.getRightOperand();
            if (elements != null) {
                for (int i = 0; i < elements.length; i++) {
                    EnumerationCriterion.EnumElement element = (EnumerationCriterion.EnumElement) elements[i];
                    Object key = element.getId();
                    Object record = keyToRecordTransform.execute(key);

                    if (record != null) {
                        selectedRecords.add(record);
                        selectedKeys.add(key);
                    }
                }
            }
        }
    }
}
