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

import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.9 $
 */
public class ApplicationInfo extends DefaultObservable {
    private static final Log log = Log.getCategory(ApplicationInfo.class);

    static final String NAME_KEY = "applicationName";
    static final String ID_KEY = "applicationId";
    private static final String VERSION_NUMBER_KEY = "versionNumber";
    private static final String ENVIRONMENT_NAME_KEY = "environmentName";
    static final String ENVIRONMENT_ID_KEY = "environmentId";

    public static final String DEFAULT_ENVIRONMENT_ID = "default";
    public static final String DEFAULT_ID = "default";

    private static final DecimalFormat memoryDecimalFormat = new DecimalFormat("###,###");

    private static ApplicationInfo instance;

    private String id;
    private String name;
    private String versionNumber;
    private String environmentName;
    private String environmentId;

    private boolean loaded;
    private String usedMemory;
    private String allocatedMemory;
    private String maximumMemory;
    private String cpuLoad;
    private String cpuLoadAverage;
    private int numProcessors;
    private String homeDriveSpace;

    private PropertyGroup applicationProperties;
    private java.util.Date startTime;
    private String host;
    private int diagnosticPort;


    public static synchronized ApplicationInfo getInstance() {
        if (instance == null) instance = new ApplicationInfo();
        return instance;
    }

    private ApplicationInfo() {
        startTime = new java.util.Date();

        applicationProperties = ApplicationProperties.getApplicationProperties();
        id = getProperty(applicationProperties, ID_KEY, DEFAULT_ID);
        name = getProperty(applicationProperties, NAME_KEY, "Default");
        versionNumber = getProperty(applicationProperties, VERSION_NUMBER_KEY, "Unknown");
        environmentId = getProperty(applicationProperties, ENVIRONMENT_ID_KEY, DEFAULT_ENVIRONMENT_ID);
        environmentName = getProperty(applicationProperties, ENVIRONMENT_NAME_KEY, "Unknown");


        try {
            setNumProcessors(Runtime.getRuntime().availableProcessors());
        } catch (Throwable e) {
            // No native library - oh well.
            setNumProcessors(-1);
        }

        Timer systemInfoTimer = new Timer("SystemInfoChecker", true);
        systemInfoTimer.scheduleAtFixedRate(new SystemInfoActionListener(), 1000, 1000);
        System.gc();

        try {
            InetAddress localhostInetAddress = InetAddress.getLocalHost();
            host = localhostInetAddress.getCanonicalHostName();
        } catch (Throwable e) {
            log.error(e);
            host = "Unknown";
        }

        try {
            setSystemProperties();
        } catch (Throwable throwable) {
            log.error("Could not set-up logging variables.", throwable);
        }
    }

    private String getProperty(PropertyGroup applicationProperties, String key, String defaultValue) {
        String propertyValue = applicationProperties.getProperty(key);
        if (propertyValue == null || propertyValue.length() == 0) return defaultValue;
        return propertyValue;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    private void setSystemProperties() {
        System.setProperty(ID_KEY, getId());
        System.setProperty(ENVIRONMENT_ID_KEY, getEnvironmentId());
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        boolean oldValue = this.loaded;
        this.loaded = loaded;
        firePropertyChange("loaded", oldValue, loaded);
    }

    public String getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(long usedMemory) {
        String oldValue = this.usedMemory;
        this.usedMemory = convertToMemoryString(usedMemory);
        firePropertyChange("usedMemory", oldValue, this.usedMemory);
    }

    public String getAllocatedMemory() {
        return allocatedMemory;
    }

    public void setAllocatedMemory(long allocatedMemory) {
        String oldValue = this.allocatedMemory;
        this.allocatedMemory = convertToMemoryString(allocatedMemory);
        firePropertyChange("allocatedMemory", oldValue, this.allocatedMemory);
    }

    public String getMaximumMemory() {
        return maximumMemory;
    }

    public void setMaximumMemory(long maximumMemory) {
        String oldValue = this.maximumMemory;
        this.maximumMemory = convertToMemoryString(maximumMemory);
        firePropertyChange("maximumMemory", oldValue, this.maximumMemory);
    }

    private static String convertToMemoryString(long value) {
        return memoryDecimalFormat.format(value / 1000) + "K";
    }

    public String getCpuLoad() {
        return cpuLoad;
    }

    public void setCpuLoad(String cpuLoad) {
        String oldValue = this.cpuLoad;
        this.cpuLoad = cpuLoad;
        firePropertyChange("cpuLoad", oldValue, cpuLoad);
    }

    public String getCpuLoadAverage() {
        return cpuLoadAverage;
    }

    public void setCpuLoadAverage(String cpuLoadAverage) {
        Object oldValue = this.cpuLoadAverage;
        this.cpuLoadAverage = cpuLoadAverage;
        firePropertyChange("cpuLoadAverage", oldValue, cpuLoadAverage);
    }

    public int getNumProcessors() {
        return numProcessors;
    }

    public void setNumProcessors(int numProcesses) {
        int oldValue = this.numProcessors;
        this.numProcessors = numProcesses;
        firePropertyChange("numProcessors", oldValue, numProcesses);
    }

    public java.util.Date getStartTime() {
        return startTime;
    }

    public String getUsername() {
        return System.getProperty("user.name");
    }

    public String getLoadedConfigFileName() {
        return Configuration.getLoadedConfigFileName(Configuration.DEFAULT);
    }

    private class SystemInfoActionListener extends TimerTask {
        private MemoryWatchdog memoryWatchdog;
        private NumberFormat numberFormat = new DecimalFormat("00");
        private int counter = 0;
//        private Environment.DiskSpaceInfo diskSpaceInfo = new Environment.DiskSpaceInfo();

        public SystemInfoActionListener() {
            memoryWatchdog = MemoryWatchdog.getInstance();
            setMaximumMemory(memoryWatchdog.getMaxMemory());

            try {
                setHomeDriveSpace(Environment.getDiskSpaceInfo(IOUtilities.getUserBaseDirectory()).toString());
            } catch (Exception e) {
                setHomeDriveSpace("Unknown");
            }
        }

        public void run() {
            setUsedMemory(memoryWatchdog.getUsedMemory());
            setAllocatedMemory(memoryWatchdog.getAllocatedMemory());
            setCpuLoad(numberFormat.format((CpuLoad.getInstance().getLoad() * 100.0)) + "%");
            setCpuLoadAverage(numberFormat.format(CpuLoad.getInstance().getLoadAverage() * 100.0) + "%");

            counter = (++counter)%60;
            if (counter == 0) { // check this only every minute
                setHomeDriveSpace(Environment.getDiskSpaceInfo(IOUtilities.getUserBaseDirectory()).toString());
            }
        }
    }

    public String getHost() {
        return host;
    }

    public String getDiagnosticUrl() {
        if (diagnosticPort == 0) return "Application not JMX-enabled";
        return "http://" + getHost() + ":" + getDiagnosticPort() + "/";
    }

    public int getDiagnosticPort() {
        return diagnosticPort;
    }

    public void setDiagnosticPort(int diagnosticPort) {
        int oldValue = this.diagnosticPort;
        this.diagnosticPort = diagnosticPort;
        firePropertyChange("diagnosticPort", oldValue, diagnosticPort);
    }

    public String getHomeDriveSpace() {
        return homeDriveSpace;
    }

    public void setHomeDriveSpace(String newValue) {
        String oldValue = homeDriveSpace;
        homeDriveSpace = newValue;
        firePropertyChange("homeDriveSpace", oldValue, newValue);
    }
}
