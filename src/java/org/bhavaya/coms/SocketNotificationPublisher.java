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

import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

/**
 * Description
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public class SocketNotificationPublisher extends NotificationPublisher {
    private static final Log log = Log.getCategory(SocketNotificationPublisher.class);
    private static final String HOST_PROPERTY = "host";
    private static final String PUBLISH_PORT_PROPERTY = "publishPort";

    private Socket socket;
    private Writer out;

    public SocketNotificationPublisher(PropertyGroup subjectPropertyGroup) {
        super(subjectPropertyGroup);
    }

    protected synchronized void connectImplementation() throws NotificationException {
        String host = getProperties().getMandatoryProperty(HOST_PROPERTY);
        int port = new Integer(getProperties().getMandatoryProperty(PUBLISH_PORT_PROPERTY)).intValue();

        log.info(getSubject() + ": Connecting to host: " + host + ", port: " + port);

        try {
            socket = SocketUtil.newSocket(host, port, 0L);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            throw new NotificationException(e);
        }
    }

    protected void closeImplementation() {
        SocketUtil.closeSocket(socket);
        socket = null;
        out = null;
    }

    protected void commit(String message) throws NotificationException {
        try {
            message = message + '\0';// append 'end-of-message' with a null character
            out.write(message);
            out.flush();
        } catch (IOException e) {
            throw new NotificationException(e);
        }
    }
}
