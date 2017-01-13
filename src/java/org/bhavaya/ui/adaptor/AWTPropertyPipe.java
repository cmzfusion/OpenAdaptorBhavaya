package org.bhavaya.ui.adaptor;

import org.bhavaya.util.Transform;

import java.awt.*;

/**
 * This Pipe updates GUI components on EDT thread. Use this pipe to bind bean property to GUI component
 * not the other way round as all the GUI notifications are anyway triggered on EDT.
 * Only the 'sink' part is executed on EDT to prevent data loading on EDT.
 *
 * @author Andrew J. Dean and James Langley
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public class AWTPropertyPipe extends PropertyPipe {

    public AWTPropertyPipe(Object source, String sourceProperty, Object sink, String sinkProperty, Transform transform) {
        super(new PropertySource(source, sourceProperty), new AWTPropertySink(sink, sinkProperty), transform, false);
    }

    public static class AWTPropertySink extends PropertySink {
        public AWTPropertySink(Object instance, String property) {
            super(instance, property);
        }

        public void setData(final Object data) {
            if (EventQueue.isDispatchThread()) {
                super.setData(data);
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        AWTPropertySink.super.setData(data);
                    }
                });
            }
        }
    }
}