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
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.adaptor.Source;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.beans.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */
public class GenericTableTest extends TestCase {
    private static final Log log;

    static {
        log = Log.getCategory(GenericTableTest.class);
//        log.setDebugOverride(true);
    }

    private static final String[] COLUMN_LOCATORS = new String[]{"someDouble", "b.someBoolean", "b.c.someString", "b.c.d.i"};
    private static final int ROW_COUNT = 50;
    private static final int NUM_EVENTS = 100000;

    private Map<Class<?>, Source> typeToRandomExpression = new HashMap<Class<?>, Source>();

    public GenericTableTest(String s) {
        super(s);
        typeToRandomExpression.put(Integer.class, new Source() {
            public Object getData() {
                return (int) (Math.random() * Integer.MAX_VALUE);
            }
        });
        typeToRandomExpression.put(Long.class, new Source() {
            public Object getData() {
                return (long) (Math.random() * Long.MAX_VALUE);
            }
        });
        typeToRandomExpression.put(Boolean.class, new Source() {
            public Object getData() {
                return Math.random() > 0.5;
            }
        });
        typeToRandomExpression.put(String.class, new Source() {
            public Object getData() {
                return String.valueOf((int) (Math.random() * Integer.MAX_VALUE));
            }
        });
        typeToRandomExpression.put(Double.class, new Source() {
            public Object getData() {
                return Math.random();
            }
        });
        typeToRandomExpression.put(Float.class, new Source() {
            public Object getData() {
                return new Float(Math.random());
            }
        });
    }

    private BeanCollection getFunctionalBeanCollection(Class type, String[] columnLocators, int rowCount) throws Exception {
        BeanCollection beanCollection = new DefaultBeanCollection(type);

        for (int i = 0; i < rowCount; i++) {
            Object bean = type.newInstance();
            for (int j = 0; j < columnLocators.length; j++) {
                String columnLocator = columnLocators[j];
                String[] beanPath = Generic.beanPathStringToArray(columnLocator);
                Attribute beanAttribute = Generic.getAttribute(type, beanPath);
                Generic.set(bean, beanPath, getFunctionalValueForType(ClassUtilities.typeToClass(beanAttribute.getType()), i, j), false);
            }
            beanCollection.add(bean);
        }
        return beanCollection;
    }

    private Object getFunctionalValueForType(Class type, int i, int j) {
        if (type == String.class) {
            return "" + (j + i * 2);
        } else if (type == Integer.class) {
            return j + i * 2;
        } else {
            return null;
        }
    }

    private BeanCollection getRandomBeanCollection(Class beanClass, String[] columnLocators, int rows) throws Exception {
        BeanCollection beanCollection = new DefaultBeanCollection(beanClass);

        for (int i = 0; i < rows; i++) {
            Object bean = beanClass.newInstance();
            for (int j = 0; j < columnLocators.length; j++) {
                String columnLocator = columnLocators[j];
                String[] beanPath = Generic.beanPathStringToArray(columnLocator);
                Attribute beanAttribute = Generic.getAttribute(beanClass, beanPath);
                Generic.set(bean, beanPath, getRandomValueForType(ClassUtilities.typeToClass(beanAttribute.getType())), false);
            }
            beanCollection.add(bean);
        }
        return beanCollection;
    }

    private Object getRandomValueForType(Class type) {
        Source source = typeToRandomExpression.get(ClassUtilities.typeToClass(type));
        return source != null ? source.getData() : null;
    }

    public void testBeanCollectionModelSynchronous() throws Exception {
        testBeanCollectionModel(false);
    }

    public void testBeanCollectionModelAsynchronous() throws Exception {
        testBeanCollectionModel(true);
    }

