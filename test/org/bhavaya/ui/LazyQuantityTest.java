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

import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.VerticalFlowLayout;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.util.LazyQuantity;
import org.bhavaya.util.LoadClosure;
import org.bhavaya.util.Quantity;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This class shall demostrate what problems can occur with LazyQuantity,
 * when through differents threads the getter of the property is called by the table,
 * before the value is initialized by e.g. the notification process.
 */
public class LazyQuantityTest {
    private static double storedValue;

    public static void main(String[] args) {
        final DefaultBeanCollection beanCollection = new DefaultBeanCollection(TestBean.class);

        final BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, false);
        beanCollectionTableModel.addColumnLocator("amount");
        beanCollectionTableModel.addColumnLocator("nonWorkingQuantity.amount");
        beanCollectionTableModel.addColumnLocator("workingQuantity.amount");

        beanCollectionTableModel.setColumnName("nonWorkingQuantity.amount","non working",false);
        beanCollectionTableModel.setColumnName("workingQuantity.amount","working",false);

        final JTextField amountField = new JTextField(5);

        // inserts a new row with
        // column 1: the amount set to -1
        // column 2: Quantity of null / pretending getter gets called before value is initialized with Quantity(NaN(null,null))/load true.
        // column 3: Initialized LazyQuantity, Quantity(NaN(null,null))/load true.
        JButton insertButton = new JButton(new AbstractAction("InsertRow") {
            public void actionPerformed(ActionEvent e) {
                try {
                    TestBean bean = new TestBean();
                    storedValue = bean.getAmount();
                    beanCollection.add(bean);
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                }
            }
        });

        final AnalyticsTable jTable = new AnalyticsTable(beanCollectionTableModel, true);

        //update the selected row with the value inside the textfield. If value is not
        // a number, Double.NaN is taken.
        JButton updateButton = new JButton(new AbstractAction("UpdateRow") {
            public void actionPerformed(ActionEvent e) {
                final int selectedRow = jTable.getSelectedRow();
                if (selectedRow < 0) return;
                final Object beanForRow = beanCollectionTableModel.getBeanForRow(selectedRow);
                double value1 = Double.NaN;
                try {
                    value1 = new Double(amountField.getText().toString()).doubleValue();
                } catch (NumberFormatException e1) {
                }
                if (beanForRow != null && beanForRow instanceof TestBean) {
                    TestBean bean = (TestBean) beanForRow;
                    bean.setAmount(value1);
                    storedValue = value1;
                    bean.setNonWorkingQuantity(getNewQuantityWithLoadClosure());
                    bean.setWorkingQuantity(getNewQuantityWithLoadClosure());
                }
            }
        });

        JPanel mainPanel = new JPanel(new VerticalFlowLayout());
        mainPanel.add(new JScrollPane(jTable));
        JPanel changePanel = new JPanel();
        changePanel.add(UIUtilities.createLabelledComponent("amount", amountField));
        changePanel.add(insertButton);
        changePanel.add(updateButton);
        mainPanel.add(changePanel);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Creates the kind of LazyQuantity, which gets triggered by the notification process.
     * @return  Quantity(NaN(null,null))/load true
     */
    public static LazyQuantity getNewQuantityWithLoadClosure() {
        LazyQuantity lazyQuantity = new LazyQuantity();
        lazyQuantity.setLoad(new MyLoadClosure());
        return lazyQuantity;
    }

    // the getters and setter for the quantities are the same as the one get generated by the source code generator
    // for BondTrade.nominalValue.
    public static class TestBean extends org.bhavaya.util.DefaultObservable {
        // the amount set
        private double amount;
        // the quantity, which was intialized with null first.
        private Quantity nonWorkingQuantity;
        // the quantity, which was initialized correctly with Quantity(NaN(null,null))/load true
        private Quantity workingQuantity;

        public TestBean() {
            this(-1, null, getNewQuantityWithLoadClosure());
        }

        public TestBean(double amount, Quantity nonWorkingQuantity, Quantity workingQuantity) {
            this.amount = amount;
            this.nonWorkingQuantity = nonWorkingQuantity;
            this.workingQuantity = workingQuantity;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            double oldValue = this.amount;
            this.amount = amount;
            firePropertyChange("amount", oldValue, amount);
        }

        public Quantity getNonWorkingQuantity() {
            if (org.bhavaya.beans.BeanFactory.isLazy(nonWorkingQuantity)) {
                setNonWorkingQuantity((org.bhavaya.util.Quantity) ((org.bhavaya.util.LoadClosure) nonWorkingQuantity).load());
            }
            if (org.bhavaya.beans.BeanFactory.isLazyNull(nonWorkingQuantity)) {
                return null;
            }
            return nonWorkingQuantity;
        }

        public void setNonWorkingQuantity(Quantity nonWorkingQuantity) {
            boolean newValueLazy = org.bhavaya.beans.BeanFactory.isLazy(nonWorkingQuantity);
            org.bhavaya.util.Quantity oldValue = this.nonWorkingQuantity;
            boolean oldValueLazy = org.bhavaya.beans.BeanFactory.isLazy(oldValue);
            if (!oldValueLazy && newValueLazy) {
                nonWorkingQuantity = (org.bhavaya.util.Quantity) ((org.bhavaya.util.LoadClosure) nonWorkingQuantity).load();
                newValueLazy = false;
            }
            org.bhavaya.util.Quantity tempOldValue = org.bhavaya.beans.BeanFactory.isLazyNull(oldValue) ? null : oldValue;
            org.bhavaya.util.Quantity tempNewValue = org.bhavaya.beans.BeanFactory.isLazyNull(nonWorkingQuantity) ? null : nonWorkingQuantity;
            this.nonWorkingQuantity = nonWorkingQuantity;
            if (!oldValueLazy) firePropertyChange("nonWorkingQuantity", tempOldValue, tempNewValue);
        }

        public Quantity getWorkingQuantity() {
            if (org.bhavaya.beans.BeanFactory.isLazy(workingQuantity)) {
                setWorkingQuantity((org.bhavaya.util.Quantity) ((org.bhavaya.util.LoadClosure) workingQuantity).load());
            }
            if (org.bhavaya.beans.BeanFactory.isLazyNull(workingQuantity)) {
                return null;
            }
            return workingQuantity;
        }

        public void setWorkingQuantity(Quantity workingQuantity) {
            boolean newValueLazy = org.bhavaya.beans.BeanFactory.isLazy(workingQuantity);
            org.bhavaya.util.Quantity oldValue = this.workingQuantity;
            boolean oldValueLazy = org.bhavaya.beans.BeanFactory.isLazy(oldValue);
            if (oldValue != null && !oldValueLazy && newValueLazy) {
                workingQuantity = (org.bhavaya.util.Quantity) ((org.bhavaya.util.LoadClosure) workingQuantity).load();
                newValueLazy = false;
            }
            org.bhavaya.util.Quantity tempOldValue = org.bhavaya.beans.BeanFactory.isLazyNull(oldValue) ? null : oldValue;
            org.bhavaya.util.Quantity tempNewValue = org.bhavaya.beans.BeanFactory.isLazyNull(workingQuantity) ? null : workingQuantity;
            this.workingQuantity = workingQuantity;
            if (!oldValueLazy && !newValueLazy) firePropertyChange("workingQuantity", tempOldValue, tempNewValue);
        }

    }

    // just sets the storedValue for the Quantity
    private static class MyLoadClosure implements LoadClosure {
        public Object load() {
            try {
                return new LazyQuantity(storedValue, "EUR");
            } catch (NumberFormatException e1) {
                return new LazyQuantity();
            }
        }
    }
}