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

package org.bhavaya.beans;

import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.LoadClosure;
import org.bhavaya.util.BhavayaPersistenceDelegate;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.10 $
 */
public class BeanFactoryLoad implements LoadClosure {
    protected Object key;
    protected BeanFactory beanFactory;
    protected String index;
    protected LoadGroup loadGroup;
    protected ThreadLocal propertyValueForThread;

    static {
        BeanUtilities.addPersistenceDelegate(BeanFactoryLoad.class, new BhavayaPersistenceDelegate(new String[]{"beanFactory", "key", "index"}));
    }

    public BeanFactoryLoad(Class beanType, Object key) {
        this(BeanFactory.getInstance(beanType), key, null);
    }

    public BeanFactoryLoad(Class beanType, Object key, String index) {
        this(BeanFactory.getInstance(beanType), key, index, null);
    }

    public BeanFactoryLoad(BeanFactory beanFactory, Object key, String index) {
        this(beanFactory, key, index, null);
    }

    public BeanFactoryLoad(BeanFactory beanFactory, Object key, String index, LoadGroup loadGroup) {
        this.key = key;
        this.beanFactory = beanFactory;
        this.index = index;
        this.loadGroup = loadGroup;
        if (loadGroup != null) loadGroup.add(this);
        propertyValueForThread = new ThreadLocal();
    }

    public final Object load() {
        Object propertyValue = propertyValueForThread.get();
        if (propertyValue != null) return propertyValue;

        /* I could have attempt synchronisation here, but I'm worried that might cause more pile-ups than a traffic
           roundabout in New York.  Also, the presence of Threadlocals in this class leads me to assume that Parwy was
           taking care to avoid synchronisation.  Take a snapshot here.  Worst case (I think) is that we load the data
           more than once.
        */
        final LoadGroup loadGroup = this.loadGroup;

        // each thread will go through this code, but we dont care, better than introducing locks which reduce concurrency and introduce potential deadlocks
        if (loadGroup == null || loadGroup.isLoaded()) {
            propertyValue = set();
        } else {
            // BeanFactory.pushBeanFactoryLoadStack will keep all references strong until all loads in a loadGroup are inflated
            // it is important to iterate over all of them if using WEAK/SOFT references in the BeanFactory,
            BeanFactory.pushBeanFactoryLoadStack(beanFactory);
            try {
                propertyValue = set();
                loadGroup.setLoaded();
            } finally {
                BeanFactory.popBeanFactoryLoadStack(beanFactory);
            }
        }
        return propertyValue;
    }

    protected void reset() {
        loadGroup = null;
    }

    protected Object get() {
        return beanFactory.get(key, index);
    }

    private Object set() {
        Object propertyValue = get();
        if (propertyValue == null) propertyValue = beanFactory.getLazyNull();
        propertyValueForThread.set(propertyValue);
        return propertyValue;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BeanFactoryLoad)) return false;

        final BeanFactoryLoad load = (BeanFactoryLoad) o;

        if (!beanFactory.equals(load.beanFactory)) return false;
        if (index != null ? !index.equals(load.index) : load.index != null) return false;
        if (!key.equals(load.key)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = key.hashCode();
        result = 29 * result + beanFactory.hashCode();
        result = 29 * result + (index != null ? index.hashCode() : 0);
        return result;
    }

    public String toString() {
        return beanFactory + "/" + index + "/" + key;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public Object getKey() {
        return key;
    }

    public String getIndex() {
        return index;
    }

}