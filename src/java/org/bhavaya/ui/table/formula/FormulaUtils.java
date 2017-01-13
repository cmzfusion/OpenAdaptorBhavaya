package org.bhavaya.ui.table.formula;

import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

/**
 * Set of static utility methods for Formula.
 * Details about formula, such as factory implementation, formula recalc interval etc, are initialised from the application properties.
 * User: ga2mhana
 * Date: 11/02/11
 * Time: 08:34
 */
public class FormulaUtils {
    private static final Log log = Log.getCategory(FormulaUtils.class);
    private static FormulaFactory factoryInstance = null;
    private static FormulaMonitor formulaMonitorInstance = null;
    private static final int DEFAULT_FORMULA_RECALC_INTERVAL = 500;
    private static final int DEFAULT_FORMULA_RECALC_THREAD_POOL_SIZE = 3;
    private static int formulaRecalcInterval;
    private static int formulaRecalcThreadPoolSize;
    private static final String FORMULA_PREFIX = "#FORMULA#";

    static {
        PropertyGroup group = ApplicationProperties.getApplicationProperties().getGroup("formula");
        if(group != null) {
            String property = group.getProperty("enabled");
            if("true".equals(property)) {
                Class c = getClassValue(group, "formulaFactoryClass");
                if(c != null) {
                    try {
                        factoryInstance = (FormulaFactory)c.newInstance();
                    } catch (Exception e) {
                        log.error("Error instantiating FormulaFactory", e);
                    }
                }
                c = getClassValue(group, "formulaMonitorClass");
                if(c != null) {
                    try {
                        formulaMonitorInstance = (FormulaMonitor)c.newInstance();
                    } catch (Exception e) {
                        log.error("Error instantiating FormulaMonitor", e);
                    }
                }
                formulaRecalcInterval = getIntValue(group, "formulaRecalcInterval", DEFAULT_FORMULA_RECALC_INTERVAL);
                formulaRecalcThreadPoolSize = getIntValue(group, "formulaRecalcThreadPoolSize", DEFAULT_FORMULA_RECALC_THREAD_POOL_SIZE);
            }
        }
    }

    private static Class getClassValue(PropertyGroup group, String propertyName) {
        String propertyValue = group.getProperty(propertyName);
        Class result = null;
        if(propertyValue != null) {
            try {
                result = Class.forName(propertyValue);
            } catch (Exception e) {
                log.warn("Invalid class property "+propertyName);
            }
        } else {
            log.info("No property "+propertyName+" defined");
        }
        return result;
    }

    private static int getIntValue(PropertyGroup group, String propertyName, int defaultValue) {
        String propertyValue = group.getProperty(propertyName);
        int result = defaultValue;
        if(propertyValue != null) {
            try {
                result = Integer.parseInt(propertyValue);
            } catch (Exception e) {
                log.info("Invalid property "+propertyName+" - using default "+defaultValue);
            }
        } else {
            log.info("No property "+propertyName+" - using default "+defaultValue);
        }
        return result;
    }

    private FormulaUtils() {}

    public static String getColumnLocator(Formula formula) {
        return FORMULA_PREFIX+"."+formula.getName();
    }

    public static boolean formulasEnabled() {
        return factoryInstance != null;
    }

    public static FormulaFactory getFactoryInstance() {
        return factoryInstance;
    }

    public static FormulaMonitor getFormulaMonitorInstance() {
        return formulaMonitorInstance;
    }

    public static boolean isFormulaPath(String propertyPath) {
        return isFormulaPath(Generic.beanPathStringToArray(propertyPath));
    }

    public static boolean isFormulaPath(String[] propertyPath) {
        return propertyPath != null &&
                propertyPath.length >= 2 && //just in case there's a "." in the name
                FORMULA_PREFIX.equals(propertyPath[0]);
    }

    public static String getFormulaNameFromLocator(String[] propertyPathArray) {
        String result = null;
        if(isFormulaPath(propertyPathArray)) {
            if(propertyPathArray.length == 2) {
                result = propertyPathArray[1];
            } else {
                //just in case there's a "." in the name
                String[] nameArray = new String[propertyPathArray.length-1];
                System.arraycopy(propertyPathArray, 1, nameArray, 0, nameArray.length);
                result = Generic.beanPathArrayToString(nameArray);
            }
        }
        return result;
    }

    public static String getFormulaNameFromLocator(String propertyPath) {
        return getFormulaNameFromLocator(Generic.beanPathStringToArray(propertyPath));
    }

    public static String getDisplayNameForPropertyPath(String propertyPath) {
        return "Formula - "+getFormulaNameFromLocator(propertyPath);
    }

    public static int getFormulaRecalcInterval() {
        return formulaRecalcInterval;
    }

    public static int getFormulaRecalcThreadPoolSize() {
        return formulaRecalcThreadPoolSize;
    }
}
