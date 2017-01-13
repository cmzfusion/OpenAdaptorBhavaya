package org.bhavaya.ui;

import java.awt.*;

/**
 * Interface to define a flashing component
 * User: ga2mop0
 * Date: 29/07/13
 * Time: 15:09
 */
public interface FlashingTabButton {
    void startFlashing(Color color);

    void stopFlashing();

    boolean isSelected();

    void setButtonVisible(boolean show);
}
