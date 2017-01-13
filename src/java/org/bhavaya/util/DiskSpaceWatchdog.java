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

import java.util.*;


/**
 * A simple thread which polls the available disk space and prints a log statement when a certain threshold is reached.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */

public class DiskSpaceWatchdog {
    private static final Log userLog = Log.getUserCategory();
    private static final Log log = Log.getCategory(DiskSpaceWatchdog.class);

    public static interface LowDiskSpaceListener {
        public void lowDiskSpaceWarning(String path, Environment.DiskSpaceInfo diskSpaceInfo);
        public void lowDiskSpaceError(String path, Environment.DiskSpaceInfo diskSpaceInfo);
    }

    private static final int SLEEP_TIME = 120000;

    private static DiskSpaceWatchdog instance;

    private List listeners = Collections.synchronizedList(new ArrayList());
    private Set watches = Collections.synchronizedSet(new HashSet());
    private WatcherThread watcherThread;

    public static DiskSpaceWatchdog getInstance() {
        if (instance == null) {
            instance = new DiskSpaceWatchdog();
        }
        return instance;
    }

    private DiskSpaceWatchdog() {
        watcherThread = new WatcherThread();
        watcherThread.start();
    }

    public void addLowDiskSpaceListener(LowDiskSpaceListener listener) {
        listeners.add(listener);
    }

    public void removeLowDiskSpaceListener(LowDiskSpaceListener listener) {
        listeners.remove(listener);
    }

    public void start() {
        log.info("Starting Disk Space Watcher Thread.  Polling every " + ((double) SLEEP_TIME / 1000d) + " seconds");
        watcherThread.start();
    }

    /**
     * Adds a path for which available space will be periodically checked.
     * @param path
     * @param warningLimit minimum size in bytes before warning is logged to the user log.
     * @param errorLimit minimum size in bytes before error is logged to the user log.
     */
    public void addPath(String path, long warningLimit, long errorLimit) {
        Watch watch = new Watch(path, warningLimit, errorLimit);
        watches.add(watch);
    }

    private void fireLowDiskSpaceError(String path, Environment.DiskSpaceInfo diskSpaceInfo) {
        Object[] listeners = this.listeners.toArray();
        for (int i = 0; i < listeners.length; i++) {
            LowDiskSpaceListener listener = (LowDiskSpaceListener) listeners[i];
            listener.lowDiskSpaceError(path, diskSpaceInfo);
        }
    }

    private void fireLowDiskSpaceWarning(String path, Environment.DiskSpaceInfo diskSpaceInfo) {
        Object[] listeners = this.listeners.toArray();
        for (int i = 0; i < listeners.length; i++) {
            LowDiskSpaceListener listener = (LowDiskSpaceListener) listeners[i];
            listener.lowDiskSpaceWarning(path, diskSpaceInfo);
        }
    }

    private class WatcherThread extends Thread {
        public WatcherThread() {
            super("DiskSpace-WatchDog");
            setDaemon(true);
        }

        public void run() {
            while (true) {
                try {
                    Object[] watches = DiskSpaceWatchdog.this.watches.toArray();
                    for (int i = 0; i < watches.length; i++) {
                        Watch watch = (Watch)watches[i];
                        Environment.DiskSpaceInfo diskSpaceInfo = Environment.getDiskSpaceInfo(watch.path);
                        if (!diskSpaceInfo.successful) {
                            watch.state = Watch.UNKNOWN_STATE;
                        } else if (diskSpaceInfo.availableSpace < watch.errorLimit) {
                            if (watch.state < Watch.ERROR_STATE) {
                                userLog.error("You are running low of disk space on your " + watch.path + " drive. Available space is: " + diskSpaceInfo.getFormattedAvailableSpace());
                                fireLowDiskSpaceError(watch.path, diskSpaceInfo);
                            }
                            watch.state = Watch.ERROR_STATE;
                        } else if (diskSpaceInfo.availableSpace < watch.warningLimit) {
                            if (watch.state < Watch.WARN_STATE) {
                                userLog.warn("You are running low of disk space on your " + watch.path + " drive. Available space is: " + diskSpaceInfo.getFormattedAvailableSpace());
                                fireLowDiskSpaceWarning(watch.path, diskSpaceInfo);
                            } else if (watch.state > Watch.WARN_STATE) {
                                log.info("Back to WARN state.");
                            }
                            watch.state = Watch.WARN_STATE;
                        } else {
                            if (watch.state != Watch.OK_STATE) {
                                log.info("Back to OK state");
                                watch.state = Watch.OK_STATE;
                            }
                        }
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

    private static class Watch {

        static final int UNKNOWN_STATE = -1;
        static final int OK_STATE = 0;
        static final int WARN_STATE = 1;
        static final int ERROR_STATE = 2;

        String path;
        long warningLimit;
        long errorLimit;
        int state = OK_STATE;

        public Watch(String path, long warningLimit, long errorLimit) {
            this.path = path;
            this.warningLimit = warningLimit;
            this.errorLimit = errorLimit;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Watch watch = (Watch) o;

            if (path != null ? !path.equals(watch.path) : watch.path != null) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            return (path != null ? path.hashCode() : 0);
        }
    }
}
