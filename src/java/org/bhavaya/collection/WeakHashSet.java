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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
public class WeakHashSet extends AbstractSet {
    private WeakHashMap map;

    private static final Object DUMMY = new Object();

    public WeakHashSet() {
        map = new WeakHashMap();
    }

    public WeakHashSet(Collection c) {
        map = new WeakHashMap(Math.max((int) (c.size() / .75f) + 1, 16));
        addAll(c);
    }

    public WeakHashSet(int initialCapacity, float loadFactor) {
        map = new WeakHashMap(initialCapacity, loadFactor);
    }

    public WeakHashSet(int initialCapacity) {
        map = new WeakHashMap(initialCapacity);
    }

    public Iterator iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean add(Object o) {
        return map.put(o, DUMMY) == null;
    }

    public boolean remove(Object o) {
        return map.remove(o) == DUMMY;
    }

    public void clear() {
        map.clear();
    }
}
