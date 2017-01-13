package org.bhavaya.ui.table.formula.conditionalhighlight;

import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import java.util.*;

/**
 * Set of columns that should be excluded from conditional highlighting, grouped by view class.
 * User: ga2mhana
 * Date: 18/03/11
 * Time: 15:37
 */
public class ConditionalHighlightColumnExclusionMap {
    private static final Log log = Log.getCategory(ConditionalHighlightColumnExclusionMap.class);
    private static ConditionalHighlightColumnExclusionMap instance = new ConditionalHighlightColumnExclusionMap();
    private Map<Class, ConditionalHighlightColumnExclusions> exclusionMap = new HashMap<Class, ConditionalHighlightColumnExclusions>();

    static {
        PropertyGroup group = ApplicationProperties.getApplicationProperties().getGroup("conditionalHighlightColumnExclusions");
        if(group != null) {
            PropertyGroup[] exclusionGroups = group.getGroups("exclusion");
            for(PropertyGroup exclusionGroup : exclusionGroups) {
                Class c = getClassValue(exclusionGroup, "tableViewClass");
                if(c != null) {
                    ConditionalHighlightColumnExclusions exclusions = new ConditionalHighlightColumnExclusions();
                    instance.exclusionMap.put(c, exclusions);
                    boolean excludeAll = getBoolean(exclusionGroup, "allColumnPaths");
                    if(excludeAll) {
                        exclusions.setExcludeAllEnabled(true);
                    } else {
                        String[] columnPatterns = exclusionGroup.getProperties("columnPatterns");
                        if(c != null && columnPatterns != null && columnPatterns.length > 0) {
                            exclusions.addExclusionPatterns(columnPatterns);
                        }
                    }
                }
            }
        }
    }

    private static Class getClassValue(PropertyGroup group, String propertyName) {
        String propertyValue = group.getProperty(propertyName);
        Class result = null;
        if(propertyValue != null) {
            try {
                result = Class.forName(propertyValue);
            } catch (Exception e) {
                log.warn("Invalid class property "+propertyName);
            }
        } else {
            log.info("No property "+propertyName+" defined");
        }
        return result;
    }

    private static boolean getBoolean(PropertyGroup group, String propertyName) {
        String propertyValue = group.getProperty(propertyName);
        if(propertyValue != null) {
            return Boolean.parseBoolean(propertyValue);
        }
        return false;
    }

    private ConditionalHighlightColumnExclusionMap() { }

    public static ConditionalHighlightColumnExclusionMap getInstance() {
        return instance;
    }

    public void addExclusionPatterns(Class clazz, String... excludedColumnIds) {
        ConditionalHighlightColumnExclusions exclusions = exclusionMap.get(clazz);
        if(exclusions == null) {
            exclusions = new ConditionalHighlightColumnExclusions();
            exclusionMap.put(clazz, exclusions);
        }
        exclusions.addExclusionPatterns(excludedColumnIds);
    }

    public boolean isExcluded(Class clazz, String columnId) {
        ConditionalHighlightColumnExclusions exclusions = exclusionMap.get(clazz);
        return exclusions != null && exclusions.isExcluded(columnId);
    }

    public ConditionalHighlightColumnExclusions getExclusionsForClass(Class clazz) {
        return exclusionMap.get(clazz);
    }
}
