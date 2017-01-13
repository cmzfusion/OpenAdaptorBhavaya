package org.bhavaya.ui;

import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

/**
 * A plain document with a configured max length. Used to limit characters in text components like JTextField. 
 * User: dhayatd
 * Date: 10-Aug-2009
 * Time: 14:29:19
 */
public class LimitedLengthPlainDocument extends PlainDocument {
    private int maxLength;

    public LimitedLengthPlainDocument(int maxLength) {
        super();
        this.maxLength = maxLength;
    }

    public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
        if (str == null) return;
        if ((getLength() + str.length()) <= maxLength) {
            super.insertString(offset, str, attr);
        }
    }
}
