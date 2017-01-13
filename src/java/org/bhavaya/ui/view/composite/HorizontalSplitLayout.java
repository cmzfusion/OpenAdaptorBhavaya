package org.bhavaya.ui.view.composite;

import org.bhavaya.ui.view.View;

import javax.swing.*;
import java.awt.*;

/**
 * Lay out child views in a left to right split pane
 * It is only possible to add two child components to a composite view using this layout strategy
 */
public class HorizontalSplitLayout implements CompositeViewLayoutStrategy {

    private JSplitPane splitPane = new JSplitPane();

    public void configureContainer(Container c) {
        c.setLayout(new BorderLayout());
        c.add(splitPane, BorderLayout.CENTER);
    }

    public void addComponents(Container c, View[] existingViews, View... viewsToAdd) {
        for (View childView : viewsToAdd) {
            addChildView(c, childView);
        }
    }

    public void removeComponents(Container c, View[] existingViews, View... viewsToRemove) {
        for (View view : viewsToRemove) {
            splitPane.remove(view.getComponent());
        }
    }

    public void dispose(Container c) {}

    private void addChildView(Container c, View childView) {
        if ( childView.isDisplayable()) {
            if (splitPane.getLeftComponent().getClass() == JButton.class) {  //JButton is the default component in the split pane
                splitPane.setLeftComponent(childView.getComponent());
            } else if (splitPane.getRightComponent().getClass() == JButton.class) {
                splitPane.setRightComponent(childView.getComponent());
            } else {
                throw new UnsupportedOperationException("Already 2 components added to composite view with SplitPaneLeftRightLayout");
            }
        }
    }

    public String getDescription() {
        return "Horizontal Split";
    }
    
}
