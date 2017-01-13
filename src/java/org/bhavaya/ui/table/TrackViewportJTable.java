package org.bhavaya.ui.table;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
* User: ebbuttn
* Date: 25-Jan-2008
* Time: 16:33:14
* To change this template use File | Settings | File Templates.
*
* This class is a workaround for the problem drag and dropping onto a target JTable which does not fill its scrollpane
* In jdk 1.6 there is built in support in jtable to correct this ( JTable.setFillsViewportHeight(boolean) )
* Unless we mandate 1.6 the workaround is to override JTable getScrollableTracksViewportHeight
*
* For more information see http://weblogs.java.net/blog/shan_man/archive/2006/01/enable_dropping.html
*/
public class TrackViewportJTable extends FixedForCustomRowHeightsJTable {

    public TrackViewportJTable(TableModel tableModel, TableColumnModel tableColumnModel) {
        super(tableModel, tableColumnModel);
    }

    public TrackViewportJTable(TableModel tableModel) {
        super(tableModel);
    }

    public boolean getScrollableTracksViewportHeight() {
        // fetch the table's parent
        Container viewport = getParent();

        // if the parent is not a viewport, calling this isn't useful
        if (! (viewport instanceof JViewport)) {
            return false;
        }

        // return true if the table's preferred height is smaller
        // than the viewport height, else false
        return getPreferredSize().height < viewport.getHeight();
    }
}
