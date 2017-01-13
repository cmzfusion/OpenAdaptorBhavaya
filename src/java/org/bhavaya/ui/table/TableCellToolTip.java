package org.bhavaya.ui.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * ToolTip that can be used to display full value of the table cell by rendering tooltip over the cell.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public class TableCellToolTip extends JToolTip {

    public TableCellToolTip() {
        updateUI();
    }

    public void updateUI() {
        setUI(TableCellToolTipUI.createUI(this));
    }

    public void setComponent(JTable table) {
        super.setComponent(table);
        setBackground(table.getBackground());
    }

    public void setTableToolTipRenderer(TableToolTipRenderer tableToolTipRenderer) {
        ((TableCellToolTipUI) ui).setTableToolTipRenderer(tableToolTipRenderer);
    }

    public static Rectangle getToolTipRectForTableCell(JTable table, int row, int column) {
        Rectangle cellRectIncludingSpacing = table.getCellRect(row, column, true); // this rectangle includes right and bottom border but not the left and top
        cellRectIncludingSpacing.x--;
        cellRectIncludingSpacing.y--;
        cellRectIncludingSpacing.width++;
        cellRectIncludingSpacing.height++;
        return cellRectIncludingSpacing;
    }

    public static class TableToolTipRenderer {
        private int row;
        private int column;
        private JTable table;
        private Insets insets;

        public TableToolTipRenderer(JTable table, int row, int column) {
            this.table = table;
            this.row = row;
            this.column = column;
        }

        public Component getTableCellRendererComponent() {
            TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
            return table.prepareRenderer(cellRenderer, row, column);
        }

        /**
         * Preferred size of the tooltip is:
         * - preferred width of the renderer component but at least the width of the cell.
         * - height of the cell including spacing + border
         */
        public Dimension getPreferredSize() {
            Component rendererComponent = getTableCellRendererComponent();
            Dimension preferredSize = rendererComponent.getPreferredSize();
            Rectangle toolTipRect = getToolTipRectForTableCell(table, row, column);
            Insets insets = getInsets();
            preferredSize.width += insets.left + insets.right + 5;
            if (preferredSize.width < toolTipRect.width) {
                preferredSize.width = toolTipRect.width;
            }
            preferredSize.height = toolTipRect.height;
            return preferredSize;
        }

        /**
         * Returns the minimum size to display full cell value
         */
        public Dimension getMinimumSize() {
            Component rendererComponent = getTableCellRendererComponent();
            Dimension ret = rendererComponent.getPreferredSize();
            Insets insets = getInsets();
            ret.width += insets.left + insets.right;
            return ret;
        }

        /**
         * Returns insets including 1px for border.
         */
        public Insets getInsets() {
            if (insets == null) {
                int rowMargin = table.getRowMargin();   // margin already includes 1px for right and bottom border line
                int columnMargin = table.getColumnModel().getColumnMargin();
                int top = 1 + rowMargin / 2; // 1px for border + margin
                int left = 1 + columnMargin / 2;
                int bottom = rowMargin - top + 1;
                int right = columnMargin - left + 1;
                insets = new Insets(top, left, bottom, right);
            }
            return insets;
        }
    }
}
