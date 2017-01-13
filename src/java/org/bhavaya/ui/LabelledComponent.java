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
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class LabelledComponent extends JPanel {
    public static final String TOP = BorderLayout.NORTH;
    public static final String BOTTOM = BorderLayout.SOUTH;
    public static final String LEFT = BorderLayout.WEST;
    public static final String RIGHT = BorderLayout.EAST;
    private JLabel label;
    private Component formElement;


    public LabelledComponent(String label, Component formElement) {
        this(new JLabel(label), formElement, 1);
    }

    public LabelledComponent(String label, Component formElement, int relativeFontSize) {
        this(new JLabel(label), formElement, relativeFontSize);
    }

    public LabelledComponent(JLabel label, Component formElement) {
        this(label, formElement, 1);
    }

    public LabelledComponent(JLabel label, Component formElement, int relativeFontSize) {
        this(label, formElement, relativeFontSize, TOP);
    }

    public LabelledComponent(JLabel label, Component formElement, int relativeFontSize, String labelPosition) {
        super(new BorderLayout(4, 0));

        this.formElement = formElement;
        this.label = label;
        setRelativeFontSize(relativeFontSize);

        LayoutManager layout;
        if (labelPosition == LEFT || labelPosition == RIGHT) {
            layout = new BorderLayout();
        } else {
            layout = new FlowLayout(FlowLayout.LEFT, 0, 0);
        }
        JPanel labelPanel = new JPanel(layout);
        labelPanel.add(label);
        labelPanel.setOpaque(false);
        label.setOpaque(false);
        this.setOpaque(false);

        add(labelPanel, labelPosition != null ? labelPosition : TOP);
        add(formElement, BorderLayout.CENTER);
        setFocusable(false);
    }

    public void setRelativeFontSize(int relativePointSize) {
        final Font sampleFont = new JLabel("TEST").getFont();
        label.setFont(label.getFont().deriveFont(sampleFont.getSize() + (float) relativePointSize));
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        label.setEnabled(enabled);
        formElement.setEnabled(enabled);
    }
}
