package org.bhavaya.ui.table;

import org.bhavaya.ui.table.DecimalEditor;
import org.bhavaya.ui.table.DecimalRenderer;
import org.bhavaya.ui.table.AnalyticsTableModel;
import org.bhavaya.ui.DecimalTextField;
import org.bhavaya.util.Quantity;
import org.bhavaya.util.Generic;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Table editor for Quantity. Needs a given beanPath to find the unit for the quantity class to be created in. Takes 
 * into account the value in renderer option.
 * 
 * @author <a href="mailto:Sabine.Haas@drkw.com">Sabine Haas, Dresdner Kleinwort</a>
 * @version $Revision: 1.1 $
 */
public class QuantityEditor extends DecimalEditor {
    private static final double MULTIPLIER_DEFAULT = 1d;

    private String currencyBeanPath;
    private double multiplier = MULTIPLIER_DEFAULT;
    private String currency;

    public QuantityEditor(final DecimalTextField decimalTextField, String unitBeanPath) {
        super(decimalTextField);
        this.currencyBeanPath = unitBeanPath;

        decimalTextField.removeActionListener(delegate);
        delegate = new QuantityEditorDelegate(decimalTextField);
        decimalTextField.addActionListener(delegate);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        setMultiplier(table, row, column);
        setCurrency(table, row, column);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    private void setMultiplier(JTable table, int row, int column) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        if (renderer instanceof DecimalRenderer) {
            double rendererMultiplierer = ((DecimalRenderer) renderer).getMultiplier();
            multiplier = Double.isNaN(rendererMultiplierer) || rendererMultiplierer == Double.MIN_VALUE ? MULTIPLIER_DEFAULT : rendererMultiplierer;
        } else {
            multiplier = MULTIPLIER_DEFAULT;
        }
    }

    private void setCurrency(JTable table, int row, int column) {
        Object[] beansForLocation = ((AnalyticsTableModel) table.getModel()).getBeansForLocation(row, column);
        currency = (String) Generic.get(beansForLocation[0], Generic.beanPathStringToArray(currencyBeanPath));
    }

    private class QuantityEditorDelegate extends EditorDelegate {
        private final DecimalTextField decimalTextField;

        public QuantityEditorDelegate(DecimalTextField decimalTextField) {
            this.decimalTextField = decimalTextField;
        }

        public void setValue(Object value) {
            Double newValue = (value != null) ? new Double(((Quantity) value).getAmount() * multiplier) : null;
            decimalTextField.setValue(newValue);
        }

        public Object getCellEditorValue() {
            Number value = decimalTextField.getValue();
            if (value == null) return null;
            return new Quantity(value.doubleValue() / multiplier, currency);
        }
    }
}