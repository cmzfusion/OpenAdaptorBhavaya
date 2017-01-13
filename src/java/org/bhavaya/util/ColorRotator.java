package org.bhavaya.util;

import java.awt.*;

/**
 * Set of colours to rotate through
 * Stolen from Nick's jtimeseries code...
 * User: Jon Moore
 * Date: 26/05/11
 * Time: 10:04
 */
public class ColorRotator {
    private static Color[] seriesColors = new Color[] {
            Color.BLUE.darker(),
            Color.GREEN.darker(),
            Color.RED.darker(),
            Color.GRAY,
            Color.CYAN.darker(),
            Color.DARK_GRAY,
            Color.MAGENTA,
            Color.ORANGE,
            Color.YELLOW.darker(),
            Color.PINK,
            Color.BLUE,
            Color.CYAN,
            Color.BLUE.brighter(),
            Color.GREEN.darker(),
            Color.MAGENTA.darker()
    };

    private int lastColor;

    public Color getNextColor() {
        return seriesColors[lastColor++ % seriesColors.length];
    }
}
