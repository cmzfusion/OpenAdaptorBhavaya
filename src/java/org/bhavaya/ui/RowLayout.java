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

import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.awt.*;
import java.beans.Encoder;
import java.beans.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A layout manager that is behaves similarly to a flow layout, except that it introduces the concept of
 * a row.  Components are added to rows with optional constraints and rows are added to the layout.
 *
 * @author Brendon McLean
 * @version $Revision: 1.13.6.1 $
 */
public class RowLayout implements LayoutManager2 {
    static {
        BeanUtilities.addPersistenceDelegate(RowLayout.class, new BhavayaPersistenceDelegate(new String[]{"preferredWidth", "vGap", "dynamicLayout"}) {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                super.initialize(type, oldInstance, newInstance, out);
                RowLayout newLayout = (RowLayout) newInstance;
                RowLayout oldLayout = (RowLayout) oldInstance;
                int count = oldLayout.getRowCount();
                for (int i = 0; i < count; i++) {
                    Row row = oldLayout.getRow(i);
                    out.writeStatement(new Statement(newLayout, "addRow", new Object[]{row}));
                }
            }
        });
        BeanUtilities.addPersistenceDelegate(RowLayout.Row.class, new BhavayaPersistenceDelegate(new String[]{"defaultHGap", "horizontalAlign", "verticalAlign", "justify"}) {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                super.initialize(type, oldInstance, newInstance, out);
                Row newRow = (Row) newInstance;
                Row oldRow = (Row) oldInstance;
                int count = oldRow.getComponentCount();
                for (int i = 0; i < count; i++) {
                    Component c = oldRow.getComponent(i);
                    WidthConstraint constraint = oldRow.getConstraint(c);
                    out.writeStatement(new Statement(newRow, "addComponent", new Object[]{c, constraint, new Integer(-1)}));
                }
            }
        });
        BeanUtilities.addPersistenceDelegate(FixedWidthConstraint.class, new BhavayaPersistenceDelegate(new String[]{"width"}));
        BeanUtilities.addPersistenceDelegate(PreferredWidthConstraint.class, new BhavayaPersistenceDelegate(new String[]{"modelComponent"}));
        BeanUtilities.addPersistenceDelegate(RelativeWidthConstraint.class, new BhavayaPersistenceDelegate(new String[]{"relativeWidthRatio"}));
    }

    public static final int TOP = 0;
    public static final int MIDDLE = 1;
    public static final int BOTTOM = 2;

    public static final int LEFT = 0;
    public static final int CENTRE = 1;
    public static final int RIGHT = 2;


    private Dimension maximumLayoutSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private ArrayList rows = new ArrayList();

    private int preferredWidth;
    private int vGap;
    private boolean dynamicLayout;


    /**
     * @param preferredWidth The preferred row width (or wrapping width)
     */
    public RowLayout(int preferredWidth, int vGap) {
        this(preferredWidth, vGap, true);
    }

    public RowLayout(int preferredWidth, int vGap, boolean dynamicLayout) {
        this.preferredWidth = preferredWidth;
        this.vGap = vGap;
        this.dynamicLayout = dynamicLayout;
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public int getvGap() {
        return vGap;
    }

    public boolean isDynamicLayout() {
        return dynamicLayout;
    }

    public void addRow(Row row) {
        rows.add(row);
    }

    public void addRow(int i, Row newRow) {
        rows.add(i, newRow);
    }

    public Row getRow(int index) {
        return (Row) rows.get(index);
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getVGap() {
        return vGap;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LayoutManager methods

    public Dimension maximumLayoutSize(Container target) {
        return maximumLayoutSize;
    }

    public Dimension preferredLayoutSize(Container parent) {
        return minimumLayoutSize(parent);
    }

    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(preferredWidth, getHeight(parent));
    }

    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int targetWidth = parent.getSize().width - (insets.left + insets.right);

        int y = insets.top;

        for (int i = 0; i < rows.size(); i++) {
            Row row = (Row) rows.get(i);
            row.setYOffset(y);
            if (dynamicLayout) row.recalcMaxHeight();
            int rowWidth = row.getWidth(targetWidth);

            /* Floating point is essential here because of integer rounding.  Maintaining position and hgap in
               floating poing allows us to avoid the round errors and ensure that the rightmost component will
               be flush against the right margin if justify is on */
            float x = insets.left;
            float hGap;

            if (row.isJustify()) {
                hGap = (float) (targetWidth - (rowWidth - row.getWhitespaceWidth())) / (float) (row.getComponentCount() - 1);
            } else {
                hGap = (float) row.getDefaultHGap();

                switch (row.getHorizontalAlign()) {
                    case RowLayout.LEFT:
                        x += 0;
                        break;
                    case RowLayout.CENTRE:
                        x += (targetWidth - rowWidth) / 2d;
                        break;
                    case RowLayout.RIGHT:
                        x += targetWidth - rowWidth;
                        break;
                }
            }

            int maxHeight = parent.getHeight() - y - insets.bottom;
            int rowHeight = Math.min(row.getHeight(), maxHeight);
            for (int j = 0; j < row.getComponentCount(); j++) {
                Component component = row.getComponent(j);
                WidthConstraint widthConstraint = row.getConstraint(component);

                int componentWidth = widthConstraint.getWidth(component, targetWidth, Math.round(x) - insets.left);

                layoutComponent(component, componentWidth, Math.round(x), y, rowHeight, row.getVerticalAlign());

                x += hGap + (float) componentWidth;
            }

            y += vGap + row.getHeight();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Private methods

    private void layoutComponent(Component component, int componentWidth, int x, int y, int availableHeight, int vAlign) {
        Dimension componentSize = component.getPreferredSize();
        componentSize.width = componentWidth;
        /*if (!dynamicLayout) */componentSize.height = Math.min(componentSize.height, availableHeight);

        // Modify y to reflect the desired vertical alignment
        switch (vAlign) {
            case TOP:
                // Do nothing, the existing y value is already at the top
                break;
            case MIDDLE:
                y += (availableHeight - componentSize.height) / 2;
                break;
            case BOTTOM:
                y += availableHeight - componentSize.height;
                break;
        }

        component.setLocation(x, y);
        component.setSize(componentSize);
    }

    private int getHeight(Container parent) {
        Insets insets = parent.getInsets();
        int result = insets.top;

        for (Iterator iterator = rows.iterator(); iterator.hasNext();) {
            Row row = (Row) iterator.next();
            result += row.getHeight() + vGap;
        }

        return result - vGap + insets.bottom;
    }

    public Component getComponentAtIndex(int index) {
        int currentIndex = 0;
        for (Iterator iterator = rows.iterator(); iterator.hasNext();) {
            Row row = (Row) iterator.next();
            for (int i = 0; i < row.getComponentCount(); i++) {
                if (currentIndex == index) {
                    return row.getComponent(i);
                }
                currentIndex++;
            }
        }
        return null;
    }

    /**
     * Represents a row in row layout.  In addition to components being added to the Container, they should be
     * added to the row as well (possibly with a constraint).
     */
    public static class Row {
        private int defaultHGap;
        private int verticalAlign;
        private int horizontalAlign;
        private boolean justify;

        private int height = -1;

        private Map constraints = new HashMap();
        private ArrayList components = new ArrayList();
        private int yOffset = 0;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Public API

        public Row(int defaultHGap, int horizontalAlign, int verticalAlign, boolean justify) {
            this.defaultHGap = defaultHGap;
            this.verticalAlign = verticalAlign;
            this.horizontalAlign = horizontalAlign;
            this.justify = justify;
        }

        /**
         * Adds a component using its preferred size a width constraint
         */
        public Component addComponent(Component component) {
            return addComponent(component, null);
        }

        /**
         * @param component
         * @param widthConstraint
         * @param index           allows you to insert a component in the middle of the row (-1 indicates end of row, as with Container.add)
         * @return the same component added - allows chaining. (eg. container.add(rowLayout.addComponent(c));
         */
        public Component addComponent(Component component, WidthConstraint widthConstraint, int index) {
            if (index < 0) {
                index = components.size();
            }
            components.add(index, component);

            if (widthConstraint == null) {
                if (EventQueue.isDispatchThread()) {
                    // getPreferredSize can acquire tree locks and we might not be on the EDT now.
                    widthConstraint = new FixedWidthConstraint(component.getPreferredSize().width);
                } else {
                    widthConstraint = new LazyFixedWidthConstraint(component);
                }
            }

            this.constraints.put(component, widthConstraint);

            return component;
        }

        /**
         * Adds a component using a given constraint
         *
         * @return returns the same component as it was sent to allow for anonymous declarations (like StringBuffer)
         */
        public Component addComponent(Component component, WidthConstraint widthConstraint) {
            return addComponent(component, widthConstraint, -1);
        }

        public boolean removeComponent(Component component) {
            boolean removed = this.components.remove(component);
            this.constraints.remove(component);
            return removed;
        }

        public int indexOf(Component component) {
            return this.components.indexOf(component);
        }

        public void recalcMaxHeight() {
            this.height = 0;
            for (Iterator iterator = components.iterator(); iterator.hasNext();) {
                Component component = (Component) iterator.next();
                height = Math.max(height, component.getPreferredSize().height);
            }
        }

        public int getComponentCount() {
            return components.size();
        }

        public Component getComponent(int index) {
            return (Component) components.get(index);
        }

        public WidthConstraint getConstraint(Component key) {
            return (WidthConstraint) constraints.get(key);
        }

        public void setConstraint(Component key, WidthConstraint constraint) {
            constraints.put(key, constraint);
        }

        public int getHeight() {
            if (height == -1) recalcMaxHeight();
            return height;
        }

        public boolean isJustify() {
            return justify;
        }

        public int getHorizontalAlign() {
            return horizontalAlign;
        }

        public int getDefaultHGap() {
            return defaultHGap;
        }

        public int getVerticalAlign() {
            return verticalAlign;
        }

        public int getWidth(int targetWidth) {
            int result = 0;

            // Add up all the component widths and spaces
            for (Iterator itar = components.iterator(); itar.hasNext();) {
                Component component = (Component) itar.next();
                WidthConstraint constraint = (WidthConstraint) constraints.get(component);
                result += constraint.getWidth(component, targetWidth, result) + defaultHGap;
            }
            return result - defaultHGap;
        }

        public int getWhitespaceWidth() {
            return defaultHGap * (components.size() - 1);
        }

        public void setYOffset(int y) {
            this.yOffset = y;
        }

        public int getYOffset() {
            return yOffset;
        }
    }


    /**
     * A simple constraint interface that deals only with constraining width.
     */
    public static interface WidthConstraint {
        public abstract int getWidth(Component component, int targetRowWidth, int currentRowWidth);
    }


    /**
     * Forces a component to be a specific width
     */
    public static class FixedWidthConstraint implements WidthConstraint {
        protected int width;

        public FixedWidthConstraint(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public int getWidth(Component component, int targetRowWidth, int currentRowWidth) {
            return width;
        }
    }


    /**
     * Forces a component to be a specific width
     */
    public static class LazyFixedWidthConstraint extends FixedWidthConstraint {
        private Component component;

        public LazyFixedWidthConstraint(Component component) {
            super(-1);
            this.component = component;
        }

        public int getWidth() {
            if (width == -1 && component != null) {
                width = component.getPreferredSize().width;
                component = null;
            }
            return width;
        }

        public int getWidth(Component component, int targetRowWidth, int currentRowWidth) {
            return getWidth();
        }
    }


    /**
     * Sets a component to follow a specific components preferred width
     */
    public static class PreferredWidthConstraint implements WidthConstraint {
        private Component modelComponent;

        public PreferredWidthConstraint(Component modelComponent) {
            this.modelComponent = modelComponent;
        }

        public Component getModelComponent() {
            return modelComponent;
        }

        public int getWidth(Component component, int targetRowWidth, int currentRowWidth) {
            return modelComponent.getPreferredSize().width;
        }
    }


    /**
     * Allows specifying a components width as a ratio (0.0 - 1.0) of its container
     */
    public static class RelativeWidthConstraint implements WidthConstraint {
        private double relativeWidthRatio;

        public RelativeWidthConstraint(double relativeWidthRatio) {
            this.relativeWidthRatio = relativeWidthRatio;
        }

        public double getRelativeWidthRatio() {
            return relativeWidthRatio;
        }

        public int getWidth(Component component, int targetRowWidth, int currentRowWidth) {
            return (int) ((double) targetRowWidth * relativeWidthRatio);
        }
    }


    /**
     * Component will use up all remaining width in a row.
     */
    public static class RemainingWidthConstraint implements WidthConstraint {
        private double remainingRatio;

        public RemainingWidthConstraint() {
            this(1d);
        }

        private RemainingWidthConstraint(double remainingRatio) {
            this.remainingRatio = remainingRatio;
        }

        public int getWidth(Component component, int targetRowWidth, int currentRowWidth) {
            return (int) (((double) targetRowWidth - (double) currentRowWidth) * remainingRatio);
        }
    }


    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    public void invalidateLayout(Container target) {
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void addLayoutComponent(Component comp, Object constraint) {
    }

    public void removeLayoutComponent(Component comp) {
        for (Iterator iterator = rows.iterator(); iterator.hasNext();) {
            Row row = (Row) iterator.next();
            if (row.removeComponent(comp)) return;
        }
    }

    public int getComponentIndex(Component comp) {
        int index = 0;
        for (Iterator iterator = rows.iterator(); iterator.hasNext();) {
            Row row = (Row) iterator.next();
            int rowIndex = row.indexOf(comp);
            if (rowIndex >= 0) return rowIndex + index;
            index += row.getComponentCount();
        }
        return -1;
    }

    public int getRowIndex(Row row) {
        return rows.indexOf(row);
    }

    public void removeRow(Row row) {
        rows.remove(row);
    }

    public void removeRow(int i) {
        rows.remove(i);
    }

    public Row getRow(Component component) {
        for (int i = 0; i < getRowCount(); i++) {
            Row row = getRow(i);
            if (row.indexOf(component) >= 0) return row;
        }
        return null;
    }

    public int getComponentCount() {
        int count = 0;
        for (Iterator iterator = rows.iterator(); iterator.hasNext();) {
            Row row = (Row) iterator.next();
            count += row.getComponentCount();
        }
        return count;
    }
}
