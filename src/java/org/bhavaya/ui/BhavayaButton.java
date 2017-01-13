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

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */

public class BhavayaButton extends JButton {
    private static final Color BORDER_COLOR = new Color(0, 0, 0);
    private static final Color BACKGROUND_ROLLOVER_COLOR = new Color(120, 139, 174);
    private static final Color BACKGROUND_PRESSED_COLOR = new Color(166, 180, 206);

    private Color normalColor;
    private Icon icon;

    public BhavayaButton(Action a, Icon icon) {
        super(a);
        this.icon = icon;
        setFocusable(false);
        setBorder(null);
        setRolloverEnabled(true);
        setOpaque(false);
        normalColor = getBackground();
        setIcon(new EnhanceIcon(normalColor, normalColor, 0));
        setRolloverIcon(new EnhanceIcon(BORDER_COLOR, BACKGROUND_ROLLOVER_COLOR, 0));
        setPressedIcon(new EnhanceIcon(BORDER_COLOR, BACKGROUND_PRESSED_COLOR, 1));
    }

    private class EnhanceIcon implements Icon {
        private Color borderColor;
        private Color backgroundColor;
        private int offset;

        public EnhanceIcon(Color borderColor, Color backgroundColor, int offset) {
            this.borderColor = borderColor;
            this.backgroundColor = backgroundColor;
            this.offset = offset;
        }

        public int getIconWidth() {
            return icon == null ? 0 : icon.getIconWidth();
        }

        public int getIconHeight() {
            return icon == null ? 0 : icon.getIconHeight();
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (icon != null) {
                g.setColor(backgroundColor);
                g.fillRect(0, 0, c.getWidth() - 1, c.getHeight() - 1);

                g.setColor(borderColor);
                g.drawRect(0, 0, c.getWidth() - 1, c.getHeight() - 1);

                icon.paintIcon(c, g, x + offset, y + offset);
            }
        }
    }
}
