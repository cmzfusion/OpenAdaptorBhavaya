package org.bhavaya.util;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.util.Collection;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 28-May-2008
 * Time: 16:32:34
 *
 * This utility can be used to capture images of a set of dialogs or windows to disk,
 * either on a one off basis, or periodically
 */
public class WindowCapture {

    private static final Log log = Log.getCategory(WindowCapture.class);
    private Collection<Window> windows;
    private WindowCaptureFileNameProvider windowCaptureFileNameProvider;
    private String windowCollectionName;
    private boolean deleteFilesOnExit;
    private Timer weakReferenceTimer;

    //do not convert to local variable.
    //Must hold a reference otherwise it will be collected due to the weak listener
    private ActionListener timerListener;

    private Robot robot;
    private TaskQueue queue;  //queue to write files off the swing event thread

    public WindowCapture(Collection<Window> windows, WindowCaptureFileNameProvider windowCaptureFileNameProvider, String windowCollectionName, boolean deleteFilesOnExit) {
        this.windows = windows;
        this.windowCaptureFileNameProvider = windowCaptureFileNameProvider;
        this.windowCollectionName = windowCollectionName;
        this.deleteFilesOnExit = deleteFilesOnExit;

        createFileWritingQueue();
        createTimer();
        createRobot();
    }

    private void createFileWritingQueue() {
        queue = new TaskQueue(windowCollectionName, false);
        queue.start();
    }

    private void createRobot() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            log.warn("WindowCapture for " + windowCollectionName + " cannot create Robot for screen capture. Perhaps this is running in a headless mode, or the security manager prevents it?", e);
        }
    }

    public boolean isWindowCapturePossible() {
        return robot != null;
    }

    private void createTimer() {
        timerListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                captureNow();
            }
        };

        this.weakReferenceTimer = WeakReferenceTimer.createTimer(60000, timerListener);
        this.weakReferenceTimer.setInitialDelay(0);
    }

    public void startCapture(int captureIntervalMillis) {
        weakReferenceTimer.setDelay(captureIntervalMillis);
        if ( ! weakReferenceTimer.isRunning() ) {
            weakReferenceTimer.start();
        }
    }

    public void stopCapture() {
        if ( weakReferenceTimer.isRunning()) {
            weakReferenceTimer.stop();
        }
    }

    public void captureNow() {
        if ( isWindowCapturePossible()) {

            Window[] windowArray;
            synchronized(windows) { //try to protect against concurrent modifications
                windowArray = windows.toArray(new Window[windows.size()]);
            }

            for ( Window w : windowArray ) {
                if ( w.isShowing() && windowCaptureFileNameProvider.isCaptureRequired(w) ) {
                    captureWindow(w);
                }
            }
        }
    }

    private void captureWindow(Window window) {
        long startTime = System.currentTimeMillis();
        File newImageFile = null;
        try {
            File parentDirectory = windowCaptureFileNameProvider.getParentDirectory(window);
            String name = windowCaptureFileNameProvider.getFileName(window);
            if ( ! name.endsWith(".png")) {
                name = name + ".png";
            }

            newImageFile = new File(parentDirectory, name);
            BufferedImage image = robot.createScreenCapture(window.getBounds());
            writeImageFile(image, newImageFile);
            log.debug("RFQ image capture took " + (System.currentTimeMillis() - startTime) + " milliseconds for " + name);
        } catch (Exception e) {
            log.warn("Failed to create image file " + newImageFile + " for window " + window, e);
        }
    }

    private void writeImageFile(BufferedImage windowImage, File newImageFile) throws IOException {
        queue.addTask(new ImageFileWriteTask(windowImage, newImageFile, deleteFilesOnExit));
    }

    /**
     * Client classes should implement this interface and pass it to WindowCapture
     * to provide a directory and filename for each window image
     */
    public static interface WindowCaptureFileNameProvider {

        boolean isCaptureRequired(Window w);

        String getFileName(Window w);

        File getParentDirectory(Window w);
    }


    /**
     * A task to write image files to disk, off the swing event thread
     */
    public static class ImageFileWriteTask extends Task {
        private BufferedImage windowImage;
        private File newImageFile;
        private boolean deleteFilesOnExit;
        private static boolean directoryWarningShown;

        public ImageFileWriteTask(BufferedImage windowImage, File newImageFile, boolean deleteFilesOnExit) {
            super("WindowCapture task to write image file " + newImageFile);
            this.windowImage = windowImage;
            this.newImageFile = newImageFile;
            this.deleteFilesOnExit = deleteFilesOnExit;
        }

        public void run() throws IOException {
            if ( newImageFile.getParentFile().canWrite()) {
                ImageIO.write(windowImage, "png", newImageFile);
                if ( deleteFilesOnExit ) {
                  newImageFile.deleteOnExit();
                }
            } else {
                if ( ! directoryWarningShown ) {
                    log.warn("Cannot write image file to directory " + newImageFile.getParentFile() + " directory is not writeable");
                    directoryWarningShown = true;
                }
            }
        }
    }

}
