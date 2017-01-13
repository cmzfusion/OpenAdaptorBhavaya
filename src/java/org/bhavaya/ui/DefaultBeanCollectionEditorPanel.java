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

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.Column;
import org.bhavaya.beans.Schema;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.IndexedSet;
import org.bhavaya.ui.table.KeyedColumnTableModelAdapter;
import org.bhavaya.ui.table.SortedTableModel;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * This class has now been split with most of the functionality in DefaultBeanCollectionEditorPanel. Ideally you should use that
 * class and embed the panel in your own Dialog
 *
 * @author
 * @version $Revision: 1.5.4.2 $
 */
public class DefaultBeanCollectionEditorPanel extends JPanel {
    private static final Log log = Log.getCategory(DefaultBeanCollectionEditorPanel.class);

    private static final int SPACING = 10;

    private BeanCollection beanCollection;
    private String[] searchPropertyDisplayNames;
    private String[] tableProperties;
    private Class[] tablePropertyTypes;
    private String[] tablePropertyDisplayNames;
    private double[] columnRatios;
    private JTextField[] searchFields;
    private int[] sortingColumns;
    private Filter[] filters;
    private String sql;
    private boolean singleRowSelection;
    private String descriptionRenderText;
    private int width;

    private FilteredListModel availableRecords;
    private ListTable availableRecordsTable;
    private ListModelTableSorter sortedAvailableRecordsModel;
    private IndexedSet selectedRecords;
    private ListTable.ListTableModel selectedRecordsModel;
    private ListModelTableSorter sortedSelectedRecordsModel = null;
    private Set addedKeys;
    private Set removedKeys;
    private Runnable selectedRecordsUpdateTrigger = null;
    private ListTable selectedRecordsTable;
    private boolean orderable;

    public DefaultBeanCollectionEditorPanel(BeanCollection beanCollection,
                                       String[] searchProperties,
                                       String[] searchPropertyDisplayNames,
                                       String[] tableProperties,
                                       String[] tablePropertyDisplayNames,
                                       Class[] tablePropertyTypes,
                                       int width,
                                       double[] columnRatios,
                                       int[] sortingColumns,
                                       boolean singleRowSelection,
                                       String dataSourceName,
                                       String sql,
                                       String descriptionRenderText,
                                       Transform beanToRecordTransformer,
                                       Filter[] additionalFilters,
                                       boolean orderable) {
        init(beanCollection, searchPropertyDisplayNames, tableProperties, tablePropertyTypes, sortingColumns, tablePropertyDisplayNames, columnRatios, dataSourceName, sql, singleRowSelection, descriptionRenderText, width, searchProperties, beanToRecordTransformer, additionalFilters, orderable);
    }

