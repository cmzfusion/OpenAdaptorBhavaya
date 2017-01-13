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

import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Constructor;

/**
 * Just like JSpinner combined with <code>SpinnerNumberModel</code> with these improvements:
 * - higlights invalid number with red color
 * - empty string in text component is considered as zero
 * - uses value class' constructor rather than decimal formatter to parse/validate the entered text
 * (formatter accepts '2233bla' as a value 2233 but constructor throws exception which is what is expected)
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public class BhavayaNumberSpinner extends JSpinner {

    public BhavayaNumberSpinner(SpinnerNumberModel model, String format) {
        super(model);

        final DefaultEditor editor = (DefaultEditor) getEditor();
        final JFormattedTextField textField = editor.getTextField();

        NumberEditorFormatter formatter = new NumberEditorFormatter(model, new DecimalFormat(format));
        DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
        textField.setFormatterFactory(factory);

        formatter.setCommitsOnValidEdit(true);
        textField.addPropertyChangeListener("editValid", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Boolean newValue = (Boolean) evt.getNewValue();
                textField.setForeground(newValue.booleanValue() ? Color.BLACK : Color.RED);
            }
        });

        Dimension preferredSize = UIUtilities.calculateDimensionForFormat(format);
        preferredSize.height = preferredSize.height - getInsets().top - getInsets().bottom;
        preferredSize.width = preferredSize.width - getInsets().left - getInsets().right;
        editor.setPreferredSize(preferredSize);
    }

    private static class NumberEditorFormatter extends NumberFormatter {
        private static final Integer ZERO_VALUE = new Integer(0);
        private final SpinnerNumberModel model;

        NumberEditorFormatter(SpinnerNumberModel model, NumberFormat format) {
            super(format);
            this.model = model;
            setValueClass(model.getValue().getClass());
        }

        public void setMinimum(Comparable min) {
            model.setMinimum(min);
        }

        public Comparable getMinimum() {
            return model.getMinimum();
        }

        public void setMaximum(Comparable max) {
            model.setMaximum(max);
        }

        public Comparable getMaximum() {
            return model.getMaximum();
        }

        public Object stringToValue(String string) throws ParseException {
            Class valueClass = getValueClass();
            if (string == null || string.length() == 0) {
                return Utilities.changeType(valueClass, ZERO_VALUE);
            }

            JFormattedTextField textField = getFormattedTextField();

            if (getFormat() instanceof DecimalFormat) {
                DecimalFormat format = (DecimalFormat) getFormat();
                DecimalFormatSymbols decimalFormatSymbols = format.getDecimalFormatSymbols();
                char groupingSeparator = decimalFormatSymbols.getGroupingSeparator();
                string = string.replaceAll("" + groupingSeparator, "");
            }

            if (valueClass == null && textField != null) {
                Object value = textField.getValue();

                if (value != null) {
                    valueClass = value.getClass();
                }
            }
            if (valueClass != null) {
                Constructor constructor;

                try {
                    constructor = valueClass.getConstructor(new Class[]{String.class});

                } catch (NoSuchMethodException nsme) {
                    constructor = null;
                }

                if (constructor != null) {
                    try {
                        return constructor.newInstance(new Object[]{string});
                    } catch (Throwable ex) {
                        throw new ParseException("Error creating instance", 0);
                    }
                }
            }
            return string;
        }
    }
}