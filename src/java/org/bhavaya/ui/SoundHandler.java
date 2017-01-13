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

package org.bhavaya.ui;

import org.bhavaya.collection.WeakHashSet;
import org.bhavaya.util.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

/**
 * Provides you with functionality for configuring a sound.
 *
 * Nick - n.b. This has been modified so that the sound file name can be invalid -
 * If the file name is invalid (the sound file no longer exists) the default sound should be used -
 * But we keep the invalid filename so that it can be shown to the user in red, so that the user can
 * see what it used to point to, and be aware it needs to be reconfigured
 *
 * @author Sabine Haas
 * @version $Revision: 1.12 $
 */
public class SoundHandler {

    public static ImageIcon SPEAKER_ICON = ImageIconCache.getImageIcon("speaker_icon.png");
    public static final String DEFAULT_SOUND_CLIP_FILENAME = "/" + IOUtilities.RESOURCE_DIR + "/" + "ping.wav";
    private static final Log log = Log.getCategory(SoundHandler.class);
    private static final Log userLog = Log.getUserCategory();
    private static final String UNKNOWN = "unknown";

    private static final String CONFIG_NAME = "sound";
    private static final String USE_EXTERNAL_PLAYER_KEY= "useExternalSoundPlayer";
    private static final String EXTERNAL_PLAYER_JVM_KEY= "externalSoundPlayerJVM";

    private static final Object lock = new Object();
    private static boolean useExternalPlayer = false;
    private static String externalPlayerJvm = ExternalPlayerManager.getDefaultJVMPath();
    private static ExternalPlayerManager externalPlayerManager = new ExternalPlayerManager();
    private static Set soundHandlers = new WeakHashSet();

    private boolean soundEnabled;
    private SoundSample soundSample;
    private String soundClipFilename;
    private String description;
    private boolean isSoundFileValid;

    static {
        Configuration configuration = Configuration.getRoot(CONFIG_NAME);
        externalPlayerJvm = (String) configuration.getObject(EXTERNAL_PLAYER_JVM_KEY, ExternalPlayerManager.getDefaultJVMPath(), String.class);

        Matcher jdkMatcher = Pattern.compile("(1\\.\\d)\\.\\d").matcher(externalPlayerJvm);
        if (jdkMatcher.find() && !jdkCompatible(jdkMatcher.group(1))) {
            externalPlayerJvm = ExternalPlayerManager.getDefaultJVMPath();
        }

        boolean useExternalPlayer = ((Boolean) configuration.getObject(USE_EXTERNAL_PLAYER_KEY, Boolean.FALSE, Boolean.class)).booleanValue();
        setUseExternalPlayer(useExternalPlayer);

        Configuration.addSaveTask(new Task("Save sound settings") {
            public void run() {
                Configuration configuration = Configuration.getRoot(CONFIG_NAME);
                configuration.putObject(USE_EXTERNAL_PLAYER_KEY, Boolean.valueOf(SoundHandler.useExternalPlayer));
                configuration.putObject(EXTERNAL_PLAYER_JVM_KEY, externalPlayerJvm);
            }
        });
    }

    private static boolean jdkCompatible(String javaMajorVersionString) {
        String runningVersionString = System.getProperty("java.specification.version", "1.5");

        BigDecimal javaMajorVersion = new BigDecimal(javaMajorVersionString);
        BigDecimal runningVersion = new BigDecimal(runningVersionString);

        return javaMajorVersion.compareTo(runningVersion) >= 0;
    }

