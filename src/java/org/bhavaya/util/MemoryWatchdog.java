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

import java.util.ArrayList;
import java.util.Iterator;


/**
 * A simple thread which polls the available memory and prints a log statement when a certain threshold is reached.
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */

public class MemoryWatchdog {
    private static final Log log = Log.getCategory(MemoryWatchdog.class);

    public static interface MemoryLowListener {
        public void memoryLow();
    }

    private static final int WARNING_THRESHOLD = 1024 * 1024 * 7;
    private static final int SLEEP_TIME = 10000;

    private static MemoryWatchdog instance;

    private ArrayList listeners = new ArrayList();
    private WatcherThread watcherThread;
    private long thresholdCorrectionOffset = 0;
    private long maxMemory;


    public static MemoryWatchdog getInstance() {
        if (instance == null) {
            instance = new MemoryWatchdog();
        }
        return instance;
    }

    private MemoryWatchdog() {
        watcherThread = new WatcherThread();
        String javaVersionString = System.getProperty("java.version");
        if (javaVersionString.indexOf("1.4.0") != -1 || javaVersionString.indexOf("1.4.1") != -1) {
            log.info("Detect Java Version with bug id 4686462.  Adjusting threshold by 64MB to compensate");
            thresholdCorrectionOffset = 1024 * 1024 * 64;
        }
        maxMemory = Runtime.getRuntime().maxMemory() - thresholdCorrectionOffset;
        log.info("Maximum memory found for this machine (-mx Flag): " + (maxMemory / 1024 / 1024) + "MB");
    }

    public void addMemoryLowListener(MemoryLowListener listener) {
        listeners.add(listener);
    }

    public void removeMemoryLowListener(MemoryLowListener listener) {
        listeners.remove(listener);
    }

    public final long getAvailableMemory() {
        return getMaxMemory() - getUsedMemory();
    }

    public final long getMaxMemory() {
        return maxMemory;
    }

    public final long getAllocatedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public final long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public void start() {
        log.info("Starting Memory Watcher Thread.  Polling every " + ((double) SLEEP_TIME / 1000d) + " seconds");
        watcherThread.start();
    }

    private void fireMemoryLow() {
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            MemoryLowListener memoryLowListener = (MemoryLowListener) iterator.next();
            memoryLowListener.memoryLow();
        }
    }

    private class WatcherThread extends Thread {
        public WatcherThread() {
            super("Memory-WatchDog");
            setDaemon(true);
        }

        public void run() {
            while (true) {
                try {
                    if (getAvailableMemory() < WARNING_THRESHOLD) {
                        Log.getUserCategory().warn("Available memory has dropped below threshold of " + ((WARNING_THRESHOLD) / 1024) + "K");
                        fireMemoryLow();
                    } else if (getAvailableMemory() < (getMaxMemory() * 0.1)) {
                        log.info("Now using more than 90% of the allowable memory.  Available = " + (getAvailableMemory() / 1024) + "K");
                    }
                } catch (Exception e) {
                    // Ignored.
                }

                try {
                    sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
        }
    }
}
