package org.bhavaya.util;


import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.EmptyStackException;


/**
 * Workaround Java WebStart Bug #4845341.
 * <p/>
 * Your code might need to be signed to have access to the
 * EventQueue.
 * The workaround is activated for 8 seconds if Java WebStart 1.4.2
 * is detected. After 8 seconds it will deactivate itself if it is
 * still active. In that time frame, if a WindowEvent about the
 * Desktop Integration Window is intercepted, the window will be disposed.
 * <p/>
 * Instructions:
 * // Declaration
 * private WorkAround4845341_DesktopIntegration wa;
 * <p/>
 * // Just before showing up your login dialog:
 * wa = new WorkAround4845341_DesktopIntegration();
 * <p/>
 * // Before disposing your login dialog, we need to make sure that we
 * // poped out our EventQueue, in case it is still active. We don't want
 * // our thread to pop out another EventQueue that could be installed on
 * // the EventQueue stack when your app continue to initialize itself.
 * wa.shutdown();
 * <p/>
 * Disclaimer:
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY. USE IT AT YOUR OWN RISKS.
 *
 * @author Martin Miller <martinm (at) jovaco.com>
 * @version 1.2 2003-08-29
 */

public class WorkAround4845341_DesktopIntegration extends Thread {

    private static final long timeToLive = 8 * 1000;
    private boolean shutdown = false;

    private WorkAround4845341_DesktopIntegration.DesktopIntegrationKiller dik = null;


    public WorkAround4845341_DesktopIntegration() {
        setDaemon(true);
        String javawsVersion = System.getProperty("javawebstart.version", "");

        if (javawsVersion.startsWith("javaws-1.4.2")) {
            System.out.println("WA4845341: Detected Java WebStart 1.4.2[_01]");
            try {
                EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                dik = new WorkAround4845341_DesktopIntegration.DesktopIntegrationKiller();
                eq.push(dik);
            } catch (Exception e) {
                System.err.println("WA4845341: Could not get System's EventQueue.");
            }
            start();
        }
    }


    public void run() {
        try {
            sleep(timeToLive);
        } catch (InterruptedException ie) {
        }
        shutdown();
    }


    public void shutdown() {
        if (shutdown == false && dik != null) {
            shutdown = true;
            dik.pop();
        }
    }

    /**
     * This EventQueue looks for WindowsEvents about the Desktop Integration Window.
     * If an event about that window is intercepted, the window will be killed.
     */
    private class DesktopIntegrationKiller extends EventQueue {

        public DesktopIntegrationKiller() {
            System.out.println("WA4845341: Started.");
        }

        protected void dispatchEvent(AWTEvent e) {
            if (e instanceof WindowEvent) {
                WindowEvent we = (WindowEvent) e;
                String name = we.getWindow().getAccessibleContext().getAccessibleName();

                if (name != null) {
                    // Known to work locales: fr, en, and de
                    if (name.endsWith("Integration")) {
                        we.getWindow().setVisible(false);
                        we.getWindow().dispose();
                        System.out.println("WA4845341: Integration Window killed.");
                        WorkAround4845341_DesktopIntegration.this.shutdown();
                        return;
                    }
                }
            }
            super.dispatchEvent(e);
        }

        public void pop() {
            try {
                super.pop();
                System.out.println("WA4845341: Stopped.");
            } catch (EmptyStackException ese) {
            }
        }
    }
}