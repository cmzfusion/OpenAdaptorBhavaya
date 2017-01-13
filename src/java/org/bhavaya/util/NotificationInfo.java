package org.bhavaya.util;

import org.bhavaya.coms.NotificationSubscriber;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Jul 25, 2005
 * Time: 10:20:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class NotificationInfo extends DefaultObservable {
    public static final Log log = Log.getCategory(NotificationInfo.class);

    private String sqlNotificationSubject;
    private boolean realtime;
    private long numberOfReceivedNotifications;
    private long numberOfProcessedNotifications;

    private NotificationSubscriber notificationSubscriber;

    public NotificationInfo(String sqlNotificationSubject) {
        this.sqlNotificationSubject = sqlNotificationSubject;

        notificationSubscriber = NotificationSubscriber.getInstance(sqlNotificationSubject);
        if (notificationSubscriber != null) {
            notificationSubscriber.addInfoListener(new NotificationInfoListener(notificationSubscriber));
        } else {
            setRealtime(false);
        }
    }

    public String getSqlNotificationSubject() {
        return sqlNotificationSubject;
    }

    public boolean getRealtime() {
        return realtime;
    }

    public void setRealtime(boolean realtime) {
        boolean oldValue = this.realtime;
        this.realtime = realtime;
        firePropertyChange("realtime", oldValue, this.realtime);
    }

    public long getNumberOfReceivedNotifications() {
        return numberOfReceivedNotifications;
    }

    public void setNumberOfReceivedNotifications(long numberOfReceivedNotifications) {
        long oldValue = this.numberOfReceivedNotifications;
        this.numberOfReceivedNotifications = numberOfReceivedNotifications;
        firePropertyChange("numberOfReceivedNotifications", oldValue, numberOfReceivedNotifications);
    }

    public long getNumberOfProcessedNotifications() {
        return numberOfProcessedNotifications;
    }

    public void setNumberOfProcessedNotifications(long numberOfProcessedNotifications) {
        long oldValue = this.numberOfProcessedNotifications;
        this.numberOfProcessedNotifications = numberOfProcessedNotifications;
        firePropertyChange("numberOfProcessedNotifications", oldValue, numberOfProcessedNotifications);
    }

    private class NotificationInfoListener implements NotificationSubscriber.InfoListener {
        public NotificationInfoListener(NotificationSubscriber notificationSubscriber) {
            setRealtime(notificationSubscriber.isConnected());
            setNumberOfProcessedNotifications(notificationSubscriber.getProcessedNotificationsCount());
            setNumberOfReceivedNotifications(notificationSubscriber.getReceivedNotificationsCount());
        }

        public void numberOfReceivedNotifications(final long numberOfReceivedNotifications) {
            setNumberOfReceivedNotifications(numberOfReceivedNotifications);
        }

        public void numberOfProcessedNotifications(final long numberOfProcessedNotifications) {
            setNumberOfProcessedNotifications(numberOfProcessedNotifications);
        }

        public void connected(final boolean connected) {
            setRealtime(connected);
        }
    }
}
