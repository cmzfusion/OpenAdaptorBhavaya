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

import org.bhavaya.collection.BeanCollection;

import javax.swing.*;
import java.awt.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class LabelView extends AbstractView {
    private Box component;
    private String labelText;
    private ImageIcon icon;

    public LabelView(String name, String tabTitle, String frameTitle, String labelText, ImageIcon icon) {
        super(name, tabTitle, frameTitle);
        this.labelText = labelText;
        this.icon = icon;
    }

    public Component getComponent() {
        if (component == null) {
            JLabel message = new JLabel(labelText, icon, SwingConstants.CENTER);
            message.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            message.setMaximumSize(message.getPreferredSize());

            Box messageBox = Box.createHorizontalBox();
            messageBox.add(Box.createGlue());
            messageBox.add(message);
            messageBox.add(Box.createGlue());

            component = Box.createVerticalBox();
            component.add(Box.createGlue());
            component.add(messageBox);
            component.add(Box.createGlue());
        }
        return component;
    }

    public BeanCollection getBeanCollection() {
        return null;
    }

    public void setBeanCollection(BeanCollection beanCollection) {
    }
}
