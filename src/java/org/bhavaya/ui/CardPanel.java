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

import org.bhavaya.util.Log;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * There are many ways to describe this component but the simplest way to describe is as a JTabbedPane with no tabs.
 * The interface is designed to mimick that of JTabbedPane.
 *
 * @author Brendon McLean
 * @version $Revision: 1.10 $
 */
public class CardPanel extends JComponent {

    private static final Log log = Log.getCategory(CardPanel.class);

    private ArrayList listeners = new ArrayList();
    private int selectedIndex = -1;

    private boolean stretchToFit;


    public CardPanel() {
        this(true);
    }

    public CardPanel(boolean stretchToFit) {
        this.stretchToFit = stretchToFit;
        setLayout(new CardLayoutManager());
    }

    public void addChangeListener(ChangeListener changeListener) {
        listeners.add(changeListener);
    }

    public void removeChangeListener(ChangeListener changeListener) {
        listeners.remove(changeListener);
    }

    private void fireChange() {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            ChangeListener listener = (ChangeListener) iterator.next();
            listener.stateChanged(new ChangeEvent(this));
        }
    }

    public Component add(Component comp) {
        addComponent(comp);
        return comp;
    }

    public Component add(String name, Component comp) {
        addComponent(comp);
        return comp;
    }

    public Component add(Component comp, int index) {
        Component old = getComponent(index);
        insertComponent(comp, index);
        return old;
    }

    public void add(Component comp, Object constraints) {
        addComponent(comp);
    }

    public void add(Component comp, Object constraints, int index) {
        insertComponent(comp, index);
    }

    public void addComponent(Component component) {
        insertComponent(component, getComponentCount());
    }

    public void remove(int index) {
        checkSafeToModify();

        checkIndex(index);

        Component selectedComponent = getSelectedComponent();

        Component removeComponent = getComponent(index);
        super.remove(index);

        boolean shouldDecrement = selectedIndex > index;
        if (getComponentCount() == 0) {
            setSelectedIndexImpl(-1, selectedComponent);
        } else {
            int selectedIndex = shouldDecrement ? getSelectedIndex() - 1 : getSelectedIndex();
            selectedIndex = Math.min(selectedIndex, getComponentCount() - 1); // Stop array our of bounds
            selectedIndex = Math.max(selectedIndex, 0); // Stop array under bounds
            setSelectedIndexImpl(selectedIndex, selectedComponent);
        }

        // Restore the component's default visibility.
        removeComponent.setVisible(true);

        revalidate();
        repaint();
        fireChange();
    }

    public void remove(Component comp) {
        removeComponentAt(indexOfComponent(comp));
    }

    public void insertComponent(Component component, int index) {
        checkSafeToModify();
        assert component != null : "Component cannot be null";

        int newIndex = index;

        int removeIndex = indexOfComponent(component);
        if (removeIndex != -1) {
            removeComponentAt(removeIndex);
            if (newIndex > removeIndex) {
                newIndex--;
            }
        }

        Component selectedComponent = getSelectedComponent();

        component.setVisible(false);
        addImpl(component, null, index);

        if (getComponentCount() == 1) {
            setSelectedIndexImpl(0, selectedComponent);
        } else {
            if (newIndex <= selectedIndex) {
                setSelectedIndexImpl(selectedIndex + 1, selectedComponent);
            }
        }

        revalidate();
        repaint();
        fireChange();
    }

    private void checkSafeToModify() {
        if (!(getParent() == null || EventQueue.isDispatchThread())) {
            log.warn("CardPanel modified whilst not on event thread.");
        }
    }

    public void removeComponent(Component component) {
        removeComponentAt(indexOfComponent(component));
    }

    public void removeComponentAt(int index) {
        remove(index);
    }

    public void setSelectedIndex(int index) {
        checkSafeToModify();

        if (selectedIndex == index) return;
        if (index != -1) {
            checkIndex(index);
        }

        setSelectedIndexImpl(index, getSelectedComponent());

        revalidate();
        repaint();
        fireChange();
    }

    private void setSelectedIndexImpl(int index, Component currentSelectedComponent) {
        boolean hasFocus = false;
        if (currentSelectedComponent != null) {
            hasFocus = currentSelectedComponent.isFocusOwner();
            currentSelectedComponent.setVisible(false);
        }

        selectedIndex = index;

        if (selectedIndex != -1) {
            Component component = getSelectedComponent();
            component.setVisible(true);
            if (hasFocus) component.requestFocus();
        }
    }

    public void setSelectedComponent(Component component) {
        setSelectedIndex(indexOfComponent(component));
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public Component getSelectedComponent() {
        return selectedIndex == -1 ? null : getComponent(selectedIndex);
    }

    /**
     * @deprecated use getComponent()
     */
    public Component getComponentAt(int index) {
        return super.getComponent(index);
    }

    public int indexOfComponent(Component component) {
        Component[] components = getComponents();
        for (int i = 0; i < components.length; i++) {
            Component test = components[i];
            if (test == component) return i;
        }
        return -1;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= getComponentCount()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Component count: " + getComponentCount());
        }
    }

    /**
     * This is similar to java.awt.CardLayout hence the name.
     *
     * @see java.awt.CardLayout
     */
    private class CardLayoutManager implements LayoutManager {
        public void addLayoutComponent(String name, Component comp) {
            synchronized (comp.getTreeLock()) {
                if (getComponentCount() > 0) {
                    comp.setVisible(false);
                }
            }
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int ncomponents = getComponentCount();
                int w = 0;
                int h = 0;

                for (int i = 0; i < ncomponents; i++) {
                    Component comp = getComponent(i);
                    Dimension d = comp.getPreferredSize();
                    if (d.width > w) {
                        w = d.width;
                    }
                    if (d.height > h) {
                        h = d.height;
                    }
                }
                return new Dimension(insets.left + insets.right + w, insets.top + insets.bottom + h);
            }
        }

        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int ncomponents = getComponentCount();
                int w = 0;
                int h = 0;

                for (int i = 0; i < ncomponents; i++) {
                    Component comp = getComponent(i);
                    Dimension d = comp.getMinimumSize();
                    if (d.width > w) {
                        w = d.width;
                    }
                    if (d.height > h) {
                        h = d.height;
                    }
                }
                return new Dimension(insets.left + insets.right + w, insets.top + insets.bottom + h);
            }
        }

        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();

                if ((getComponentCount() > 0) && getSelectedComponent() != null) {
                    Component comp = getSelectedComponent();
                    int width = stretchToFit ? parent.getWidth() : comp.getPreferredSize().width;
                    int height = stretchToFit ? parent.getHeight() : comp.getPreferredSize().height;
                    comp.setBounds(insets.left, insets.top, width - (insets.left + insets.right), height - (insets.top + insets.bottom));
                }
            }

        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 2; i++) {
            JTextArea textArea = new JTextArea(15, 60);
            JScrollPane textAreaScrollPane = new JScrollPane(textArea);
            JTable table = new JTable(new Object[][]{{"AA", "AB", "AC"}, {"BA", "BB", "BC"}, {"CA", "CB", "CC"}}, new Object[]{"Col1", "Col2", "Col3"});
            JScrollPane tableScrollPane = new JScrollPane(table);

            final CardPanel cardPanel = new CardPanel(i == 1);
            cardPanel.addComponent(textAreaScrollPane);
            cardPanel.addComponent(tableScrollPane);

            JButton buttonOne = new JButton(new AbstractAction("One") {
                public void actionPerformed(ActionEvent e) {
                    cardPanel.setSelectedIndex(0);
                }
            });
            JButton buttonTwo = new JButton(new AbstractAction("Two") {
                public void actionPerformed(ActionEvent e) {
                    cardPanel.setSelectedIndex(1);
                }
            });
            JButton buttonThree = new JButton(new AbstractAction("Null") {
                public void actionPerformed(ActionEvent e) {
                    cardPanel.setSelectedIndex(-1);
                }
            });
            JButton removeButton = new JButton(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    cardPanel.remove(0);
                }
            });
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(buttonOne);
            buttonPanel.add(buttonTwo);
            buttonPanel.add(buttonThree);
            buttonPanel.add(removeButton);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(buttonPanel, BorderLayout.NORTH);
            mainPanel.add(cardPanel, BorderLayout.CENTER);

            JFrame frame = new JFrame("test");
            frame.getContentPane().add(mainPanel);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }
    }

    /**
     * @deprecated use removeAll();
     */
    public void clear() {
        super.removeAll();
        setSelectedIndex(-1);
    }

    public boolean isStretchToFit() {
        return stretchToFit;
    }

    public void setStretchToFit(boolean stretchToFit) {
        this.stretchToFit = stretchToFit;
    }
}
