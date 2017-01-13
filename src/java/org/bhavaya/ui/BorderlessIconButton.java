package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 14-Sep-2009
 * Time: 12:29:04
 *
 * An icon button with a rollover effect which makes the icon darker
 * I'm not sure this was the best way to achieve this result, sorry!
 * But I had to deliver something within 20 mins or so
 */
public class BorderlessIconButton extends JButton {

    private int alphaTransparancyForRollover = 70;

    public BorderlessIconButton(Action a) {
        super(a);
        initialize(getIcon(), false);
        setText("");
    }

    public BorderlessIconButton(ImageIcon i)  {
        super(i);
        initialize(i, false);
    }

    public BorderlessIconButton(ImageIcon i, boolean borderPainted) {
        super(i);
        initialize(i, borderPainted);
    }

    private void initialize(Icon i, boolean borderPainted) {
        setBorderPainted(borderPainted);
        setContentAreaFilled(false);
        setRolloverEnabled(true);
        setRolloverIcon(new DarkerIcon(getIcon()));
        setIconTextGap(0);
        if ( i != null) {
            setPreferredSize(new Dimension(i.getIconWidth(), i.getIconHeight()));
        }
    }

    public boolean isFocusable() {
        return false;
    }

    private class DarkerIcon implements Icon {

        private Icon wrappedIcon;

        private DarkerIcon(Icon wrappedIcon) {
            this.wrappedIcon = wrappedIcon;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            wrappedIcon.paintIcon(c, g, x, y);
            Color bg = c.getBackground();
            g.setColor(new Color(bg.getRed(),bg.getGreen(),bg.getBlue(),alphaTransparancyForRollover));
            g.fillRect(x, y, getIconWidth(), getIconHeight());
        }

        public int getIconWidth() {
            return wrappedIcon.getIconWidth();
        }

        public int getIconHeight() {
            return wrappedIcon.getIconHeight();
        }
    }
}
