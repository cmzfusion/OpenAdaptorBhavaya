package org.bhavaya.ui.table;

import org.bhavaya.ui.ToolTipFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Provides support for JTable to display full cell content in a tooltip.
 * <p/>
 * There is a set of method calls to JComponent before tooltip is displayed
 * between which we need to share information. Its basically only the mouse position
 * that is missing in the createToolTip method but we can share other stuff as well.
 * <p/>
 * For example how to use this class see the demo code in this file.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public class TableToolTipManager implements ToolTipFactory {

    /**
     * Methods of the JComponent and subsequently of this class must be invoked
     * in following order:
     * <p/>
     * 1. getToolTipText
     * 2. getToolTipLocation
     * 3. createToolTip
     */

    private JTable table;
    private MouseEvent event;
    private TableCellToolTip.TableToolTipRenderer tableToolTipRenderer;
    private Rectangle toolTipRectForTableCell;
    private boolean showTooltip;

    public TableToolTipManager(JTable table) {
        this.table = table;
    }

    public String getToolTipText(MouseEvent event) {
        this.event = event; // for diagnostic purposes only
        Point p = event.getPoint();
        int columnIndex = table.columnAtPoint(p);
        int rowIndex = table.rowAtPoint(p);
        if ((columnIndex != -1) && (rowIndex != -1)) {
            Object cellValue = table.getValueAt(rowIndex, columnIndex);
            if (cellValue != null) {
                tableToolTipRenderer = new TableCellToolTip.TableToolTipRenderer(table, rowIndex, columnIndex);
                toolTipRectForTableCell = TableCellToolTip.getToolTipRectForTableCell(table, rowIndex, columnIndex);
                Dimension minimumTooltipSize = tableToolTipRenderer.getMinimumSize();
                if (minimumTooltipSize.width > toolTipRectForTableCell.width) { // don't display tooltip for cells big enough to display full value of the cell
                    showTooltip = true;
                    return cellValue.toString(); // this string is not actualy used as we will use the table cell renderer to render the actual value;
                }
            }
        }
        showTooltip = false;
        return null;
    }

    public Point getToolTipLocation(MouseEvent event) {
        assert event == this.event : "Unexpected state - we should receive same event in this method as we do in getToolTipText.";
        if (!showTooltip) return null;
        return new Point(toolTipRectForTableCell.x, toolTipRectForTableCell.y);
    }

    public JToolTip createToolTip() {
        assert showTooltip : "I shouldn't get here when not showing tooltip.";
        TableCellToolTip toolTip = new TableCellToolTip();
        toolTip.setComponent(table);
        toolTip.setTableToolTipRenderer(tableToolTipRenderer);
        return toolTip;
    }

    /////////////////////////////////////////////////////////////////////////
    // Following is some demo code
    /////////////////////////////////////////////////////////////////////////

    public static class ExampleTable extends JTable {

        private TableToolTipManager toolTipManager;

        public ExampleTable(final Object[][] rowData, final Object[] columnNames) {
            super(rowData, columnNames);
            toolTipManager = new TableToolTipManager(this);
        }

        public String getToolTipText(MouseEvent event) {
            return toolTipManager.getToolTipText(event);
        }

        public Point getToolTipLocation(MouseEvent event) {
            return toolTipManager.getToolTipLocation(event);
        }

        public JToolTip createToolTip() {
            return toolTipManager.createToolTip();
        }
    }

    static final Object[] demoTableColumnNames = {"a", "b", "c", "d"};

    static final Object[][] demoTableData = {
        {"Long string that doesn't fit into the cell", "150", "Another long string", "55 656 565"},
        {"Long string that doesn't fit into the cell", "150", "Another long string", "55 656 565"},
        {"Long string that doesn't fit into the cell", "150", "Another long string", "55 656 565"},
        {"Long string that doesn't fit into the cell", "150", "Another long string", "55 656 565"}
    };

    public static void main(String[] args) {
        JFrame frame = new JFrame("TableToolTipManager Demo");
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        ExampleTable table = new ExampleTable(demoTableData, demoTableColumnNames);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        contentPane.add(new JScrollPane(table), BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
