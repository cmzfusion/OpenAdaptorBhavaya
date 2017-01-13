package org.bhavaya.ui.diagnostics;

import org.bhavaya.util.Environment;
import org.bhavaya.util.Utilities;

/**
 * Deprecated.  See ThreadDiagnosticContext
 *
 * @author Dan
 * @version $Revision: 1.8 $
 */
@Deprecated
public class MonitorDump implements MonitorDumpMBean {
    private String monitorDumpSnapShot = "Please press \"Request Dump\" to show thread dump.";

    @Deprecated
    public String getMonitorDump() {
        return monitorDumpSnapShot;
    }

    @Deprecated
    public void requestDump() {
        final StringBuffer buf = new StringBuffer();
        Environment.nativeRequestThreadDump(buf);
        monitorDumpSnapShot = "View the source of this page for a formatted dump\n" + buf;
        Thread emailThread = Utilities.newThread(new Runnable() {
            public void run() {
                HeadlessApplicationDiagnostics.getHeadlessInstance().sendQuickDiagnosticMessage("Monitor Dump", buf.toString(), false);
            }
        }, "EmailMonitorDump", true);
        emailThread.start();
    }
}
