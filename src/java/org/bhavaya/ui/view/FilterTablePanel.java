package org.bhavaya.ui.view;

import org.bhavaya.ui.GradientPanel;
import org.bhavaya.ui.BorderlessIconButton;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.table.ChainableFilteredTableModel;
import org.bhavaya.util.IOUtilities;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import com.od.filtertable.TableCell;
import com.od.filtertable.TableCellFinder;
import org.bhavaya.util.ImageIconCache;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 10-Sep-2008
 * Time: 16:24:59
 */
public class FilterTablePanel extends JPanel {

    private static ImageIcon CLEAR_SEARCH_ICON = ImageIconCache.getImageIcon("remove_all.png");
    private static ImageIcon ARROW_UP_BLUE_ICON = ImageIconCache.getImageIcon("arrow_up_blue.png");
    private static ImageIcon ARROW_DOWN_BLUE_ICON = ImageIconCache.getImageIcon("arrow_down_blue.png");
    private static ImageIcon HIDE_ICON = ImageIconCache.getImageIcon("delete2.png");

    private GradientPanel filterPanel;
    private AnalyticsTableModel analyticsTableModel;
    private JTextField filterTextField;
    private AnalyticsTable analyticsTable;
    private FilterTablePanelModel filterPanelModel;
    private JCheckBox filterWithFindCheckbox;
    private JCheckBox includeSubstringsInFindCheckbox;
    private JButton findPrevious;
    private JButton findNext;
    private JButton clearFilterButton;
    private JButton hideSearchButton;
    private TableCellFinder tableCellFinder;
    private JLabel matchCountLabel;
    private int lastMatchCount;
    private Action filterEnterAction = null;

    public FilterTablePanel(AnalyticsTable analyticsTable) {
        this.analyticsTable = analyticsTable;
        this.tableCellFinder = analyticsTable.getMatchFinder();
        this.analyticsTableModel = analyticsTable.getAnalyticsTableModel();
        this.filterPanelModel = analyticsTable.getFilterPanelModel();

        createFilterPanel();
        addListeners();
        addKeyBindings();

        setLayout(new BorderLayout());
        add(analyticsTable, BorderLayout.CENTER);
        add(filterPanel, BorderLayout.SOUTH);
    }

