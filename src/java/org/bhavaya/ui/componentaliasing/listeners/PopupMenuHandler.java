package org.bhavaya.ui.componentaliasing.listeners;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Created by IntelliJ IDEA.
 * User: ga2adaz
 * Date: 19/07/12
 * Time: 12:07
 * Handles displaying the popup menu as well processing selections
 */
public interface PopupMenuHandler extends MouseListener {

    public void mouseClicked(MouseEvent e);

    public void mousePressed(MouseEvent e);

    public void mouseReleased(MouseEvent e);

    public void mouseEntered(MouseEvent e);

    public void mouseExited(MouseEvent e);

    /*
     *  The JPopupMenu being handled
     */
    public JPopupMenu getPopupMenu();

}
