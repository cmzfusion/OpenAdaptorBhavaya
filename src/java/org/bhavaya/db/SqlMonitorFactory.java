package org.bhavaya.db;

import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

/**
 * Factory to load a SqlMonitor implementation
 * User: ga2mop0
 * Date: 02/10/14
 * Time: 11:47
 */
public class SqlMonitorFactory {
    private static final Log log = Log.getCategory(SqlMonitorFactory.class);
    private static SqlMonitor sqlMonitor = null;

    static {
        Class c = getClassValue(ApplicationProperties.getApplicationProperties(), "sqlMonitorClass");
        if(c != null) {
            try {
                sqlMonitor = (SqlMonitor)c.newInstance();
            } catch (Exception e) {
                log.error("Error instantiating FormulaFactory", e);
            }
        }
        if(sqlMonitor == null) {
            sqlMonitor = new NullSqlMonitor();
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

    public static SqlMonitor getSqlMonitorInstance() {
        return sqlMonitor;
    }

    private static class NullSqlMonitor implements SqlMonitor {
        @Override
        public void sqlExecuted() { }
    }

}
