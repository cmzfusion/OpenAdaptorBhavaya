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
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Description
 *
 * @author
 * @version $Revision: 1.6.4.3 $
 */
public class MenuPanel extends JPanel {

    private String name;
    private int mnemonic;
    private boolean showMnemonic;
    private Component component;
    private int splitterOffset;
    private FlashingTabButton flashingTabButton;
    private boolean resizeable = true;

    public MenuPanel(String name, Component component, int orientation, boolean autoTitle, int mnemonic, boolean showMnemonic) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.name = name;
        this.component = component;
        this.mnemonic = mnemonic;
        this.showMnemonic = showMnemonic;

        if (autoTitle) {
            TitlePanel title = new TitlePanel(name);
            add(title);
        }
        add(component);

        this.splitterOffset = (orientation == SplitPanel.LEFT || orientation == SplitPanel.RIGHT) ? getPreferredSize().width : getPreferredSize().height;
    }

    public MenuPanel(String name, Component component, int orientation, boolean autoTitle, int mnemonic) {
        this(name, component, orientation, autoTitle, mnemonic, true);
    }

    public MenuPanel(String name, Component component, int orientation, boolean autoTitle) {
        this(name, component, orientation, autoTitle, KeyEvent.VK_UNDEFINED, false);
    }

    public MenuPanel(String name, Component component, boolean autoTitle) {
        this(name, component, SplitPanel.LEFT, autoTitle);
    }

    public String getName() {
        return name;
    }

    public Component getComponent() {
        return component;
    }

    public int getSplitterOffset() {
        return splitterOffset;
    }

    public void setSplitterOffset(int splitterOffset) {
        this.splitterOffset = splitterOffset;
    }

    public boolean isResizeable() {
        return resizeable;
    }

    public void setResizeable(boolean resizeable) {
        this.resizeable = resizeable;
    }

    public int getMnemonic() {
        return mnemonic;
    }

    public void setMnemonic(int mnemonic) {
        this.mnemonic = mnemonic;
    }

    public String getDisplayName() {
        if (showMnemonic && mnemonic != KeyEvent.VK_UNDEFINED && name.toUpperCase().indexOf(mnemonic) == -1) {
            return (char)mnemonic + ": " + name;
        } else {
            return name;
        }
    }

    public synchronized void setFlashingTabButton(FlashingTabButton flashingTabButton) {
        this.flashingTabButton = flashingTabButton;
    }

    public synchronized void startPanelButtonFlashing(Color color) {
        if(flashingTabButton != null) {
            flashingTabButton.startFlashing(color);
        }
    }

    public synchronized void stopPanelButtonFlashing() {
        if(flashingTabButton != null) {
            flashingTabButton.stopFlashing();
        }
    }

    public synchronized FlashingTabButton getFlashingTabButton() {
        return flashingTabButton;
    }
}
