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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Description
 *
 * @author
 * @version $Revision: 1.3 $
 */
public abstract class AuditBeanCollection<E> extends DefaultBeanCollection<E> {

    public AuditBeanCollection(Class<E> type) {
        super(type);
    }

    protected abstract void writeAccess();

    protected abstract void readAccess();

    protected boolean add(E value, boolean fireCommit, boolean fireCollectionChanged) {
        writeAccess();
        return super.add(value, fireCommit, fireCollectionChanged);
    }

    protected void add(int index, E value, boolean fireCommit, boolean fireCollectionChanged) {
        writeAccess();
        super.add(index, value, fireCommit, fireCollectionChanged);
    }

    protected boolean addAll(Collection<? extends E> c, boolean fireCommit, boolean fireCollectionChanged) {
        writeAccess();
        return super.addAll(c, fireCommit, fireCollectionChanged);
    }

    protected boolean remove(Object value, boolean fireCommit, boolean fireCollectionChanged) {
        writeAccess();
        return super.remove(value, fireCommit, fireCollectionChanged);
    }

    protected boolean removeAll(Collection<?> c, boolean fireCommit, boolean fireCollectionChanged) {
        writeAccess();
        return super.removeAll(c, fireCommit, fireCollectionChanged);
    }

    protected void clear(boolean fireCommit, boolean fireCollectionChanged) {
        writeAccess();
        super.clear(fireCommit, fireCollectionChanged);
    }

    public boolean retainAll(Collection<?> c, boolean fireCommit) {
        writeAccess();
        return super.retainAll(c, fireCommit);
    }

    public int size() {
        readAccess();
        return super.size();
    }

    public boolean isEmpty() {
        readAccess();
        return super.isEmpty();
    }

    public boolean contains(Object o) {
        readAccess();
        return super.contains(o);
    }

    public Iterator<E> iterator() {
        readAccess();
        return super.iterator();
    }

    public Object[] toArray() {
        readAccess();
        return super.toArray();
    }

    public <T> T[] toArray(T a[]) {
        readAccess();
        return super.toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        readAccess();
        return super.containsAll(c);
    }

    public E get(int index) {
        readAccess();
        return super.get(index);
    }

    public int indexOf(Object o) {
        readAccess();
        return super.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        readAccess();
        return super.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        readAccess();
        return super.listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        readAccess();
        return super.listIterator(index);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        readAccess();
        return super.subList(fromIndex, toIndex);
    }

    public void addCollectionListener(CollectionListener<E> l) {
        readAccess();
        super.addCollectionListener(l);
    }
}
