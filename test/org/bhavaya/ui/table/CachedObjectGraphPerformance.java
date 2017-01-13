/**
 * Description
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
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

import org.bhavaya.ui.table.CachedObjectGraph;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;
import org.bhavaya.util.PerformanceTestCase;

public class CachedObjectGraphPerformance extends PerformanceTestCase {
    private static final Log log;

    private static final int TEST_SET_SIZE = 100 * 1000;

    static {
        log = Log.getCategory(CachedObjectGraphPerformance.class);
//        log.setDebugOverride(true);
    }

    public CachedObjectGraphPerformance(String s) {
        super(s);
    }

    public void testGetPerformanceSynchronised() {
        final String[] stringArray = new String[]{"String1", "String2", "String3", "String4", "String5"};


        final BeanA a = new BeanA();
        final BeanC c = a.getB().getC();

        final CachedObjectGraph graph = new CachedObjectGraph(BeanA.class);
        graph.addRootObject(a);

        PropertyChangeObserver bListener = new PropertyChangeObserver("b", "b");
        PropertyChangeObserver bCListener = new PropertyChangeObserver("b.c", "b.c");
        PropertyChangeObserver bCSomeStringListener = new PropertyChangeObserver("b.c.someString", "b.c.someString");
        graph.addPathListener("b", bListener);
        graph.addPathListener("b.c", bCListener);
        graph.addPathListener("b.c.someString", bCSomeStringListener);

        // Testing pure getter performance.
        String[] propertyArray = "b.c.someString".split("\\.");
        for (int j = 1; j <= propertyArray.length; j++) {
            final String propertyPath = Generic.beanPathArrayToString((String[]) Utilities.subSection(propertyArray, 0, j));

            log.info("Sync: CachedObjectGraph.get() - propertyDepth " + j + ": " + measureWithReport(new PerformanceTestCase.PerformanceTestRunnable() {
                public void iteration(int i) {
                    graph.get(a, propertyPath);
                }
            }, TEST_SET_SIZE));
        }

        graph.removePathListener("b.c", bCListener);
        graph.removePathListener("b", bListener);

        // Test event performance
        log.info("Sync: CachedObjectGraph event speed: " + measureWithReport(new PerformanceTestCase.PerformanceTestRunnable() {
            public void iteration(int iterationCont) {
                c.setSomeString(stringArray[iterationCont % stringArray.length]);
            }
        }, TEST_SET_SIZE));
    }

    public void testGetPerformanceAsynchronous() {
        final String[] stringArray = new String[]{"String1", "String2", "String3", "String4", "String5"};

        final BeanA a = new BeanA();
        final BeanC c = a.getB().getC();

        final CachedObjectGraph graph = new CachedObjectGraph(BeanA.class, true);
        graph.addRootObject(a);

        PropertyChangeObserver bListener = new PropertyChangeObserver("b", "b");
        PropertyChangeObserver bCListener = new PropertyChangeObserver("b.c", "b.c");
        PropertyChangeObserver bCSomeStringListener = new PropertyChangeObserver("b.c.someString", "b.c.someString");
        graph.addPathListener("b", bListener);
        graph.addPathListener("b.c", bCListener);
        graph.addPathListener("b.c.someString", bCSomeStringListener);

        // Testing pure getter performance.
        String[] propertyArray = "b.c.someString".split("\\.");
        for (int j = 1; j <= propertyArray.length; j++) {
            final String propertyPath = Generic.beanPathArrayToString((String[]) Utilities.subSection(propertyArray, 0, j));

            log.info("ASync: CachedObjectGraph.get() - propertyDepth " + j + ": " + measureWithReport(new PerformanceTestCase.PerformanceTestRunnable() {
                public void iteration(int i) {
                    graph.get(a, propertyPath);
                }
            }, TEST_SET_SIZE));
        }

        graph.removePathListener("b.c", bCListener);
        graph.removePathListener("b", bListener);

        // Test event performance
        log.info("ASync: CachedObjectGraph event speed: " + measureWithReport(new PerformanceTestCase.PerformanceTestRunnable() {
            public void iteration(int iterationCont) {
                c.setSomeString(stringArray[iterationCont % stringArray.length]);
            }
        }, TEST_SET_SIZE));
    }
}
