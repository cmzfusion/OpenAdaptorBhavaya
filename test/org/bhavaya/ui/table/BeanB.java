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

import java.util.Date;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class BeanB extends TestBean {
    private BeanC c = new BeanC();
    private BeanB b = this;
    private boolean someBoolean = false;
    private Date date;

    public BeanB() {
    }

    public BeanB(BeanC c, boolean someBoolean) {
        this.c = c;
        this.someBoolean = someBoolean;
    }

    public BeanC getC() {
        return c;
    }

    public void setC(BeanC c) {
        Object oldValue = this.c;
        this.c = c;
        firePropertyChange("c", oldValue, c);
    }

    public BeanB getB() {
        return b;
    }

    public void setB(BeanB b) {
        Object oldValue = this.b;
        this.b = b;
        firePropertyChange("b", oldValue, b);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        Object oldValue = this.date;
        this.date = date;
        firePropertyChange("date", oldValue, date);
    }

    public boolean isSomeBoolean() {
        return someBoolean;
    }

    public void setSomeBoolean(boolean someBoolean) {
        boolean oldValue = this.someBoolean;
        this.someBoolean = someBoolean;
        firePropertyChange("someBoolean", oldValue, someBoolean);
    }
}
