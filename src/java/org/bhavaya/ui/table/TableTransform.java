package org.bhavaya.ui.table;

import javax.swing.table.TableModel;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author Daniel van Enckevort
 * @version $Revision: 1.1 $
 */
public interface TableTransform {
    int mapSourceRowToRow(int sourceRow);

    int mapSourceColumnToColumn(int sourceColumn);

    TableModel getModel();

//todo: do we want this?
//    TableModel getSourceModel();
}
