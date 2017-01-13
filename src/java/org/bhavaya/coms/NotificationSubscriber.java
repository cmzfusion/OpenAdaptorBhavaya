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

import org.bhavaya.util.*;

import java.beans.ExceptionListener;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.15 $
 */
public abstract class NotificationSubscriber {
    private static final Log log = Log.getCategory(NotificationSubscriber.class);
    private static final Log notificationLog = Log.getCategory("notification");
    private static final String NOTIFICATION_PROPERTY_GROUP = "notifications";

    private static Map instances = new HashMap();
    private static volatile boolean initialised = false;
    private static final Object initLock = new Object();

    private String subject;
    private PropertyGroup properties;
    private int maximumQueuedNotifications;

    private NotificationProcessor notificationProcessor;
    private List notificationList;
    private Object notificationListenersLock = new Object();
    private List notificationListeners = new ArrayList();
    private boolean processing;
    private boolean connected;
    private Object runLock = new Object();
    private int reconnectionPeriod;

    private List exceptionListeners = new ArrayList();
    private int exceptionCount;

    private List slowConsumerListeners = new ArrayList();
    private int slowConsumerCount;

    private Object infoListenersLock = new Object();
    private List infoListeners = new ArrayList();
    private long receivedNotificationsCount;
    private long processedNotificationsCount;

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
                        String subscriberMethod = subjectPropertyGroup.getMandatoryProperty("subscriber");
                        String subject = subjectPropertyGroup.getMandatoryProperty("name");

                        NotificationSubscriber notificationSubscriber;

