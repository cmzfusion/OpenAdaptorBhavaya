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

package org.bhavaya.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class SynchronizedAssociation implements Association {
    private Association delegate;

    public SynchronizedAssociation(Association delegate) {
        this.delegate = delegate;
    }

    public synchronized Object put(Object key, Object value) {
        return delegate.put(key, value);
    }

    public synchronized Object remove(Object key) {
        return delegate.remove(key);
    }

    public synchronized int size() {
        return delegate.size();
    }

    public synchronized boolean isEmpty() {
        return delegate.isEmpty();
    }

    public synchronized boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    public synchronized Object get(Object key) {
        return delegate.get(key);
    }

    public synchronized void putAll(Map t) {
        delegate.putAll(t);
    }

    public synchronized void clear() {
        delegate.clear();
    }

    public synchronized boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    public synchronized Object getKeyForValue(Object value) {
        return delegate.getKeyForValue(value);
    }

    public synchronized Set keySet() {
        return delegate.keySet();
    }

    public synchronized Collection values() {
        return delegate.values();
    }

    public synchronized Set entrySet() {
        return delegate.entrySet();
    }
}
