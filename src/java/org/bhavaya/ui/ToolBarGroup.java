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

import org.bhavaya.ui.view.Workspace;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.8 $
 */

public class ToolBarGroup {

    private String id;

    private static int toolBarSize = 24;

    private static ImageIcon resizeIcon(ImageIcon icon) {
        if(icon.getIconHeight() == toolBarSize) {
            return icon;
        }
        int width = icon.getIconWidth() * toolBarSize / icon.getIconHeight();
        Image scaled = icon.getImage().getScaledInstance(width, toolBarSize, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static void resizeIcon(Action action) {
        ImageIcon icon = (ImageIcon)action.getValue(Action.SMALL_ICON);
        if(icon != null) {
            action.putValue(Action.SMALL_ICON, resizeIcon(icon));
        }
    }

    private static void resizeIcon(AbstractButton button) {
        ImageIcon icon = (ImageIcon) button.getIcon();
        if(icon != null) {
            button.setIcon(resizeIcon(icon));
        }
    }

    public interface Element {
        public void applyAdd(JToolBar jToolBar);
        public void resize();
    }

    public static class SeperatorElement implements Element {
        public void applyAdd(JToolBar jToolBar) {
            jToolBar.addSeparator();
        }

        public void resize() {}
    }

    public static class ComponentElement implements Element {
        private Component component;

        public ComponentElement(Component component) {
            this.component = component;
            this.component.setFocusable(false);
        }

        public void resize() {
            recurseComponents(component);
        }

        private void recurseComponents(Component component) {
            if(component instanceof AbstractButton) {
                resizeIcon((AbstractButton)component);
            } else if (component instanceof TrafficLight) {
                ((TrafficLight)component).refresh();
            } else if (component instanceof JComponent) {
                for(Component c : ((JComponent)component).getComponents()) {
                    recurseComponents(c);
                }
            }
        }

        public void applyAdd(JToolBar jToolBar) {
            jToolBar.add(component);
        }
    }

    public static class ActionElement implements Element {
        private Action action;

        public ActionElement(Action action) {
            this.action = action;
        }

        public void resize() {
            resizeIcon(action);
        }

        public void applyAdd(JToolBar jToolBar) {
            jToolBar.add(action);
        }
    }

    public static class ToolBarGroupElement implements Element {
        private ToolBarGroup toolBarGroup;

        public ToolBarGroupElement(ToolBarGroup toolBarGroup) {
            this.toolBarGroup = toolBarGroup;
        }

        public void resize() {
            for (Element element : toolBarGroup.getElements()) {
                element.resize();
            }
        }

        public void applyAdd(JToolBar jToolBar) {
            for (Element element : toolBarGroup.getElements()) {
                element.applyAdd(jToolBar);
            }
        }
    }

    private java.util.List<Element> elementList = new ArrayList<Element>();

    public ToolBarGroup(String id) {
        this.id = id;
    }

    public void addElement(Element element) {
        element.resize();
        this.elementList.add(element);
    }

    public Element[] getElements() {
        return elementList.toArray(new Element[elementList.size()]);
    }

    public void clearElements() {
        this.elementList.clear();
    }

    public int getSize() {
        return elementList.size();
    }

    public boolean isEmpty() {
        if(elementList.isEmpty()) {
            return true;
        }
        for(Element element : elementList) {
            if(element instanceof ToolBarGroupElement) {
                ToolBarGroupElement tbge = (ToolBarGroupElement)element;
                if(tbge.toolBarGroup.isEmpty()) {
                    continue;
                }
            }
            //non empty element
            return false;
        }
        //All are group elements that are empty
        return true;
    }

    public String getId() {
        return id;
    }

    public ToolBarGroup[] splitOnSeparators() {
        List<ToolBarGroup> newGroups = new ArrayList<ToolBarGroup>();
        ToolBarGroup currentGroup = new ToolBarGroup(getId());
        int subId = 1;
        for(Element element : elementList) {
            if(element instanceof SeperatorElement) {
                if(!currentGroup.isEmpty()) {
                    newGroups.add(currentGroup);
                    currentGroup = new ToolBarGroup(getId() +"_"+subId++);
                }
            } else {
                currentGroup.addElement(element);
            }
        }
        if(!currentGroup.isEmpty()) {
            newGroups.add(currentGroup);
        }
        return newGroups.toArray(new ToolBarGroup[newGroups.size()]);
    }

    public static int getToolBarSize() {
        return toolBarSize;
    }

    public static void setToolBarSize(int toolBarSize) {
        ToolBarGroup.toolBarSize = toolBarSize;
        Workspace.getInstance().forceUpdate();
    }
}
