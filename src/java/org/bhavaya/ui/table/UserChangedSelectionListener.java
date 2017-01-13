package org.bhavaya.ui.table;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Combination key and mouse listener that detects when a table selection change occurs
 * as a result of mouse click or key press
 * User: ga2mhan
 * Date: 18/10/11
 * Time: 14:44
 */
public abstract class UserChangedSelectionListener extends MouseAdapter implements KeyListener {
    @Override
    public void mouseClicked(MouseEvent e) {
        userChangedSelection();
    }

    public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_UP ||
                e.getKeyCode() == KeyEvent.VK_DOWN ||
                e.getKeyCode() == KeyEvent.VK_PAGE_UP ||
                e.getKeyCode() == KeyEvent.VK_PAGE_DOWN ||
                e.getKeyCode() == KeyEvent.VK_HOME ||
                e.getKeyCode() == KeyEvent.VK_END) {
            userChangedSelection();
        }
    }

    protected abstract void userChangedSelection();

    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {}

}