    private void init(final BeanCollection beanCollection,
                      String[] searchPropertyDisplayNames,
                      String[] tableProperties,
                      Class[] tablePropertyTypes,
                      int[] sortingColumns,
                      String[] tablePropertyDisplayNames,
                      double[] columnRatios,
                      String dataSourceName,
                      String sql,
                      boolean singleRowSelection,
                      String descriptionRenderText,
                      int width,
                      String[] searchProperties,
                      final Transform beanToRecordTransformer,
                      Filter[] additionalFilters,
                      boolean orderable) {

        this.beanCollection = beanCollection;
        this.searchPropertyDisplayNames = searchPropertyDisplayNames;
        this.tableProperties = tableProperties;
        this.tablePropertyTypes = tablePropertyTypes;
        this.sortingColumns = sortingColumns;
        this.tablePropertyDisplayNames = tablePropertyDisplayNames;
        this.columnRatios = columnRatios;
        this.sql = sql != null ? ApplicationProperties.substituteApplicationProperties(sql) : null;
        this.singleRowSelection = singleRowSelection;
        this.orderable = orderable && !singleRowSelection;
        this.descriptionRenderText = descriptionRenderText;
        this.width = width;

        boolean showAllData = searchProperties.length == 0;
        NarrowableListModel allRecords;
        if (sql != null) {
            allRecords = new DBNarrowableListModel(dataSourceName, this.sql, showAllData);
        } else {
            if (showAllData) {
                Collection allBeans = Arrays.asList(BeanFactory.getInstance(beanCollection.getType()).getAllObjects());
                allRecords = new NarrowableListModel(allBeans, showAllData);
            } else {
                allRecords = new NarrowableListModel(showAllData);
            }
        }
        if (!showAllData) allRecords.setRenderer(new GenericStringRenderer(searchProperties[0]));

        availableRecords = new FilteredListModel(allRecords);
        PlainDocument narrowingTextDocument = new PlainDocument();
        narrowingTextDocument.addDocumentListener(new NarrowingDocumentListener(allRecords));

        if (!showAllData) {
            searchFields = new JTextField[searchProperties.length];
            int additionalFilterLength = (additionalFilters != null) ? additionalFilters.length : 0;
            filters = new Filter[searchProperties.length - 1 + additionalFilterLength];

            for (int i = 0; i < searchProperties.length; i++) {
                String searchProperty = searchProperties[i];
                JTextField searchTextField;
                if (i == 0) {
                    searchTextField = new JTextField(narrowingTextDocument, "", 15);
                } else {
                    PlainDocument filteringTextDocument = new PlainDocument();
                    filteringTextDocument.addDocumentListener(new FilteringDocumentListener(availableRecords));
                    searchTextField = new JTextField(filteringTextDocument, "", 15);
                    filters[i - 1] = new FieldFilter(searchProperty, searchTextField);
                }
                searchFields[i] = searchTextField;
            }
            if (additionalFilters != null && additionalFilters.length > 0) {
                for (int i = 0; i < additionalFilters.length; i++) {
                    Filter filter = additionalFilters[i];
                    filters[searchProperties.length - 1 + i] = filter;
                }
            }
        } else if (additionalFilters != null && additionalFilters.length > 0) {
            filters = additionalFilters;
        }

        availableRecords.setFilters(filters);

        if (!singleRowSelection) {
            selectedRecords = new IndexedSet();
            addedKeys = new HashSet();
            removedKeys = new HashSet();
        }

        initPanel(beanToRecordTransformer);
    }

    private void populateSelectedRecords(BeanCollection beanCollection, Transform beanToRecordTransformer) {

        for (Iterator iterator = beanCollection.iterator(); iterator.hasNext();) {
            Object bean = iterator.next();
            if (beanToRecordTransformer != null) {
                selectedRecords.add(beanToRecordTransformer.execute(bean));
            } else {
                selectedRecords.add(bean);
            }
        }
    }

    private void initPanel(Transform beanToRecordTransformer) {
        setLayout(new BorderLayout());
        JPanel searchPanel = createSearchPanel();
        if(searchPanel != null) {
            add(createSearchPanel(), BorderLayout.NORTH);
        }
        add(createMainPanel(beanToRecordTransformer), BorderLayout.CENTER);
        if (descriptionRenderText != null) {
            add(createDescriptionPanel(), BorderLayout.SOUTH);
        }
    }

