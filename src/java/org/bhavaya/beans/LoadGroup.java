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

import org.bhavaya.util.LoadClosure;

import java.util.ArrayList;
import java.util.List;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class LoadGroup {
    private List loads;
    private boolean loaded;

    public LoadGroup() {
        loads = new ArrayList();
        loaded = false;
    }

    public void add(LoadClosure load) {
        loads.add(load);
    }

    public void setLoaded() {
        synchronized (this) { //syncing on this is important as a Load may sync on its loadGroup
            if (loaded) return;
            loaded = true;
        }

        // do not lock this or loads while iterating to avoid deadlocks
        for (int i = 0; i < loads.size(); i++) {
            LoadClosure load = (LoadClosure) loads.get(i);
            load.load();
        }
    }

    public boolean isLoaded() {
        synchronized (this) {
            return loaded;
        }
    }
}
