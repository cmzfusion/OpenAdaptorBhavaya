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

import org.bhavaya.ui.CloseAction;
import org.bhavaya.ui.RowLayout;
import org.bhavaya.ui.UIUtilities;

import javax.swing.*;


/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */

public class RowLayoutTest {
    public static void main(String[] args) {
//        createRowLayoutTestFrame(false, "RowLayout without row.recalcMaxHeight()");
        createRowLayoutTestFrame(true, "RowLayout with row.recalcMaxHeight()");
    }

    private static void createRowLayoutTestFrame(boolean dynamicLayout, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        RowLayout rowLayout = new RowLayout(300, 15, dynamicLayout);
        JPanel panel = new JPanel(rowLayout);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Row 1
        RowLayout.Row row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, true);

        JTextField tx1 = new JTextField("Hello");
        panel.add(tx1);
        row.addComponent(tx1);

        JTextField tx2 = new JTextField("How are you?");
        panel.add(tx2);
        row.addComponent(tx2, new RowLayout.RemainingWidthConstraint());

        // Row2
        rowLayout.addRow(row);
        row = new RowLayout.Row(10, RowLayout.RIGHT, RowLayout.MIDDLE, false);

        JTextField tx3 = new JTextField("Number 3");
        panel.add(row.addComponent(UIUtilities.createLabelledComponent("Textfield3", tx3), new RowLayout.RelativeWidthConstraint(0.4f)));

        JTextField tx4 = new JTextField("Number 4");
        panel.add(tx4);
        row.addComponent(tx4, new RowLayout.RelativeWidthConstraint(0.3f));

        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, false);
        JPanel flowLayout2Panel = new JPanel(new FlowLayout2());
        flowLayout2Panel.add(new JButton("Flow button 1"));
        flowLayout2Panel.add(new JButton("Flow button 2"));
        flowLayout2Panel.add(new JButton("Flow button 3"));
        flowLayout2Panel.add(new JButton("Flow button 4"));
        flowLayout2Panel.add(new JButton("Flow button 5"));
        flowLayout2Panel.add(new JButton("Flow button 6"));
        panel.add(row.addComponent(flowLayout2Panel,new RowLayout.RemainingWidthConstraint()));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.RIGHT, RowLayout.MIDDLE, false);

        JTable table = new JTable(new Object[][]{{"Hello"}}, new Object[]{"Hello"});
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(row.addComponent(scrollPane, new RowLayout.RemainingWidthConstraint()));

        rowLayout.addRow(row);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(new JButton("Ok"));
        buttonPanel.add(new JButton(new CloseAction(frame)));
        row = new RowLayout.Row(10, RowLayout.RIGHT, RowLayout.MIDDLE, true);
        panel.add(row.addComponent(buttonPanel));
        rowLayout.addRow(row);

        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
