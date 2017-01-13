package org.bhavaya.ui;

import org.bhavaya.util.IOUtilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
public class ImageComponent extends JPanel {
    private BufferedImage image;
    private int width, height;

    public ImageComponent(String resourceName) {
        super(false);
        try {
            image = ImageIO.read(IOUtilities.getResourceAsStream(resourceName));
            width = image.getWidth();
            height = image.getHeight();
        } catch (IOException e) {
        }
    }

    public ImageComponent(String resourceName, int imageHeight) {
        this(resourceName);
        setHeight(imageHeight);
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, this);
    }

    public void setHeight(int height) {
        int oldHeight = this.height;
        this.height = height;
        width = width * this.height / oldHeight;
    }
}
