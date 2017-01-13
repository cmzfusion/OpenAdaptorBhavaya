package org.bhavaya.ui.table;

import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Cell renderer that converts bean paths to a readable form
 * User: ga2mhana
 * Date: 10/02/11
 * Time: 16:54
 */
public class ColumnPathRenderer implements TableCellRenderer {
    private final TableCellRenderer delegate;

    public ColumnPathRenderer() {
        this(new DefaultTableCellRenderer());
    }

    public ColumnPathRenderer(TableCellRenderer delegate) {
        this.delegate = delegate;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String formattedValue = Utilities.getDisplayNameForPropertyPath(value.toString());
        return delegate.getTableCellRendererComponent(table, formattedValue, isSelected, hasFocus, row, column);
    }
}
