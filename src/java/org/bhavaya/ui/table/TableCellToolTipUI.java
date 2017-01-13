package org.bhavaya.ui.table;


import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import java.awt.*;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.2 $
 */
public class TableCellToolTipUI extends BasicToolTipUI {

    private static final TableCellToolTipUI sharedInstance = new TableCellToolTipUI();

    protected CellRendererPane rendererPane;
    protected TableCellToolTip.TableToolTipRenderer tableToolTipRenderer;

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    private TableCellToolTipUI() {
        super();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        rendererPane = new CellRendererPane();
        c.add(rendererPane);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.remove(rendererPane);
        rendererPane = null;
    }

    public void paint(Graphics g, JComponent c) {
        Component rendererComponent = tableToolTipRenderer.getTableCellRendererComponent();
        Insets insets = tableToolTipRenderer.getInsets();
        Dimension size = c.getSize();
        rendererPane.paintComponent(g, rendererComponent, c, insets.left, insets.top, size.width - insets.left - insets.right, size.height - insets.top - insets.bottom, true);
    }

    public Dimension getPreferredSize(JComponent c) {
        if (tableToolTipRenderer != null) {
            return tableToolTipRenderer.getPreferredSize();
        } else {
            return new Dimension(0, 0);
        }
    }


    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public void setTableToolTipRenderer(TableCellToolTip.TableToolTipRenderer tableToolTipRenderer) {
        this.tableToolTipRenderer = tableToolTipRenderer;
    }
}
