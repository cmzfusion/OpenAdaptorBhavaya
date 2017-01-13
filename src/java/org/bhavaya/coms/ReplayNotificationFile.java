package org.bhavaya.coms;

import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Sep 5, 2006
 * Time: 11:17:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class ReplayNotificationFile {
    public static void main(String[] args) throws FileNotFoundException, NotificationException {
        if (args.length != 2) exitWithUsage(null);

        String filename = args[1];
        File inputFile = new File(filename);
        if (!inputFile.exists()) exitWithUsage(filename + " does not exist");

        String notificationSubject = args[0];
        NotificationPublisher notificationPublisher = NotificationPublisher.getInstance(notificationSubject);
        if (notificationPublisher == null) exitWithUsage(notificationSubject + " is not a valid notification subject");

        notificationPublisher.connect();
        
        publishFile(inputFile, notificationPublisher);
    }

    private static void publishFile(File inputFile, NotificationPublisher notificationPublisher) throws FileNotFoundException, NotificationException {
        Pattern logPattern = Pattern.compile("(\\d+-\\d+-\\d+ \\d+:\\d+:\\d+,\\d+ - )(.*)", Pattern.DOTALL | Pattern.MULTILINE);

        char[] msgBuffer = new char[4096];
        int bufferCapacity = 4096;

        int bufferIndex;
        char[] msg;
        int ch;

        Reader reader = new InputStreamReader(new FileInputStream(inputFile));

        boolean active = true;

        while (active) {
            bufferIndex = 0;
            try {
                do {
                    ch = reader.read();
                    if (ch == -1 || ch == Character.MAX_VALUE) { // We don't know why Character.MAX_VALUE, but remove at your peril.
                        active = false;
                        break;
                    }

                    if (bufferIndex >= bufferCapacity) {
                        bufferCapacity *= 2;
                        char[] newBuffer = new char[bufferCapacity];
                        System.arraycopy(msgBuffer, 0, newBuffer, 0, msgBuffer.length);
                        msgBuffer = newBuffer;
                    }

                    msgBuffer[bufferIndex++] = (char) ch;
                } while (ch != '\0');

                if (!active) {
                    break;
                }

                msg = new char[bufferIndex - 1];
                System.arraycopy(msgBuffer, 0, msg, 0, bufferIndex - 1);
            } catch (IOException e) {
                active = false;
                break;
            }

            String messageString = new String(msg);
            Matcher matcher = logPattern.matcher(messageString);
            String strippedMessage = matcher.find() ? matcher.group(2) : messageString;

            notificationPublisher.commit(strippedMessage);
        }
    }

    private static void exitWithUsage(String message) {
        System.out.println(message);
        System.out.println("Usage: ReplayNotificationFile <notificationSubject> <notificationFile>");
        System.out.println("\n\t-notificationSubject\tshould be matching entry in application properties");
        System.out.println("\n\t-notificationFile\tfile recorded by NotificationLogger");
        System.exit(1);
    }
}
