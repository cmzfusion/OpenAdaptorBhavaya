package org.bhavaya.ui;

import javax.swing.tree.DefaultTreeCellRenderer;
import java.util.HashMap;
import java.util.Map;

/**
 * Dramaticaly improves the speed of cell rendering when an HTML is used. It caches the view object to save the time.
 * Be aware that it newer removes an object from the cache.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public class FastHTMLCellRenderer extends DefaultTreeCellRenderer {

    private Map views = new HashMap();

    public void setText(String text) {
        if (text != null && text.startsWith("<html>")) {
            Object view = views.get(text);
            if (view == null) {
                // generate the html for the value
                super.setText(text);
                views.put(text, getClientProperty("html"));
            } else {
                putClientProperty("html", view);
            }
        } else {
            super.setText(text);
        }
    }
}

