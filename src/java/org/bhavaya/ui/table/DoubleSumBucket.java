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


/**
 * Created by IntelliJ IDEA.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.4 $
 */
public class DoubleSumBucket implements GroupedRowManager.ValueBucket {
    private Object currentOutput = null;    //the actual output. Could be null, one of the possible NaN values, real number or PartialBucketValue
    private double sum = 0;         // sum including NaNs
    private double nonNaNSum = 0;   //keeps track of the actual sum, but we exclude all NaN's (because they are contagious)
                                    //this prevents us doing a full bucket recalc if a number goes to NaN and back again
    private int nanCount = 0;
    private int count = 0;

    public Object getOutput() {
        return currentOutput;
    }

    private void setOutputValue() {
        if (nanCount == 0) {
            currentOutput = new Double(nonNaNSum);
        } else if (count == 1) {
            currentOutput = new Double(sum);
        } else {
            currentOutput = new PartialBucketValue(new Double(nonNaNSum), new Double(sum), count);
        }
    }

    public void insert(Object value) {
        if (value == null) return; //no effect
        insertInternal(value);
        setOutputValue();
    }

    private void insertInternal(Object value) {
        if (value == null) return;  //no effect

        if (value instanceof PartialBucketValue) {
            nanCount += 1;
            PartialBucketValue partialValue = (PartialBucketValue) value;
            sum += ((Double) partialValue.getFullValue()).doubleValue();
            nonNaNSum += ((Double) partialValue.getPartialValue()).doubleValue();
            count += partialValue.getBucketValueCount();
        } else {
            count += 1;
            Double aDouble = (Double) value;
            if (!aDouble.isNaN() && !aDouble.isInfinite()) {
                nonNaNSum += aDouble.doubleValue();
            } else {
                nanCount += 1;
            }

            if (nanCount == 0) {
                sum = nonNaNSum;
            } else {
                sum += ((Double) value).doubleValue();
            }
        }
    }

    public boolean update(Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) return true;
        deleteInternal(oldValue);
        insertInternal(newValue);
        setOutputValue();
        return true;
    }

    public boolean delete(Object value) {
        if (value == null) return true; //no effect
        boolean result = deleteInternal(value);
        setOutputValue();
        return result;
    }

    public boolean deleteInternal(Object value) {
        if (value == null) return true; //no effect

        if (value instanceof PartialBucketValue) {
            nanCount -= 1;
            PartialBucketValue partialValue = (PartialBucketValue) value;
            nonNaNSum -= ((Double) partialValue.getPartialValue()).doubleValue();
            if (nanCount == 0) {
                sum = nonNaNSum;
            } else {
                sum -= ((Double) partialValue.getFullValue()).doubleValue();
            }
            count -= partialValue.getBucketValueCount();
        } else {
            count -= 1;
            Double aDouble = (Double) value;
            if (!aDouble.isNaN() && !aDouble.isInfinite()) {
                nonNaNSum -= aDouble.doubleValue();
            } else {
                nanCount -= 1;
            }

            if (nanCount == 0) {
                sum = nonNaNSum;
            } else {
                sum -= ((Double) value).doubleValue();
            }
        }
        return true;
    }

    public String toString() {
        return "Total";
    }
}
