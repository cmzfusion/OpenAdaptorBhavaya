package org.bhavaya.util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 11-Feb-2010
 * Time: 12:05:20
 *
 */
public class ImageIconCache {

    //static map to make sure we don't create duplicate image instances
    private static final Map<String, ImageIcon> resourceToImageMap = new ConcurrentHashMap<String, ImageIcon>();
    private static final Map<ImageKey, ImageIcon> sizedImageMap = new ConcurrentHashMap<ImageKey, ImageIcon>();
    private static Image shortcutImage;
    private final static String shortcutImagePrefix = "shortcut_";

    private static final int SCALED_WIDTH = -1;

    static {
        shortcutImage= ImageIconCache.getImageIcon("shortcut.png").getImage();
    }

    /**
     * Call this from the UI event thread only
     * @return an ImageIcon loaded from resourcePath
     */
    public static ImageIcon getImageIcon(String resourcePath) {
        ImageIcon i = resourceToImageMap.get(resourcePath);
        if (i == null) {
            URL imageResource = IOUtilities.getResource(resourcePath);
            if ( imageResource != null) {
                i = new ImageIcon(imageResource);
                resourceToImageMap.put(resourcePath, i);

                ImageKey imageKey = new ImageKey(i.getIconWidth(), i.getIconHeight(), resourcePath);
                sizedImageMap.put(imageKey, i);
            }
        }
        return i;
    }

    /**
     * Call this from the UI event thread only
     * @return ImageIcon loaded from resourcePath with a shortcut image imposed ontop of it.
     */
    public static ImageIcon getShortcutImageIcon(String resourcePath) {
        String shorcutImageResourcePath = shortcutImagePrefix + resourcePath;
        ImageIcon i = resourceToImageMap.get(shorcutImageResourcePath);
        if (i == null) {
            ImageIcon img = ImageIconCache.getImageIcon(resourcePath);
            if(img!=null) {
                i = overlayShortcutImage(img.getImage());
                resourceToImageMap.put(shorcutImageResourcePath, i);
                ImageKey imageKey = new ImageKey(i.getIconWidth(), i.getIconHeight(), shorcutImageResourcePath);
                sizedImageMap.put(imageKey, i);
            }
        }
        return i;
    }

    private static ImageIcon overlayShortcutImage(Image imageToOverlay) {
        BufferedImage bgImage = new BufferedImage(imageToOverlay.getWidth(null), imageToOverlay.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bgImage.createGraphics();
        g2d.drawImage(imageToOverlay,0,0,bgImage.getWidth(),bgImage.getHeight(),null);
        int y = bgImage.getHeight() - shortcutImage.getHeight(null);
        int x1 = 0; int y1 =y;
        int x2 = x1 + shortcutImage.getWidth(null); int y2 = bgImage.getHeight();
        g2d.drawImage(shortcutImage,
                        x1, y1,
                        x2,y2,
                        0,0,
                        shortcutImage.getWidth(null),shortcutImage.getHeight(null)
                        ,null);
        g2d.dispose();
        return new ImageIcon(bgImage);
    }

    /**
     * @return an ImageIcon loaded from resourcePath scaled to width, height
     */
    public static ImageIcon getImageIcon(String resource, int width, int height) {
        //use the temporary key for the lookup, rather than newing one up each time, to avoid object cycling
        ImageIcon i = sizedImageMap.get(new ImageKey(width, height, resource));
        if ( i == null ) {
            ImageIcon defaultSizedImage = getImageIcon(resource);
            if ( defaultSizedImage != null ) {
                Image scaled = defaultSizedImage.getImage().getScaledInstance(
                    width, height, java.awt.Image.SCALE_SMOOTH
                );
                i = new ImageIcon(scaled);
                sizedImageMap.put(new ImageKey(width, height, resource), i);
            }
        }
        return i;
    }

    /**
     * @return an ImageIcon loaded from resourcePath scaled to new height with width in proportion
     */
    public static ImageIcon getImageIcon(String resource, int height) {
        //use the temporary key for the lookup, rather than newing one up each time, to avoid object cycling
        ImageIcon i = sizedImageMap.get(new ImageKey(SCALED_WIDTH, height, resource));
        if ( i == null ) {
            ImageIcon defaultSizedImage = getImageIcon(resource);
            if ( defaultSizedImage != null ) {
                int width = defaultSizedImage.getIconWidth() * height / defaultSizedImage.getIconHeight();
                Image scaled = defaultSizedImage.getImage().getScaledInstance(
                    width, height, java.awt.Image.SCALE_SMOOTH
                );
                i = new ImageIcon(scaled);
                sizedImageMap.put(new ImageKey(SCALED_WIDTH, height, resource), i);
            }
        }
        return i;
    }

    private static class ImageKey {

        int width, height;
        String resourceUrl;

        private ImageKey() {
        }

        private ImageKey(int width, int height, String resourceUrl) {
            this.width = width;
            this.height = height;
            this.resourceUrl = resourceUrl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ImageKey imageKey = (ImageKey) o;

            if (height != imageKey.height) return false;
            if (width != imageKey.width) return false;
            if (resourceUrl != null ? !resourceUrl.equals(imageKey.resourceUrl) : imageKey.resourceUrl != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = width;
            result = 31 * result + height;
            result = 31 * result + (resourceUrl != null ? resourceUrl.hashCode() : 0);
            return result;
        }
    }
}
