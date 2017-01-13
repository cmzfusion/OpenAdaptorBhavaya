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
public class BeanD extends TestBean {
    private static int counter = 0;

    private Class someClass = BeanD.class;
    private String someString = "default D String";
    private int i = counter++; // by default unique int
    private double aDouble = 0;

    public BeanD() {
    }

    public BeanD(String someString, int i) {
        this.someString = someString;
        this.i = i;
    }

    public Class getSomeClass() {
        return someClass;
    }

    public void setSomeClass(Class someClass) {
        Object oldValue = this.someClass;
        this.someClass = someClass;
        firePropertyChange("someClass", oldValue, someClass);
    }

    public double getADouble() {
        return aDouble;
    }

    public void setADouble(double aDouble) {
        double oldValue = this.aDouble;
        this.aDouble = aDouble;
        firePropertyChange("aDouble", oldValue, aDouble);
    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        Object oldValue = this.someString;
        this.someString = someString;
        firePropertyChange("someString", oldValue, someString);
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        int oldValue = this.i;
        this.i = i;
        firePropertyChange("i", oldValue, i);
        firePropertyChange("index", oldValue, i);
    }

    public int getIndex() {
        return i;
    }
}
