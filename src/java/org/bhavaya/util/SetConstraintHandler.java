package org.bhavaya.util;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * @author <a href="mailto:Sabine.Haas@dkib.com">Sabine Haas, Dresdner Kleinwort</a>
 */
public class SetConstraintHandler {

    private static String SET_CONSTRAINTS_HANDLER_SAVE_KEY = "SetConstraintHandler";
    private static HashMap beanTypeToSetConstraintMap;

    static {
        BeanUtilities.addPersistenceDelegate(SetConstraintHandler.class, new BhavayaPersistenceDelegate(new String[]{"beanPathToSetConstraints",}));

        // load/save set constraints defined by user
        Configuration.addSaveTask(new Task("Saving bean path set constraints") {
            public void run() {
                Configuration configuration = Configuration.getRoot();
                configuration.putObject(SET_CONSTRAINTS_HANDLER_SAVE_KEY, beanTypeToSetConstraintMap);
            }
        });
    }


    private HashMap beanPathToSetConstraints = new HashMap();

    public static SetConstraintHandler getInstance(Class beanType) {
        if (beanTypeToSetConstraintMap == null) loadConfiguration();
        SetConstraintHandler handler = (SetConstraintHandler) beanTypeToSetConstraintMap.get(beanType);
        if (handler == null) {
            handler = new SetConstraintHandler(new HashMap());
            beanTypeToSetConstraintMap.put(beanType, handler);
        }
        return handler;
    }

    public static void loadConfiguration() {
        Configuration configuration = Configuration.getRoot();
        beanTypeToSetConstraintMap = configuration.getObject(SET_CONSTRAINTS_HANDLER_SAVE_KEY, new HashMap(), HashMap.class);
    }

    public SetConstraintHandler(HashMap beanPathToSetConstraints) {
        this.beanPathToSetConstraints = beanPathToSetConstraints;
    }

    public void addColumnSetConstraint(String beanPath, SetConstraint setConstraint) {
        beanPathToSetConstraints.put(beanPath, setConstraint);
    }

    public void removeColumnSetConstraint(String beanPath) {
        beanPathToSetConstraints.remove(beanPath);
    }

    public SetConstraint getColumnSetConstraint(Object beanPath) {
        return (SetConstraint) beanPathToSetConstraints.get(beanPath);
    }

    public boolean validate(Object bean, String beanPath, Object value) {
        SetConstraint setConstraint = (SetConstraint) beanPathToSetConstraints.get(beanPath);
        if (setConstraint != null) {
            String[] beanPathArray = Generic.beanPathStringToArray(beanPath);
            Object oldValue = Generic.get(bean, beanPathArray);
            if (setConstraint.constraintViolated(oldValue, value)) {
                Window parentComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
                String propetyName = beanPathArray[beanPathArray.length - 1];
                String message = Utilities.capitalise(propetyName) + ": Set contraint broken [" + setConstraint.getErrorMessage(oldValue, value) + "]: \n Do you want to continue?";
                if (JOptionPane.showConfirmDialog(parentComponent, message, "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return false;
            }
        }
        return true;
    }

    // for the persistence layer       
    @PropertyMetaData(hidden = true)
    public HashMap getBeanPathToSetConstraints() {
        return beanPathToSetConstraints;
    }

    public void setBeanPathToSetConstraints(HashMap beanPathToSetConstraints) {
        if (beanPathToSetConstraints != null) {
            this.beanPathToSetConstraints = beanPathToSetConstraints;
        }
    }
}
