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

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.Schema;
import org.bhavaya.coms.NotificationSubscriber;
import org.bhavaya.db.DataSourceFactory;
import org.bhavaya.db.MetaDataSource;
import org.bhavaya.ui.diagnostics.*;
import org.bhavaya.ui.table.CachedObjectGraph;
import org.bhavaya.ui.table.UserPriorityExecutionManager;
import org.bhavaya.ui.view.DefaultViewContext;
import org.bhavaya.ui.view.View;
import org.bhavaya.ui.view.ViewContext;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.48.4.1 $
 */

public abstract class ApplicationContext extends DefaultObservable {
    private static final Log log = Log.getCategory(ApplicationContext.class);

    private static final String SPLASH_SCREEN_IMAGE_KEY = "splashScreenImage";
    private static final String SPLASH_SCREEN_LOGO_IMAGE_KEY = "splashScreenLogoImage";
    private static final String APPLICATION_ICON_KEY = "frameIcon";

    protected static final String[] DEFAULT_APPLICATION_DESCRIPTIONS = new String[]{"Username", "Application Name", "Environment Name", "Version Number", "Start Time", "Configuration",
                                                                                  "Diagnostic Url",
                                                                                  "Used Memory", "Allocated Memory", "Maximum Memory", "Cpu Load (2.5s)", "Cpu Load (1min)", "Number of Processors", "Home Drive Space"};
    protected static final String[] DEFAULT_APPLICATION_PROPERTY_NAMES = new String[]{"username", "name", "environmentName", "versionNumber", "startTime", "loadedConfigFileName",
                                                                                    "diagnosticUrl",
                                                                                    "usedMemory", "allocatedMemory", "maximumMemory", "cpuLoad", "cpuLoadAverage", "numProcessors", "homeDriveSpace"};

    protected static final String[] NOTIFICATION_INFO_DESCRIPTIONS = new String[]{"Sql Notification Subject", "Realtime", "Notifications Received", "Notifications Processed"};
    protected static final String[] NOTIFICATION_INFO_PROPERTY_NAMES = new String[]{"sqlNotificationSubject", "realtime", "numberOfReceivedNotifications", "numberOfProcessedNotifications"};

    private static final Object lock = new Object();
    private static final PropertyGroup applicationProperties = ApplicationProperties.getApplicationProperties();
    private static final ImageIcon SAVE_ICON = ImageIconCache.getImageIcon("save_32.gif");

    protected static final ImageIcon INFO_ICON = ImageIconCache.getImageIcon("information.png");
    protected static final ImageIcon WARN_ICON = ImageIconCache.getImageIcon("warning.png");
    protected static final ImageIcon ERROR_ICON = ImageIconCache.getImageIcon("error.png");

    private static ApplicationContext instance;
    private static SplashScreen splashScreen;

    private ImageIcon applicationIcon;
    private Configuration configuration;

    private ApplicationDiagnostics applicationDiagnostics;
    private LookAndFeelManager lookAndFeelManager;

    private PriorityTaskQueue taskQueue;
    private ErrorLogFrame errorLogFrame;

    private boolean useSubclassMappingCache = false;
    private boolean useMetaDataCache = false;

    private UserPriorityExecutionManager executionManager;

    private Profiler.Task startupMetaTask;


    public static final ApplicationContext getInstance() {
        synchronized (lock) {
            if (instance == null) {
                String contextManagerClassName = applicationProperties.getMandatoryProperty("applicationContextClass");
                log.info("Retrieving application context: " + contextManagerClassName);
                try {
                    instance = (ApplicationContext) ClassUtilities.getClass(contextManagerClassName).newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Could not find an ApplicationContext subclass. Check application properties exist and runtime flags is set", e);
                }
            }
        }
        return instance;
    }

    protected ApplicationContext() throws Exception {
        // Redirect AWT errors to this class
        System.setProperty("sun.awt.exception.handler", "org.bhavaya.ui.ExceptionHandler");

        String applicationIconProperty = applicationProperties.getProperty(APPLICATION_ICON_KEY);
        applicationIcon = ImageIconCache.getImageIcon(applicationIconProperty != null ? applicationIconProperty : "default_frame_icon.gif");

        String subclassMappingValue = applicationProperties.getProperty("useSubclassMappingCache");
        useSubclassMappingCache = subclassMappingValue != null && Boolean.valueOf(subclassMappingValue).booleanValue();

        String metaDataMappingValue = applicationProperties.getProperty("useMetaDataCache");
        useMetaDataCache = metaDataMappingValue != null && Boolean.valueOf(metaDataMappingValue).booleanValue();

        initApplicationDiagnositics();

        if (useMetaDataCache) {
            MetaDataSource.setVersion(Long.toString(Configuration.getExpectedVersion()));
        }

        if (useSubclassMappingCache) {
            PropertyModel.setVersion(Long.toString(Configuration.getExpectedVersion()));
        }

        taskQueue = new PriorityTaskQueue("ApplicationContext", true);

        executionManager = new UserPriorityExecutionManager();
    }

