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

package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.ApplicationContext;
import org.bhavaya.ui.ArrayListModel;
import org.bhavaya.ui.table.ExclusionColumnCalculator;
import org.bhavaya.ui.table.ChainableFilteredTableModel;
import org.bhavaya.ui.view.TableView;
import org.bhavaya.util.ApplicationInfo;
import org.bhavaya.util.KeyValuePair;
import org.bhavaya.util.Log;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.13.6.1 $
 */
public class ApplicationDiagnosticsMBeanAdaptor implements ApplicationDiagnosticsMBeanAdaptorMBean {
    private static final Log log = Log.getCategory(ApplicationDiagnosticsMBeanAdaptor.class);


    public ApplicationDiagnosticsMBeanAdaptor() {
    }

    public boolean isLoaded() {
        return ApplicationInfo.getInstance().isLoaded();
    }

    public void saveConfiguration() {
        ApplicationContext.getInstance().saveConfiguration();
    }

    public String getApplicationName() {
        return ApplicationInfo.getInstance().getName();
    }

    public String getVersionNumber() {
        return ApplicationInfo.getInstance().getVersionNumber();
    }

    public String getEnvironmentName() {
        return ApplicationInfo.getInstance().getEnvironmentName();
    }

    public Date getStartTime() {
        return ApplicationInfo.getInstance().getStartTime();
    }

    public String getUsername() {
        return ApplicationInfo.getInstance().getUsername();
    }

    public String getLoadedConfigFileName() {
        return ApplicationInfo.getInstance().getLoadedConfigFileName();
    }

    public String getUsedMemory() {
        return ApplicationInfo.getInstance().getUsedMemory();
    }

    public String getAllocatedMemory() {
        return ApplicationInfo.getInstance().getAllocatedMemory();
    }

    public String getMaximumMemory() {
        return ApplicationInfo.getInstance().getMaximumMemory();
    }

    public String getCpuLoad() {
        return ApplicationInfo.getInstance().getCpuLoad();
    }

    public String getCpuLoadAverage() {
        return ApplicationInfo.getInstance().getCpuLoadAverage();
    }

    public int getNumProcessors() {
        return ApplicationInfo.getInstance().getNumProcessors();
    }

    public boolean isLogBeanToScreenDelays() {
        return HeadlessApplicationDiagnostics.getHeadlessInstance().isLogBeanToScreenDelays();
    }

    public void setLogBeanToScreenDelays(boolean logBeanToScreenDelays) {
        HeadlessApplicationDiagnostics.getHeadlessInstance().setLogBeanToScreenDelays(logBeanToScreenDelays);
    }

    public boolean isTableHighlighterDebuggingEnabled() {
        return TableView.isEnableHighlighterDebugging();
    }

    public void setTableHighlighterDebuggingEnabled(boolean enabled) {
        TableView.setEnableHighlighterDebugging(enabled);
    }

    public void setDebugLoggingEnabled(boolean selected) {
        HeadlessApplicationDiagnostics.getHeadlessInstance().setDebugLoggingEnabled(selected);
    }

    public boolean isDebugLoggingEnabled() {
        return HeadlessApplicationDiagnostics.getHeadlessInstance().isDebugLoggingEnabled();
    }

    public KeyValuePair[] getSystemProperties() {
        return SystemPropertiesDiagnosticContext.getSystemProperties();
    }

    public void saveConfigurationAndExit() {
        log.info("MBean initiated shutdown: saveConfigurationAndExit()");
        ApplicationContext.getInstance().exit(true, false);
    }

    public void exit() {
        log.info("MBean initiated shutdown: exit()");
        ApplicationContext.getInstance().exit(false, false);
    }

    public void sendDiagnosticReport(String detail) {
        HeadlessApplicationDiagnostics.getHeadlessInstance().sendDiagnosticReport(detail);
    }

    public String searchLogsByLine(String searchPattern) {
        return HeadlessApplicationDiagnostics.getHeadlessInstance().searchLogFilesByLine(searchPattern).toString();
    }

