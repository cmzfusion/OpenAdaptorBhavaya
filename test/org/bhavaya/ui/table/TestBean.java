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

import junit.framework.Assert;
import org.bhavaya.util.Log;

import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
class TestBean extends org.bhavaya.util.DefaultObservable {
    private static final Log log = Log.getCategory(TestBean.class);

    Map propertyToListeners = new HashMap();

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        List listeners = (List) propertyToListeners.get(propertyName);
        if (listeners == null) {
            listeners = new ArrayList();
            propertyToListeners.put(propertyName, listeners);
        }
        listeners.add(propertyChangeListener);

        super.addPropertyChangeListener(propertyName, propertyChangeListener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        List listeners = (List) propertyToListeners.get(propertyName);
        Assert.assertNotNull("asking to remove a listener that has not been added. Property: " + propertyName + " listener: " + propertyChangeListener, listeners);
        boolean containedItem = listeners.remove(propertyChangeListener);
        Assert.assertTrue("asking to remove a listener that has not been added. Property: " + propertyName + " listener: " + propertyChangeListener, containedItem);

        if (log.isDebug())log.debug("Removing listener: " + propertyChangeListener + " from property: " + propertyName + " of bean:" + this);
        super.removePropertyChangeListener(propertyName, propertyChangeListener);
    }

    public List getListeners(String propertyName) {
        return (List) propertyToListeners.get(propertyName);
    }

    public int getAllListenersCount() {
        int count = 0;
        Iterator iter = propertyToListeners.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            count += list.size();
        }
        return count;
    }
}