    protected void initApplicationDiagnositics() {
        applicationDiagnostics = ApplicationDiagnostics.getInstance(); // construct early as its constructs adds a listener on Log
        applicationDiagnostics.addDiagnosticContext(new VisualLogDiagnosticContext("Errors", ERROR_ICON, ApplicationDiagnostics.getInstance().getErrorLogStatements()));
        applicationDiagnostics.addDiagnosticContext(new VisualLogDiagnosticContext("Warnings", WARN_ICON, ApplicationDiagnostics.getInstance().getWarnLogStatements()));
        applicationDiagnostics.addDiagnosticContext(new VisualLogDiagnosticContext("Last 5000 Log Messages", INFO_ICON, ApplicationDiagnostics.getInstance().getAllLogStatements()));
        applicationDiagnostics.addDiagnosticContext(new BeanInfoDiagnosticContext("Application Info", null, ApplicationInfo.getInstance(),
                DEFAULT_APPLICATION_DESCRIPTIONS, DEFAULT_APPLICATION_PROPERTY_NAMES));

        String[] sqlNotificationSubjects = NotificationSubscriber.getSubjects();
        for (int i = 0; i < sqlNotificationSubjects.length; i++) {
            String sqlNotificationSubject = sqlNotificationSubjects[i];
            NotificationInfo notificationInfo = new NotificationInfo(sqlNotificationSubject);
            applicationDiagnostics.addDiagnosticContext(new BeanInfoDiagnosticContext("Notifications: " + sqlNotificationSubject, null,
                    notificationInfo, NOTIFICATION_INFO_DESCRIPTIONS, NOTIFICATION_INFO_PROPERTY_NAMES));
        }
        applicationDiagnostics.addDiagnosticContext(new SystemPropertiesDiagnosticContext("System Properties", null));
        applicationDiagnostics.addDiagnosticContext(new LogControlDiagnosticContext());
    }


    /**
     * @deprecated use ErrorLogFrame class directly to initialize and display error log window
     */
    protected void useErrorLogFrame() {
        errorLogFrame = new ErrorLogFrame();
        errorLogFrame.setIconImage(getApplicationIcon().getImage());
    }

    /**
     * @deprecated use ErrorLogFrame class directly to display error log window
     */
    protected void showErrorLogFrame() {
        if (errorLogFrame != null) UIUtilities.runInDispatchThread(new Runnable() {
            public void run() {
                errorLogFrame.setVisible(true);
            }
        });
    }

    public ImageIcon getApplicationIcon() {
        return applicationIcon;
    }

    protected boolean isErrorLogPopup() {
        return false;
    }

    /**
     * Called before any GUI is setup.
     */
    protected void authenticate() throws Exception {
    }

    public void prepareStart() {
        log.info("prepareStart");

        startupMetaTask = Profiler.metaTaskStarted(Profiler.STARTUP);

        // This is to force some heavy weight initialisation during init
        Profiler.Task subTask = Profiler.taskStarted(Profiler.MAJOR_METHOD, "Schema initilisation");
        Schema.getInstances();
        Profiler.taskStopped(subTask);

        subTask = Profiler.taskStarted(Profiler.MAJOR_METHOD, "Property Model Initialisation");
        PropertyModel.init();
        Profiler.taskStopped(subTask);

        setCompulsoryCriteria();

        // Touch the singleton to register it with save events.  Otherwise can lose all view configurations
        TableViewConfigurationMap.init();
    }

    /**
     * Override this to set the compulsory criteria for a bean type.
     * These will be automatically used to restrict any criteria collection created.
     */
    protected void setCompulsoryCriteria() {
        // override me
    }

