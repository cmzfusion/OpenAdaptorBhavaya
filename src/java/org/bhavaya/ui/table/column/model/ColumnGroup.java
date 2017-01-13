package org.bhavaya.ui.table.column.model;

import org.bhavaya.util.DefaultObservable;
import org.bhavaya.util.Utilities;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 24-Sep-2009
 * Time: 15:24:05
 *
 * A bean representing a column group
 * Each HidableTableColumn may be associated with a ColumnGroup instance.
 *
 * The ColumnHidingColumnModel provides logic to show or hide columns which are members of a group.
 */
public class ColumnGroup extends DefaultObservable implements Comparable {

    private String groupName;
    private boolean isHidden;

    //need this for reflective instantiation
    public ColumnGroup() {
    }

    public ColumnGroup(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        if ( hidden != this.isHidden) {
            isHidden = hidden;
            firePropertyChange("hidden", ! isHidden, hidden);
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColumnGroup that = (ColumnGroup) o;

        if (groupName != null ? !groupName.equals(that.groupName) : that.groupName != null) return false;

        return true;
    }

    public int hashCode() {
        int result = groupName != null ? groupName.hashCode() : 0;
        result = 31 * result + (isHidden ? 1 : 0);
        return result;
    }

    public int compareTo(Object o) {
        return groupName.compareTo(((ColumnGroup)o).groupName);
    }

    public String toString() {
        return groupName;
    }

    /**
     * @return true if groups are equal (or both null), including the isHidden state of the group
     */
    public static boolean groupsAreEqualForPersistence(ColumnGroup g1, ColumnGroup g2) {
        boolean equal = Utilities.equals(g1, g2);
        if ( equal && g1 != null) {
            equal = g1.isHidden == g2.isHidden;
        }
        return equal;
    }
}
