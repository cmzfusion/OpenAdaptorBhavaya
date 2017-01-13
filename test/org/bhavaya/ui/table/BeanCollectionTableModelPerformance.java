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
import org.bhavaya.ui.table.BeanCollectionTableModel;
import org.bhavaya.util.Log;
import org.bhavaya.util.PerformanceTestCase;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class BeanCollectionTableModelPerformance extends PerformanceTestCase {
    private static final Log log;
    private static final int ITERATION_COUNT = 8000;

    static {
        log = Log.getCategory(CachedObjectGraphPerformance.class);
//        log.setDebugOverride(true);
    }

    public BeanCollectionTableModelPerformance(String s) {
        super(s);
    }

    public void testAdd() throws Exception {
        final BeanCollection beanACollection = new DefaultBeanCollection(BeanA.class);

        BeanCollectionTableModel beanCollectionTableModel = new BeanCollectionTableModel(beanACollection, false);
        beanCollectionTableModel.addColumnLocator("someDouble");
        beanCollectionTableModel.addColumnLocator("b.someBoolean");
        beanCollectionTableModel.addColumnLocator("b.c.someString");
        beanCollectionTableModel.addColumnLocator("b.c.d.i");

        ArrayList beanList = new ArrayList();
        for (int i = 0; i < ITERATION_COUNT; i++) {
            beanList.add(new BeanA());
        }
        System.gc();
        Thread.sleep(100);
        final BeanA[] beanArray = (BeanA[]) beanList.toArray(new BeanA[beanList.size()]);

        log.info("beanCollection.add(Object) performance: " + measureWithReport(new PerformanceTestRunnable() {
            public void iteration(int iterationCont) {
                beanACollection.add(beanArray[iterationCont]);
            }
        }, ITERATION_COUNT));

        System.gc();
        Thread.sleep(100);

        log.info("beanCollection.remove(Object) performance: " + measureWithReport(new PerformanceTestRunnable() {
            public void iteration(int iterationCont) {
                beanACollection.remove(beanArray[iterationCont]);  // A bit a rubbish test as this is a function of the iteration count
            }
        }, ITERATION_COUNT / 50));

        beanACollection.addAll(Arrays.asList(beanArray));
        System.gc();
        Thread.sleep(100);

        log.info("beanCollection.remove(int) performance: " + measureWithReport(new PerformanceTestRunnable() {
            public void iteration(int iterationCont) {
                beanACollection.remove(0); // A bit a rubbish test as this is a function of the iteration count
            }
        }, ITERATION_COUNT / 50));
   }
}
