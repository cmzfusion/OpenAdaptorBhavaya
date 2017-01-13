/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.util;

import org.bhavaya.collection.LRUMap;

import java.util.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.8 $
 */

public class PropertyGroup {
    public interface LazyProperty {
        public String[] getProperties();
    }

    private static final java.util.regex.Pattern beanPathSeperatarPattern = java.util.regex.Pattern.compile("\\.");
    private static Map<String, String[]> stringToArray = new LRUMap<String, String[]>(100);

    private String name;
    private PropertyGroup parent;
    private Map<String, List<String>> values = new LinkedHashMap<String, List<String>>();
    private Map<String, List<LazyProperty>> lazyValues = new LinkedHashMap<String, List<LazyProperty>>();
    private Map<String, List<PropertyGroup>> childGroups = new LinkedHashMap<String, List<PropertyGroup>>();

    public PropertyGroup(PropertyGroup parent, String name) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public PropertyGroup getParent() {
        return parent;
    }

    public String[] getPropertyNames() {
        String[] names = values.keySet().toArray(new String[values.size()]);
        String[] lazyNames = lazyValues.keySet().toArray(new String[lazyValues.size()]);
        return Utilities.appendArrays(names, lazyNames);
    }

    public String getPropertyWithoutSplit(String propertyName) {
        return getProperty(new String[]{propertyName});
    }

    public String getProperty(String dotDelimitedPropertyPath) {
        return getProperty(stringToArray(dotDelimitedPropertyPath));
    }

    public String getProperty(String[] propertyPath) {
        String[] properties = getProperties(propertyPath);
        return properties == null || properties.length != 1 ? null : properties[0];
    }

    public String[] getProperties(String dotDelimitedPropertyPath) {
        return getProperties(dotDelimitedPropertyPath, false);
    }

    public String[] getProperties(String dotDelimitedPropertyPath, boolean allowMissingGroups) {
        return getProperties(stringToArray(dotDelimitedPropertyPath), allowMissingGroups);
    }

    public String[] getProperties(String[] propertyPath) {
        return getProperties(propertyPath, false);
    }

    public String[] getProperties(String[] propertyPath, boolean allowMissingGroups) {
        List<String> propertiesList;
        List<LazyProperty> lazyPropertiesList;
        if (propertyPath == null || propertyPath.length == 0) {
            propertiesList = null;
            lazyPropertiesList = null;
        } else if (propertyPath.length == 1) {
            propertiesList = values.get(propertyPath[0]);
            lazyPropertiesList = lazyValues.get(propertyPath[0]);
        } else {
            String[] parentPath = Utilities.subSection(propertyPath, 0, propertyPath.length - 1);
            String propertyName = propertyPath[propertyPath.length - 1];
            PropertyGroup propertyGroup = getGroup(parentPath);
            if (propertyGroup == null) {
                if (!allowMissingGroups) {
                    throw new RuntimeException("Missing property group: " + Utilities.asString(parentPath, "."));
                } else {
                    return null;
                }
            }
            propertiesList = propertyGroup.values.get(propertyName);
            lazyPropertiesList = propertyGroup.lazyValues.get(propertyName);
        }

        String[] properties = propertiesList == null ? new String[0] : propertiesList.toArray(new String[propertiesList.size()]);
        String[] lazyProperties = fetchLazyProperties(lazyPropertiesList);
        return propertiesList == null && lazyProperties == null
                ? null
                : Utilities.appendArrays(properties, lazyProperties);
    }

    private String[] fetchLazyProperties(List<LazyProperty> lazyValuesList) {
        if (lazyValuesList == null || lazyValuesList.size() == 0) return new String[0];

        List<String> results = new ArrayList<String>();
        for (LazyProperty lazyProperty : lazyValuesList) {
            results.addAll(Arrays.asList(lazyProperty.getProperties()));
        }
        return results.toArray(new String[results.size()]);
    }

    public String getFQN() {
        StringBuffer buffer = new StringBuffer();
        PropertyGroup propertyGroup = this;
        while (propertyGroup.parent != null) {
            if (propertyGroup != this) buffer.insert(0, ".");
            buffer.insert(0, propertyGroup.name);
            propertyGroup = propertyGroup.parent;
        }
        if (buffer.length() > 0 && buffer.charAt(0) == '.') buffer.deleteCharAt(0);
        return buffer.toString();
    }

    public String getMandatoryProperty(String dotDelimitedPropertyPath) {
        return getMandatoryProperty(stringToArray(dotDelimitedPropertyPath));
    }

    public String getMandatoryProperty(String[] propertyPath) {
        String property = getProperty(propertyPath);
        if (property == null) throw new RuntimeException("Could not find property: " + name + "." + Utilities.asString(propertyPath, "."));
        return property;
    }

