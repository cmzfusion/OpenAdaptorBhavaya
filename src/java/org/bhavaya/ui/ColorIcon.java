package org.bhavaya.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.AffineTransform;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Mar 28, 2005
 * Time: 5:09:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ColorIcon implements Icon {
    private static final int DEFAULT_WIDTH = 24;
    private static final int DEFAULT_HEIGHT = 16;

    private Color color;
    private int width;
    private int height;
    private int arcDiameter;
    private RenderingHints renderHints;
    private Color outlineColor;


    public ColorIcon(Color color) {
        this(color, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public ColorIcon(Color color, int width, int height) {
        this.color = color;
        this.width = width;
        this.height = height;
        this.arcDiameter = Math.min(width, height) /  2;
        this.outlineColor = UIUtilities.createDarkerColor(color, 0.5f);
        renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g;

        // Save current render state
        RenderingHints oldRenderingHints = g2d.getRenderingHints();
        Paint oldPaint = g2d.getPaint();
        Stroke oldStroke = g2d.getStroke();
        AffineTransform oldTransform = g2d.getTransform();

        // Turn on anti-aliasing
        g2d.setRenderingHints(renderHints);

        Shape roundRect = new RoundRectangle2D.Float(x, y, width - 2, height - 2, arcDiameter, arcDiameter);
        Stroke outlineStroke = new BasicStroke(1.5f);
        g2d.translate(1, 1);

        // Fill the background first in the normal colour
        g2d.setPaint(color);
        g2d.fill(roundRect);

        // Then stroke the outline
        g2d.setPaint(outlineColor);
        g2d.setStroke(outlineStroke);
        g2d.draw(roundRect);

        // Reset to previous render state
        g2d.setRenderingHints(oldRenderingHints);
        g2d.setPaint(oldPaint);
        g2d.setStroke(oldStroke);
        g2d.setTransform(oldTransform);
    }

    public int getIconWidth() {
        return width;
    }

    public int getIconHeight() {
        return height;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JButton("Red", new ColorIcon(Color.red)));
        frame.pack();
        frame.show();
    }
}
