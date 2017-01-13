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

import org.bhavaya.util.TrafficLightModel;

import javax.swing.*;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class TrafficLightBar extends JToolBar {
    private TrafficLightBarModel model;
    private TrafficLightBarModel.Listener modelListener;


    public TrafficLightBar(TrafficLightBarModel model) {
        super(JToolBar.HORIZONTAL);
        setOpaque(false);
        setFloatable(false);

        modelListener = new ModelListener(this);
        setModel(model);
    }

    public void setModel(TrafficLightBarModel newModel) {
        TrafficLightBarModel oldModel = this.model;

        TrafficLightBarModel.Listener modelListener = getModelListener();
        if (oldModel != null){
            oldModel.removeListener(modelListener);
            for (Iterator iterator = oldModel.getTrafficLights().iterator(); iterator.hasNext();) {
                TrafficLightModel lightModel = (TrafficLightModel) iterator.next();
                modelListener.trafficLightRemoved(lightModel);
            }
        }
        this.model = newModel;
        if (newModel != null) {
            for (Iterator iterator = newModel.getTrafficLights().iterator(); iterator.hasNext();) {
                TrafficLightModel lightModel = (TrafficLightModel) iterator.next();
                modelListener.trafficLightAdded(lightModel);
            }
            newModel.addListener(modelListener);
        }
    }

    public TrafficLightBarModel getModel() {
        return model;
    }

    protected TrafficLightBarModel.Listener getModelListener() {
        return modelListener;
    }

    protected void addTrafficLight(TrafficLight trafficLight) {
        add(trafficLight);
        revalidate();
        repaint();
    }

    protected void removeTrafficLight(TrafficLight trafficLight) {
        remove(trafficLight);
        revalidate();
        repaint();
    }

    protected static class ModelListener implements TrafficLightBarModel.Listener {
        private HashMap modelToComponent = new HashMap();
        private TrafficLightBar trafficLightBar;

        public ModelListener(TrafficLightBar trafficLightBar) {
            this.trafficLightBar = trafficLightBar;
        }

        public void trafficLightAdded(TrafficLightModel model) {
            TrafficLight trafficLight = new TrafficLight(model);
            trafficLight.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));

            modelToComponent.put(model, trafficLight);
            trafficLightBar.addTrafficLight(trafficLight);
        }

        public void trafficLightRemoved(TrafficLightModel model) {
            TrafficLight trafficLight = (TrafficLight) modelToComponent.remove(model);
            trafficLightBar.removeTrafficLight(trafficLight);
            trafficLight.dispose();
        }
    }
}
