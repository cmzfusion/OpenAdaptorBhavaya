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

package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.Channel;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;

/**
 * Represents a resources backed playable sound object.  In theory, this doesn't really add much more than an
 * javax.sound.Clip, but as we've noticed repeated instablity with several implementations, we've shielded the
 * rest of the framework from the way we decide to implement the audio.
 *
 * @author Brendon McLean
 * @version $Revision: 1.6 $
 */

public class SoundSample {
    /*
     * Implementation History
     *
     * This class has been through many more iterations than its CVS history suggests.  It's used SourceDataLines,
     * Clips, and even applet's AudioClip.  But so far we've always run into machine configurations where playing
     * a simple beep will bring down the VM.  1.4.2_05 is looking more promising but still we seem to have problems.
     * The approach used here is to circumvent a bug in 1.4.2_05 that may or may not contribute to a VM crash.  The
     * bug has been filed but has yet to receive a number.  Basically, each sound card can only play a finite number
     * of sounds simultaneously.  Once these channels are committed, JavaSound will throw a LineUnavailableException.
     * From an API perspective, this is perfect, but in reality JavaSound leaks the entire buffer (ie. the size of the
     * sound file).  Given the frequency with which this can happen and also the size of some users' preferred sound
     * file these leaks can add up quickly.  This implementation aims to provide a preventative Java limit on number
     * sound channels.
     */

    private static final Log log = Log.getCategory(SoundSample.class);
    private static final int MAXIMUM_NUMBER_OF_SOUND_CHANNELS = 16;

    private static PooledExecutor executor;

    static {
        Channel channel = new BoundedBuffer(MAXIMUM_NUMBER_OF_SOUND_CHANNELS);
        executor = new PooledExecutor(channel);
        executor.setMaximumPoolSize(MAXIMUM_NUMBER_OF_SOUND_CHANNELS);
        executor.setMinimumPoolSize(3);
        executor.setBlockedExecutionHandler(new DiscardOldestWhenBlocked(channel));
        executor.setKeepAliveTime(1000 * 60 * 5); // 5 Minutes
    }

    public static final int INIT_BUF_SIZE = 17000;

    private byte[] buffer;
    private float gain;

    public SoundSample(String fileName) throws IOException {
        InputStream is = SoundSample.class.getResourceAsStream(fileName);
        if (is == null) is = new BufferedInputStream(new FileInputStream(fileName), 1024);
        this.buffer = loadData(is);
        is.close();
    }

    /**
     * amplifies the sound clip.
     *
     * @param gain ranges between -1 and 1. -1 is maximum attenuation, 1 is maximum amplification, 0 is unchanged
     */
    public void setGain(float gain) {
        this.gain = Math.min(Math.max(-1, gain), 1);
    }

    public float getGain() {
        return gain;
    }

    public void play() {
        play(null);
    }

    public void play(final SoundPlayListener listener) {
        try {
            executor.execute(new DiscardableRunnable() {
                public void run() {
                    try {
                        if (listener != null) {
                            listener.beforePlayingSound();
                        }
                        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(buffer));
                        AudioFormat audioFormat = audioInputStream.getFormat();

                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

                        line.open(audioFormat);
                        line.start();

                        int bytesRead = 0;
                        byte[] data = new byte[INIT_BUF_SIZE];
                        while (bytesRead != -1) {
                            bytesRead = audioInputStream.read(data, 0, data.length);
                            if (bytesRead >= 0) line.write(data, 0, bytesRead);
                        }

                        line.drain();
                        line.close();

                        if (listener != null) {
                            listener.afterPlayingSound();
                        }
                    } catch (Exception e) {
                        log.error("Error during sound playback", e);
                    }
                }

                public void discarded() {
                    if (listener != null) {
                        listener.discarded();
                    }
                }
            });
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    private byte[] loadData(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(INIT_BUF_SIZE);
        int bytesRead;
        byte[] byteBuf = new byte[INIT_BUF_SIZE / 8];
        while ((bytesRead = is.read(byteBuf)) != -1) {
            bos.write(byteBuf, 0, bytesRead);
        }
        return bos.toByteArray();
    }

    private interface DiscardableRunnable extends Runnable {
        /**
         * Tells the Runnable that is has been discarded rather than run.
         */
        public void discarded();
    }

    /**
     * Redo of the class from concurrent library with addition that it tells the task it has been discarded without
     * being executed.
     */
    private static class DiscardOldestWhenBlocked implements PooledExecutor.BlockedExecutionHandler {
        Channel channel;

        public DiscardOldestWhenBlocked(Channel channel) {
            this.channel = channel;
        }

        public boolean blockedAction(Runnable command) throws InterruptedException {
            Object toBeDiscarded = channel.poll(0);
            if (toBeDiscarded instanceof DiscardableRunnable) {
                ((DiscardableRunnable)toBeDiscarded).discarded();
            }
            if (!channel.offer(command, 0)) {
                command.run();
            }
            return true;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        JFrame frame = new JFrame("Sound Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final JTextField textField = new JTextField("Test running...");
        frame.setContentPane(textField);
        frame.pack();
        frame.show();

        int batchCount = 0;
        SoundSample fixSoundSample = new SoundSample("/buyitem.wav");
        for (; ;) {
            System.out.println("Running test batch: " + batchCount++);

            System.out.println("Test 1: Repeatedly play same SoundSample");
            SoundSample soundSample = new SoundSample("/sonar.wav");
            for (int i = 0; i < 1000; i++) {
                soundSample.play();
            }

            System.out.println("Test 2: Repeatedly play different SoundSample");
            for (int i = 0; i < 1000; i++) {
                soundSample = new SoundSample("/das_boot.wav");
                soundSample.play();
            }

            System.out.println("Test 3: Repeatedly play fix SoundSample");
            for (int i = 0; i < 1000; i++) {
                fixSoundSample.play();
            }

            System.out.println("Test 4: Play new SoundSamples simultaneously in different threads");
            final int THREAD_COUNT = 10;
            final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT + 1);
            for (int i = 0; i < THREAD_COUNT; i++) {
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        for (int j = 0; j < 100; j++) {
                            try {
                                SoundSample soundSample;
                                soundSample = new SoundSample("/ping.wav");
                                soundSample.play();
                            } catch (IOException e) {
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

            barrier.barrier();
            System.out.println("Batch complete");
            Thread.sleep(50);
            System.gc();
            Thread.sleep(50);
        }
    }
}
