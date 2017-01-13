package org.bhavaya.ui.diagnostics;

import org.bhavaya.db.SqlBeanFactory;
import org.bhavaya.ui.MenuGroup;

import java.awt.*;

/**
 * Allows users to force a reconnect to the Notification Server
 * User: dhayatd
 * Date: 12-Jun-2008
 * Time: 10:45:52
 */
public class ReconnectNotificationServerDiagnosticContext extends DiagnosticContext {

    public ReconnectNotificationServerDiagnosticContext() {
        super("Reconnect to the Notification server", null);
    }

    public Component createComponent() {
        return null;
    }

    public String createHTMLDescription() {
        StringBuffer buffer = new StringBuffer();
        DiagnosticUtilities.contextHeader(buffer, "Reconnect to the Notification server");
        return buffer.toString();
    }

    public MenuGroup[] createMenuGroups() {
        return new MenuGroup[0];
    }

    public DiagnosticContext.Attachment[] createAttachments() {
        return new DiagnosticContext.Attachment[0];
    }

    public Object createMBean() {
        return new ForceReconnectToNotificationServer();
    }

    public interface ForceReconnectToNotificationServerMBean {
        public void forceReconnect();
    }

    public class ForceReconnectToNotificationServer implements ForceReconnectToNotificationServerMBean {
        public void forceReconnect() {
            SqlBeanFactory.forceReconnectToNotificationServer();
        }
    }
}
