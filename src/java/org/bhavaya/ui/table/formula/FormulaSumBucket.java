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

package org.bhavaya.ui.table.formula;


import org.bhavaya.ui.table.DoubleSumBucket;
import org.bhavaya.ui.table.GroupedRowManager;
import org.bhavaya.ui.table.SortedTableModel;
import org.bhavaya.util.Log;
import org.bhavaya.util.Numeric;

/**
 * ValueBucket for Formula summation
 * User: Jon Moore
 * Date: 27/01/11
 * Time: 17:05
 */
public class FormulaSumBucket implements GroupedRowManager.ValueBucket {
    private GroupedRowManager.ValueBucket defaultDelegate = new GroupedRowManager.UnmergeableDataBucket();
    private GroupedRowManager.ValueBucket doubleDelegate = new DoubleSumBucket();

    public Object getOutput() {
        Object output = doubleDelegate.getOutput();
        return output == null ? getDefaultDelegate().getOutput() : output;
    }

    protected GroupedRowManager.ValueBucket getDefaultDelegate() {
        return defaultDelegate;
    }

    public void insert(Object value) {
        if(value != null) {
            Double d = getDoubleValue(value);
            if(d != null) {
                doubleDelegate.insert(d);
            } else {
                getDefaultDelegate().insert(value);
            }
        }
    }

    private Double getDoubleValue(Object value) {
        Double returnValue = null;
        if(value != null) {
            if(value instanceof FormulaResult) {
                FormulaResult result = (FormulaResult)value;
                if(result.isDouble()) {
                    returnValue = result.doubleValue();
                }
            } else if (value instanceof Number) {
                returnValue = ((Number)value).doubleValue();
            } else if (value instanceof Numeric) {
                returnValue = ((Numeric)value).doubleValue();
            }
        }
        return returnValue;
    }

    public boolean update(Object oldValue, Object newValue) {
        if(oldValue != null) {
            Double oldDouble = getDoubleValue(oldValue);
            if(oldDouble != null) {
                Double newDouble = getDoubleValue(newValue);
                return doubleDelegate.update(oldDouble, newDouble);
            }
            return getDefaultDelegate().update(oldValue, newValue);
        }
        return false;
    }

    public boolean delete(Object value) {
        if(value != null) {
            Double d = getDoubleValue(value);
            if(d != null) {
                return doubleDelegate.delete(d);
            }
            return getDefaultDelegate().delete(value);
        }
        return false;
    }

    public String toString() {
        return "Total";
    }
}
