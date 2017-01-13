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

import org.bhavaya.collection.SingleItemSet;
import org.bhavaya.util.NullSourcePropertyChangeEvent;

import java.util.Set;

/**
 * encapsulates a value change for a given property path.
 * This change holds for every object in the set of "roots"
 */
public class PathPropertyChangeEvent extends NullSourcePropertyChangeEvent {
    private Object newValue;
    private Set roots;
    private String[] pathFromRoot;

    public PathPropertyChangeEvent(Set roots, Object source, String[] parentPathFromRoot, Object oldValue) {
        super(source, parentPathFromRoot[parentPathFromRoot.length-1], oldValue, null);
        this.roots = roots;
        this.pathFromRoot = parentPathFromRoot;
    }

    protected void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Set getRoots() {
        return roots;
    }

    /**
     * really only expecting CachedObjectGraph to use this
     * @param additionalRoots
     */
    protected void addRoots(Set additionalRoots) {
        roots = SingleItemSet.mergeSets(roots, additionalRoots);
    }

    public String[] getPathFromRoot() {
        return pathFromRoot;
    }
}
