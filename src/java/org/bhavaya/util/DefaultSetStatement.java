package org.bhavaya.util;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
public class DefaultSetStatement implements SetStatement {
    private String[] validBeanPaths;

    public DefaultSetStatement(String beanPath) {
        this(new String[]{beanPath});
    }

    public DefaultSetStatement(String[] validBeanPaths) {
        this.validBeanPaths = validBeanPaths;
    }

    public String[] getValidBeanPaths() {
        return validBeanPaths;
    }

    public void execute(Object target, String beanPath, Object propertyValue) {
        Generic.set(target, Generic.beanPathStringToArray(beanPath), propertyValue, false);
    }

    public boolean isSettable(Object target, String beanPath) {
        return true;
    }
}
