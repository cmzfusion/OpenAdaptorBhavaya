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

package org.bhavaya.coms;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import com.sun.jdmk.comm.HtmlAdaptorServer;
import org.bhavaya.util.*;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Simple class to that receives notifications and logs them via the logging system.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.8 $
 */
public class NotificationLogger implements NotificationLoggerMBean {

    private static final Log log = Log.getCategory(NotificationLogger.class);

    private static Latch closeLatch = new Latch();
    private static MBeanServer mBeanServer;

    public static void main(String[] args) {
        ClassUtilities.setGenerateClassesDynamically(false);
        org.bhavaya.beans.Schema.setGenerationMode(true);
        NotificationSubscriber[] subscribers = NotificationSubscriber.getInstances();

        try {
            for (int i = 0; i < subscribers.length; i++) {
                NotificationSubscriber subscriber = subscribers[i];
                subscriber.addExceptionListener(new NotificationSubscriber.ReconnectionExceptionListener(subscriber));
                subscriber.addNotificationListener(new NotificationLoggerNotificationListener());
                subscriber.connect();
                subscriber.startProcessing();
            }

            mBeanServer = MBeanServerFactory.createMBeanServer(Utilities.MBEANSERVER_DOMAIN);
            mBeanServer.registerMBean(new NotificationLogger(), new ObjectName(mBeanServer.getDefaultDomain() + ":type=" + ClassUtilities.getUnqualifiedClassName(NotificationLogger.class)));

            HtmlAdaptorServer htmlAdaptor = new HtmlAdaptorServer(1222);
            htmlAdaptor.start();
            while (htmlAdaptor.getState() == HtmlAdaptorServer.STARTING) Thread.sleep(100);
            mBeanServer.registerMBean(htmlAdaptor, new ObjectName("Adaptor:name=html,port=" + 1222));

            closeLatch.acquire();
        } catch (Exception e) {
            log.error(e);
            System.exit(1);
        }
    }

    public void emailLogs(String emailAddress) {
        sendLogFilesToEmailAddress(emailAddress);
    }

    public void exit() {
        closeLatch.release();
    }

    private static byte[] zipLogFiles() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        zos.setLevel(ZipOutputStream.DEFLATED);

        String logDirStr = Log.getLogDirectory();
        File logDir = new File(logDirStr);
        if (!logDir.exists()) return null;
        File[] files = logDir.listFiles();
        if (files == null || files.length == 0) return null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
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

    private static void sendLogFilesToEmailAddress(String emailAddress) {
        try {
            String username = ApplicationInfo.getInstance().getUsername();
            String subject = ApplicationInfo.getInstance().getName() + ": Log Files: environment: "
                    + ApplicationInfo.getInstance().getEnvironmentName() + ", username: " + username;
            Message mailMessage = EmailUtilities.getDefaultMimeMessage(new String[] {emailAddress}, subject);

            MimeBodyPart logAttachment = null;
            byte[] data = zipLogFiles();
            if (data != null && data.length > 0) {
                logAttachment = new MimeBodyPart();
                logAttachment.setDataHandler(new DataHandler(new EmailUtilities.ByteArrayDataSource(data, "application/zip")));
                logAttachment.setFileName("logs_" + username + ".zip");
            }

            MimeBodyPart summaryBodyPart = new MimeBodyPart();
            summaryBodyPart.setContent(createSendLogFilesReport(logAttachment != null), "text/html");
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

    private static String createSendLogFilesReport(boolean hasAttachments) {
        StringBuffer buffer = new StringBuffer("<html>"
                + "<font face=arial><b>Log files for " + ApplicationInfo.getInstance().getName() + "</b></font><br><hr size=1><br>");
        buffer.append("Notification Log Files").append("<br><br><br>");
        if (!hasAttachments) {
            buffer.append("No log files found.");
        } else {
            buffer.append("<font face=Arial size=1><i>Log files attached.</i></font>");
        }
        buffer.append("</html>");
        return buffer.toString();
    }

    private static class NotificationLoggerNotificationListener implements NotificationListener {
        private static final Log notificationLog = Log.getCategory("notification");

        public NotificationLoggerNotificationListener() {
        }

        public void receive(String message) {
            notificationLog.info(message);
        }
    }
}
