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

import org.bhavaya.ui.dataset.BeanCollectionSelector;
import org.bhavaya.ui.view.DefaultApplicationView;
import org.bhavaya.ui.view.TableViewSelector;
import org.bhavaya.ui.view.View;
import org.bhavaya.ui.view.Workspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;

/**
 *
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.10 $
 */
public class DefaultApplicationContext extends ApplicationContext {
    private View applicationView;

    public DefaultApplicationContext() throws Exception {
    }

    public View getApplicationView() {
        if (applicationView == null) {
            applicationView = new ApplicationView();
        }
        return applicationView;
    }

    private class ApplicationView extends DefaultApplicationView {
        private Container viewComponent;

        public MenuGroup[] createMenuGroups(GenericWindow window) {
            MenuGroup fileMenuGroup = new MenuGroup("File", KeyEvent.VK_F);
            fileMenuGroup.setHorizontalLayout(MenuGroup.LEFT);
            fileMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ApplicationContext.SaveAction(false))));
            fileMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new DeleteConfigurationAndExitAction())));
            fileMenuGroup.addElement(new MenuGroup.SeparatorElement());
            fileMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new BeanCollectionSelector.ImportAction())));
            fileMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new TableViewSelector.ImportAction())));
            fileMenuGroup.addElement(new MenuGroup.SeparatorElement());
            fileMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ApplicationContext.ExitWithoutSavingAction())));
            fileMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new ApplicationContext.ExitAction())));

            MenuGroup helpMenuGroup = new MenuGroup("Help", KeyEvent.VK_H);
            helpMenuGroup.setHorizontalLayout(MenuGroup.RIGHT);
            helpMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new AboutAction())));
            helpMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new DiagnosticsAction())));

            return new MenuGroup[]{fileMenuGroup, Workspace.getInstance().createWindowMenuGroup(window), helpMenuGroup};
        }

        public Component getComponent() {
            if (viewComponent == null) {
                SplitPanel leftSplitPanel = new SplitPanel(Workspace.getInstance().getTabbedPane(), SplitPanel.LEFT);
                SplitControlPanel leftControlPanel = new SplitControlPanel(SplitPanel.LEFT, leftSplitPanel, leftSplitPanel);
                viewComponent = leftControlPanel;

                MenuPanel collectionsMenuPanel = new MenuPanel("Collections", new BeanCollectionSelector(), SplitPanel.LEFT, false);
                collectionsMenuPanel.setSplitterOffset(200);
                leftControlPanel.addMenuPanel(collectionsMenuPanel);
            }

            return viewComponent;
        }

        public ToolBarGroup createToolBarGroup() {
            ToolBarGroup toolBarGroup = new ToolBarGroup("DefaultApplication");

            toolBarGroup.addElement(new ToolBarGroup.ActionElement(new SaveAction(true)));

            return toolBarGroup;
        }

        public void exportAction(WeakReference<Action> action) {
            //not implemented. Do not want alias buttons added to the main ebond screen
        }

        public void unexportAction(WeakReference<Action> action) {
            //not implemented. Do not want alias buttons added to the main ebond screen
        }

        public java.util.List<WeakReference<Action>> getExportedActions() {
            return null;  //not implemented. Do not want alias buttons added to the main ebond screen
        }

        public long getViewId() {
            return 0;  //not implemented. Do not want alias buttons added to the main ebond screen
        }



    }
}

