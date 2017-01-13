package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A JLabel that mimics the hyper link behaviour. Fires an action event on clicking the link.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class HyperLink extends JLabel {

    private ArrayList actionListeners = new ArrayList();
    private String text;
    private String actionCommand;

    public HyperLink(Action action) {
        configureFromAction(action);
        init();
    }

    public HyperLink(String text) {
        this.text = text;
        init();
    }

    private void init() {
        setForeground();
        setFont(getFont().deriveFont(Font.PLAIN));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                setText(isEnabled());
            }

            public void mouseExited(MouseEvent e) {
                setText(false);
            }

            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && isEnabled()) {
                    fireActionPerformed();
                }
            }
        });
        setText(false);
    }

    private void configureFromAction(Action action) {
        text = (String) action.getValue(Action.NAME);
        actionCommand = (String) action.getValue(Action.ACTION_COMMAND_KEY);
        addActionListener(action);
    }

    protected void setText(boolean underlined) {
        if (underlined) {
            setText("<html><a href='#'>" + text + "</a></html>");
        } else {
            setText("<html><a>" + text + "</a></html>");
        }
    }

    public void addActionListener(ActionListener l) {
        actionListeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
        actionListeners.remove(l);
    }

    protected void fireActionPerformed() {
        fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionCommand));
    }

    protected void fireActionPerformed(ActionEvent event) {
        Iterator it = actionListeners.iterator();
        while (it.hasNext()) {
            ActionListener listener = (ActionListener) it.next();
            listener.actionPerformed(event);
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setForeground();
    }

    public void setForeground() {
        if (isEnabled()) {
            setForeground(Color.BLUE);
        } else {
            setForeground(Color.GRAY);
        }
    }
}
