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

import com.sun.jdmk.comm.HtmlAdaptorServer;
import org.bhavaya.util.*;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cool, fast and hip.
 *
 * @author Philip Milne
 * @author Brendon McLean
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public class NotificationServer {
    private static final Log log = Log.getCategory(NotificationServer.class);

    private static final int BUFFER_INIT_SIZE = 8192;

    public static final String DEFAULT_NOTIFICATION_SUBJECT = "sql";

    public static final String NOTIFICATION_PROPERTY_GROUP = "notifications";
    public static final String RECONNECTION_PERIOD_PROPERTY = "serverReconnectionPeriod";
    public static final String PUBLISH_PORT_PROPERTY = "publishPort";
    public static final String SUBSCRIBE_PORT_PROPERTY = "subscribePort";
    public static final String DIAGNOSTIC_PORT_PROPERTY = "diagnosticPort";
    private static final String MAX_QUEUED_NOTIFICATIONS_PER_SUBSCRIBER_PROPERTY = "maximumQueuedNotificationsPerSubscriberOnServer";

    private int txPort;
    private int rxPort;
    private int diagnosticPort;
    private boolean daemon;
    private long reconnectionPeriod;
    private int maximumQueuedNotificationsPerSubscriber;

    private TxDispatcher txDispatcher;
    private RxDispatcher rxDispatcher;
    private MBeanServer mBeanServer;
    private HtmlAdaptorServer htmlAdaptor;


    public static NotificationServer newInstance(String subject, boolean daemon) throws Exception {
        log.info("Using Subject: " + subject);

        PropertyGroup notificationProperties = ApplicationProperties.getApplicationProperties().getGroup(NOTIFICATION_PROPERTY_GROUP);

        PropertyGroup subjectProperties = null;
        org.bhavaya.util.PropertyGroup[] subjectsPropertyGroup = notificationProperties.getGroups("subject");
        for (int i = 0; i < subjectsPropertyGroup.length && subjectProperties == null; i++) {
            if (subjectsPropertyGroup[i].getProperty("name").equals(subject)) {
                subjectProperties = subjectsPropertyGroup[i];
            }
        }

        if (subjectProperties == null) {
            throw new RuntimeException("Cannot find subject: " + subject);
        }

        String txPortString = subjectProperties.getMandatoryProperty(SUBSCRIBE_PORT_PROPERTY);
        String rxPortString = subjectProperties.getMandatoryProperty(PUBLISH_PORT_PROPERTY);
        String diagnosticPortString = subjectProperties.getMandatoryProperty(DIAGNOSTIC_PORT_PROPERTY);
        String maximumQueuedNotificationsPerSubscriberString = subjectProperties.getMandatoryProperty(MAX_QUEUED_NOTIFICATIONS_PER_SUBSCRIBER_PROPERTY);
        String reconnectionPeriodString = subjectProperties.getMandatoryProperty(RECONNECTION_PERIOD_PROPERTY);

        int txPort = Integer.parseInt(txPortString);
        int rxPort = Integer.parseInt(rxPortString);
        int diagnosticPort = Integer.parseInt(diagnosticPortString);
        int maximumQueuedNotificationsPerSubscriber = Integer.parseInt(maximumQueuedNotificationsPerSubscriberString);
        long reconnectionPeriod = Long.parseLong(reconnectionPeriodString);

        return new NotificationServer(txPort, rxPort, diagnosticPort, daemon, reconnectionPeriod, maximumQueuedNotificationsPerSubscriber);
    }

    public NotificationServer(int txPort, int rxPort, int diagnosticPort, boolean daemon, long reconnectionPeriod, int maximumQueuedNotificationsPerSubscriber) throws Exception {
        log.info("Notification server instantiating");
        this.txPort = txPort;
        this.rxPort = rxPort;
        this.diagnosticPort = diagnosticPort;
        this.daemon = daemon;
        this.reconnectionPeriod = reconnectionPeriod;
        this.maximumQueuedNotificationsPerSubscriber = maximumQueuedNotificationsPerSubscriber;
        setupDiagnostics(diagnosticPort);
        log.info("Notification server instantiated");
    }

    private void setupDiagnostics(int diagnosticPort) throws Exception {
        log.info("Binding diagnostics to port: " + diagnosticPort);
        htmlAdaptor = new HtmlAdaptorServer(diagnosticPort);
        htmlAdaptor.start();
        while (htmlAdaptor.getState() == HtmlAdaptorServer.STARTING) Thread.sleep(100);
        if (!htmlAdaptor.isActive()) throw new IOException("Could not bind diagnostics to port: " + diagnosticPort);
        mBeanServer = MBeanServerFactory.createMBeanServer(Utilities.MBEANSERVER_DOMAIN);
        mBeanServer.registerMBean(htmlAdaptor, new ObjectName("Adaptor:name=html,port=" + diagnosticPort));
    }

    /**
     * Gets the port which notification are sent to
     */
    public int getTxPort() {
        return txPort;
    }

    /**
     * Gets the port which notification are published on
     */
    public int getRxPort() {
        return rxPort;
    }

    public int getDiagnosticPort() {
        return diagnosticPort;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public long getReconnectionPeriod() {
        return reconnectionPeriod;
    }

    public int getMaximumQueuedNotificationsPerSubscriber() {
        return maximumQueuedNotificationsPerSubscriber;
    }

    public MBeanServer getMBeanServer() {
        return mBeanServer;
    }

    public void start() throws Exception {
        log.info("Notification server starting");
        log.info("Instantiating dispatchers");
        txDispatcher = new TxDispatcher(txPort, reconnectionPeriod, daemon, maximumQueuedNotificationsPerSubscriber, mBeanServer);
        rxDispatcher = new RxDispatcher(rxPort, reconnectionPeriod, daemon, txDispatcher, mBeanServer);
        startDispatcher(txDispatcher);
        startDispatcher(rxDispatcher);
        log.info("Notification server started");
    }

    private void startDispatcher(Dispatcher dispatcher) throws Exception {
        if (dispatcher == null) return;
        log.info("Registering MBean: " + dispatcher.getObjectName());
        mBeanServer.registerMBean(dispatcher, new ObjectName(dispatcher.getObjectName()));
        dispatcher.start();
    }

    private void stopDispatcher(Dispatcher dispatcher) {
        if (dispatcher == null) return;
        log.info("Unregistering MBean: " + dispatcher.getObjectName());
        unregisterMBean(dispatcher.getObjectName());
        dispatcher.close();
    }

    private void unregisterMBean(String objectName) {
        try {
            log.info("Unregistering MBean: " + objectName);
            mBeanServer.unregisterMBean(new ObjectName(objectName));
        } catch (Exception e) {
            log.error(e);
        }
    }

    public void close() {
        log.info("Notification server closing");
        stopDispatcher(rxDispatcher);
        rxDispatcher = null;
        stopDispatcher(txDispatcher);
        txDispatcher = null;
        log.info("Notification server closed");
    }


    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
        }
    }

    public static interface ConnectionMBean {
        public void close();

        public int getMessageCount();

        public boolean isActive();

        public String getName();
    }

    private abstract static class Connection extends Thread implements ConnectionMBean {
        protected Socket socket;
        protected boolean active;
        protected int messageCount = 0;

        public Connection(String name, Socket socket, boolean daemon) {
            super(name + "(" + socket.getInetAddress() + ":" + socket.getPort() + ")");
            this.socket = socket;
            this.active = true;
            setDaemon(daemon);
        }

        public String toString() {
            return getName();
        }

        public void close() {
            log.info(getName() + ": Terminating after " + messageCount + " messages");
            active = false;
            closeImplementation();
            closeSocket(socket);
            if (log.isDebug()) log.debug(getName() + ": Terminated");
        }

        protected abstract void closeImplementation();

        protected void finalize() throws Throwable {
            if (log.isDebug()) log.debug(getName() + ": garbage collecting");
            super.finalize();
        }

        public int getMessageCount() {
            return messageCount;
        }

        public boolean isActive() {
            return active;
        }

        public String getObjectName() {
            return Utilities.MBEANSERVER_DOMAIN + ":type=" + ClassUtilities.getUnqualifiedClassName(getClass()) + ",host=" + socket.getInetAddress() + ",port=" + socket.getPort();
        }
    }

    private static class RXConnection extends Connection {
        private BufferedReader reader;
        private RxDispatcher rxDispatcher;
        private TxDispatcher txDispatcher;

        private RXConnection(Socket socket, boolean daemon, RxDispatcher rxDispatcher, TxDispatcher txDispatcher) throws IOException {
            super("RXConnection", socket, daemon);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.rxDispatcher = rxDispatcher;
            this.txDispatcher = txDispatcher;
        }

        public void run() {
            // Why not StringBuffer: because it's heinously slow compared to this
            char[] msgBuffer = new char[BUFFER_INIT_SIZE];
            int bufferCapacity = BUFFER_INIT_SIZE;

            int bufferIndex;
            char[] msg;
            int ch;

            while (active) {
                bufferIndex = 0;
                try {
                    do {
                        ch = reader.read();
                        if (ch == -1 || ch == Character.MAX_VALUE) { // We don't know why Character.MAX_VALUE, but remove at your peril.
                            active = false;
                            break;
                        }

                        if (bufferIndex >= bufferCapacity) {
                            bufferCapacity *= 2;
                            char[] newBuffer = new char[bufferCapacity];
                            System.arraycopy(msgBuffer, 0, newBuffer, 0, msgBuffer.length);
                            msgBuffer = newBuffer;
                        }

                        msgBuffer[bufferIndex++] = (char) ch;
                    } while (ch != '\0');

                    if (!active) {
                        break;
                    }

                    msg = new char[bufferIndex];
                    System.arraycopy(msgBuffer, 0, msg, 0, bufferIndex);
                    messageCount++;
                } catch (IOException e) {
                    active = false;
                    break;
                }

                txDispatcher.processLine(msg);
            }

            rxDispatcher.closeConnection(this);
        }

        protected void closeImplementation() {
        }
    }


    private static class TXConnection extends Connection {
        private Writer writer;
        private int maximumQueuedNotificationsPerSubscriber;
        private List lineBuffer = new ArrayList();
        private TxDispatcher txDispatcher;

        private TXConnection(Socket socket, boolean daemon, TxDispatcher txDispatcher, int maximumQueuedNotificationsPerSubscriber) throws IOException {
            super("TXConnection", socket, daemon);
            this.maximumQueuedNotificationsPerSubscriber = maximumQueuedNotificationsPerSubscriber;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.txDispatcher = txDispatcher;
        }

        public void run() {
            while (active) {
                try {
                    Object[] lineBufferCopy;
                    synchronized (lineBuffer) {
                        while (active && lineBuffer.isEmpty()) {
                            //nothing to do, relinquish the lock
                            lineBuffer.wait();
                        }

                        lineBufferCopy = lineBuffer.toArray(new Object[lineBuffer.size()]);
                        lineBuffer.clear();
                    }

                    if (active && !isInterrupted()) {
                        for (int i = 0; i < lineBufferCopy.length && active; i++) {
                            char[] line = (char[]) lineBufferCopy[i];
                            writer.write(line);
                            writer.flush();
                            messageCount++;
                        }
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    log.info(getName() + ": Has been interrupted and will now die");
                    active = false;
                    break;
                } catch (IOException e) {
                    active = false;
                    break;
                }
            }

            txDispatcher.closeConnection(this);
        }

        protected void closeImplementation() {
            synchronized (lineBuffer) {
                lineBuffer.notify();// required if called from a seperate thread
            }
        }

        public void processLine(char[] line) {
            if (!isInterrupted()) {
                //make sure we are locked
                synchronized (lineBuffer) {
                    int bufferDepth = lineBuffer.size();
                    lineBuffer.add(line);

                    // Wake up sender thread after releasing lock.
                    if (maximumQueuedNotificationsPerSubscriber >= 0 && bufferDepth > maximumQueuedNotificationsPerSubscriber) {
                        log.warn(getName() + ": is too slow.  Interrupting thread to kick client");
                        interrupt();
                    }

                    lineBuffer.notify();
                }
            }
        }
    }

    public static interface DispatcherMBean {
        public void close();

        public int getConnectionCount();

        public boolean isActive();

    }

    private static abstract class Dispatcher extends Thread implements DispatcherMBean {
        private ServerSocket serverSocket;
        protected List clientConnections;
        private int port;
        private long reconnectionPeriod;
        private boolean daemon;
        private MBeanServer mBeanServer;
        private boolean active;

        public Dispatcher(String name, long reconnectionPeriod, int port, boolean daemon, MBeanServer mBeanServer) throws IOException {
            super(name);
            this.port = port;
            this.reconnectionPeriod = reconnectionPeriod;
            this.daemon = daemon;
            this.mBeanServer = mBeanServer;
            this.active = true;
            clientConnections = Collections.synchronizedList(new ArrayList());

            log.info(getName() + ": Binding to port " + port);
            try {
                serverSocket = new ServerSocket(port);
                log.info(getName() + ": Bind successful");
            } catch (IOException e) {
                log.error(getName() + ": Could not bind to port " + port, e);
                throw e;
            }

            setDaemon(daemon);
            log.info(getName() + ": reconnectionPeriod: " + reconnectionPeriod / 1000.0 + " seconds");
        }

        public void run() {
            log.info(getName() + ": Starting thread");
            int numFailures = 0;

            while (active) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Connection connection;

                    synchronized (clientConnections) {
                        if (!active) {
                            closeSocket(clientSocket);
                            break;
                        }
                        connection = createConnection(clientSocket, daemon);
                        clientConnections.add(connection);
                        mBeanServer.registerMBean(connection, new ObjectName(connection.getObjectName()));
                    }

                    log.info(connection + ": Connected");
                    connection.start();
                    numFailures = 0;
                } catch (Exception e) {
                    synchronized (clientConnections) {
                        if (!active) break;
                    }

                    numFailures++;
                    log.error(getName() + ": Problem accepting connections on port " + port, e);
                    log.error(getName() + ": Sleeping for: " + reconnectionPeriod / 1000.0 + " seconds and trying again.  Attempt: " + numFailures);

                    try {
                        Thread.sleep(reconnectionPeriod);
                        log.error(getName() + ": Creating a new socket");
                        serverSocket.close();
                        serverSocket = new ServerSocket(port);
                        log.error(getName() + ": Created a new socket");
                    } catch (Exception ex) {
                        log.error(getName() + ": Could not create a new socket", e);
                    }
                }
            }

            if (log.isDebug()) log.debug(getName() + ": Ending thread");
        }

        public void close() {
            log.info(getName() + ": closing");
            synchronized (clientConnections) {
                active = false;

                // prevent new connections
                if (serverSocket != null) {
                    try {
                        log.info(getName() + ": closing serverSocket");
                        serverSocket.close();
                        serverSocket = null;
                    } catch (IOException e) {
                        log.error(e);
                    }
                }

                // kick off all existing connections
                Connection[] connections = getConnections();
                for (int i = 0; i < connections.length; i++) {
                    Connection connection = connections[i];
                    closeConnection(connection);
                }
            }
            log.info(getName() + ": closed");
        }

        public Connection[] getConnections() {
            synchronized (clientConnections) {
                return (Connection[]) clientConnections.toArray(new Connection[clientConnections.size()]);
            }
        }

        public int getConnectionCount() {
            return clientConnections.size();
        }

        public void closeConnection(Connection connection) {
            boolean removed = clientConnections.remove(connection);
            if (removed) {
                try {
                    log.info("Unregistering MBean: " + connection.getObjectName());
                    mBeanServer.unregisterMBean(new ObjectName(connection.getObjectName()));
                } catch (Exception e) {
                    log.error(e);
                }
                connection.close(); //connection may have been removed on another thread
            }
        }

        protected abstract Connection createConnection(Socket socket, boolean daemon) throws IOException;

        protected void finalize() throws Throwable {
            if (log.isDebug()) log.debug(getName() + ": garbage collecting");
            super.finalize();
        }

        public boolean isActive() {
            return active;
        }

        public String getObjectName() {
            return Utilities.MBEANSERVER_DOMAIN + ":type=" + ClassUtilities.getUnqualifiedClassName(getClass()) + ",port=" + port;
        }
    }

    private static class RxDispatcher extends Dispatcher {
        private TxDispatcher txDispatcher;

        public RxDispatcher(int rxPort, long reconnectionPeriod, boolean daemon, TxDispatcher txDispatcher, MBeanServer mBeanServer) throws IOException {
            super("RXDispatcher", reconnectionPeriod, rxPort, daemon, mBeanServer);
            this.txDispatcher = txDispatcher;
        }

        protected Connection createConnection(Socket socket, boolean daemon) throws IOException {
            return new RXConnection(socket, daemon, this, txDispatcher);
        }
    }

    private static class TxDispatcher extends Dispatcher {
        private int maximumQueuedNotificationsPerSubscriber;

        public TxDispatcher(int txPort, long reconnectionPeriod, boolean daemon, int maximumQueuedNotificationsPerSubscriber, MBeanServer mBeanServer) throws IOException {
            super("TXDispatcher", reconnectionPeriod, txPort, daemon, mBeanServer);
            this.maximumQueuedNotificationsPerSubscriber = maximumQueuedNotificationsPerSubscriber;
            log.info(getName() + ": maximumQueuedNotificationsPerSubscriber: " + maximumQueuedNotificationsPerSubscriber);
        }

        protected Connection createConnection(Socket socket, boolean daemon) throws IOException {
            return new TXConnection(socket, daemon, this, maximumQueuedNotificationsPerSubscriber);
        }

        public TXConnection[] getTxConnections() {
            synchronized (clientConnections) {
                return (TXConnection[]) clientConnections.toArray(new TXConnection[clientConnections.size()]);
            }
        }

        public void processLine(char[] msg) {
            // Take a snapshot of the listeners to spend as little time locked on client connections as possible
            TXConnection[] txConnections = getTxConnections();

            for (int i = 0; i < txConnections.length && msg != null; i++) {
                txConnections[i].processLine(msg);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        NotificationServer notificationServer;
        if (args.length == 1) {
            String subject = args[0];
            notificationServer = NotificationServer.newInstance(subject, false);
        } else if (args.length == 5) {
            int rxPort = Integer.parseInt(args[0]);
            int txPort = Integer.parseInt(args[1]);
            int diagnosticPort = Integer.parseInt(args[2]);
            long reconnectionPeriod = Long.parseLong(args[3]);
            int maximumQueuedNotificationsPerSubscriber = Integer.parseInt(args[4]);
            notificationServer = new NotificationServer(rxPort, txPort, diagnosticPort, false, reconnectionPeriod, maximumQueuedNotificationsPerSubscriber);
        } else {
            throw new RuntimeException("Invalid parameters");
        }
        notificationServer.start();
    }
}