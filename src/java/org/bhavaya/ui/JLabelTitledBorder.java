package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Allows any component to be used as a title renderer instead of just a string.
 *
 * Adapted, license free, from http://www2.gol.com/users/tame/.  Thanks to author who doesn't actually mention his/her
 * name on the website.  It didn't actually work, but the inset stuff was a good grounding.
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */
public class JLabelTitledBorder extends TitledBorder {

    private JLabel component;
    private JComponent pointlessParent = new JPanel();
    private boolean antiAliased = false;
    private static final int PAD = 4;

    public JLabelTitledBorder(String htmlText, boolean antiAliased) {
        super(null, null, LEFT, TOP, null, null);
        this.antiAliased = antiAliased;
        this.border = super.getBorder();
        this.component = new JLabel(htmlText);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Rectangle borderR = new Rectangle(x + EDGE_SPACING, y + EDGE_SPACING, width - (EDGE_SPACING * 2), height - (EDGE_SPACING * 2));

        Insets borderInsets;
        if (border != null) {
            borderInsets = border.getBorderInsets(c);
        } else {
            borderInsets = new Insets(0, 0, 0, 0);
        }

        Rectangle rect = new Rectangle(x, y, width, height);
        Insets insets = getBorderInsets(c);
        Rectangle compR = getComponentRect(rect, insets);
        int diff;
        switch (titlePosition) {
            case ABOVE_TOP:
                diff = compR.height + TEXT_SPACING;
                borderR.y += diff;
                borderR.height -= diff;
                break;
            case TOP:
            case DEFAULT_POSITION:
                diff = insets.top / 2 - borderInsets.top - EDGE_SPACING;
                borderR.y += diff;
                borderR.height -= diff;
                break;
            case BELOW_TOP:
            case ABOVE_BOTTOM:
                break;
            case BOTTOM:
                diff = insets.bottom / 2 - borderInsets.bottom - EDGE_SPACING;
                borderR.height -= diff;
                break;
            case BELOW_BOTTOM:
                diff = compR.height + TEXT_SPACING;
                borderR.height -= diff;
                break;
        }
        border.paintBorder(c, g, borderR.x, borderR.y,
                borderR.width, borderR.height);

        Color col = g.getColor();
        g.setColor(c.getBackground());
        g.fillRect(compR.x - PAD / 2, compR.y, compR.width + PAD, compR.height);
        g.setColor(col);

        if (antiAliased) {
            Graphics2D graphics2D = (Graphics2D) g;
            Object textAntiAliasingHint = graphics2D.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            SwingUtilities.paintComponent(g, component, pointlessParent, compR);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntiAliasingHint);
        } else {
            SwingUtilities.paintComponent(g, component, pointlessParent, compR);
        }

        component.repaint();
    }


    public Insets getBorderInsets(Component c, Insets insets) {
        Insets borderInsets;
        if (border != null) {
            borderInsets = border.getBorderInsets(c);
        } else {
            borderInsets = new Insets(0, 0, 0, 0);
        }
        insets.top = EDGE_SPACING + TEXT_SPACING + borderInsets.top;
        insets.right = EDGE_SPACING + TEXT_SPACING + borderInsets.right;
        insets.bottom = EDGE_SPACING + TEXT_SPACING + borderInsets.bottom;
        insets.left = EDGE_SPACING + TEXT_SPACING + borderInsets.left;

        if (c == null || component == null) {
            return insets;
        }

        int compHeight = 0;
        if (component != null) {
            compHeight = component.getPreferredSize().height;
        }

        switch (titlePosition) {
            case ABOVE_TOP:
                insets.top += compHeight + TEXT_SPACING;
                break;
            case TOP:
            case DEFAULT_POSITION:
                insets.top += Math.max(compHeight, borderInsets.top) - borderInsets.top;
                break;
            case BELOW_TOP:
                insets.top += compHeight + TEXT_SPACING;
                break;
            case ABOVE_BOTTOM:
                insets.bottom += compHeight + TEXT_SPACING;
                break;
            case BOTTOM:
                insets.bottom += Math.max(compHeight, borderInsets.bottom) - borderInsets.bottom;
                break;
            case BELOW_BOTTOM:
                insets.bottom += compHeight + TEXT_SPACING;
                break;
        }
        return insets;
    }

    public Rectangle getComponentRect(Rectangle rect, Insets borderInsets) {
        Dimension compD = component.getPreferredSize();
        Rectangle compR = new Rectangle(0, 0, compD.width, compD.height);
        switch (titlePosition) {
            case ABOVE_TOP:
                compR.y = EDGE_SPACING;
                break;
            case TOP:
            case DEFAULT_POSITION:
                compR.y = EDGE_SPACING + (borderInsets.top - EDGE_SPACING - TEXT_SPACING - compD.height) / 2;
                break;
            case BELOW_TOP:
                compR.y = borderInsets.top - compD.height - TEXT_SPACING;
                break;
            case ABOVE_BOTTOM:
                compR.y = rect.height - borderInsets.bottom + TEXT_SPACING;
                break;
            case BOTTOM:
                compR.y = rect.height - borderInsets.bottom + TEXT_SPACING + (borderInsets.bottom - EDGE_SPACING - TEXT_SPACING - compD.height) / 2;
                break;
            case BELOW_BOTTOM:
                compR.y = rect.height - compD.height - EDGE_SPACING;
                break;
        }
        switch (titleJustification) {
            case LEFT:
            case DEFAULT_JUSTIFICATION:
                compR.x = TEXT_INSET_H + borderInsets.left;
                break;
            case RIGHT:
                compR.x = rect.width - borderInsets.right - TEXT_INSET_H - compR.width;
                break;
            case CENTER:
                compR.x = (rect.width - compR.width) / 2;
                break;
        }
        return compR;
    }

}


