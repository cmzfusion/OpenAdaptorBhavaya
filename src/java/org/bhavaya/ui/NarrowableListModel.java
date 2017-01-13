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

import org.bhavaya.collection.IndexedSet;
import org.bhavaya.util.Log;
import org.bhavaya.util.StringRenderer;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This is the ListModel that accompanies the NarrowableComboBox.  It can contain any objects and narrows down
 * by finding subsets of data within a sorted list.  Note that this is also used elsewhere to create narrowing
 * JLists and even narrow JTables (by using ListModelTableModels).
 *
 * @author Mr. Daniel "The Baron" Van Enckevort
 * @author Mr. Brendon "Gui King" McLean
 * @author starring 'Lucy Love' in her first speaking role.
 * @version $Revision: 1.5.4.2 $
 */
public class NarrowableListModel extends AbstractListModel implements INarrowableListModel {
    private static final Log log = Log.getCategory(NarrowableListModel.class);


    // An alphabetically sorted list of all possible values
    private List dataList;

    // Used to render arbitrary objects to strings.
    private StringRenderer objectRenderer = StringRenderer.TO_STRING_RENDERER;

    // A string used to filter the contents of dataList.
    private String lastNarrowText = "";

    // The first element in the list that matches lastNarrowText
    private int viewStart = 0;

    // The last element in the list that matches lastNarrowText. Inclusive so -1 indicates no matches
    private int viewEnd = -1;

    // If data has been added, but there's been no narrow command, this model is invalid
    private boolean valid = false;

