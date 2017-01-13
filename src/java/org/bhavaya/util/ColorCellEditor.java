package org.bhavaya.util;

import org.bhavaya.ui.UIUtilities;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Colour table cell editor.
 * User: Jon Moore
 * Date: 26/05/11
 * Time: 12:32
 */
public class ColorCellEditor extends AbstractCellEditor implements TableCellEditor {
    private ColorComponent component = new ColorComponent();
    Color currentColor;
    private JColorChooser colorChooser;
    private JDialog dialog;

    public ColorCellEditor() {

        colorChooser = new JColorChooser();
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                colorChooser.setColor(currentColor);
                showDialog();
            }
        });
    }

    private void showDialog() {
        if (dialog == null) {
            ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    currentColor = colorChooser.getColor();
                }
            };
            dialog = JColorChooser.createDialog(UIUtilities.getDialogParent(component), "Pick a Color", true, colorChooser, al, null);
        }
        dialog.setVisible(true);
        //Make the renderer reappear.
        fireEditingStopped();
    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
    public Object getCellEditorValue() {
        return currentColor;
    }

    //Implement the one method defined by TableCellEditor.
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        currentColor = (Color) value;
        component.setColor((Color) currentColor);
        component.setBackground(isSelected || table.hasFocus() ? table.getSelectionBackground() : table.getBackground());
        return component;
    }
}
