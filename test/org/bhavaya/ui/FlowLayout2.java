package org.bhavaya.ui;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: deana
 * Date: 18-Jun-2004
 * Time: 12:52:22
 * To change this template use File | Settings | File Templates.
 */
public class FlowLayout2 extends FlowLayout {
    private boolean doneLayout = false;

    public FlowLayout2() {
        super(FlowLayout.LEFT);
    }

    public void layoutContainer(Container target) {
        super.layoutContainer(target);
        doneLayout = true;
    }

    public Dimension preferredLayoutSize(Container target) {
        if (!doneLayout) {
            return super.preferredLayoutSize(target);
        } else {
            synchronized (target.getTreeLock()) {
                int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE;
                Dimension dim = new Dimension(0, 0);
                int count = target.getComponentCount();

                for (int i = 0; i < count; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Rectangle bounds = m.getBounds();
                        x = Math.min(x, bounds.x);
                        y = Math.min(y, bounds.y);
                        dim.width = Math.max(dim.width, bounds.x + bounds.width);
                        dim.height = Math.max(dim.height, bounds.y + bounds.height);
                    }
                }
                // The width and height are currently assuming the top left component is at 0,0
                // correct this assumption
                dim.width -= x;
                dim.height -= y;

                Insets insets = target.getInsets();
                dim.width += insets.left + insets.right;
                dim.height += insets.top + insets.bottom;
                return dim;
            }
        }
    }
}
