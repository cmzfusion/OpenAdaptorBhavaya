package org.bhavaya.util;

import org.bhavaya.coms.SocketUtil;
import org.bhavaya.ui.SoundHandler;

import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class of a stand-alone sound player application. This is an attempt to work arround sound playing
 * problems in Java. We can easily kill old and start new process in attempt to recover sound playing
 * functionality. Unless the whole OS is affected with the sound play malfunction, this should solve the
 * problem where instance of JVM cannot play sound anymore.
 * <p>
 * Player is managed remotely through the socket connetion. It automatically shuts down when the connection
 * is lost.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class SoundPlayer extends Thread {

    private static final Log log = Log.getCategory(SoundPlayer.class);

    public static final String CANNOT_PLAY_FILE_MSG = "CANNOT_PLAY_FILE";
    public static final String PLAYED_DEFAULT_SOUND_MSG = "PLAYED_DEFAULT_SOUND";
    public static final String REQUEST_DISCARDED_MSG = "REQUEST_DISCARDED";
    public static final String ERROR_LOADING_FILE_MSG = "ERROR_LOADING_FILE";
    public static final String UNKNOWN_COMMAD_MSG = "UNKNOWN_COMMAD";

    public static final long DEFAULT_CONNECT_TIMEOUT = 30000L;
    private static final Object UNKNOWN_SOUND_FILE = new Object() {
        public String toString() {
            return "Unknown sound file";
        }
    };

    private int serverPort;
    private TaskQueue preloadQueue;
    private Map soundSampleMap = Collections.synchronizedMap(new HashMap());
    private SoundSample defaultSoundSample;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public SoundPlayer(String id, int serverPort) {
        super(id);
        this.serverPort = serverPort;
        preloadQueue = new TaskQueue(id + "-PQ");
        defaultSoundSample = loadSoundFile(SoundHandler.DEFAULT_SOUND_CLIP_FILENAME);
    }

    public synchronized void start() {
        preloadQueue.start();
        super.start();
    }

    public void run() {
        try {
            socket = SocketUtil.newSocket("localhost", serverPort, DEFAULT_CONNECT_TIMEOUT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            while (true) {
                String cmdLine = in.readLine();
                if (cmdLine != null) {
                    cmdLine = cmdLine.trim();
                    process(cmdLine);
                }
            }
        } catch (Exception ex) {
            log.error(ex);
            shutdown(1);
        }
    }

    private void process(String cmdLine) {
        log.info("Processing: " + cmdLine);
        Command cmd = getCommand(cmdLine);
        cmd.execute();
    }

    public void shutdown(int status) {
        log.info("Stopping the player");
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                // ignore
            }
        }
        SocketUtil.closeSocket(socket);
        System.exit(status);
    }

    private Command getCommand(String cmdLine) {
        if (cmdLine.startsWith("play")) {
            return new PlaySoundCommand(cmdLine);
        } else if (cmdLine.startsWith("preload")) {
            return new PreloadSoundFileCommand(cmdLine);
        } else if (cmdLine.startsWith("release")) {
            return new ReleaseSoundFileCommand(cmdLine);
        } else if (cmdLine.startsWith("stop")) {
            return new ShutdownCommand(cmdLine);
        } else {
            return new UnknownCommand(cmdLine);
        }
    }

    private SoundSample loadSoundFile(String soundFileName) {
        try {
            log.info("Loading file " + soundFileName);
            SoundSample soundSample = new SoundSample(soundFileName);
            soundSampleMap.put(soundFileName, soundSample);
            return soundSample;
        } catch (Exception ex) {
            log.error("Error loading sound file: " + soundFileName);
            soundSampleMap.put(soundFileName, UNKNOWN_SOUND_FILE);
            return null;
        }
    }

    /**
     * Format of a command is:
     * <command> [<id_used_to_identify_response>] [<sound_file>]
     *
     * Note: Only the sound_file may contain spaces.
     */
    private abstract class Command {
        String cmdLine;
        String command;
        String id;
        String soundFileName;

        public Command(String cmdLine) {
            this.cmdLine = cmdLine;
            parseCommand();
        }

        protected void parseCommand() {
            int index = cmdLine.indexOf(' ');
            if (index == -1) {
                command = cmdLine;
                return;
            } else {
                command = cmdLine.substring(0, index);
            }
            cmdLine = cmdLine.substring(index).trim();
            index = cmdLine.indexOf(' ');
            if (index == -1) {
                id = cmdLine;
                return;
            } else {
                id = cmdLine.substring(0, index);
            }
            soundFileName = cmdLine.substring(index).trim();
        }

        public abstract void execute();

        protected void respondWithSuccess() {
            respondWithSuccess("");
        }

        protected void respondWithSuccess(String message) {
            sendResponse(id + " OK " + message);
        }

        protected void respondWithWarning(String message) {
            sendResponse(id + " WARN " + message);
        }

        protected void respondWithError(String message) {
            sendResponse(id + " ERROR " + message);
        }

        private void sendResponse(String response) {
            try {
                synchronized(out) {
                    out.write(response);
                    out.write('\n');
                    out.flush();
                }
            } catch (Exception ex) {
                log.error(ex);
                shutdown(1); // shutdown player on any communication error
            }
        }

        public String toString() {
            return "Command{" +
                    "command='" + command + "'" +
                    ", id='" + id + "'" +
                    ", soundFileName='" + soundFileName + "'" +
                    "}";
        }
    }

    /**
     * Plays the sound. Preloads file when not already in memory.
     */
    private class PlaySoundCommand extends Command implements SoundPlayListener {
        private boolean playedDefaultSound = false;

        public PlaySoundCommand(String cmdLine) {
            super(cmdLine);
        }

        public void execute() {
            Object soundSample = soundSampleMap.get(soundFileName);
            if (soundSample == null) {
                log.warn("Attempting to play not preloaded file: " + soundFileName);
                soundSample = loadSoundFile(soundFileName);
            }
            if (soundSample == null || soundSample == UNKNOWN_SOUND_FILE) {
                if (defaultSoundSample != null && defaultSoundSample != UNKNOWN_SOUND_FILE) {
                    log.warn("Playing default sound sample instead of " + soundFileName);
                    playedDefaultSound = true;
                    defaultSoundSample.play(this);
                } else {
                    respondWithError(CANNOT_PLAY_FILE_MSG + " " + soundFileName);
                            // Couldn't play sound file: " + soundFileName + " nor the default sound file is available."
                }
            } else {
                ((SoundSample)soundSample).play(this);
            }
        }

        public void beforePlayingSound() {
            log.info("About to play sound for " + this.toString());
        }

        public void afterPlayingSound() {
            log.info("Succesfuly played sound for " + this.toString());
            if (playedDefaultSound) {
                // "Played default sound instead of requested sound sample."
                respondWithWarning(PLAYED_DEFAULT_SOUND_MSG);
            } else {
                respondWithSuccess();
            }
        }

        public void discarded() {
            log.info("Request discarded for " + this.toString());
            respondWithWarning(REQUEST_DISCARDED_MSG); // Request discarded
        }
    }

    /**
     * Preloads sound sample into the memory.
     */
    private class PreloadSoundFileCommand extends Command {

        public PreloadSoundFileCommand(String cmdLine) {
            super(cmdLine);
        }

        public void execute() {
            try {
                /**
                 * Don't need to lock when preloading file. If we preload it twice, one of the instances
                 * get garbage collected and the other will be used, so it causes no harm.
                 */
                Object soundSample = soundSampleMap.get(soundFileName);
                if (soundSample != null && soundSample != UNKNOWN_SOUND_FILE) {
                    log.info("Sound sample already preloaded for " + soundFileName);
                    respondWithSuccess();
                } else {
                    preloadQueue.addTask(new Task(cmdLine) {
                        public void run() {
                            SoundSample soundSample = loadSoundFile(soundFileName);
                            if (soundSample == null || soundSample == UNKNOWN_SOUND_FILE) {
                                respondWithError(ERROR_LOADING_FILE_MSG + " " + soundFileName);
                            } else {
                                respondWithSuccess();
                            }
                        }
                    });
                }
            } catch (Exception ex) {
                log.error(ex);
                respondWithError(ERROR_LOADING_FILE_MSG + " " + soundFileName);
            }
        }
    }

    /**
     * Releases sound sample preloaded in memory.
     */
    private class ReleaseSoundFileCommand extends Command {

        public ReleaseSoundFileCommand(String cmdLine) {
            super(cmdLine);
        }

        public void execute() {
            log.info("Releasing file " + soundFileName);
            soundSampleMap.remove(soundFileName);
            respondWithSuccess();
        }
    }

    private class ShutdownCommand extends Command {

        public ShutdownCommand(String cmdLine) {
            super(cmdLine);
        }

        public void execute() {
            shutdown(0);
        }
    }

    private class UnknownCommand extends Command {

        public UnknownCommand(String cmdLine) {
            super(cmdLine);
        }

        public void execute() {
            respondWithError(UNKNOWN_COMMAD_MSG + " " + command);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            log.error("Invalid number of parameters");
            System.exit(1);
        }
        String id = args[0];
        int serverPortNumber = Integer.parseInt(args[1]);
        new SoundPlayer(id, serverPortNumber).start();
    }
}
