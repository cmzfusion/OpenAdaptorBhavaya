package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.AlwaysOnTopJOptionPane;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.view.Workspace;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.List;

/**
 * The ui model classes and operations required by the formula management dialog
 * User: Jon Moore
 * Date: 21/01/2011
 * Time: 9:35:42 AM
 */
class FormulaManagementDialogModel extends SymbolMappingsEditorModel {

    private FormulaManager formulaManager;

    private AbstractTableModel formulaTableModel;

    public FormulaManagementDialogModel(BeanCollectionTableModel beanCollectionTableModel, FormulaManager formulaManager) throws FormulaException {
        super(beanCollectionTableModel, formulaManager.getSymbolMappings());
        this.formulaManager = formulaManager;
        this.formulaTableModel = new FormulaTableModel();
    }

    public FormulaManager getFormulaManager() {
        return formulaManager;
    }

    public TableModel getFormulaTableModel() {
        return formulaTableModel;
    }

    public void addFormula() {
        FormulaFactory factory = FormulaUtils.getFactoryInstance();
        formulaManager.addFormula(factory.createFormula());
        int index = formulaManager.getAllFormulas().size()-1;
        formulaTableModel.fireTableRowsInserted(index, index);
    }

    public void removeFormulas(int[] indexes) {
        for(int i=indexes.length-1; i>=0; i--) {
            formulaManager.removeFormulaAt(indexes[i]);
            formulaTableModel.fireTableRowsDeleted(indexes[i], indexes[i]);
        }
    }

    String[] updateFormulaManager() {
        String[] errors = formulaManager.validate();
        if(errors == null) {
            getBeanCollectionTableModel().setFormulaManager(formulaManager);
        }
        return errors;
    }


    private class FormulaTableModel extends AbstractTableModel {

        private List<Formula> list = formulaManager.getAllFormulas();
        private final String[] COLUMN_NAMES = {" ", "Formula Name", "Expression"};

        public int getRowCount() {
            return list.size();
        }

        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Formula formula = list.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return formula.isEnabled();
                case 1:
                    return formula.getName();
                case 2:
                    return formula.getExpression();
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // This check seems unnecessary but have had range check exceptions thrown when editingStopped()
            // is called after removing a row from the symbol table
            if(rowIndex < list.size()) {
                Formula formula = list.get(rowIndex);
                switch(columnIndex) {
                    case 0:
                        formula.setEnabled((Boolean) aValue);
                        break;
                    case 1:
                        formula.setName(aValue.toString());
                        break;
                    case 2:
                        try {
                            formula.setExpression(aValue.toString());
                        } catch (FormulaException e) {
                            if(!isOkBeingPressed()) {
                                AlwaysOnTopJOptionPane.showMessageDialog(Workspace.getInstance().getApplicationFrame().getContentPane(),
                                        "Formula \""+aValue+"\" is invalid", "Error setting formula expression", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        break;
                    default:
                        break;
                }
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }
    }
}
