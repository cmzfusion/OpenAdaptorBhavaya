package org.bhavaya.ui.view.composite;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 13-May-2008
 * Time: 14:31:41
 */
public class VerticalLayout extends AbstractSingleRowOrColumnLayout {

    protected void configureGridBagConstraints(GridBagConstraints gbc) {
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.gridx = 0;
    }

    public String getDescription() {
        return "Vertical";
    }
}
