package org.bhavaya.ui.table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created with IntelliJ IDEA.
 * User: ga2armn
 * Date: 5/3/13
 * Time: 8:36 AM
 */
public class FadingWindow extends JWindow {

    protected final float MAX_OPACITY = 0.75f;
    protected final float MIN_OPACITY = 0.2f;
    protected final float DX_OPACITY = 0.015f;

    protected float opacity = MAX_OPACITY;

    protected JPanel panel;

    Action fadeAction = new AbstractAction(){
        public void actionPerformed(ActionEvent e) {
            opacity -= DX_OPACITY;
            panel.repaint();
            if(opacity < MIN_OPACITY){
                stop();
            }
        }
    };

    private Timer fadeTimer = new Timer(30, fadeAction){
        public void restart() {
            super.restart();
            opacity = MAX_OPACITY;
        }
    };

    public FadingWindow(Component owner){
        super((JFrame) SwingUtilities.getRoot(owner));
    }

    public void initPanel(JPanel panel){
        this.panel = panel;

        this.getRootPane().setOpaque(false);
        opacity = MAX_OPACITY;

        try{
            this.setBackground(new Color(0, 0, 0, 0));
            this.getRootPane().setBackground(new Color(0, 0, 0, 0));
        }
        catch( java.lang.UnsupportedOperationException uoe){
           // Do nothing - if its not supported the background will be visible regardless of alpha
        }
        this.getContentPane().add(panel);
        this.setAlwaysOnTop(true);
        pack();

        fadeTimer.setInitialDelay(500);
        fadeTimer.start();

    }

    protected void mouseHasEntered(MouseEvent e){
        fadeTimer.stop();
        opacity = MAX_OPACITY;
        repaint();
    }

    public void mouseHasExited(MouseEvent e) {
        fadeTimer.restart();
        repaint();
    }

    protected void stop(){
        fadeTimer.stop();
        setVisible(false);
    }

    public void setVisible(boolean vis) {
        if(vis){
            opacity = MAX_OPACITY;
            fadeTimer.restart();
        }
        super.setVisible(vis);
    }

}
