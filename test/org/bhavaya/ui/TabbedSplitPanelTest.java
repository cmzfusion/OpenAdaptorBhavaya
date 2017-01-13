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

import org.bhavaya.ui.MenuPanel;
import org.bhavaya.ui.SplitControlPanel;
import org.bhavaya.ui.SplitPanel;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * A test bed for the SplitPanel and its controllers
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */

public class TabbedSplitPanelTest extends JPanel {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Fred");

        SplitPanel rightMenu = new SplitPanel(new JTextArea("Right text area"), SplitPanel.BOTTOM);
        SplitControlPanel rightControlPanel = new SplitControlPanel(SplitPanel.BOTTOM, rightMenu, rightMenu);
        MenuPanel bottomMenuPanel = new MenuPanel("Bottom Menu", new JList(new String[]{"Bottom List"}), SplitPanel.BOTTOM, true, KeyEvent.VK_6, true);
        rightControlPanel.addMenuPanel(bottomMenuPanel);

        SplitPanel centreSplitPanel = new SplitPanel(new JTextArea("Centre text area"), SplitPanel.RIGHT);
        SplitControlPanel centreControlPanel = new SplitControlPanel(SplitPanel.RIGHT, centreSplitPanel, centreSplitPanel);
        MenuPanel rightMenuPanel = new MenuPanel("Right Menu", rightControlPanel, SplitPanel.BOTTOM, true, KeyEvent.VK_M, true);
        centreControlPanel.addMenuPanel(rightMenuPanel);

        SplitPanel bottomSplitPanel = new SplitPanel(centreControlPanel, SplitPanel.BOTTOM);
        SplitControlPanel bottomControlPanel = new SplitControlPanel(SplitPanel.BOTTOM, bottomSplitPanel, bottomSplitPanel);
        MenuPanel mainBottomMenuPanel = new MenuPanel("My test", new JTextArea("Hello"), SplitPanel.BOTTOM, true, KeyEvent.VK_2, false);
        bottomControlPanel.addMenuPanel(mainBottomMenuPanel);

        SplitPanel leftSplitPanel = new SplitPanel(bottomControlPanel, SplitPanel.LEFT);
        SplitControlPanel leftControlPanel = new SplitControlPanel(SplitPanel.LEFT, leftSplitPanel, leftSplitPanel);
        MenuPanel leftPanelOne = new MenuPanel("Left 1", new JList(new String[]{"one", "1"}), SplitPanel.LEFT, true, KeyEvent.VK_0, true);
        MenuPanel leftPanelTwo = new MenuPanel("Left 2", new JList(new String[]{"two", "2"}), SplitPanel.LEFT, true, KeyEvent.VK_3, false);
        leftControlPanel.addMenuPanel(leftPanelOne);
        leftControlPanel.addMenuPanel(leftPanelTwo);

        frame.setContentPane(leftControlPanel);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}