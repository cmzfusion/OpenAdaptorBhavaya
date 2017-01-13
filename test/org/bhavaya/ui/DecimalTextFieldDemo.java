package org.bhavaya.ui;

import org.bhavaya.ui.DecimalTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Date: 01-Mar-2004
 * Time: 12:09:09
 */
public class DecimalTextFieldDemo implements ActionListener {

    private JLabel label;
    private DecimalTextField dtf;

    public DecimalTextFieldDemo(JPanel contents, String format) {
        dtf = new DecimalTextField(format);
        dtf.addActionListener(this);
        contents.add(dtf, BorderLayout.NORTH);
        label = new JLabel("No number", JLabel.CENTER);
        contents.add(label, BorderLayout.SOUTH);
    }

    public void actionPerformed(ActionEvent event) {
        label.setText(dtf.getValue().toString());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Decimal Text Field Demo");
        JPanel contents = new JPanel();
        contents.setLayout(new BorderLayout());

        String format = "###.##";

        new DecimalTextFieldDemo(contents, format);

        frame.setContentPane(contents);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
