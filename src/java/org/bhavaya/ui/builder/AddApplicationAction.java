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

package org.bhavaya.ui.builder;

import org.bhavaya.beans.generator.Application;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.ImageIconCache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
class AddApplicationAction extends AbstractAction {
    private static final String ADD_ICON = "add.gif";

    private ApplicationCollection applications;
    private Application application;

    public AddApplicationAction(ApplicationCollection applications) {
        super("Add", ImageIconCache.getImageIcon(ADD_ICON));
        putValue(Action.SHORT_DESCRIPTION, "Add Application");
        putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
        this.applications = applications;
    }

    public void actionPerformed(ActionEvent e) {
        // bring up the form with the same data the user last entered
        application = application == null ? new Application() : (Application) BeanUtilities.verySlowDeepCopy(application);

        final ApplicationForm form = new ApplicationForm(application, true);
        String title = "Create Application";
        JFrame frame = new JFrame(title) {
            public void dispose() {
                form.dispose();
                super.dispose();
            }
        };
        frame.setIconImage(Builder.FRAME_ICON.getImage());
        Action runAction = new CreateApplicationAction(frame, application, applications);
        Component owner = (Component) e.getSource();
        form.displayForm(frame, owner, runAction);
    }
}
