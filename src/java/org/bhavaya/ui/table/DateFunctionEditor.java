package org.bhavaya.ui.table;

import org.bhavaya.ui.JDateFunctionChooser;
import org.bhavaya.util.DateFunction;
import org.bhavaya.util.FixedDateFunction;
import org.bhavaya.util.RelativeDateFunction;
import org.bhavaya.util.SymbolicDateFunction;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateFunctionEditor extends DefaultCellEditor implements MouseListener {
    private DateFunction currentDate;
    private JTable currentTable;
    private String columnName = "";
    private SimpleDateFormat dateFormater = null;

    public DateFunctionEditor(JTextField textField, String dateFormat, TimeZone timezone) {
        super(textField);
        dateFormater = new SimpleDateFormat(dateFormat);
        if (dateFormater != null) dateFormater.setTimeZone(timezone);
        String text = currentDate != null ? dateFormater.format(currentDate.getDate()) : "";
        textField.setText(text);
        textField.addMouseListener(this);
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        currentDate = (DateFunction) value;
        currentTable = table;
        columnName = table.getColumnName(column);
        JTextField cellEditorComponent = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
        cellEditorComponent.setBorder(new LineBorder(Color.black));
        if (value instanceof FixedDateFunction) {
            cellEditorComponent.setText(dateFormater.format(((DateFunction) value).getDate()));
        }
        cellEditorComponent.select(0, cellEditorComponent.getText().length());
        return cellEditorComponent;
    }

    protected void updateDate(DateFunction newDate, boolean fireStopCellEditing) {
        currentDate = newDate;
        if (fireStopCellEditing) {
            stopCellEditing();
        }
        currentTable.requestFocusInWindow();
    }

    public Object getCellEditorValue() {
        String newText = ((JTextField) getComponent()).getText();
        if (currentDate == null || !currentDate.getDescription().equals(newText)) {
            parseAndUpdateNewDateFunction(newText);
        }
        return currentDate;
    }

    private void showDateEditor() {
        DateFunction newDate = JDateFunctionChooser.showDialog(currentTable, "Edit " + columnName, currentDate);
        ((JTextField) getComponent()).setText(newDate.getDescription());
        updateDate(newDate, true);
    }

    public void mouseClicked(MouseEvent e) {
        showDateEditor();
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    private void parseAndUpdateNewDateFunction(String newDate) {
        DateFunction modifiedDateFunction = null;
        boolean update = true;
        int length = newDate.length();
        // first check if the user is trying to clear the old date value
        if (length > 0) {
            if (length == 1) {
                // Capatalsie the first char as thats how symbolic dates are stored.
                newDate = newDate.toUpperCase();
            }
            modifiedDateFunction = SymbolicDateFunction.getInstance(newDate);
            if (modifiedDateFunction == null) {
                // ok so its not a symbolic date . lets check if its fixed date.
                if (newDate.indexOf("/") > 0) {
                    try {
                        Date date = dateFormater.parse(newDate);
                        FixedDateFunction fixedDateFucntion = new FixedDateFunction(date);
                        modifiedDateFunction = fixedDateFucntion;
                    } catch (ParseException e) {
                        // cant parse this so dont update
                        update = false;
                    }
                } else {  // Check if its a relative date.
                    RelativeDateFunction relativeDateFunction = new RelativeDateFunction();
                    if (newDate.startsWith("-")) {
                        relativeDateFunction.setPreposition("-");
                    } else {
                        relativeDateFunction.setPreposition("+");
                    }
                    try {
                        char c = newDate.charAt(newDate.length() - 1);
                        String middle = newDate.substring(0, newDate.length() - 1);
                        middle = middle.replace("-", "");
                        middle = middle.replace("+", "");
                        relativeDateFunction.setOffset(Integer.parseInt(middle));
                        relativeDateFunction.setCalendarOffsetType(Character.toString(c).toLowerCase());
                        modifiedDateFunction = relativeDateFunction;
                    } catch (Exception e) {
                        // cant parse this so dont update
                        update = false;
                    }
                }
            }
        }
        if (update) {   // update only if we could parse the newly entered date or if the user tried to clear the date
            updateDate(modifiedDateFunction, false);
        }
    }
}
