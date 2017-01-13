package org.bhavaya.ui;

/**
 * An alternate action to be be supplied to a MenuTabButton from the MenuTabButtonFactory.
 * This action will be performed when the button is pressed and the methods return value will determine whether to
 * continue
 * User: ga2mop0
 * Date: 03/09/13
 * Time: 15:34
 */
public interface MenuTabButtonAlternateAction {
    /**
     * Perform the alternate action
     * @return True if the processing should continue
     */
    boolean performAlternateAction();
}
