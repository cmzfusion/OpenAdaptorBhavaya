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

import org.bhavaya.util.Filter;
import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public class FilteredListModel implements ListModel {
    private static final Log log = Log.getCategory(FilteredListModel.class);

    private ListModel source;
    private DefaultListModel delegate;
    private Filter[] filters;
    private SourceListDataListener sourceListDataListener;

    public FilteredListModel(ListModel source) {
        this(source, null);
    }

    public FilteredListModel(ListModel source, Filter[] filters) {
        this.delegate = new DefaultListModel();
        this.source = source;
        sourceListDataListener = new SourceListDataListener();
        setFilters(filters);
    }

    public void setFilters(Filter[] filters) {
        this.filters = filters;
        if (delegate.getListDataListeners().length > 0) filter();
    }

    public void filter() {
        delegate.clear();
        for (int i = 0; i < source.getSize(); i++) {
            Object o = source.getElementAt(i);
            if (evaluate(o)) {
                delegate.addElement(o);
            }
        }
    }

    private boolean evaluate(Object o) {
        if (filters == null) return true;

        for (int j = 0; j < filters.length; j++) {
            Filter filter = filters[j];
            if (!filter.evaluate(o)) return false;
        }
        return true;
    }

    public int getSize() {
        return delegate.getSize();
    }

    public Object getElementAt(int index) {
        return delegate.getElementAt(index);
    }

    public synchronized void addListDataListener(ListDataListener l) {
        if (delegate.getListDataListeners().length == 0) {
            source.addListDataListener(sourceListDataListener);
            filter();
        }
        delegate.addListDataListener(l);
    }

    public synchronized void removeListDataListener(ListDataListener l) {
        delegate.removeListDataListener(l);
        if (delegate.getListDataListeners().length == 0) {
            dispose();
        }
    }

    public synchronized void dispose() {
        if (log.isDebug()) log.debug("Disposing :" + this);
        source.removeListDataListener(sourceListDataListener);
    }

    private class SourceListDataListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
            filter();
        }

        public void intervalRemoved(ListDataEvent e) {
            filter();
        }

        public void contentsChanged(ListDataEvent e) {
            filter();
        }
    }
}