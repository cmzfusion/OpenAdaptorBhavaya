package org.bhavaya.ui.view;

import org.bhavaya.ui.UIUtilities;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Dialog to display views that have not been recently activated, allowing selection of those to close.
 * User: ga2mhana
 * Date: 07/03/11
 * Time: 14:38
 */
public class ConfirmCloseViewsDialog extends JDialog {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy hh:mm");

    private final Collection<View> views;
    private SelectableViewTableModel tableModel;

    public ConfirmCloseViewsDialog(JFrame owner, Collection<View> views) throws HeadlessException {
        super(owner, "Remove Inactive Views", true);
        this.views = views;
        setAlwaysOnTop(true);
        initLayout();
    }

    private void initLayout() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(createLabelComponent(), BorderLayout.NORTH);
        contentPane.add(createTableComponent(), BorderLayout.CENTER);
        contentPane.add(createButtonComponent(), BorderLayout.SOUTH);
        setSize(600, 250);
        UIUtilities.centreInScreen(this, 0, 0);
        setVisible(true);
    }

    private JComponent createLabelComponent() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("<html>One or more views have not been activated recently.<br>" +
                "Closing unused views will improve the performance of the application.<br>" +
                "Please select the views below that you would like to remove:"));
        return topPanel;
    }

    private JComponent createTableComponent() {
        tableModel = new SelectableViewTableModel();
        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40);
        columnModel.getColumn(1).setPreferredWidth(250);
        columnModel.getColumn(2).setPreferredWidth(250);
        return new JScrollPane(table);
    }

    private JComponent createButtonComponent() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        });
        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setAllSelected(true);
            }
        });
        JButton unselectAllButton = new JButton("Unselect All");
        unselectAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.setAllSelected(false);
            }
        });
        buttonPanel.add(selectAllButton);
        buttonPanel.add(unselectAllButton);
        buttonPanel.add(okButton);

        return buttonPanel;
    }

    public Collection<View> getAllSelected() {
        return tableModel.getAllSelected();
    }

    private void okPressed() {
        setVisible(false);
        dispose();
    }

    private class SelectableView {
        boolean selected = false;
        View view;

        private SelectableView(View view) {
            this.view = view;
        }
    }

    private class SelectableViewTableModel extends AbstractTableModel {
        private final List<SelectableView> selectableViews;
        private final String[] columnNames = {"", "View", "Last Activated"};
        private SelectableViewTableModel() {
            selectableViews = new ArrayList<SelectableView>(views.size());
            for(View view : views) {
                selectableViews.add(new SelectableView(view));
            }
        }

        public int getRowCount() {
            return selectableViews.size();
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            SelectableView selectableView = selectableViews.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return selectableView.selected;
                case 1:
                    return selectableView.view.getName();
                case 2:
                    return DATE_FORMAT.format(selectableView.view.getLastActivated());
            }
            return "";
        }

        public Collection<View> getAllSelected() {
            Collection<View> selected = new HashSet<View>(selectableViews.size());
            for(SelectableView selectableView : selectableViews) {
                if(selectableView.selected) {
                    selected.add(selectableView.view);
                }
            }

            return selected;
        }

        private void setAllSelected(boolean selected) {
            for(SelectableView view : selectableViews) {
                view.selected = selected;
            }
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if(columnIndex == 0 && aValue != null) {
                SelectableView selectableView = selectableViews.get(rowIndex);
                selectableView.selected = (Boolean)aValue;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if(columnIndex == 0) {
                return Boolean.class;
            }
            return String.class;
        }
    }
}
