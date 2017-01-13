package org.bhavaya.ui.ghost;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class GhostComponentAdapter extends MouseAdapter
{
    protected GhostGlassPane glassPane;
	private GhostDropListener listener;

    public GhostComponentAdapter(GhostGlassPane glassPane, GhostDropListener listener) {
        this.glassPane = glassPane;
        this.listener = listener;
    }

    public void mousePressed(MouseEvent e) {
        Component c = e.getComponent();

        BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        c.paint(g);

        glassPane.setVisible(true);

        Point p = (Point) e.getPoint().clone();
        SwingUtilities.convertPointToScreen(p, c);
        SwingUtilities.convertPointFromScreen(p, glassPane);

        glassPane.setPoint(p);
        glassPane.setOffset(e.getPoint().x, e.getPoint().y);
        glassPane.setImage(image);
        glassPane.repaint();
    }

    public void mouseReleased(MouseEvent e) {
        Component c = e.getComponent();

        Point p = (Point) e.getPoint().clone();
        SwingUtilities.convertPointToScreen(p, c);
        glassPane.setVisible(false);
        glassPane.setImage(null);

        listener.ghostDropped(p);
    }
}