package org.bhavaya.ui.adaptor;

import org.bhavaya.util.Observable;
import org.bhavaya.util.Transform;

import javax.swing.*;


/**
 * This Adapter prevent that any other thread than the EventDispatchThread is updating the gui.
 * @author <a href="mailto:Sabine.Haas@drkw.com">Sabine Haas, Dresdner Kleinwort Wasserstein</a>
 * @version $Revision: 1.3 $
 * @deprecated this functionality has been built into the Adapter class, so you can safely use that one now.
 */
public class AWTAdapter extends Adapter {

    public AWTAdapter(Observable bean) {
        super(bean);
    }

    protected Pipe createBeanToGuiPipe(String propertyName, Object guiComponent, String guiSetterProperty, Transform guiSetterTransform) {
        return new AWTPropertyPipe(super.getBean(), propertyName, guiComponent, guiSetterProperty, guiSetterTransform);
    }
}
