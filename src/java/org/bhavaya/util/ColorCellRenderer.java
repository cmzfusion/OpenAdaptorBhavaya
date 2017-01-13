package org.bhavaya.util;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Colour table cell renderer
 * User: ga2mhana
 * Date: 26/05/11
 * Time: 12:31
 */
public class ColorCellRenderer implements TableCellRenderer {
    private ColorComponent component = new ColorComponent();

    public Component getTableCellRendererComponent(JTable table, Object color, boolean isSelected, boolean hasFocus, int row, int column) {
        component.setColor((Color) color);
        component.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return component;
    }
}