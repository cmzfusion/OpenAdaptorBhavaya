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

package org.bhavaya.ui.dataset;

import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.ui.CloseAction;
import org.bhavaya.ui.AuditedAbstractAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */

public class CriterionDialog extends JDialog {
    CriterionPanel criterionPanel = null;

    public CriterionDialog(Criterion initialCriterion, Class beanType, JFrame owner, String criteriaKey) {
        super(owner, "Criterion editor", true);
        init(initialCriterion, beanType, criteriaKey);
    }

    public CriterionDialog(Criterion initialCriterion, Class beanType, JDialog owner, String criteriaKey) {
        super(owner, "Criterion editor", true);
        init(initialCriterion, beanType, criteriaKey);
    }

    private void init(Criterion initialCriterion, Class beanType, String criteriaKey) {
        criterionPanel = new CriterionPanel(initialCriterion, criteriaKey, beanType);

        JButton applyButton = new JButton(new OkAction());
        JButton cancelButton = new JButton(new CloseAction(this , "Cancel"));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(criterionPanel, BorderLayout.CENTER);
        containerPanel.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(containerPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
    }

    public Criterion getCriterion() {
        return criterionPanel.getCriterion();
    }

    private class OkAction extends AuditedAbstractAction {
        public OkAction() {
            putValue(Action.NAME, "OK");
        }

        public void auditedActionPerformed(ActionEvent e) {
            try {
                criterionPanel.setActiveCriterion();
                CriterionDialog.this.dispose();
            } catch (CriterionPanel.ValidationException ex) {
                JOptionPane.showMessageDialog(CriterionDialog.this, createDetailString(ex),
                        "Insufficient information to generate criterion", JOptionPane.WARNING_MESSAGE);
            }
        }

        private String createDetailString(CriterionPanel.ValidationException e) {
            StringBuffer messageBuffer = new StringBuffer("<html>");
            String[] reasons = e.getInvalidFields();
            for (int i = 0; i < reasons.length; i++) {
                messageBuffer.append(reasons[i]).append("<br>");
            }
            return messageBuffer.append("</ul></html>").toString();
        }
    }
}
