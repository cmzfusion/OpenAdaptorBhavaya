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

import org.bhavaya.util.Utilities;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.*;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public abstract class ReferenceValueMap implements Map {
    private Map underlyingMap;
    private transient Set entrySet;
    private transient Set keySet;
    private transient Collection values;
    private ReferenceQueue referenceQueue;
    private List referenceRemovedListeners;

    protected static interface KeyedReference {
        public Object getKey();

        public Object get();

        public void clear();
    }

    protected abstract KeyedReference newReference(Object key, Object value, ReferenceQueue queue);

    public ReferenceValueMap() {
        this(new HashMap());
    }

    public ReferenceValueMap(Map underlyingMap) {
        this.underlyingMap = underlyingMap;
        referenceQueue = new ReferenceQueue();
    }

    public void addReferenceRemovedListener(MapListener l) {
        if (referenceRemovedListeners == null) referenceRemovedListeners = new ArrayList();
        referenceRemovedListeners.add(l);
    }

    public void removeReferenceRemovedListener(MapListener l) {
        if (referenceRemovedListeners == null) return;
        referenceRemovedListeners.remove(l);
    }

    private void fireReferenceRemoved(Object key) {
        if (referenceRemovedListeners == null) return;
        MapEvent event = new MapEvent(this, MapEvent.DELETE, key, null);
        Iterator iter = referenceRemovedListeners.iterator();
        while (iter.hasNext()) {
            MapListener listener = (MapListener) iter.next();
            listener.mapChanged(event);
        }
    }

    private void clearReferenceQueue() {
        KeyedReference referenceToRemove = (KeyedReference) referenceQueue.poll();
        int i = 0;
        while (referenceToRemove != null) {
            i++;
            if (i % 25 == 0) Thread.yield();
            Object key = referenceToRemove.getKey();
            underlyingMap.remove(key);
            fireReferenceRemoved(key);
            referenceToRemove = (KeyedReference) referenceQueue.poll();
        }
    }

    public Object put(Object key, Object value) {
        clearReferenceQueue();
        return putInternal(key, value);
    }

    private Object putInternal(Object key, Object value) {
        KeyedReference referenceValue = newReference(key, value, referenceQueue);
        KeyedReference oldReference = (KeyedReference) underlyingMap.put(key, referenceValue);
        if (oldReference != null) {
            Object oldValue = oldReference.get();
            oldReference.clear();
            return oldValue;
        } else {
            return null;
        }
    }

    public Object get(Object key) {
        clearReferenceQueue();
        Reference reference = (Reference) underlyingMap.get(key);
        if (reference == null) return null;
        return reference.get();
    }

    public boolean containsKey(Object o) {
        clearReferenceQueue();
        return underlyingMap.containsKey(o);
    }

    public boolean containsValue(Object value) {
        clearReferenceQueue();
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            Reference reference = (Reference) iterator.next();
            if (reference.get().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public Object remove(Object key) {
        clearReferenceQueue();
        Reference reference = (Reference) underlyingMap.remove(key);
        if (reference != null) {
            return reference.get();
        } else {
            return null;
        }
    }

    public int size() {
        clearReferenceQueue();
        return underlyingMap.size();
    }

    public boolean isEmpty() {
        clearReferenceQueue();
        return underlyingMap.isEmpty();
    }

    public void putAll(Map t) {
        clearReferenceQueue();
        for (Iterator iterator = t.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            putInternal(key, value);
        }
    }

    public void clear() {
        clearReferenceQueue();
        underlyingMap.clear();
        entrySet = null;
        keySet = null;
        values = null;
    }

    public Set keySet() {
        clearReferenceQueue();
        if (keySet == null) {
            keySet = new KeySet();
        }
        return keySet;
    }

    public Collection values() {
        clearReferenceQueue();
        if (values == null) {
            values = new Values();
        }
        return values;
    }

    public Set entrySet() {
        clearReferenceQueue();
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    private class EntrySet extends AbstractSet {
        private Set underlyingEntrySet;

        public EntrySet() {
            super();
            underlyingEntrySet = underlyingMap.entrySet();
        }

        public int size() {
            return underlyingEntrySet.size();
        }

        public Iterator iterator() {
            clearReferenceQueue();
            return new EntryIterator(underlyingEntrySet.iterator());
        }
    }

    private class EntryIterator implements Iterator {
        private Iterator underlyingIterator;

        public EntryIterator(Iterator underlyingIterator) {
            this.underlyingIterator = underlyingIterator;
        }

        public boolean hasNext() {
            return underlyingIterator.hasNext();
        }

        public Object next() {
            Map.Entry nextEntry = (Map.Entry) underlyingIterator.next();
            return new MapEntry(nextEntry);
        }

        public void remove() {
            underlyingIterator.remove();
        }
    }

    private class KeySet extends AbstractSet {
        public Iterator iterator() {
            clearReferenceQueue();
            final Iterator underlyingIterator = underlyingMap.keySet().iterator();
            return new DelegatingIterator(underlyingIterator);
        }

        public int size() {
            return ReferenceValueMap.this.size();
        }

        public boolean contains(Object k) {
            return ReferenceValueMap.this.containsKey(k);
        }
    }

    private class Values extends AbstractCollection {
        public Iterator iterator() {
            clearReferenceQueue();
            return new Iterator() {
                private Iterator i = entrySet().iterator();

                public boolean hasNext() {
                    return i.hasNext();
                }

                public Object next() {
                    return ((Map.Entry) i.next()).getValue();
                }

                public void remove() {
                    i.remove();
                }
            };
        }

        public int size() {
            return ReferenceValueMap.this.size();
        }

        public boolean contains(Object v) {
            return ReferenceValueMap.this.containsValue(v);
        }
    }

    private static class MapEntry implements Map.Entry {
        private Object key;
        private Object value;

        private MapEntry(Map.Entry underlyingMapEntry) {
            this.key = underlyingMapEntry.getKey();
            Reference reference = (Reference) underlyingMapEntry.getValue();
            this.value = reference.get();
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object v) {
            throw new UnsupportedOperationException();
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }

            Map.Entry other = (Map.Entry) o;
            return Utilities.equals(key, other.getKey()) && Utilities.equals(value, other.getValue());
        }

        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }
    }
}
