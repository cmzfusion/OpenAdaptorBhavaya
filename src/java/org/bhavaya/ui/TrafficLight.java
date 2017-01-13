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

import org.bhavaya.util.*;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.Encoder;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public class TrafficLight extends JLabel {

    static {
        BeanUtilities.addPersistenceDelegate(TrafficLight.class, new BhavayaPersistenceDelegate(new String[]{"model"}) {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                //don't call any of the setters and getters after construction
                return;
            }
        });
    }

    private TrafficLightModel model;
    private PropertyChangeListener listener;
    private Action doubleClickAction;
    private java.util.List popActions;

    public TrafficLight(TrafficLightModel model) {
        super();
        setFocusable(true);

        this.model = model;
        setHorizontalTextPosition(JLabel.CENTER);
        setVerticalTextPosition(JLabel.BOTTOM);

        setFont(getFont().deriveFont(10f));
        setName(model.getName());
        setState(model.getState());
        if (model.getDescription() != null) {
            registerForToolTips();
        }

        listener = new TrafficLightStateListener();
        model.addPropertyChangeListener(listener);

        popActions = new ArrayList();
        addMouseListener(new EditActionMouseAdapter());

        Action editAction = model.getEditAction();
        if (editAction != null) {
            doubleClickAction = editAction;
        }
        Action[] popupActions = model.getPopupActions();
        for (int i = 0; i < popupActions.length; i++) {
            addPopupAction(popupActions[i]);
        }
    }

    public void refresh() {
        setState(model.getState());
    }

    public void setName(String name) {
        setText(name);
    }

    private void registerForToolTips() {
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.registerComponent(this);
    }

    private void unregisterForToolTips() {
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.unregisterComponent(this);
    }

    /**
     * Overriden parent method to provide up-to-date tooltip.
     */
    public String getToolTipText() {
        return model.getDescription();
    }

    private void setState(TrafficLightState state) {
        setIcon(stateToIcon(state));
    }

    public TrafficLightModel getModel() {
        return model;
    }

    protected void setDoubleClickAction(Action doubleClickAction) {
        this.doubleClickAction = doubleClickAction;
    }

    public void addPopupAction(Action action) {
        popActions.add(action);
    }

    public void dispose() {
        model.removePropertyChangeListener(listener);
    }

    private static Icon stateToIcon(TrafficLightState state) {
        int height = ToolBarGroup.getToolBarSize()-6;
        if (state == TrafficLightState.RED) {
            return ImageIconCache.getImageIcon("led_red.png", height);
        } else if (state == TrafficLightState.YELLOW) {
            return ImageIconCache.getImageIcon("led_amber.png", height);
        } else if (state == TrafficLightState.GREEN) {
            return ImageIconCache.getImageIcon("led_green.png", height);
        } else {
            throw new RuntimeException("Unknown state: " + state);
        }
    }

    private class TrafficLightStateListener implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent evt) {
            UIUtilities.runInDispatchThread(new Runnable() {
                public void run() {
                    if (evt.getPropertyName().equals("state")) {
                        TrafficLightState state = (TrafficLightState) evt.getNewValue();
                        setState(state);
                    } else if (evt.getPropertyName().equals("name")) {
                        String name = (String) evt.getNewValue();
                        setName(name);
                    } else if (evt.getPropertyName().equals("description")) {
                        if (evt.getNewValue()==null) {
                            unregisterForToolTips();
                        } else if (evt.getOldValue()==null) {
                            registerForToolTips();
                        }
                    }
                }
            });
        }
    }

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame();
        JPanel contents = new JPanel();

        TrafficLightModel model = new DefaultTrafficLightModel();
        TrafficLight trafficLight = new TrafficLight(model);
        contents.add(trafficLight);
        frame.setContentPane(contents);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        while (true) {
            Thread.sleep(1000);
            model.setState(TrafficLightState.YELLOW);
            Thread.sleep(1000);
            model.setState(TrafficLightState.GREEN);
            Thread.sleep(1000);
            model.setState(TrafficLightState.YELLOW);
            Thread.sleep(1000);
            model.setState(TrafficLightState.RED);
        }
    }

    private class EditActionMouseAdapter extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if (doubleClickAction != null && doubleClickAction.isEnabled() && e.getClickCount() == 2) {
                doubleClickAction.actionPerformed(null);
            }
        }

        public void mousePressed(MouseEvent e) {
            checkupPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            checkupPopup(e);
        }

        private void checkupPopup(MouseEvent e) {
            if (e.isPopupTrigger() && popActions.size() > 0) {
                JPopupMenu popup = new JPopupMenu();
                for (Iterator iterator = popActions.iterator(); iterator.hasNext();) {
                    Action action = (Action) iterator.next();
                    JMenuItem menuItem = new JMenuItem(action);
                    popup.add(menuItem);
                }
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
