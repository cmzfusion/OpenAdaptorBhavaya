package org.bhavaya.ui.componentaliasing.listeners;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Created ga2adaz
 * Displays a simple pop menu. The popup to display as well as selection handler is provided by the implementing class.
 */
public abstract class AbstractPopUpMenuHandler implements PopupMenuHandler{

    public abstract JPopupMenu getPopupMenu();

    public void mouseClicked(MouseEvent e) {
        displayPopUp(e);
    }

    public void mousePressed(MouseEvent e) {
        displayPopUp(e);
    }

    //On windows right click is triggered after the mouse had been released
    public void mouseReleased(MouseEvent e) {
        displayPopUp(e);
    }

    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void displayPopUp(MouseEvent e) {
        if(e.isPopupTrigger()) {
            Component src = (Component) e.getSource();
            getPopupMenu().show(src, e.getX(), e.getY());
        }
    }



}
