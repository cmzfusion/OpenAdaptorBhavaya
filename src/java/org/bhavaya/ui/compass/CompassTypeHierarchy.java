package org.bhavaya.ui.compass;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mcleanb
 * Date: 06-Apr-2004
 * Time: 10:48:20
 * To change this template use File | Settings | File Templates.
 */
public abstract class CompassTypeHierarchy {
    public static class Result {
        private Object key;
        private String description;

        public Result(Object key, String description) {
            this.key = key;
            this.description = description;
        }

        public Object getKey() {
            return key;
        }

        public String getDescription() {
            return description;
        }

        public String toString() {
            return description;
        }
    }

    private CompassTaxonomyDefinition compassTaxonomyDefinition;
    private CompassTypeHierarchy nextType;
    private CompassTypeHierarchy prevType;
    private String name;
    private Class type;
    private Set keysInHierarchyCache = new HashSet();
    private Set keysNotInHierarchyCache = new HashSet();

    public CompassTypeHierarchy(String name, Class type) {
        this.name = name;
        this.type = type;
    }

    public CompassTypeHierarchy setNextType(CompassTypeHierarchy nextType) {
        this.nextType = nextType;
        nextType.setPrevType(this);
        return this;
    }

    private CompassTypeHierarchy setPrevType(CompassTypeHierarchy prevType) {
        this.prevType = prevType;
        return this;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

//    public abstract Object getKey(Object o);

//    public abstract String getDescription(Object o);

    /**
     * @param key object/dbkey of object/row of parent object.
     * @return a Collection&lt;Result&gt; of children that have parent of <code>key</code>
     */
    public abstract List getChildrenOfThisType(Object key);

    /**
     * @param key object/dbkey of object/row of parent object.
     * @return a Collection&lt;Result&gt; of children that have parent of <code>key</code>
     */
    public abstract List getChildrenOfNextType(Object key);

    public CompassTypeHierarchy getNextType() {
        return nextType;
    }

    public CompassTypeHierarchy getPrevType() {
        return prevType;
    }

    /**
     * @param searchString text to search for.
     * @return a Collection&lt;Result&gt;
     */
    public abstract List findMatchesForString(String searchString);

    public abstract Object findParentKeyOfThisType(Object key);

    public abstract Object findParentKeyOfNextType(Object key);

    /**
     *
     * @return the Boolean value to be returned if a node is queried about its leaf status. null means do not override
     * but perform the "getChildCount" calculation (that may destroy laziness)
     */
    public abstract Boolean getNodeIsLeafOverride();

    protected void setCompassTaxonomyDefinition(CompassTaxonomyDefinition compassTaxonomyDefinition) {
        this.compassTaxonomyDefinition = compassTaxonomyDefinition;
        if (nextType != null) nextType.setCompassTaxonomyDefinition(this.compassTaxonomyDefinition);
    }

    public CompassTaxonomyDefinition getCompassTaxonomyDefinition() {
        return compassTaxonomyDefinition;
    }

    public boolean isKeyWithinTaxonomy(Object taxonomyRootKey, Object key) {
        if (keysInHierarchyCache.contains(key)) return true;
        if (keysNotInHierarchyCache.contains(key)) return false;

        if (key.equals(taxonomyRootKey)) {
            return true;
        } else {
            Object parent = findParentKeyOfThisType(key);
            if (parent != null) {
                boolean result = isKeyWithinTaxonomy(taxonomyRootKey, parent);
                if (result) {
                    keysInHierarchyCache.add(key);
                } else {
                    keysNotInHierarchyCache.add(key);
                }
                return result;
            } else {
                if (getPrevType() == null) {
                    return false;
                } else {
                    parent = getPrevType().findParentKeyOfNextType(key);
                    if (parent != null) {
                        boolean result = getPrevType().isKeyWithinTaxonomy(taxonomyRootKey, parent);
                        if (result) {
                            keysInHierarchyCache.add(key);
                        } else {
                            keysNotInHierarchyCache.add(key);
                        }
                        return result;
                    } else {
                        return false;
                    }
                }
            }
        }
    }
}
