package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.table.BeanFormTableModel;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Observable;

import javax.swing.*;
import java.awt.*;

/**
 * Exposes a Bean's properties as both a visual table and a report table.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class BeanInfoDiagnosticContext extends DiagnosticContext {
    private Observable bean;
    private String[] descriptions;
    private String[] propertyNames;

    public BeanInfoDiagnosticContext(String name, ImageIcon icon, Observable bean, String[] descriptions, String[] propertyNames) {
        super(name, icon);
        this.bean = bean;
        this.descriptions = descriptions;
        this.propertyNames = propertyNames;
        assert descriptions.length == propertyNames.length;
    }

    public Component createComponent() {
        BeanFormTableModel tableModel = new BeanFormTableModel(bean, propertyNames, descriptions);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    }

    public String createHTMLDescription() {
        StringBuffer buffer = new StringBuffer();
        DiagnosticUtilities.contextHeader(buffer, getName());
        DiagnosticUtilities.tableHeader(buffer);

        for (int i = 0; i < descriptions.length; i++) {
            String description = descriptions[i];
            String propertyName = propertyNames[i];
            DiagnosticUtilities.tableRow(buffer, new Object[]{description, Generic.get(bean, propertyName)});
        }

        DiagnosticUtilities.tableFooter(buffer);

        return buffer.toString();
    }

    public MenuGroup[] createMenuGroups() {
        return new MenuGroup[0];
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }
}
