package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 17-Sep-2008
 * Time: 14:23:43
 */
public class GradientPanel extends JPanel {

    private Color color1;
    private Color color2;
    private BufferedImage cache;

    public GradientPanel(LayoutManager layout, Color color1, Color color2) {
        super(layout);
        this.color1 = color1;
        this.color2 = color2;
    }

     public GradientPanel(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;
    }

    public GradientPanel(LayoutManager layout) {
        this(layout, Color.WHITE, SystemColor.control);
    }

    public GradientPanel() {
        this(Color.WHITE, SystemColor.control);
    }

    public void setColor1(Color c1) {
        this.color1 = c1;
        cache = null;
        repaint();
    }

    public void setColor2(Color c2) {
        this.color2 = c2;
        cache = null;
        repaint();
    }     // Overloaded in order to paint the background

    public void setColors(Color c1, Color c2) {
        this.color2 = c2;
        this.color1 = c1;
        cache = null;
        repaint();
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        instrumentedPaintComponent(g2);
    }

    protected void instrumentedPaintComponent(Graphics2D g2)
    {
        if (cache == null || cache.getHeight() != getHeight()) {
            cache = new BufferedImage(2, getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = cache.createGraphics();

            GradientPaint paint = new GradientPaint(0, 0, color1,0, getHeight(), color2);
            g2d.setPaint(paint);
            g2d.fillRect(0, 0, 2, getHeight());
            g2d.dispose();
        }
        g2.drawImage(cache, 0, 0, getWidth(), getHeight(), null);
    }
    
}
