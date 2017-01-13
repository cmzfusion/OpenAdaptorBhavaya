package org.bhavaya.coms;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Sep 4, 2006
 * Time: 10:29:14 AM
 * To change this template use File | Settings | File Templates.
 */
public interface NotificationLoggerMBean {
    public void emailLogs(String emailAddress);
    public void exit();
}
