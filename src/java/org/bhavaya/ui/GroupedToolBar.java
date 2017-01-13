package org.bhavaya.ui;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: ebbuttn
 * Date: 13-Feb-2008
 * Time: 13:51:38
 * To change this template use File | Settings | File Templates.
 *
 * A JToolBar with utility methods for ToolBarGroups
 */
public class GroupedToolBar extends JToolBar {

    public GroupedToolBar() {
        setFloatable(false);
    }

    public void addToolBarGroups(ToolBarGroup... toolBarGroups) {
        for ( ToolBarGroup toolBarGroup : toolBarGroups ) {
            addToolBarGroup(toolBarGroup);
        }
    }

    public void addToolBarGroup(ToolBarGroup toolBarGroup) {
        ToolBarGroup.Element[] toolBarGroupElements = toolBarGroup.getElements();

        if (getComponentCount() > 0 &&
                !(getComponent(getComponentCount() - 1) instanceof Separator)
                && toolBarGroupElements.length > 0 && !(toolBarGroupElements[0] instanceof ToolBarGroup.SeperatorElement)) {
            addSeparator();
        }

        for (ToolBarGroup.Element element : toolBarGroupElements) {
            element.applyAdd(this);
        }
    }
}
