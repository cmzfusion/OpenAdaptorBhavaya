package org.bhavaya.ui.view.diagnostics;

/**
 * MBean to interrogate views for a workspace
 * User: ga2mhana
 * Date: 28/02/11
 * Time: 09:58
 */
public interface ViewDiagnosticsMBean {

    /**
     * Return HTML representation of the open views
     * @param worthlessParameter - required to make jmx render the returned string as a separate page (which lets us add html markup)
     * @return HTML description of views.
     */
    String getViewStats(String worthlessParameter);
}
