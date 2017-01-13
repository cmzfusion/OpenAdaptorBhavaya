package org.bhavaya.ui.view.diagnostics;

import org.bhavaya.ui.MenuGroup;
import org.bhavaya.ui.diagnostics.DiagnosticContext;

import java.awt.*;

/**
 * View diagnostic context.Provides the MBean to interrogate views
 * User: ga2mhana
 * Date: 28/02/11
 * Time: 09:56
 */
public class ViewDiagnosticContext extends DiagnosticContext {

    private ViewDiagnostics viewDiagnostics = new ViewDiagnostics();

    public ViewDiagnosticContext() {
        super("View Diagnostics", null);
    }

    public Object createMBean() {
        return viewDiagnostics;
    }

    public String createHTMLDescription() {
        return viewDiagnostics.getViewStats(null);
    }

    public Component createComponent() {
        return null;
    }

    public MenuGroup[] createMenuGroups() {
        return new MenuGroup[0];
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }
}
