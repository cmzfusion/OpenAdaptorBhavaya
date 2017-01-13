package org.bhavaya.ui.table.formula.conditionalhighlight;

import org.bhavaya.ui.AlwaysOnTopJOptionPane;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.ui.table.formula.SymbolMappingsEditorModel;
import org.bhavaya.ui.table.formula.*;
import org.bhavaya.ui.view.Workspace;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Model for conditional highlight editor
 * User: ga2mhana
 * Date: 15/03/11
 * Time: 13:55
 */
public class ConditionalHighlightDialogModel extends SymbolMappingsEditorModel {
    private AbstractTableModel conditionTableModel;
    private HighlightConditionSet highlightConditionSet;

    public ConditionalHighlightDialogModel(BeanCollectionTableModel beanCollectionTableModel, HighlightConditionSet highlightConditionSet) {
        super(beanCollectionTableModel, highlightConditionSet.getSymbolMappings());
        this.highlightConditionSet = highlightConditionSet;
        this.conditionTableModel = new ConditionTableModel();
    }

    public TableModel getConditionTableModel() {
        return conditionTableModel;
    }

    String[] checkForErrors() {
        List<String> errors = new ArrayList<String>();
        errors.addAll(Arrays.asList(highlightConditionSet.getSymbolMappings().validate()));
        List<HighlightCondition> highlightConditions = highlightConditionSet.getHighlightConditions();
        List<String> allFormulaSymbols = new LinkedList<String>();
        for(HighlightCondition highlightCondition : highlightConditions) {
            allFormulaSymbols.add(highlightCondition.getConditionFormula().getSymbol());
        }
        for(HighlightCondition highlightCondition : highlightConditions) {
            Formula formula = highlightCondition.getConditionFormula();
            if(formula.hasExpression()) {
                try {
                    formula.parseExpression();
                } catch (FormulaException e) {
                    errors.add("Condition \""+formula.getExpression()+"\" is invalid");
                }
            }
            for(String symbol : formula.getSymbols()) {
                if(!highlightConditionSet.getSymbolMappings().containsSymbol(symbol) && !HighlightCondition.CURRENT_COL.equals(symbol)) {
                    errors.add("Condition \""+formula.getExpression()+"\" depends on an invalid symbol \""+symbol+"\"");
                }
            }
        }

        return errors.isEmpty() ? null : errors.toArray(new String[errors.size()]);
    }



    public void addCondition() {
        highlightConditionSet.addHighlightCondition();
        int index = highlightConditionSet.getHighlightConditions().size()-1;
        conditionTableModel.fireTableRowsInserted(index, index);
    }

    public void removeConditions(int[] indexes) {
        if(indexes != null) {
            List<HighlightCondition> conditions = highlightConditionSet.getHighlightConditions();
            for(int i=indexes.length-1; i>=0; i--) {
                conditions.remove(indexes[i]);
                conditionTableModel.fireTableRowsDeleted(indexes[i], indexes[i]);
            }
        }
    }

    public void moveConditionsUp(int[] indexes) {
        if(indexes != null && indexes[0] > 0) {
            for(int i=0; i<indexes.length; i++) {
                swapCondition(--indexes[i]);
            }
            conditionTableModel.fireTableDataChanged();
        }
    }

    public void moveConditionsDown(int[] indexes) {
        List<HighlightCondition> conditions = highlightConditionSet.getHighlightConditions();
        if(indexes != null && indexes[indexes.length-1] < conditions.size()-1) {
            for(int i=indexes.length-1; i>=0; i--) {
                swapCondition(indexes[i]++);
            }
            conditionTableModel.fireTableDataChanged();
        }
    }

    private void swapCondition(int index) {
        //Swap the condition at index with that at index+1
        List<HighlightCondition> conditions = highlightConditionSet.getHighlightConditions();
        HighlightCondition condition = conditions.remove(index+1);
        conditions.add(index, condition);
    }

    private class ConditionTableModel extends AbstractTableModel {

        private List<HighlightCondition> list = highlightConditionSet.getHighlightConditions();
        private final String[] COLUMN_NAMES = {" ", "Condition", "Colour"};

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
            HighlightCondition condition = list.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return condition.isEnabled();
                case 1:
                    return condition.getConditionFormula().getExpression();
                case 2:
                    return condition.getColor();
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // This check seems unnecessary but have had range check exceptions thrown when editingStopped()
            // is called after removing a row from the symbol table
            if(rowIndex < list.size()) {
                HighlightCondition condition = list.get(rowIndex);
                switch(columnIndex) {
                    case 0:
                        condition.setEnabled((Boolean) aValue);
                        break;
                    case 1:
                        try {
                            condition.getConditionFormula().setExpression(aValue.toString());
                        } catch (FormulaException e) {
                            if(!isOkBeingPressed()) {
                                AlwaysOnTopJOptionPane.showMessageDialog(Workspace.getInstance().getApplicationFrame().getContentPane(),
                                        "Condition \""+aValue+"\" is invalid", "Error setting condition expression", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        break;
                    case 2:
                        condition.setColor((Color) aValue);
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
            switch (columnIndex) {
                case 0:
                    return Boolean.class;
                case 1:
                    return String.class;
                case 2:
                    return Color.class;
            }

            return super.getColumnClass(columnIndex);
        }
    }
}
