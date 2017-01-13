package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.AlwaysOnTopJOptionPane;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.view.Workspace;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.List;

/**
 * Model for Symbol mapping editor
 * User: ga2mhana
 * Date: 16/03/11
 * Time: 12:15
 */
public class SymbolMappingsEditorModel {
    private BeanCollectionTableModel beanCollectionTableModel;
    private AbstractTableModel symbolTableModel;
    private SymbolMappings symbolMappings;
    private boolean okBeingPressed;

    public SymbolMappingsEditorModel(BeanCollectionTableModel beanCollectionTableModel, SymbolMappings symbolMappings)  {
        this.beanCollectionTableModel = beanCollectionTableModel;
        this.symbolMappings = symbolMappings;
        symbolTableModel = new SymbolTableModel();
    }

    public BeanCollectionTableModel getBeanCollectionTableModel() {
        return beanCollectionTableModel;
    }

    public TableModel getSymbolTableModel() {
        return symbolTableModel;
    }

    public void addSymbol() {
        symbolMappings.addSymbolMapping();
        int index = symbolMappings.getSymbolBeanPathPairs().size()-1;
        symbolTableModel.fireTableRowsInserted(index, index);
    }

    public void removeSymbols(int[] indexes) {
        for(int i=indexes.length-1; i>=0; i--) {
            symbolMappings.removeSymbolAt(indexes[i]);
            symbolTableModel.fireTableRowsDeleted(indexes[i], indexes[i]);
        }
    }

    public boolean isOkBeingPressed() {
        return okBeingPressed;
    }

    public void setOkBeingPressed(boolean okBeingPressed) {
        this.okBeingPressed = okBeingPressed;
    }

    private class SymbolTableModel extends AbstractTableModel {
        private List<SymbolMappings.SymbolBeanPathPair> pairs = symbolMappings.getSymbolBeanPathPairs();
        private final String[] COLUMN_NAMES = {"Symbol", "Column Path"};

        public int getRowCount() {
            return pairs.size();
        }

        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            SymbolMappings.SymbolBeanPathPair pair = pairs.get(rowIndex);
            if(columnIndex == 0) {
                return pair.getSymbol();
            }
            return pair.getBeanPath();
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String value = aValue == null ? null : aValue.toString();
            // This check seems unnecessary but have had range check exceptions thrown when editingStopped()
            // is called after removing a row from the symbol table
            if(rowIndex < pairs.size()) {
                SymbolMappings.SymbolBeanPathPair pair = pairs.get(rowIndex);
                if(columnIndex == 0) {
                    try {
                        pair.setSymbol(value);
                    } catch (FormulaException e) {
                        AlwaysOnTopJOptionPane.showMessageDialog(Workspace.getInstance().getApplicationFrame().getContentPane(),
                                e.getMessage(), "Error setting symbol", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    pair.setBeanPath(value);
                }
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }

}
