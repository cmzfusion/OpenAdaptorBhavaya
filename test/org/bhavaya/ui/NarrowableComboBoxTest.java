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

import junit.framework.TestCase;
import org.bhavaya.util.StringRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3.26.2 $
 */

public class NarrowableComboBoxTest extends TestCase {
    private static final java.util.List TEST_DATA = Arrays.asList("a", "apple", "abacus", "application", "approcanthy", "applision", "appzomol", "appropriate", "aptitude", "beta", "beta", "delta");

    public NarrowableComboBoxTest(String s) {
        super(s);
    }

    public void testNarrowableListModel() {
        final NarrowableListModel model = new NarrowableListModel(TEST_DATA);

        // Size should be zero because narrow(...) hasn'r been called.
        assertEquals("Model size incorrect.", 0, model.getSize());

        // Should still be 5 and abacus should be first, aptitude last
        model.narrow("a");
        assertEquals("Model size incorrect", 9, model.getSize());
        assertTrue("First element incorrect", model.getElementAt(0).equals("a"));
        assertTrue("Last element incorrect", model.getElementAt(8).equals("aptitude"));

        // Should be apple, application, appropriate
        model.narrow("app");
        assertEquals("Model size incorrect", 6, model.getSize());
        assertTrue("First element incorrect", model.getElementAt(0).equals("apple"));
        assertTrue("Last element incorrect", model.getElementAt(5).equals("appzomol"));

        // Now try a partial match. Should return just zero
        model.narrow("abacuss");
        assertEquals("Model size incorrect", 0, model.getSize());

        // Hook up and listener to check correct event signalling.
        final MutableBoolean listenerCalled = new MutableBoolean(false);
        model.addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
                fail("Method should not be called");
            }

            public void intervalRemoved(ListDataEvent e) {
                fail("Method should not be called");
            }

            public void contentsChanged(ListDataEvent e) {
                // List UI's don't actually look for e.index0 and e.index1, they just redraw so getting here is good enough
                listenerCalled.setBoolean(true);
                model.removeListDataListener(this);
            }
        });
        model.narrow("aba");
        assertTrue("Model sending model change events", listenerCalled.getBoolean());
    }

    public static void main(String[] args) {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent event) {
                System.out.println("event = " + event);
            }
        }, AWTEvent.FOCUS_EVENT_MASK);

        RowLayout rowLayout = new RowLayout(400, 15);
        JPanel contentPanel = new JPanel(rowLayout);

        RowLayout.Row row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, false);
        NarrowableListModel model = new NarrowableListModel(TEST_DATA);
        final NarrowableComboBox narrowableComboBox = new NarrowableComboBox(10, model, 4);
        narrowableComboBox.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                System.out.println("List selection event, selected idx: " + narrowableComboBox.getSelectedValue());
            }
        });
        narrowableComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Action event, selected: " + narrowableComboBox.getChosenObject());
            }
        });
        contentPanel.add(row.addComponent(UIUtilities.createLabelledComponent("Normal NCB", narrowableComboBox)));

        JTextField textField = new JTextField(5);
        contentPanel.add(row.addComponent(textField));

        JButton okBut = new JButton(new AbstractAction("ok") {
            public void actionPerformed(ActionEvent e) {
                narrowableComboBox.ensureValueSelected(new Boolean(false));
            }
        });
        contentPanel.add(row.addComponent(okBut));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, false);
        model = new NarrowableListModel(TEST_DATA);
        final NarrowableComboBox narrowableComboBox2 = new NarrowableComboBox(10, model, 4);
        narrowableComboBox2.setChosenObject(TEST_DATA.get(1));
        narrowableComboBox2.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                System.out.println("List selection event, selected idx: " + narrowableComboBox2.getSelectedValue());
            }
        });
        narrowableComboBox2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Action event, selected: " + narrowableComboBox2.getChosenObject());
            }
        });
        contentPanel.add(row.addComponent(UIUtilities.createLabelledComponent("Normal NCB", narrowableComboBox2)));

        textField = new JTextField(5);
        contentPanel.add(row.addComponent(textField));

        okBut = new JButton(new AbstractAction("ok") {
            public void actionPerformed(ActionEvent e) {
                narrowableComboBox2.ensureValueSelected(new Boolean(false));
            }
        });
        contentPanel.add(row.addComponent(okBut));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, false);

        NarrowableListModel model3 = new NarrowableListModel(TEST_DATA, true);
        final NarrowableComboBox narrowableComboBox3 = new NarrowableComboBox(10, model3, 4);
        narrowableComboBox3.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                System.out.println("List selection event, selected idx: " + narrowableComboBox3.getSelectedValue());
            }
        });
        narrowableComboBox3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Action event, selected: " + narrowableComboBox3.getChosenObject());
            }
        });
        contentPanel.add(row.addComponent(UIUtilities.createLabelledComponent("NCB (all show)", narrowableComboBox3)));
        contentPanel.add(row.addComponent(new JTextField(5)));
        rowLayout.addRow(row);

        row = new RowLayout.Row(10, RowLayout.LEFT, RowLayout.MIDDLE, false);
        NarrowableListModel delayedModel = new DelayedNarrowableListModel();
        final NarrowableComboBox dealyedNarrowableComboBox = new NarrowableComboBox(10, delayedModel, 4);
        dealyedNarrowableComboBox.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                System.out.println("List selection event, selected idx: " + dealyedNarrowableComboBox.getSelectedValue());
            }
        });
        dealyedNarrowableComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Action event, selected: " + dealyedNarrowableComboBox.getChosenObject());
            }
        });
        contentPanel.add(row.addComponent(UIUtilities.createLabelledComponent("Delayed NCB", dealyedNarrowableComboBox)));
        contentPanel.add(row.addComponent(new JTextField(5)));
        rowLayout.addRow(row);

        row = new RowLayout.Row(15, RowLayout.LEFT, RowLayout.MIDDLE, false);
        NarrowableComboBox nc2 = new NarrowableComboBox(10, new NarrowableListModel(Arrays.asList(3, 4)), 4);
        nc2.setRenderer(new StringRenderer() {
            public String render(Object o) {
                return "0x" + (((Integer) o).intValue());
            }
        });
        contentPanel.add(row.addComponent(UIUtilities.createLabelledComponent("Test rendering trick (enter in 0x3)", nc2)));
        rowLayout.addRow(row);

        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(contentPanel);
        frame.pack();
        frame.show();
    }

    /**
     * Just used as a means of maintaining a reference to a mutable boolean.
     */
    private class MutableBoolean {
        private boolean bool;

        public MutableBoolean(boolean bool) {
            this.bool = bool;
        }

        public void setBoolean(boolean bool) {
            this.bool = bool;
        }

        public boolean getBoolean() {
            return bool;
        }
    }

    private static class DelayedNarrowableListModel extends CachedDataSourceNarrowableListModel {
        public DelayedNarrowableListModel() {
            super(new DataQuery() {
                public Collection execAndGetCollection(String searchString) {
                    return TEST_DATA;
                }

                public Object getQueryKey() {
                    return TEST_DATA;
                }
            }, false);
        }
    }
}
