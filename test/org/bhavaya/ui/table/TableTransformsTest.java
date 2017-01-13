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

import junit.framework.TestCase;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.util.Generic;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class TableTransformsTest extends TestCase {

    public void testPivotSimpleTest() {
        final BeanCollection beanCollection = new DefaultBeanCollection(BeanA.class);
        AnalyticsTableConfigurator configurator = new AnalyticsTableConfigurator(beanCollection);
        KeyedColumnTableModel tableModel = configurator.getConfig("columnTotallingEnabled pivoted rowTotallingEnabled");

        AnalyticsTableTest test = new AnalyticsTableTest("Simple pivot test.", tableModel, new RandomDataFeed(beanCollection) {
            public void run() {
                int count = 5;
                for (int i = 0; i < count; i++) {
                    BeanA bean = new BeanA();
//                bean.setB(sharedBean);
                    bean.setB((BeanB) getRandom(BeanB.class));
                    beanCollection.add(bean);
                }
                sleep(500);

                // modify pivot data
                for (int j = 0; j < 1; j++) {
                    sleep(500);
                    Object b = beanCollection.get(j);
                    String str = random.nextBoolean() ? "AUD" : "SKK";
                    Generic.set(b, Generic.beanPathStringToArray("b.c.someString"), str, true);
                }

            }
        });

        test.runTest();

        System.out.println("All done");
    }

    public void testTables() {
        BeanCollection beanCollection = new DefaultBeanCollection(BeanA.class);
        AnalyticsTableConfigurator configurator = new AnalyticsTableConfigurator(beanCollection);
        RandomDataFeed dataFeed = new RandomDataFeed(beanCollection);

        AnalyticsTableTest test = new AnalyticsTableTest("Analytics table test", null, dataFeed); // model is set in the loop

        Iterator it = configurator.iterator();
        for (int i = 0; it.hasNext(); i++) {
            beanCollection.clear();
            KeyedColumnTableModel tableModel = (KeyedColumnTableModel) it.next();
            test.setName("Table test: " + i);
            test.setTableModel(tableModel);
            test.runTest();
            sleep(500);
        }
        System.out.println("All done");
    }

    public void testScenario(String config) {
        BeanCollection beanCollection = new DefaultBeanCollection(BeanA.class);
        AnalyticsTableConfigurator configurator = new AnalyticsTableConfigurator(beanCollection);
        KeyedColumnTableModel tableModel = configurator.getConfig(config);
        RandomDataFeed dataFeed = new RandomDataFeed(beanCollection);
        AnalyticsTableTest test = new AnalyticsTableTest("Single scenarion test", tableModel, dataFeed);
        test.runTest();
        System.out.println("All done");
    }

    public void testScenario(int mask) {
        BeanCollection beanCollection = new DefaultBeanCollection(BeanA.class);
        AnalyticsTableConfigurator configurator = new AnalyticsTableConfigurator(beanCollection);
        KeyedColumnTableModel tableModel = configurator.getConfig(mask);
        RandomDataFeed dataFeed = new RandomDataFeed(beanCollection);
        AnalyticsTableTest test = new AnalyticsTableTest("Single scenarion test", tableModel, dataFeed);
        test.runTest();
        System.out.println("All done");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static class AnalyticsTableConfigurator {

        BeanCollectionTableModel beanCollectionTableModel;

        /**
         * All combinations of these properties will be tested.
         */
        static final String[] booleanProperties = {
            "columnTotallingEnabled",
            "dateBucketSpreading",
            "grouped",
            "pivoted",
            "rowTotallingEnabled",
            "useFullSeries",
            "usingDateSeries"
        };

        public int getMask(String props) {
            StringTokenizer st = new StringTokenizer(props);
            List l = Arrays.asList(booleanProperties);
            int ret = 0;
            while (st.hasMoreTokens()) {
                String prop = st.nextToken();
                int idx = l.indexOf(prop);
                if (idx != -1) {
                    int bit = 1 << idx;
                    ret |= bit;
                }
            }
            return ret;
        }

        public AnalyticsTableConfigurator(BeanCollectionTableModel beanCollectionTableModel) {
            setBeanCollectionTableModel(beanCollectionTableModel);
        }

        public AnalyticsTableConfigurator(BeanCollection beanCollection) {
            setBeanCollection(beanCollection);
        }

        public void setBeanCollection(BeanCollection beanCollection) {
            this.beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, true);
        }

        public void setBeanCollectionTableModel(BeanCollectionTableModel beanCollectionTableModel) {
            this.beanCollectionTableModel = beanCollectionTableModel;
        }

        public KeyedColumnTableModel getConfig(String config) {
            return getConfig(getMask(config));
        }

        public KeyedColumnTableModel getConfig(int mask) {
            AnalyticsTableModel analyticsTableModel = new AnalyticsTableModel(beanCollectionTableModel);

            beanCollectionTableModel.addColumnLocator("b.b.date");
            beanCollectionTableModel.addColumnLocator("b.b.c.someDouble");
            beanCollectionTableModel.addColumnLocator("b.b.c.someString");

            analyticsTableModel.getTableSorter().addSortingColumn("b.b.date", true);
            String configInfo = "";
            for (int i = 0; i < booleanProperties.length; i++) {
                String booleanProperty = booleanProperties[i];
                int bit = 1 << i;
                if ((mask & bit) > 0) {
                    Generic.set(analyticsTableModel, booleanProperty, Boolean.TRUE);
                    configInfo += booleanProperty + ", ";
                } else {
                    Generic.set(analyticsTableModel, booleanProperty, Boolean.FALSE);
                }
            }
            System.out.println("Config (" + mask + ") " + configInfo);

            analyticsTableModel.getTableSorter().addTableModelListener(new EventAndStateVerifier(analyticsTableModel.getTableSorter()));
            analyticsTableModel.getTableSorter().addTableModelListener(new SortedTest(analyticsTableModel.getTableSorter()));
            analyticsTableModel.getTableGrouper().addTableModelListener(new EventAndStateVerifier(analyticsTableModel.getTableGrouper()));
            analyticsTableModel.getTablePivoter().addTableModelListener(new EventAndStateVerifier(analyticsTableModel.getTablePivoter()));

            if (analyticsTableModel.isUsingDateSeries()) {
                analyticsTableModel.getTableDateSeriesSpliter().addTableModelListener(new EventAndStateVerifier(analyticsTableModel.getTableDateSeriesSpliter()));
            }

            return analyticsTableModel;
        }

        public Iterator iterator() {
            return new ConfigIterator();
        }

        private class ConfigIterator implements Iterator {
            int currentConfig = 0;


            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return currentConfig < ((1 << booleanProperties.length) - 1);
            }

            public Object next() {
                return getConfig(currentConfig++);
            }
        }

        private static class SortedTest implements TableModelListener {
            private SortedTableModel sortedTableModel;

            public SortedTest(SortedTableModel sortedTableModel) {
                this.sortedTableModel = sortedTableModel;
            }

            public void tableChanged(TableModelEvent e) {
                int count = sortedTableModel.getRowCount();
                if (sortedTableModel.isRowTotallingEnabled()) count -= 1;
                for (int i = 0; i < count; i++) {
                    int underlying = sortedTableModel.mapModelToUnderlying(i);
                    int model = sortedTableModel.mapUnderlyingToModel(underlying);
                    TestCase.assertEquals("Mapping from model to underlying, back to model was inconsistent!", i, model);
                }
            }
        }
    }

    public static class AnalyticsTableTest {

        private JFrame frame;
        private KeyedColumnTableModel tableModel;
        private Runnable dataFeed;
        private String name;

        public AnalyticsTableTest(String name, KeyedColumnTableModel tableModel, Runnable dataFeed) {
            this.name = name;
            this.tableModel = tableModel;
            this.dataFeed = dataFeed;
        }

        public void runTest() {
            createGUI(name, tableModel);
            dataFeed.run();
        }

        public void setDataFeed(Runnable dataFeed) {
            this.dataFeed = dataFeed;
        }

        public void setTableModel(KeyedColumnTableModel tableModel) {
            this.tableModel = tableModel;
        }

        public void setName(String name) {
            this.name = name;
        }

        private void createGUI(String title, KeyedColumnTableModel tableModel) {
            AnalyticsTable analyticsTable = new AnalyticsTable(tableModel, true);
            analyticsTable.setHighlightNewRows(true);
            analyticsTable.setFading(true);

            if (frame == null) {
                frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(new JScrollPane(analyticsTable));
                frame.setSize(600, 800);
                frame.show();
            } else {
                frame.setTitle(title);
                frame.getContentPane().removeAll();
                frame.getContentPane().add(new JScrollPane(analyticsTable));
                frame.validate();
            }
        }
    }

    public static void main(String[] args) {
        TableTransformsTest test = new TableTransformsTest();
//        test.testPivotSimpleTest();
//        test.testTables();
//        test.testScenario("columnTotallingEnabled");
        test.testScenario(13);
    }
}
