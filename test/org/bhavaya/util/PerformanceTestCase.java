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

package org.bhavaya.util;

import junit.framework.TestCase;
import org.bhavaya.util.Log;
import org.bhavaya.ui.table.CachedObjectGraphPerformance;

import java.text.DecimalFormat;

/**
 * Note that performance benchmarking is extremely tricky, especially for these iterative tests.  Several VM params
 * should be used.  Firstly, -mx and -ms should be specified to avoid allocation costs (although not completely,
 * Windows will always allocate less real memory than the virtual memory page size).  Secondly, the hotspot compilet
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public abstract class PerformanceTestCase extends TestCase {
    private static final DecimalFormat PER_ITERATION_FORMAT = new DecimalFormat("0.00E00");
    private static final DecimalFormat PER_SECOND_FORMAT = new DecimalFormat("###,###");

    private double methodOverheadTime;
    private static final Log log;

    public interface PerformanceTestRunnable {
        public void iteration(int iterationCont);
    }

    static {
        log = Log.getCategory(CachedObjectGraphPerformance.class);
    }

    public PerformanceTestCase(String s) {
        super(s);
        methodOverheadTime = measure(new PerformanceTestRunnable() {
            public void iteration(int i) {
            }
        }, 2000000);
        log.info("Method overhead time: " + createReportString(methodOverheadTime));
    }

    protected double measure(PerformanceTestRunnable runnable, long iterationCount) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterationCount; i++) {
            runnable.iteration(i);
        }
        long endTime = System.currentTimeMillis();
        return (endTime - startTime - (methodOverheadTime * iterationCount)) / (double) iterationCount / 1000.0;
    }

    protected String formatPerIteration(double speed) {
        return PER_ITERATION_FORMAT.format(speed);
    }

    protected String formatPerSecond(double speed) {
        return PER_SECOND_FORMAT.format((long) (1 / speed));
    }

    protected String measureWithReport(PerformanceTestRunnable runnable, long iterationCount) {
        double speed = measure(runnable, iterationCount);
        return createReportString(speed);
    }

    protected String createReportString(double speed) {
        return formatPerSecond(speed) + " iterations per second, " + formatPerIteration(speed) + " secs per iteration";
    }
}
