package org.bhavaya.ui.diagnostics;

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.AuditedAbstractAction;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Set;

/**
 * Provides reports on BeanFactory listener counts.  This helps check for bean-related memory leaks.
 *
 * @author Brendon McLean
 * @version $Revision: 1.7 $
 */
public class BeanFactoryDiagnosticContext extends DiagnosticContext {
    private static final Log log = Log.getCategory(BeanFactoryDiagnosticContext.class);

    public BeanFactoryDiagnosticContext() {
        super("Bean Factory Statistics", null);
    }

    public Component createComponent() {
        return null;
    }

    public String createHTMLDescription() {
        StringBuffer buffer = new StringBuffer();
        DiagnosticUtilities.contextHeader(buffer, "Bean Factories");
        DiagnosticUtilities.tableHeader(buffer);

        DiagnosticUtilities.tableHeaderRow(buffer, new Object[]{"Type", "Bean Count", "Listener Count"});

        BeanFactoryStatistics[] beanFactoriesStatistics = getBeanFactoriesStatistics();
        for (int i = 0; i < beanFactoriesStatistics.length; i++) {
            BeanFactoryStatistics beanFactoryStatistics = beanFactoriesStatistics[i];
            if (beanFactoryStatistics.getBeanCount() > 0 || beanFactoryStatistics.getListenerCount() > 0) {
                DiagnosticUtilities.tableRow(buffer, new Object[]{beanFactoryStatistics.getType(),
                                                                  "" + beanFactoryStatistics.getBeanCount(),
                                                                  "" + beanFactoryStatistics.getListenerCount()});
            }
        }
        DiagnosticUtilities.tableFooter(buffer);
        return buffer.toString();
    }

    public MenuGroup[] createMenuGroups() {
        MenuGroup settingsMenuGroup = new MenuGroup("Options", KeyEvent.VK_O);

        settingsMenuGroup.addElement(new MenuGroup.MenuItemElement(new JMenuItem(new GarbageCollectionAction())));
        return new MenuGroup[]{settingsMenuGroup};
    }

    public static BeanFactoryStatistics[] getBeanFactoriesStatistics() {
        int totalSize = 0;
        int totalListeners = 0;
        org.bhavaya.beans.BeanFactory[] beanFactories = org.bhavaya.beans.BeanFactory.getInstances();
        BeanFactoryStatistics[] beanFactoriesStatistics = new BeanFactoryStatistics[beanFactories.length + 1];
        for (int i = 0; i < beanFactories.length; i++) {
            org.bhavaya.beans.BeanFactory beanFactory = beanFactories[i];
            BeanFactoryStatistics stats = createStats(beanFactory);
            beanFactoriesStatistics[i] = stats;

            totalSize += stats.getBeanCount();
            totalListeners += stats.getListenerCount();
        }
        beanFactoriesStatistics[beanFactoriesStatistics.length - 1] = new BeanFactoryStatistics("Total", totalSize, totalListeners);
        return beanFactoriesStatistics;
    }

    private static BeanFactoryStatistics createStats(BeanFactory beanFactory) {
        String type = beanFactory.getType().getName();
        boolean classLoaded = beanFactory.isClassLoaded();
        int size = classLoaded ? beanFactory.size() : -1;
        int listeners = classLoaded ? beanFactory.getMapListenerCount() : -1;
        return new BeanFactoryStatistics(type, size, listeners);
    }

    public static class BeanFactoryStatistics {
        private String type;
        private int beanCount;
        private int listenerCount;

        public BeanFactoryStatistics(String type, int beanCount, int listenerCount) {
            this.type = type;
            this.beanCount = beanCount;
            this.listenerCount = listenerCount;
        }

        public String getType() {
            return type;
        }

        public int getBeanCount() {
            return beanCount;
        }

        public int getListenerCount() {
            return listenerCount;
        }

        public String toString() {
            return "Type: " + type + ", beanCount: " + beanCount + ", listenerCount: " + listenerCount;
        }
    }

    private class GarbageCollectionAction extends AuditedAbstractAction {
        public GarbageCollectionAction() {
            putValue(Action.NAME, "Garbage Collect");
        }

        public void auditedActionPerformed(ActionEvent e) {
            // logging stats has an important side-effect, it touches all beanFactories,
            // which triggers any weak referenced beans to be removed (it is a lazy operation)
            logBeanFactoryStatistics();
            System.gc();
        }

