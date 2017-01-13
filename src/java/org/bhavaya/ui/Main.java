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

package org.bhavaya.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.ui.diagnostics.EventQueueWatchdog;
import org.bhavaya.ui.diagnostics.Profiler;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.ApplicationInfo;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Configuration;
import org.bhavaya.util.ConfigurationPolicy;
import org.bhavaya.util.Log;

/**
 * The class that gets it all going; has a main() method which starts the app.
 *
 * @author Brendon McLean
 * @version $Revision: 1.32 $
 */

public class Main {
    public static void main(String[] args) {
        try {
            System.setSecurityManager(null); // for Webstart, otherwise we get a FilePermissionException on JFileChooser
            
            installCustomEventQueue();
            
            // Config selection popup required here...
            getAppDefinedConfigurationManagementPolicy();
            Configuration.configureDefaultSourcesAndSinks();

            Thread.currentThread().setContextClassLoader(ClassUtilities.getApplicationClassLoader());
            BeanFactory.setPostInited(false); // this ensure that postInit happens explicity when BeanFactory.postInitAllBeanFactoryTypes() called in ApplicationContext.postStart()

            ApplicationContext.showSplashScreen();
                  
            // Move logging initialisation to here to fix issue caused by config loading.
            Log.getPrimaryLoadingLog().info("Loading Bhavaya libraries");
            Log.getSecondaryLoadingLog().info("");


            final ApplicationContext applicationContext = ApplicationContext.getInstance();
            final ApplicationInfo applicationInfo = ApplicationInfo.getInstance();

            final Log log = Log.getCategory(Main.class); // get log after setting thread's context class loader
            String startingMessage = "Starting " + applicationInfo.getName() + ", version: "
                    + applicationInfo.getVersionNumber() + ", environment: "
                    + applicationInfo.getEnvironmentName();
            log.info(startingMessage);
            Log.getSecondaryLoadingLog().info(startingMessage);

            Workspace.getInstance().installLookAndFeelManager(applicationContext.getLookAndFeelManager());
            applicationContext.authenticate();
            
            ApplicationContext.splashScreenToFront();
            
            Profiler.Task task = Profiler.taskStarted(Profiler.MAJOR_METHOD, "prepareStart");
            applicationContext.prepareStart();
            Profiler.taskStopped(task);

            final GenericWindow applicationWindow = Workspace.getInstance().getApplicationFrame();
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    log.info("Showing application frame");
                    Profiler.Task task = Profiler.taskStarted(Profiler.MAJOR_METHOD, "Showing application frame");
                    Workspace.getInstance().activateState();
                    Profiler.taskStopped(task);
                    ApplicationContext.hideSplashScreen();
                    Workspace.getInstance().checkViewActivation();
                }
            });

            task = Profiler.taskStarted(Profiler.MAJOR_METHOD, "postStart");
            applicationContext.postStart();
            Profiler.taskStopped(task);

            log.info("Startup thread exiting");
        } catch (Throwable e) {
            JFrame exceptionFrame = new ExceptionPanel("Application Exception", "Encountered an unexpected error during startup.", e);
            UIUtilities.centreInScreen(exceptionFrame, 0, 0);
            exceptionFrame.show();
            Log.getCategory(Main.class).error("Exception during startup", e);
        }
    }

    private static void installCustomEventQueue() {

        // Owing to some quirks with Java Web Start, EDTs, RMI and the Imp of the Perverse, add this now.
        if (System.getProperty("monitorEventQueue") != null) {

            //use bhavaya's own EventQueueWatchdog as the default, apps may now specify their own alternative
            EventQueueWatchdog defaultEventQueue = new EventQueueWatchdog();
            getApplicationDefinedEventQueue(defaultEventQueue).install();
        }
    }

    private static EventQueueWatchdog getApplicationDefinedEventQueue(EventQueueWatchdog defaultQueue) {
        EventQueueWatchdog result = defaultQueue;
        String eventQueueClass = System.getProperty("eventQueueClass");
        if ( eventQueueClass != null ) {
            try {
                result = (EventQueueWatchdog)Class.forName(eventQueueClass).newInstance();
            } catch ( Throwable t) {
                System.err.println("Failed to install eventQueueClass " + eventQueueClass);
                t.printStackTrace();
            }
        }
        return result;
    }
    
    
    private static void getAppDefinedConfigurationManagementPolicy() {
    	ConfigurationPolicy policy = null;
        String configPolicyClass = System.getProperty("configurationManagementPolicy");
        if ( configPolicyClass != null ) {
            try {
            	policy = (ConfigurationPolicy)Class.forName(configPolicyClass).newInstance();
            	policy.selectConfiguration();
            } catch ( Throwable t) {
                System.err.println("Failed to execute Configuration Management Policy " + configPolicyClass);
                t.printStackTrace();
            }
        }
    }
    
    public static class LoadingComponent extends Box {
        public LoadingComponent() {
            super(BoxLayout.Y_AXIS);
            MessageLabel primaryLabel = new MessageLabel("Courier New", Font.PLAIN, 11.0f);
            MessageLabel secondaryLabel = new MessageLabel("Courier New", Font.PLAIN, 10.0f);
            add(primaryLabel, BorderLayout.NORTH);
            add(secondaryLabel, BorderLayout.SOUTH);
            Log.getPrimaryLoadingLog().addListener(primaryLabel);
            Log.getSecondaryLoadingLog().addListener(secondaryLabel);
        }
    }

}
