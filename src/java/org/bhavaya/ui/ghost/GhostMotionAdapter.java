package org.bhavaya.ui.ghost;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class GhostMotionAdapter extends MouseMotionAdapter
{
    private GhostGlassPane glassPane;
    private Component container;

	public GhostMotionAdapter(GhostGlassPane glassPane, Component container) {
		this.glassPane = glassPane;
        this.container = container;
	}

	public void mouseDragged(MouseEvent e){
        Component c = e.getComponent();
        Point gpPoint = (Point) e.getPoint().clone();
        SwingUtilities.convertPointToScreen(gpPoint, c);
        Point containerPoint = (Point)gpPoint.clone();
        SwingUtilities.convertPointFromScreen(gpPoint, glassPane);
        SwingUtilities.convertPointFromScreen(containerPoint, container);
        if(container != null) {
            //check if we're in the outer container
            glassPane.setVisible(container.getBounds().contains(containerPoint));
        }
        glassPane.setPoint(gpPoint);
        glassPane.repaint();
    }
}