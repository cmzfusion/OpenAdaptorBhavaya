package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.ListTable;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.util.KeyValuePair;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class SystemPropertiesDiagnosticContext extends DiagnosticContext {
    public SystemPropertiesDiagnosticContext(String name, ImageIcon icon) {
        super(name, icon);
    }

    public Component createComponent() {
        KeyValuePair[] systemProperties = getSystemProperties();
        ListTable table = new ListTable(new ListTable.CollectionListTableModel(Arrays.asList(systemProperties), new String[]{"key", "value"}, new String[]{"Property", "Value"}), new double[]{0.4});
        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    }

    public String createHTMLDescription() {
        StringBuffer buffer = new StringBuffer();
        DiagnosticUtilities.contextHeader(buffer, "System properties");

        DiagnosticUtilities.tableHeader(buffer);

        KeyValuePair[] systemProperties = getSystemProperties();
        for (int i = 0; i < systemProperties.length; i++) {
            KeyValuePair systemProperty = systemProperties[i];
            DiagnosticUtilities.tableRow(buffer, new Object[]{systemProperty.getKey(), systemProperty.getValue()});
        }

        DiagnosticUtilities.tableFooter(buffer);

        return buffer.toString();
    }

    public static KeyValuePair[] getSystemProperties() {
        Properties systemProperties = System.getProperties();
        Enumeration propertyNames = systemProperties.propertyNames();
        Collection propertyCollection = new TreeSet(); // sort by name
        while (propertyNames.hasMoreElements()) {
            String propertyName = (String) propertyNames.nextElement();
            propertyCollection.add(new KeyValuePair(propertyName, systemProperties.getProperty(propertyName)));
        }
        return (KeyValuePair[]) propertyCollection.toArray(new KeyValuePair[propertyCollection.size()]);
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }

    public MenuGroup[] createMenuGroups() {
        return new MenuGroup[0];
    }
}
