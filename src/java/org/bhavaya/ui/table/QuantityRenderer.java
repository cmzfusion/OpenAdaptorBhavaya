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

package org.bhavaya.ui.table;

import org.bhavaya.util.Quantity;

import java.awt.*;
import java.text.NumberFormat;

/**
 * This class formats all double values to have no exponent and to be red or blue depending on the
 * sign of the number.
 * <p/>
 * This class is now officially a mess
 *
 * @author Philip Milne
 * @version $Revision: 1.13 $
 */
public class QuantityRenderer extends DecimalRenderer {
    private static Color POSITIVE_NUMBER_COLOUR = new Color(0f, 0f, 0.8f);
    private static Color NEGATIVE_NUMBER_COLOUR = new Color(0.8f, 0f, 0f);
    private static final int DEFAULT_PRECISION = 0;

    private Font normalFont;
    private Font italicsFont;

    private static final Color HIGHLIGHT_COLOR = new Color(180, 0, 180);
    private Color normalColor = Color.BLACK;

    public QuantityRenderer() {
        super(UNSET);
    }

    public QuantityRenderer(String precisionString) {
        this(Integer.parseInt(precisionString));
    }

    public QuantityRenderer(int precision) {
        super(precision);
    }

//    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//        if (value == null) return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//        Quantity quantity = (Quantity) value;
//        JLabel component = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//        return component;
//    }

    public void setValue(Object value) {
        if (normalFont == null || normalFont != getFont()) {
            normalFont = getFont();
            italicsFont = normalFont.deriveFont(Font.ITALIC);
        }
        setFont(normalFont);
        setForeground(normalColor);

        if (value == null) {
            setText("");
            return;
        }

        double amount;
        boolean markAsTainted = false;
        if (value instanceof Quantity) {
            Quantity quantity = (Quantity) value;
            amount = quantity.getAmount();
            if (Double.isNaN(amount)) {
                setText("?");
                return;
            } else if (quantity.getUnit().isConversionUnit()) {
                setText("Currency?");
                setFont(italicsFont);
                setForeground(HIGHLIGHT_COLOR);
                return;
            }
            markAsTainted = quantity.isTainted() && !(Double.isNaN(amount) || Double.isInfinite(amount));
            checkPrecision(quantity);
        } else if (value instanceof Number) {
            amount = ((Number) value).doubleValue();
            if (Double.isNaN(amount)) {
                setText("?");
                return;
            }
        } else if (value instanceof PartialBucketValue) {
            amount = ((Number) ((PartialBucketValue) value).getPartialValue()).doubleValue();
            markAsTainted = true;
        } else {
            setText("N/A");
            return;
        }

        String suffix = "";
        if (multiplier == AUTO_MULTIPLIER) {
            double absValue = Math.abs(amount);
            if (absValue >= 1000000) {
                amount /= 1000000;
                suffix = "m";
            } else if (absValue >= 1000) {
                amount /= 1000;
                suffix = "k";
            }
        } else if (!Double.isNaN(multiplier)) {
            amount = amount * multiplier;
        }

        setForeground(amount >= 0.0 ? POSITIVE_NUMBER_COLOUR : NEGATIVE_NUMBER_COLOUR);
        NumberFormat format = getNumberFormat();
        String amountString = format.format(amount) + suffix;
        if (markAsTainted) {
            amountString = amountString + "?";
            setFont(italicsFont);
        }
        setText(amountString);
    }

    protected void checkPrecision(Quantity quantity) {
        if (getPrecision() == UNSET) {
            applyPrecision(DEFAULT_PRECISION, DEFAULT_PRECISION);
        } else {
            // precision does not change
        }
    }
}
