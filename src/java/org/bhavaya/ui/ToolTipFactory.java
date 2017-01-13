package org.bhavaya.ui;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.*;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public interface ToolTipFactory {

    public String getToolTipText(MouseEvent event);

    public Point getToolTipLocation(MouseEvent event);

    public JToolTip createToolTip();

}