    public SoundHandler(boolean soundEnabled, String soundClipFilename, String description) {
        this.soundEnabled = soundEnabled;

        // set file and sound sample - sound sample still set localy - we might play it in some cases when external player crashes for instance ...
        setSoundClipFilename(soundClipFilename);
        this.description = description == null ? UNKNOWN : description;
        synchronized (lock) {
            soundHandlers.add(this);
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public boolean isSoundFileValid() {
        return isSoundFileValid;
    }

    public String getSoundClipFilename() {
        return soundClipFilename;
    }

    public void setSoundClipFilename(String soundClipFilename) {
        setSoundFile(soundClipFilename);
        setSoundSample();
    }

    private void setSoundFile(String soundClipFilename) {
        this.soundClipFilename = soundClipFilename;
        this.isSoundFileValid = soundClipFilename != null && new File(soundClipFilename).exists();
    }

    public void play() {
        play(null);
    }

    public void play(SoundPlayListener listener) {
        if (soundEnabled) {
            synchronized (lock) {
                if (useExternalPlayer) {
                    try {
                        //default to the ping if sound filename is not valid
                        String soundFile = isSoundFileValid ? soundClipFilename : DEFAULT_SOUND_CLIP_FILENAME;
                        externalPlayerManager.play(soundFile, listener);
                        return;
                    } catch (Exception ex) {
                        log.error(ex); // log the exception and continue playing sound localy
                    }
                }
            }
            if (soundSample != null) {
                soundSample.play(listener);
            }
        }
    }

    public Runnable getPlayRunnable() {
        return new PlayAction();
    }

    public DefaultButtonModel getSetSoundEnabledToggleButtonModel() {
        return new SetSoundEnabledToggleButtonModel();
    }

    /**
     * @param l, ActionListener which will be notified when the sound clip is changed
     */
    public AbstractAction getSetSoundClipFilenameAction(ActionListener l) {
        return new SetSoundClipFilenameAction(l);
    }

    public AbstractAction getSetSoundClipFilenameAction() {
        return new SetSoundClipFilenameAction();
    }

    private void setSoundSample() {
        if (log.isDebug()) log.debug("Setting sound to: " + this.soundClipFilename + " for owner: " + description);

        String soundClipFilename = isSoundFileValid() ? this.soundClipFilename : DEFAULT_SOUND_CLIP_FILENAME;
        try {
            soundSample = new SoundSample(soundClipFilename);
            log.info("Set sound to: " + soundClipFilename);
        } catch (Exception ex) {
            userLog.error("Error loading sound: " + soundClipFilename + " for owner: " + description);
            log.error("Error loading sound: " + soundClipFilename + " for onwer: " + description, ex);

            try {
                soundSample = new SoundSample(DEFAULT_SOUND_CLIP_FILENAME);
                log.info("Set sound to: " + soundClipFilename + " for owner: " + description);
            } catch (Exception ex2) {
                userLog.error("Error loading sound: " + soundClipFilename + " for owner: " + description);
                log.error("Error loading sound: " + soundClipFilename + " for owner: " + description, ex);
            }
        }
    }

    public static void setUseExternalPlayer(boolean useExternalPlayer) {
        synchronized (lock) {
            if (useExternalPlayer) {
                try {
                    externalPlayerManager.start(externalPlayerJvm);
                    for (Iterator iterator = soundHandlers.iterator(); iterator.hasNext();) {
                        SoundHandler soundHandler = (SoundHandler) iterator.next();
                        externalPlayerManager.preload(soundHandler.soundClipFilename);
                    }
                    SoundHandler.useExternalPlayer = true;
                } catch (Exception ex) {
                    log.error("Cannot start external player", ex);
                    SoundHandler.useExternalPlayer = false;
                    externalPlayerManager.stop();
                }
            } else {
                SoundHandler.useExternalPlayer = false;
                externalPlayerManager.stop();
            }
        }
    }

    public static boolean isUseExternalPlayer() {
        synchronized (lock) {
            if (useExternalPlayer) {
                // check that the player is running
                useExternalPlayer = externalPlayerManager.isRunning();
            }
            return useExternalPlayer;
        }
    }

    public static String getExternalPlayerJvm() {
        return externalPlayerJvm;
    }

    public static void setExternalPlayerJvm(String externalPlayerJvm) {
        SoundHandler.externalPlayerJvm = externalPlayerJvm;
    }

    public static void restartExternalPlayer() {
        synchronized (lock) {
            if (isUseExternalPlayer()) {
                externalPlayerManager.restart();
            }
        }
    }

    /**
     * amplifies the sound clip.
     *
     * @param gain ranges between -1 and 1. -1 is maximum attenuation, 1 is maximum amplification, 0 is unchanged
     */
    public void setGain(float gain) {
        soundSample.setGain(gain);
    }

    public float getGain() {
        return soundSample.getGain();
    }

    private class PlayAction implements Runnable {
        public void run() {
            play();
        }
    }

    private class SetSoundEnabledToggleButtonModel extends JToggleButton.ToggleButtonModel {
        public boolean isSelected() {
            return isSoundEnabled();
        }

        public void setSelected(boolean b) {
            super.setSelected(b);
            setSoundEnabled(b);
        }
    }

    private class SetSoundClipFilenameAction extends AuditedAbstractAction {
        private ActionListener actionListener;

        public SetSoundClipFilenameAction() {
            this(null);
        }

        public SetSoundClipFilenameAction(ActionListener actionListener) {
            super("Set Sound File...");
            this.actionListener = actionListener;
        }

        public void auditedActionPerformed(ActionEvent e) {
            SecurityManager backup = System.getSecurityManager();
            System.setSecurityManager(null); // for Webstart, otherwise we get a FilePermissionException

            JFileChooser fileChooser = new JFileChooser(IOUtilities.getUserBaseDirectory());
            fileChooser.setDialogTitle("Set Sound File");
            if (soundClipFilename != null && !Utilities.equals(soundClipFilename, DEFAULT_SOUND_CLIP_FILENAME)) fileChooser.setSelectedFile(new File(soundClipFilename));
            fileChooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".wav");
                }

                public String getDescription() {
                    return "Wave sound (*.wav)";
                }
            });

