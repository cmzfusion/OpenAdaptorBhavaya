package org.bhavaya.ui.compass;

import org.bhavaya.ui.compass.CompassTypeHierarchy;

/**
 * Defines a simple Taxonomy relationship.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class CompassTaxonomyDefinition {
    private Object rootParentKey;
    private CompassTypeHierarchy sqlTypeHierarchy;
    private String name;
    private String icon;

    public CompassTaxonomyDefinition(String name, String icon, Object rootParentKey, CompassTypeHierarchy sqlTypeHierarchy) {
        this.name = name;
        this.icon = icon;
        this.rootParentKey = rootParentKey;
        this.sqlTypeHierarchy = sqlTypeHierarchy;
        this.sqlTypeHierarchy.setCompassTaxonomyDefinition(this);
    }

    public CompassTypeHierarchy getTypeHierarchy() {
        return sqlTypeHierarchy;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public Object getRootParentKey() {
        return rootParentKey;
    }

    public String toString() {
        return getName();
    }
}
