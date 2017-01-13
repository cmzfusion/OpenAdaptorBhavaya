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

package org.bhavaya.ui.form;

import org.bhavaya.ui.JExtendedButton;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Description
 *
 * @author Brendon McLean, Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public abstract class Form extends JPanel {
    private JTabbedPane tabbedPane;

    protected JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public Form(FormModel formModel) {
        super(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.setModel(new FormTabbedModel());
        addTabs(tabbedPane, formModel);
        tabbedPane.setFocusable(false);

        add(tabbedPane, BorderLayout.CENTER);

        // Book buttons
        Action[] finishButtonActions = formModel.getFinalActions(this);
        NavigationButtonBar buttonBar = new NavigationButtonBar(finishButtonActions);
        add(buttonBar, BorderLayout.SOUTH);

        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof FocusInfo) {
            FocusInfo focusInfo = (FocusInfo) selectedComponent;
            focusInfo.getFirstComponent().requestFocusInWindow();
        }
    }

    protected abstract void addTabs(JTabbedPane tabbedPane, FormModel formModel);

    private static boolean acceptable(Component testComponent) {
        return testComponent.isEnabled() && testComponent.isFocusable() && testComponent.isVisible() && testComponent.isDisplayable();
    }

    private class FormTabbedModel extends ArrayList implements SingleSelectionModel {
        int selectedIndex = -1;

        public int getSelectedIndex() {
            return selectedIndex;
        }

        public void setSelectedIndex(int index) {
            selectedIndex = index;
            fireUpdate();
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    Component selectedComponent = tabbedPane.getSelectedComponent();
                    if (selectedComponent instanceof FocusInfo) {
                        FocusInfo focusInfo = (FocusInfo) selectedComponent;
                        Component firstComponent = focusInfo.getFirstComponent();
                        if (acceptable(firstComponent)) {
                            firstComponent.requestFocusInWindow();
                        }
                    }
                }
            });
        }

        public void clearSelection() {
        }

        public boolean isSelected() {
            return true;
        }

        public void addChangeListener(ChangeListener listener) {
            add(listener);
        }

        public void removeChangeListener(ChangeListener listener) {
            remove(listener);
        }

        private void fireUpdate() {
            for (Iterator itar = iterator(); itar.hasNext();) {
                ((ChangeListener) itar.next()).stateChanged(new ChangeEvent(this));
            }
        }
    }

    private class NavigationButtonBar extends JPanel implements ActionListener {
        private JButton prevButton;
        private JButton nextButton;

        public NavigationButtonBar(Action[] finishButtonActions) {
            super(new FlowLayout(FlowLayout.RIGHT));

            java.util.List focusTraversalOrder = new ArrayList();

            if (tabbedPane.getComponentCount() > 1) {
                prevButton = new JExtendedButton("Prev");
                prevButton.setEnabled(false);
                prevButton.setMnemonic('P');
                prevButton.addActionListener(this);

                nextButton = new JExtendedButton("Next");
                focusTraversalOrder.add(nextButton);
                nextButton.setMnemonic('N');
                nextButton.addActionListener(this);

                add(prevButton);
                add(nextButton);

                this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control TAB"), "nextTab");
                this.getActionMap().put("nextTab", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (nextButton.isEnabled()) {
                            tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex() + 1);
                            updateButtonState();
                        }
                    }
                });
                this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("shift control TAB"), "prevTab");
                this.getActionMap().put("prevTab", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (prevButton.isEnabled()) {
                            tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex() - 1);
                            updateButtonState();
                        }
                    }
                });
            }
            JExtendedButton[] finishButtons = new JExtendedButton[finishButtonActions.length];
            for (int i = 0; i < finishButtons.length; i++) {
                JExtendedButton finishButton = new JExtendedButton(finishButtonActions[i]);
                add(finishButton);
                focusTraversalOrder.add(finishButton);
            }

            if (tabbedPane.getComponentCount() > 1) {
                focusTraversalOrder.add(prevButton);
            }

            setFocusCycleRoot(true);
            setFocusTraversalPolicy(new NavigationFocusTraversalPolicy((Component[]) focusTraversalOrder.toArray(new Component[focusTraversalOrder.size()])));

            tabbedPane.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    updateButtonState();
                }
            });
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == prevButton) {
                tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex() - 1);
            } else if (e.getSource() == nextButton) {
                tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex() + 1);
            }
            updateButtonState();
        }

        private void updateButtonState() {
            prevButton.setEnabled(tabbedPane.getSelectedIndex() != 0);
            nextButton.setEnabled(tabbedPane.getSelectedIndex() != tabbedPane.getTabCount() - 1);
        }
    }

    /**
     * This focus traversal policy primarily controls which buttons get focus.  We need this because
     * we have Prev, Next, Finish buttons in that order, and the default policy will give focus to the
     * Prev button first.  This policy makes the focus order next, finish, prev and obviously does not
     * give focus to invisible, disabled, unfocusable components.
     */
    private static class NavigationFocusTraversalPolicy extends FocusTraversalPolicy {
        private Component[] components;

        public NavigationFocusTraversalPolicy(Component[] components) {
            this.components = components;
        }

        public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
            return getComponentAfter(find(aComponent), 0);
        }

        private Component getComponentAfter(int componentIndex, int recursionDepth) {
            if (recursionDepth > components.length) {
                return null;
            }

            Component testComponent = components[(componentIndex + 1) % components.length];
            if (!acceptable(testComponent)) {
                return getComponentAfter(componentIndex + 1, recursionDepth + 1);
            }

            return testComponent;
        }

        public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
            return getComponentBefore(find(aComponent), 0);
        }

        private Component getComponentBefore(int componentIndex, int recursionDepth) {
            if (recursionDepth > components.length) {
                throw new RuntimeException("Infinite recursion detected");
            }

            Component testComponent = components[(componentIndex + components.length - 1) % components.length];
            if (!acceptable(testComponent)) {
                return getComponentAfter(componentIndex + components.length - 1, recursionDepth + 1);
            }

            return testComponent;
        }

        public Component getFirstComponent(Container focusCycleRoot) {
            return components[0];
        }

        public Component getLastComponent(Container focusCycleRoot) {
            return components[components.length - 1];
        }

        public Component getDefaultComponent(Container focusCycleRoot) {
            return getComponentAfter(-1, 0);
        }

        private int find(Component aComponent) {
            for (int i = 0; i < components.length; i++) {
                if (components[i] == aComponent) {
                    return i;
                }
            }
            return -1;
        }
    }

}