    public String searchLogsByLine(String fileNamePattern, String searchPattern) {
        return HeadlessApplicationDiagnostics.getHeadlessInstance().searchLogFilesByLine(fileNamePattern, searchPattern).toString();
    }

    public void sendDiagnosticReport(String detail, String emailAddress) {
        HeadlessApplicationDiagnostics.getHeadlessInstance().sendDiagnosticReport(detail, emailAddress);        
    }

    public void sendScreenCapture() {
        ApplicationDiagnostics.getInstance().sendScreenCapture();
    }

    public ApplicationDiagnostics.LoggingEventBean[] getExceptionLogStatements() {
        List exceptionLogStatements = HeadlessApplicationDiagnostics.getHeadlessInstance().getExceptionLogStatements();
        return (ApplicationDiagnostics.LoggingEventBean[]) exceptionLogStatements.toArray(new ApplicationDiagnostics.LoggingEventBean[exceptionLogStatements.size()]);
    }

    public ApplicationDiagnostics.LoggingEventBean[] getLogStatements() {
        ArrayListModel allLogStatements = HeadlessApplicationDiagnostics.getHeadlessInstance().getAllLogStatements();
        return (ApplicationDiagnostics.LoggingEventBean[]) allLogStatements.toArray(new ApplicationDiagnostics.LoggingEventBean[allLogStatements.size()]);
    }

    public ApplicationDiagnostics.LoggingEventBean[] getWarnLogStatements() {
        ArrayListModel warnLogStatements = HeadlessApplicationDiagnostics.getHeadlessInstance().getWarnLogStatements();
        return (ApplicationDiagnostics.LoggingEventBean[]) warnLogStatements.toArray(new ApplicationDiagnostics.LoggingEventBean[warnLogStatements.size()]);
    }

    public ApplicationDiagnostics.LoggingEventBean[] getErrorLogStatements() {
        ArrayListModel errorLogStatements = HeadlessApplicationDiagnostics.getHeadlessInstance().getErrorLogStatements();
        return (ApplicationDiagnostics.LoggingEventBean[]) errorLogStatements.toArray(new ApplicationDiagnostics.LoggingEventBean[errorLogStatements.size()]);
    }

    public String[] getColumnKeysDynamicallyExcludedFromFiltering() {
        return ChainableFilteredTableModel.getDynamicallyExcludedColumnKeys().toArray(new String[ChainableFilteredTableModel.getDynamicallyExcludedColumnKeys().size()]);
    }

    public BeanFactoryDiagnosticContext.BeanFactoryStatistics[] getBeanFactoriesStatistics() {
        return BeanFactoryDiagnosticContext.getBeanFactoriesStatistics();
    }

    public void sendLogFiles(String detail) {
        HeadlessApplicationDiagnostics.getHeadlessInstance().sendLogFiles(detail);
    }

    public void sendLogFiles(String detail, String emailAddress) {
        HeadlessApplicationDiagnostics.getHeadlessInstance().sendLogFiles(detail, emailAddress);
    }

    public String getHomeDriveSpace() {
        return ApplicationInfo.getInstance().getHomeDriveSpace();
    }

    public void runGC() {
        System.gc();
    }

    //ahem not yet 'all' commands if we're honest..but at least the all makes it show up first alphabetically!
    public String allCommandsHelp() {
        StringBuffer sb = new StringBuffer();
        sb.append("<style type='text/css'>td { padding: 5px, 10px, 0px, 10px } </style>");
        sb.append("<table>");
        sb.append("<tr><th>function:</th><th>arguments</th><th>Description</th></tr>");
        sb.append("<tr><td>searchLogs</td><td>(file name pattern) search pattern (replacement pattern)</td><td>Search all log files for the given pattern which may span multiple log lines, optionally replacing pattern with the given replacement</td></tr>");
        sb.append("<tr><td>searchLogsByLine</td><td>(file name pattern) search pattern (replacement pattern)</td><td>Search all log files for log lines which contain the specified pattern</td></tr>");
        sb.append("</table>");
        return sb.toString();
    }
}
