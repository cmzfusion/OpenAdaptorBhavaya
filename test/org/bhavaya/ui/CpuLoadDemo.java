package org.bhavaya.ui;

import org.bhavaya.util.CpuLoad;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA.
 * User: deana
 * Date: 04-Mar-2004
 * Time: 12:02:09
 * To change this template use File | Settings | File Templates.
 */
public class CpuLoadDemo {
    private Timer timer;
    private CpuLoad load = CpuLoad.getInstance();
    private JLabel loadLabel;
    private JLabel avLoadLabel;
    private static final DecimalFormat formatter = new DecimalFormat("##.##");

    public CpuLoadDemo(JPanel panel) {

        loadLabel = new JLabel();
        avLoadLabel = new JLabel();

        panel.setLayout(new BorderLayout());
        panel.add(loadLabel, BorderLayout.NORTH);
        panel.add(avLoadLabel, BorderLayout.SOUTH);

        timer = new Timer();
        timer.schedule(new Load(),
                0*1000,   //initial delay
                5*1000);  //subsequent rate
    }

    private class Load extends TimerTask{
        public void run() {
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    loadLabel.setText("Current Load = " + formatter.format(load.getLoad()*100) + "%");
                    avLoadLabel.setText("Average Load = " + formatter.format(load.getLoadAverage()*100) + "%");
                }});
        }
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("Cpu Load Demo");
        JPanel panel = new JPanel();

        new CpuLoadDemo(panel);

        frame.setContentPane(panel);
        frame.setSize(170,60);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }

}
