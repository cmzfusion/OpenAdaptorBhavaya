package org.bhavaya.util;

import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.ui.diagnostics.ThreadDiagnosticContext;

import java.util.TimerTask;

/**
 * Code to try and prevent the hanging process issue
 * User: ga2mop0
 * Date: 06/02/15
 * Time: 16:45
 */
public class SystemExitController {

    private static final Log log = Log.getCategory(SystemExitController.class);

    public static void exit(int exitStatus) {
        checkSecurityManager();
        Runtime.runFinalizersOnExit(false);
        scheduleHalt(exitStatus, 60000);
        System.exit(exitStatus);
    }

    public static void checkSecurityManager() {
        if(System.getSecurityManager() != null) {
            log.warn("SecurityManager should be null - resetting");
            System.setSecurityManager(null);
        }
    }

    public static void scheduleHalt(final int exitStatus, final long maxDelay) {
        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    log.warn("Application is still running "+maxDelay+"ms after exit called");
                    log.warn("Performing thread dump");
                    ThreadDiagnosticContext.getInstance().performThreadDump();
                    log.warn("Sending diagnostic report");
                    ApplicationDiagnostics.getInstance().sendDiagnosticReport("Client failed to close down");
                    log.warn("Now calling Runtime.halt()");
                    Runtime.getRuntime().halt(exitStatus);
                    log.error("Application still active after Runtime.halt() called");
                } catch (Throwable t) {
                    log.error("Error trying to shutdown", t);
                }
            }
        }, maxDelay);
    }


}
