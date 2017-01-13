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

package org.bhavaya.beans.criterion;

import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import java.util.*;

/**
 * Comments in progress (by Brendon as gets his head around them).
 *
 * BeanPathTransformerGroup is responsible for relating one class to another through bean paths.  For instance,
 * when a CriterionBeanCollection evaluates a criterion which tests the condition: instrument.type == 0, it will
 * know how to drill down to the instrument type to the instrument type through a bean path (YUK!!!)
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.8 $
 */
public class BeanPathTransformerGroup {
    private static final Log log = Log.getCategory(BeanPathTransformerGroup.class);

    private static final String NAME_PROPERTY = "name";
    private static final String CRITERION_TYPE = "type";
    private static final String TO_BEAN_TYPE = "toBeanType";
    private static final String FROM_BEAN_TYPES_PROPERTY = "beanTypes";
    private static final String FROM_BEAN_TYPE_PROPERTY = "beanType";
    private static final String FROM_BEAN_TYPE_BEANPATH_PROPERTY = "beanPath";

    private static BeanPathTransformerGroup[] instances;
    private static final Map instancesById = new HashMap();
    private static final Map instancesByBeanType = new HashMap();
    private static PropertyGroup criterionPropertyGroups;

    private String id;
    private String name;
    private String criterionType;
    private Map transformersByFromBeanType;
    private Class toBeanType;

    public static synchronized BeanPathTransformerGroup[] getInstances() {
        if (instances == null) {
            List instanceList = new ArrayList();
            PropertyGroup[] rootGroups = getProperties().getGroups();
            for (int i = 0; i < rootGroups.length; i++) {
                PropertyGroup rootGroup = rootGroups[i];
                PropertyGroup[] propertiesForRoot = rootGroup.getGroups();

                for (int j = 0; j < propertiesForRoot.length; j++) {
                    PropertyGroup propertiesForGroup = propertiesForRoot[j];
                    String id = rootGroup.getName() + "." + propertiesForGroup.getName();

                    BeanPathTransformerGroup instance = getInstance(id);
                    instanceList.add(instance);
                }
            }
            instances = (BeanPathTransformerGroup[]) instanceList.toArray(new BeanPathTransformerGroup[instanceList.size()]);
        }
        return instances;
    }

    public static synchronized BeanPathTransformerGroup[] getInstances(String root, Class fromBeanType) {
        Key key = new Key(root, fromBeanType);
        BeanPathTransformerGroup[] instances = (BeanPathTransformerGroup[]) instancesByBeanType.get(key);

        if (instances == null) {
            List instanceList = new ArrayList();
            final PropertyGroup group = getProperties().getGroup(root);
            if (group == null) {
                instances = new BeanPathTransformerGroup[0];
            } else {
                PropertyGroup[] propertiesForRoot = group.getGroups();

                for (int j = 0; j < propertiesForRoot.length; j++) {
                    PropertyGroup propertiesForGroup = propertiesForRoot[j];
                    String id = root + "." + propertiesForGroup.getName();

                    BeanPathTransformerGroup instance = getInstance(id);
                    if (instance != null && instance.isValidForBeanType(fromBeanType)) {
                        instanceList.add(instance);
                    }
                }
                instances = (BeanPathTransformerGroup[]) instanceList.toArray(new BeanPathTransformerGroup[instanceList.size()]);
            }
            instancesByBeanType.put(key, instances);
        }
        return instances;
    }

    public static synchronized BeanPathTransformerGroup getInstance(String id) {
        BeanPathTransformerGroup beanPathTransformerGroup = (BeanPathTransformerGroup) instancesById.get(id);
        if (beanPathTransformerGroup == null) {
            PropertyGroup propertiesForGroup = getProperties().getGroup(id);
            if (propertiesForGroup != null) {
                String toBeanTypeName = propertiesForGroup.getProperty(TO_BEAN_TYPE);
                if (toBeanTypeName == null || ClassUtilities.getClass(toBeanTypeName, false, false) != null) {
                    beanPathTransformerGroup = new BeanPathTransformerGroup(id, propertiesForGroup);
                    if (beanPathTransformerGroup.isValid()) {
                        instancesById.put(id, beanPathTransformerGroup);
                    }
                }
            }
        }
        return beanPathTransformerGroup;
    }

