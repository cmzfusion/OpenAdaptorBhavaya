package org.bhavaya.util;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 22-Feb-2008
 * Time: 15:25:44
 *
 * A subclass of java.swing.Timer which wraps the ActionListener with a weak reference listener,
 * so that references reachable by the running timer thread do not cause garbage collection issues.
 *
 * If a client class adds an ActionListener to a normal Timer, and the ActionListener
 * is an inner class or holds references back to the client, this can prevent instances of
 * the client class being garbage collected - until the timer is stopped.
 * For the UI, if a UI component uses a timer, this will prevent the component being released when
 * it is no longer used, unless there is special dispose logic to stop the timer.
 *
 * The WeakReferenceTimer detects when the client class has been garbage collected,
 * and stops the Timer automatically
 */
public class WeakReferenceTimer extends Timer {

    private WeakReferenceTimer(int delay, ActionListener listener) {
        super(delay, listener);
    }

    /**
     * @return A Timer in which the ActionListener is wrapped with a WeakReference listener
     *
     * nb. you must hold a reference to the action listener when you call this
     * Calling it with an action listener instance which is an anonymous inner class will simply result
     * in the listener being collected immediately!
     */
    public static Timer createTimer(int delay, ActionListener listener) {
        WeakRefActionListener l = new WeakRefActionListener(listener);
        WeakReferenceTimer t = new WeakReferenceTimer(delay, l);
        l.setTimer(t);
        return t;
    }

    private static class WeakRefActionListener implements ActionListener {
        private WeakReference<ActionListener> weakListener;
        private Timer timer;

        public WeakRefActionListener(ActionListener l) {
            weakListener = new WeakReference<ActionListener>(l);
        }

        private void setTimer(Timer timer) {
            this.timer = timer;
        }

        public void actionPerformed(ActionEvent e) {
            ActionListener l = weakListener.get();
            if ( l != null ) {
                l.actionPerformed(e);
            }
            else {
                timer.stop();
            }
        }
    }
}
