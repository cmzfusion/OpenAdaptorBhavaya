package org.bhavaya.ui.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 4/26/13
 * Time: 1:20 PM
 */
class FixedTableHeaderCellRenderer implements TableCellRenderer {
    private TableCellRenderer decoratedRenderer;

    private final Color topClr = new Color(220,220,220);
    private final Color bottomClr = new Color(150,150,150);

    public FixedTableHeaderCellRenderer(TableCellRenderer decoratedRenderer) {
        this.decoratedRenderer = decoratedRenderer;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JComponent c = (JComponent)decoratedRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if ( c instanceof MultiLineHeaderRenderer ) {
            MultiLineHeaderRenderer r = (MultiLineHeaderRenderer)c;
            r.setGradientColors(topClr, bottomClr);
        }
        return c;
    }
}