    public void testMultithreaded() throws Exception {
        log.info("Generating ordered table model");
        String[] columnLocators = new String[]{"b.c.someString", "b.c.d.i"};
        final BeanCollection beanCollection = getFunctionalBeanCollection(BeanA.class, columnLocators, ROW_COUNT);
        final BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, true);
        for (int i = 0; i < columnLocators.length; i++) {
            String columnLocator = columnLocators[i];
            beanCollectionTableModel.addColumnLocator(columnLocator);
        }

        AnalyticsTable table = new AnalyticsTable(beanCollectionTableModel, true);
        JFrame frame = new JFrame();
        frame.getContentPane().add(table);
        frame.pack();
        frame.setVisible(true);

        log.info("Taking snapshot");
        beanCollectionTableModel.waitForEventsToClear();
        Thread.sleep(1000);
        final Object[][] tableSnapShot = getSnapShot(beanCollectionTableModel);

        log.info("Creating multiple threads to increment/decrement model");
        final int ITERATIONS = 500;
        int NUM_THREADS = 5;
        ThreadGroup runningThreadGroup = new ThreadGroup("Updater Threads");
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread incrementThread = new Thread(runningThreadGroup, new Runnable() {
                public void run() {
                    for (int n = 0; n < ITERATIONS; n++) {
                        for (int i = 0; i < beanCollectionTableModel.getRowCount(); i++) {
                            Object bean = beanCollection.get(i);
                            synchronized (BeanA.class) {
                                String string = (String) Generic.get(bean, Generic.beanPathStringToArray("b.c.someString"));
                                Integer integer = (Integer) Generic.get(bean, Generic.beanPathStringToArray("b.c.d.i"));
                                Generic.set(bean, Generic.beanPathStringToArray("b.c.someString"), "" + (Integer.valueOf(string) + 1), false);
                                Generic.set(bean, Generic.beanPathStringToArray("b.c.d.i"), integer.intValue() + 1, false);
                                if ((n % 5) == 0) {
                                    UIUtilities.runInDispatchThread(new Runnable() {
                                        public void run() {
                                            beanCollectionTableModel.removeColumnLocator("b.c.someString");
                                        }
                                    });
                                }
                                if ((n % 7) == 0) {
                                    UIUtilities.runInDispatchThread(new Runnable() {
                                        public void run() {
                                            beanCollectionTableModel.addColumnLocator("b.c.someString");
                                        }
                                    });
                                }
                            }
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                log.error(e);
                            }
                        }
                    }
                    log.info("Thread finished");
                }
            });
            Thread decrementThread = new Thread(runningThreadGroup, new Runnable() {
                public void run() {
                    for (int n = 0; n < ITERATIONS; n++) {
                        for (int i = 0; i < beanCollectionTableModel.getRowCount(); i++) {
                            Object bean = beanCollection.get(i);
                            synchronized (BeanA.class) {
                                String string = (String) Generic.get(bean, Generic.beanPathStringToArray("b.c.someString"));
                                Integer integer = (Integer) Generic.get(bean, Generic.beanPathStringToArray("b.c.d.i"));
                                Generic.set(bean, Generic.beanPathStringToArray("b.c.someString"), "" + (Integer.valueOf(string) - 1), false);
                                Generic.set(bean, Generic.beanPathStringToArray("b.c.d.i"), integer.intValue() - 1, false);
                                if ((n % 5) == 0) {
                                    UIUtilities.runInDispatchThread(new Runnable() {
                                        public void run() {
                                            beanCollectionTableModel.removeColumnLocator("b.c.d.i");
                                        }
                                    });
                                }
                                if ((n % 7) == 0) {
                                    UIUtilities.runInDispatchThread(new Runnable() {
                                        public void run() {
                                            beanCollectionTableModel.addColumnLocator("b.c.d.i");
                                        }
                                    });
                                }
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    log.error(e);
                                }
                            }
                        }
                    }
                }
            });
            incrementThread.start();
            decrementThread.start();
        }

        log.info("Waiting for threads to finish");
        while (runningThreadGroup.activeCount() > 0) {
            Thread.sleep(50);
        }
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                beanCollectionTableModel.removeColumnLocator("b.c.d.i");
                beanCollectionTableModel.addColumnLocator("b.c.someString");
                beanCollectionTableModel.addColumnLocator("b.c.d.i");
            }
        });

        log.info("Comparing snapshots");
        beanCollectionTableModel.waitForEventsToClear();
        getSnapShot(beanCollectionTableModel); // discard to get rid of loading items
        Thread.sleep(1000);
        assertEquals("Snapshots should have zero differences", 0, snapshotsDifferences(tableSnapShot, getSnapShot(beanCollectionTableModel)));
    }

    public void testBeanCollectionModel(boolean asynchronous) throws Exception {
        log.info("Generating random table model");
        BeanCollection beanCollection = getRandomBeanCollection(BeanA.class, COLUMN_LOCATORS, ROW_COUNT);
        BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanCollection, asynchronous);
        beanCollectionTableModel.addTableModelListener(new EventAndStateVerifier(beanCollectionTableModel));

        for (String columnLocator : COLUMN_LOCATORS) {
            beanCollectionTableModel.addColumnLocator(columnLocator);
        }

        log.info("Taking table snapshot");
        beanCollectionTableModel.waitForEventsToClear();
        Thread.sleep(1000);
        Object[][] tableSnapShot = getSnapShot(beanCollectionTableModel);

        log.info("Sending random events from bean collection");
        Statement[] undoOperations = randomiseBeanCollection(beanCollection, COLUMN_LOCATORS, NUM_EVENTS);

        log.info("Running undo events in reverse order");
        for (int i = undoOperations.length - 1; i >= 0; i--) {
            Statement undoOperation = undoOperations[i];
            undoOperation.execute();
        }

        log.info("Comparing snapshots");
        beanCollectionTableModel.waitForEventsToClear();
        Thread.sleep(1000);
        assertEquals("Snapshots should have zero differences", 0, snapshotsDifferences(tableSnapShot, getSnapShot(beanCollectionTableModel)));
    }

    private Statement[] randomiseBeanCollection(BeanCollection beanCollection, String[] columnLocators, int numEvents) throws Exception {
        ArrayList undoStatementList = new ArrayList();
        for (int i = 0; i < numEvents; i++) {
            int row = (int) (Math.random() * beanCollection.size());
            String columnLocator = columnLocators[(int) (Math.random() * columnLocators.length)];
            Object bean = beanCollection.get(row);
            String[] beanPath = Generic.beanPathStringToArray(columnLocator);
            Attribute attribute = Generic.getAttribute(BeanA.class, beanPath);
            Object currentValue = Generic.get(bean, beanPath);
            Generic.set(bean, beanPath, getRandomValueForType(ClassUtilities.typeToClass(attribute.getType())), false);
            undoStatementList.add(new Statement(Generic.class, "set", new Object[]{bean, beanPath, currentValue, false}));
            if ((i % 20) == 0) Thread.yield();
        }
        return (Statement[]) undoStatementList.toArray(new Statement[undoStatementList.size()]);
    }

    private int snapshotsDifferences(Object[][] snapShotA, Object[][] snapShotB) {
        int snapshotDifferences = 0;
        if (snapShotA.length != snapShotB.length) return Integer.MAX_VALUE;
        if (snapShotA[0].length != snapShotB[0].length) return Integer.MAX_VALUE;
        for (int i = 0; i < snapShotA.length; i++) {
            for (int j = 0; j < snapShotA[0].length; j++) {
                if (!Utilities.equals(snapShotA[i][j], snapShotB[i][j])) {
                    snapshotDifferences++;
                }
            }
        }
        return snapshotDifferences;
    }

    private Object[][] getSnapShot(TableModel tableModel) {
        Object[][] snapshot = new Object[tableModel.getRowCount()][tableModel.getColumnCount()];
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                snapshot[i][j] = tableModel.getValueAt(i, j);
            }
        }
        return snapshot;
    }
}
