package org.bhavaya.ui.table;

/**
 * Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 08-Oct-2008
* Time: 16:18:28
*/
public interface ColumnConfigFactory {

    /**
     * @return the ColumnConfig for the given columnKey, or null if no ColumnConfig exists
     */
    public ColumnConfig getColumnConfigForColumn(Object columnKey);
}
