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

import org.bhavaya.util.DateFunction;
import org.bhavaya.util.DateFunctionInterval;
import org.bhavaya.util.SymbolicDateFunction;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class DateIntervalTextField extends Box {
    protected static final DateFunction DEFAULT_FROM_VALUE = SymbolicDateFunction.TODAY_DATEFUNCTION;
    protected static final DateFunction DEFAULT_TO_VALUE = SymbolicDateFunction.TODAY_DATEFUNCTION;

    private JTextField textField;
    protected DateFunctionInterval interval;

    public DateIntervalTextField() {
        this(new DateFunctionInterval(DEFAULT_FROM_VALUE,  DEFAULT_TO_VALUE));
    }

    public DateIntervalTextField(DateFunctionInterval interval) {
        super(BoxLayout.X_AXIS);

        this.interval = interval;

        textField = new JTextField(14);
        textField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        textField.setEditable(false);
        textField.setFocusable(false);

        JButton editButton = new JButton(new EditDateIntervalAction("...", null, null, interval) {
            protected void update(DateFunctionInterval interval) {
                DateIntervalTextField.this.interval = interval;
                DateIntervalTextField.this.update();
            }

            public String getEditorTitle() {
                return getTitle();
            }
        });
        editButton.setPreferredSize(new Dimension(20, 20));

        add(textField);
        add(editButton);

        update();
    }

	/**
    * Add an Document to the underlying textField
     * @param l documentListener to add
     */
    public void addDocumentListener(DocumentListener l) {
        textField.getDocument().addDocumentListener(l);
    }

    public void removeDocumentListener(DocumentListener l) {
        textField.getDocument().removeDocumentListener(l);
    }

    protected String getTitle() {
        return "Edit interval";
    }

    protected void update() {
        updateTextField();

        //try to force correct min size
        textField.revalidate();
        revalidate();
        setMinimumSize(getPreferredSize());
        repaint();
    }

    public DateFunctionInterval getInterval() {
        return interval;
    }

    private void updateTextField() {
        String text = interval.getDescription();
        textField.setText(text);
        textField.setToolTipText(text);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(new DateIntervalTextField());
        frame.pack();
        frame.setVisible(true);
    }

}
