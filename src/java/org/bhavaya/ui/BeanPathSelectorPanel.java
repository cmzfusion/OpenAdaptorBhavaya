package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel that allows selection fo a bean path based on a type
 * User: ga2mhana
 * Date: 16/03/11
 * Time: 11:23
 */
public class BeanPathSelectorPanel extends JPanel {
    private Class beanType;
    private PathSelectionDialog popup;
    private JTextField pathField = new JTextField(20);
    private JButton btn = new JButton("...");

    public BeanPathSelectorPanel(Class beanType) {
        this.beanType = beanType;
        add(pathField);
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPopup();
            }
        });
        btn.setPreferredSize(new Dimension(20, 20));
        add(btn);
    }

    private void showPopup() {
        if(popup == null) {
            //  Set up the dialog where we do the actual editing
            popup = new PathSelectionDialog(UIUtilities.getDialogParent(this), beanType, new PathSelectionDialog.PathSelectionListener() {
                public void pathSelected(String path) {
                    setPath(path);
                }
            });
        }
        popup.setSelectedValue(pathField.getText());
        Point p = btn.getLocationOnScreen();
        popup.setLocation(p.x, p.y + btn.getHeight());
        popup.setVisible(true);
    }

    public String getPath() {
        return pathField.getText();
    }

    public void setPath(String path) {
        pathField.setText(path);
    }

    @Override
    public void setEnabled(boolean enable) {
        pathField.setEnabled(enable);
        btn.setEnabled(enable);
    }
}

