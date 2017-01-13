package org.bhavaya.ui.freechart;

/**
 * Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 26-Feb-2008
* Time: 16:30:52
*/
public class InvalidColumnException extends Exception {
    private String columnName;

    public InvalidColumnException(String columnName) {
        super("Could not find column " + columnName);
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }
}
