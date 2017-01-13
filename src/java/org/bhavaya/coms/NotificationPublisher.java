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

import com.sun.org.apache.xml.internal.utils.FastStringBuffer;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.11 $
 */
public abstract class NotificationPublisher {
    private static final Log log = Log.getCategory(NotificationPublisher.class);
    private static final String NOTIFICATION_PROPERTY_GROUP = "notifications";

    private static Map instances = new HashMap();
    private static volatile boolean initialised = false;
    private static final Object initLock = new Object();

    private String subject;
    private boolean autoCommit;
    private FastStringBuffer messageBuffer;
    private int sentNotificationCounter;
    private PropertyGroup properties;
    private boolean connected;
    private int reconnectionPeriod;
    private boolean failedMessageCache;
    private long failedMessageCount;
    private List failedMessages;
    private long lastReconnectionTime;

    protected abstract void commit(String message) throws NotificationException;

    protected abstract void connectImplementation() throws NotificationException;

    protected abstract void closeImplementation();

    private static void init() {
        // Only run once
        synchronized (initLock) {
            if (initialised) return;
            initialised = true;

            PropertyGroup propertyGroup = ApplicationProperties.getApplicationProperties().getGroup(NOTIFICATION_PROPERTY_GROUP);
            if (propertyGroup != null) {
                org.bhavaya.util.PropertyGroup[] subjectsPropertyGroup = propertyGroup.getGroups("subject");
                if (subjectsPropertyGroup != null) {
                    for (int i = 0; i < subjectsPropertyGroup.length; i++) {
                        PropertyGroup subjectPropertyGroup = subjectsPropertyGroup[i];
                        String publisherMethod = subjectPropertyGroup.getMandatoryProperty("publisher");
                        String subject = subjectPropertyGroup.getMandatoryProperty("name");

                        NotificationPublisher notificationPublisher;

                        try {
                            if (publisherMethod != null && publisherMethod.length() > 0) {
                                Class publisherMethodClass = ClassUtilities.getClass(publisherMethod);
                                Constructor constructor = publisherMethodClass.getConstructor(new Class[]{PropertyGroup.class});
                                notificationPublisher = (NotificationPublisher) constructor.newInstance(new Object[]{subjectPropertyGroup});
                            } else {
                                notificationPublisher = null;
                            }
                        } catch (Exception e) {
                            log.error(e);
                            throw new RuntimeException(e);
                        }
                        log.info("Created NotificationPublisher for subject: " + subject + " of type: " + publisherMethod);
                        instances.put(subject, notificationPublisher);
                    }
                }
            }
        }
    }

    public static synchronized NotificationPublisher getInstance(String subject) {
        init();
        return (NotificationPublisher) instances.get(subject);
    }

    public NotificationPublisher(PropertyGroup properties) {
        this.properties = properties;
        subject = properties.getMandatoryProperty("name");
        autoCommit = Boolean.valueOf(properties.getMandatoryProperty("autoCommit")).booleanValue();
        messageBuffer = new FastStringBuffer(12);
        String reconnectionPeriodString = properties.getMandatoryProperty("reconnectionPeriod");
        this.reconnectionPeriod = Integer.parseInt(reconnectionPeriodString);
        connected = false;
        lastReconnectionTime = 0;
        failedMessageCache = Boolean.valueOf(properties.getMandatoryProperty("failedMessageCache")).booleanValue();
        failedMessages = new ArrayList();
    }

    public String getSubject() {
        return subject;
    }

    public int getReconnectionPeriod() {
        return reconnectionPeriod;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    protected PropertyGroup getProperties() {
        return properties;
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    public synchronized long getFailedMessageCount() {
        return failedMessageCount;
    }

    public synchronized void connect() throws NotificationException {
        connect(false);
    }

    private synchronized void connect(boolean commitExistingMessages) throws NotificationException {
        if (connected) {
            if (log.isDebug()) log.debug(subject + ": Connected, not connecting");
            return;
        }
        log.info(subject + ": Connecting");
        lastReconnectionTime = System.currentTimeMillis();
        rollback();
        connectImplementation();
        connected = true; // if we get here, connected successfully
        log.info(subject + ": Connected");

        commitExistingMessages(commitExistingMessages);
    }

    private void commitExistingMessages(boolean commitExistingMessages) throws NotificationException {
        if (failedMessageCount > 0) {
            if (failedMessageCache && commitExistingMessages) {
                try {
                    for (Iterator iterator = failedMessages.iterator(); iterator.hasNext();) {
                        String message = (String) iterator.next();
                        commitInternal(message);
                        iterator.remove();
                        failedMessageCount--;
                    }
                } catch (NotificationException e) {
                    log.error(subject + ": Failed sending previously failed notifications");
                    close();
                    throw e;
                }
            } else {
                log.warn(subject + ": Ignored " + failedMessageCount + " previously failed notifications");
                failedMessages.clear();
            }

            failedMessageCount = failedMessages.size();
        }
    }

    public synchronized void close() {
        if (!connected) {
            if (log.isDebug()) log.debug(subject + ": Not connected, not closing");
            return;
        }
        log.info(subject + ": Closing");
        connected = false;
        closeImplementation();
        log.info(subject + ": Closed");
    }

    /**
     * Synchronized on instance, to ensure correct access to messageBuffer
     */
    public synchronized void send(String message) throws NotificationException {
        if (message != null) {
            messageBuffer.append(message);
            messageBuffer.append('\n');

            if (autoCommit) {
                commit();
            }
        }
    }

    /**
     * Synchronized on instance, to ensure correct access to messageBuffer
     */
    public synchronized void commit() throws NotificationException {
        if (messageBuffer.length() > 0) {
            String message = messageBuffer.toString();

            try {
                if (!connected) {
                    if (System.currentTimeMillis() - lastReconnectionTime > reconnectionPeriod) {
                        connect(true);
                    } else {
                        addFailedMessage(message);
                        return;
                    }
                }

                commitInternal(message);
            } catch (NotificationException e) {
                addFailedMessage(message);
                close();
                throw e;
            } finally {
                rollback();
            }
        }
    }

    private void addFailedMessage(String message) {
        failedMessageCount++;

        if (failedMessageCache) {
            if (log.isDebug()) log.debug(subject + ": Failed sending notification: " + message);
            failedMessages.add(message);
        } else {
            if (log.isDebug()) log.debug(subject + ": Discarding failed notification: " + message);
        }
    }

    private void commitInternal(String message) throws NotificationException {
        sentNotificationCounter++;
        if (log.isDebug()) log.debug(subject + ": Sending notification: " + sentNotificationCounter + ": " + message);
        commit(message);
    }

    /**
     * Synchronized on instance, to ensure correct access to messageBuffer
     */
    public synchronized void rollback() {
        messageBuffer.setLength(0);
    }
}