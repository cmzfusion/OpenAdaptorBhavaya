package org.bhavaya.ui.table;

import junit.framework.TestCase;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.util.DynamicAttribute;
import org.bhavaya.util.DynamicObjectType;
import org.bhavaya.util.Generic;

import java.util.concurrent.Semaphore;

/**
 * Bug reported by user in which dynamic objects, grouping, beancollectiontablemodel and several other players
 * are involved.  This test has never failed, but it needed to be done to just in case.
 *
 * @author Brendon McLean.
 */
public class IntegratedTableTest extends TestCase {

    public void testEdgeCase() throws InterruptedException {
        final BeanCollection<DynamicBeanTest> dynamicBeans = new DefaultBeanCollection<DynamicBeanTest>(DynamicBeanTest.class);

        final int[] beanStart = {0};
        int n = 100;
        for (int i = beanStart[0]; i < n; i++) {
            DynamicBeanTest bean = new DynamicBeanTest();
            bean.getD().setI(i);
            dynamicBeans.add(bean);
        }

        DynamicObjectType type = (DynamicObjectType) Generic.getType(DynamicBeanTest.class);
        type.addAttribute(new DynamicAttribute("dynamicProperty", Integer.class));

        BeanCollectionTableModel tableModel = new BeanCollectionTableModel(DynamicBeanTest.class, true, "Dynamic");
        AnalyticsTableModel analyticsTableModel = new AnalyticsTableModel(tableModel);
        tableModel.setBeanCollection(dynamicBeans);

        updateMultiple(dynamicBeans, 0);

        tableModel.addColumnLocator("d.i");
        tableModel.addColumnLocator("dynamicProperty");
        analyticsTableModel.setGrouped(true);

        final Semaphore semaphore = new Semaphore(1, true);

        Thread updateDThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        semaphore.acquire();
                        int i = 0;
                        for (DynamicBeanTest dynamicBeanTest : dynamicBeans) {
                            dynamicBeanTest.getD().setI(beanStart[0] + i);
                            i++;
                        }
                        beanStart[0] = beanStart[0] + 1;
                        semaphore.release();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }, "updateDThread");
        updateDThread.start();

        Thread updateMultipleThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        semaphore.acquire();
                        DynamicBeanTest.multiple = DynamicBeanTest.multiple + 1;
                        updateMultiple(dynamicBeans, DynamicBeanTest.multiple);
                        semaphore.release();
                    } catch (InterruptedException e) {
                    }
                }
            }
        });
        updateMultipleThread.start();

        while (true) {
            Thread.sleep(1);
            semaphore.acquire();
//            Thread.sleep(100);
            tableModel.waitForEventsToClear();

            try {
                synchronized (tableModel.getChangeLock()) {
                    int an = beanStart[0] - 1;
                    int expectedResultForDI = n / 2 * (an + an + (n - 1));
                    Object object = analyticsTableModel.getValueAt(0, 0);
                    if (object == CachedObjectGraph.DATA_NOT_READY) continue;
                    int di = (Integer) object;
                    object = analyticsTableModel.getValueAt(0, 1);
                    if (object == CachedObjectGraph.DATA_NOT_READY) continue;
                    int diM = (Integer) analyticsTableModel.getValueAt(0, 1);

                    logassertEquals("di wrong", expectedResultForDI, di);
                    logassertEquals("dim wrong", expectedResultForDI * DynamicBeanTest.multiple, diM);
                    System.out.println("Checked");
                }
            } finally {
                semaphore.release();
            }
        }
    }

    private void logassertEquals(String s, int i, int diM) {
        if (i != diM) {
            System.err.println("wrong");
        }
    }

    private void updateMultiple(BeanCollection<DynamicBeanTest> dynamicBeans, int multiple) {
        for (DynamicBeanTest dynamicBeanTest : dynamicBeans) {
            dynamicBeanTest.set("dynamicProperty", dynamicBeanTest.getD().getI() * multiple);
        }
    }

}