    public final void postStart() {
        // Initial tasks are added to the default task queue
        // Ensure that poststart task gets run after other queued tasks.
        taskQueue.addTask(new Task("Post start") {
            public void run() {
                log.info("postStart");
                Profiler.metaTaskStopped(startupMetaTask);
                Log.getUserCategory().info("Application initialisation complete");
                postStartImpl();
                ApplicationInfo.getInstance().setLoaded(true);
            }
        }, PriorityTaskQueue.PRIORITY_LOW);
        taskQueue.start();
    }

    protected void postStartImpl() {
        if (useMetaDataCache) {
            MetaDataSource.writeMetaDataToFile();
        }

        if (useSubclassMappingCache) {
            PropertyModel.writeSubclassMappingsToFile();
        }

        BeanFactory.postInitAllBeanFactoryTypes();
        MemoryWatchdog.getInstance().addMemoryLowListener(new MemoryLowListener());
        MemoryWatchdog.getInstance().start();
    }

    public static String getSplashScreenFilename() {
        return applicationProperties.getProperty(SPLASH_SCREEN_IMAGE_KEY);
    }

    public static String getSplashScreenLogoFilename() {
        return applicationProperties.getProperty(SPLASH_SCREEN_LOGO_IMAGE_KEY);
    }

    public void prepareExit() {
        log.info("prepareExit");

        UIUtilities.runTaskWithProgressDialog("Exiting Application", "Closing connections...", new Runnable() {
            public void run() {
                log.info("Shutting down database connections");
                NotificationSubscriber[] notificationSubscribers = NotificationSubscriber.getInstances();
                for (int i = 0; i < notificationSubscribers.length; i++) {
                    notificationSubscribers[i].close();
                }
                log.info("Shutting down database connections");
                DataSourceFactory.closeAll();
            }
        });
    }

    public void addGuiTask(Task task) {
        addGuiTask(task, PriorityTaskQueue.PRIORITY_NORMAL);
    }

    public void addGuiTask(Task task, PriorityTaskQueue.Priority priority) {
        taskQueue.addTask(task, priority);
    }

    public boolean setGuiTaskPriority(Task task, PriorityTaskQueue.Priority priority) {
        return taskQueue.adjustTaskPriority(task, priority);
    }

    public void saveConfiguration() {
        Task[] saveTasks = Configuration.getSaveTasks();
        try {
            UIUtilities.runTasksWithProgressDialog("Saving configuration", saveTasks, Thread.NORM_PRIORITY, true);
        } catch (Throwable t) {
            log.error("Error saving configuration", t);
            JOptionPane.showMessageDialog(getApplicationView().getComponent(), "Error saving configuration");
        }
    }

    public static void showSplashScreen() {
        try {
            splashScreen = createSplashScreen();
            splashScreen.setVisible(true);
        } catch (Exception e) {
            log.warn("No splash screen found", e);
        }
    }

    private static SplashScreen createSplashScreen() {
        return new SplashScreen(IOUtilities.getResource(getSplashScreenFilename()), IOUtilities.getResource(getSplashScreenLogoFilename()), new Main.LoadingComponent());
    }

    public static void hideSplashScreen() {
        if (splashScreen != null) {
            splashScreen.dispose();
            splashScreen = null;
        }
    }

    public static void splashScreenToFront() {
        if (splashScreen != null) {
            splashScreen.toFront();
        }
    }

    public static SplashScreen getSplashScreen() {
        return splashScreen;
    }

    public abstract View getApplicationView();

    public ViewContext createViewContext(View view) {
        return new DefaultViewContext(view);
    }

    public Configuration getConfiguration() {
        if (configuration == null) {
            configuration = Configuration.getRoot();
        }
        return configuration;
    }

    public LookAndFeelManager getLookAndFeelManager() {
        if (lookAndFeelManager == null) {
            lookAndFeelManager = createLookAndFeelManager();
        }
        return lookAndFeelManager;
    }

    protected LookAndFeelManager createLookAndFeelManager() {
        return new LookAndFeelManager();
    }


    /**
     * Exit, will save the configuration if the application had completed loading
     *
     * @param showOptionPane
     */
    public final void exit(boolean showOptionPane) {
        exit(ApplicationInfo.getInstance().isLoaded(), showOptionPane);
    }

