package org.bhavaya.ui.diagnostics;

/**
 * Description
 *
 * @author Dan
 * @version $Revision: 1.2 $
 */
public interface MonitorDumpMBean {
    public String getMonitorDump();

    void requestDump();
}
