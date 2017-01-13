package org.bhavaya.ui.options;

import org.bhavaya.util.Log;

import java.util.ArrayList;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class OptionsController {
    private static final Log log = Log.getCategory(OptionsController.class);

    private static OptionsController instance;

    public static synchronized OptionsController getInstance() {
        if (instance == null) {
            instance = new OptionsController();
        }
        return instance;
    }

    private final ArrayList optionPanelControllers = new ArrayList();

    private OptionsController() {
    }

    public void addOptionsPanelController(String controllerClass) {
        try {
            addOptionsPanelController((OptionsPanelController)Class.forName(controllerClass).newInstance());
        } catch (Exception e) {
            log.error(e);
        }
    }

    public void addOptionsPanelController(OptionsPanelController controller) {
        synchronized(optionPanelControllers) {
            optionPanelControllers.add(controller);
        }
    }

    OptionsPanelController[] getOptionsPanelControllers() {
        synchronized (optionPanelControllers) {
            return (OptionsPanelController[]) optionPanelControllers.toArray(new OptionsPanelController[optionPanelControllers.size()]);
        }
    }
}
