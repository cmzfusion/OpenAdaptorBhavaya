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

package org.bhavaya.ui.view;

import org.bhavaya.ui.AcceleratorAction;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.ToolBarGroup;
import org.bhavaya.util.SetStatement;

import javax.swing.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */
public interface ViewContext {
    public void dispose();

    public MenuGroup[] createMenuGroups();

    public ToolBarGroup createToolBarGroup();

    /**
     * Return a group of actions used to generate the popup menu on the right click in table view.
     */
    public ActionGroup getActionsForBeanArray(Object[] beanArray);

    /**
     * Return default action on the bean to be executed on a double click in a table view.
     */
    public Action getActionForBeanPath(Object bean, String beanPath);

    /**
     * Return an array of actions to be registered with the keyboard listener.
     */
    public AcceleratorAction[] getAcceleratorActions();

    /**
     * @return SetStatements to perform an abstract write operation on the selected beans' properties.  For instance, could
     * send a message to a server.
     */
    public SetStatement[] getSetStatements();


    public ImageIcon getImageIcon();

    public JToolBar getToolbar();
}
