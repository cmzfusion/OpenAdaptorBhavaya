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

import org.bhavaya.util.SimpleObjectFormat;

import javax.swing.*;
import java.awt.*;


/**
 * A JLabel with a built-in SimpleObjectFormat.  The component has the concept of an active object which is passed
 * through the ObjectFormat to create the JLabel text.
 *
 * @author
 * @version $Revision: 1.3 $
 */
public class ObjectRenderLabel extends JLabel {
    private static final String DEFAULT_NULL_STRING = "[none]";

    private SimpleObjectFormat messageString;
    private boolean antiAliased = false;

    public ObjectRenderLabel(String renderPattern) {
        this(renderPattern, DEFAULT_NULL_STRING);
    }

    public ObjectRenderLabel(String renderPattern, String nullString) {
        this.messageString = new SimpleObjectFormat(renderPattern, nullString, true);
        setRenderObject(null);
    }

    public void setRenderObject(Object dataBean) {
        setText(messageString.formatObject(dataBean));
    }

    public boolean isAntiAliased() {
        return antiAliased;
    }

    public void setAntiAliased(boolean antiAliased) {
        this.antiAliased = antiAliased;
        repaint();
    }

    protected void paintComponent(Graphics g) {
        if (antiAliased) {
            Graphics2D graphics2D = (Graphics2D) g;
            Object oldHint = graphics2D.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paintComponent(g);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldHint);
        } else {
            super.paintComponent(g);
        }
    }
}
