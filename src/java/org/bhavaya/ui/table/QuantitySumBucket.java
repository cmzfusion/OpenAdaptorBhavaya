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
import org.bhavaya.util.Utilities;
import org.bhavaya.util.Log;

/**
 * Created by IntelliJ IDEA.
 * @author Daniel van Enckevort
 * @version $Revision: 1.6 $
 */
public class QuantitySumBucket implements GroupedRowManager.ValueBucket {
    private static final Log log = Log.getCategory(QuantitySumBucket.class);

    private Quantity sum = null;

    public Object getOutput() {
        return sum;
    }

    public void insert(Object value) {
        if (value == null) return;
        if (!(value instanceof Quantity)) {
            throw new IllegalArgumentException("Unexpected object type. insertedValue: " + value);
        }

        if (sum == null) {
            sum = (Quantity) value;
        } else {
            sum = sum.sum((Quantity) value);
        }
    }

    public boolean update(Object oldValue, Object newValue) {
        if (newValue != null && !(newValue instanceof Quantity)) {
            log.error(new IllegalArgumentException("Unexpected object type. newValue: " + newValue));
            return false; // this causes the whole bucket to recalculate, so we don't need to throw the exception above
        }
        if (oldValue != null && !(oldValue instanceof Quantity)) {
            log.error(new IllegalArgumentException("Unexpected object type. oldValue: " + oldValue));
            return false; // this causes the whole bucket to recalculate, so we don't need to throw the exception above
        }

        if (sum == null) {
            sum = (Quantity) newValue;
            return true;
        }

        Quantity oldQuantity = (Quantity) oldValue;
        Quantity newQuantity = (Quantity) newValue;

        if (oldQuantity == null && newQuantity == null) return true;
        if (oldQuantity == null) {
            oldQuantity = new Quantity(0, newQuantity.getUnit());
        } else if (newQuantity == null) {
            newQuantity = new Quantity(0, oldQuantity.getUnit());
        }

        if (Double.isNaN(oldQuantity.getAmount()) || Double.isInfinite(oldQuantity.getAmount())) {
            //we cannot calculate the result for a change from infinity or NaN without knowing the other values.
            //return false to indicate that the bucket needs a recalc.
            return false;
        }
        if (!Utilities.equals(oldQuantity.getUnit(), newQuantity.getUnit())) {
            //unit change. if current sum is Quantity.UNKNOWN, then we don't know how this change would affect the value
            if (sum == Quantity.UNKNOWN) return false;
        }

        Quantity delta = newQuantity.difference(oldQuantity);
        sum = sum.sum(delta);
        return true;
    }

    public boolean delete(Object value) {
        if (sum == null || value == null) {
            return true;
        } else {
            Quantity quantity = (Quantity) value;
            if (Double.isNaN(quantity.getAmount()) || Double.isInfinite(quantity.getAmount())) {
                //we cannot calculate the result for a change from infinity or NaN without knowing the other values.
                //return false to indicate that the bucket needs a recalc.
                return false;
            }
            //if current sum is Quantity.UNKNOWN, then we don't know how this change would affect the value
            if (sum == Quantity.UNKNOWN) return false;

            sum = sum.difference(quantity);
        }
        return true;
    }

    public String toString() {
        return "Total";
    }
}
