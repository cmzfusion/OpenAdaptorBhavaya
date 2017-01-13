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
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;
import org.bhavaya.util.Utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Socket;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.9 $
 */
public class SocketNotificationSubscriber extends NotificationSubscriber {
    private static final Log log = Log.getCategory(SocketNotificationSubscriber.class);
    protected static final String HOST_PROPERTY = "host";
    protected static final String SUBSCRIBE_PORT_PROPERTY = "subscribePort";
    public static final long DEFAULT_CONNECT_TIMEOUT = 30000L;

    private Socket socket;
    private Reader in;

    public SocketNotificationSubscriber(PropertyGroup subjectPropertyGroup) {
        super(subjectPropertyGroup);
    }

    protected void connectImplementation(String host, String portString) throws NotificationException {
        int port = new Integer(portString).intValue();

        log.info(getSubject() + ": Connecting to host: " + host + ", port: " + port);

        try {
            socket = SocketUtil.newSocket(host, port, DEFAULT_CONNECT_TIMEOUT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Thread socketThread = Utilities.newThread(new Receiver(), "NotificationReceiver:" + getSubject(), true);
            socketThread.start();
        } catch (Exception e) {
            throw new NotificationException(e);
        }
    }

    protected void connectImplementation() throws NotificationException {
        String host = getProperties().getMandatoryProperty(HOST_PROPERTY);
        String portString = getProperties().getMandatoryProperty(SUBSCRIBE_PORT_PROPERTY);

        if (host.length() == 0 || portString.length() == 0) {
            throw new NotificationException("Not connecting, as no host or port specified");
        }
        connectImplementation(host, portString);
    }

    protected void closeImplementation() {
        SocketUtil.closeSocket(socket);
    }

    private class Receiver implements Runnable {

        public void run() {
            try {
                FastStringBuffer incomingNotification = new FastStringBuffer(10);
                while (true) {
                    int read = in.read();
                    if (read == -1) {
                        throw new Exception("Server closed connection");
                    } else {
                        char ch = (char)read;
                        if (ch == '\0') {
                            // end of message
                            addNotification(incomingNotification.toString());
                            incomingNotification.setLength(0);
                        } else {
                            incomingNotification.append(ch);
                        }
                    }
                }
            } catch (Exception e) {
                if (isConnected()) {
                    handleException(e);
                }
            }
        }
    }
}
