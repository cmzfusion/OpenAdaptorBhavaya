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
import java.net.URL;

/**
 * Description
 *
 * @author Brendon McLean, &#169;2001
 * @version $Revision: 1.1 $ by $Author: brendon9x $ on $Date: 2003/11/13 12:47:32 $
 *
 */
public class SplashScreen extends JWindow {
    public SplashScreen(URL splashImageFilename, URL logoImageFileName, String descriptionText) {
        this(splashImageFilename, logoImageFileName, new JLabel(descriptionText));
    }

    public SplashScreen(URL splashImageFilename, URL logoImageFileName, Component description) {
        super();
        // Load and add splash image
        getContentPane().add(new JLabel(new ImageIcon(splashImageFilename)), BorderLayout.CENTER);
        JPanel descriptionPanel = new JPanel(new RowLayout(getPreferredSize().width, 0));
        descriptionPanel.setBackground(Color.WHITE);
        descriptionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));
        RowLayout rowLayout = (RowLayout) descriptionPanel.getLayout();
        RowLayout.Row row = new RowLayout.Row(0, RowLayout.LEFT, RowLayout.MIDDLE, true);
        descriptionPanel.add(row.addComponent(description, new RowLayout.RelativeWidthConstraint(0.5)));
        descriptionPanel.add(row.addComponent(new JLabel(new ImageIcon(logoImageFileName))));
        rowLayout.addRow(row);
        getContentPane().add(descriptionPanel, BorderLayout.SOUTH);

        ((JComponent) getContentPane()).setBorder(BorderFactory.createLineBorder(Color.black));
        pack();
        UIUtilities.centreInScreen(this, 0, 0);
    }
}