    private void addKeyBindings() {
        doRegisterFindNextKeyEvent(KeyEvent.VK_RIGHT, true);
        doRegisterFindNextKeyEvent(KeyEvent.VK_LEFT, false);
        doRegisterFindNextKeyEvent(KeyEvent.VK_DOWN, true);
        doRegisterFindNextKeyEvent(KeyEvent.VK_UP, false);

        registerKeyboardAction(
            new HideSearchBarAction(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        filterTextField.registerKeyboardAction(
            new GoToLastFoundCellAction(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
    }

    private void doRegisterFindNextKeyEvent(int keyEvent, boolean findForward) {
        registerKeyboardAction(
            new NextFindResultAction(findForward),
            KeyStroke.getKeyStroke(keyEvent, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
    }

    public void setFilterEnterAction(Action filterEnterAction) {
        this.filterEnterAction = filterEnterAction;
    }


    public void showFilterPanel(boolean showPanel) {
        filterPanel.setVisible(showPanel);
        filterPanelModel.setShowFilterFindPanel(showPanel);
        validate();
        if ( showPanel) {
            filterTextField.requestFocusInWindow();
        } else {
            clearSearch();
        }
    }

    private void clearSearch() {
        analyticsTableModel.setRowFiltered(false);
        filterTextField.setText("");
        clearLastFind();
    }

    private void createFilterPanel() {
        matchCountLabel = new JLabel();
        matchCountLabel.setForeground(Color.BLUE.darker());
        matchCountLabel.setPreferredSize(new Dimension(85, matchCountLabel.getSize().height));
        matchCountLabel.setMinimumSize(new Dimension(85, matchCountLabel.getSize().height));

        filterTextField = new JTextField(20);
        filterTextField.setMaximumSize(new Dimension(150, filterTextField.getPreferredSize().height));
        filterTextField.setMinimumSize(new Dimension(75, filterTextField.getPreferredSize().height));

        clearFilterButton = new BorderlessIconButton(new ClearSearchAction());

        findPrevious = new JButton(new FindPreviousAction());
        findNext = new JButton(new FindNextAction());

        hideSearchButton = new BorderlessIconButton(new HideSearchBarAction());

        filterWithFindCheckbox = new JCheckBox("Filter Rows");
        filterWithFindCheckbox.setModel(filterPanelModel.getFilterWithFindButtonModel());
        filterWithFindCheckbox.setOpaque(false);

        includeSubstringsInFindCheckbox = new JCheckBox("Include Substrings");
        includeSubstringsInFindCheckbox.setModel(filterPanelModel.getIncludeSubstringsInFindButtonModel());
        includeSubstringsInFindCheckbox.setOpaque(false);

        filterPanel = new GradientPanel(Color.WHITE, new Color(0xBD,0xBB,0xAF));
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));
        filterPanel.add(Box.createHorizontalStrut(5));
        filterPanel.add(matchCountLabel);
        filterPanel.add(Box.createHorizontalStrut(125));

        filterPanel.add(Box.createHorizontalGlue());
        filterPanel.add(filterTextField);
        filterPanel.add(clearFilterButton);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(findPrevious);
        filterPanel.add(findNext);
        filterPanel.add(Box.createHorizontalGlue());

        filterPanel.add(filterWithFindCheckbox);
        filterPanel.add(includeSubstringsInFindCheckbox);
        filterPanel.add(hideSearchButton);
        filterPanel.add(Box.createHorizontalStrut(5));
    }

    private void addListeners() {
        addFilterTextListener();
        addEnableRowFilterListener();
        addIncludeSubstringsListener();
        addMatchCountListener();
    }


    private void addMatchCountListener() {
        final ChainableFilteredTableModel rowFilteredModel = analyticsTableModel.getRowFilteredTableModel();
        rowFilteredModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if ( lastMatchCount != rowFilteredModel.getMatchCount()) {
                    lastMatchCount = rowFilteredModel.getMatchCount();
                    matchCountLabel.setText(isSearchTextEntered() ? "matches: " + lastMatchCount : "");
                }
            }
        });
    }

    private void doFindNext(boolean findForward) {
        TableCell lastFind = getLastFoundCell();
        TableCell cell = findForward ?
                tableCellFinder.findNextMatchingCell(lastFind) :
                tableCellFinder.findPreviousMatchingCell(lastFind);

        if ( cell != TableCell.NO_MATCH_TABLE_CELL) {
            analyticsTable.scrollToCell(cell.getRow(), cell.getCol());
            analyticsTable.setSelectedRow(cell.getRow());
            analyticsTable.repaint();
        }
    }

    private TableCell getLastFoundCell() {
        TableCell lastFind = tableCellFinder.getLastFindResult();

        //if no last find, start from the currently selected row, or if user has manually changed selected row
        //since the last find, start from there
        if ( lastFind == TableCell.NO_MATCH_TABLE_CELL || lastFind.getRow() != analyticsTable.getSelectedRow()) {
            lastFind = new TableCell(Math.max(analyticsTable.getSelectedRow(), 0),0);
        }
        return lastFind;
    }

    private void addFilterTextListener() {
        filterTextField.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) {
                        handleFilterTextChange();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        handleFilterTextChange();
                    }

                    public void changedUpdate(DocumentEvent e) {}

                    private void handleFilterTextChange() {
                        //if cells are being edited we get into a state when we apply the filter
                        //in which we appear to have a phantom editor floating in empty space
                        //safest to explicitly cancel the editing here
                        analyticsTable.cancelCellEditing();

                        updateFilterSettings();
                    }
                }
        );

        filterTextField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                analyticsTable.grabFocus();
                TableCell c =  tableCellFinder.getLastFindResult();
                if ( c != TableCell.NO_MATCH_TABLE_CELL) {
                    analyticsTable.setSelectedCellAndGrabFocus(c.getRow(), c.getCol());
                    if(filterEnterAction != null) {
                        filterEnterAction.actionPerformed(e);
                    }
                }
            }
        });
    }

    private void addEnableRowFilterListener() {
        filterWithFindCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearLastFind();
                updateFilterSettings();
            }
        });
    }

    private void addIncludeSubstringsListener() {
        includeSubstringsInFindCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearLastFind();
                analyticsTable.getViewConfiguration().setIncludeSubstringsInFind(
                    includeSubstringsInFindCheckbox.isSelected()
                );
                updateFilterSettings();
            }
        });
    }

    private void clearLastFind() {
        tableCellFinder.clearLastFindResult();
    }

    private void updateFilterSettings() {
        boolean isSearchTextEntered = isSearchTextEntered();

        //take the filter model out of the stack if we're not using it, otherwise all events would have to go through it
        analyticsTableModel.setRowFiltered(isSearchTextEntered);

        analyticsTableModel.setIncludeSubstringsInFilterFind(includeSubstringsInFindCheckbox.isSelected());

        analyticsTableModel.getRowFilteredTableModel().setFilterRows(
            filterWithFindCheckbox.isSelected()
        );

        // do not change this class to hold a reference to rowFilteredTableModel -
        // that would prevent it being gc'd when the filter is removed
        analyticsTableModel.getRowFilteredTableModel().setSearchTerm(filterTextField.getText());

        if ( isSearchTextEntered ) {
            //if the change to the search text has caused the 'last found cell' to no longer be a match, find the
            //next matching cell
            TableCell lastFind = tableCellFinder.getLastFindResult();
            if ( lastFind == TableCell.NO_MATCH_TABLE_CELL || ! isMatch(lastFind)) {
                doFindNext(true);
            }
        }
    }

    private boolean isSearchTextEntered() {
        return filterTextField.getText().length() > 0;
    }

    //check if a TableCell, which may or may not still represent a valid table row/col, matches the current search
    private boolean isMatch(TableCell lastFind) {
        boolean result = false;
        int tableModelCol= analyticsTable.getTableModelColIndexInFixedOrScrollableTable(lastFind.getCol());
        int tableModelRow = lastFind.getRow();
        if ( tableModelCol != -1) {
            result = analyticsTableModel.getRowCount() > tableModelRow &&
                     analyticsTableModel.isCellMatchingSearch(tableModelRow, tableModelCol);
        }
        return result;
    }

    public boolean isIncludeSubstringsInFilterFind() {
        return includeSubstringsInFindCheckbox.isSelected();
    }

    public void setIncludeSubstringsInFilterFind(boolean include) {
        includeSubstringsInFindCheckbox.setSelected(include);
    }

    private class NextFindResultAction extends AbstractAction {

        private boolean findForward;

        private NextFindResultAction(boolean findForward) {
            super("NextFindResult");
            this.findForward = findForward;
        }

        public void actionPerformed(ActionEvent e) {
            doFindNext(findForward);
        }
    }

    private class ClearSearchAction extends AbstractAction {

        private ClearSearchAction() {
            super("ClearSearchAction", CLEAR_SEARCH_ICON);
        }

        public void actionPerformed(ActionEvent e) {
            clearSearch();
        }
    }

    private class HideSearchBarAction extends AbstractAction {

        private HideSearchBarAction() {
            super("HideSearchBarAction", HIDE_ICON);
        }

        public void actionPerformed(ActionEvent e) {
            showFilterPanel(false);
        }
    }

    private class GoToLastFoundCellAction extends AbstractAction {

        private GoToLastFoundCellAction() {
            super("GoToLastFoundCellAction");
        }

        public void actionPerformed(ActionEvent e) {
            analyticsTable.grabFocus();
            TableCell c =  tableCellFinder.getLastFindResult();
            if ( c != TableCell.NO_MATCH_TABLE_CELL) {
                analyticsTable.setSelectedCellAndGrabFocus(c.getRow(), c.getCol());
            }
        }
    }

    private class FindNextAction extends AbstractAction {

        private FindNextAction() {
            super("Find Next", ARROW_DOWN_BLUE_ICON);
        }

        public void actionPerformed(ActionEvent e) {
            doFindNext(true);
        }
    }

    private class FindPreviousAction extends AbstractAction {

        private FindPreviousAction() {
            super("Find Previous", ARROW_UP_BLUE_ICON);
        }

        public void actionPerformed(ActionEvent e) {
            doFindNext(false);
        }
    }
}