    public String[] getMandatoryProperties(String dotDelimitedPropertyPath) {
        return getMandatoryProperties(stringToArray(dotDelimitedPropertyPath));
    }

    public String[] getMandatoryProperties(String[] propertyPath) {
        String[] properties = getProperties(propertyPath);
        if (properties == null || properties.length == 0) throw new RuntimeException("Could not find property: " + name + "." + Utilities.asString(propertyPath, "."));
        return properties;
    }

    public PropertyGroup getGroup(String dotDelimitedPropertyPath) {
        return getGroup(stringToArray(dotDelimitedPropertyPath));
    }

    public PropertyGroup getGroup(String[] propertyPath) {
        PropertyGroup[] propertyGroups = getGroups(propertyPath);
        if (propertyGroups == null) return null;
        if (propertyGroups.length != 1) throw new RuntimeException("Asked for single property group (scalar) using: <PropertyGroup> getGroup(...), but there were " + propertyGroups.length + " groups return from that call.  Either use <PropertyGroup[]> getGroups(...) or look at " + Utilities.asString(propertyPath, ".") + " which led to this error");
        return propertyGroups[0];
    }

    public PropertyGroup[] getGroups(String dotDelimitedPropertyPath) {
        return getGroups(stringToArray(dotDelimitedPropertyPath));
    }

    public PropertyGroup[] getGroups(String[] propertyPath) {
        return getGroups(propertyPath, false);
    }

    public PropertyGroup[] getGroups(String[] propertyPath, boolean allowMissingGroups) {
        List<PropertyGroup> propertyGroups;
        if (propertyPath == null || propertyPath.length == 0) {
            propertyGroups = null;
        } else if (propertyPath.length == 1) {
            propertyGroups = childGroups.get(propertyPath[0]);
        } else {
            PropertyGroup propertyGroup = this;
            for (int i = 0; i < propertyPath.length - 1; i++) {
                String propertyName = propertyPath[i];
                propertyGroup = propertyGroup.getGroup(new String[]{propertyName});
                if (propertyGroup == null) {
                    if (!allowMissingGroups) {
                        throw new RuntimeException("Could not find propertyGroup: " + propertyName + " in " + name + "." + Utilities.asString(propertyPath, "."));
                    } else {
                        return null;
                    }
                }
            }
            propertyGroups = propertyGroup.childGroups.get(propertyPath[propertyPath.length - 1]);
        }

        return propertyGroups == null ? null : propertyGroups.toArray(new PropertyGroup[propertyGroups.size()]);
    }

    public PropertyGroup[] getGroups() {
        List<PropertyGroup> groups = new ArrayList<PropertyGroup>();
        Collection<List<PropertyGroup>> groupLists = childGroups.values();

        for (List<PropertyGroup> groupList : groupLists) {
            for (PropertyGroup group : groupList) {
                groups.add(group);
            }
        }

        return groups.toArray(new PropertyGroup[groups.size()]);
    }

    public void addProperty(String name, String value) {
        List<String> valueList = values.get(name);
        if (valueList == null) {
            valueList = new ArrayList<String>();
            values.put(name, valueList);
        }

        valueList.add(value);
    }

    public void addLazyProperty(String name, LazyProperty lazyProperty) {
        List<LazyProperty> lazyValueList = lazyValues.get(name);
        if (lazyValueList == null) {
            lazyValueList = new ArrayList<LazyProperty>();
            lazyValues.put(name, lazyValueList);
        }

        lazyValueList.add(lazyProperty);
    }

    public void addPropertyGroup(String name, PropertyGroup propertyGroup) {
        List<PropertyGroup> groupList = childGroups.get(name);
        if (groupList == null) {
            groupList = new ArrayList<PropertyGroup>();
            childGroups.put(name, groupList);
        }

        groupList.add(propertyGroup);
    }

    public Number getNumericProperty(String dotDelimitedPropertyPath) {
        String valueString = getProperty(dotDelimitedPropertyPath);
        return (Number) Utilities.changeType(Double.class, valueString);
    }

    public Properties toJavaProperties() {
        Properties properties = new Properties();

        String[] propertyNames = getPropertyNames();
        for (String propertyName : propertyNames) {
            properties.put(propertyName, getPropertyWithoutSplit(propertyName));
        }
        return properties;
    }

    static String[] stringToArray(String locator) {
        if (locator == null || locator.length() == 0) return null;
        String[] nameArray = stringToArray.get(locator);
        if (nameArray == null) {
            nameArray = beanPathSeperatarPattern.split(locator);
            stringToArray.put(locator, nameArray);
        }
        return nameArray;
    }
}