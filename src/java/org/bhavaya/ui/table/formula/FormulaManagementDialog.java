package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.AlwaysOnTopJOptionPane;
import org.bhavaya.ui.table.BeanCollectionTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Dialog for management of formula
 * User: Jon Moore
 * Date: 21/01/11
 * Time: 11:15
 */
public class FormulaManagementDialog extends AbstractFormulaEditorDialog {

    private FormulaManagementDialogModel model;
    private ColumnWidthTable formulaTable;

    public FormulaManagementDialog(JFrame owner, BeanCollectionTableModel beanCollectionTableModel) throws FormulaException {
        super(owner, "Manage Formulas", beanCollectionTableModel, "formulas");
        FormulaManager formulaManager = new FormulaManager(beanCollectionTableModel.getFormulaManager());
        this.model = new FormulaManagementDialogModel(beanCollectionTableModel, formulaManager);

        initComponents(model);
    }

    protected void okPressed() {
        super.okPressed();
        String[] errors = model.updateFormulaManager();
        if(errors == null) {
            dispose();
        } else {
            StringBuilder sb = new StringBuilder("Formula setup is invalid:");
            for(String error : errors) {
                sb.append("\n").append(error);
            }
            AlwaysOnTopJOptionPane.showMessageDialog(this, sb.toString(),
                    "Error Updating Formulas", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected JTable getTable() {
        return formulaTable;
    }

    protected JPanel createCentralPanel() {
        JPanel formulaPanel = new JPanel(new BorderLayout());
        formulaPanel.add(new BoldLabel("Formulas"), BorderLayout.NORTH);
        formulaTable = new ColumnWidthTable(model.getFormulaTableModel(), new int[] {20, 100, 130});
        formulaPanel.add(new JScrollPane(formulaTable), BorderLayout.CENTER);
        AbstractAction addAction = new AbstractAction(null, ADD_ICON) {
            public void actionPerformed(ActionEvent e) {
                model.addFormula();
            }
        };
        AbstractAction removeAction = new AbstractAction(null, REMOVE_ICON) {
            public void actionPerformed(ActionEvent e) {
                int[] selected = formulaTable.getSelectedRows();
                if(selected.length > 0) {
                    model.removeFormulas(selected);
                    formulaTable.selectIndexes(selected);
                }
            }
        };
        formulaPanel.add(new VerticalButtonPanel(addAction, removeAction), BorderLayout.EAST);

        return formulaPanel;
    }

}
