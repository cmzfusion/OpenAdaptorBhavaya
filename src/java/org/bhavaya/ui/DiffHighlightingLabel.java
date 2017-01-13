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

import org.bhavaya.util.Generic;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.beans.Statement;
import java.lang.reflect.Method;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
public class DiffHighlightingLabel extends JLabel {
    private static final Log log = Log.getCategory(DiffHighlightingLabel.class);
    private static final Color CHANGED_COLOR = Color.blue;

    private Object oldValue;
    private Color originColour;

    public DiffHighlightingLabel(String labelText, final JComponent formElement, final String property) {
        super(labelText);
        this.oldValue = Generic.get(formElement, property);

        try {
            Runnable action = new Runnable() {
                public void run() {
                    if (!Utilities.equals(oldValue, Generic.get(formElement, property))) {
                        if (originColour == null) originColour = getForeground();
                        setForeground(CHANGED_COLOR);
                    } else if (getForeground().equals(CHANGED_COLOR)) {
                        setForeground(originColour);
                        originColour = null;
                    }
                }
            };

            Method addMethod;
            if (formElement instanceof JComboBox || formElement instanceof NarrowableComboBox
                    || formElement instanceof AbstractButton || formElement instanceof RadioButtonPanel) {
                addMethod = formElement.getClass().getMethod("addActionListener", ActionListener.class);
            } else if (formElement instanceof JSpinner) {
                addMethod = formElement.getClass().getMethod("addChangeListener", ChangeListener.class);
            } else {
                addMethod = formElement.getClass().getMethod("addFocusListener", FocusListener.class);
            }
            addMethod.invoke(formElement, UIUtilities.triggerMethodOnEvent(getListenerClassForComponent(formElement), null, action, "run"));
        } catch (Exception e) {
            log.error(e);
        }
    }

    public static void addListenerToComponent(ActionListener actionListener, JComponent formElement) throws Exception {
        Statement statement = new Statement(formElement, "addActionListener", new Object[]{actionListener});
        statement.execute();
    }

    public static void addListenerToComponent(ChangeListener changeListener, JComponent formElement) throws Exception {
        Statement statement = new Statement(formElement, "addActionListener", new Object[]{changeListener});
        statement.execute();
    }

    public static void addListenerToComponent(FocusListener focusListener, JComponent formElement) throws Exception {
        Statement statement = new Statement(formElement, "addActionListener", new Object[]{focusListener});
        statement.execute();
    }

    protected Class getListenerClassForComponent(JComponent guiComponent) {
        Class listenerClass;
        if (guiComponent instanceof JComboBox || guiComponent instanceof NarrowableComboBox
                || guiComponent instanceof AbstractButton || guiComponent instanceof RadioButtonPanel) {
            listenerClass = ActionListener.class;
        } else if (guiComponent instanceof JSpinner) {
            listenerClass = ChangeListener.class;
        } else {
            listenerClass = FocusListener.class;
        }
        return listenerClass;
    }
}
