package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.MenuGroup;
import org.bhavaya.util.ClassUtilities;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author Andrew J. Dean
 * @version $Revision: 1.2 $
 */
public class MBeanOnlyDiagnosticContext extends DiagnosticContext {

    private Object mBean;
    private String mBeanIdentifer;

    public MBeanOnlyDiagnosticContext(Object mBean) {
        this(mBean, null);
    }

    public MBeanOnlyDiagnosticContext(Object mBean, String mBeanIdentifier) {
        super(ClassUtilities.getUnqualifiedClassName(mBean.getClass()), null);
        this.mBean = mBean;
        this.mBeanIdentifer = mBeanIdentifier;
    }

    public Component createComponent() {
        return null;
    }

    public String createHTMLDescription() {
        return null;
    }

    public MenuGroup[] createMenuGroups() {
        return new MenuGroup[0];
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }

    public Object createMBean() {
        return mBean;
    }

    public String getMBeanIdentifier() {
        return mBeanIdentifer;
    }
}
