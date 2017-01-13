package org.bhavaya.ui.table;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 21-Oct-2008
* Time: 12:59:09
*/
class PivotAwareTableCellRenderer implements TableCellRenderer {
    private AnalyticsTableModel analyticsTableModel;
    private TableCellRenderer decoratedRenderer;
    private Color lockedPivotColor = new Color(235,231,157);

    public PivotAwareTableCellRenderer(AnalyticsTableModel analyticsTableModel, TableCellRenderer decoratedRenderer) {
        this.analyticsTableModel = analyticsTableModel;
        this.decoratedRenderer = decoratedRenderer;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = decoratedRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        TableColumn col = table.getColumnModel().getColumn(column);
        Object identifier = analyticsTableModel.getColumnKey(col.getModelIndex());
        if ( c instanceof MultiLineHeaderRenderer ) {
            MultiLineHeaderRenderer r = (MultiLineHeaderRenderer)c;
            if ( analyticsTableModel.isPivotGeneratedColumn(identifier) &&
                 analyticsTableModel.getTablePivoter().isLockedPivotColumn((PivotTableModel.GeneratedColumnKey)identifier)) {
                r.setGradientColors(Color.WHITE, lockedPivotColor);
            } else {
                r.setDefaultGradientColors();
            }
        }

        return c;
    }
}
