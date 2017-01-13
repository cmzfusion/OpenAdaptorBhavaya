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


import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */

public class MenuGroup {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    public static final int TOP = 0;
    public static final int BOTTOM = 1;

    /**
     * Takes a JMenuBar and populates it with menuGroups
     * @param alignment whether to process groups by adding to the left (like "File") or adding to the right ("Help)
     */
    public static void processMenuGroups(JMenuBar jMenuBar, MenuGroup[] menuGroups, int alignment) {
        for (int i = 0; i < menuGroups.length; i++) {
            if (menuGroups[i].getHorizontalLayout() == alignment) {
                appendMenuGroup(jMenuBar, menuGroups[i]);
            }
        }
    }

    private static void appendMenuGroup(JMenuBar jMenuBar, MenuGroup menuGroup) {
        JMenu menu = getContainedJMenu(jMenuBar, menuGroup.getMenuBarText());

        // Will be non-null if there a menugroup of a certain name already exists.
        // If not we have to create a new one and add it to the JMenuBar
        if (menu == null) {
            menu = new JMenu(menuGroup.getMenuBarText());
            menu.setMnemonic(menuGroup.getMnemonic());
            jMenuBar.add(menu);
        } else {
            // This is done to force an ordering precedence.  The menu is effectively built from the middle outwards.
            // All menus are either added to the left or right of the existing menus.  Doing this imposes a reordering rule
            // on menus.  ie. The File menu will always be most left and the Help menu will always be the most right.
            jMenuBar.remove(menu);
            jMenuBar.add(menu, menuGroup.getHorizontalLayout() == MenuGroup.LEFT ? 0 : -1);
        }

        Element[] elements = menuGroup.getElements();
        for (int i = 0; i < elements.length; i++) {
            Element element = elements[i];
            if (menuGroup.getVerticalLayout() == TOP) {
                element.applyInsert(menu);
            } else {
                element.applyAdd(menu);
            }
        }
    }

    private static JMenu getContainedJMenu(JMenuBar jMenuBar, String menuText) {
        for (int i = 0; i < jMenuBar.getMenuCount(); i++) {
            if (jMenuBar.getMenu(i).getText().equals(menuText)) {
                return jMenuBar.getMenu(i);
            }
        }
        return null;
    }

    public interface Element {
        public void applyAdd(JMenu jMenu);

        public void applyInsert(JMenu jMenu);
    }

    public static class SeparatorElement implements Element {
        public void applyAdd(JMenu jMenu) {
            jMenu.addSeparator();
        }

        public void applyInsert(JMenu jMenu) {
            jMenu.insertSeparator(0);
        }
    }

    public static class MenuElement implements Element {
        private JMenu jMenu;

        public MenuElement(JMenu jMenu) {
            this.jMenu = jMenu;
        }

        public void applyAdd(JMenu jMenu) {
            jMenu.add(this.jMenu);
        }

        public void applyInsert(JMenu jMenu) {
            jMenu.insert(jMenu, 0);
        }
    }

    public static class MenuItemElement implements Element {
        private JMenuItem jMenuItem;

        public MenuItemElement(JMenuItem jMenuItem) {
            this.jMenuItem = jMenuItem;
        }

        public void applyAdd(JMenu jMenu) {
            jMenu.add(jMenuItem);
        }

        public void applyInsert(JMenu jMenu) {
            jMenu.add(jMenuItem, 0);
        }
    }

    private String menuBarText;
    private int mnemonic;
    private int horizontalLayout = RIGHT;
    private int verticalLayout = BOTTOM;

    private List elementList = new ArrayList();

    public MenuGroup(String menuBarText, int mnemonic) {
        this.menuBarText = menuBarText;
        this.mnemonic = mnemonic;
    }

    public int getMnemonic() {
        return mnemonic;
    }

    public void setHorizontalLayout(int horizontalLayout) {
        this.horizontalLayout = horizontalLayout;
    }

    public void setVerticalLayout(int verticalLayout) {
        this.verticalLayout = verticalLayout;
    }

    public void addElement(Element element) {
        elementList.add(element);
    }

    public Element[] getElements() {
        return (Element[]) elementList.toArray(new Element[elementList.size()]);
    }

    public String getMenuBarText() {
        return menuBarText;
    }

    public int getHorizontalLayout() {
        return horizontalLayout;
    }

    public int getVerticalLayout() {
        return verticalLayout;
    }
}
