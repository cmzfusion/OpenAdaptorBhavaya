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

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.DefaultSetStatement;

import java.util.Date;
import java.util.Random;

/**
 * not finished, see Dan
 */
public class AnalyticsTableModelTest {
    private BeanCollection beanCollection;
    private AnalyticsTable analyticsTable;
    private BeanCollectionTableModel beanCollectionTableModel;
    private UIControls uiControls = new UIControls();

    public AnalyticsTableModelTest() {

        beanCollection = new DefaultBeanCollection(Foo.class);
        String[] names = new String[]{"Fred", "George", "Bert", "Ernie", "Hamish", "Macbeth"};
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            long randomDate = new Date(2050, 1, 1).getTime() - System.currentTimeMillis();
            randomDate = (long) ((double) randomDate * random.nextDouble()) + System.currentTimeMillis();
            int randomNameIndex = random.nextInt(names.length);
            Foo bean = new Foo();
            bean.setDate(new Date(randomDate));
            bean.setIndex(i);
            bean.setName(names[randomNameIndex]);
            bean.setValue(random.nextDouble() * 100.0);
            beanCollection.add(bean);
        }

        beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, false);
        beanCollectionTableModel.addSetStatement(new DefaultSetStatement(new String[]{"name", "value"}));
        analyticsTable = new AnalyticsTable(beanCollectionTableModel, true);
    }

    public AnalyticsTable getAnalyticsTable() {
        return analyticsTable;
    }

    public BeanCollection getBeanCollection() {
        return beanCollection;
    }

    public BeanCollectionTableModel getTabularList() {
        return beanCollectionTableModel;
    }

    public UIControls getUIControls() {
        return uiControls;
    }

    public class UIControls{
        private InsertRowControl newRowControl = new InsertRowControl();
        private DeleteRowControl deleteRowControl = new DeleteRowControl();

        public InsertRowControl getNewRowControl() {
            return newRowControl;
        }

        public DeleteRowControl getDeleteRowControl() {
            return deleteRowControl;
        }

        public class DeleteRowControl {
            private int rowToDelete;

            public int getRowToDelete() {
                return rowToDelete;
            }

            public void setRowToDelete(int rowToDelete) {
                this.rowToDelete = rowToDelete;
            }

            public void deleteSelectedRow() {
                getBeanCollection().remove( getRowToDelete() );
            }
        }

        public class InsertRowControl {
            private Foo newBean = new Foo();

            public void insertRow() {
                Foo bean = (Foo) BeanUtilities.verySlowDeepCopy(newBean);
                bean.setIndex(getBeanCollection().size());
                getBeanCollection().add(bean);
            }

            public void setNewRowName(String name) {
                newBean.setName(name);
            }

            public String getNewRowName() {
                return newBean.getName();
            }

            public void setNewRowValue(Number value) {
                newBean.setValue(value.doubleValue());
            }

            public void setNewRowValue(double value) {
                newBean.setValue(value);
            }

            public double getNewRowValue() {
                return newBean.getValue();
            }
        }
    }


    public static class Foo extends org.bhavaya.util.DefaultObservable {
        private String name;
        private double value;
        private Date date;
        private int index;

        public Foo() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String oldValue = this.name;
            this.name = name;
            firePropertyChange("name", oldValue, name);
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            Date oldValue = this.date;
            this.date = date;
            firePropertyChange("date", oldValue, date);
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            double oldValue = this.value;
            this.value = value;
            firePropertyChange("value", oldValue, value);
        }
    }
}