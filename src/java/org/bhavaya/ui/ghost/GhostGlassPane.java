package org.bhavaya.ui.ghost;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by IntelliJ IDEA.
 * User: ga2mhana
 * Date: 04/05/11
 * Time: 12:22
 * To change this template use File | Settings | File Templates.
 */
public class GhostGlassPane extends JPanel {
	private AlphaComposite composite;
    private BufferedImage dragged = null;
    private Point location = new Point(0, 0);
    private int xOffset = 0;
    private int yOffset = 0;

    public GhostGlassPane() {
        setOpaque(false);
        composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);
    }

    public void setImage(BufferedImage dragged) {
        this.dragged = dragged;
    }

    public void setOffset(int xOffset, int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public void setPoint(Point location) {
        this.location = location;
    }

    public void paintComponent(Graphics g) {
        if (dragged == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setComposite(composite);
        g2.drawImage(dragged,
                     (int)location.getX() - xOffset,
                     (int)location.getY() - yOffset,
                     null);
    }
}
