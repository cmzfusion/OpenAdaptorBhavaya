package org.bhavaya.ui.view;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 14-Sep-2009
 * Time: 10:20:09
 */
public class FilterTablePanelModel {

    private ButtonModel includeSubstringsInFind = new JToggleButton.ToggleButtonModel();
    private ButtonModel filterWithFindButtonModel = new JToggleButton.ToggleButtonModel();
    private boolean showFilterFindPanel;

    public ButtonModel getIncludeSubstringsInFindButtonModel() {
        return includeSubstringsInFind;
    }

    public boolean isIncludeSubstringsInFind() {
        return includeSubstringsInFind.isSelected();
    }

    public void setIncludeSubstringsInFind(boolean include) {
        includeSubstringsInFind.setSelected(include);
    }

    public boolean isFilterWithFind() {
        return filterWithFindButtonModel.isSelected();
    }

    public void setFilterWithFind(boolean enabled) {
        filterWithFindButtonModel.setSelected(enabled);
    }

    public ButtonModel getFilterWithFindButtonModel() {
        return filterWithFindButtonModel;
    }


    public boolean isShowFilterFindPanel() {
        return showFilterFindPanel;
    }

    public void setShowFilterFindPanel(boolean showFilterFindPanel) {
        this.showFilterFindPanel = showFilterFindPanel;
    }
}
