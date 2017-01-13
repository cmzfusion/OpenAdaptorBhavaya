package org.bhavaya.util;

import org.bhavaya.coms.SocketUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Manages the external sound player.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class ExternalPlayerManager {

    private static final Log log = Log.getCategory(ExternalPlayerManager.class);

    private static String soundPlayerJarLocation;

    private Process externalPlayerProcess;
    private SoundPlayerServer soundPlayerServer;
    private final Object startupLock = new Object();

    private int playerIdCount = 0; // used to generate unique ID for external sound player
    private int playIdCounter = 0; // used to generate unique ID for playing sound
    private int preloadIdCounter = 0; // used to generate unique ID for preload commands
    private int releaseIdCounter = 0; // used to generate unique ID for release command

    private Map responseListeners = Collections.synchronizedMap(new HashMap());

    // this is only used when restarting player (manually or after crash)
    private final HashMap preloadedSounds = new HashMap();
    private String jvmPath;

    /**
     * Returns path to java executable of the currently running JVM.
     */
    public static String getDefaultJVMPath() {
        // first check whether we are running as a JWS application.
        String jwsJVMPath = System.getProperty("jnlpx.jvm");
        if (jwsJVMPath != null) return jwsJVMPath;

        String javaHomePath = System.getProperty("java.home");
        if (javaHomePath != null) {
            String fileSeparator = System.getProperty("file.separator");
            return javaHomePath + fileSeparator + "bin" + fileSeparator + "java.exe";
        }

        return null;
    }

    private static boolean isRunningAsJavaWebStart() {
        return System.getProperty("javawebstart.version") != null;
    }

    public void start() throws IOException {
        start(getDefaultJVMPath());
    }

    public void start(String pathToJVM) throws IOException {
        if (checkPlayerIsRunning()) {
            log.warn("Attempt to start second instance of sound player. Player is already running.");
            return;
        }

        responseListeners.clear();

        // start the socket server
        int portNumber = startupSocketServer();

        if (pathToJVM == null) {
            pathToJVM = getDefaultJVMPath();
        }
        jvmPath = pathToJVM;
        String commandToStartPlayer;
        if (isRunningAsJavaWebStart()) {
            // get the copy of JAR file
            String pathToSoundPlayerJar = getSoundPlayerJarLocation();
            commandToStartPlayer = pathToJVM + " -classpath \"" + pathToSoundPlayerJar + "\"";
        } else {
            String classpath = System.getProperty("java.class.path");
            commandToStartPlayer = pathToJVM + " -classpath \"" + classpath + "\"";
        }
        commandToStartPlayer += " -Dlog4j.configuration=resources/logSoundPlayer.xml "
                + " org.bhavaya.util.SoundPlayer Player" + (playerIdCount++) + " " + portNumber;
        log.info("Running sound player: " + commandToStartPlayer);

        try {
            externalPlayerProcess = Runtime.getRuntime().exec(commandToStartPlayer);

            StartupCheckThread startupCheckThread = new StartupCheckThread();
            startupCheckThread.start();

            // read any output produced by the process
            ProcessOutputReader processErrorReader = new ProcessOutputReader(externalPlayerProcess.getErrorStream());
            processErrorReader.start();
            ProcessOutputReader processOutputReader = new ProcessOutputReader(externalPlayerProcess.getInputStream());
            processOutputReader.start();
        } catch (IOException ex) {
            stop();
            throw ex;
        }
    }

    /**
     * Extracts the sound player JAR and puts it into the local directory.
     */
    private static synchronized String getSoundPlayerJarLocation() throws IOException {
        if (soundPlayerJarLocation == null) {
            InputStream is = ExternalPlayerManager.class.getResourceAsStream("/resources/soundPlayer.jar");
            if (is == null) {
                throw new IOException("Couldn't find the soundPlayer.jar library.");
            }
            String libraryLocation = System.getProperty("user.home") + "/.bhavaya/soundPlayer.jar";
            FileOutputStream fos = new FileOutputStream(new File(libraryLocation));
            byte[] buf = new byte[10000];
            int c;
            while ((c = is.read(buf)) != -1) {
                fos.write(buf, 0, c);
            }
            is.close();
            fos.close();
            soundPlayerJarLocation = libraryLocation;
        }
        return soundPlayerJarLocation;
    }

    private static final String SOUND_PLAYER_PROPERTY_GROUP = "soundPlayer";

    private int startupSocketServer() throws IOException {
        PropertyGroup soundPlayerPropertyGroup = ApplicationProperties.getApplicationProperties().getGroup(SOUND_PLAYER_PROPERTY_GROUP);
        int startPort = soundPlayerPropertyGroup.getNumericProperty("communicationPortMin").intValue();
        int endPort = soundPlayerPropertyGroup.getNumericProperty("communicationPortMax").intValue();
        for (int port = startPort; port <= endPort; ++port) {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                synchronized(startupLock) {
                    soundPlayerServer = new SoundPlayerServer(serverSocket);
                    soundPlayerServer.setDaemon(true);
                    soundPlayerServer.setPriority(Thread.NORM_PRIORITY);
                    soundPlayerServer.start();
                }
                log.info("Server started at port: " + port);
                return port;
            } catch (IOException e) {
                // ignore
            }
        }
        throw new IOException("Couldn't open socket connection in port range " + startPort + "-" + endPort);
    }

    public void stop() {
        log.info("Stopping the sound player.");
        responseListeners.clear();
        preloadedSounds.clear();
        synchronized(startupLock) {
            if (soundPlayerServer != null) {
                soundPlayerServer.stopProcessing();
                soundPlayerServer = null;
            }
            if (externalPlayerProcess != null) {
                externalPlayerProcess.destroy();
                externalPlayerProcess = null;
            }
        }
    }

    public void restart() {
        log.info("Restarting sound player.");
        Object[] fileNames = preloadedSounds.keySet().toArray();
        stop();
        try {
            start(jvmPath);
            for (int i = 0; i < fileNames.length; i++) {
                preload((String) fileNames[i]);
            }
        } catch (IOException ex) {
            log.error("Couldn't restart the sound player.", ex);
        }

    }

    public void play(String fileName) {
        play(fileName, null);
    }

    public void play(String fileName, final SoundPlayListener listener) {
        checkPlayerIsRunningAndThrowException();
        String id = "play" + (playIdCounter++);
        if (listener != null) {
            listener.beforePlayingSound();
            responseListeners.put(id, new ResponseListener() {
                public void receivedResponse(Response response) {
                    if (response.level == Response.LEVEL_OK) {
                        listener.afterPlayingSound();
                    }
                }
            });
        }
        soundPlayerServer.sendCommand("play " + id + " " + fileName);
    }

    public void preload(String fileName) {
        checkPlayerIsRunningAndThrowException();
        soundPlayerServer.sendCommand("preload preload" + (preloadIdCounter++) + " " + fileName);

        synchronized (preloadedSounds) {
            MutableInteger counter = (MutableInteger) preloadedSounds.get(fileName);
            if (counter == null) {
                counter = new MutableInteger(1);
                preloadedSounds.put(fileName, counter);
            } else {
                counter.value++;
            }
        }
    }

    public void release(String fileName) {
        checkPlayerIsRunningAndThrowException();
        soundPlayerServer.sendCommand("release release" + (releaseIdCounter++) + " " + fileName);

        synchronized (preloadedSounds) {
            MutableInteger counter = (MutableInteger) preloadedSounds.get(fileName);
            if (counter != null) {
                counter.value--;
                if (counter.value == 0) {
                    preloadedSounds.remove(fileName);
                }
            }
        }
    }

    public boolean isRunning() {
        return checkPlayerIsRunning();
    }

    private boolean checkPlayerIsRunning() {
        if (soundPlayerServer == null || !soundPlayerServer.isAlive()) {
            return false;
        }
        return soundPlayerServer.checkPlayerIsConnected(); // block until player is connected
    }

    private void checkPlayerIsRunningAndThrowException() {
        if (!checkPlayerIsRunning()) {
            throw new RuntimeException("External player not started.");
        }
    }

    /**
     * Format of a command is:
     * <id_used_to_identify_response> <level> [<message>]
     *
     * Note: Only the message may contain spaces.
     */
    private class Response {
        public static final int LEVEL_OK = 0;
        public static final int LEVEL_WARN = 1;
        public static final int LEVEL_ERROR = 2;

        String response;
        String id;
        int level;
        String message;

        public Response(String response) {
            this.response = response;
            int index = response.indexOf(' ');
            if (index == -1) {
                id = response;
                return;
            } else {
                id = response.substring(0, index);
            }
            response = response.substring(index).trim();
            index = response.indexOf(' ');
            if (index == -1) {
                level = getLevelForString(response);
                return;
            } else {
                level = getLevelForString(response.substring(0, index));
            }
            message = response.substring(index).trim();
        }

        private int getLevelForString(String levelStr) {
            if ("OK".equals(levelStr)) {
                return LEVEL_OK;
            } else if ("WARN".equals(levelStr)) {
                return LEVEL_WARN;
            } else {
                return LEVEL_ERROR;
            }
        }

        public String toString() {
            return "Response{" +
                    "id='" + id + "'" +
                    ", level='" + level + "'" +
                    ", message='" + message + "'" +
                    "}";
        }
    }

    private interface ResponseListener {
        public void receivedResponse(Response response);
    }

    private class SoundPlayerServer extends Thread {
        private ServerSocket serverSocket;
        private Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private boolean alive = true;
        private boolean playerConnected = false;

        public SoundPlayerServer(ServerSocket serverSocket) {
            super("SoundPlayerServer");
            this.serverSocket = serverSocket;
        }

        public void run() {
            try {
                log.info("Waiting for client connection.");
                socket = serverSocket.accept();
                log.info("Client connection accepted.");
                synchronized (startupLock) {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    playerConnected = true;
                    startupLock.notifyAll();
                }

                while (alive) {
                    String responseString = in.readLine();
                    if (!alive) return;
                    if (responseString != null) {
                        Response response = new Response(responseString);
                        switch (response.level) {
                            case Response.LEVEL_OK:
                                log.info("Received response for id: " + response.id + (response.message == null ? "" : " - " + response.message));
                                break;
                            case Response.LEVEL_WARN:
                                log.warn("Received response for id: " + response.id + " - " + response.message);
                                break;
                            case Response.LEVEL_ERROR:
                                log.error("Received response for id: " + response.id + " - " + response.message);
                                break;
                        }

                        ResponseListener responseListener = (ResponseListener) responseListeners.get(response.id);
                        if (responseListener != null) {
                            responseListener.receivedResponse(response);
                            responseListeners.remove(response.id);
                        }
                    }
                }
            } catch (IOException e) {
                if (alive) {
                    log.error(e);
                    restart();
                }
            }
        }

        /**
         * Checks whether the player is connected, blocks until it actually gets connected.
         */
        public boolean checkPlayerIsConnected() {
            synchronized (startupLock) {
                if (alive && !playerConnected) {
                    try {
                        startupLock.wait();
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
                return alive && playerConnected;
            }
        }

        /**
         * Stops the sound player server.
         */
        public void stopProcessing() {
            alive = false;
            SocketUtil.closeSocket(socket);
            if (serverSocket != null && ! serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
            socket = null;
            serverSocket = null;
        }

        public synchronized void sendCommand(String cmd) {
            try {
                out.write(cmd);
                out.write('\n');
                out.flush();
            } catch (IOException e) {
                log.error(e);
                restart();
                if (checkPlayerIsRunning()) {
                    // send command to the newly created sound player
                    log.info("Resending command to player: " + cmd);
                    soundPlayerServer.sendCommand(cmd);
                }
            }
        }
    }

    /**
     * Used to read any output produced by the process created with {@link Runtime#exec(String)} method.
     * This is to avoid potential process deadlock. Read more here {@link Process}
     */
    private class ProcessOutputReader extends Thread {
        BufferedReader reader;

        public ProcessOutputReader(InputStream stream) {
            super("ProcessOutputReader");
            setDaemon(true);
            setPriority(Thread.NORM_PRIORITY);
            this.reader = new BufferedReader(new InputStreamReader(stream));
        }

        public void run() {
            try {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        return; // stream doesn't seem to close when the app is closed, but it rather returns null (not sure this is the case on other platforms though)
                    }
                    log.info("Player says: " + line);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private class StartupCheckThread extends Thread {
        public StartupCheckThread() {
            super("SoundPlayerStartupCheck");
            setPriority(Thread.MIN_PRIORITY);
        }

        public void run() {
            // this thread just wakes up the StartupCheckThread as soon as the process ends
            Thread t = new Thread("WakeUpThread") {
                public void run() {
                    try {
                        externalPlayerProcess.waitFor();
                        StartupCheckThread.this.interrupt();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            };
            t.start();

            synchronized (startupLock) {
                try {
                    startupLock.wait(10000);
                } catch (InterruptedException e) {
                    // ignore
                }
                try {
                    int exitValue = externalPlayerProcess.exitValue();
                    log.error(new Exception("External player stopped with exit value: " + exitValue));
                    soundPlayerServer.stopProcessing();
                    soundPlayerServer = null;
                    externalPlayerProcess = null;
                    startupLock.notifyAll();
                } catch (IllegalThreadStateException ex) {
                    // ignore - this means the process is running
                }
            }
        }
    }
}
