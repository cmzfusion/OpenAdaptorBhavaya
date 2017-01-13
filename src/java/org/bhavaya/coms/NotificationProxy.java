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

import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;
import org.bhavaya.util.Utilities;

import java.io.*;
import java.net.Socket;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */
public class NotificationProxy {
    private static final Log log = Log.getCategory(NotificationProxy.class);
    private static final String REMOTE_HOST_PROPERTY = "remoteHost";
    private static final String REMOTE_PORT_PROPERTY = "remotePort";

    private static final String LOCALHOST = "localhost";

    private String remoteHost;
    private int remotePort;
    private Socket remoteSocket;
    private InputStream in;

    private int localPort;
    private Socket localSocket;
    private OutputStream out;

    private Thread socketThread;
    private int retryPeriod;

    public NotificationProxy(String remoteHost, int remotePort, int localPort, int retryPeriod) throws NotificationException {
        if (remoteHost.length() == 0) {
            throw new NotificationException("Not connecting, as no remoteHost or remotePort specified");
        }

        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.retryPeriod = retryPeriod;

        try {
            socketThread = Utilities.newThread(new Receiver(), "NotificationReceiver", true);
        } catch (Exception e) {
            throw new NotificationException(e);
        }
    }

    private void connect() throws IOException {
        log.info("Connecting to remote remoteHost: " + remoteHost + ", remotePort: " + remotePort);
        remoteSocket = SocketUtil.newSocket(remoteHost, remotePort, 0L);
        in = new BufferedInputStream(remoteSocket.getInputStream());

        localSocket = SocketUtil.newSocket(LOCALHOST, localPort, 0L);
        out = new BufferedOutputStream(localSocket.getOutputStream());
    }

    public void start() {
        socketThread.start();
    }

    private class Receiver implements Runnable {
        public void run() {
            while (true) {
                try {
                    connect();
                    while (true) {
                        char ch = (char) in.read();
                        out.write(ch);
                        if (ch == '\0') out.flush();
                    }
                } catch (Exception e) {
                    log.warn("Disconnected from remote server, will try reconnect in " + (retryPeriod / 1000) + " seconds", e);
                    SocketUtil.closeSocket(remoteSocket);
                    SocketUtil.closeSocket(localSocket);
                }

                try {
                    Thread.sleep(retryPeriod);
                } catch (Exception e) {
                }
            }
        }
    }


    public static void main(String[] args) throws Exception {
        String subject = args[0];
        PropertyGroup notificationPropertyGroup = ApplicationProperties.getApplicationProperties().getGroup(NotificationServer.NOTIFICATION_PROPERTY_GROUP);
        PropertyGroup subjectProperties = null;
        org.bhavaya.util.PropertyGroup[] subjectsPropertyGroup = notificationPropertyGroup.getGroups("subject");
        for (int i = 0; i < subjectsPropertyGroup.length && subjectProperties == null; i++) {
            if (subjectsPropertyGroup[i].getProperty("name").equals(subject)) {
                subjectProperties = subjectsPropertyGroup[i];
            }
        }

        if (subjectProperties == null) {
            throw new RuntimeException("Cannot find subject: " + subject);
        }

        String remoteHost = subjectProperties.getMandatoryProperty(REMOTE_HOST_PROPERTY);
        String remotePortString = subjectProperties.getMandatoryProperty(REMOTE_PORT_PROPERTY);
        int remotePort = Integer.parseInt(remotePortString);
        String reconnectionPeriodString = subjectProperties.getMandatoryProperty(NotificationServer.RECONNECTION_PERIOD_PROPERTY);
        int reconnectionPeriod = Integer.parseInt(reconnectionPeriodString);

        NotificationServer notificationServer = NotificationServer.newInstance(subject, false);
        notificationServer.start();
        NotificationProxy notificationProxy = new NotificationProxy(remoteHost, remotePort, notificationServer.getRxPort(), reconnectionPeriod);
        notificationProxy.start();
    }
}
