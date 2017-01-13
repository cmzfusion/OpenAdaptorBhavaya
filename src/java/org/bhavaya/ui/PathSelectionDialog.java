package org.bhavaya.ui;

import org.bhavaya.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog to allow selection of a bean path, based on a bean type
 * User: ga2mhan
 * Date: 23/05/12
 * Time: 13:46
 */
public class PathSelectionDialog extends JDialog {
    private SearchableBeanPathSelector columnControl;
    private ColumnSelectionModel selectionModel;
    private PathSelectionListener listener;

    public PathSelectionDialog(JDialog parent, Class beanType, PathSelectionListener listener) {
        super(parent, "Select Bean Path", true);
        selectionModel = new ColumnSelectionModel();
        this.listener = listener;
        columnControl = new SearchableBeanPathSelector(beanType,
                FilteredTreeModel.DEFAULT_ADD_PROPERTY_FILTER,
                FilteredTreeModel.DEFAULT_ADD_CHILDREN_FILTER,
                selectionModel);

        getContentPane().add(columnControl);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        JButton ok = new JButton("Ok");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseSelected();
            }
        });
        ok.setPreferredSize(cancel.getPreferredSize());

        JPanel buttons = new JPanel();
        buttons.add(ok);
        buttons.add(cancel);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setSize(200, 400);
    }

    public void setSelectedValue(String value) {
        columnControl.selectPath(value);
    }

    private void chooseSelected() {
        if(listener != null) {
            listener.pathSelected(selectionModel.getSelectedPath());
        }
        setVisible(false);
    }

    private class ColumnSelectionModel extends BeanPathSelector.SelectionModel {
        private String selected;
        public ColumnSelectionModel() {
            super(false);
        }

        public void locatorSelected(String columnLocator) {
            selected = columnLocator;
        }

        public boolean isSelected(String columnLocator) {
            return Utilities.equals(columnLocator, selected);
        }

        public String getSelectedPath() {
            return selected;
        }

        @Override
        public void selectionComplete() {
            chooseSelected();
        }

        @Override
        public void clearSelected() {
            selected = "";
        }
    }

    public interface PathSelectionListener {
        void pathSelected(String path);
    }
}