    // Comparator used to sort and narrow the lists.  Uses the object renderer if set, otherwise straight string comparision
    protected Comparator OBJECT_TO_STRING_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(objectRenderer.render(o1), o2.toString());
        }
    };

    // Comparator used to sort and narrow the lists.  Uses the object renderer if set, otherwise straight string comparision
    protected Comparator OBJECT_TO_OBJECT_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(objectRenderer.render(o1), objectRenderer.render(o2));
        }
    };
    private boolean emptyStringIsAllData;


    public NarrowableListModel() {
        this(false);
    }

    public NarrowableListModel(boolean emptyStringIsAllData) {
        this(null, emptyStringIsAllData);
    }

    public NarrowableListModel(Collection data) {
        this(data, false);
    }

    public NarrowableListModel(Collection data, boolean emptyStringIsAllData) {
        this.emptyStringIsAllData = emptyStringIsAllData;
        if (data != null) {
            dataList = new IndexedSet(data.size());
            addData(data);
        } else {
            dataList = new IndexedSet();
        }
        narrow("", !emptyStringIsAllData); // if loading all load them on the same thread
    }

    public void addData(Collection newData) {
        dataList.addAll(newData);
        Utilities.sort(dataList, OBJECT_TO_OBJECT_COMPARATOR);
        valid = false;
    }

    public void clear() {
        dataList.clear();
        valid = false;
    }

    public void setRenderer(StringRenderer objectRenderer) {
        this.objectRenderer = objectRenderer;
        Utilities.sort(dataList, OBJECT_TO_OBJECT_COMPARATOR);
        narrow();
    }

    protected StringRenderer getRenderer() {
        return objectRenderer;
    }

    public synchronized void narrow(String narrowText) {
        narrow(narrowText, true);
    }

    public synchronized void narrow(String narrowText, boolean scheduleLoad) {
        doNarrow(narrowText);
    }

    public synchronized boolean isLoadingData() {
        return false;
    }

    protected final void doNarrow(String narrowText) {
        setLastNarrowText(narrowText);
        if (log.isDebug()) log.debug("doing narrow for: " + narrowText);
        if (narrowText.length() == 0) {
            int end = emptyStringIsAllData ? dataList.size() - 1 : -1;
            setVisibleRange(0, end);
        } else {
            int firstIndex = getFirstMatch(narrowText);
            int lastIndex = getLastMatchFor(narrowText);

            setVisibleRange(firstIndex, lastIndex);
        }
        if (log.isDebug()) log.debug("done narrow for: " + narrowText);
    }

    protected synchronized void setLastNarrowText(String lastNarrowText) {
        assert lastNarrowText != null;
        this.lastNarrowText = lastNarrowText;
    }

    protected synchronized String getLastNarrowText() {
        return lastNarrowText;
    }

    protected void narrow() {
        doNarrow(getLastNarrowText());
    }

    protected boolean isEmptyStringIsAllData() {
        return emptyStringIsAllData;
    }

    /**
     * returns the current view size (i.e. the number of elements that should be displayed)
     */
    public int getSize() {
        return viewEnd - viewStart + 1;
    }

    public Object getElementAt(int index) {
        throwExceptionIfInvalid();

        int modelIndex = index + viewStart;
        if (index >= 0 && modelIndex < dataList.size()) {
            return dataList.get(index + viewStart);
        } else {
            return null;
        }
    }

    private void throwExceptionIfInvalid() {
        if (!valid) {
            throw new IllegalStateException("No narrow has been called after listData update");
        }
    }

    /**
     * set the visible interval to be between start and end (inclusive)
     * e.g. start = 0, end = -1 (show no items)
     *      start = 0, end = 0 (show item 0)
     *      start = 0, end = size-1 (show all items)
     *
     * @param start is the index of the first item to show
     * @param end is the index of the last item to show
     */
    private void setVisibleRange(int start, int end) {
        int oldViewStart = viewStart;
        int oldViewEnd = viewEnd;

        viewStart = start;
        viewEnd = end;
        valid = true;

        fireContentsChangedSwingHack(this, 0, oldViewEnd - oldViewStart);
    }

    /**
     * This is the solution to a very bad bug in the Swing look and feel.  The UI classes are responsible for
     * working out sizes, but they only do this on a contents changed event.  The problem with this is that the
     * other listeners may need the preferred size before the UI gets it's event.  This method circumvents the
     * problem hunting through the listeners for the UI class and calling it first before passing the event
     * on to other listeners.
     */
    private void fireContentsChangedSwingHack(Object source, int index0, int index1) {
        ListDataListener[] listeners = getListDataListeners();
        for (int i = 0; i < listeners.length; i++) {
            ListDataListener listener = listeners[i];
            if (listener.getClass().getPackage().getName().equals("javax.swing.plaf.basic")) {
                listener.contentsChanged(new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1));
            }
        }
        super.fireContentsChanged(source, index0, index1);
    }

    public Object getFirstMatchingObject(String str) {
        int index = getFirstMatch(str);
        if (index >= 0 && index < dataList.size()) {
            return dataList.get(index);
        } else {
            return null;
        }
    }

    /**
     * @return The index of the first match (or closest match) in the model to the given string.
     */
    private int getFirstMatch(String str) {
        return getFirstMatch(dataList, str);
    }

    protected int getFirstMatch(List data, String str) {
        int index = Collections.binarySearch(data, str, OBJECT_TO_STRING_COMPARATOR);

        if (index < 0) {
            // An exact match was not found, so this is represents the closest match
            index = Math.abs(index + 1);
        } else {
            // Found a match, make sure we return the first such match
            while (index >= 0 && objectRenderer.render(data.get(index)).equalsIgnoreCase(str)) {
                index--;
            }

            // index is now at first entry before the match pos, so increment it.
            index++;
        }

        return index;
    }

    /**
     * @return The last match in the model for the given string.
     */
    private int getLastMatchFor(String str) {
        return getLastMatchFor(dataList, str);
    }

    protected int getLastMatchFor(List data, String str) {
        StringBuffer firstNonMatchStrBuf = new StringBuffer(str.substring(0, str.length() - 1));
        char lastLetter = str.charAt(str.length() - 1);
        lastLetter = Character.toLowerCase(lastLetter);
        lastLetter++;

        firstNonMatchStrBuf.append(lastLetter);

        // Use the getFirstMatch to find the closest match to a string with its last char incremented by one
        int firstNonMatchIdx = getFirstMatch(data, firstNonMatchStrBuf.toString());

        return firstNonMatchIdx - 1;
    }

    public void addListDataListener(ListDataListener l) {
        super.addListDataListener(l);
    }

    public void removeListDataListener(ListDataListener l) {
        super.removeListDataListener(l);
    }
}