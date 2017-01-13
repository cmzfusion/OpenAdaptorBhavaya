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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 5/10/13
 * Time: 1:06 PM
 */
public class DateFunctionDialog extends JDialog {

    private final DateFunctionDlgPanel datePanel;
    private JCheckBox allRowsCheckBox;
    private boolean cancelled = true;
    private final boolean allRowsOnly ;



    public DateFunctionDialog(Component component, DateFunction dateFunction, String title, boolean allRowsOnly){
        super((JFrame) UIUtilities.getWindowParent(component), title, true);

        datePanel = new DateFunctionDlgPanel(dateFunction);
        this.allRowsOnly = allRowsOnly;
        init();
        pack();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    void init(){
        datePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Select Date Function"));

        JPanel popupButtonPanel = new JPanel(new BorderLayout());

        allRowsCheckBox = new JCheckBox("Apply to All Table Rows");
        allRowsCheckBox.setSelected(allRowsOnly);
        allRowsCheckBox.setEnabled(!allRowsOnly);

        JPanel cancelP = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton cancelBtn = new JButton(new AuditedAbstractAction("Cancel") {
            public void auditedActionPerformed(ActionEvent e) {
                cancelled = true;
                dispose();
            }
        });
        cancelP.add(cancelBtn, BorderLayout.WEST);


        JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okPanel.add(allRowsCheckBox);

        JButton okBtn = new JButton(new AuditedAbstractAction("OK") {
            public void auditedActionPerformed(ActionEvent e) {
                cancelled = false;
                dispose();
            }
        });
        okPanel.add(okBtn);

        popupButtonPanel.add(cancelP, BorderLayout.WEST);
        popupButtonPanel.add(okPanel, BorderLayout.EAST);


        Container popupContents = getContentPane();
        popupContents.setLayout(new BorderLayout());
        popupContents.add(datePanel, BorderLayout.CENTER);
        popupContents.add(popupButtonPanel, BorderLayout.SOUTH);

    }

    public DateFunction getDateFunction(){
        return datePanel.getDateFunction();
    }

    public static void main(String a[]){
        JFrame jf = new JFrame("Test DateFunctionDialog");
        jf.pack();
        jf.setVisible(true);

        DateFunction df = new RelativeDateFunction();

        for(int i=0; i<10; i++){
            DateFunctionDialog dlg = new DateFunctionDialog(jf, df, "fred", false);
            dlg.setVisible(true);
            System.out.println("Df => " + dlg.getDateFunction() + " class => " + df.getClass().getName());
            df = dlg.getDateFunction();
        }

    }

    public boolean isCancelled() {
        return cancelled;
    }
    public boolean isAllRows(){
        return  allRowsCheckBox.isSelected();
    }
}