/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.ui;

import java.awt.*;

/**
 * A vertical flow layout is used to lay out components in a panel.
 * It will arrange the components from top to bottom until no more components fit in the same column.
 * The columns can be aligned vertically.
 *
 * @author
 * @version $Revision: 1.4 $
 */
public class VerticalFlowLayout implements LayoutManager {
    public static final int TOP = 1;
    public static final int CENTER = 0;
    public static final int BOTTOM = 2;

    private int align;
    private int hgap;
    private int vgap;
    private boolean maximise;

    /**
     * Constructs a new layout with a centered alignment and a
     * default 5-unit horizontal and vertical gap.
     */
    public VerticalFlowLayout() {
        this(CENTER, 5, 5, false);
    }

    /**
     * Constructs a new layout with the specified alignment and a
     * default 5-unit horizontal and vertical gap.
     * @param align the alignment value
     */
    public VerticalFlowLayout(int align) {
        this(align, 5, 5, false);
    }

    /**
     * Constructs a new layout with the specified alignment and gap
     * values.
     * @param align the alignment value
     * @param hgap the horizontal gap variable
     * @param vgap the vertical gap variable
     */
    public VerticalFlowLayout(int align, int hgap, int vgap) {
        this(align, hgap, vgap, false);
    }

    /**
     * Constructs a new layout with the specified alignment and gap
     * values.
     * @param align the alignment value
     * @param hgap the horizontal gap variable
     * @param vgap the vertical gap variable
     */
    public VerticalFlowLayout(int align, int hgap, int vgap, boolean maximise) {
        if (align != TOP && align != CENTER && align != BOTTOM)
            throw new IllegalArgumentException("illegal alignment value:" + align);
        if (hgap < 0)
            throw new IllegalArgumentException("horizontal gap is less than 0");
        if (vgap < 0)
            throw new IllegalArgumentException("vertical gap is less than 0");
        this.align = align;
        this.hgap = hgap;
        this.vgap = vgap;
        this.maximise = maximise;
    }

    /**
     * Returns the alignment value for this layout (TOP, CENTER, or BOTTOM).
     */
    public int getAlignment() {
        return align;
    }

    /**
     * Sets the alignment value for this layout.
     * @param align the alignment value (TOP, CENTER, or BOTTOM).
     */
    public void setAlignment(int align) {
        if (align != TOP && align != CENTER && align != BOTTOM)
            throw new IllegalArgumentException("illegal alignment value:" + align);
        this.align = align;
    }

    /**
     * Returns the horizontal gap between components.
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components.
     * @param hgap the horizontal gap between components
     */
    public void setHgap(int hgap) {
        if (hgap < 0)
            throw new IllegalArgumentException("horizontal gap is less than 0");
        this.hgap = hgap;
    }

    /**
     * Returns the vertical gap between components.
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components.
     * @param vgap the vertical gap between components
     */
    public void setVgap(int vgap) {
        if (vgap < 0)
            throw new IllegalArgumentException("vertical gap is less than 0");
        this.vgap = vgap;
    }

    /**
     * Adds the specified component to the layout. Not used by this class.
     * @param name the name of the component
     * @param comp the the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout. Not used by this class.
     * @param comp the component to remove
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Returns the preferred dimensions for this layout given the components
     * in the specified target container.
     * @param target the component which needs to be laid out
     * @see Container
     * @see #minimumLayoutSize
     */
    public Dimension preferredLayoutSize(Container target) {
        Dimension dim = new Dimension(0, 0);
        int nmembers = target.getComponentCount();
        for (int i = 0; i < nmembers; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                Dimension d = maximise ? m.getMaximumSize() : m.getPreferredSize();
                dim.width = Math.max(dim.width, d.width);
                if (i > 0)
                    dim.height += vgap;
                dim.height += d.height;
            }
        }
        Insets insets = target.getInsets();
        dim.width += insets.left + insets.right + hgap * 2;
        dim.height += insets.top + insets.bottom + vgap * 2;
        return dim;
    }

    /**
     * Returns the minimum dimensions needed to layout the components
     * contained in the specified target container.
     * @param target the component which needs to be laid out
     * @see #preferredLayoutSize
     */
    public Dimension minimumLayoutSize(Container target) {
        Dimension dim = new Dimension(0, 0);
        int nmembers = target.getComponentCount();
        for (int i = 0; i < nmembers; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                Dimension d = m.getMinimumSize();
                dim.width = Math.max(dim.width, d.width);
                if (i > 0)
                    dim.height += vgap;
                dim.height += d.height;
            }
        }
        Insets insets = target.getInsets();
        dim.width += insets.left + insets.right + hgap * 2;
        dim.height += insets.top + insets.bottom + vgap * 2;
        return dim;
    }

    /**
     * Centers the elements in the specified column, if there is any slack.
     * @param target the component which needs to be moved
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width dimensions
     * @param height the height dimensions
     * @param columnStart the beginning of the column
     * @param columnEnd the the ending of the column
     */
    private void moveComponents(Container target, int x, int y, int width, int height, int columnStart, int columnEnd) {
        switch (align) {
            case TOP:
                break;
            case CENTER:
                y += height / 2;
                break;
            case BOTTOM:
                y += height;
                break;
        }
        for (int i = columnStart; i < columnEnd; i++) {
            Component m = target.getComponent(i);
            Rectangle mbounds = m.getBounds();
            if (m.isVisible()) {
                m.setLocation(x + (width - mbounds.width) / 2, y);
                y += vgap + mbounds.height;
            }
        }
    }

    /**
     * Lays out the container. This method will actually reshape the
     * components in the target in order to satisfy the constraints of
     * the BorderLayout object.
     * @param target the specified component being laid out.
     * @see Container
     */
    public void layoutContainer(Container target) {
        Insets insets = target.getInsets();
        Rectangle bounds = target.getBounds();
        int maxheight = bounds.height - (insets.top + insets.bottom + vgap * 2);
        int maxWidth = bounds.width - (insets.left + insets.right + hgap * 2);
        int nmembers = target.getComponentCount();
        int y = 0;
        int x = insets.left + hgap;
        int maxRowWidth = 0;
        int start = 0;

        for (int i = 0; i < nmembers; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                Dimension d = maximise ? m.getPreferredSize() : m.getPreferredSize();
                if (d.width > maxWidth) d.width = maxWidth;
                m.setSize(d.width, d.height);

                if ((y == 0) || ((y + d.height) <= maxheight)) {
                    if (y > 0)
                        y += vgap;
                    y += d.height;
                    maxRowWidth = Math.max(maxRowWidth, d.width);
                } else {
                    moveComponents(target, x, insets.top + vgap, maxRowWidth, maxheight - y, start, i);
                    x += hgap + maxRowWidth;
                    y = d.height;
                    maxRowWidth = d.width;
                    start = i;
                }
            }
        }
        moveComponents(target, x, insets.top + vgap, maxRowWidth, maxheight - y, start, nmembers);
    }

    /**
     * Returns the string representation of this layout's values.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getClass().getName());
        buffer.append("[hgap=");
        buffer.append(hgap);
        buffer.append(",vgap=");
        buffer.append(vgap);
        switch (align) {
            case TOP:
                buffer.append(",align=top");
                break;
            case CENTER:
                buffer.append(",align=center");
                break;
            case BOTTOM:
                buffer.append(",align=bottom");
                break;
        }
        buffer.append("]");
        return buffer.toString();
    }
}