    public final void exit(boolean saveConfiguration, boolean showOptionPane) {
        Component owner = Workspace.getInstance().getApplicationFrame().getContentPane();

        int ret = JOptionPane.CANCEL_OPTION;
        if (showOptionPane) {
            ret = AlwaysOnTopJOptionPane.showConfirmDialog(owner, "Exit the application?", "Exit", JOptionPane.OK_CANCEL_OPTION);
        }
        if (!showOptionPane || ret == JOptionPane.OK_OPTION) {
            try {
                if (saveConfiguration) saveConfiguration();
                prepareExit();
                log.info("User has successfully exited the application");
                SystemExitController.exit(0);
            } catch (Exception e) {
                log.error("Error exiting application", e);
                JOptionPane.showMessageDialog(Workspace.getInstance().getApplicationFrame().getContentPane(),
                        "There was an error exiting application.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                SystemExitController.exit(-1);
            }
        }
    }

    public CachedObjectGraph.ExecutionController createCachedObjectGraphExecutionController() {
        return executionManager.createExecutionController();
    }

    protected final class SaveAction extends AuditedAbstractAction {
        public SaveAction(boolean useIcon) {
            putValue(Action.NAME, "Save");
            putValue(Action.SHORT_DESCRIPTION, "Save");
            putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
            if (useIcon) {
                putValue(Action.SMALL_ICON, SAVE_ICON);
            }
        }

        public void auditedActionPerformed(ActionEvent e) {
            saveConfiguration();
        }
    }


    protected class ExitAction extends AuditedAbstractAction {
        public ExitAction() {
            putValue(Action.NAME, "Exit");
        }

        public void auditedActionPerformed(ActionEvent e) {
            exit(true, true);
        }
    }

    protected class ExitWithoutSavingAction extends AuditedAbstractAction {
        public ExitWithoutSavingAction() {
            putValue(Action.NAME, "Exit without saving");
        }

        public void auditedActionPerformed(ActionEvent e) {
            exit(false, true);
        }
    }

    private class MemoryLowListener implements MemoryWatchdog.MemoryLowListener {
        private static final String MEMORY_LOW_ERROR_MESSAGE = "Your system is running low on available memory.\n"
                + "The data is no longer live.\n"
                + "Your configuration has been saved.\n"
                + "You should restart the application.";

        public void memoryLow() {
            MemoryWatchdog.getInstance().removeMemoryLowListener(this);
            NotificationSubscriber[] notificationSubscribers = NotificationSubscriber.getInstances();
            for (int i = 0; i < notificationSubscribers.length; i++) {
                notificationSubscribers[i].close();
            }
            saveConfiguration();
            JOptionPane.showMessageDialog(Workspace.getInstance().getApplicationFrame().getContentPane(), MEMORY_LOW_ERROR_MESSAGE, "Memory Low", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected final class DeleteConfigurationAndExitAction extends AuditedAbstractAction {
        public DeleteConfigurationAndExitAction() {
            putValue(Action.NAME, "Delete Configuration and Exit");
        }

        public void auditedActionPerformed(ActionEvent e) {
            Component owner = Workspace.getInstance().getApplicationFrame().getContentPane();

            int ret = JOptionPane.showConfirmDialog(owner, "Delete configuration (warning this is cannot be undone) and exit the application?", "Delete and exit", JOptionPane.OK_CANCEL_OPTION);

            if (ret == JOptionPane.OK_OPTION) {
                try {
                    log.info("Deleting user's config directory: " + IOUtilities.getUserConfigDirectory());
                    IOUtilities.delete(new File(IOUtilities.getUserConfigDirectory()));

                    String oldConfigDir = IOUtilities.getOldUserConfigDirectory();
                    if (oldConfigDir != null) {
                        log.info("Deleting user's config directory: " + oldConfigDir);
                        IOUtilities.delete(new File(oldConfigDir));
                    }

                    log.info("User has successfully exited the application");
                    SystemExitController.exit(0);
                } catch (Exception ex) {
                    log.error("Error exiting application", ex);
                    JOptionPane.showMessageDialog(Workspace.getInstance().getApplicationFrame().getContentPane(),
                            "There was an error exiting application.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    SystemExitController.exit(-1);
                }
            }
        }
    }

    protected final class AboutAction extends AuditedAbstractAction {
        public AboutAction() {
            putValue(Action.NAME, "About...");
            putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
        }

        public void auditedActionPerformed(ActionEvent e) {
            Container owner = UIUtilities.getWindowParent((Component) e.getSource());
            new AboutFrame(owner, "About " + ApplicationInfo.getInstance().getName());
        }
    }


    protected final class DiagnosticsAction extends AuditedAbstractAction {
        public DiagnosticsAction() {
            putValue(Action.NAME, "Diagnostics...");
        }

        public void auditedActionPerformed(ActionEvent e) {
            applicationDiagnostics.showFrame();
        }
    }
}