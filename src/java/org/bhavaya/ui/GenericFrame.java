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

import org.bhavaya.util.Configuration;
import org.bhavaya.util.Task;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * @author Daniel van Enckevort
 * @version $Revision: 1.6.44.5 $
 */
public class GenericFrame extends JFrame implements GenericWindow {
    private static final String BRING_ALL_TO_FRONT_KEY = "bringAllToFrontWhenFocussed";

    private static final KeyStroke CLOSE_AND_DOCK_WINDOW_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK);
    private static final KeyStroke CLOSE_AND_DISCARD_WINDOW_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);

    private static boolean bringAllToFrontWhenFocused = ((Boolean) Configuration.getRoot().getObject(BRING_ALL_TO_FRONT_KEY, Boolean.FALSE, Boolean.class)).booleanValue();

    private static final ArrayList windows = new ArrayList();
    private static boolean lostFocusToNative = false;


    private WindowFocusListener toFrontWindowFocusListener;
    private NormalBoundsListener normalBoundsListener;
    private Rectangle normalBounds; // Frame bounds in NORMAL state

    private boolean shiftDown = false;
    private boolean closedByCntlW = true;

    private KeyEventDispatcher keyEventDispatcher = new KeyEventDispatcher() {
        public boolean dispatchKeyEvent(final KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.isShiftDown()) {
                shiftDown = true;
            }
            else{
                shiftDown = false;
            }

            if(GenericFrame.this == KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow()){
                if(closedByCntlW && e.getKeyCode() == KeyEvent.VK_W && e.isControlDown()){
                    WindowEvent windowClosing = new WindowEvent(GenericFrame.this, WindowEvent.WINDOW_CLOSING);
                    GenericFrame.this.dispatchEvent(windowClosing);
                    return true;
                }
            }
            // Pass the KeyEvent to the next KeyEventDispatcher in the chain
            return false;
        }
    };
    static {
        Configuration.addSaveTask(new Task(BRING_ALL_TO_FRONT_KEY) {
            public void run() {
                Configuration.getRoot().putObject(BRING_ALL_TO_FRONT_KEY, Boolean.valueOf(bringAllToFrontWhenFocused));
            }
        });
    }

    public static boolean isBringAllToFrontWhenFocussed() {
        return bringAllToFrontWhenFocused;
    }

    public static void setBringAllToFrontWhenFocused(boolean bringAllToFrontWhenFocused) {
        GenericFrame.bringAllToFrontWhenFocused = bringAllToFrontWhenFocused;
    }

    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyEventDispatcher);
    }
    public GenericFrame() throws HeadlessException {
        super();
        init();
    }

    public GenericFrame(GraphicsConfiguration gc) {
        super(gc);
        init();
    }

    public GenericFrame(String title) throws HeadlessException {
        this(title, true);
    }

    public GenericFrame(String title, boolean closedByCntlW) throws HeadlessException {
        super(title);
        this.closedByCntlW = closedByCntlW;
        init();
    }

    public GenericFrame(String title, GraphicsConfiguration gc) {
        super(title, gc);
        init();
    }

    public void dispose() {
        synchronized (windows) {
            removeWindowFocusListener(toFrontWindowFocusListener);
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyEventDispatcher);
            windows.remove(this);
            // now frame could never be readded to windows before dispose complete
        }
        removeComponentListener(normalBoundsListener);
        super.dispose();
    }


    protected void init() {
        synchronized (windows) {
            toFrontWindowFocusListener = new WindowFocusListener(this);
            addWindowFocusListener(toFrontWindowFocusListener);
            windows.add(this);
        }
        normalBoundsListener = new NormalBoundsListener();
        addComponentListener(normalBoundsListener);
        installKeyboardActions();
    }

    public static void bringAllToFront() {
        Object[] windowsArray;
        synchronized (windows) {
            windowsArray = windows.toArray();
        }

        for (int i = 0; i < windowsArray.length; i++) {
            Object o = windowsArray[i];
            if (o instanceof Window) {
                Window window = (Window) o;
                if (window instanceof Frame) {
                    Frame frame = (Frame) window;
                    if ((frame.getExtendedState() & Frame.ICONIFIED) != Frame.ICONIFIED) {
                        window.toFront();
                    }
                }
            }
        }
    }

    /**
     * Returns the frame bounds in NORMAL state (not maximized)
     */
    public Rectangle getNormalBounds() {
        return normalBounds;
    }

    public boolean isAlwaysOntop() {
        return this.isAlwaysOnTop();
    }

    /**
     * Old pre_Java_1.5 implementation of setAlwaysOnTop. Use {@link Window#setAlwaysOnTop} when using
     * Java 1.5 or higher.
     * @param alwaysOnTop
     */
    public void setAlwaysOnTopOld(boolean alwaysOnTop) {
        this.setAlwaysOnTop(alwaysOnTop);
    }

    public boolean isRedockingOnClose() {
        return !shiftDown;
    }

    protected void setRootPane(JRootPane root) {
        super.setRootPane(root);
        installKeyboardActions();
    }

    protected void installKeyboardActions() {
        ActionMap actionMap = getRootPane().getActionMap();
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "Escape");
        actionMap.put("Escape", new CloseOnEscapeAction(this));
    }

    private static class WindowFocusListener implements java.awt.event.WindowFocusListener {
        private GenericFrame frame;

        public WindowFocusListener(GenericFrame frame) {
            this.frame = frame;
        }

        public void windowGainedFocus(WindowEvent e) {
            synchronized (windows) {
                windows.remove(frame);
                windows.add(frame);

                if (lostFocusToNative) {
                    lostFocusToNative = false;
                    if (bringAllToFrontWhenFocused) bringAllToFront();
                }
            }
        }

        public void windowLostFocus(WindowEvent e) {
            Window oppositeWindow = e.getOppositeWindow();
            if (oppositeWindow == null) {   //lost focus to native app
                lostFocusToNative = true;
            }
        }
    }

    private class NormalBoundsListener implements ComponentListener {

        public void componentHidden(ComponentEvent e) {
            // do nothing
        }

        public void componentMoved(ComponentEvent e) {
            if (isInNormalState()) {
                storeBounds();
            }
        }

        private void storeBounds() {
            Rectangle bounds = getBounds();
            assert bounds.getHeight() > 0 && bounds.getWidth() > 0 : "This doesn't seem right - is the window really so small?";
            if (bounds.getHeight() > 0 && bounds.getWidth() > 0) {
                normalBounds = bounds;
            }
        }

        public void componentResized(ComponentEvent e) {
            if (isInNormalState()) {
                storeBounds();
            }
        }

        public void componentShown(ComponentEvent e) {
            if (isInNormalState()) {
                storeBounds();
            }
        }

        private boolean isInNormalState() {
            return (getExtendedState() == Frame.NORMAL);
        }
    }
}

