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

package org.bhavaya.beans.criterion;

import org.bhavaya.util.Describeable;
import org.bhavaya.util.Filter;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */

public interface Criterion extends Filter, Describeable {
    static final String CRITERION_XML_FILE = "criterion.xml";
    public static final String OPERATOR_EQUALS = "=";
    public static final Criterion[] ALL_CRITERION = new Criterion[0];

    /**
     * @return the "user readable" name of this criterion instance (e.g. "Currency euro"
     */
    public String getName();

    /**
     * @param bean
     * @return true if the given bean passes the criterion
     */
    public boolean evaluate(Object bean);

    /**
     * @return a string representing the value a given bean is evaulated against (used for display purposes) e.g. "10-04-2020"
     */
    public String getDescription();


    /**
     * @return whether this criterion can be used for the given class
     */
    public boolean isValidForBeanType(Class beanType);
}
