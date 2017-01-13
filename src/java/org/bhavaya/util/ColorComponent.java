package org.bhavaya.util;

import javax.swing.*;
import java.awt.*;

/**
 * Component to display a colour (used in ColorCellEditor and ColorCellRenderer)
 * User: Jon Moore
 * Date: 26/05/11
 * Time: 12:31
 */
public class ColorComponent extends JComponent {
    private Color color = null;
    private Color background = null;

    public ColorComponent() {
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int border = 3;
        int height = getHeight(), width = getWidth();
        g.setColor(background);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.black);
        g.drawRect(border, border, width - (2 * border) - 1, height - (2 * border) - 1);
        g.setColor(color);
        g.fillRect(border + 1, border + 1, width - (2 * border) - 2, height - (2 * border) - 2);
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setBackground(Color background) {
        this.background = background;
    }
}