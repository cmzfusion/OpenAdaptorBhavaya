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

import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.ScalableNumber;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.beans.Encoder;
import java.text.NumberFormat;

/**
 * This class formats all double values to have no exponent and to be red or blue depending on the
 * sign of the number.
 *
 * This class is now a mess.
 *
 * @author Philip Milne
 * @version $Revision: 1.10 $
 */
public class DecimalRenderer extends DefaultTableCellRenderer {
    public static int UNSET = Integer.MIN_VALUE;
    public static double AUTO_MULTIPLIER = Double.MIN_VALUE;

    private NumberFormat numberFormat;
    protected int precision = UNSET;
    protected double multiplier = Double.NaN;

    private static final Color HIGHLIGHT_COLOR = new Color(180, 0, 180);
    private Font normalFont;
    private Font italicsFont;
    private Color normalColor = Color.BLACK;

    static {
        BeanUtilities.addPersistenceDelegate(DecimalRenderer.class, new BhavayaPersistenceDelegate(new String[]{"precision"}) {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
            }
        });
    }

    public DecimalRenderer() {
        this(UNSET);
    }

    public DecimalRenderer(int precision) {
        this.precision = precision;
        setHorizontalAlignment(JLabel.RIGHT);

        if (precision != UNSET) {
            applyPrecision(precision, precision);
        }
    }

    public DecimalRenderer(String precisionString) {
        this(Integer.parseInt(precisionString));
    }

    public int getPrecision() {
        return precision;
    }

    protected void applyPrecision(int upper, int lower) {
        getNumberFormat().setMaximumFractionDigits(upper);
        getNumberFormat().setMinimumFractionDigits(lower);
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    protected NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            numberFormat = NumberFormat.getInstance();
        }
        return numberFormat;
    }

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

        Number number;
        int defaultPrecision = 9;
        boolean markAsTainted = false;

        if (value instanceof ScalableNumber) {
            ScalableNumber scalableNumber = (ScalableNumber) value;
            number = new Double(scalableNumber.getScaledAmount());
            if (scalableNumber.getScaling() == ScalableNumber.PERCENTAGE) {
                defaultPrecision = 3;
            } else if (scalableNumber.getScaling() == ScalableNumber.BASIS) {
                defaultPrecision = 1;
            }
        } else if (value instanceof Number) {
            number = (Number) value;
        } else if (value instanceof PartialBucketValue) {
            number = (Number) ((PartialBucketValue) value).getPartialValue();
            markAsTainted = true;
            setFont(italicsFont);
            setForeground(HIGHLIGHT_COLOR);
        } else {
            setText("N/A");
            return;
        }

        if (Double.isNaN(number.doubleValue())) {
            setText("?");
            return;
        }

        if (precision == UNSET) {
            applyPrecision(defaultPrecision, 0);
        }

        String suffix = "";
        if (multiplier == AUTO_MULTIPLIER) {
            double absValue = Math.abs(number.doubleValue());
            if (absValue >= 1000000) {
                number = new Double(number.doubleValue() / 1000000);
                suffix = "m";
            } else if (absValue >= 1000) {
                number = new Double(number.doubleValue() / 1000);
                suffix = "k";
            }
        } else if (!Double.isNaN(multiplier)) {
            number = new Double(number.doubleValue() * multiplier);
        }

        NumberFormat format = getNumberFormat();
        String numberString = format.format(number) + suffix;
        if (markAsTainted) numberString = numberString + "?";
        setText(numberString);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecimalRenderer)) return false;

        final DecimalRenderer decimalRenderer = (DecimalRenderer) o;

        if (precision != decimalRenderer.precision) return false;

        return true;
    }

    public String toString() {
        return precision == UNSET ? "Unconstrained" : "" + precision;
    }

    public int hashCode() {
        return precision;
    }
}
