package org.bhavaya.ui.view.composite;

import org.bhavaya.ui.view.View;

import javax.swing.*;
import java.awt.*;

/**
 * A layout strategy responsible for arranging the child views within the composite view
 */
public interface CompositeViewLayoutStrategy {

    /**
     * This is called once when a layout strategy is set on the composite view,
     * before any components are added
     */
    void configureContainer(Container mainContainer);

    /**
     * @param mainContainer
     * @param existingViews - current children before the add
     * @param viewsToAdd    - new views
     */
    void addComponents(Container mainContainer, View[] existingViews, View... viewsToAdd);

    /**
     * @param mainContainer
     * @param existingViews - current children before the remove
     * @param viewsToRemove - views to remove
     */
    void removeComponents(Container mainContainer, View[] existingViews, View... viewsToRemove);


    /**
     * Called when the container changes layout strategy, to give the current layout a chance to
     * clean up.
     */
    void dispose(Container mainContainer);


    /**
     * @return a description of the layout
     */
    String getDescription();

}
