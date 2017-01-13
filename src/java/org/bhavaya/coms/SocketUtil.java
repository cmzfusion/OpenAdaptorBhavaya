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

import java.io.IOException;
import java.net.Socket;

/**
 * Heavily revised version of this class that now creates new sockets in a background
 * thread and includes a timeout option to avoid an observed problem with Socket's
 * constructor blocking indefinitely.  The previous blocking behaviour can still be
 * obtained by passing a timeout of 0 to the <code>newSocket</code> method.
 * @author Parwinder Sekhon
 * @author James Langley
 * @version $Revision: 1.3 $
 */
public class SocketUtil {
    private static final Log log = Log.getCategory(SocketUtil.class);

    /**
     * Creates a new socket to the specified host and port, waiting for the specified
     * timeout period.  If the timeout is 0, then the method will wait indefinitely.
     * @param host the host to connect to.
     * @param port the port to connect to.
     * @param timeout how long to wait for a connection to be established.
     * @return the newly created socket.
     * @throws IOException if an exception occurs whilst creating the socket or if
     * the timeout is reached.
     */
    public static Socket newSocket(String host, int port, long timeout) throws IOException {
        assert timeout >=0 : "Attempt to create a socket with a negative timeout";

        NewSocketThread newSocketThread = new NewSocketThread(host, port);
        newSocketThread.setDaemon(true);
        newSocketThread.start();
        try {
            newSocketThread.join(timeout);
        } catch (InterruptedException e) {
            log.error(e);
        }
        if (newSocketThread.isAlive()) {
            newSocketThread.cancel();
            throw new IOException("Timeout whilst connecting to: [" + host + " : " + port + "]");
        } else {
            Socket socket = newSocketThread.getSocket();
            if (socket == null) {
                newSocketThread.cancel();
                IOException cause = newSocketThread.getException();
                IOException exceptionToThrow = new IOException("Error occurred whilst creating socket to: [" + host + " : " + port + "]");
                exceptionToThrow.initCause(cause);
                throw exceptionToThrow;
            } else {
                return socket;
            }
        }
    }

    public static void closeSocket(Socket socket) {
        if (socket == null) return;
        try {
            if (socket.isClosed()) return;
            log.info("Closing socket: " + socket);
            socket.close();
        } catch (Throwable e) {
            log.error(e);
        }
    }

    /**
     * Creates a socket in a background thread to avoid blocking calls
     * to Socket's constructor.  The creation can also be cancelled.
     * If cancelled, the newly created socket will be closed automatically.
     */
    private static class NewSocketThread extends Thread {
        private Socket socket;
        private String host;
        private int port;
        private IOException exception;
        private boolean cancelled = false;

        public NewSocketThread(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public synchronized void cancel() {
            cancelled = true;
            log.info("Closing socket as creation was cancelled");
            closeSocket(socket);
        }

        public Socket getSocket() {
            return socket;
        }

        public IOException getException() {
            return exception;
        }

        public void run() {
            try {
                log.info("Opening socket [" + host + " : " + port + "]");
                socket = new Socket(host, port);
            } catch (IOException e) {
                exception = e;
            } finally {
                synchronized(this) {
                    if (cancelled) {
                        log.info("Closing socket as creation was cancelled");
                        closeSocket(socket);
                        socket = null;
                    }
                }
            }
        }
    }
}
