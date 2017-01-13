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

package org.bhavaya.ui;

import javax.swing.*;
import java.util.*;


/**
 * Description
 *
 * @author
 * @version $Revision: 1.3.66.1 $
 */
public class ArrayListModel extends AbstractListModel implements java.util.List, RandomAccess {
    public java.util.List delegate;

    public ArrayListModel(List delegate) {
        this.delegate = delegate;
    }

    public ArrayListModel() {
        this.delegate = new ArrayList();
    }

    public boolean add(Object o) {
        boolean b = delegate.add(o);
        fireUpdate();
        return b;
    }

    public java.util.List getList() {
        return delegate;
    }

    public Object remove(int index) {
        Object o = delegate.remove(index);
        fireUpdate();
        return o;
    }

    /**
     * Remove a number of items from the end of the list as a single transaction
     * Try to use the more efficient removeLast if the delegate is a LinkedList
     *
     * This is a workaround for problems with the current diagnostics logging implementation
     */
    public void removeFromEnd(int numberOfItemsToRemove) {
        LinkedList l = delegate instanceof LinkedList ? (LinkedList)delegate : null;
        for ( int loop=0; loop < numberOfItemsToRemove; loop++) {
            if ( l != null) {
                l.removeLast();
            } else {
                delegate.remove(delegate.size() - 1);
            }
        }
        fireUpdate();
    }

    public Object set(int index, Object o) {
        Object r = delegate.set(index, o);
        fireUpdate();
        return r;
    }

    public Object getElementAt(int index) {
        return get(index);
    }

    public int getSize() {
        return size();
    }

    private void fireUpdate() {
        fireContentsChanged(this, -1, Integer.MAX_VALUE);
    }

    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public void add(int index, Object element) {
        delegate.add(index, element);
        fireUpdate();
    }

    public boolean addAll(Collection c) {
        boolean b = delegate.addAll(c);
        fireUpdate();
        return b;
    }

    public boolean addAll(int index, Collection c) {
        boolean b = delegate.addAll(index, c);
        fireUpdate();
        return b;
    }

    public void clear() {
        delegate.clear();
        fireUpdate();
    }

    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    public boolean containsAll(Collection c) {
        return delegate.containsAll(c);
    }

    public Object get(int index) {
        return delegate.get(index);
    }

    public Iterator iterator() {
        return delegate.iterator();
    }

    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return delegate.listIterator();
    }

    public ListIterator listIterator(int index) {
        return delegate.listIterator(index);
    }

    public boolean remove(Object o) {
        boolean b = delegate.remove(o);
        fireUpdate();
        return b;
    }

    public boolean removeAll(Collection c) {
        boolean b = delegate.removeAll(c);
        fireUpdate();
        return b;
    }

    public boolean retainAll(Collection c) {
        boolean b = delegate.retainAll(c);
        fireUpdate();
        return b;
    }

    public int size() {
        return delegate.size();
    }

    public List subList(int fromIndex, int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return delegate.toArray();
    }

    public Object[] toArray(Object a[]) {
        return delegate.toArray(a);
    }
}
