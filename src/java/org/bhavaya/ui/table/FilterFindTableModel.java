package org.bhavaya.ui.table;

import com.od.filtertable.IndexedTableModel;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 02-Sep-2009
 * Time: 12:09:31
 *
 * Expose the methods from IndexedTableModel which may need rowIndex/colIndex translated by the sort etc.
 */
public interface FilterFindTableModel extends KeyedColumnTableModel, IndexedTableModel {

    /**
     *  Typically we want to highlight all search matches, this method true if the cell is matched to the current search term
     */
    boolean isCellMatchingSearch(int rowIndex, int colIndex);

}
