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
public class BeanC extends TestBean {
    public static final String DEFAULT_STRING = "default C string";

    private BeanB b;
    private BeanD d = new BeanD();
    private String someString = DEFAULT_STRING;
    private double someDouble = 0;

    public BeanC() {
    }

    public BeanC(BeanD d, String someString) {
        this.d = d;
        this.someString = someString;
    }

    public BeanD getD() {
        return d;
    }

    public void setD(BeanD d) {
        Object oldValue = this.d;
        this.d = d;
        firePropertyChange("d", oldValue, d);
    }

    public BeanB getB() {
        return b;
    }

    public void setB(BeanB b) {
        Object oldValue = this.b;
        this.b = b;
        firePropertyChange("b", oldValue, b);
    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        Object oldValue = this.someString;
        this.someString = someString;
        firePropertyChange("someString", oldValue, someString);
    }

    public double getSomeDouble() {
        return someDouble;
    }

    public void setSomeDouble(double aDouble) {
        double oldValue = this.someDouble;
        this.someDouble = aDouble;
        firePropertyChange("someDouble", oldValue, aDouble);
    }

}
