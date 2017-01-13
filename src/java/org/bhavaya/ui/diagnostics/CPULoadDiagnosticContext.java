package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.MenuGroup;
import org.bhavaya.util.CpuLoad;
import org.bhavaya.util.NativeProcess;
import org.bhavaya.util.Utilities;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;


/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class CPULoadDiagnosticContext extends DiagnosticContext {
    public CPULoadDiagnosticContext() {
        super("Processes", null);
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }

    public Component createComponent() {
        return null;
    }

    public String createHTMLDescription() {
        StringBuffer buffer = new StringBuffer();
        DiagnosticUtilities.contextHeader(buffer, "Process CPU Loads");
        DiagnosticUtilities.tableHeader(buffer);

        NativeProcess[] nativeProcesses = NativeProcess.getAllProcesses();
        ArrayList processBeanList = new ArrayList();
        for (int i = 0; i < nativeProcesses.length; i++) {
            NativeProcess nativeProcess = nativeProcesses[i];
            double processLoadFactor = CpuLoad.getInstance().getProcessCPULoadFactor(nativeProcess.getProcessId());
            String processName = nativeProcess.getExecutableName();
            processBeanList.add(new ProcessBean(processName, processLoadFactor));
        }
        Collections.sort(processBeanList, new Comparator() {
            public int compare(Object o1, Object o2) {
                ProcessBean bean1 = (ProcessBean) o1;
                ProcessBean bean2 = (ProcessBean) o2;
                return Utilities.compare(new Double(bean2.getProcessRatio()), new Double(bean1.getProcessRatio()));
            }
        });

        for (Iterator iterator = processBeanList.iterator(); iterator.hasNext();) {
            ProcessBean processBean = (ProcessBean) iterator.next();
            DiagnosticUtilities.tableRow(buffer, new Object[]{processBean.getProcessName(), new Double(processBean.getProcessRatio() * 100d)});
        }

        DiagnosticUtilities.tableFooter(buffer);

        return buffer.toString();
    }

    public MenuGroup[] createMenuGroups() {
        return new MenuGroup[0];
    }

    private static class ProcessBean {
        private double processRatio;
        private String processName;

        public ProcessBean(String processName, double processRatio) {
            this.processName = processName;
            this.processRatio = processRatio;
        }

        public String getProcessName() {
            return processName;
        }

        public double getProcessRatio() {
            return processRatio;
        }
    }
}
