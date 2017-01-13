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

import org.bhavaya.ui.adaptor.Pipe;
import org.bhavaya.ui.adaptor.PropertyPipe;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import java.awt.event.FocusListener;

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
 * @version $Revision: 1.1 $
 */
public class BhavayaIntegerSpinner extends JSpinner {
    private DecimalTextField textField;

    public BhavayaIntegerSpinner(int min, int max) {
        this(min, max, new Integer(0));
    }

    public BhavayaIntegerSpinner(int min, int max, Integer initialValue) {
        this(new BhavayaSpinnerIntegerModel(initialValue, min, max));
    }

    public BhavayaIntegerSpinner(BhavayaSpinnerIntegerModel bhavayaSpinnerDateModel) {
        super(bhavayaSpinnerDateModel);
        super.setFocusable(false);
        textField.setBorder(new EmptyBorder(1, 1, 1, 1));
    }

    protected JComponent createEditor(SpinnerModel model) {
        if (model instanceof BhavayaSpinnerIntegerModel) {
            textField = new DecimalTextField("###", (int) Math.ceil(Math.log(((BhavayaSpinnerIntegerModel) model).getMax())),
                    (Number) model.getValue());

            // Bind value to textfield
            Pipe valueToTextFieldPipe = new PropertyPipe(this, "value", textField, "value");
            model.addChangeListener((ChangeListener) valueToTextFieldPipe.getListenerInterface(ChangeListener.class));

            // Find textfield to value
            Pipe textfieldToValuePipe = new PropertyPipe(textField, "value", this, "value");
            textField.addFocusListener((FocusListener) textfieldToValuePipe.getListenerInterface(FocusListener.class));

            return textField;
        } else {
            throw new IllegalStateException();
        }
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


    /**
     * Allows null date values.
     */
    public static class BhavayaSpinnerIntegerModel extends AbstractSpinnerModel {
        private int min;
        private int max;
        private Integer value;


        public BhavayaSpinnerIntegerModel(Integer value, int start, int end) {
            this.min = start;
            this.max = end;

            if (value != null) {
                this.value = value;
            }
        }

        public int getMax() {
            return max;
        }

        public int getMin() {
            return min;
        }

        public Object getNextValue() {
            if (value == null) return null;
            return value.intValue() + 1 < max ? new Integer(value.intValue() + 1) : value;
        }


        public Object getPreviousValue() {
            if (value == null) return null;
            return value.intValue() - 1 >= min ? new Integer(value.intValue() - 1) : value;
        }


        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            if (value != null && !(value instanceof Number)) throw new IllegalArgumentException("Invalid value");

            if (Utilities.equals(this.value, value)) {
                return;
            } else if (this.value != null && value == null) {
                this.value = null;
            } else if (this.value == null && value != null) {
                this.value = new Integer(((Number) value).intValue());
            } else if (value != null && !value.equals(this.value)) {
                this.value = new Integer(((Number) value).intValue());
            }
            fireStateChanged();
        }
    }
}