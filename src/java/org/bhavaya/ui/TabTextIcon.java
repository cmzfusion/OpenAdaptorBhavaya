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
import java.awt.font.*;
import java.awt.geom.AffineTransform;

/**
 * Description
 *
 * @author
 * @version $Revision: 1.4 $
 */
class TabTextIcon implements Icon {
    static final int NONE = 0;
    static final int CW = 1;
    static final int CCW = 2;

    private static final int XPAD = 2;

    private int rotate;
    private Font font;
    private GlyphVector glyphs;
    private float width;
    private float height;
    private float ascent;
    private RenderingHints renderHints;
    private Rectangle mnemonicBounds;

    public TabTextIcon(String text, int rotate) {
        this(text, KeyEvent.VK_UNDEFINED, rotate);
    }

    public TabTextIcon(String text, int mnemonic, int rotate) {
        this.rotate = rotate;
        this.font = new Font("Arial", Font.PLAIN, 12);

        FontRenderContext fontRenderContext = new FontRenderContext(null, true, true);
        glyphs = font.createGlyphVector(fontRenderContext, text);
        width = (int) glyphs.getLogicalBounds().getWidth() + XPAD * 2;

        LineMetrics lineMetrics = font.getLineMetrics(text, fontRenderContext);
        ascent = lineMetrics.getAscent();
        height = (int) lineMetrics.getHeight();

        if (mnemonic != KeyEvent.VK_UNDEFINED) {
            int mnemonicIndex = text.toUpperCase().indexOf(mnemonic);
            if (mnemonicIndex != -1) {
                mnemonicBounds = glyphs.getGlyphVisualBounds(mnemonicIndex).getBounds();
            }
        }

        renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public int getIconWidth() {
        return (int) (rotate == TabTextIcon.CW || rotate == TabTextIcon.CCW ? height : width);
    }

    public int getIconHeight() {
        return (int) (rotate == TabTextIcon.CW || rotate == TabTextIcon.CCW ? width : height);
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform oldTransform = g2d.getTransform();
        RenderingHints oldHints = g2d.getRenderingHints();

        g2d.setFont(font);
        g2d.setRenderingHints(renderHints);
        g2d.setColor(c.getForeground());

        if (rotate == TabTextIcon.NONE) {
            x += XPAD;
            y += ascent;
            g2d.drawGlyphVector(glyphs, x, y);
            if (mnemonicBounds != null) {
                x += mnemonicBounds.x;
                y += 1;
                g2d.drawLine(x, y, x + mnemonicBounds.width , y);
            }
        } else if (rotate == TabTextIcon.CW) {
            AffineTransform trans = new AffineTransform();
            trans.concatenate(oldTransform);
            trans.translate(x, y + XPAD);
            trans.rotate(Math.PI / 2, height / 2, width / 2);
            g2d.setTransform(trans);
            float x2 = (height - width) / 2;
            float y2 = (width - height) / 2 + ascent;
            g2d.drawGlyphVector(glyphs, x2, y2);
            if (mnemonicBounds != null) {
                x2 += mnemonicBounds.x;
                y2 += 2;
                g2d.drawLine((int)x2, (int)y2, (int)x2 + mnemonicBounds.width , (int)y2);
            }
        } else if (rotate == TabTextIcon.CCW) {
            AffineTransform trans = new AffineTransform();
            trans.concatenate(oldTransform);
            trans.translate(x, y - XPAD);
            trans.rotate(Math.PI * 3 / 2, height / 2, width / 2);
            g2d.setTransform(trans);
            float x2 = (height - width) / 2;
            float y2 = (width - height) / 2 + ascent;
            g2d.drawGlyphVector(glyphs, x2, y2);
            if (mnemonicBounds != null) {
                x2 += mnemonicBounds.x;
                y2 += 1;
                g2d.drawLine((int)x2, (int)y2, (int)x2 + mnemonicBounds.width , (int)y2);
            }
        }

        g2d.setTransform(oldTransform);
        g2d.setRenderingHints(oldHints);
    }
}
