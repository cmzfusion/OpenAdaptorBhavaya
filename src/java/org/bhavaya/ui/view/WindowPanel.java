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

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * @author Daniel van Enckevort
 * @version $Revision: 1.8 $
 */
public class WindowPanel extends JPanel {
    private JToolBar oldBar;

    public WindowPanel(Component component) {
        super(new BorderLayout());
        add(component, BorderLayout.CENTER);
        setMinimumSize(new Dimension(100, 100));
    }

    public JToolBar getJToolBar() {
        return oldBar;
    }

    public void setJToolBar(JToolBar toolBar) {
        if (oldBar != null) {
            remove(oldBar);
        }
        if (toolBar != null) {
            add(toolBar, BorderLayout.NORTH);
        }
        oldBar = toolBar;
        revalidate();
    }

    public Component getComponent() {
        return getComponent(0);
    }
}
