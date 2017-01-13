package org.bhavaya.ui.table;

import org.bhavaya.util.Numeric;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * boolean table renderer that is pretty loose with its data.
 * send it strings, booleans, numbers, it wont care.
 * for numbers == 0 it shows false, else true;
 * for strings it uses Boolean.valueOf(String)
 * @author Daniel van Enckevort
 * @version $Revision: 1.4 $
 */
public class BooleanTableCellRenderer extends JCheckBox implements TableCellRenderer {

    public BooleanTableCellRenderer() {
        super();
        setHorizontalAlignment(JLabel.CENTER);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        }
        else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        boolean selected = false;
        if (value instanceof Boolean) {
            selected = ((Boolean)value).booleanValue();
        } else if (value instanceof Number) {
            if (((Number)value).doubleValue() != 0) selected = true;
        } else if (value instanceof Numeric) {
            if (((Numeric)value).doubleValue() != 0) selected = true;
        } else if (value instanceof String) {
            selected = Boolean.valueOf((String) value).booleanValue();
        }
        setSelected(selected);
        return this;
    }
}
