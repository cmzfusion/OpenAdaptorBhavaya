package org.bhavaya.util;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: deana
 * Date: 09-Mar-2004
 * Time: 11:13:05
 * To change this template use File | Settings | File Templates.
 */
public class ThreadDumpDemo {
    private static DemoLock lock1 = new DemoLock();
    private static DemoLock lock2 = new DemoLock();

    public static void main(String[] args) {
        new DeadlockThread().start();
        new DeadlockThread().start();
        new DeadlockThread().start();
        new DeadlockThread().start();
        new DeadlockThread().start();
        new DeadlockThread().start();
        new DeadlockThread().start();

        StringBuffer tDump = new StringBuffer(500);
        Environment.nativeRequestThreadDump(tDump);

        JFrame frame = new JFrame("Thread Dump Demo");
        JPanel panel = new JPanel();
        JTextArea textArea = new JTextArea(20,60);
        textArea.append(tDump.toString());

        JScrollPane scrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        textArea.setEditable(false);

        panel.add(scrollPane);
        frame.setContentPane(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }


    private static class DemoLock {}

    private static class DeadlockThread extends Thread {
        public void run() {
            synchronized(lock1) {
                synchronized(lock1) {
                    lock(lock2);
                }
            }
        }

        private void lock(Object lock) {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }
}
