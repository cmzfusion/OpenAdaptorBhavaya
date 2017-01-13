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

package org.bhavaya.util;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class ScalableNumber implements Comparable, Numeric {
    public static final double INVERSE_PERCENTAGE = 0.01;
    public static final int PERCENTAGE = 100;
    public static final int BASIS = 10000;

    double amount;
    double scaling;

    static {
        BeanUtilities.addPersistenceDelegate(ScalableNumber.class, new BhavayaPersistenceDelegate(new String[]{"amount", "scaling"}));
    }

    /**
     * this is to cater for the case where they have already scaled the number on the database.
     */
    public ScalableNumber(double scaledAmount, double scaling, boolean preScaled) {
        this(preScaled ? (scaledAmount / scaling) : scaledAmount, scaling);
    }

    public ScalableNumber(double scaledAmount, int scaling, boolean preScaled) {
        this(preScaled ? (scaledAmount / scaling) : scaledAmount, scaling);
    }

    public ScalableNumber(double amount, double scaling) {
        this.amount = amount;
        this.scaling = scaling;
    }

    public ScalableNumber(double amount, int scaling) {
        this.amount = amount;
        this.scaling = scaling;
    }

    public double getAmount() {
        return amount;
    }

    public double getScaling() {
        return scaling;
    }

    public double getScaledAmount() {
        return amount * scaling;
    }

    public int compareTo(Object other) {
        // Throw on comparing to wrong type.
        ScalableNumber otherNumber = (ScalableNumber) other;
        return Double.compare(amount, otherNumber.amount);
    }

    public String toString() {
        return (amount * scaling) + "%";
    }

    public double doubleValue() {
        return getScaledAmount();
    }
}
