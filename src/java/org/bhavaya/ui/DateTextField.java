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
import org.bhavaya.util.RelativeDateFunction;
import org.bhavaya.util.SymbolicDateFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class DateTextField extends Box {
    protected static final DateFunction DEFAULT_FROM_VALUE = SymbolicDateFunction.TODAY_DATEFUNCTION;

    private JTextField textField;
    private JButton editButton;
    protected DateFunction dateFunction;

    public DateTextField() {
        this(DEFAULT_FROM_VALUE);
    }

    public DateTextField(DateFunction dateFunction) {
        super(BoxLayout.X_AXIS);

        this.dateFunction = dateFunction;

        textField = new JTextField(14);
        textField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        textField.setEditable(false);
        textField.setFocusable(false);

        editButton = new JButton(new EditAction());
        editButton.setPreferredSize(new Dimension(20, 20));

        add(textField);
        add(editButton);

        update();
    }

    protected String getTitle() {
        return "Edit date";
    }

    protected void update() {
        updateTextField();

        //try to force correct min size
        textField.revalidate();
        revalidate();
        setMinimumSize(getPreferredSize());
        repaint();
    }

    public DateFunction getDateFunction() {
        return dateFunction;
    }

    private void updateTextField() {
        String text = dateFunction.getDescription();
        textField.setText(text);
        textField.setToolTipText(text);
    }

    private class EditAction extends AbstractAction {
        public EditAction() {
            super("...");
        }

        public void actionPerformed(ActionEvent e) {
            JFrame frameParent = UIUtilities.getFrameParent(DateTextField.this);
            final JDialog dialog = new JDialog(frameParent, getTitle(), true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            final DateFunctionPanel fromDatePanel = new DateFunctionPanel(RelativeDateFunction.PREPOSITION_BEFORE);
            fromDatePanel.setValue(dateFunction);
            fromDatePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "From"));

            JPanel popupDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            popupDatePanel.add(fromDatePanel);

            JPanel popupButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            popupButtonPanel.add(new JButton(new AuditedAbstractAction("OK") {
                public void auditedActionPerformed(ActionEvent e) {
                    dateFunction = fromDatePanel.getValue();
                    update();
                    dialog.dispose();
                }
            }));

            popupButtonPanel.add(new JButton(new AuditedAbstractAction("Cancel") {
                public void auditedActionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            }));

            JPanel popupContents = new JPanel(new BorderLayout());
            popupContents.add(popupDatePanel, BorderLayout.CENTER);
            popupContents.add(popupButtonPanel, BorderLayout.SOUTH);

            dialog.getContentPane().add(popupContents);
            dialog.pack();
            UIUtilities.centreInContainer(frameParent, dialog, 0, 0);
            dialog.show();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(new DateTextField());
        frame.pack();
        frame.setVisible(true);
    }
}
