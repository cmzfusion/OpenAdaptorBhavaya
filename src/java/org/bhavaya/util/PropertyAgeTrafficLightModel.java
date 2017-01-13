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

package org.bhavaya.util;

import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.ui.CloseAction;
import org.bhavaya.ui.DecimalTextField;
import org.bhavaya.ui.UIUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public abstract class PropertyAgeTrafficLightModel extends DefaultTrafficLightModel {
    public static final int DEFAULT_DELAY = 15000;

    private static final Timer timer = new Timer(true);

    private Observable bean;
    private String property;
    private PropertyChangeListener listener;
    private TimerTask timerTask;
    private long delayBetweenStateChange;

    private Action editAction;

    public PropertyAgeTrafficLightModel(Observable bean, String property, String name, long delayBetweenStateChange) {
        this.bean = bean;
        this.property = property;
        this.delayBetweenStateChange = delayBetweenStateChange;

        setName(name);
        setDescription(createDescription());

        listener = new DelayPropertyChangeListener();
        bean.addPropertyChangeListener(property, listener);

        timerTask = new DelayTimerTask();
        timer.scheduleAtFixedRate(timerTask, delayBetweenStateChange, delayBetweenStateChange);

        editAction = new PropertyAgeTrafficLightEditAction(this);
    }

    protected Observable getBean() {
        return bean;
    }

    protected String getProperty() {
        return property;
    }

    protected String createDescription() {
        return getName() + ": " + TrafficLightState.YELLOW + " = " + getDelayBetweenStateChange() / 1000.0 + " seconds, " + TrafficLightState.RED + " = " + ((getDelayBetweenStateChange() * 2) / 1000.0) + " seconds";
    }

    public long getDelayBetweenStateChange() {
        return delayBetweenStateChange;
    }

    public void setDelayBetweenStateChange(long delayBetweenStateChange) {
        this.delayBetweenStateChange = delayBetweenStateChange;
        resetTimerTask(delayBetweenStateChange);
        setDescription(createDescription());
    }

    private void resetTimerTask(long delayBetweenStateChange) {
        timerTask.cancel();
        timerTask = new DelayTimerTask();
        timer.schedule(timerTask, delayBetweenStateChange, delayBetweenStateChange);
    }

    public void dispose() {
        timerTask.cancel();
        bean.removePropertyChangeListener(property, listener);
    }

    public Action getEditAction() {
        return editAction;
    }

    public Action[] getPopupActions() {
        return new Action[]{editAction};
    }

    private class DelayPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            setState(TrafficLightState.GREEN);
            resetTimerTask(delayBetweenStateChange);
        }
    }

    private class DelayTimerTask extends TimerTask {
        public void run() {
            if (!getState().equals(TrafficLightState.RED)) {
                setState(getState().incrementState());
            }
        }
    }

    public static class PropertyAgeTrafficLightEditAction extends AuditedAbstractAction {
        private PropertyAgeTrafficLightModel trafficLightModel;
        private static final String format = "###,###,##0.00";

        public PropertyAgeTrafficLightEditAction(PropertyAgeTrafficLightModel trafficLightModel) {
            putValue(Action.NAME, "Edit...");
            this.trafficLightModel = trafficLightModel;
        }

        public void auditedActionPerformed(ActionEvent e) {
            JFrame frameParent = UIUtilities.getFrameParent((Component) e.getSource());
            final JDialog dialog = new JDialog(frameParent, "Edit " + trafficLightModel.getName(), true);
            final DecimalTextField field = new DecimalTextField(format, format.length(), new Long(trafficLightModel.getDelayBetweenStateChange() / 1000));

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(new JLabel("Enter delay, in seconds, between GREEN-YELLOW and YELLOW-RED"), BorderLayout.NORTH);
            mainPanel.add(field, BorderLayout.CENTER);
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(new JButton(new OkAction(field, dialog)));
            buttonPanel.add(new JButton(new CloseAction(dialog, "Cancel")));

            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.add(mainPanel, BorderLayout.CENTER);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setContentPane(contentPanel);

            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.pack();
            UIUtilities.centreInContainer(frameParent, dialog, 0, 0);
            dialog.show();
        }

        private class OkAction extends AuditedAbstractAction {
            private final DecimalTextField field;
            private final JDialog dialog;

            public OkAction(DecimalTextField field, JDialog dialog) {
                super("Ok", "Set TrafficLight age policy");
                this.field = field;
                this.dialog = dialog;
            }

            public void auditedActionPerformed(ActionEvent e) {
                Number enteredValue = field.getValue();

                try {
                    if (enteredValue != null) {
                        Long delay = new Long(enteredValue.toString());
                        long delayValue = delay.longValue() * 1000;
                        if (delayValue > 0) {
                            trafficLightModel.setDelayBetweenStateChange(delayValue);
                        } else if (trafficLightModel.getDelayBetweenStateChange() <= 0) {
                            trafficLightModel.setDelayBetweenStateChange(DEFAULT_DELAY);
                        }
                    }
                } catch (NumberFormatException ex) {
                }


                dialog.dispose();
            }
        }
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyAgeTrafficLightModel)) return false;

        final PropertyAgeTrafficLightModel propertyAgeTrafficLightModel = (PropertyAgeTrafficLightModel) o;

        if (delayBetweenStateChange != propertyAgeTrafficLightModel.delayBetweenStateChange) return false;
        if (!bean.equals(propertyAgeTrafficLightModel.bean)) return false;
        if (!property.equals(propertyAgeTrafficLightModel.property)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = bean.hashCode();
        result = 29 * result + property.hashCode();
        result = 29 * result + (int) (delayBetweenStateChange ^ (delayBetweenStateChange >>> 32));
        return result;
    }
}
