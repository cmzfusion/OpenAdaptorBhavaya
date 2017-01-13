package org.bhavaya.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Allows any component to be used as a title renderer instead of just a string.
 *
 * Adapted, license free, from http://www2.gol.com/users/tame/.  Thanks to author who doesn't actually mention his/her
 * name on the website.  It didn't actually work, but the inset stuff was a good grounding.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class ComponentStampBorder extends TitledBorder {

    private JComponent component;
    private JComponent pointlessParent = new JPanel();

    public ComponentStampBorder(JComponent component) {
        this(null, component, LEFT, TOP);
    }

    public ComponentStampBorder(Border border) {
        this(border, null, LEFT, TOP);
    }

    public ComponentStampBorder(Border border, JComponent component) {
        this(border, component, LEFT, TOP);
    }

    public ComponentStampBorder(Border border,JComponent component,int titleJustification,int titlePosition) {
        super(border, null, titleJustification,titlePosition, null, null);
        this.component = component;
        if (border == null) {
            this.border = super.getBorder();
        }
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
        g.fillRect(compR.x, compR.y, compR.width, compR.height);
        g.setColor(col);
        SwingUtilities.paintComponent(g, component, pointlessParent , compR);

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

    public JComponent getTitleComponent() {
        return component;
    }

    public void setTitleComponent(JComponent component) {
        this.component = component;
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
                compR.y = EDGE_SPACING +
                        (borderInsets.top - EDGE_SPACING - TEXT_SPACING - compD.height) / 2;
                break;
            case BELOW_TOP:
                compR.y = borderInsets.top - compD.height - TEXT_SPACING;
                break;
            case ABOVE_BOTTOM:
                compR.y = rect.height - borderInsets.bottom + TEXT_SPACING;
                break;
            case BOTTOM:
                compR.y = rect.height - borderInsets.bottom + TEXT_SPACING +
                        (borderInsets.bottom - EDGE_SPACING - TEXT_SPACING - compD.height) / 2;
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
