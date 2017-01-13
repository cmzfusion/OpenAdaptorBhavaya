package org.bhavaya.util;

import org.bhavaya.util.WeakReferenceTimer;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 22-Feb-2008
 * Time: 15:37:25
 */
public class TestWeakReferenceTimer {

   public static void main(String[] args) {
       for ( int loop=0; loop < 1000; loop ++) {
            new TimerClient();
       }
   }

   public static class TimerClient {
       private int[] wasteMemory = new int[500000];

       private ActionListener actionListener = new ActionListener() {
           public void actionPerformed(ActionEvent e) {
           }
       };
       
       public TimerClient() {

           //uncommenting this will cause a heap overflow since the ActionListener timer hangs onto the
           //TimerClient outer class, prevening it from being garbage collected
           //testTimer();

           //using the weak ref timer prevents the out of memory since the weak ref wrapper makes the
           //TimerClient instances available for garbage collection
           testWeakRefTimer();
       }

       private void testWeakRefTimer() {
           Timer t = WeakReferenceTimer.createTimer(1000, actionListener);
           t.start();
       }

       private void testTimer() {
           Timer t = new Timer(1000, actionListener);
           t.start();
       }


   }
}
