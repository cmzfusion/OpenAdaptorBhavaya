package org.bhavaya.ui.view.composite;

import org.bhavaya.ui.view.View;

import java.awt.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 13-May-2008
 * Time: 14:34:08
 */
public abstract class AbstractSingleRowOrColumnLayout implements CompositeViewLayoutStrategy {

    public void configureContainer(Container c) {
        c.setLayout(new GridBagLayout());
    }

    public void addComponents(Container c, View[] existingViews, View... viewsToAdd) {
        for (View childView : viewsToAdd) {
            addChildView(c, childView);
        }
    }

    public void removeComponents(Container c, View[] existingViews, View... viewsToRemove) {
        Set<View> toRemove = new HashSet<View>();
        toRemove.addAll(Arrays.asList(viewsToRemove));

        c.removeAll();
        for (View view : existingViews) {
            if (!toRemove.contains(view)) {
                addChildView(c, view);
            }
        }
    }

    private void addChildView(Container c, View childView) {
        if ( childView.isDisplayable() ) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1;
            configureGridBagConstraints(gbc);
            c.add(childView.getComponent(), gbc);
        }
    }

    protected abstract void configureGridBagConstraints(GridBagConstraints gbc);

    public void dispose(Container c) {}
}
