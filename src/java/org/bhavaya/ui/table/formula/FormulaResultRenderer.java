package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.table.DecimalRenderer;
import org.bhavaya.ui.table.PartialBucketValue;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Table cell renderer for FormulaResults
 * User: Jon Moore
 * Date: 27/01/11
 * Time: 14:29
 */
public class FormulaResultRenderer extends DefaultTableCellRenderer {

    private DecimalRenderer decimalRenderer;


    public FormulaResultRenderer() {
        this(new DecimalRenderer());
    }

    public FormulaResultRenderer(DecimalRenderer decimalRenderer) {
        this.decimalRenderer = decimalRenderer;
        if(decimalRenderer != null) {
            decimalRenderer.setHorizontalAlignment(getHorizontalAlignment());
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column) {
        if(value instanceof Number || value instanceof PartialBucketValue) {
            return decimalRenderer.getTableCellRendererComponent(table, value,
                          isSelected, hasFocus, row, column);
        }
        if(value instanceof FormulaResult) {
            FormulaResult formulaResult = (FormulaResult)value;
            if(formulaResult.isDouble()) {
                return decimalRenderer.getTableCellRendererComponent(table, formulaResult.getObjectResult(),
                              isSelected, hasFocus, row, column);
            }
            return super.getTableCellRendererComponent(table, formulaResult.getObjectResult(),
                    isSelected, hasFocus, row, column);
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    @Override
    public void setHorizontalAlignment(int alignment) {
        super.setHorizontalAlignment(alignment);
        if(decimalRenderer != null) {
            decimalRenderer.setHorizontalAlignment(alignment);
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormulaResultRenderer)) return false;

        final FormulaResultRenderer renderer = (FormulaResultRenderer) o;
        return renderer.decimalRenderer.equals(decimalRenderer);
    }

    public int hashCode() {
        return decimalRenderer.hashCode();
    }
}
