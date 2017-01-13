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

import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * A formatted and validated text field that can multiply its value by T, M and B.  This is one of the coolest
 * classes I've made.  I'm so proud.
 *
 * @author Brendon McLean
 * @version $Revision: 1.10.4.1 $
 */

public class DecimalTextField extends JTextField {
    private static final Log log = Log.getCategory(DecimalTextField.class);
    private final static String SHIFT_DECIMAL_3_ACTION_KEY = "shift3";
    private final static String SHIFT_DECIMAL_6_ACTION_KEY = "shift6";
    private final static String SHIFT_DECIMAL_9_ACTION_KEY = "shift9";
    private final static String DECIMAL_TYPED_ACTION_KEY = "decimalTyped";

    private NumberFormat displayFormat;
    private NumberFormat editFormat;

    private boolean allowNegative = true;
    private boolean selectAfterDecimal = false;
    private boolean disableFocusSelect = false;
    private double displayMultiplier = 1;
    private int memory = -1;

    public DecimalTextField(String format, int columns, Number initalValue) {
        this(format, columns);
        setValue(initalValue);
    }

    public DecimalTextField(String format) {
        this(format, format.length());
    }

    public DecimalTextField(String format, int columns) {
        this(columns);
        setDecimal(format);
    }

    public void setDisplayMultiplier(double displayMultiplier) {
        this.displayMultiplier = displayMultiplier;
        Document document = getDocument();
        if (document instanceof FormattedDocument) {
            FormattedDocument formattedDocument = (FormattedDocument) document;
            formattedDocument.setDisplayMultiplier(displayMultiplier);
        }
    }

    protected double getDisplayMultiplier() {
        return displayMultiplier;
    }