    private JPanel createDescriptionPanel() {
        JPanel descriptionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, SPACING));

        final ObjectRenderLabel descriptionLabel = new ObjectRenderLabel(descriptionRenderText);
        addAvailableRecordsListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    ListSelectionModel model = (ListSelectionModel) e.getSource();
                    int index = model.getLeadSelectionIndex();
                    Object record = getAvailableRecord(index);
                    descriptionLabel.setRenderObject(record);
                }
            }
        });

        descriptionPanel.add(descriptionLabel);
        descriptionPanel.setPreferredSize(new Dimension(width, 60));
        return descriptionPanel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = null;
        if (searchPropertyDisplayNames.length > 0) {
            searchPanel = new JPanel(new RowLayout(width, SPACING));
            RowLayout rowLayout = (RowLayout) searchPanel.getLayout();

            for (int i = 0; i < searchFields.length; i++) {
                JTextField searchField = searchFields[i];
                RowLayout.Row row = new RowLayout.Row(SPACING, RowLayout.LEFT, RowLayout.MIDDLE, false);
                searchPanel.add(row.addComponent(new JLabel(Utilities.getDisplayName(searchPropertyDisplayNames[i])), new RowLayout.FixedWidthConstraint(100)));
                searchPanel.add(row.addComponent(searchField, new RowLayout.FixedWidthConstraint(150)));
                rowLayout.addRow(row);
            }
        }

        return searchPanel;
    }


    private Container createMainPanel(final Transform beanToRecordTransformer) {
        final JPanel container = new JPanel(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 1, 1,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

        ListTable.ListTableModel availableRecordsModel = new ListTable.ListModelTableModel(availableRecords, tableProperties, tablePropertyDisplayNames, tablePropertyTypes);
        sortedAvailableRecordsModel = getSortedTableModel(availableRecordsModel);
        availableRecordsTable = new ListTable(sortedAvailableRecordsModel, columnRatios);
        availableRecordsTable.setDefaultRenderer(Object.class, new SelectedRecordsCellRenderer(availableRecordsTable.getDefaultRenderer(Object.class)));
        initListTable(availableRecordsTable);
        JScrollPane availableRecordsScrollPane = new JScrollPane(availableRecordsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        availableRecordsScrollPane.getVerticalScrollBar().setFocusable(false);
        LabelledComponent availableRecordsLabelledTable = new LabelledComponent(new JLabel("Available"), availableRecordsScrollPane);
        container.add(availableRecordsLabelledTable, constraints);

        if (singleRowSelection) {
            availableRecordsScrollPane.setPreferredSize(new Dimension(width, (int) availableRecordsScrollPane.getPreferredSize().getHeight()));
        } else {
            JPanel buttonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, SPACING));
            final AddAction addAction = new AddAction();
            JButton addButton = new BhavayaButton(addAction, ImageIconCache.getImageIcon("right_arrow.gif"));
            addButton.setToolTipText("Add selection");
            buttonPanel.add(addButton);
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
            buttonPanel.add(removeButton);
            constraints.weightx = 0;
            container.add(buttonPanel, constraints);

            availableRecordsScrollPane.setPreferredSize(new Dimension(((int) ((width * 0.5) - buttonPanel.getPreferredSize().width)), (int) availableRecordsScrollPane.getPreferredSize().getHeight()));

            selectedRecordsModel = new ListTable.CollectionListTableModel(selectedRecords, tableProperties, tablePropertyDisplayNames, tablePropertyTypes);
            if(orderable) {
                selectedRecordsTable = new ListTable(selectedRecordsModel, columnRatios);
            } else {
                sortedSelectedRecordsModel = getSortedTableModel(selectedRecordsModel);
                selectedRecordsTable = new ListTable(sortedSelectedRecordsModel, columnRatios);
            }
            selectedRecordsTable.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        removeAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, ""));
                    }
                }
            });

            initListTable(selectedRecordsTable);
            selectedRecordsUpdateTrigger = selectedRecordsModel.getEventBinding(Runnable.class);
            JScrollPane selectedRecordsScrollPane = new JScrollPane(selectedRecordsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            selectedRecordsScrollPane.getVerticalScrollBar().setFocusable(false);
            LabelledComponent selectedRecordsLabelledTable = new LabelledComponent(new JLabel("Selected"), selectedRecordsScrollPane);
            constraints.weightx = 1;
            container.add(selectedRecordsLabelledTable, constraints);

            if(orderable) {
                buttonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, SPACING));
                buttonPanel.add(new BhavayaButton(new UpAction(), ImageIconCache.getImageIcon("up_arrow.gif")));
                buttonPanel.add(new BhavayaButton(new DownAction(), ImageIconCache.getImageIcon("down_arrow.gif")));
                constraints.weightx = 0;
                container.add(buttonPanel, constraints);
            }

            selectedRecordsLabelledTable.setPreferredSize(new Dimension(((int) ((width * 0.5) - buttonPanel.getPreferredSize().width)), (int) selectedRecordsLabelledTable.getPreferredSize().getHeight()));
        }

        if (!singleRowSelection) {
            final CardPanel cardPanel = new CardPanel();
            final LoadingPanel loadingPanel = new LoadingPanel("Loading data...");
            cardPanel.addComponent(loadingPanel);
            cardPanel.addComponent(container);
            cardPanel.setSelectedComponent(loadingPanel);

            Utilities.newThread(new Runnable() {
                public void run() {
                    try {
                        populateSelectedRecords(beanCollection, beanToRecordTransformer);
                        cardPanel.setSelectedComponent(container);
                        loadingPanel.dispose();
                    } catch (Throwable e) {
                        loadingPanel.dispose();
                        JComponent errorView = UIUtilities.createErrorComponent("<html>There was a problem loading the data.<br>Please go to the Help/Diagnotics menu and send a report to support</html>");
                        cardPanel.addComponent(errorView);
                        cardPanel.setSelectedComponent(errorView);
                    }
                }
            }, "DefaultBeanCollectionEditorLoad", false).start();
            return cardPanel;

        } else {
            return container;
        }
    }

    private void addAvailableRecordsListSelectionListener(ListSelectionListener l) {
        availableRecordsTable.getSelectionModel().addListSelectionListener(l);
    }

    private Object getAvailableRecord(int index) {
        return sortedAvailableRecordsModel.getObjectAtRow(index);
    }

    private Object getSelectedRecord(int index) {
        if(sortedSelectedRecordsModel != null) {
            return sortedSelectedRecordsModel.getObjectAtRow(index);
        }
        return selectedRecordsModel.getObjectAtRow(index);
    }

    private ListModelTableSorter getSortedTableModel(final ListTable.ListTableModel listTableModel) {
        ListModelTableSorter sortedModel = new ListModelTableSorter(listTableModel);
        for (int i = 0; i < sortingColumns.length; i++) {
            Object columnKey = sortedModel.getSourceModel().getColumnKey(sortingColumns[i]);
            sortedModel.addSortingColumn(columnKey, false);
        }
        return sortedModel;
    }

    private void initListTable(ListTable listTable) {
        listTable.setColumnSelectionAllowed(false);
        listTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, UIUtilities.normalForwardTraversalKeystrokes);
        listTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, UIUtilities.backwardTraversalKeystrokes);
        if (singleRowSelection) {
            listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        } else {
            listTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
    }

    public void setDefaultRenderer(Class aClass, TableCellRenderer tableCellRenderer) {
        availableRecordsTable.setDefaultRenderer(aClass, new SelectedRecordsCellRenderer(tableCellRenderer));
        if (!singleRowSelection) {
            selectedRecordsTable.setDefaultRenderer(aClass, tableCellRenderer);
        }
    }

    private class AddAction extends AuditedAbstractAction {
        public void auditedActionPerformed(ActionEvent e) {
            int[] selectedRows = availableRecordsTable.getSelectedRows();
            int[] savedSelectedRows = selectedRecordsTable.getSelectedRows();
            for (int i = 0; i < selectedRows.length; i++) {
                Object objectAtRow = getAvailableRecord(selectedRows[i]);
                selectedRecords.add(objectAtRow);
                availableRecordsTable.removeRowSelectionInterval(selectedRows[i], selectedRows[i]);
                Object key = getKey(objectAtRow);
                addedKeys.add(key);
                removedKeys.remove(key);
            }
            selectedRecordsUpdateTrigger.run();
            availableRecordsTable.repaint();
            setSelectedRows(selectedRecordsTable, savedSelectedRows);
        }
    }

    private class RemoveAction extends AuditedAbstractAction {
        public void auditedActionPerformed(ActionEvent e) {
            int[] selectedRows = selectedRecordsTable.getSelectedRows();
            Object[] selectedObjects = new Object[selectedRows.length];

            for (int i = 0; i < selectedRows.length; i++) {
                selectedObjects[i] = getSelectedRecord(selectedRows[i]);
            }

            for (int i = 0; i < selectedObjects.length; i++) {
                selectedRecords.remove(selectedObjects[i]);
                Object key = getKey(selectedObjects[i]);
                addedKeys.remove(key);
                removedKeys.add(key);
            }

            selectedRecordsUpdateTrigger.run();
            availableRecordsTable.repaint();
        }
    }

    private class UpAction extends AuditedAbstractAction {
        private UpAction() {
            putValue(Action.SHORT_DESCRIPTION, "Move selection up");
        }

        public void auditedActionPerformed(ActionEvent e) {
            int[] selectedRows = selectedRecordsTable.getSelectedRows();
            if(selectedRows != null && selectedRows.length > 0 && selectedRows[0] > 0) {
                int[] newSelectedRows = new int[selectedRows.length];
                for(int i=0; i<selectedRows.length; i++) {
                    swapRowWithNext(selectedRows[i]-1);
                    newSelectedRows[i] = selectedRows[i]-1;
                }
                selectedRecordsUpdateTrigger.run();
                availableRecordsTable.repaint();
                setSelectedRows(selectedRecordsTable, newSelectedRows);
            }
        }
    }

    private class DownAction extends AuditedAbstractAction {
        private DownAction() {
            putValue(Action.SHORT_DESCRIPTION, "Move selection down");
        }

        public void auditedActionPerformed(ActionEvent e) {
            int[] selectedRows = selectedRecordsTable.getSelectedRows();
            if(selectedRows != null && selectedRows.length > 0 && selectedRows[selectedRows.length-1] < selectedRecordsTable.getRowCount()-1) {
                int[] newSelectedRows = new int[selectedRows.length];
                for(int i=selectedRows.length-1; i>=0; i--) {
                    swapRowWithNext(selectedRows[i]);
                    newSelectedRows[i] = selectedRows[i]+1;
                }
                selectedRecordsUpdateTrigger.run();
                availableRecordsTable.repaint();
                setSelectedRows(selectedRecordsTable, newSelectedRows);
            }
        }
    }

    private void swapRowWithNext(int row) {
        //swap the item at the given row with the one above it
        Object temp = selectedRecords.remove(row);
        selectedRecords.add(row+1, temp);
    }

    private void setSelectedRows(JTable table, int[] selected) {
        for(int i=0; i<selected.length; i++) {
            table.changeSelection(selected[i], 0, true, false);
        }
    }

    public void updateCollection() {
        if (singleRowSelection) {
            int selectedRow = availableRecordsTable.getSelectedRow();
            if (selectedRow != -1) {
                Object objectAtRow = getAvailableRecord(selectedRow);
                Object key = getKey(objectAtRow);
                Object value = BeanFactory.getInstance(beanCollection.getType()).get(key);
                if (value != null) beanCollection.add(value, false);
            }
        } else if (orderable){
            beanCollection.clear(false);
            for(Iterator iterator = selectedRecords.iterator(); iterator.hasNext(); ) {
                Object key = getKey(iterator.next());
                Object value = BeanFactory.getInstance(beanCollection.getType()).get(key);
                if (value != null) beanCollection.add(value, false);
            }
            beanCollection.fireCommit();
        } else {
            for (Iterator iterator = addedKeys.iterator(); iterator.hasNext();) {
                Object key = iterator.next();
                Object value = BeanFactory.getInstance(beanCollection.getType()).get(key);
                if (value != null) beanCollection.add(value, false);
            }

            for (Iterator iterator = removedKeys.iterator(); iterator.hasNext();) {
                Object key = iterator.next();
                Object value = BeanFactory.getInstance(beanCollection.getType()).get(key);
                beanCollection.remove(value, false);
            }

            beanCollection.fireCommit();
        }
    }

    public void clearSelection() {
        if (!singleRowSelection) {
            selectedRecords.clear();
            addedKeys.clear();
            removedKeys.clear();
        }
    }

    private class SelectedRecordsCellRenderer implements TableCellRenderer {
        private Color normalColor;
        private Color alreadySelectedColor;
        private TableCellRenderer orig;


        public SelectedRecordsCellRenderer(TableCellRenderer orig) {
            this.orig = orig;
            normalColor = Color.black;
            alreadySelectedColor = Color.lightGray;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component;

            boolean alreadySelected = false;

            if (!singleRowSelection) {
                // need to use keys to check if a record is already selected,
                // use keys not object.equals as their can be multiple records per bean/key
                Object availableRecordKey = getKey(sortedAvailableRecordsModel.getObjectAtRow(row));

                for (Iterator iterator = selectedRecords.iterator(); iterator.hasNext() && !alreadySelected;) {
                    Object selectedRecordKey = getKey(iterator.next());
                    alreadySelected = availableRecordKey.equals(selectedRecordKey);
                }
            }

            if (!singleRowSelection && alreadySelected) {
                component = orig.getTableCellRendererComponent(table, value, false, hasFocus, row, column);
                component.setForeground(alreadySelectedColor);
            } else {
                component = orig.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                component.setForeground(normalColor);
            }

            return component;
        }
    }

    private Object getKey(Object object) {
        if (sql == null && Schema.hasInstance(object.getClass())) {
            return BeanFactory.getKeyForBean(object);
        } else {
            Schema schema = Schema.getInstance(beanCollection.getType());
            Column[] primaryKey = schema.getPrimaryKey();
            String[] primaryKeyNames = Column.columnsToNames(primaryKey);
            return Utilities.createKey(primaryKeyNames, object);
        }
    }

    private static class NarrowingDocumentListener implements DocumentListener {
        private NarrowableListModel narrowableListModel;

        public NarrowingDocumentListener(NarrowableListModel narrowableListModel) {
            this.narrowableListModel = narrowableListModel;
        }

        public void insertUpdate(DocumentEvent e) {
            narrowSearch(e);
        }

        public void removeUpdate(DocumentEvent e) {
            narrowSearch(e);
        }

        public void changedUpdate(DocumentEvent e) {
            narrowSearch(e);
        }

        private void narrowSearch(DocumentEvent e) {
            try {
                Document document = e.getDocument();
                String text = document.getText(0, document.getLength());
                narrowableListModel.narrow(text);
            } catch (BadLocationException e1) {
                log.error(e1);
            }
        }
    }

    private static class FilteringDocumentListener implements DocumentListener {
        private FilteredListModel listModel;

        public FilteringDocumentListener(FilteredListModel ListModel) {
            this.listModel = ListModel;
        }

        public void insertUpdate(DocumentEvent e) {
            filter();
        }

        public void removeUpdate(DocumentEvent e) {
            filter();
        }

        public void changedUpdate(DocumentEvent e) {
            filter();
        }

        private void filter() {
            listModel.filter();
        }
    }

    private static class FieldFilter implements Filter {
        private final StringRenderer stringRenderer;
        private final JTextField searchTextField;

        public FieldFilter(String property, JTextField searchTextField) {
            this.stringRenderer = new GenericStringRenderer(property);
            this.searchTextField = searchTextField;
        }

        public boolean evaluate(Object obj) {
            String objectString = stringRenderer.render(obj);
            String text = searchTextField.getText();
            if (text == null || text.trim().length() == 0) return true;
            return objectString.toLowerCase().startsWith(text.toLowerCase());
        }
    }


    private static class ListModelTableSorter extends SortedTableModel {
        private final ListTable.ListTableModel listTableModel;

        public ListModelTableSorter(ListTable.ListTableModel listTableModel) {
            super(new KeyedColumnTableModelAdapter(listTableModel));
            this.listTableModel = listTableModel;
        }

        public Object getObjectAtRow(int index) {
            int mappedIndex = mapModelToUnderlying(index);
            return listTableModel.getObjectAtRow(mappedIndex);
        }
    }
}