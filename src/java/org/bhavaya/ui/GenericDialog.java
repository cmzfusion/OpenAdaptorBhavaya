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
import java.awt.event.ActionEvent;

/**
 * Implements GenericWindow interface and implements some extras like close on escape.
 * Creates non-modal dialog unless constructor with modal flag is used.
 *
 * @author Daniel van Enckevort
 * @author Vladimir Hrmo
 * @version $Revision: 1.5.44.2 $
 */
public class GenericDialog extends JDialog implements GenericWindow {
    private Frame frameContainer;

    {
        installKeyboardActions();
    }

    public GenericDialog() throws HeadlessException {
    }

    public GenericDialog(Dialog owner) throws HeadlessException {
        super(owner);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Dialog owner, boolean modal) throws HeadlessException {
        super(owner, modal);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Dialog owner, String title) throws HeadlessException {
        super(owner, title);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Dialog owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) throws HeadlessException {
        super(owner, title, modal, gc);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Frame owner) throws HeadlessException {
        super(owner);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Frame owner, boolean modal) throws HeadlessException {
        super(owner, modal);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Frame owner, String title) throws HeadlessException {
        super(owner, title);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Frame owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public GenericDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
    }

    public static GenericDialog newInstance(GenericWindow owner) throws HeadlessException {
        if (owner instanceof Frame) {
            return new GenericDialog((Frame) owner);
        } else if (owner instanceof Dialog) {
            return new GenericDialog((Dialog) owner);
        } else {
            return null;
        }
    }

    public static GenericDialog newInstance(GenericWindow owner, boolean modal) throws HeadlessException {
        if (owner instanceof Frame) {
            return new GenericDialog((Frame) owner, modal);
        } else if (owner instanceof Dialog) {
            return new GenericDialog((Dialog) owner, modal);
        } else {
            return null;
        }
    }

    public static GenericDialog newInstance(GenericWindow owner, String title) throws HeadlessException {
        if (owner instanceof Frame) {
            return new GenericDialog((Frame) owner, title);
        } else if (owner instanceof Dialog) {
            return new GenericDialog((Dialog) owner, title);
        } else {
            return null;
        }
    }

    public static GenericDialog newInstance(GenericWindow owner, String title, boolean modal) throws HeadlessException {
        if (owner instanceof Frame) {
            return new GenericDialog((Frame) owner, title, modal);
        } else if (owner instanceof Dialog) {
            return new GenericDialog((Dialog) owner, title, modal);
        } else {
            return null;
        }
    }

    public static GenericDialog newInstance(GenericWindow owner, String title, boolean modal, GraphicsConfiguration gc) {
        if (owner instanceof Frame) {
            return new GenericDialog((Frame) owner, title, modal, gc);
        } else if (owner instanceof Dialog) {
            return new GenericDialog((Dialog) owner, title, modal, gc);
        } else {
            return null;
        }
    }

    /**
     * Looks up components parent window and creates a new non modal dialog. If the parent window
     * is not found, focused window is used instead.
     * @param component
     * @return non modal dialog
     */
    public static GenericDialog newInstance(Component component) {
        Window window = UIUtilities.getWindowParent(component);
        if (window == null) {
            window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        }
        if (window instanceof Frame) {
            return new GenericDialog((Frame) window);
        } else if (window instanceof Dialog) {
            return new GenericDialog((Dialog) window);
        } else {
            return null;
        }
    }

    public static GenericDialog newInstance(Component component, String title) {
        GenericDialog dialog = newInstance(component);
        if (dialog != null) dialog.setTitle(title);
        return dialog;
    }

    public void setIconImage(Image image) {
    }

    public Image getIconImage() {
        return null;
    }

    public boolean isAlwaysOntop() {
        return this.isAlwaysOnTop();
    }

    /**
     * Old pre_Java_1.5 implementation of setAlwaysOnTop. Use {@link Window#setAlwaysOnTop} when using
     * Java 1.5 or higher.
     *
     * @param alwaysOnTop
     */
    public void setAlwaysOnTopOld(boolean alwaysOnTop) {
        getFrameContainer().setAlwaysOnTop(alwaysOnTop);
    }

    public boolean isRedockingOnClose() {
        return false;
    }

    private Frame getFrameContainer() {
        if (frameContainer == null) {
            frameContainer = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
        }
        return frameContainer;
    }

    protected void setRootPane(JRootPane root) {
        super.setRootPane(root);
        installKeyboardActions();
    }

    private void installKeyboardActions() {
        ActionMap actionMap = getRootPane().getActionMap();
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "Escape");
        actionMap.put("Escape", new CloseOnEscapeAction(this));
    }
}
