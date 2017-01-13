package org.bhavaya.ui.table;

import org.bhavaya.ui.UIUtilities;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.awt.*;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public class HighlightColor {

    static {
        BeanUtilities.addPersistenceDelegate(HighlightColor.class, new BhavayaPersistenceDelegate(new String[]{"baseColor"}));
    }

    private Color evenRowColor;
    private Color oddRowColor;

    public HighlightColor(Color baseColor) {
        this.evenRowColor = baseColor;
        this.oddRowColor = UIUtilities.createBrighterColor(baseColor, 0.5f);
    }

    public Color getHighlightForRow(int row) {
        if ((row % 2) == 0) {
            return evenRowColor;
        } else {
            return oddRowColor;
        }
    }

    public Color getBaseColor() {
        return evenRowColor;
    }
}
