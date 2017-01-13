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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.varia.NullAppender;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Currently a API shielding class that shields Bhavaya from making a decision on whether to use
 * Log4J or the JDK1.4 logging API.
 * <p/>
 * This class currently delegates to log4J.  It sets up certain defaults so that it can be instantly
 * used, but a configuration log file should be specified as runtime argument.  This file should be
 * a log4J xml file following the Log4J DTD.  This will allow developers to run with different logging
 * configurations than production clients.
 *
 * @author Philip Milne
 * @version $Revision: 1.16 $
 */
public class Log {
    private static final String LOG_SUBDIRECTORY = ".bhavaya/log";
    private static final String LOG_DIRECTORY_PROPERTY = "logDirectory";
    private static final String USER_CATEGORY = "USER";
    private static final String PRIMARY_LOADING_CATEGORY = "PRIMARYLOADINGLOG";
    private static final String SECONDARY_LOADING_CATEGORY = "SECONDARYLOADINGLOG";
    public static final String newLine = System.getProperty("line.separator");
    private static final String LOG_ROOT_SYSTEM_PROPERTY = "logRootSysProp";

    private static final Map instanceMap;
    private static final Log root;

    static {
        String logDirectory = getLogDirectory();
        System.setProperty(LOG_DIRECTORY_PROPERTY, logDirectory);
        try{
            // this allows us to place soft-code variables into log configuration files, e.g. applicationId, environmentId
            System.setProperty(ApplicationInfo.ID_KEY, getProperty(ApplicationInfo.ID_KEY, ApplicationInfo.DEFAULT_ID));
            System.setProperty(ApplicationInfo.NAME_KEY, getProperty(ApplicationInfo.NAME_KEY, "Default"));
            System.setProperty(ApplicationInfo.ENVIRONMENT_ID_KEY, getProperty(ApplicationInfo.ENVIRONMENT_ID_KEY, ApplicationInfo.DEFAULT_ENVIRONMENT_ID));
        } catch (Exception e) {
            System.err.println("Error setting system properties for logging");
        }

        instanceMap = new HashMap(); // dont move this init to before ApplicationInfo.getInstance()..., as this indicates a class is using logging when it shouldnt
        root = new Log();
    }

    private static Log userLog;
    private static Log primaryLoadingLog;
    private static Log secondaryLoadingLog;

    private Logger logger;
    private Level normalLevel;

    public static String getLogDirectory() {
        //You can set a system property to point to another system property which defines
        //the logging root directory - ${user.home} is the default if unspecified

        //This allows, for example, you to use ${java.io.tmpdir} instead of ${user.home} as the base dir for the logs
        //This won't often be necessary but may make sense if the io.tmpdir is local but
        //user.home is a network drive mounted across a wan!
        String logBaseDirSysProp = System.getProperties().containsKey(LOG_ROOT_SYSTEM_PROPERTY) ?
            System.getProperty(LOG_ROOT_SYSTEM_PROPERTY) :
            "user.home";


        //if the defined sys prop is not set, revert to user.home
        if ( ! System.getProperties().containsKey(logBaseDirSysProp)) {
            logBaseDirSysProp = "user.home";
        }

        String logDirectoryName = System.getProperty(logBaseDirSysProp) + File.separator + LOG_SUBDIRECTORY;
        File logDirectory = new File(logDirectoryName);
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }

        String logPath = logDirectory.getAbsolutePath();