    private static PropertyGroup getProperties() {
        if (criterionPropertyGroups == null) criterionPropertyGroups = CriterionFactory.getCriterionPropertyGroup();
        return criterionPropertyGroups;
    }

    private BeanPathTransformerGroup(String id, PropertyGroup propertiesForGroup) {
        this.id = id;
        this.name = propertiesForGroup.getProperty(NAME_PROPERTY);
        this.criterionType = propertiesForGroup.getProperty(CRITERION_TYPE);
        this.toBeanType = ClassUtilities.getClass(propertiesForGroup.getProperty(TO_BEAN_TYPE), false, false);
        if (toBeanType == null) log.error("Cannot find toBeanType for BeanPathTransformerGroup: " + id);

        PropertyGroup fromBeanTypesPropertyGroup = propertiesForGroup.getGroup(FROM_BEAN_TYPES_PROPERTY);
        if (fromBeanTypesPropertyGroup == null) throw new RuntimeException("Error instantiating criterion: " + id + " no beanTypes defined");
        PropertyGroup[] fromBeanTypePropertyGroups = fromBeanTypesPropertyGroup.getGroups();
        if (fromBeanTypePropertyGroups == null) throw new RuntimeException("Error instantiating criterion: " + id + " no beanTypes defined");

        transformersByFromBeanType = new HashMap();

        for (int i = 0; i < fromBeanTypePropertyGroups.length; i++) {
            PropertyGroup fromBeanTypePropertyGroup = fromBeanTypePropertyGroups[i];
            String fromBeanTypeName = fromBeanTypePropertyGroup.getProperty(FROM_BEAN_TYPE_PROPERTY);
            Class fromBeanType = ClassUtilities.getClass(fromBeanTypeName, false, false);
            if (fromBeanType != null) {
                String[] beanPaths = fromBeanTypePropertyGroup.getProperties(FROM_BEAN_TYPE_BEANPATH_PROPERTY);
                BeanPathTransformer transformer = new BeanPathTransformer(fromBeanType, beanPaths);
                transformersByFromBeanType.put(fromBeanType, transformer);
            } else {
                log.error("Not adding fromBeanType: " + fromBeanTypeName + " to criterion: " + id + ", cannot load class");
            }
        }
    }

    private boolean isValid() {
        return toBeanType != null;
    }

    public BeanPathTransformer[] getTransformers() {
        Collection transformers = transformersByFromBeanType.values();
        return (BeanPathTransformer[]) transformers.toArray(new BeanPathTransformer[transformers.size()]);
    }

    public BeanPathTransformer getTransformer(Class fromBeanType) {
        BeanPathTransformer beanPathTransformer = (BeanPathTransformer) transformersByFromBeanType.get(fromBeanType);
        if (beanPathTransformer == null) {
            Class superBeanType = fromBeanType.getSuperclass();
            while (superBeanType != null) {
                beanPathTransformer = (BeanPathTransformer) transformersByFromBeanType.get(superBeanType);
                if (beanPathTransformer != null) {
                    transformersByFromBeanType.put(fromBeanType, beanPathTransformer);
                    return beanPathTransformer;
                }
                superBeanType = superBeanType.getSuperclass();
            }
            throw new RuntimeException("No beanPath for criterion: " + id + " for fromBeanType: " + fromBeanType.getName());
        }
        return beanPathTransformer;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCriterionType() {
        return criterionType;
    }

    public Class getToBeanType() {
        return toBeanType;
    }

    public boolean isValidForBeanType(Class fromBeanType) {
        if (transformersByFromBeanType.containsKey(fromBeanType)) return true;

        for (Iterator iterator = transformersByFromBeanType.keySet().iterator(); iterator.hasNext();) {
            Class validType = (Class) iterator.next();
            if (validType.isAssignableFrom(fromBeanType)) return true;
        }

        return false;
    }

    private static class Key {
        private String root;
        private Class beanType;

        public Key(String root, Class beanType) {
            this.root = root;
            this.beanType = beanType;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;

            final Key key = (Key) o;

            if (!beanType.equals(key.beanType)) return false;
            if (!root.equals(key.root)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = root.hashCode();
            result = 29 * result + beanType.hashCode();
            return result;
        }
    }
}
