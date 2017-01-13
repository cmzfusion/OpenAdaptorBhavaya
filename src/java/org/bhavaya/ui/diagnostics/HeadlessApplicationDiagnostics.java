package org.bhavaya.ui.diagnostics;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import com.sun.jdmk.comm.HtmlAdaptorServer;
import org.bhavaya.util.*;
import org.bhavaya.ui.ArrayListModel;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Priority;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Nov 14, 2005
 * Time: 4:03:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class HeadlessApplicationDiagnostics {
    private static final Log log = Log.getCategory(ApplicationDiagnostics.class);

    public static final int MAX_LOG_MESSAGES = 5000;
    public static final int MAX_ERRORS_OR_WARNINGS = 500;


    protected static Mutex instanceMutext = new Mutex();
    protected static HeadlessApplicationDiagnostics instance;

    protected ArrayList<DiagnosticContext> diagnosticContexts = new ArrayList<DiagnosticContext>();

    private MBeanServer mBeanServer;
    private HashSet<StackTraceElement> sendDiagnosticMessageCallerStackFrame = new HashSet<StackTraceElement>();
    private boolean logBeanToScreenDelays;
    private ArrayList<String> debugLogPackageList = new ArrayList<String>();

    protected ArrayListModel allLogStatements = new ArrayListModel(new LinkedList());
    protected ArrayListModel warnLogStatements = new ArrayListModel(new LinkedList());
    protected ArrayListModel errorLogStatements = new ArrayListModel(new LinkedList());
    protected ArrayListModel exceptionLogStatements = new ArrayListModel(new LinkedList());

    private int mBeanServerHtmlAdaptorPort;

    private static final String FIRST_FOUR_LOGS_PATTERN = ".*\\.log$|.*\\.log\\.[1-3]$";
    private static final String FIRSTLOG_PATTERN = ".*\\.log$";

    public static HeadlessApplicationDiagnostics getHeadlessInstance() {
        ThreadUtilities.quietAquire(instanceMutext);
        if (instance == null) instance = new HeadlessApplicationDiagnostics();
        instanceMutext.release();
        return instance;
    }

    protected HeadlessApplicationDiagnostics() {
        Log.getRoot().addListener(new LogListener(isHeadless()));
    }

    protected boolean isHeadless() {
        return true;
    }

    public void addDiagnosticContext(DiagnosticContext diagnosticContext) {
        log.info("Registering diagnostic context: " + diagnosticContext.getName());
        diagnosticContexts.add(diagnosticContext);
        registerMBeanForDiagnosticContext(diagnosticContext);
    }

    public void productionAssert(boolean condition, String message) {
        if (!condition) {
            assert false : message;
            //this line will only get run if the vm is not running with assertions enabled (i.e. production clients)
            sendDiagnosticReportOnlyOnce(message);
        }
    }

    public ArrayListModel getAllLogStatements() {
        return allLogStatements;
    }

    public ArrayListModel getWarnLogStatements() {
        return warnLogStatements;
    }

    public ArrayListModel getErrorLogStatements() {
        return errorLogStatements;
    }

    public List getExceptionLogStatements() {
        return exceptionLogStatements;
    }

    public void addApplicationDebugLogPackage(String debugLogPackage) {
        debugLogPackageList.add(debugLogPackage);
    }

    private String createUserCommentsSummary(String userComments) {
        return "<b><font face=arial>User comments</font></b><br>"
                + (userComments == null || userComments.length() == 0 ? "None" : userComments);
    }

    public boolean sendDiagnosticReportOnlyOnce(String comments) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (!sendDiagnosticMessageCallerStackFrame.contains(stackTrace[1])) {
            StringBuffer message = new StringBuffer("<html>Automated report from:<br>");
            for (int i = 1; i < stackTrace.length; i++) {
                message.append("     ").append(stackTrace[i]).append("<br>\n");
            }
            message.append("Message:<br>\n");
            message.append(comments).append("</html>");
            sendDiagnosticReport(message.toString());
            sendDiagnosticMessageCallerStackFrame.add(stackTrace[1]);
            return true;
        }
        return false;
    }

    public void sendDiagnosticReport(String comments) {
        doSendDiagnosticReport(comments, ApplicationProperties.getApplicationProperties().getMandatoryProperty("emailService.to"));
    }

    public void sendDiagnosticReport(String comments, String emailAddress) {
        doSendDiagnosticReport(comments, DiagnosticEmail.addDomainToEmail(emailAddress));
    }

    protected void doSendDiagnosticReport(String comments, String emailAddress) {
        LinkedList<DiagnosticContext.Attachment> attachmentsList = getAttachmentsList();
        addConfigRootAttachments(attachmentsList);

        String reportBody = getHtmlReport();

        DiagnosticEmail diagnosticEmail = new DiagnosticEmail("Diagnostic report", emailAddress, comments, reportBody, "details.zip", attachmentsList);
        diagnosticEmail.sendEmail();
    }

    protected void addConfigRootAttachments(LinkedList<DiagnosticContext.Attachment> attachmentsList) {
        String[] configRoots = Configuration.getConfigRootNames();
        for (String configRoot : configRoots) {
            try {
                String configXMLString = Configuration.getConfigXMLString(configRoot);

                //this is a temporary fix for a bhavaya bug where the diagnostics fail for the first run of the app once a new
                //config root is added. This occurs because a saved config file for that root was not loaded when the app
                //was started. In any case, it would probably be better to send the in memory config, rather than the config from
                //file system (which doesn't reflect any non-saved changes)
                configXMLString = configXMLString == null ? "No saved config file yet" : configXMLString;

                long versionNumber = Configuration.getRoot(configRoot).getVersionNumber();
                attachmentsList.add(
                    new DiagnosticContext.Attachment(
                        configRoot + "." + versionNumber + ".xml",
                        configXMLString.getBytes()
                    )
                );
            } catch (IOException e) {
                log.warn("Could not find config file to include in diagnostic.", e);
            }
        }
    }

    protected String getHtmlReport() {
        StringBuffer reportBuffer = new StringBuffer();
        for (DiagnosticContext diagnosticContext : diagnosticContexts) {
            String htmlDescription = null;
            try {
                htmlDescription = diagnosticContext.createHTMLDescription();
            } catch (Throwable e) {
                log.error("DiagnosticContext generated error: " + diagnosticContext.getName(), e);
            }

            if (htmlDescription != null) reportBuffer.append(htmlDescription).append("<br><br>");
        }
        return reportBuffer.toString();
    }

    protected LinkedList<DiagnosticContext.Attachment> getAttachmentsList() {
        LinkedList<DiagnosticContext.Attachment> attachmentsList = new LinkedList<DiagnosticContext.Attachment>();
        for (DiagnosticContext diagnosticContext : diagnosticContexts) {
            DiagnosticContext.Attachment[] attachments = diagnosticContext.createAttachments();
            for (DiagnosticContext.Attachment attachment : attachments) {
                attachmentsList.add(attachment);
            }
        }
        return attachmentsList;
    }

    public void sendQuickDiagnosticMessage(String type, String message, boolean html) {
        EmailUtilities.sendQuickMessage(DiagnosticEmail.createDiagnosticSubject(type), message, html);
    }

    public void sendFile(String fileName) {
        try {
            String username = ApplicationInfo.getInstance().getUsername();
            String subject = ApplicationInfo.getInstance().getName() + ": Files: environment: "
                    + ApplicationInfo.getInstance().getEnvironmentName() + ", username: " + username;
            Message mailMessage = EmailUtilities.getDefaultMimeMessage(subject);

            MimeBodyPart logAttachment = null;
            byte[] data = zipFile(fileName);
            if (data != null && data.length > 0) {
                logAttachment = new MimeBodyPart();
                logAttachment.setDataHandler(new DataHandler(new EmailUtilities.ByteArrayDataSource(data, "application/zip")));
                File f = new File(fileName);
                logAttachment.setFileName(f.getName() + ".zip");
            }

            MimeBodyPart summaryBodyPart = new MimeBodyPart();
            summaryBodyPart.setContent(createSendFilesReport("", logAttachment != null), "text/html");
            summaryBodyPart.setHeader("Content-Type", "text/html; charset=iso-8859-1");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(summaryBodyPart);
            if (logAttachment != null) multipart.addBodyPart(logAttachment);

            mailMessage.setContent(multipart);
            EmailUtilities.sendMessage(mailMessage);
            log.info("Sent file " + fileName + " to: " + ApplicationProperties.getApplicationProperties().getMandatoryProperty("emailService.to"));
        } catch (Throwable t) {
            log.error("Unable to send file " + fileName, t);
        }
    }

    protected String createSendLogFilesReport(String userComments, boolean hasAttachments) {
        StringBuffer buffer = new StringBuffer("<html>"
                + "<font face=arial><b>Log files for " + ApplicationInfo.getInstance().getName() + "</b></font><br><hr size=1><br>");
        buffer.append(createUserCommentsSummary(userComments)).append("<br><br><br>");
        if (!hasAttachments) {
            buffer.append("No log files found.");
        } else {
            buffer.append("<font face=Arial size=1><i>Log files attached.</i></font>");
        }
        buffer.append("</html>");
        return buffer.toString();
    }

    protected String createSendFilesReport(String userComments, boolean hasAttachments) {
        StringBuffer buffer = new StringBuffer("<html>"
                + "<font face=arial><b>Files for " + ApplicationInfo.getInstance().getName() + "</b></font><br><hr size=1><br>");
        buffer.append(createUserCommentsSummary(userComments)).append("<br><br><br>");
        if (!hasAttachments) {
            buffer.append("No files found.");
        } else {
            buffer.append("<font face=Arial size=1><i>Files attached.</i></font>");
        }
        buffer.append("</html>");
        return buffer.toString();
    }

    protected byte[] zipFile(String fileName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        zos.setLevel(ZipOutputStream.DEFLATED);

        File file = new File(fileName);
        if (file.isFile() && file.canRead()) {
            zos.putNextEntry(new ZipEntry(file.getName()));
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[1024 * 100];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
        } else {
            return null;
        }
        zos.close();
        return bos.toByteArray();
    }

    protected byte[] zipLogFiles() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        zos.setLevel(ZipOutputStream.DEFLATED);

        File[] files = getLogFiles();
        if (files.length == 0) return null;
        for (File file : files) {
            if (file.isFile() && file.canRead()) {
                zos.putNextEntry(new ZipEntry(file.getName()));
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

                byte[] buffer = new byte[1024 * 100];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead);
                }
            }
        }

        zos.close();
        return bos.toByteArray();
    }

    public StringBuilder searchLogFilesByLine(String searchPattern) {
        //can be expensive so limit to FIRSTLOG_PATTERN
        return searchLogFiles(FIRSTLOG_PATTERN, "^.*" + searchPattern + ".*$", java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    public StringBuilder searchLogFilesByLine(String fileNamePattern, String searchPattern) {
        return searchLogFiles(fileNamePattern, "^.*" + searchPattern + ".*$", java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    public StringBuilder searchLogFiles(String filePattern, String searchPattern, int patternFlags ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<style type='text/css'>td { vertical-align: top }</style>\n");
        sb.append("<table>\n");
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(searchPattern, patternFlags);
            File[] logFiles = getLogFiles(filePattern);
            if ( logFiles.length == 0) {
                insertExclamationText(sb, "No logs were found");
            } else {
                doSearch(sb, pattern, logFiles);
            }
        } catch (PatternSyntaxException p) {
            insertExclamationText(sb, "Invalid search pattern");
            p.printStackTrace();
        }
        sb.append("</table>\n");
        return sb;
    }

    private void doSearch(StringBuilder sb, java.util.regex.Pattern p, File[] logFiles) {
        for ( File f : logFiles) {
            searchLogFile(f, p, sb);
        }
    }

    //add some highlighted text before the main results
    private void insertExclamationText(StringBuilder sb, String prefixText) {
        sb.insert(0, "<font color='red' size='+2'>" + prefixText + "</font><br/>\n");
    }

    private void searchLogFile(File f, java.util.regex.Pattern pattern, StringBuilder results) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if ( m.find()) {
                    String match = m.group(0);
                    if (results.length() + match.length() > 1000000) {
                        insertExclamationText(results, "Too many matches or match too large.. results truncated");
                        return;
                    }
                    results.append("<tr><td>").append(f.getName()).append("</td><td>").append(match).append("</td></tr>\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if ( br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public File[] getLogFiles() {
        return getLogFiles(FIRST_FOUR_LOGS_PATTERN);
    }

    /**
     * @return an array of log files
     */
    public File[] getLogFiles(final String fileNamePattern) {
        String logDirStr = Log.getLogDirectory();
        File logDir = new File(logDirStr);
        File[] result = new File[0];
        if (logDir.exists()) {
            result = logDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    boolean matches = name.matches(fileNamePattern);
                    return matches;
                }
            });
        }
        return result;
    }

    /**
     * Will create an MBean server and register all MBeans provided by registered diagnostic contexts with it.
     * By default also starts HTML adaptor on a default port 1025.
     * <p/>
     * Configuration can be tweaked by putting following into application configuration file (application.xml)
     * <p/>
     * <propertyGroup key="JMX">
     * <propertyGroup key="htmlAdaptor">
     * <!-- Binds the HTML adaptor server to the first available port within the range -->
     * <property key="portRangeStart" value="1025"/>
     * <property key="portRangeEnd" value="65535"/>
     * </propertyGroup>
     * <propertyGroup key="jmxConnector">
     * <property key="port" value="4115"/>
     * </propertyGroup>
     * </propertyGroup>
     */
    public void createMBeanServer() {
        try {
            mBeanServer = MBeanServerFactory.createMBeanServer(Utilities.MBEANSERVER_DOMAIN);
            String applicationDiagnosticsObjectName = mBeanServer.getDefaultDomain() + ":type=" + ClassUtilities.getUnqualifiedClassName(ApplicationDiagnostics.class)
                    + ",applicationId=" + ApplicationInfo.getInstance().getId()
                    + ",environmentId=" + ApplicationInfo.getInstance().getEnvironmentId();
            if (log.isDebug())log.debug("Registering MBean: " + applicationDiagnosticsObjectName);
            mBeanServer.registerMBean(new ApplicationDiagnosticsMBeanAdaptor(), new ObjectName(applicationDiagnosticsObjectName));

            for (Object diagnosticContext1 : diagnosticContexts) {
                DiagnosticContext diagnosticContext = (DiagnosticContext) diagnosticContext1;
                registerMBeanForDiagnosticContext(diagnosticContext);
            }

            int portRangeStart = 1025;
            int portRangeEnd = 65535;
            PropertyGroup jmxProperties = ApplicationProperties.getApplicationProperties().getGroup("JMX");
            if (jmxProperties != null) {
                PropertyGroup htmlAdaptorProperties = jmxProperties.getGroup("htmlAdaptor");
                if (htmlAdaptorProperties != null) {
                    Number portRangeStartNumber = htmlAdaptorProperties.getNumericProperty("portRangeStart");
                    Number portRangeEndNumber = htmlAdaptorProperties.getNumericProperty("portRangeEnd");
                    if (portRangeStartNumber != null) portRangeStart = portRangeStartNumber.intValue();
                    if (portRangeEndNumber != null) portRangeEnd = portRangeEndNumber.intValue();
                }
                PropertyGroup jmxConnectorProperties = jmxProperties.getGroup("jmxConnector");
                if (jmxConnectorProperties != null) {
                    Number jmxConnectorPortNumber = jmxConnectorProperties.getNumericProperty("port");
                    if (jmxConnectorPortNumber != null) {
                        int jmxConnectorPort = jmxConnectorPortNumber.intValue();
                        try {
                            log.info("Binding JMXConnectorServer to port: " + jmxConnectorPort);
                            JMXConnectorServer connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(new JMXServiceURL("jmxmp", null, jmxConnectorPort), null, mBeanServer);
                            connectorServer.start();
                        } catch (Exception e) {
                            log.error("Could not bind JMXConnectorServer to port: " + jmxConnectorPort, e);
                        }
                    }
                }
            }

            for (int port = portRangeStart; port < portRangeEnd; port++) {
                log.info("Binding diagnostics to port " + port);
                try {
                    HtmlAdaptorServer htmlAdaptor = new HtmlAdaptorServer(port);
                    htmlAdaptor.start();
                    while (htmlAdaptor.getState() == HtmlAdaptorServer.STARTING) Thread.sleep(100);
                    if (htmlAdaptor.isActive()) {
                        mBeanServer.registerMBean(htmlAdaptor, new ObjectName("Adaptor:name=html,port=" + port));
                        ApplicationInfo.getInstance().setDiagnosticPort(port);
                        mBeanServerHtmlAdaptorPort = port;
                        break;
                    }
                } catch (Exception e) {
                    log.info("Could not bind HtmlAdaptorServer to port: " + port);
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    public int getMBeanServerHtmlAdaptorPort() {
        return mBeanServerHtmlAdaptorPort;
    }

    private void registerMBeanForDiagnosticContext(DiagnosticContext diagnosticContext) {
        if (mBeanServer == null) return;
        Object mBean = diagnosticContext.createMBean();
        if (mBean != null) {
            String mBeanName = ClassUtilities.getUnqualifiedClassName(mBean.getClass());
            if (log.isDebug())log.debug("Registering MBean: " + mBeanName);
            try {
                String mBeanIdentifier = diagnosticContext.getMBeanIdentifier();
                if (mBeanIdentifier == null) {
                    mBeanIdentifier = ":type=" + mBeanName;
                }
                mBeanServer.registerMBean(mBean, new ObjectName(mBeanServer.getDefaultDomain() + mBeanIdentifier));
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public boolean isLogBeanToScreenDelays() {
        return logBeanToScreenDelays;
    }

    public void setLogBeanToScreenDelays(boolean logBeanToScreenDelays) {
        BeanPropertyChangeSupport.logEventTime = logBeanToScreenDelays;
        this.logBeanToScreenDelays = logBeanToScreenDelays;
    }

    public void setDebugLoggingEnabled(boolean selected) {
        Log.getCategory("org.bhavaya").setDebugOverride(selected);
        for (String logPackage : debugLogPackageList) {
            Log.getCategory(logPackage).setDebugOverride(selected);
        }
    }

    public boolean isDebugLoggingEnabled() {
        return Log.getCategory("org.bhavaya").isDebug();
    }

    public void sendLogFiles(String userComments) {
        String emailAddress = ApplicationProperties.getApplicationProperties().getMandatoryProperty("emailService.to");
        sendLogFilesToEmailAddress(userComments, emailAddress);
    }

    public void sendLogFiles(String userComments, String emailAddress) {
        String address = DiagnosticEmail.addDomainToEmail(emailAddress);
        sendLogFilesToEmailAddress(userComments, address);
    }

    private void sendLogFilesToEmailAddress(String userComments, String emailAddress) {
        try {
            String username = ApplicationInfo.getInstance().getUsername();
            String subject = ApplicationInfo.getInstance().getName() + ": Log Files: environment: "
                    + ApplicationInfo.getInstance().getEnvironmentName() + ", username: " + username;

            String[] addresses = emailAddress.split(";");
            Message mailMessage = EmailUtilities.getDefaultMimeMessage(addresses, subject);

            MimeBodyPart logAttachment = null;
            byte[] data = zipLogFiles();
            if (data != null && data.length > 0) {
                logAttachment = new MimeBodyPart();
                logAttachment.setDataHandler(new DataHandler(new EmailUtilities.ByteArrayDataSource(data, "application/zip")));
                logAttachment.setFileName("logs_" + username + ".zip");
            }

            MimeBodyPart summaryBodyPart = new MimeBodyPart();
            summaryBodyPart.setContent(createSendLogFilesReport(userComments, logAttachment != null), "text/html");
            summaryBodyPart.setHeader("Content-Type", "text/html; charset=iso-8859-1");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(summaryBodyPart);
            if (logAttachment != null) multipart.addBodyPart(logAttachment);

            mailMessage.setContent(multipart);
            EmailUtilities.sendMessage(mailMessage);
            log.info("Sent log files to: " + emailAddress);
        } catch (Throwable t) {
            log.error("Unable to send log files", t);
        }
    }

    public static class LoggingEventBean {
        private LoggingEvent loggingEvent;

        public LoggingEventBean(LoggingEvent loggingEvent) {
            this.loggingEvent = loggingEvent;
        }

        public Date getTimestamp() {
            return new Date(loggingEvent.timeStamp);
        }

        public String getThreadName() {
            return loggingEvent.getThreadName();
        }

        public String getLevel() {
            if (loggingEvent.level == null) return null;
            return loggingEvent.level.toString();
        }

        public String getCategory() {
            if (loggingEvent.categoryName == null) return null;
            int lastDot = loggingEvent.categoryName.lastIndexOf('.');
            if (lastDot == -1 || ((lastDot + 1) > (loggingEvent.categoryName.length() - 1))) return loggingEvent.categoryName;
            return loggingEvent.categoryName.substring(lastDot + 1);
        }

        public String getMessage() {
            return loggingEvent.getRenderedMessage();
        }

        public String[] getThrowableStrings() {
            return loggingEvent.getThrowableStrRep();
        }

        public Throwable getThrowable() {
            if (loggingEvent.getThrowableInformation() == null) return null;
            return loggingEvent.getThrowableInformation().getThrowable();
        }

        public LoggingEvent getLoggingEvent() {
            return loggingEvent;
        }

        public String toString() {
            return getMessage();
        }
    }

    /**
     * This class and the design of the internal log / diagnostics panel needs to be reviewed since I'm really not comfortable
     * forcing work onto the AWT thread for every logged statement, this gets messy especially where an error in a feed results
     * in lots of logging. I'd favour lazy loading log lines on demand direct from the log file, at least for info or debug level logging.
     *
     * For short term triage I have removed the multiple Thread.yield() on AWT thread, fixed inefficiency in
     * iterating multiple times to remove items from the end of the linked list of errors, and now fire a data changed update
     * through the diagnostics table models only once per batch rather than on every row.
     *
     * Log events are now queued and batch processed in a single AWT task rather than adding a task
     * to the swing queue for each line
     */
    protected class LogListener implements Log.Listener {
        private boolean headless;

        private Object queueLock = new Object();
        private List<LoggingEventBean> allQueue = new LinkedList<LoggingEventBean>();
        private List<LoggingEventBean> warnQueue = new LinkedList<LoggingEventBean>();
        private List<LoggingEventBean> errorQueue = new LinkedList<LoggingEventBean>();
        private List<LoggingEventBean> exceptionQueue = new LinkedList<LoggingEventBean>();

        public LogListener(boolean headless) {
            this.headless = headless;
        }

        public void logMessage(final LoggingEvent loggingEvent) {
            if ( loggingEvent == null) return ;  // just in case

            synchronized (queueLock) {
                LoggingEventBean loggingEventBean = new LoggingEventBean(loggingEvent);
                addToQueues(loggingEvent, loggingEventBean);

                if ( headless ) {
                    logMessageInternal();
                } else {
                    if ( allQueue.size() == 1) {
                        //this is the first log statement added since the AWT task last ran and cleared queues
                        //so we need to add a new AWT task, by the time it runs we may have several queued log statements to add
                        //so we are doing the work in batches rather than one task on the queue per log line
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                logMessageInternal();
                            }
                        });
                    }
                }
            }
        }

        //add to the various log queues according to the priority of the message
        private void addToQueues(LoggingEvent loggingEvent, LoggingEventBean loggingEventBean) {
            allQueue.add(loggingEventBean);

            if (loggingEvent.level.equals(Priority.WARN)) {
                warnQueue.add(loggingEventBean);
            }

            if (loggingEvent.level.isGreaterOrEqual(Priority.ERROR)) {
                errorQueue.add(loggingEventBean);

                if (loggingEventBean.getThrowable() != null) {
                    exceptionQueue.add(loggingEventBean);
                }
            }
        }

        private void logMessageInternal() {
            synchronized (queueLock) {
                addToLogsAndClearQueue(allLogStatements, allQueue, MAX_LOG_MESSAGES);
                addToLogsAndClearQueue(warnLogStatements, warnQueue, MAX_ERRORS_OR_WARNINGS);
                addToLogsAndClearQueue(errorLogStatements, errorQueue, MAX_ERRORS_OR_WARNINGS);
                addToLogsAndClearQueue(exceptionLogStatements, exceptionQueue, MAX_ERRORS_OR_WARNINGS);
            }
        }

        private void addToLogsAndClearQueue(ArrayListModel list, List<LoggingEventBean> toAdd, int maxMessages) {
            if ( toAdd.size() > 0) {
                addAndClear(list, toAdd, maxMessages);
            }
        }

        private void addAndClear(ArrayListModel list, List<LoggingEventBean> toAdd, int maxMessages) {
            synchronized (list) { //not ideal but presently the diagnostics report synchronizes on the exceptionLogStatements
                list.addAll(0, toAdd);
                int removeCount = list.size() - maxMessages;
                if ( removeCount > 0 ) {
                    list.removeFromEnd(removeCount);
                }
            }
            toAdd.clear();
        }
    }

}
