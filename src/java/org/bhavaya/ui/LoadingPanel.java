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
 * Created by IntelliJ IDEA.
 * @author Daniel van Enckevort
 * @version $Revision: 1.3 $
 */
public class LoadingPanel extends JPanel {
    private JProgressBar progressBar;
    private JLabel label;

    public LoadingPanel(String text) {
        this(text, null);
    }

    public LoadingPanel(String text, BoundedRangeModel progressModel) {
        progressBar = new JProgressBar();
        setProgressModel(progressModel);

        label = new JLabel(text);
        final LabelledComponent component = new LabelledComponent(label, progressBar);
        component.setBackground(null);
        setLayout(new MyLayoutManager(component));
        this.add(component);
    }

    public void setProgressModel(BoundedRangeModel progressModel) {
        if (progressModel != null) {
            progressBar.setModel(progressModel);
            progressBar.setIndeterminate(false);
        } else {
            progressBar.setIndeterminate(true);
        }
    }

    public void setText(String text) {
        label.setText(text);
    }

    public void setProgressText(String text) {
        progressBar.setString(text);
        progressBar.setStringPainted(text != null);
    }

    public void dispose() {
        if (progressBar.isIndeterminate()) progressBar.setIndeterminate(false);
    }

    private static class MyLayoutManager implements LayoutManager {
        private final LabelledComponent component;

        public MyLayoutManager(LabelledComponent component) {
            this.component = component;
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return component.getPreferredSize();
        }

        public Dimension minimumLayoutSize(Container parent) {
            return component.getMinimumSize();
        }

        public void layoutContainer(Container parent) {
            Dimension preferredSize = component.getPreferredSize();
            Dimension containerSize = parent.getSize();
            int x = (containerSize.width - preferredSize.width) / 2;
            int y = (containerSize.height - preferredSize.height) / 2;
            component.setBounds(x, y, preferredSize.width, preferredSize.height);
        }
    }
}