            int returnVal = fileChooser.showOpenDialog(UIUtilities.getWindowParent((Component) e.getSource()));

            System.setSecurityManager(backup);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                //I'm not too sure the logic here is right... we copy the file to the user config area
                //but the soundClipFilename keeps pointing to the source file
                setSoundClipFilename(fileChooser.getSelectedFile().getAbsolutePath());
                String soundDir = IOUtilities.getUserConfigDirectory() + "/sound";
                if (!soundClipFilename.replace('\\','/' ).startsWith(soundDir)) {
                    try {
                        String destination = soundDir + "/" + fileChooser.getSelectedFile().getName();
                        IOUtilities.copyFile(soundClipFilename, destination);
                    } catch (IOException e1) {
                        log.warn("Sound file copy did not work.", e1);
                    }
                }
            } else {
                if (soundClipFilename == null) {
                    setSoundSample();
                }
            }

            //notify listener that the file has changed
            if (actionListener != null ) {
                actionListener.actionPerformed(e);
            }
        }
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return "SoundHandler{" + description + "," +
                "soundClipFilename='" + soundClipFilename + "'" +
                ", soundEnabled=" + soundEnabled +
                "}";
    }

    public static void main(String[] args) {
        try {
            SoundHandler soundHandler = new SoundHandler(true, "C:/Projects_DrKW/SHIVA_HEAD/Bhavaya/resources/buyitem.wav", "");
            soundHandler.setGain(-1);
            soundHandler.getPlayRunnable().run();
            System.in.read();

            soundHandler.setGain(-0.2f);
            soundHandler.getPlayRunnable().run();
            System.in.read();

            soundHandler.setGain(0);
            soundHandler.getPlayRunnable().run();
            System.in.read();

            soundHandler.setGain(0.5f);
            soundHandler.getPlayRunnable().run();
            System.in.read();

            soundHandler.setGain(1);
            soundHandler.getPlayRunnable().run();
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
    }

}
