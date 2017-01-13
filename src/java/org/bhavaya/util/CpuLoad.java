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

import gnu.trove.TIntLongHashMap;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.ui.table.AnalyticsTable;
import org.bhavaya.ui.table.BeanCollectionTableModel;

import javax.swing.*;
import java.util.*;
import java.util.Timer;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.4 $
 */
public class CpuLoad {
    private static final int DEFAULT_LOAD_AVE_SAMPLE_SIZE = 24; // One minute
    private static final CpuLoad defaultInstance = new CpuLoad();

    /**
     * Think before you lower this value.  Heuristically, this process will use up 15% CPU (14% kernel, 1% user) on a
     * 2GHz P4 Xeon when the sleep delay is set to 100.  Also, there is probably not much harm in setting this value
     * to quite a big number because the averaging nature of the getUsage() function
     *
     * @see #getLoad()
     */
    private static final int POLL_PERIOD = 2500;

    static {
        Timer cpuPoll = new Timer(true);
        TimerTask timerTask = new TimerTask() {
            public void run() {
                try {
                    defaultInstance.updateUsage();
                } catch (Error e) {
                    // Platform has no cpu monitor ability.
                }
            }
        };
        cpuPoll.schedule(timerTask, 0, POLL_PERIOD);
    }

    private double load = 0;
    private long lastRealTime = System.currentTimeMillis();
    private TIntLongHashMap lastProcessTotalMap = new TIntLongHashMap();
    private TIntLongHashMap lastProcessTimeMap = new TIntLongHashMap();

    private double[] loadSamples;
    private int currentLoadSampleIndex = 0;
    private int currentLoadSampleArraySize = 0;
    private long lastTotalProcessTime = 0;


    private CpuLoad() {
        loadSamples = new double[DEFAULT_LOAD_AVE_SAMPLE_SIZE];
    }

    /**
     * @return The average CPU load over the polling period.  Because the underlying algorithm uses process times, this
     *         does not represent a 'snapshot' of the CPU usage, but an average load factor over the POLL_PERIOD.
     */
    public double getLoad() {
        return load;
    }

    public double getProcessCPULoadFactor(int nativeProcessId) {
        synchronized (this) {
            if (lastProcessTimeMap.containsKey(nativeProcessId)) {
                return (double) lastProcessTimeMap.get(nativeProcessId) / (double) lastTotalProcessTime;
            } else {
                return 0;
            }
        }
    }

    private void updateUsage() {
        NativeProcess[] nativeProcesses = NativeProcess.getAllProcesses();

        // Add up total process time which is used to calculate total CPU time.
        long totalProcessTime = 0;
        synchronized (this) {
            for (int i = 0; i < nativeProcesses.length; i++) {
                NativeProcess nativeProcess = nativeProcesses[i];
                if (nativeProcess != null) {
                    int processId = nativeProcess.getProcessId();
                    long currentProcessTotal = Environment.getProcessCPUTime(nativeProcess.getProcessId());
                    long lastProcessTotal = lastProcessTotalMap.containsKey(processId) ? lastProcessTotalMap.get(processId) : 0;
                    long processTime = currentProcessTotal - lastProcessTotal;
                    totalProcessTime += processTime;
                    lastProcessTotalMap.put(processId, currentProcessTotal);
                    lastProcessTimeMap.put(processId, processTime);
                }
            }
            long currentTime = System.currentTimeMillis();
            load = Math.min(((double) totalProcessTime) / ((double) currentTime - (double) lastRealTime), 1d);
            lastTotalProcessTime = currentTime - lastRealTime;
            lastRealTime = currentTime;

            // Store process time history to work out rolling averages.
            currentLoadSampleIndex = (currentLoadSampleIndex + 1) % 20;
            loadSamples[currentLoadSampleIndex] = load;
            currentLoadSampleArraySize = Math.min(currentLoadSampleArraySize + 1, loadSamples.length);
        }
    }

    public float getLoadAverage() {
        synchronized (this) {
            float total = 0;
            for (int i = 0; i < currentLoadSampleArraySize; i++) {
                total += loadSamples[i];
            }
            return total / currentLoadSampleArraySize;
        }
    }

    public static CpuLoad getInstance() {
        return CpuLoad.defaultInstance;
    }

    public static void main(String[] args) throws Exception {
        final Map processBeanMap = new HashMap();

        DefaultBeanCollection processBeanCollection = new DefaultBeanCollection(ProcessBean.class);
        BeanCollectionTableModel tableModel = new BeanCollectionTableModel(processBeanCollection, true);
        tableModel.setColumnLocators(Arrays.asList(new String[]{"processId", "processName", "processRatio"}));

        AnalyticsTable processTable = new AnalyticsTable(tableModel, true);
        JScrollPane scrollPane = new JScrollPane(processTable);

        JFrame frame = new JFrame("CPU Loads");
        frame.getContentPane().add(scrollPane);
        frame.pack();
        frame.show();

        while (true) {
            Thread.sleep(500);
            NativeProcess[] processes = NativeProcess.getAllProcesses();
            Set activeProcesses = new HashSet(processBeanMap.keySet());
            for (int i = 0; i < processes.length; i++) {
                NativeProcess process = processes[i];
                ProcessBean processBean = (ProcessBean) processBeanMap.get(new Integer(process.getProcessId()));
                double processCPULoadFactor = CpuLoad.getInstance().getProcessCPULoadFactor(process.getProcessId());
                if (processBean != null) {
                    processBean.setProcessRatio(processCPULoadFactor * 100);
                } else {
                    processBean = new ProcessBean(process.getProcessId(), process.getExecutableName(), processCPULoadFactor * 100);
                    processBeanMap.put(new Integer(process.getProcessId()), processBean);
                    processBeanCollection.add(processBean);
                }
                activeProcesses.remove(new Integer(process.getProcessId()));
            }

            // Remove dead instance
            for (Iterator iterator = activeProcesses.iterator(); iterator.hasNext();) {
                Integer deadProcessId = (Integer) iterator.next();
                ProcessBean processBean = (ProcessBean) processBeanMap.remove(deadProcessId);
                processBeanCollection.remove(processBean);
            }
        }
    }

    private static class ProcessBean extends DefaultObservable {
        private int processId;
        private double processRatio;
        private String processName;

        public ProcessBean(int processId, String processName, double processRatio) {
            this.processId = processId;
            this.processName = processName;
            this.processRatio = processRatio;
        }

        public int getProcessId() {
            return processId;
        }

        public void setProcessId(int processId) {
            int oldValue = this.processId;
            this.processId = processId;
            firePropertyChange("processId", oldValue, processId);
        }

        public double getProcessRatio() {
            return processRatio;
        }

        public void setProcessRatio(double processRatio) {
            double oldValue = this.processRatio;
            this.processRatio = processRatio;
            firePropertyChange("processRatio", oldValue, processRatio);
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            Object oldValue = this.processName;
            this.processName = processName;
            firePropertyChange("processName", oldValue, processName);
        }
    }
}
