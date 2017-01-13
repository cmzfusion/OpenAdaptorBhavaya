package org.bhavaya.util;

import org.bhavaya.util.Environment;

import javax.swing.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: deana
 * Date: 09-Mar-2004
 * Time: 11:14:56
 * To change this template use File | Settings | File Templates.
 */
public class EnvironmentVariablesDemo {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Environment Variables Demo");
        JPanel panel = new JPanel();
        JTextArea textArea = new JTextArea(10,50);

        Map propertyMap;
        propertyMap = Environment.getProperties();
        Set data = propertyMap.entrySet();
        Iterator iterator = data.iterator();
        while(iterator.hasNext()){
            textArea.append(iterator.next().toString() + "\n");
        }

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
}

