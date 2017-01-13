package org.bhavaya.ui.table.column.dialog;

import org.bhavaya.ui.view.TableView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: 364045
 * Date: Aug 30, 2006
 * Time: 11:53:54 AM
 *
 * A dialog to manage columns -
 * Allows the user to allocate columns to groups and change the column order and column attributes
 */
public class ColumnManagementDialog extends JFrame {

    private ColumnManagementDialogModel columnManagementDialogModel;
    private TableView tableView;
    private ColumnListPanel selectedItemList;

    public ColumnManagementDialog(){}
    public ColumnManagementDialog(TableView tableView, ColumnManagementDialogModel columnManagementDialogModel) {
        super("Manage Columns");
        setIconImage(ColumnListPanel.addToGroupIcon.getImage());
        this.columnManagementDialogModel = columnManagementDialogModel;
        this.tableView = tableView;

        initComponents();
        setSize(800, 600);
    }

    public void setColumnMoveListener(ColumnListPanel.ColumnMoveListener columnMoveListener) {
        selectedItemList.setColumnMoveListener(columnMoveListener);
    }

    private void initComponents() {
        JPanel panel = createCentralPanel();
        JPanel bottomPanel = createBottomPanel();

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(5,5,5,5));
        mainPanel.add(new GroupListPanel(columnManagementDialogModel), BorderLayout.NORTH);
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private JPanel createBottomPanel() {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        bottomPanel.add(okButton);
        return bottomPanel;
    }

    private JPanel createCentralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        selectedItemList = new ColumnListPanel(tableView, columnManagementDialogModel);
        panel.add(selectedItemList, gbc);
        return panel;
    }

}
