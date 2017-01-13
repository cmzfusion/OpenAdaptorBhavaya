package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Tab button to display on bottom/side gutters of a split pane
 * Refactored from SplitControlPane
 * User: ga2mop0
 * Date: 25/07/13
 * Time: 10:24
 */
public class TabButton extends JPanel implements FlashingTabButton {
    private static final int ICON_X_PAD = 6;
    private static final int ICON_Y_PAD = 2;

    private TabTextIcon tabTextIcon;
    private int rotation;

    private boolean selected = false;
    private boolean pressed = false;
    private boolean armed = false;
    private boolean over = false;
    private boolean flashing = false;
    private float flashColorFactor;
    private Color flashColor;

    private List<ActionListener> actionListeners = new ArrayList<>();

    public TabButton(String text, int mnemonic, String tooltip, int rotation) {
        super(new BorderLayout());

        this.rotation = rotation;
        this.tabTextIcon = new TabTextIcon(text, mnemonic, rotation);

        setBorder(null);
        setFocusable(false);
        setToolTipText(tooltip);
        // by registering keyboard action we get keyboard shortcut displayed in a nice way in the tooltip
        if (mnemonic != KeyEvent.VK_UNDEFINED) {
            registerKeyboardAction(null, KeyStroke.getKeyStroke(mnemonic, InputEvent.ALT_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        addMouseListener(new MouseHandler());

        setName("TabButton-" + tooltip);
    }

    private void setFlashing(boolean b) {
        flashing = b;
        flashColorFactor = 0;
        repaint();
    }

    private void flash() {
        flashColorFactor += 0.20f;
        if (flashColorFactor > 2) {
            flashColorFactor = 0;
        }
        repaint();
    }

    /**
     * Static management of flashing buttons
     * Set of buttons to flash is only accessed from Swing thread so no need for synchronisation
     */
    private static final Set<TabButton> buttonsToFlash = new HashSet<>();
    private static Timer buttonFlashTimer = null;

    @Override
    public void startFlashing(final Color color) {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                initFlasherTimer();
                buttonsToFlash.add(TabButton.this);
                flashColor = color;
                setFlashing(true);
            }
        });
    }

    @Override
    public void stopFlashing() {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                buttonsToFlash.remove(TabButton.this);
                setFlashing(false);
            }
        });
    }

    private void initFlasherTimer() {
        if (buttonFlashTimer == null) {
            buttonFlashTimer = new Timer(150, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for(TabButton button : buttonsToFlash) {
                        button.flash();
                    }
                }
            });
            buttonFlashTimer.start();
        }
    }

    @Override
    public void setButtonVisible(final boolean show) {
        UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                setVisible(show);
            }
        });
    }

    private class MouseHandler extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                pressed = true;
                armed = true;
                repaint();
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && pressed) {
                pressed = false;
                armed = false;
                fireActionEvent();
            }
        }

        public void mouseExited(MouseEvent e) {
            over = false;
            if (pressed) armed = false;
            repaint();
        }

        public void mouseEntered(MouseEvent e) {
            over = true;
            if (pressed) armed = true;
            repaint();
        }
    }

    private void fireActionEvent() {
        ActionEvent event = new ActionEvent(this, 0, "");
        for(ActionListener l : actionListeners) {
            l.actionPerformed(event);
        }
    }

    public void addActionListener(ActionListener l) {
        actionListeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
        actionListeners.remove(l);
    }

    public void paint(Graphics g) {
        super.paint(g);

        Color normalColor = getBackground();
        Color rolloverColor = UIUtilities.createDarkerColor(getBackground(), 0.08f);
        Color darkerColour = UIUtilities.createDarkerColor(rolloverColor, 0.18f);

        int x = (getWidth() - tabTextIcon.getIconWidth()) / 2;
        int y = (getHeight() - tabTextIcon.getIconHeight()) / 2;

        Color oldColor = g.getColor();
        if (flashing) {
            float t = flashColorFactor <= 1 ? flashColorFactor : 2 - flashColorFactor;
            g.setColor(UIUtilities.blend(flashColor, getBackground(), t));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
        } else if (selected || armed) {
            g.setColor(darkerColour);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
        } else if (over) {
            g.setColor(rolloverColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
            g.setColor(darkerColour);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 3, 3);
        } else {
            g.setColor(normalColor);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(darkerColour);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 3, 3);
        }
        g.setColor(oldColor);
        tabTextIcon.paintIcon(this, g, x, y);
    }

    public Dimension getPreferredSize() {
        boolean notRotated = rotation == TabTextIcon.NONE;
        int xPad = notRotated ? ICON_X_PAD * 2 : ICON_Y_PAD * 2;
        int yPad = notRotated ? ICON_Y_PAD * 2 : ICON_X_PAD * 2;
        return new Dimension(tabTextIcon.getIconWidth() + xPad, tabTextIcon.getIconHeight() + yPad);
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public boolean isArmed() {
        return armed;
    }

    public void setArmed(boolean armed) {
        this.armed = armed;
    }

    public boolean isOver() {
        return over;
    }

    public void setOver(boolean over) {
        this.over = over;
    }
}
