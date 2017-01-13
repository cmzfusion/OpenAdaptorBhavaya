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

import org.bhavaya.util.DateUtilities;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.InternationalFormatter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.*;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Extends the normal date spinner to add more features.
 * 1) Cursor left and right change fields (i.e. year, month, day), rather than character positions in the text field.
 * 2) Pressing the end button, sets the calendarField to that of the last field (rather than the default behavoir which is not to change it).
 * 3) The next and previous arrow buttons do not operate on calendarField passed through the constructor.  E.g if the calendar
 * field is SECOND, the arrows change the DAY rather than the second.  The user would have to explicitly move the cursor
 * to the seconds field to modify them using the arrow buttons.
 * 4) The date model allows null values.
 * 5) The initial size is based on the date format rather than the actual formatted date value, as a null date formats to "".
 * 6) Where we are not interest in the time component, assume the date is GMT, however the date is not rendered in local time, so it
 * appears as the same date in all timezones.
 *
 * @author Daniel van Enckevort
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public class BhavayaDateSpinner extends JSpinner implements EditableComponent {

    private JFormattedTextField textField;

    public BhavayaDateSpinner(int calendarField) {
        this(calendarField, new Date());
    }

    public BhavayaDateSpinner(int calendarField, Date initialDate) {
        this(new BhavayaSpinnerDateModel(initialDate, null, null, calendarField));
    }

    public BhavayaDateSpinner(BhavayaSpinnerDateModel bhavayaSpinnerDateModel) {
        super(bhavayaSpinnerDateModel);
        super.setFocusable(false);
        UIUtilities.formatSpinner(this, getDateFormat(getModel()));
        setDefaultCalendarField(); // do this last
    }

    public void setDefaultCaretPosition(int defaultCaretPosition) {
        ((BhavayaDateEditor) getEditor()).setDefaultCaretPosition(defaultCaretPosition);
    }

    protected JComponent createEditor(SpinnerModel model) {
        if (model instanceof BhavayaSpinnerDateModel) {
            String dateFormatString = getDateFormat(model);
            JSpinner.DateEditor editor = new BhavayaDateEditor(this, (SpinnerDateModel) model, dateFormatString, createOverrideDateFormat());
            textField = editor.getTextField();
            return editor;
        } else {
            throw new IllegalStateException();
        }
    }

    protected DateFormat createOverrideDateFormat() {
        return null;
    }

    public void setEditable(boolean editable) {
        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            // disable arrow buttons
            if (component instanceof JButton) {
                component.setEnabled(editable);
            }
        }
        textField.setEditable(editable);
    }

    public boolean isEditable() {
        return textField.isEditable();
    }

    public void setFocusable(boolean focusable) {
        textField.setFocusable(focusable);
    }

    public boolean requestFocusInWindow() {
        return textField.requestFocusInWindow();
    }

    public void requestFocus() {
        textField.requestFocus();
    }

    public boolean requestFocus(boolean temporary) {
        return textField.requestFocus(temporary);
    }

    private String getDateFormat(SpinnerModel model) {
        return DateFormatProvider.getInstance().getDateFormatString(((BhavayaSpinnerDateModel) model).getInitialCalendarField());
    }

    public void updateDateFormat() {
        UIUtilities.formatSpinner(this, getDateFormat(getModel()));
        setEditor(createEditor(getModel()));
        updateUI();
    }

    public java.util.Date getDate() {
        java.util.Date date = (java.util.Date) getValue();

        if (date == null) return null;

        int initialCalendarField = getDateModel().getInitialCalendarField();
        if (initialCalendarField == Calendar.DAY_OF_MONTH ||
                initialCalendarField == Calendar.DAY_OF_WEEK ||
                initialCalendarField == Calendar.DAY_OF_WEEK_IN_MONTH ||
                initialCalendarField == Calendar.DAY_OF_YEAR ||
                initialCalendarField == Calendar.MONTH ||
                initialCalendarField == Calendar.YEAR) {
            return new java.sql.Date(date.getTime());
        } else {
            return date;
        }
    }

    public void setDate(java.util.Date date) {
        setValue(date);
    }

    public BhavayaSpinnerDateModel getDateModel() {
        return (BhavayaSpinnerDateModel) getModel();
    }

    private void setDefaultCalendarField() {
        int modifyCalendarField = 0;
        int initialCalendarField = getDateModel().getInitialCalendarField();
        if (initialCalendarField == Calendar.YEAR || initialCalendarField == Calendar.MONTH) {
            modifyCalendarField = initialCalendarField;
        } else {
            modifyCalendarField = Calendar.DAY_OF_MONTH;
        }

        getDateModel().setCalendarField(modifyCalendarField);
    }

    private int getLastCalendarField() {
        String text = textField.getText();
        if (text == null) return getDateModel().getCalendarField();
        return getCalendarField(text.length() - 1);

    }

    private int getCalendarField(int start) {
        JFormattedTextField.AbstractFormatter formatter = textField.getFormatter();

        if (formatter instanceof InternationalFormatter) {
            Format.Field[] fields = ((InternationalFormatter) formatter).getFields(start);

            for (int counter = 0; counter < fields.length; counter++) {
                if (fields[counter] instanceof DateFormat.Field) {
                    int calendarField;

                    if (fields[counter] == DateFormat.Field.HOUR1) {
                        calendarField = Calendar.HOUR;
                    } else {
                        calendarField = ((DateFormat.Field) fields[counter]).getCalendarField();
                    }

                    if (calendarField != -1) {
                        return calendarField;
                    }
                }
            }
        }
        return getDateModel().getCalendarField();
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (!isEditable()) return false;
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    /**
     * Allows null date values.
     */
    public static class BhavayaSpinnerDateModel extends SpinnerDateModel {
        private Comparable start, end;
        private Calendar value;
        private int initialCalendarField;
        private int calendarField;

        private boolean calendarFieldOK(int calendarField) {
            switch (calendarField) {
                case Calendar.ERA:
                case Calendar.YEAR:
                case Calendar.MONTH:
                case Calendar.WEEK_OF_YEAR:
                case Calendar.WEEK_OF_MONTH:
                case Calendar.DAY_OF_MONTH:
                case Calendar.DAY_OF_YEAR:
                case Calendar.DAY_OF_WEEK:
                case Calendar.DAY_OF_WEEK_IN_MONTH:
                case Calendar.AM_PM:
                case Calendar.HOUR:
                case Calendar.HOUR_OF_DAY:
                case Calendar.MINUTE:
                case Calendar.SECOND:
                case Calendar.MILLISECOND:
                    return true;
                default:
                    return false;
            }
        }


        public BhavayaSpinnerDateModel(Date value, Comparable start, Comparable end, int calendarField) {
            if (!calendarFieldOK(calendarField)) throw new IllegalArgumentException("invalid calendarField");
            this.start = start;
            this.end = end;
            this.calendarField = calendarField;
            this.initialCalendarField = calendarField;

            if (value != null) {
                if (!(((start == null) || (start.compareTo(value) <= 0)) && ((end == null) || (end.compareTo(value) >= 0)))) {
                    throw new IllegalArgumentException("(start <= value <= end) is false");
                }

                this.value = fixDatetime(value);
            }
        }

        private Calendar fixDatetime(Date date) {
            if (date == null) return null;
            if (initialCalendarField == Calendar.DAY_OF_MONTH ||
                    initialCalendarField == Calendar.DAY_OF_WEEK ||
                    initialCalendarField == Calendar.DAY_OF_WEEK_IN_MONTH ||
                    initialCalendarField == Calendar.DAY_OF_YEAR ||
                    initialCalendarField == Calendar.MONTH ||
                    initialCalendarField == Calendar.YEAR) {
                Calendar gmtCalendar = DateUtilities.newGmtCalendar();
                gmtCalendar.setTime(date);
                gmtCalendar.set(Calendar.HOUR_OF_DAY, 0);
                gmtCalendar.set(Calendar.MINUTE, 0);
                gmtCalendar.set(Calendar.SECOND, 0);
                gmtCalendar.set(Calendar.MILLISECOND, 0);
                return gmtCalendar;
            } else {
                Calendar localCalendar = Calendar.getInstance();
                localCalendar.setTime(date);
                return localCalendar;
            }
        }

        public int getInitialCalendarField() {
            return initialCalendarField;
        }


        public BhavayaSpinnerDateModel() {
            this(null, null, null, Calendar.DAY_OF_MONTH);
        }

        public void setStart(Comparable start) {
            if ((start == null) ? (this.start != null) : !start.equals(this.start)) {
                this.start = start;
                fireStateChanged();
            }
        }

        public Comparable getStart() {
            return start;
        }


        public void setEnd(Comparable end) {
            if ((end == null) ? (this.end != null) : !end.equals(this.end)) {
                this.end = end;
                fireStateChanged();
            }
        }

        public Comparable getEnd() {
            return end;
        }


        public void setCalendarField(int calendarField) {
            if (!calendarFieldOK(calendarField)) {
                throw new IllegalArgumentException("invalid calendarField");
            }
            if (calendarField != this.calendarField) {
                this.calendarField = calendarField;
                fireStateChanged();
            }
        }

        public int getCalendarField() {
            return calendarField;
        }

        public Object getNextValue() {
            if (value == null) return null;
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            cal.setTime(value.getTime());
            cal.add(calendarField, 1);
            Date next = cal.getTime();
             return ((end == null) || (end.compareTo(next) >= 0)) ? next : null;
        }


        public Object getPreviousValue() {
            if (value == null) return null;
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            cal.setTime(value.getTime());
            cal.add(calendarField, -1);
            Date prev = cal.getTime();
            return ((start == null) || (start.compareTo(prev) <= 0)) ? prev : null;
        }


        public Date getDate() {
            if (value == null) return null;
            return value.getTime();
        }

        public Object getValue() {
            if (value == null) return null;
            return value.getTime();
        }

        public void setValue(Object value) {
            if (value != null && !(value instanceof Date)) throw new IllegalArgumentException("Invalid value");

            if (Utilities.equals(this.value, value)) {
                return;
            } else if (this.value != null && value == null) {
                this.value = null;
            } else if (this.value == null && value != null) {
                this.value = fixDatetime((Date) value);
            } else if (!value.equals(this.value.getTime())) {
                this.value = fixDatetime((Date) value);
            }
            fireStateChanged();
        }
    }

    private static class BhavayaDateEditor extends JSpinner.DateEditor implements FocusListener {

        private int defaultCaretPosition = 0;

        public BhavayaDateEditor(BhavayaDateSpinner spinner, SpinnerDateModel model, String dateFormatString, DateFormat overrideDateFormat) {
            super(spinner, dateFormatString);
            JFormattedTextField textField = getTextField();
            textField.addKeyListener(new DateEditorKeyAdapter(spinner));
            textField.addFocusListener(this);

            DateFormatter formatter = new BhavayaDateEditorFormatter(model, dateFormatString, overrideDateFormat);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            textField.setFormatterFactory(factory);
        }

        public void setDefaultCaretPosition(int defaultCaretPosition) {
            this.defaultCaretPosition = defaultCaretPosition;
        }

        public void focusGained(FocusEvent e) {
            if (!e.isTemporary()) {
                //I know that this looks strange, as we're already on the event thread, but it's a
                // workaround for longstanding Swing bug where the text is changed after the focus is gained.
                // For more info see bug 4699955 or http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4699955
                final JFormattedTextField textField = getTextField();
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        textField.setCaretPosition(defaultCaretPosition);
                    }
                });
            }
        }

        public void focusLost(FocusEvent e) {

        }
    }

    private static class BhavayaDateEditorFormatter extends DateFormatter {
        private final SpinnerDateModel model;
        private final DateFormat gmtFormat;
        private DateFormat overideFormat;
        private final String formatString;

        BhavayaDateEditorFormatter(SpinnerDateModel model, String formatString, DateFormat overrideFormat) {
            super(new SimpleDateFormat(formatString));
            this.overideFormat = overrideFormat;
            this.model = model;
            this.formatString = formatString;
            this.gmtFormat = (DateFormat) getFormat().clone();
            gmtFormat.setCalendar(DateUtilities.newGmtCalendar());
        }

        public void setMinimum(Comparable min) {
            model.setStart(min);
        }

        public Comparable getMinimum() {
            return model.getStart();
        }

        public void setMaximum(Comparable max) {
            model.setEnd(max);
        }

        public Comparable getMaximum() {
            return model.getEnd();
        }

        public String valueToString(Object value) throws ParseException {
            if (value != null && formatString.length() < 11) {
                String string = gmtFormat.format(value);
                return string;
            } else {
                return super.valueToString(value);
            }
        }

        public Object stringToValue(String text) throws ParseException {
            if (overideFormat != null) {
                Date date = overideFormat.parse(text);
                if (date != null) return date;
            }
            int length = text.length();

            if (length < 11) {
                return gmtFormat.parse(text);
            } else {
                return super.stringToValue(text);
            }
        }
    }

    private static class DateEditorKeyAdapter extends KeyAdapter {
        private BhavayaDateSpinner spinner;

        public DateEditorKeyAdapter(BhavayaDateSpinner spinner) {
            this.spinner = spinner;
        }

        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_RIGHT: {
                    int caretPosition = spinner.textField.getCaretPosition();
                    String text = spinner.textField.getText();

                    if (text != null && caretPosition < text.length() - 1) {
                        setNextNonDigitPosition(caretPosition, text, e);
                    }
                }
                break;
                case KeyEvent.VK_LEFT: {
                    int caretPosition = spinner.textField.getCaretPosition();
                    String text = spinner.textField.getText();

                    if (text != null && caretPosition > 0) {
                        setPreviousNonDigitPosition(caretPosition, text, e);
                    }
                }
                break;
                case KeyEvent.VK_END: {
                    spinner.getDateModel().setCalendarField(spinner.getLastCalendarField());
                }
                break;
                default:
            }
        }

        private void setCaretPosition(int caretPosition, int nextPosition, KeyEvent e) {
            if (nextPosition > 0) {
                spinner.textField.setCaretPosition(nextPosition);
            } else {
                spinner.textField.setCaretPosition(caretPosition);
            }
            e.consume();
        }

        private void setPreviousNonDigitPosition(int caretPosition, String text, KeyEvent e) {
            int nextPosition = -1;
            for (int i = caretPosition - 1; i >= 0; i--) {
                if (!Character.isDigit(text.charAt(i))) {
                    nextPosition = i - 1;
                    break;
                }
            }
            setCaretPosition(caretPosition, nextPosition, e);
        }

        private void setNextNonDigitPosition(int caretPosition, String text, KeyEvent e) {
            int nextPosition = -1;
            for (int i = caretPosition; i < text.length(); i++) {
                if (!Character.isDigit(text.charAt(i))) {
                    nextPosition = i + 1;
                    break;
                }
            }
            setCaretPosition(caretPosition, nextPosition, e);
        }
    }

}