    protected DecimalTextField(int columns) {
        super(columns);
        setHorizontalAlignment(JTextField.RIGHT);

        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), SHIFT_DECIMAL_3_ACTION_KEY);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), SHIFT_DECIMAL_3_ACTION_KEY);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), SHIFT_DECIMAL_6_ACTION_KEY);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), SHIFT_DECIMAL_9_ACTION_KEY);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DECIMAL, 0), DECIMAL_TYPED_ACTION_KEY);
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0), DECIMAL_TYPED_ACTION_KEY);

        getActionMap().put(SHIFT_DECIMAL_3_ACTION_KEY, new MultiplyAction(1000));
        getActionMap().put(SHIFT_DECIMAL_6_ACTION_KEY, new MultiplyAction(1000000));
        getActionMap().put(SHIFT_DECIMAL_9_ACTION_KEY, new MultiplyAction(1000000000));
        getActionMap().put(DECIMAL_TYPED_ACTION_KEY, new DecimalTypedAction());
        
        
    }
    
    public void setCaretPosition(int value)
    {
    	log.info("setting caret " + value);
    	super.setCaretPosition(value);
    }
    
    public void setDecimal(String format) {
        Number currentValue = getValue();
        setEditFormat(new DecimalFormat(format));
        setDisplayFormat(new DecimalFormat(format));
        String editPattern = "-?\\d*\\.?\\d{0," + editFormat.getMaximumFractionDigits() + "}+";
        setDocument(new FormattedDocument(editPattern, this, displayMultiplier));
        if (currentValue != null) setValue(currentValue);
    }

    public boolean isAllowNegative() {
        return allowNegative;
    }

    public void setAllowNegative(boolean allowNegative) {
        this.allowNegative = allowNegative;
    }

    public boolean isSelectAfterDecimal() {
        return selectAfterDecimal;
    }

    public void setSelectAfterDecimal(boolean selectAfterDecimal) {
        this.selectAfterDecimal = selectAfterDecimal;
    }
    
    public boolean isDisableFocusSelect() {
        return disableFocusSelect;
    }

    public void setDisableFocusSelect(boolean disableFocusSelect) {
        this.disableFocusSelect = disableFocusSelect;
    }

    protected void processFocusEvent(FocusEvent e) 
    {
    	
    	if (isEditable())
    	{
    		
             if (e.getID() == FocusEvent.FOCUS_LOST) {
            	 memory = getCaretPosition();
             }
             
    	     if (!e.isTemporary() && isEditable()) 
             {
                 ((FormattedDocument) getDocument()).reFormat();
             
                 if (e.getID() == FocusEvent.FOCUS_GAINED) {
                 	if (!disableFocusSelect) 
                 	{
                        setSelectionAfterFocusGain();                 		
                 	}
                 	else
                 	{
                 		if (memory > 0)
                 		{
                 			setCaretPosition(memory);	
                 		} 
                 		disableFocusSelect = false;
                 	}            	
                 }
             }
    	}
        super.processFocusEvent(e);
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (!isEditable()) return false;
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    protected void setSelectionAfterFocusGain() {
        String value = getText();
        if (selectAfterDecimal) {
            int indexOfDot = value.indexOf(".");
            if (indexOfDot < 0) {
                setSelectionStart(0);
            } else {
                setSelectionStart(indexOfDot + 1); // Dont select the "."
            }
        } else {
            setSelectionStart(0);
        }
        setSelectionEnd(getText().length());
    }

    public void setValue(Number value) {
        ((FormattedDocument) getDocument()).setValue(value);
    }

    public Number getValue() {
        Document document = getDocument();
        if (document instanceof FormattedDocument) {
            FormattedDocument formattedDocument = (FormattedDocument) document;
            return formattedDocument.getValue();
        }
        return null;
    }

    private void multiplyValue(int multiplier) {
        Number currentValue = getValue();

        if (currentValue != null) {
            if (currentValue instanceof Double) {
                setValue(new Double(currentValue.doubleValue() * multiplier));
            } else if (currentValue instanceof Float) {
                setValue(new Float(currentValue.floatValue() * multiplier));
            } else if (currentValue instanceof Long) {
                setValue(new Long(currentValue.longValue() * multiplier));
            } else if (currentValue instanceof Integer) {
                setValue(new Integer(currentValue.intValue() * multiplier));
            }
        }
    }

    public NumberFormat getDisplayFormat() {
        return displayFormat;
    }

    public void setDisplayFormat(NumberFormat displayFormat) {
        this.displayFormat = displayFormat;
    }

    public NumberFormat getEditFormat() {
        return editFormat;
    }

    public void setEditFormat(NumberFormat editFormat) {
        this.editFormat = editFormat;
        editFormat.setGroupingUsed(false);
    }

    protected static class FormattedDocument extends PlainDocument {
        private Pattern validEditPattern;
        private ArrayList listeners = new ArrayList();
        private Number value = null;
        private DecimalTextField decimalTextField;
        private double displayMultiplier = 1;

        public FormattedDocument(String editPattern, DecimalTextField field, double displayMultiplier) {
            decimalTextField = field;
            this.displayMultiplier = displayMultiplier;
            validEditPattern = Pattern.compile(editPattern);
        }

        public void addDocumentListener(DocumentListener listener) {
            listeners.add(listener);
        }

        public void removeDocumentListener(DocumentListener listener) {
            listeners.remove(listener);
        }

        public void setValue(Number value) {
            this.value = value;

            try {
                int sizeSnapshot = super.getLength();
                super.remove(0, sizeSnapshot);
                fireRemoveUpdate(new DefaultDocumentEvent(0, sizeSnapshot, DocumentEvent.EventType.REMOVE));

                if (value != null && !Double.isNaN(value.doubleValue())) {
                    value = multiplyByDisplayMultiplier(value);
                    String insertString = decimalTextField.isFocusOwner() ? decimalTextField.getEditFormat().format(value) : decimalTextField.getDisplayFormat().format(value);
                    super.insertString(0, insertString, null);
                    fireInsertUpdate(new DefaultDocumentEvent(0, insertString.length(), DocumentEvent.EventType.INSERT));
                }
            } catch (BadLocationException e) {
                log.error("Unexpected error");
            }
        }

        private Number multiplyByDisplayMultiplier(Number value) {
            if ( displayMultiplier != 1) {
                value = value.doubleValue() * displayMultiplier;
            }
            return value;
        }

        public Number getValue() {
            return value;
        }

        public void reFormat() {
            setValue(getValue());
        }

        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if(str != null) {
                str = str.trim();
            }
            StringBuffer buf = new StringBuffer(getText(0, getLength()));
            buf.insert(offs, str);
            String current = buf.toString();

            if (validEditPattern.matcher(current.toString()).matches() && (decimalTextField.isAllowNegative() || !(decimalTextField.isAllowNegative() || current.toString().indexOf('-') > -1))) {
                try {
                    super.insertString(offs, str, a);
                } catch (BadLocationException e) {
                    log.error("Impossible error");
                }

                try {
                    Number value = decimalTextField.getEditFormat().parse(current.toString());
                    divideByDisplayMultiplierAndSetValue(value);
                } catch (ParseException e) {
                    value = null;
                }
            }
        }

        public void remove(int offs, int len) throws BadLocationException {
            try {
                super.remove(offs, len);
            } catch (BadLocationException e) {
                log.error("Impossible error");
            }

            try {
                Number value = decimalTextField.getEditFormat().parse(getText(0, getLength()));
                divideByDisplayMultiplierAndSetValue(value);
            } catch (ParseException e) {
                value = null;
            }
        }

        private void divideByDisplayMultiplierAndSetValue(Number value) {
            if ( displayMultiplier != 1) {
                value = value.doubleValue() / displayMultiplier;
            }
            this.value = value;
        }

        protected void fireInsertUpdate(DocumentEvent e) {
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                DocumentListener listener = (DocumentListener) iterator.next();
                listener.insertUpdate(e);
            }
        }

        protected void fireRemoveUpdate(DocumentEvent e) {
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                DocumentListener listener = (DocumentListener) iterator.next();
                listener.removeUpdate(e);
            }
        }

        public void setDisplayMultiplier(double displayMultiplier) {
            this.displayMultiplier = displayMultiplier;
            setValue(value);
        }
    }

    private class MultiplyAction extends AbstractAction {
        private int multiplyBy;

        public MultiplyAction(int multiplyBy) {
            this.multiplyBy = multiplyBy;
        }

        public void actionPerformed(ActionEvent e) {
            multiplyValue(multiplyBy);
            if (getText().length() > 0) {
                transferFocus();
            }
        }
    }

    private class DecimalTypedAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            replaceSelection(".");
        }
    }


}
