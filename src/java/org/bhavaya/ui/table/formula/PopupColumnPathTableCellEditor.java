package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.PathSelectionDialog;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Table cell editor that pops up a dialog to allow selection of a bean path
 * User: ga2mhana
 * Date: 16/03/11
 * Time: 11:23
 */
public class PopupColumnPathTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    private Class beanType;
    private JDialog parentDialog;
    private PathSelectionDialog popup;
    private String currentValue = null;
    private JLabel component = new JLabel();

    public PopupColumnPathTableCellEditor(JDialog parentDialog, Class beanType) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup();
            }
        });
        this.beanType = beanType;
        this.parentDialog = parentDialog;
    }

    private void showPopup() {
        if(popup == null) {
            //  Set up the dialog where we do the actual editing
            popup = new PathSelectionDialog(parentDialog, beanType, new PathSelectionDialog.PathSelectionListener() {
                public void pathSelected(String path) {
                    currentValue = path;
                }
            });
        }
        popup.setSelectedValue(currentValue);
        Point p = component.getLocationOnScreen();
        popup.setLocation(p.x, p.y + component.getHeight());
        popup.setVisible(true);
        fireEditingStopped();
    }

    public Object getCellEditorValue() {
        return currentValue;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        currentValue = value.toString();
        String formattedValue = Utilities.getDisplayNameForPropertyPath(currentValue);
        component.setText(formattedValue);
        return component;
    }
}
