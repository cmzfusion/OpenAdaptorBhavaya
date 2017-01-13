package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import org.apache.log4j.Level;
import org.bhavaya.ui.LogPanel;
import org.bhavaya.ui.SoundHandler;
import org.bhavaya.util.ExternalPlayerManager;
import org.bhavaya.util.Log;
import org.bhavaya.util.SoundPlayListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class ExternalPlayerTest {

    private static final Log log = Log.getCategory(ExternalPlayerTest.class);

    private static final LoggingSoundPlayListener LISTENER = new LoggingSoundPlayListener();

    private ExternalPlayerManager manager;
    private JTextField soundFileField;
    private JComboBox soundHandlerChooser;
    private JLabel jvmLabel;
    private boolean runStressTest = false;

    public ExternalPlayerTest() {
        this.manager = new ExternalPlayerManager();
    }

    /**
     * Runs the external player test by using the external player manager directly.
     */
    private void runExternalPlayerManagerTestGui() {
        final JFrame frame = new JFrame("External player test (ExternalPlayerManager edition)");
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        soundFileField = new JTextField("c:\\Temp\\ping.wav");
        panel.add(soundFileField, BorderLayout.NORTH);

        LogPanel logPanel = new LogPanel();
        Log.getCategory(ExternalPlayerManager.class).addListener(logPanel, Level.INFO);
        Log.getCategory(ExternalPlayerTest.class).addListener(logPanel, Level.INFO);
        Log.getCategory(SoundHandler.class).addListener(logPanel, Level.INFO);
        panel.add(logPanel, BorderLayout.CENTER);

        jvmLabel = new JLabel(ExternalPlayerManager.getDefaultJVMPath());
        panel.add(jvmLabel, BorderLayout.SOUTH);

        JPanel actionsPanel = new JPanel(new GridLayout(10, 1, 5, 5));

        actionsPanel.add(new JButton(new SpawnAction("Start") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    manager.start(jvmLabel.getText());
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Stop") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    manager.stop();
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Restart") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    manager.restart();
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Preload") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    manager.preload(soundFileField.getText());
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Play") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    manager.play(soundFileField.getText());
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Release") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    manager.release(soundFileField.getText());
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        final Thread stressTestThread = new Thread("Stress test") {
            public void run() {
                int batchCount = 0;
                while (true) {
                    while (!runStressTest) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            log.error(e);
                        }
                    }
                    log.info("Running test batch: " + batchCount++);

                    log.info("Test 1: Repeatedly play same sound");
                    for (int i = 0; i < 1000; i++) {
                        manager.play("c:/temp/sonar.wav");
                    }

                    log.info("Test 2: Play new sounds simultaneously from different threads");
                    final int THREAD_COUNT = 10;
                    final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT + 1);
                    for (int i = 0; i < THREAD_COUNT; i++) {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                for (int j = 0; j < 100; j++) {
                                    try {
                                        manager.play("c:/temp/ping.wav");
                                    } catch (Exception e) {
                                        log.error(e);
                                    }
                                }
                                try {
                                    barrier.barrier();
                                } catch (InterruptedException e) {
                                    log.error(e);
                                }
                            }
                        }, "Test Thread " + i);
                        thread.start();
                    }

                    try {
                        barrier.barrier();
                    } catch (InterruptedException ex) {
                        log.error(ex);
                    }
                    log.info("Batch complete");
                }
            }
        };

        actionsPanel.add(new JToggleButton(new SpawnAction("Run stress test") {
            public void actionPerformedInNewThread(ActionEvent e) {
                JToggleButton toggleButton = (JToggleButton) e.getSource();
                if (toggleButton.isSelected()) {
                    runStressTest = true;
                    if (!stressTestThread.isAlive()) {
                        stressTestThread.setPriority(Thread.NORM_PRIORITY);
                        stressTestThread.start();
                    }
                } else {
                    runStressTest = false;
                }
            }

        }));


        actionsPanel.add(new JButton(new SpawnAction("Select JVM") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    FileDialog fileDialog = new FileDialog(frame);
                    fileDialog.setVisible(true);
                    final String fileName = fileDialog.getFile();
                    final String dir = fileDialog.getDirectory();
                    if (fileName != null && fileName.length() > 0) {
                        File f = new File(dir, fileName);
                        if (f.exists() && f.isFile()) {
                            jvmLabel.setText(f.getAbsolutePath());
                        }
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        panel.add(actionsPanel, BorderLayout.EAST);

        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Runs the external player test by using the {@link org.bhavaya.ui.SoundHandler} class
     */
    private void runExternalPlayerTestGui() {
        final JFrame frame = new JFrame("External player test (SoundHandler edition)");
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        soundHandlerChooser = new JComboBox();
        soundHandlerChooser.setEditable(true);
        soundHandlerChooser.getEditor().setItem("c:\\Temp\\ping.wav");
        panel.add(soundHandlerChooser, BorderLayout.NORTH);

        LogPanel logPanel = new LogPanel();
        Log.getCategory(ExternalPlayerManager.class).addListener(logPanel, Level.INFO);
        Log.getCategory(ExternalPlayerTest.class).addListener(logPanel, Level.INFO);
        Log.getCategory(SoundHandler.class).addListener(logPanel, Level.INFO);
        panel.add(logPanel, BorderLayout.CENTER);

        jvmLabel = new JLabel(ExternalPlayerManager.getDefaultJVMPath());
        panel.add(jvmLabel, BorderLayout.SOUTH);

        JPanel actionsPanel = new JPanel(new GridLayout(10, 1, 5, 5));

        actionsPanel.add(new JToggleButton(new SpawnAction("Enable external player") {
            public void actionPerformedInNewThread(ActionEvent e) {
                JToggleButton toggleButton = (JToggleButton) e.getSource();
                SoundHandler.setUseExternalPlayer(toggleButton.isSelected());
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Create sound handler") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    Object selectedItem = soundHandlerChooser.getSelectedItem();
                    if (selectedItem instanceof SoundHandler) {
                        log.info("SoundHandler already created.");
                    } else {
                        SoundHandler soundHandler = new SoundHandler(true, (String) selectedItem, "test");
                        soundHandlerChooser.addItem(soundHandler);
                        soundHandlerChooser.setSelectedItem(soundHandler);
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Enable") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    Object selectedItem = soundHandlerChooser.getSelectedItem();
                    if (selectedItem instanceof SoundHandler) {
                        ((SoundHandler) selectedItem).setSoundEnabled(true);
                    } else {
                        log.error("Please select sound handler item");
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Disable") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    Object selectedItem = soundHandlerChooser.getSelectedItem();
                    if (selectedItem instanceof SoundHandler) {
                        ((SoundHandler) selectedItem).setSoundEnabled(false);
                    } else {
                        log.error("Please select sound handler item");
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        actionsPanel.add(new JButton(new SpawnAction("Play") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    Object selectedItem = soundHandlerChooser.getSelectedItem();
                    if (selectedItem instanceof SoundHandler) {
                        final SoundHandler soundHandler = (SoundHandler) selectedItem;
                        soundHandler.play(new SoundPlayListener() {
                            public void beforePlayingSound() {
                                log.info("About to play sound " + soundHandler.getSoundClipFilename());
                            }

                            public void afterPlayingSound() {
                                log.info("Succesfuly played sound " + soundHandler.getSoundClipFilename());
                            }

                            public void discarded() {
                                log.info("Sound discarded " + soundHandler.getSoundClipFilename());
                            }
                        });
                    } else {
                        log.error("Please select sound handler item");
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        final Thread stressTestThread = new Thread("Stress test") {
            public void run() {
                int batchCount = 0;
                SoundHandler fixSoundSample = new SoundHandler(true, "c:/temp/buyitem.wav", "test");
                while (true) {
                    while (!runStressTest) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            log.error(e);
                        }
                    }
                    log.info("Running test batch: " + batchCount++);

                    log.info("Test 1: Repeatedly play same SoundSample");
                    SoundHandler soundSample = new SoundHandler(true, "c:/temp/sonar.wav", "test");
                    for (int i = 0; i < 1000; i++) {
                        soundSample.play(LISTENER);
                    }

                    log.info("Test 2: Repeatedly play different SoundSample");
                    for (int i = 0; i < 1000; i++) {
                        soundSample = new SoundHandler(true, "c:/temp/das_boot.wav", "test");
                        soundSample.play(LISTENER);
                    }

                    log.info("Test 3: Repeatedly play fix SoundSample");
                    for (int i = 0; i < 1000; i++) {
                        fixSoundSample.play(LISTENER);
                    }

                    log.info("Test 4: Play new SoundSamples simultaneously in different threads");
                    final int THREAD_COUNT = 10;
                    final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT + 1);
                    for (int i = 0; i < THREAD_COUNT; i++) {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                for (int j = 0; j < 100; j++) {
                                    try {
                                        SoundHandler soundSample;
                                        soundSample = new SoundHandler(true, "c:/temp/ping.wav", "test");
                                        soundSample.play(LISTENER);
                                    } catch (Exception e) {
                                        log.error(e);
                                    }
                                }
                                try {
                                    barrier.barrier();
                                } catch (InterruptedException e) {
                                    log.error(e);
                                }
                            }
                        }, "Test Thread " + i);
                        thread.start();
                    }

                    try {
                        barrier.barrier();
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                    log.info("Batch complete");
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                    System.gc();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
            }
        };

        actionsPanel.add(new JToggleButton(new SpawnAction("Run stress test") {
            public void actionPerformedInNewThread(ActionEvent e) {
                JToggleButton toggleButton = (JToggleButton) e.getSource();
                if (toggleButton.isSelected()) {
                    runStressTest = true;
                    if (!stressTestThread.isAlive()) {
                        stressTestThread.setPriority(Thread.NORM_PRIORITY);
                        stressTestThread.start();
                    }
                } else {
                    runStressTest = false;
                }
            }

        }));

        actionsPanel.add(new JButton(new SpawnAction("Select JVM") {
            public void actionPerformedInNewThread(ActionEvent e) {
                try {
                    FileDialog fileDialog = new FileDialog(frame);
                    fileDialog.setVisible(true);
                    final String fileName = fileDialog.getFile();
                    final String dir = fileDialog.getDirectory();
                    if (fileName != null && fileName.length() > 0) {
                        File f = new File(dir, fileName);
                        if (f.exists() && f.isFile()) {
                            jvmLabel.setText(f.getAbsolutePath());
                            SoundHandler.setExternalPlayerJvm(f.getAbsolutePath());
                        }
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        }));

        panel.add(actionsPanel, BorderLayout.EAST);

        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private abstract class SpawnAction extends AbstractAction {
        public SpawnAction(String name) {
            super(name);
        }

        public void actionPerformed(final ActionEvent e) {
            Thread t = new Thread(String.valueOf(getValue(Action.NAME))) {
                public void run() {
                    actionPerformedInNewThread(e);
                }
            };
            t.start();
        }

        public abstract void actionPerformedInNewThread(ActionEvent e);
    }

    private static class LoggingSoundPlayListener implements SoundPlayListener {
        public void beforePlayingSound() {
            log.info("before");
        }

        public void afterPlayingSound() {
            log.info("after");
        }

        public void discarded() {
            log.info("discarded");
        }
    }

    public static void main(String[] args) {
//        new ExternalPlayerTest().runExternalPlayerManagerTestGui();
        new ExternalPlayerTest().runExternalPlayerTestGui();
    }
}
