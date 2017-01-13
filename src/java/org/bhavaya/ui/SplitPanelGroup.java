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


import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Similar to a ButtonGroup in that this class ensures that only one SplitPanel with a group is actually displaying
 * anything at a time (like RadioButtons).  This class had not been tested, or even used so use with the same caution
 * a man wearing wax wings has before jumping off the eiffel tower.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */

public class SplitPanelGroup {
    private List splitPanelList = new ArrayList();
    private ChangeHandler changeHandler = new ChangeHandler();
    private SplitPanel activeSplitPanel;
    private SplitPanel repressedSplitPanel;
    private MenuPanel repressedMenuPanel;

    public SplitPanelGroup() {
    }

    public void add(SplitPanel splitPanel) {
        splitPanelList.add(splitPanel);
        splitPanel.addChangeListener(changeHandler);
    }

    private class ChangeHandler implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            SplitPanel eventSplitPanel = (SplitPanel) e.getSource();

            // If a panel is now visible, then.
            if (eventSplitPanel.getVisibleMenuPanel() != null) {

                // If there is already a panel showing.
                if (activeSplitPanel != null) {

                    // Save its state as a repressed panel
                    repressedSplitPanel = activeSplitPanel;
                    repressedMenuPanel = activeSplitPanel.getVisibleMenuPanel();

                    // Hide it and swap
                    activeSplitPanel.setVisibleMenuPanel(null);
                    activeSplitPanel = eventSplitPanel;
                }
            }
            // Else, a panel is now hiding itself
            else {
                // Was there a repressed panel
                if (repressedSplitPanel != null) {

                    // Restore its state
                    repressedSplitPanel.setVisibleMenuPanel(repressedMenuPanel);
                    repressedSplitPanel = null;
                    activeSplitPanel = repressedSplitPanel;
                } else {
                    activeSplitPanel = null;
                }
            }
        }
    }
}
