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
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class BeanA extends TestBean {
    private BeanB b = new BeanB();
    private double someDouble = Double.MAX_VALUE;
    private String longString = "Long multiline\nstring\nzzzzz";

    public BeanA() {
    }

    public BeanA(BeanB b, double someDouble) {
        this.b = b;
        this.someDouble = someDouble;
    }

    public BeanB getB() {
        return b;
    }

    public void setB(BeanB b) {
        Object oldValue = this.b;
        this.b = b;
        firePropertyChange("b", oldValue, b);
    }

    public int getId() {
        return System.identityHashCode(this);
    }

    public double getSomeDouble() {
        return someDouble;
    }

    public void setSomeDouble(double someDouble) {
        double oldValue = this.someDouble;
        this.someDouble = someDouble;
        firePropertyChange("someDouble", oldValue, someDouble);
    }

    public String getLongString() {
        return longString;
    }

    public void setLongString(String longString) {
        String oldValue = this.longString;
        this.longString = longString;
        firePropertyChange("longString", oldValue, longString);
    }
}
