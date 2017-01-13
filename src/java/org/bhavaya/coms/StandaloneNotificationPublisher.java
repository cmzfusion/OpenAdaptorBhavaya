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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

/**
 * This publisher does not depend on any Bhavaya classes or external libraries.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class StandaloneNotificationPublisher {
    private boolean autoCommit;
    private FastStringBuffer messageBuffer;
    private int sentNotificationCounter;
    private boolean connected;
    private Socket socket;
    private Writer out;
    private String host;
    private int port;

    public StandaloneNotificationPublisher(String host, int port) {
        this.host = host;
        this.port = port;
        autoCommit = true; // start in autoCommit mode by default
        messageBuffer = new FastStringBuffer();
        connected = false;
    }

    public void connect() throws IOException {
        close();
        synchronized (this) {
            System.out.println("Connecting to host: " + host + ", port: " + port);
            socket = new Socket(host, port);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            System.out.println("Connected");
        }
        // if we get here, connected successfully
        connected = true;
    }

    public void close() {
        rollback();
        if (connected) {
            connected = false;
            try {
                System.out.println("Closing socket: " + socket);
                socket.close();
            } catch (Throwable e) {
                System.err.println(e);
            }
            System.out.println("Closed");
        }
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Synchronized on instance, to ensure correct access to messageBuffer
     */
    public synchronized void send(String message) throws IOException {
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
    public synchronized void commit() throws IOException {
        if (messageBuffer.length() > 0) {
            sentNotificationCounter++;
            String message = messageBuffer.toString();
            System.out.println("Sending notification: " + sentNotificationCounter + ": " + message);
            message = message + '\0';// append 'end-of-message' with a null character
            out.write(message);
            out.flush();
            messageBuffer.setLength(0);
        }
    }

    /**
     * Synchronized on instance, to ensure correct access to messageBuffer
     */
    public synchronized void rollback() {
        messageBuffer.setLength(0);
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public synchronized void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

}
