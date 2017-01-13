package org.bhavaya.ui.table.formula.conditionalhighlight;

import org.bhavaya.ui.AlwaysOnTopJOptionPane;
import org.bhavaya.ui.table.formula.AbstractFormulaEditorDialog;
import org.bhavaya.ui.table.formula.FormulaException;
import org.bhavaya.ui.view.TableView;
import org.bhavaya.util.ColorCellEditor;
import org.bhavaya.util.ColorCellRenderer;
import org.bhavaya.util.ImageIconCache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Dialog to edit conditional highlighters
 * User: ga2mhana
 * Date: 15/03/11
 * Time: 13:52
 */
public class ConditionalHighlightDialog extends AbstractFormulaEditorDialog {
    private static final ImageIcon UP_ICON = ImageIconCache.getImageIcon("arrow_up_blue.png");
    private static final ImageIcon DOWN_ICON = ImageIconCache.getImageIcon("arrow_down_blue.png");

    private ConditionalHighlightDialogModel model;
    private ColumnWidthTable conditionTable;
    private TableView tableView;
    private String columnIdentifier;
    private HighlightConditionSet highlightConditionSet;

    public ConditionalHighlightDialog(JFrame owner, TableView tableView, String columnIdentifier, HighlightConditionSet highlightConditionSet) throws FormulaException {
        super(owner, "Edit Conditional Highlighting", tableView.getBeanCollectionTableModel(), "conditions");
        highlightConditionSet = new HighlightConditionSet(highlightConditionSet);
        this.model = new ConditionalHighlightDialogModel(tableView.getBeanCollectionTableModel(), highlightConditionSet);
        this.tableView = tableView;
        this.columnIdentifier = columnIdentifier;
        this.highlightConditionSet = highlightConditionSet;

        initComponents(model);
    }

    protected void okPressed() {
        super.okPressed();
        String[] errors = model.checkForErrors();
        if(errors == null) {
            if(errors == null) {
                tableView.getConditionalHighlighter().setColumnCondition(columnIdentifier, highlightConditionSet);
            }
            dispose();
        } else {
            StringBuilder sb = new StringBuilder("Highlight condition setup is invalid:");
            for(String error : errors) {
                sb.append("\n").append(error);
            }
            AlwaysOnTopJOptionPane.showMessageDialog(this, sb.toString(),
                    "Error Updating Highlight Conditions", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected JTable getTable() {
        return conditionTable;
    }

    protected JPanel createCentralPanel() {
        JPanel conditionPanel = new JPanel(new BorderLayout());
        conditionPanel.add(new BoldLabel("Conditions"), BorderLayout.NORTH);

        conditionTable = new ColumnWidthTable(model.getConditionTableModel(), new int[] {20, 100, 130});
        conditionTable.setDefaultRenderer(Color.class, new ColorCellRenderer());
        conditionTable.setDefaultEditor(Color.class, new ColorCellEditor());
        conditionPanel.add(new JScrollPane(conditionTable), BorderLayout.CENTER);

        AbstractAction upAction = new AbstractAction(null, UP_ICON) {
            public void actionPerformed(ActionEvent e) {
                int[] selected = conditionTable.getSelectedRows();
                model.moveConditionsUp(selected);
                conditionTable.selectIndexes(selected);
            }
        };
        AbstractAction addAction = new AbstractAction(null, ADD_ICON) {
            public void actionPerformed(ActionEvent e) {
                model.addCondition();
            }
        };
        AbstractAction removeAction = new AbstractAction(null, REMOVE_ICON) {
            public void actionPerformed(ActionEvent e) {
                int[] selected = conditionTable.getSelectedRows();
                model.removeConditions(selected);
                conditionTable.selectIndexes(selected);
            }
        };
        AbstractAction downAction = new AbstractAction(null, DOWN_ICON) {
            public void actionPerformed(ActionEvent e) {
                int[] selected = conditionTable.getSelectedRows();
                model.moveConditionsDown(selected);
                conditionTable.selectIndexes(selected);
            }
        };
        conditionPanel.add(new VerticalButtonPanel(upAction, addAction, removeAction, downAction), BorderLayout.EAST);

        return conditionPanel;
    }
}
