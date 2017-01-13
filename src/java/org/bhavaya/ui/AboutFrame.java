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

import org.bhavaya.util.ImageIconCache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * About box.  Basically, the splash screen again and some useful application and system information.
 *
 * @author Parwinder Sekhon, Brendon McLean
 * @version $Revision: 1.5 $
 */
public class AboutFrame extends JFrame {
    private static ImageIcon image;

    public AboutFrame(Container container, String title) {
        super(title);
        setIconImage(ApplicationContext.getInstance().getApplicationView().getImageIcon().getImage());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        JLabel imagePanel = createImagePanel(ApplicationContext.getSplashScreenFilename());

        JButton okButton = new JButton(new AuditedAbstractAction("Ok") {
            public void auditedActionPerformed(ActionEvent e) {
                 AboutFrame.this.dispose();
            }
        });
        JPanel okButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        okButtonPanel.add(okButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(imagePanel);
        panel.add(okButtonPanel, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        setContentPane(panel);
        pack();

        UIUtilities.centreInContainer(container, this, 0, 0);
        setVisible(true);
    }

    private JLabel createImagePanel(String graphicFile) {
        if (image == null) {
            image = ImageIconCache.getImageIcon(graphicFile);
        }
        JLabel imageLabel = new JLabel(image);
        return imageLabel;
    }
}
