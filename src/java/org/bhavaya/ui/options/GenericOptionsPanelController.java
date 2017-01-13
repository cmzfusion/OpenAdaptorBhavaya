package org.bhavaya.ui.options;

import org.bhavaya.util.ValidationException;

import javax.swing.*;
import java.util.*;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.4 $
 */
public class GenericOptionsPanelController extends OptionsPanelController {

    private Icon icon;
    private String categoryName;
    private final HashMap tabGroups = new HashMap();
    private final HashMap customOptionControllers = new HashMap();

    private GenericOptionsPanel panel;

    public GenericOptionsPanelController(Icon icon, String categoryName) {
        this.icon = icon;
        this.categoryName = categoryName;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getCategoryName() {
        return categoryName;
    }

    /**
     * Added to take a tab name. So all options is in a tab
     */
    public void addOption(String tabName, String optionsGroupName, Option option) {
        synchronized (tabGroups) {
            Map optionGroups = (Map) tabGroups.get(tabName);
            if (optionGroups == null) {
                optionGroups = new HashMap();
                tabGroups.put(tabName, optionGroups);
            }
            List optionsList = (List) optionGroups.get(optionsGroupName);
            if (optionsList == null) {
                optionsList = new ArrayList();
                optionGroups.put(optionsGroupName, optionsList);
            }
            optionsList.add(option);
        }
    }

    /**
     * Added to take a tab name. So all options is in a tab
     */
    public void addOption(String tabName, String optionsGroupName, Option option, GenericOptionsPanel.OptionController controller) {
        addOption(tabName, optionsGroupName, option);
        synchronized (customOptionControllers) {
            customOptionControllers.put(option, controller);
        }
    }

    public GenericOptionsPanel.OptionController getOptionController(Option option) {
        GenericOptionsPanel.OptionController optionController;
        synchronized (customOptionControllers) {
            optionController = (GenericOptionsPanel.OptionController) customOptionControllers.get(option);
        }
        return optionController;
    }

    public String[] getOptionTabNames() {
        synchronized (tabGroups) {
            Set keySet = tabGroups.keySet();
            return (String[]) keySet.toArray(new String[keySet.size()]);
        }
    }

    public String[] getOptionGroupNames(String tabName) {
        synchronized (tabGroups) {
            Map groups = (Map) tabGroups.get(tabName);
            Set tabGroup = (Set) groups.keySet();
            return (String[]) tabGroup.toArray(new String[tabGroup.size()]);
        }
    }

    public Option[] getOptions(String tabName, String optionsGroupName) {
        synchronized (tabGroups) {
            Map groups = (Map) tabGroups.get(tabName);
            List options = (List) groups.get(optionsGroupName);
            return (Option[]) options.toArray(new Option[options.size()]);
        }
    }

    public void reload() {
        if (panel != null) getPanel().reload();
    }

    public void applyChanges() {
        if (panel != null) getPanel().applyChanges();
    }

    public void cancel() {
    }

    public void validate() throws ValidationException {
        // TODO implement
    }

    public JComponent getComponent() {
        return getPanel();
    }

    private GenericOptionsPanel getPanel() {
        if (panel == null) {
            panel = new GenericOptionsPanel(this);
        }
        return panel;
    }
}
