package org.bhavaya.ui.diagnostics;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import org.bhavaya.ui.MenuGroup;
import org.bhavaya.util.ThreadUtilities;

import javax.swing.*;
import java.awt.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.3 $
 */
public abstract class DiagnosticContext {
    public static class Attachment {
        private String name;
        private byte[] data;

        public Attachment(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }

        public String getName() {
            return name;
        }
    }

    private String name;
    private ImageIcon icon;

    private Mutex componentCreationMutex = new Mutex();
    private Component component;


    public DiagnosticContext(String name, ImageIcon icon) {
        this.name = name;
        this.icon = icon;
    }

    public Component getComponent() {
        ThreadUtilities.quietAquire(componentCreationMutex);
        if (component == null) {
            component = createComponent();
        }
        componentCreationMutex.release();
        return component;
    }

    public abstract Component createComponent();

    public abstract String createHTMLDescription();

    public ImageIcon getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public void update() {
    }

    public abstract MenuGroup[] createMenuGroups();

    public abstract Attachment[] createAttachments();

    public Object createMBean() {
        return null;
    }

    public String getMBeanIdentifier() {
        return null;
    }
}
