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

import java.util.*;

/**
 * No check is currently made in put() that the values are unique.
 * The association class extends the LinkedHashMap, so iterators return elements
 * in the order they were added to the Association.
 *
 * @author
 * @version $Revision: 1.3 $
 */
public class DefaultAssociation implements Association {
    private Map valueToKey;
    private Map keyToValue;
    private transient Set entrySet;
    private transient Set keySet;
    private transient Collection values;

    public DefaultAssociation() {
        this(new HashMap(), new HashMap());
    }

    public DefaultAssociation(int initialSize) {
        this(new HashMap(initialSize), new HashMap(initialSize));
    }

    public DefaultAssociation(Map keyToValue) {
        this(keyToValue, new HashMap());
    }

    protected DefaultAssociation(Map keyToValue, Map valueToKey) {
        this.keyToValue = keyToValue;
        this.valueToKey = valueToKey;
    }

    public Object put(Object key, Object value) {
        Object oldValue = keyToValue.put(key, value);
        if (oldValue != null) valueToKey.remove(oldValue);
        valueToKey.put(value, key);
        return oldValue;
    }

    public Object remove(Object key) {
        Object value = keyToValue.get(key);
        valueToKey.remove(value);
        return keyToValue.remove(key);
    }

    public int size() {
        return keyToValue.size();
    }

    public boolean isEmpty() {
        return keyToValue.isEmpty();
    }

    public boolean containsKey(Object key) {
        return keyToValue.containsKey(key);
    }

    public Object get(Object key) {
        return keyToValue.get(key);
    }

    public void putAll(Map t) {
        for (Iterator iterator = t.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            put(key, value);
        }
    }

    public void clear() {
        valueToKey.clear();
        keyToValue.clear();
    }

    public boolean containsValue(Object value) {
        // This is more efficient.
        return valueToKey.containsKey(value);
    }

    public Object getKeyForValue(Object value) {
        return valueToKey.get(value);
    }

    public Object clone() {
        throw new UnsupportedOperationException();
    }

    public Set keySet() {
        if (keySet == null) {
            keySet = new KeySet();
        }
        return keySet;
    }

    public Collection values() {
        if (values == null) {
            values = new Values();
        }
        return values;
    }

    public Set entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    private class EntrySet extends AbstractSet {
        public EntrySet() {
            super();
        }

        public int size() {
            return keyToValue.size();
        }

        public Iterator iterator() {
            final Iterator underlyingIterator = keyToValue.entrySet().iterator();
            return new DelegatingIterator(underlyingIterator);
        }
    }

    private class KeySet extends AbstractSet {
        public Iterator iterator() {
            final Iterator underlyingIterator = keyToValue.keySet().iterator();
            return new DelegatingIterator(underlyingIterator);
        }

        public int size() {
            return DefaultAssociation.this.size();
        }

        public boolean contains(Object k) {
            return DefaultAssociation.this.containsKey(k);
        }
    }

    private class Values extends AbstractCollection {
        public Iterator iterator() {
            final Iterator underlyingIterator = keyToValue.values().iterator();
            return new DelegatingIterator(underlyingIterator);
        }

        public int size() {
            return DefaultAssociation.this.size();
        }

        public boolean contains(Object v) {
            return DefaultAssociation.this.containsValue(v);
        }
    }

    public static void main(String[] args) {
        Association m = new DefaultAssociation(new LinkedHashMap());
        m.put("foo", "bar");
        m.put("god", "aeker");
        m.put("frabe", "normington");
        System.out.println(m.get("foo"));
        System.out.println(m.getKeyForValue("aeker"));

        for (Iterator iterator = m.values().iterator(); iterator.hasNext();) {
            Object o = (Object) iterator.next();
            System.out.println("o = " + o);
        }
    }
}