        //so we can see exactly where the logs are going from the diagnostics:
        System.setProperty("actualLogDirectory", logPath);
        return logPath;
    }

    public static Log getCategory(Class clazz) {
        if (instanceMap.get(clazz) == null) {
            instanceMap.put(clazz, new Log(clazz));
        }

        return (Log) instanceMap.get(clazz);
    }

    public static Log getCategory(String categoryName) {
        if (instanceMap.get(categoryName) == null) {
            instanceMap.put(categoryName, new Log(categoryName));
        }

        return (Log) instanceMap.get(categoryName);
    }

    public static Log getUserCategory() {
        if (userLog == null) {
            LogManager.getLogger(USER_CATEGORY).setLevel(Level.INFO);
            userLog = getCategory(USER_CATEGORY);
        }
        return userLog;
    }

    public static Log getPrimaryLoadingLog() {
        if (primaryLoadingLog == null) {
            LogManager.getLogger(PRIMARY_LOADING_CATEGORY).setLevel(Level.INFO);
            LogManager.getLogger(PRIMARY_LOADING_CATEGORY).setAdditivity(false);
            primaryLoadingLog = getCategory(PRIMARY_LOADING_CATEGORY);
            primaryLoadingLog.logger.addAppender(new NullAppender());
        }
        return primaryLoadingLog;
    }

    public static Log getSecondaryLoadingLog() {
        if (secondaryLoadingLog == null) {
            LogManager.getLogger(SECONDARY_LOADING_CATEGORY).setLevel(Level.INFO);
            LogManager.getLogger(SECONDARY_LOADING_CATEGORY).setAdditivity(false);
            secondaryLoadingLog = getCategory(SECONDARY_LOADING_CATEGORY);
            secondaryLoadingLog.logger.addAppender(new NullAppender());
        }
        return secondaryLoadingLog;
    }

    public static Log getRoot() {
        return root;
    }

    public void setDebugOverride(boolean debugOverride) {
        if (debugOverride && normalLevel == null) {
            normalLevel = logger.getLevel();
            logger.setLevel(Level.DEBUG);
        } else {
            logger.setLevel(normalLevel);
            normalLevel = null;
        }
    }

    public boolean isDebugOverride() {
        return normalLevel != null;
    }

    public boolean isDebug() {
        Level effectiveLevel = logger.getEffectiveLevel();
        return effectiveLevel != null && effectiveLevel.equals(Level.DEBUG);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    private Log(Class clazz) {
        this.logger = Logger.getLogger(clazz);
    }

    private Log(String categoryName) {
        this.logger = Logger.getLogger(categoryName);
    }

    private Log() {
        this.logger = Logger.getRootLogger();
    }

    public void addListener(Listener listener) {
        addListener(listener, null);
    }

    public void addListener(final Listener listener, final Level level) {
        logger.addAppender(new ListenerAppender(listener, level));
    }

    public final void debug(Object o, Throwable t) {
        logger.debug(o, t);
    }

    public final void debug(Object o) {
        logger.debug(o);
    }

    public final void info(Object o) {
        logger.info(o);
    }

    public final void info(Object o, Throwable t) {
        logger.info(o, t);
    }

    public final void warn(Object o) {
        logger.warn(o);
    }

    public final void warn(Object o, Throwable t) {
        logger.warn(o, t);
    }

    public final void warn(Throwable t) {
        logger.warn(t.getMessage(), t);
    }

    public void error(Object o) {
        logger.error(o);
    }

    public void error(Throwable t) {
        error(t.getMessage(), t);
    }

    public void error(Object o, Throwable t) {
        logger.error(o, t);

        if (t.getCause() != null) {
            error("Unwinding exception chain: ", t.getCause());
        } else {
            //a bit messy, but useful
            if (t instanceof SQLException) {
                SQLException currentException = (SQLException) t;
                logger.error("SQL Error code: " + currentException.getErrorCode());
                logger.error("SQL State: " + currentException.getSQLState());
                SQLException nextException = ((SQLException) t).getNextException();
                if (nextException != null) {
                    error("Unwinding exception chain: ", nextException);
                }
            }

            if (t instanceof SAXParseException) {
                SAXParseException currentException = (SAXParseException) t;
                logger.error("PublicId = " + currentException.getPublicId());
                logger.error("SystemId = " + currentException.getSystemId());
                logger.error("LineNumber = " + currentException.getLineNumber());
                logger.error("ColumnNumber = " + currentException.getColumnNumber());
            }
        }
    }

    public static void errorToSerr(Object o, Throwable t) {
        System.err.println(o + ": " + t);
        t.printStackTrace(System.err);

        if (t.getCause() != null) {
            errorToSerr("Unwinding exception chain: ", t.getCause());
        } else {
            //a bit messy, but useful
            if (t instanceof SQLException) {
                SQLException currentException = (SQLException) t;
                System.err.println("SQL Error code: " + currentException.getErrorCode());
                System.err.println("SQL State: " + currentException.getSQLState());
                SQLException nextException = ((SQLException) t).getNextException();
                if (nextException != null) {
                    errorToSerr("Unwinding exception chain: ", nextException);
                }
            }

            if (t instanceof SAXParseException) {
                SAXParseException currentException = (SAXParseException) t;
                System.err.println("PublicId = " + currentException.getPublicId());
                System.err.println("SystemId = " + currentException.getSystemId());
                System.err.println("LineNumber = " + currentException.getLineNumber());
                System.err.println("ColumnNumber = " + currentException.getColumnNumber());
            }
        }
    }

    public final void fatal(Object o) {
        logger.fatal(o);
    }

    public final void fatal(Object o, Throwable t) {
        logger.fatal(o, t);
    }

    public interface Listener {
        public void logMessage(LoggingEvent loggingEvent);
    }

    public static class ThreadFilter extends LevelRangeFilter {
        private String threadName;

        public String getThreadName() {
            return threadName;
        }

        public void setThreadName(String threadName) {
            this.threadName = threadName;
        }

        public int decide(LoggingEvent event) {
            if (super.decide(event) == Filter.DENY && event.getThreadName().equals(threadName)) {
                return Filter.DENY;
            }
            return Filter.NEUTRAL;
        }
    }

    private static String getProperty(String key, String defaultValue) {
        String propertyValue = ApplicationProperties.getApplicationProperties().getProperty(key);
        if (propertyValue == null || propertyValue.length() == 0) return defaultValue;
        return propertyValue;
    }

    private static class ListenerAppender extends AppenderSkeleton {
        private final Level level;
        private final Listener listener;

        public ListenerAppender(Listener listener, Level level) {
            this.level = level;
            this.listener = listener;
        }

        protected void append(LoggingEvent event) {
            if (level == null || event.level.isGreaterOrEqual(level)) {
                listener.logMessage(event);
            }
        }

        public void close() {
        }

        public boolean requiresLayout() {
            return false;
        }
    }
}
