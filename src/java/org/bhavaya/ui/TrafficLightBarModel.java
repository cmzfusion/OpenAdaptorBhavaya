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

import org.bhavaya.ui.view.View;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.TrafficLightModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * @author Daniel van Enckevort
 * @version $Revision: 1.3 $
 */
public class TrafficLightBarModel {
    private Set trafficLights;
    private ArrayList listeners = new ArrayList();


    public TrafficLightBarModel() {
        this.trafficLights = new LinkedHashSet();
        // Todo there is a problem with calculating the preferred size of the ReorderableGroupToolBar which results in the
        // bounds not being set correctly and revalidation of the toolbar not happening. This listener will force the
        // Toolbar ro be rebuilt for the current view (and any floating windows) which is a bit heavy handed but will only happen rarely
        addListener(new Listener() {
            public void trafficLightAdded(TrafficLightModel model) {
                Workspace.getInstance().forceUpdate();
            }
            public void trafficLightRemoved(TrafficLightModel model) {
                Workspace.getInstance().forceUpdate();
            }
        });
    }

    protected Set getTrafficLights() {
        return trafficLights;
    }


    public void addTrafficLightModel(TrafficLightModel model) {
        if (trafficLights.add(model)) {
            fireLightAdded(model);
        }
    }

    public void removeTrafficLightModel(TrafficLightModel model) {
        if (trafficLights.remove(model)) {
            fireLightRemoved(model);
        }
    }

    public void addListener(Listener modelListener) {
        listeners.add(modelListener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    protected void fireLightAdded(TrafficLightModel model) {
        for (int i=0; i<listeners.size(); i++) {
            ((Listener) listeners.get(i)).trafficLightAdded(model);
        }
    }

    protected void fireLightRemoved(TrafficLightModel model) {
        for (int i=0; i<listeners.size(); i++) {
            ((Listener) listeners.get(i)).trafficLightRemoved(model);
        }
    }

    public interface Listener {
        public void trafficLightAdded(TrafficLightModel model);
        public void trafficLightRemoved(TrafficLightModel model);
    }
}
