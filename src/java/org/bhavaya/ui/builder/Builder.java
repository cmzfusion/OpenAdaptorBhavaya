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

package org.bhavaya.ui.builder;

import org.bhavaya.beans.Schema;
import org.bhavaya.beans.generator.Application;
import org.bhavaya.coms.SocketUtil;
import org.bhavaya.ui.ArrayEnumeration;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public class Builder {
    private static final Log log = Log.getCategory(Builder.class);

    public static final ImageIcon FRAME_ICON = ImageIconCache.getImageIcon("bhavaya_icon.gif");

    protected static final String GENERATE_ICON = "generate.png";
    protected static final String RUN_ICON = "run.gif";
    protected static final String EDIT_ICON = "edit.gif";
    protected static final String COPY_ICON = "copy.png";
    protected static final String REMOVE_ICON = "remove.gif";
    protected static final String SAVE_ICON = "save16.gif";
    protected static final String BLANK_ICON = "blank16.gif";

    private State state;
    private String baseDir;
    private JFrame frame;
    private ApplicationTable table;
    private int urlClassLoaderPort;
    private String urlClassLoaderHost;

    public static void main(String[] args) {
        try {
            String baseDir = IOUtilities.getUserBaseDirectory() + File.separator + ".bhavayaBuilder";
            new Builder(baseDir);
        } catch (IOException e) {
            handleException(e);
        }
    }

    protected static void handleException(Throwable e) {
        log.error(e);
        StringBuffer message = new StringBuffer("Could not start: " + Builder.class.getName() + "\n");
        appendException(e, message);
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static void appendException(Throwable e, StringBuffer message) {
        message.append(e).append("\n");
        Throwable parent = e;
        Throwable cause = e.getCause();
        while (cause != null && cause != parent) {
            message.append(cause).append("\n");
            parent = cause;
            cause = parent.getCause();
        }
    }

    public Builder(final String baseDir) throws IOException {
        System.setSecurityManager(null); // for Webstart, otherwise we get a PermissionExceptions
        System.setProperty("sun.awt.exception.handler", "org.bhavaya.ui.ExceptionHandler");
        ClassUtilities.setGenerateClassesDynamically(false);
        ClassUtilities.setParentClassLoader(new MutableUrlClassLoader(this.getClass().getClassLoader()));
        Schema.setGenerationMode(true);
        PropertyModel.setStrategy(new PropertyModel.DefaultStrategy());

        copyClass(ApplicationLauncher.class, baseDir);
        copyClass(ApplicationLauncher.SocketUrlClassLoader.class, baseDir);
        copyClass(ArrayEnumeration.class, baseDir);
        copyClass(MutableUrlClassLoader.class, baseDir);

        SocketClassURLServer socketClassURLServer = new SocketClassURLServer();
        urlClassLoaderPort = socketClassURLServer.getPort();
        urlClassLoaderHost = "localhost";

        try {
            UIManager.setLookAndFeel(createLookAndFeel());
            UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());
        } catch (UnsupportedLookAndFeelException e) {
            log.error(e);
        }

        this.baseDir = baseDir;
        state = getState(baseDir);

        JPanel contentPanel = new JPanel(new BorderLayout());
        table = new ApplicationTable(state, baseDir, urlClassLoaderHost, urlClassLoaderPort);
        JScrollPane tableComponent = new JScrollPane(table);
        contentPanel.add(tableComponent, BorderLayout.CENTER);
        contentPanel.add(createToolBar(baseDir), BorderLayout.NORTH);

        frame = new JFrame("Bhavaya Builder");
        frame.setIconImage(FRAME_ICON.getImage());
        frame.setJMenuBar(createMenuBar());
        frame.setContentPane(contentPanel);

        if (state.getFrameConfig() != null) {
            frame.setBounds(state.getFrameConfig().getFrameBounds());
            if (!UIUtilities.getDefaultScreenSizeWithoutAdjustment().contains(frame.getBounds())) UIUtilities.centreInScreen(frame, 0, 0);
        } else {
            frame.setSize(1000, 400);
            UIUtilities.centreInScreen(frame, 0, 0);
        }

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new ExitWindowAdapter());
        frame.show();
    }

    private static void copyClass(Class clazz, String baseDir) throws IOException {
        File outFile = new File(baseDir + File.separator + "classes" + ClassUtilities.classToFilename(clazz));
        log.info("Copying " + clazz.getName() + " to " + outFile.getAbsolutePath());
        if (!outFile.getParentFile().exists()) outFile.getParentFile().mkdirs();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = ClassUtilities.getURLForClass(clazz).openStream();
            out = new FileOutputStream(outFile);
            byte[] buffer = new byte[10000];
            int length = in.read(buffer);
            while (length != -1) {
                out.write(buffer, 0, length);
                length = in.read(buffer);
            }
        } finally {
            IOUtilities.closeStream(in);
            IOUtilities.closeStream(out);
        }
    }

    protected LookAndFeel createLookAndFeel() {
        return UIUtilities.createDefaultLookAndFeel();
    }

    private JToolBar createToolBar(final String baseDir) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);

        toolBar.add(new SaveAction());
        toolBar.add(new AddApplicationAction(state.getApplications()));
        toolBar.addSeparator();

        toolBar.add(new GenerateApplicationAction(table, baseDir));
        toolBar.add(new RunApplicationAction(table, state, baseDir, urlClassLoaderHost, urlClassLoaderPort));
        toolBar.add(new EditApplicationAction(table, state.getApplications()));
        toolBar.add(new CopyApplicationAction(table, state.getApplications()));
        toolBar.add(new RemoveApplicationAction(table, state.getApplications(), baseDir));
        return toolBar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        fileMenu.add(new JMenuItem(new AddApplicationAction(state.getApplications())));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new SaveAction()));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new ExitAction()));
        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        editMenu.add(new GenerateApplicationAction(table, baseDir));
        editMenu.add(new RunApplicationAction(table, state, baseDir, urlClassLoaderHost, urlClassLoaderPort));
        editMenu.addSeparator();
        editMenu.add(new EditApplicationAction(table, state.getApplications()));
        editMenu.add(new CopyApplicationAction(table, state.getApplications()));
        editMenu.add(new RemoveApplicationAction(table, state.getApplications(), baseDir));
        menuBar.add(editMenu);

        return menuBar;
    }

    private void exit() {
        int ret = JOptionPane.showConfirmDialog(frame, "Exit the application?", "Exit", JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.OK_OPTION) {
            requestSaveState();
            System.exit(0);
        }
    }

    public State getState() {
        return state;
    }

    public String getBaseDir() {
        return baseDir;
    }

    private void requestSaveState() {
        int ret = JOptionPane.showConfirmDialog(frame, "Save changes?", "Save", JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.OK_OPTION) {
            saveState();
        }
    }

    private void saveState() {
        try {
            state.setFrameConfig(new FrameConfig(frame.getBounds()));
            state.setTableViewConfiguration(table.getViewConfiguration());

            File file = new File(baseDir + File.separator + "state.xml");
            log.info("Saving to: " + file);
            file.getParentFile().mkdirs();
            OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
            BeanUtilities.writeObjectToStream(state, stream);
        } catch (Throwable e) {
            log.error(e);
        }
    }

    private static State getState(String baseDir) {
        State state = null;

        try {
            File file = new File(baseDir + File.separator + "state.xml");
            if (file.exists() && file.canRead()) {
                state = loadState(file);
            }
        } catch (Throwable e) {
            log.error(e);
        }

        if (state == null) {
            state = new State();
        }

        return state;
    }

    private static State loadState(File file) throws FileNotFoundException {
        InputStream stream = new BufferedInputStream(new FileInputStream(file));
        State state = (State) BeanUtilities.readObjectFromStream(stream);
        return state;
    }

    private class SaveAction extends AbstractAction {
        public SaveAction() {
            super("Save", ImageIconCache.getImageIcon(SAVE_ICON));
            putValue(Action.SHORT_DESCRIPTION, "Save");
            putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        }

        public void actionPerformed(ActionEvent e) {
            requestSaveState();
        }
    }

    private class ExitAction extends AbstractAction {
        public ExitAction() {
            super("Exit", ImageIconCache.getImageIcon(BLANK_ICON));
            putValue(Action.SHORT_DESCRIPTION, "Exit");
            putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
        }

        public void actionPerformed(ActionEvent e) {
            exit();
        }
    }

    private class ExitWindowAdapter extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            exit();
        }
    }

    private static abstract class TableSelectionAction extends AbstractAction {
        private ApplicationTable table;

        public abstract void actionPerformed(Application application, Window owner);

        public TableSelectionAction(String name, String icon, ApplicationTable table) {
            super(name, ImageIconCache.getImageIcon(icon));
            putValue(Action.SHORT_DESCRIPTION, getValue(Action.NAME));
            this.table = table;
            TableListener tableListener = new TableListener(this, table);
            table.getSelectionModel().addListSelectionListener(tableListener);
            table.getModel().addTableModelListener(tableListener);
            setEnabled(isEnabled(table));
        }

        private boolean isEnabled(ApplicationTable table) {
            try {
                return table.getSelectedBeans().length == 1 && isEnabled((Application) table.getSelectedBeans()[0]);
            } catch (Exception e) {
                log.error(e);
                return true;
            }
        }

        protected boolean isEnabled(Application application) {
            return true;
        }

        public void actionPerformed(ActionEvent e) {
            Window owner = UIUtilities.getWindowParent((Component) e.getSource());
            try {
                Object[] objects = table.getSelectedBeans();
                actionPerformed((Application) objects[0], owner);
            } catch (Exception e1) {
                log.error(e1);
            }
        }

        private static class TableListener implements ListSelectionListener, TableModelListener {
            private TableSelectionAction action;
            private final ApplicationTable table;

            public TableListener(TableSelectionAction action, ApplicationTable table) {
                this.action = action;
                this.table = table;
            }

            public void tableChanged(TableModelEvent e) {
                // after actionPerformed, it is likely a TableModelEvent occurs.
                // If a row is removed TableListener can get called before the table's selection model can clear the selection.
                // Invoking later allows the selection model to handle the event before TableListener
                Runnable runnable = new Runnable() {
                    public void run() {
                        action.setEnabled(action.isEnabled(table));
                    }
                };
                EventQueue.invokeLater(runnable);
            }

            public void valueChanged(ListSelectionEvent e) {
                action.setEnabled(action.isEnabled(table));
            }
        }
    }

    private static class GenerateApplicationAction extends TableSelectionAction {
        private String baseDir;

        public GenerateApplicationAction(ApplicationTable table, String baseDir) {
            super("Generate", GENERATE_ICON, table);
            this.baseDir = baseDir;
        }

        public void actionPerformed(final Application application, Window owner) {
            String title = "Generating " + application.getId();
            log.info(title);
            UIUtilities.runTaskWithProgressDialog(owner, "", new Task(title) {
                public void run() throws Throwable {
                    application.generate(baseDir);
                }
            });
        }
    }

    private static class RunApplicationAction extends TableSelectionAction {
        private String baseDir;
        private State state;
        private String urlClassLoaderHost;
        private int urlClassLoaderPort;

        public RunApplicationAction(ApplicationTable table, State state, String baseDir, String urlClassLoaderHost, int urlClassLoaderPort) {
            super("Run", RUN_ICON, table);
            this.baseDir = baseDir;
            this.state = state;
            this.urlClassLoaderHost = urlClassLoaderHost;
            this.urlClassLoaderPort = urlClassLoaderPort;
        }

        protected boolean isEnabled(Application application) {
            return application.isGenerated(baseDir);
        }

        public void actionPerformed(Application application, Window owner) {
            try {
                Process process = application.run(baseDir, urlClassLoaderHost, urlClassLoaderPort);
                Rectangle bounds = state.getProcessFrameConfig() != null ? state.getProcessFrameConfig().getFrameBounds() : null;
                ProcessFrame processFrame = new ProcessFrame(process, application.getId(), owner, bounds) {
                    public void dispose() {
                        state.setProcessFrameConfig(new FrameConfig(getBounds()));
                        super.dispose();
                    }
                };
                processFrame.setIconImage(Builder.FRAME_ICON.getImage());
                processFrame.show();
            } catch (IOException e1) {
                log.error(e1);
                String message = "Could not run " + application.getId();
                JOptionPane.showMessageDialog(owner, message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private static class CopyApplicationAction extends TableSelectionAction {
        private ApplicationCollection applications;

        public CopyApplicationAction(ApplicationTable table, ApplicationCollection applications) {
            super("Copy", COPY_ICON, table);
            this.applications = applications;
        }

        public void actionPerformed(Application application, Window owner) {
            Application copy = application.copy();

            final ApplicationForm form = new ApplicationForm(copy, true);
            String title = "Copy Application";
            JFrame frame = new JFrame(title) {
                public void dispose() {
                    form.dispose();
                    super.dispose();
                }
            };
            frame.setIconImage(Builder.FRAME_ICON.getImage());
            Action runAction = new CreateApplicationAction(frame, copy, applications);
            form.displayForm(frame, owner, runAction);
        }

    }

    private static class EditApplicationAction extends TableSelectionAction {
        private ApplicationCollection applications;

        public EditApplicationAction(ApplicationTable table, ApplicationCollection applications) {
            super("Edit", EDIT_ICON, table);
            this.applications = applications;
        }

        public void actionPerformed(Application application, Window owner) {
            Application amendedApplication = (Application) BeanUtilities.verySlowDeepCopy(application);

            final ApplicationForm form = new ApplicationForm(application, false);
            String title = "Edit Application";
            JFrame frame = new JFrame(title) {
                public void dispose() {
                    form.dispose();
                    super.dispose();
                }
            };
            frame.setIconImage(Builder.FRAME_ICON.getImage());
            Action runAction = new AmendApplicationAction(frame, application, amendedApplication, applications);
            form.displayForm(frame, owner, runAction);
        }
    }

    private static class RemoveApplicationAction extends TableSelectionAction {
        private ApplicationTable table;
        private ApplicationCollection applications;
        private String baseDir;

        public RemoveApplicationAction(ApplicationTable table, ApplicationCollection applications, String baseDir) {
            super("Remove", REMOVE_ICON, table);
            this.table = table;
            this.applications = applications;
            this.baseDir = baseDir;
        }

        public void actionPerformed(Application application, Window owner) {
            int ret = JOptionPane.showConfirmDialog(owner, "Remove " + application.getId() + "?", "Remove", JOptionPane.OK_CANCEL_OPTION);
            if (ret == JOptionPane.OK_OPTION) {
                applications.remove(application, baseDir);
            }
        }
    }

    private static class SocketClassURLServer {
        private static int clientIndex;
        private int port;

        public SocketClassURLServer() throws IOException {
            ServerSocket serverSocket = null;


            for (int port = 1025; port < 65535; port++) {
                log.info("Binding to port " + port);
                try {
                    serverSocket = new ServerSocket(port);
                    break;
                } catch (IOException e) {
                    log.error("Could not bind to server socket port: " + port);
                }
            }

            if (serverSocket == null) {
                throw new IOException("Could not bind to any server socket port between 1025 and 65535");
            }
            this.port = serverSocket.getLocalPort();

            Runnable runnable = new ServerHandler(serverSocket);
            Thread thread = new Thread(runnable, "SocketClassServer");
            thread.setDaemon(true);
            thread.start();
        }

        public int getPort() {
            return port;
        }

        private static class ServerHandler implements Runnable {
            private final ServerSocket serverSocket;

            public ServerHandler(ServerSocket serverSocket) {
                this.serverSocket = serverSocket;
            }

            public void run() {
                try {
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        clientIndex++;
                        String name = "ClientHandler" + clientIndex;
                        Thread clientThread = new Thread(new ClientHandler(clientSocket, name), name);
                        clientThread.setDaemon(true);
                        clientThread.start();
                    }
                } catch (IOException e) {
                    log.error("SocketClassServer failed", e);
                }

            }
        }

        private static class ClientHandler implements Runnable {
            private Reader in;
            private Writer out;
            private Socket socket;
            private String name;

            public ClientHandler(Socket socket, String name) throws IOException {
                this.socket = socket;
                this.name = name;
            }

            public void run() {
                log.info(name + " starting");

                try {
                    in = new InputStreamReader(socket.getInputStream());
                    out = new OutputStreamWriter(socket.getOutputStream());

                    while (true) {
                        String commandString = readLine(in);
                        if (commandString == null) continue;
                        int commandIndex = commandString.indexOf(':');
                        String command = commandString.substring(0, commandIndex);
                        String parameter = commandString.substring(commandIndex + 1, commandString.length());
                        String returnValue = null;

                        if (command.equals("findClass")) {
                            File pathForClass = ClassUtilities.getPathForClass(parameter);
                            if (pathForClass != null) {
                                URL url = pathForClass.toURL();
                                if (url != null) returnValue = url.toExternalForm();
                            }
                        } else if (command.equals("findLibrary")) {
                            ClassLoader classLoader = ClassUtilities.getApplicationClassLoader();
                            while (classLoader != null && classLoader != ClassLoader.getSystemClassLoader() && returnValue == null) {
                                try {
                                    Method method = classLoader.getClass().getMethod("findLibrary", String.class);
                                    method.setAccessible(true);
                                    returnValue = (String) method.invoke(classLoader, parameter);
                                    if (returnValue != null) {
                                        File file = new File(returnValue);
                                        returnValue = file.toURL().toExternalForm();
                                    }
                                } catch (Throwable e) {
                                }
                                classLoader = classLoader.getParent();
                            }

                            if (returnValue == null) {
                                String mappedLibraryFile = System.mapLibraryName(parameter);
                                String javaLibraryPathString = System.getProperty("java.library.path");
                                String[] javaLibraryPaths = javaLibraryPathString.split(System.getProperty("path.separator"));
                                for (int i = 0; i < javaLibraryPaths.length && returnValue == null; i++) {
                                    String javaLibraryPath = javaLibraryPaths[i];
                                    File dir = new File(javaLibraryPath);
                                    File[] filesInDir = dir.listFiles();
                                    for (int j = 0; j < filesInDir.length && returnValue == null; j++) {
                                        File fileInDir = filesInDir[j];
                                        if (fileInDir.getName().equals(mappedLibraryFile)) {
                                            returnValue = fileInDir.toURL().toExternalForm();
                                        }
                                    }
                                }
                            }
                            log.info("Mapped library: " + parameter + " to: " + returnValue);

                        } else if (command.equals("findResource")) {
                            URL url = IOUtilities.getResource(parameter);
                            if (url != null) returnValue = url.toExternalForm();
                        } else if (command.equals("findResources")) {
                            Enumeration enumeration = ClassUtilities.getApplicationClassLoader().getResources(parameter);
                            returnValue = Utilities.asString(enumeration, ",");
                        }

                        if (returnValue == null) returnValue = "";
                        out.write(returnValue + "\n");
                        out.flush();
                    }
                } catch (IOException e) {
                } catch (Throwable e) {
                    log.error(e);
                } finally {
                    log.info(name + " terminating");
                    SocketUtil.closeSocket(socket);
                }
            }
        }

        private static String readLine(Reader in) throws IOException {
            StringBuffer line = new StringBuffer();
            char c = (char) in.read();
            while (c != '\n' && c != -1) {
                line.append(c);
                c = (char) in.read();
            }
            String urlExternalForm = line.toString();
            return urlExternalForm;
        }
    }
}
