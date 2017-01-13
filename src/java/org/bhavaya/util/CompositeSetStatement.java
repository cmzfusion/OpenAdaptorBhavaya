package org.bhavaya.util;

import org.bhavaya.util.SetStatement;

import java.util.List;
import java.util.ArrayList;

/**
 * Composite pattern for a Set statement. Can be used when you want to use different set statements
 * of a bean path.
 * User: dhayatd
 * Date: 21-Sep-2009
 * Time: 14:00:37
 */
public class CompositeSetStatement implements SetStatement {

    private SetStatement[] setStatements = null;

    public CompositeSetStatement(SetStatement... newStatements) {
        this.setStatements = newStatements;
    }

    public void execute(Object target, String beanPath, Object propertyValue) {
        for (SetStatement statement : setStatements) {
            if (statement.isSettable(target, beanPath)) {
                statement.execute(target, beanPath, propertyValue);
            }
        }
    }

    public String[] getValidBeanPaths() {
        List<String> vaildBeanPath = new ArrayList<String>();
        for (SetStatement statement : setStatements) {
            vaildBeanPath.add(statement.getValidBeanPaths()[0]);
        }
        return vaildBeanPath.toArray(new String[vaildBeanPath.size()]);
    }

    public boolean isSettable(Object target, String beanPath) {
        for (SetStatement statement : setStatements) {
            if (statement.isSettable(target, beanPath)) {
                return true;
            }
        }
        return false;
    }

    public SetStatement[] getCompositeStatements() {
        return setStatements;
    }
}
