package org.bhavaya.ui.view.composite;

import org.bhavaya.ui.view.AbstractView;
import org.bhavaya.ui.view.View;

import java.awt.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Lay out child views in a simple left to right arrangement using GridBagLayout
 * where each child view has equal width
 */
public class HorizontalLayout extends AbstractSingleRowOrColumnLayout {

    protected void configureGridBagConstraints(GridBagConstraints gbc) {
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = 0;
    }

    public String getDescription() {
        return "Horizontal";
    }
}
