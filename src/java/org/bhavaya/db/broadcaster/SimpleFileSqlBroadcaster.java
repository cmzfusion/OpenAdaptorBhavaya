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

package org.bhavaya.db.broadcaster;

import com.sun.org.apache.xml.internal.utils.FastStringBuffer;
import org.bhavaya.coms.NotificationException;
import org.bhavaya.coms.NotificationPublisher;
import org.bhavaya.coms.NotificationServer;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.11 $
 */
public class SimpleFileSqlBroadcaster {
    private static final Log log = Log.getCategory(SimpleFileSqlBroadcaster.class);
    private static final Log sqlLog = Log.getCategory("sql");

    private static final String BROADCASTER_PROPERTY_GROUP = "sqlbroadcaster";

    private static final Pattern updatePattern = Pattern.compile("^[\\s]*update[\\s]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern insertIntoPattern = Pattern.compile("^[\\s]*insert[\\s]+into[\\s]+", Pattern.CASE_INSENSITIVE);
    //private static final Pattern insertPattern = Pattern.compile("^[\\s]*insert ", Pattern.CASE_INSENSITIVE);
    private static final Pattern deletePattern = Pattern.compile("^[\\s]*delete[\\s]+from[\\s]+", Pattern.CASE_INSENSITIVE);
    //private static final Pattern deletePattern = Pattern.compile("^[\\s]*delete[\\s]+[\\w]+\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern truncatePattern = Pattern.compile("^[\\s]*truncate[\\s]+table[\\s]+", Pattern.CASE_INSENSITIVE);

    private static final String COMMIT = "COMMIT";

    private NotificationServer notificationServer;
    private String databaseName;
    private FastStringBuffer sqlBuffer;
    private NotificationPublisher broadcaster;
    private RandomAccessFile input;
    private long retriesAttempted = 0;
    private String filename;
    private boolean allowFileToEnd;
    private boolean closed;
    private String notificationSubject;

    public SimpleFileSqlBroadcaster(boolean daemon) throws Exception {
        PropertyGroup broadcasterPropertyGroup = ApplicationProperties.getApplicationProperties().getGroup(BROADCASTER_PROPERTY_GROUP);
        this.databaseName = broadcasterPropertyGroup.getProperty("databaseName");
        this.filename = broadcasterPropertyGroup.getMandatoryProperty("sqlFilename");
        this.allowFileToEnd = Boolean.TRUE.toString().equalsIgnoreCase(broadcasterPropertyGroup.getMandatoryProperty("allowFileToEnd"));
        this.notificationSubject = broadcasterPropertyGroup.getProperty("sqlNotificationSubject");
        if (this.notificationSubject == null) this.notificationSubject = NotificationServer.DEFAULT_NOTIFICATION_SUBJECT;

        notificationServer = NotificationServer.newInstance(notificationSubject, daemon);
        broadcaster = NotificationPublisher.getInstance(notificationSubject);

        setClosed(true);
    }

    public void startWithFailover() {
        while (true) {
            try {
                start();
                if (isClosed()) return;
            } catch (Exception e) {
                log.error("SqlBroadcaster failed", e);
                try {
                    close();
                } catch (Throwable e1) {
                    log.error(e1);
                }
                retriesAttempted++;
                pauseBeforeRestart();
            }
        }
    }

    private void pauseBeforeRestart() {
        if (retriesAttempted == 1) {
            log.error("Restarting SqlBroadcaster (Attempt " + retriesAttempted + ")...");
        } else {
            log.error("Restarting SqlBroadcaster (Attempt " + retriesAttempted + ") in " + (broadcaster.getReconnectionPeriod() / 1000) + " seconds...");

            try {
                Thread.sleep(broadcaster.getReconnectionPeriod());
            } catch (InterruptedException e) {
                log.error(e);
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void start() throws Exception {
        log.info("Attempting to start broadcast");
        connect();
        broadcast();
    }

    public synchronized void connect() throws Exception {
        if (!isClosed()) {
            log.error("Attempting to connect when already connected");
            return;
        }
        setClosed(false);

        log.info("Opening file: " + filename + " for RW, although will only be used for reading.");
        if (!(new File(filename).exists())) throw new RuntimeException(filename + " does not exist");
        input = new RandomAccessFile(filename, "rw");
        if (allowFileToEnd) moveToEndOfFile();

        log.info("Starting notification server");
        notificationServer.start();

        log.info("Connecting to notification server");
        broadcaster = NotificationPublisher.getInstance(notificationSubject);
        broadcaster.connect();

        sqlBuffer = new FastStringBuffer();

        log.info("Started broadcasting");
    }

    public synchronized void close() {
        if (isClosed()) {
            log.error("Attempting to connect when already closed");
            return;
        }
        setClosed(true);

        if (broadcaster != null) {
            broadcaster.close();
            broadcaster = null;
        }

        if (notificationServer != null) {
            try {
                notificationServer.close();
            } catch (Exception e) {
                log.error(e);
            }
        }

        closeInput(input);
    }

    public void broadcast() throws Exception {
        try {
            String line = input.readLine();
            while (allowFileToEnd || line != null) {
                if (line != null) {
                    broadcastLine(line);
                } else {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }

                if (isClosed()) return;
                line = input.readLine();
            }
        } catch (Exception e) {
            if (isClosed()) {
                return;
            } else {
                throw e;
            }
        }

        if (!isClosed()) {
            // stream has closed and allowFileToEnd is false, do not allow failover to restart
            log.fatal("Input stream has closed, no point failingover");
            System.exit(1);
        }
    }

    private synchronized void broadcastLine(String line) throws NotificationException {
        if (log.isDebug()) log.debug("Received: " + line);
        String processedLine = processString(line);
        sqlBuffer.append(processedLine);
        if (line.toUpperCase().startsWith(COMMIT)) {
            broadcastBuffer();
        } else {
            sqlBuffer.append('\n');
        }
    }

    private void moveToEndOfFile() throws IOException {
        String line = input.readLine();
        while (line != null) {
            line = input.readLine();
        }
    }

    private synchronized void broadcastBuffer() throws NotificationException {
        String sqlString = sqlBuffer.toString();
        broadcaster.send(sqlString);
        broadcaster.commit();
        if (sqlLog.isDebug())sqlLog.debug(sqlString);
        this.sqlBuffer.setLength(0);
    }

    private String processString(String record) {
        if (databaseName != null && databaseName.length() > 0) {
            // prefix ownername.tablename with databasename
            Matcher updateMatcher = updatePattern.matcher(record);
            record = updateMatcher.replaceAll("update " + databaseName + ".");

            Matcher insertIntoMatcher = insertIntoPattern.matcher(record);
            record = insertIntoMatcher.replaceAll("insert into " + databaseName + ".");

            Matcher deleteMatcher = deletePattern.matcher(record);
            record = deleteMatcher.replaceAll("delete from " + databaseName + ".");

            Matcher truncateMatcher = truncatePattern.matcher(record);
            record = truncateMatcher.replaceAll("truncate table " + databaseName + ".");
        }
        return record;
    }

    private static void closeInput(RandomAccessFile input) {
        if (input != null) {
            try {
                input.close();
            } catch (Throwable e) {
                log.error(e);
            }
        }
    }

    public static void main(String[] args) {
        try {
            SimpleFileSqlBroadcaster sqlBroadcaster = new SimpleFileSqlBroadcaster(false);
            sqlBroadcaster.startWithFailover();
        } catch (Exception e) {
            log.error(e);
        }
    }
}