                        try {
                            if (subscriberMethod != null && subscriberMethod.length() > 0) {
                                Class subscriberMethodClass = ClassUtilities.getClass(subscriberMethod);
                                Constructor constructor = subscriberMethodClass.getConstructor(new Class[]{PropertyGroup.class});
                                notificationSubscriber = (NotificationSubscriber) constructor.newInstance(new Object[]{subjectPropertyGroup});
                            } else {
                                notificationSubscriber = null;
                            }
                        } catch (Exception e) {
                            log.error(e);
                            throw new RuntimeException(e);
                        }
                        log.info("Created NotificationSubscriber for subject: " + subject + " of type: " + subscriberMethod);
                        instances.put(subject, notificationSubscriber);
                    }
                }
            }
        }
    }

    public static synchronized NotificationSubscriber getInstance(String subject) {
        init();
        return (NotificationSubscriber) instances.get(subject);
    }

    public static synchronized NotificationSubscriber[] getInstances() {
        init();
        Collection collection = instances.values();
        return (NotificationSubscriber[]) collection.toArray(new NotificationSubscriber[instances.values().size()]);
    }

    public static synchronized String[] getSubjects() {
        init();
        Collection collection = instances.keySet();
        return (String[]) collection.toArray(new String[collection.size()]);
    }

    public NotificationSubscriber(PropertyGroup properties) {
        this.properties = properties;
        subject = properties.getMandatoryProperty("name");
        String maximumQueuedNotificationsString = properties.getProperty("maximumQueuedNotificationsOnSubscriber");
        if (maximumQueuedNotificationsString == null || maximumQueuedNotificationsString.length() == 0 || Integer.parseInt(maximumQueuedNotificationsString) < 1) {
            maximumQueuedNotifications = 0;
        } else {
            maximumQueuedNotifications = Integer.parseInt(maximumQueuedNotificationsString);
        }
        notificationList = new LinkedList();
        log.info(subject + ": Maximum queued notifications: " + maximumQueuedNotifications);

        String reconnectionPeriodString = properties.getMandatoryProperty("reconnectionPeriod");
        this.reconnectionPeriod = Integer.parseInt(reconnectionPeriodString);

        connected = false;
        processing = false;
    }

    public String getSubject() {
        return subject;
    }

    public int getReconnectionPeriod() {
        return reconnectionPeriod;
    }

    protected PropertyGroup getProperties() {
        return properties;
    }

    public boolean isConnected() {
        synchronized (runLock) {
            return connected;
        }
    }

    public void connect() throws NotificationException {
        synchronized (runLock) {
            if (isConnected()) {
                if (log.isDebug()) log.debug(subject + ": Connected, not connecting");
                return;
            }
            log.info(subject + ": Connecting...");
            stopProcessing();
            clearNotifications();
            if (notificationProcessor == null) {
                notificationProcessor = new NotificationProcessor();
                notificationProcessor.start();
            }
            connectImplementation();
            // if we get here, connected successfully
            connected = true;
            log.info(subject + ": Connected");
        }
        fireInfoChanged();
    }


    public void close() {
        synchronized (runLock) {
            if (!isConnected()) {
                if (log.isDebug()) log.debug(subject + ": Not connected, not closing");
                return;
            }
            log.info(subject + ": Closing...");
            stopProcessing();
            clearNotifications();
            connected = false;
            closeImplementation();
            log.info(subject + ": Closed");
        }
        fireInfoChanged();
    }

    public void closeAndStopNotificationProcessor() {
        synchronized (runLock) {
            close();
            if (notificationProcessor != null) {
                notificationProcessor.stopWorking();
                notificationProcessor.interrupt();
                notificationProcessor = null;
            }
        }
    }

    private void clearNotifications() {
        notificationList.clear();
        receivedNotificationsCount = 0;
        processedNotificationsCount = 0;
    }

    public boolean startProcessingIfNoErrors() {
        synchronized (runLock) {
            boolean start = (this.slowConsumerCount == 0 && this.exceptionCount == 0);
            if (start) startProcessing();
            return start;
        }
    }

    public void startProcessing() {
        synchronized (runLock) {
            if (!isConnected()) {
                if (log.isDebug()) log.debug(subject + ": Not connected, will not startProcessing");
                return;
            }
            if (processing) {
                if (log.isDebug()) log.debug(subject + ": Already processing, will not startProcessing");
                return;
            }

            log.info(subject + ": startProcessing");
            processing = true;
            runLock.notify();
        }
    }

    public void stopProcessing() {
        synchronized (runLock) {
            if (!processing) {
                if (log.isDebug()) log.debug(subject + ": Not processing, will not stopProcessing");
                return;
            }
            log.info(subject + ": stopProcessing");
            processing = false;
        }
    }

    protected void addNotification(String notification) {
        if (notificationLog.isDebugEnabled())notificationLog.debug(notification);
        boolean becameSlowConsumer = false;

        synchronized (runLock) {
            if (!isConnected()) return;

            if (maximumQueuedNotifications > 0) {
                int numberOfQueuedNotifications = notificationList.size();
                if (numberOfQueuedNotifications > maximumQueuedNotifications) {
                    becameSlowConsumer = true;
                }
            }

            if (!becameSlowConsumer) {
                receivedNotificationsCount++;
                if (log.isDebug()) log.debug(subject + ": Received notification: " + receivedNotificationsCount + ": " + notification);
                notificationList.add(notification);
                runLock.notify();
            }
        }

        if (becameSlowConsumer) {
            handleSlowConsumer();
        }
        fireInfoChanged();
    }

    public void addExceptionListener(ExceptionListener exceptionListener) {
        synchronized (exceptionListeners) {
            exceptionListeners.add(exceptionListener);
        }
    }

    public void removeExceptionListener(ExceptionListener exceptionListener) {
        synchronized (exceptionListeners) {
            exceptionListeners.remove(exceptionListener);
        }
    }

    public void handleException(Exception e) {
        log.error(e);
        synchronized (runLock) {
            exceptionCount++;
            close();
        }
        fireException(e);
    }

    private void fireException(Exception e) {
        ExceptionListener[] listenersSnapshot;
        synchronized (exceptionListeners) {
            listenersSnapshot = (ExceptionListener[]) exceptionListeners.toArray(new ExceptionListener[exceptionListeners.size()]);
        }

        for (int i = 0; i < listenersSnapshot.length; i++) {
            ExceptionListener listener = listenersSnapshot[i];
            listener.exceptionThrown(e);
        }
    }

    public void addSlowConsumerListener(SlowConsumerListener slowConsumerListener) {
        synchronized (slowConsumerListeners) {
            slowConsumerListeners.add(slowConsumerListener);
        }
    }

    public void removeSlowConsumerListener(SlowConsumerListener slowConsumerListener) {
        synchronized (slowConsumerListeners) {
            slowConsumerListeners.remove(slowConsumerListener);
        }
    }

    private void handleSlowConsumer() {
        log.error(subject + ": Clearing queued notifications, the number of queued notifications has exceeded the maximum allowed: " + maximumQueuedNotifications);
        synchronized (runLock) {
            slowConsumerCount++;
            stopProcessing();
            clearNotifications();
        }
        fireSlowConsumer();
    }

    private void fireSlowConsumer() {
        SlowConsumerListener[] listenersSnapshot;
        synchronized (slowConsumerListeners) {
            listenersSnapshot = (SlowConsumerListener[]) slowConsumerListeners.toArray(new SlowConsumerListener[slowConsumerListeners.size()]);
        }

        for (int i = 0; i < listenersSnapshot.length; i++) {
            SlowConsumerListener listener = listenersSnapshot[i];
            listener.slowConsumer();
        }
    }

    public void addInfoListener(InfoListener infoListener) {
        synchronized (infoListenersLock) {
            infoListeners = Utilities.add(infoListener, infoListeners);
        }
    }

    public void removeInfoListener(InfoListener infoListener) {
        synchronized (infoListenersLock) {
            infoListeners = Utilities.remove(infoListener, infoListeners);
        }
    }

    public long getReceivedNotificationsCount() {
        return receivedNotificationsCount;
    }

    public long getProcessedNotificationsCount() {
        return processedNotificationsCount;
    }

    private void fireInfoChanged() {
        List listenersSnapshot;
        synchronized (infoListenersLock) {
            listenersSnapshot = infoListeners;
        }

        for (Iterator iterator = listenersSnapshot.iterator(); iterator.hasNext();) {
            InfoListener listener = (InfoListener) iterator.next();
            listener.connected(connected);
            listener.numberOfProcessedNotifications(processedNotificationsCount);
            listener.numberOfReceivedNotifications(receivedNotificationsCount);
        }
    }

    public void addNotificationListener(NotificationListener notificationListener) {
        synchronized (notificationListenersLock) {
            notificationListeners = Utilities.add(notificationListener, notificationListeners);
        }
    }

    public void removeNotificationListener(NotificationListener notificationListener) {
        synchronized (notificationListenersLock) {
            notificationListeners = Utilities.remove(notificationListener, notificationListeners);
        }
    }

    private void fireNotification(String notification) {
        List listenersSnapshot;
        synchronized (notificationListenersLock) {
            listenersSnapshot = notificationListeners;
        }

        for (Iterator iterator = listenersSnapshot.iterator(); iterator.hasNext();) {
            NotificationListener listener = (NotificationListener) iterator.next();
            try {
                listener.receive(notification);
            } catch (Throwable e) {
                log.error(subject + ": Error processing notification: " + processedNotificationsCount, e);
            }
        }
    }

    private class NotificationProcessor extends Thread {

        private volatile boolean keepWorking = true;

        public NotificationProcessor() {
            super("NotificationProcessor:" + subject);
            setDaemon(true);
        }

        public void run() {
            String notification = null;
            while (keepWorking) {

                synchronized (runLock) {
                    // check if paused before doing anything
                    while (!(processing && notificationList.size() > 0)) {
                        try {
                            runLock.wait();
                        } catch (InterruptedException e) {
                            log.error(e);
                        }
                    }

                    notification = (String) notificationList.remove(0);
                    processedNotificationsCount++;
                }
                try {
                    fireNotification(notification);
                    if (log.isDebug()) log.debug(subject + ": Finished processing notification: " + processedNotificationsCount);
                    fireInfoChanged();
                    if (processedNotificationsCount > 0 && processedNotificationsCount % 20 == 0) Thread.yield();
                } catch (Exception e) {
                    log.error("Exception caught processing notification " + notification, e);
                }
            }
        }

        public void stopWorking() {
            keepWorking = false;
        }
    }

    public static interface InfoListener {
        public void numberOfReceivedNotifications(long numberOfReceivedNotifications);

        public void numberOfProcessedNotifications(long numberOfProcessedNotifications);

        public void connected(boolean connected);
    }

    public static interface SlowConsumerListener {
        public void slowConsumer();
    }

    public static class ReconnectionExceptionListener implements ExceptionListener {
        private NotificationSubscriber notificationSubscriber;

        public ReconnectionExceptionListener(NotificationSubscriber notificationSubscriber) {
            this.notificationSubscriber = notificationSubscriber;
        }

        public void exceptionThrown(Exception e) {
            boolean connected = false;
            while (!connected) {
                try {
                    log.error(notificationSubscriber.subject + ": Attempting reconnection to NotificationServer");
                    notificationSubscriber.connect();
                    log.error(notificationSubscriber.subject + ": Reconnection successful");
                    notificationSubscriber.startProcessing();
                    connected = true;
                } catch (Exception ex) {
                    log.error(notificationSubscriber.subject + ": Reconnection was not successful (" + Utilities.getRootCause(ex).getMessage() + ")");
                }

                try {
                    Thread.sleep(notificationSubscriber.getReconnectionPeriod());
                } catch (InterruptedException ex) {
                    log.error(ex);
                }
            }

        }
    }
}