        private void logBeanFactoryStatistics() {
            StringBuffer buffer = new StringBuffer("");
            buffer.append("BeanFactory statistics\n");
            buffer.append("Type\tBean Count\tListener Count\n");

            BeanFactoryStatistics[] beanFactoriesStatistics = getBeanFactoriesStatistics();

            for (int i = 0; i < beanFactoriesStatistics.length; i++) {
                BeanFactoryStatistics beanFactoryStatistics = beanFactoriesStatistics[i];
                if (beanFactoryStatistics.getBeanCount() > 0 || beanFactoryStatistics.getListenerCount() > 0) {
                    buffer.append(beanFactoryStatistics.getType() + "\t" + beanFactoryStatistics.getBeanCount() + "\t"
                            + beanFactoryStatistics.getListenerCount() + "\n");
                }

            }
            log.info(buffer.toString());
        }
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }

    public Object createMBean() {
        return new BeanFactoryReport();
    }

    public static class BeanFactoryReport implements BeanFactoryReportMBean {

        public String viewBeans(String beanClassName) {
            return generateBeanContentsReport(beanClassName, null, null, null);
        }

        public String viewBeansWithProperty(String beanClassName, String propertyName) {
            return generateBeanContentsReport(beanClassName, null, propertyName, null);
        }

        public String findBeansByKey(String beanClassName, String keySubstring) {
            return generateBeanContentsReport(beanClassName, keySubstring, null, null);
        }

        public String findBeansByPropertyNameAndValue(String beanClassName, String propertyName, String propertyValue) {
            return generateBeanContentsReport(beanClassName, null, propertyName, propertyValue);
        }

        private String generateBeanContentsReport(String beanClassName, String keySubstring, String propertyName, String searchValue) {
            String result;
            try {
                BeanFactory beanFactory = BeanFactory.getInstance(Class.forName(beanClassName));
                BeanFactoryStatistics s = createStats(beanFactory);
                StringBuffer sb = new StringBuffer();
                sb.append("<html><p>");
                sb.append(s);
                sb.append("<p>");
                DiagnosticUtilities.tableHeader(sb);

                sb.append("<tr>");
                sb.append("<th>Key:</th>");
                sb.append("<th>toString():</th>");

                //value for only a single property is supported
                //introspecting for all the properties would not be a good idea, since accessing a property value
                //may traverse a foreign key relationship and start to trigger sql loads
                //we can probably allow this for a single property, but would not be a good idea to do this for every
                //property of a bean
                if ( propertyName != null) {
                    sb.append("<th>").append(propertyName).append("</th>");
                }
                sb.append("</tr>");

                synchronized(beanFactory.getLock()) {
                    for ( Map.Entry e : (Set<Map.Entry>)beanFactory.entrySet()) {

                        Object key = e.getKey();
                        Object bean = e.getValue();

                        if (isSearchByPropertyValue(propertyName, searchValue)) {
                            boolean match = matchesPropertyValue(propertyName, searchValue, bean);
                            if ( ! match) {
                                continue;
                            }
                        }

                        if ( keySubstring != null) {
                            boolean match = matchesKeySubstring(keySubstring, key);
                            if ( ! match) {
                                continue;
                            }
                        }

                        sb.append("<tr>");
                        sb.append("<td>").append(key).append("</td>");
                        //sometimes the bean for a key has not yet been loaded
                        sb.append("<td>").append(bean != null ? bean.toString() : "Not Loaded").append("</td>");
                        if ( propertyName != null ) {
                            sb.append("<td>").append(bean != null ? Generic.getBeanValueIfExists(bean, propertyName) : "Not Loaded").append("</td>");
                        }
                        sb.append("</tr>");
                    }
                }
                DiagnosticUtilities.tableFooter(sb);
                sb.append("</html>");
                result = sb.toString();
            } catch (RuntimeException e) {
                result = "RuntimeException generating report " + e;
                log.error("RuntimeException generating report ", e);
            } catch (ClassNotFoundException e) {
                result = "Could not find a BeanFactory for class " + beanClassName;
            }
            return result;
        }

        private boolean matchesKeySubstring(String keySubstring, Object keyForValue) {
            return keyForValue != null && ! keyForValue.toString().contains(keySubstring);
        }

        private boolean isSearchByPropertyValue(String propertyName, String searchValue) {
            return propertyName != null && searchValue != null;
        }

        private boolean matchesPropertyValue(String propertyName, String value, Object bean) {
            boolean match = false;
            if ( bean != null) {
                Object beanValue = Generic.getBeanValueIfExists(bean, propertyName);
                match = (beanValue == null) ? value.equals("null") : beanValue.toString().trim().equals(value);
            }
            return match;
        }
    }

    public static interface BeanFactoryReportMBean {

        /**
         * propertyName can be a bean path
         */
        String findBeansByPropertyNameAndValue(String beanClassName, String propertyName, String value);

        String findBeansByKey(String beanClassName, String keySubstring);

        String viewBeans(String beanClassName);

        /**
         * propertyName can be a bean path
         */
        String viewBeansWithProperty(String beanClassName, String propertyName);

    }
}
