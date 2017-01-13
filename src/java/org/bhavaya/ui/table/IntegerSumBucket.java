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
 * @author Daniel van Enckevort
 * @version $Revision: 1.2 $
 */
public class IntegerSumBucket implements GroupedRowManager.ValueBucket {
    private static final Integer INTEGER_ZERO = new Integer(0);
    private Integer sum = null;

    public Object getOutput() {
        return sum;
    }

    public void insert(Object value) {
        if (value == null) return;

        if (sum == null) {
            sum = (Integer) value;
        } else {
            sum = new Integer(sum.intValue() + ((Integer) value).intValue());
        }
    }

    public boolean update(Object oldValue, Object newValue) {
        if (sum == null) {
            sum = (Integer) newValue;
            return true;
        }

        Number oldNumber = (Number) oldValue;
        Number newNumber = (Number) newValue;

        if (oldNumber == null && newNumber == null) return true;
        if (oldNumber == null) {
            oldNumber = INTEGER_ZERO;
        } else if (newNumber == null) {
            newNumber = INTEGER_ZERO;
        }
        int newSum = sum.intValue() + (newNumber.intValue() - oldNumber.intValue());
        sum = new Integer(newSum);
        return true;
    }

    public boolean delete(Object value) {
        if (sum == null || value == null) {
            return true;
        } else {
            sum = new Integer(sum.intValue() - ((Integer) value).intValue());
        }
        return true;
    }

    public String toString() {
        return "Total";
    }
}
