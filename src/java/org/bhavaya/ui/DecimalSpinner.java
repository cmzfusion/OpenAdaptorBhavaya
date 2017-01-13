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
import javax.swing.text.JTextComponent;
import java.awt.event.*;

public class DecimalSpinner extends JSpinner implements MouseWheelListener {
    private final static String DECIMAL_TYPED_ACTION_KEY = "decimalTyped";

    private JTextComponent textComponent;
    private SpinnerModel model;
    private boolean scrollable = false;

    public DecimalSpinner() {
    }

    public DecimalSpinner(SpinnerModel model) {
        super(model);
        this.model = model;
    }

    public void setEnabled(boolean enabled) {
        if (!scrollable || isEnabled() == enabled) {
            super.setEnabled(enabled);
            return;
        }

        if (enabled) {
            this.addMouseWheelListener(this);
        } else {
            this.removeMouseWheelListener(this);
        }
        super.setEnabled(enabled);
    }

    public void enableScrolling() {
        if (scrollable) return;

        scrollable = true;
        this.addMouseWheelListener(this);
    }

    public void disableScrolling() {
        scrollable = false;
        this.removeMouseWheelListener(this);
    }

    protected JComponent createEditor(SpinnerModel model) {
        JComponent editor = super.createEditor(model);
        configureEditor(editor);
        return editor;
    }

    private void configureEditor(JComponent editor) {
        if (editor instanceof DefaultEditor) {
            textComponent = ((DefaultEditor) editor).getTextField();
            textComponent.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(','), DECIMAL_TYPED_ACTION_KEY);

            textComponent.getActionMap().put(DECIMAL_TYPED_ACTION_KEY, new DecimalTypedAction());

            textComponent.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (!e.isTemporary()) {
                        //I know that this looks strange, as we're already on the event thread, but it's a
                        // workaround for longstanding Swing bug where the text is changed after the focus is gained.
                        // For more info see bug 4699955 or http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4699955
                        SwingUtilities.invokeLater(new Thread() {
                            public void run(){
                                textComponent.selectAll();
                            }
                        });
                    }
                }
            });
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        if (notches < 0) {
            do {
                Object nextValue = model.getNextValue();
                if (nextValue == null) break;
                model.setValue(nextValue);
                notches++;
            }
            while (notches < 0);
        } else {
            do {
                Object previousValue = model.getPreviousValue();
                if (previousValue == null) break;
                model.setValue(previousValue);
                notches--;
            }
            while (notches > 0);
        }
    }

    private class DecimalTypedAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            textComponent.replaceSelection(".");
        }
    }

    public void requestFocus() {
        ((JSpinner.DefaultEditor) getEditor()).getTextField().requestFocus();
    }

    public boolean requestFocusInWindow() {
        return ((JSpinner.DefaultEditor) getEditor()).getTextField().requestFocusInWindow();
    }

}
