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

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.beans.Schema;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.beans.PropertyDescriptor;
import java.beans.Introspector;
import java.beans.IntrospectionException;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.25 $
 */
public class PropertyModel {
    private static final Log log = Log.getCategory(PropertyModel.class);
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    private static Map<Class, PropertyModel> instances = new HashMap<Class, PropertyModel>();
    private static boolean initialised = false;
    private static final Object initLock = new Object();
    private static List<PropertyChangeListener> propertyChangeListeners = new ArrayList<PropertyChangeListener>();
    private static Map<String, Class<?>[]> classesInPathByClass = new HashMap<String, Class<?>[]>();
    private static Strategy strategy;

    private static String version;

    private Attribute[] declaredAttributes;
    private Class<?> realClass;
    private Class<?> type;
    private Class<?> superType;
    private Map<String, String> propertyToDisplayNames = new HashMap<String, String>();
    private Map<String, String> propertyToDescriptions = new HashMap<String, String>();
    private Set<String> hiddenProperties = new HashSet<String>();
    private boolean inlined;
    private Class<?>[] subClasses;
    private Map<String, Class<?>[]> validTypesForProperty = new HashMap<String, Class<?>[]>();
    private Type genericType;
    private boolean selectable;
    private boolean keyable;

    public static void init() {
        // Only run once
        synchronized (initLock) {
            if (initialised) return;
            initialised = true;

            org.bhavaya.util.PropertyGroup propertyTreePropertyGroup = ApplicationProperties.getApplicationProperties().getGroup("propertyTree");

            if (propertyTreePropertyGroup != null) {
                Log.getPrimaryLoadingLog().info("Building property tree");
                Log.getSecondaryLoadingLog().info(" ");

                String[] strategyClassNames = propertyTreePropertyGroup.getProperties("strategy");
                if (strategyClassNames == null || strategyClassNames.length == 0) {
                    strategy = new DefaultStrategy();
                } else {
                    try {
                        Strategy[] strategies = new Strategy[strategyClassNames.length];
                        for (int i = 0; i < strategyClassNames.length; i++) {
                            strategies[i] = (Strategy) ClassUtilities.getClass(strategyClassNames[i]).newInstance();
                        }

                        if (strategies.length == 1) {
                            strategy = strategies[0];
                        } else {
                            strategy = new CompoundStrategy(strategies);
                        }
                    } catch (Exception e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                }

                strategy.getRealClass(PropertyModel.class); // This is to force some heavy weight initialisation during init

                String schemaFileName = propertyTreePropertyGroup.getProperty("include");
                String reconcileOnly = propertyTreePropertyGroup.getProperty("reconcileOnly");
                if (schemaFileName != null) {
                    PropertyModelReader propertModelReader = new PropertyModelReader(schemaFileName, reconcileOnly != null && reconcileOnly.toLowerCase().equals("true"));
                    try {
                        propertModelReader.read();
                    } catch (RuntimeException e) {
                        log.error(e);
                    }
                }

                org.bhavaya.util.PropertyGroup displayNamesPropertyGroup = propertyTreePropertyGroup.getGroup("displayNames");
                if (displayNamesPropertyGroup != null) {
                    org.bhavaya.util.PropertyGroup[] displayNamesPropertyGroups = displayNamesPropertyGroup.getGroups();
                    if (displayNamesPropertyGroups != null) {
                        for (PropertyGroup displayNamePropertyGroup : displayNamesPropertyGroups) {
                            String parentTypeName = displayNamePropertyGroup.getProperty("type");
                            Class<?> parentType = ClassUtilities.getClass(parentTypeName, false, false);
                            if (parentType != null) {
                                String propertyName = displayNamePropertyGroup.getProperty("propertyName");
                                String displayName = displayNamePropertyGroup.getProperty("displayName");
                                PropertyModel.getInstance(parentType).setDisplayName(propertyName, displayName);
                            }
                        }
                    }
                }

                org.bhavaya.util.PropertyGroup hiddenPropertiesPropertyGroup = propertyTreePropertyGroup.getGroup("hiddenProperties");
                if (hiddenPropertiesPropertyGroup != null) {
                    org.bhavaya.util.PropertyGroup[] hiddenPropertyPropertyGroups = hiddenPropertiesPropertyGroup.getGroups();
                    if (hiddenPropertyPropertyGroups != null) {
                        for (PropertyGroup hiddenPropertyPropertyGroup : hiddenPropertyPropertyGroups) {
                            String propertyName = hiddenPropertyPropertyGroup.getProperty("propertyName");
                            String propertyTypeName = hiddenPropertyPropertyGroup.getProperty("class");
                            Class<?> propertyType = ClassUtilities.getClass(propertyTypeName, false, false);
                            if (propertyType != null) {
                                PropertyModel.getInstance(propertyType).setHidden(propertyName, true);
                            }
                        }
                    }
                }

                org.bhavaya.util.PropertyGroup validTypesForPropertiesPropertyGroup = propertyTreePropertyGroup.getGroup("validTypesForProperties");
                if (validTypesForPropertiesPropertyGroup != null) {
                    org.bhavaya.util.PropertyGroup[] validTypesForPropertyPropertyGroups = validTypesForPropertiesPropertyGroup.getGroups();
                    if (validTypesForPropertyPropertyGroups != null) {
                        for (PropertyGroup group : validTypesForPropertyPropertyGroups) {
                            String parentTypeName = group.getProperty("class");
                            String propertyName = group.getProperty("propertyName");
                            String[] validTypeNames = group.getProperties("validType");
                            Class<?>[] validTypes = new Class<?>[validTypeNames.length];
                            for (int j = 0; j < validTypeNames.length; j++) {
                                validTypes[j] = ClassUtilities.getClass(validTypeNames[j], false, false);
                            }
                            validTypes = (Class<?>[]) Utilities.filterArray(validTypes, new Filter() {
                                public boolean evaluate(Object obj) {
                                    return obj != null;
                                }
                            });
                            Class<?> parentType = ClassUtilities.getClass(parentTypeName, false, false);
                            if (validTypes.length > 0 && parentType != null) {
                                PropertyModel.getInstance(parentType).setValidTypesForProperty(propertyName, validTypes);
                            }
                        }
                    }
                }

                String[] inlinedTypeNames = propertyTreePropertyGroup.getProperties("inlinedTypes");
                if (inlinedTypeNames != null) {
                    for (String inlinedTypeName : inlinedTypeNames) {
                        Class<?> inlinedType = ClassUtilities.getClass(inlinedTypeName, false, false);
                        if (inlinedType != null) {
                            PropertyModel.getInstance(inlinedType).setInlined(true);
                        }
                    }
                }

                org.bhavaya.util.PropertyGroup selectableTypesPropertyGroup = propertyTreePropertyGroup.getGroup("selectableTypes");
                if (selectableTypesPropertyGroup != null) {
                    String[] typeNames = selectableTypesPropertyGroup.getProperties("selectableType");
                    if (typeNames != null) {
                        for (String typeName : typeNames) {
                            Class<?> type = ClassUtilities.getClass(typeName, false, false);
                            if (type != null) {
                                PropertyModel.getInstance(type).setSelectable(true);
                            }
                        }
                    }
                }
            } else {
                strategy = new DefaultStrategy();
            }
        }
    }

    public static void setStrategy(Strategy strategy) {
        PropertyModel.strategy = strategy;
    }

    public static PropertyModel getInstance(Class<?> type) {
        init();

        synchronized (PropertyModel.class) {
            PropertyModel instance = instances.get(type);
            if (instance == null) {
                instance = new PropertyModel(type);
                instances.put(instance.getType(), instance);
                instances.put(instance.getRealClass(), instance);
            }
            return instance;
        }
    }

    private PropertyModel(Class<?> clazz) {
        if (clazz != null && !ClassUtilities.isPrimitiveTypeOrClass(clazz) && !clazz.getName().startsWith("java")) {
            type = strategy.getType(clazz);
            realClass = strategy.getRealClass(clazz);
            superType = type.getSuperclass();
            subClasses = strategy.findSubClasses(type);
            genericType = Generic.getType(realClass);
            selectable = false;
            keyable = strategy.isKeyable(type);
            initFromMetaData();
        } else if (clazz != null) {
            type = clazz;
            realClass = clazz;
            superType = clazz.getSuperclass();
            subClasses = EMPTY_CLASS_ARRAY;
            genericType = Generic.getType(realClass);
            selectable = true;
            keyable = false;
        }
    }

    private void initFromMetaData() {
        try {
            ClassMetaData classMetaData = type.getAnnotation(ClassMetaData.class);
            if (classMetaData != null) {
                if (classMetaData.selectable()) setSelectable(true);
            }

            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(type).getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (propertyDescriptor.getPropertyType() == null) continue;
                Method getter = propertyDescriptor.getReadMethod();
                if (getter == null) continue;
                readPropertyMetaData(propertyDescriptor, getter);
            }

            //this propery exists only as an optimization and should always be hidden in the ui
            hiddenProperties.add("lazyNullInstance");
        } catch (IntrospectionException e) {
            log.warn(e);
        }
    }

    private void readPropertyMetaData(PropertyDescriptor propertyDescriptor, Method getter) {
        PropertyMetaData propertyMetaData = getter.getAnnotation(PropertyMetaData.class);
        if (propertyMetaData != null) {
            String propertyName = propertyDescriptor.getName();
            if (propertyMetaData.hidden()) setHidden(propertyName, true);
            if (!propertyMetaData.displayName().equals(PropertyMetaData.NULL)) setDisplayName(propertyName, propertyMetaData.displayName());
            if (!propertyMetaData.description().equals(PropertyMetaData.NULL)) setDescription(propertyName, propertyMetaData.description());
            if (propertyMetaData.validPropertyTypes().length > 0) setValidTypesForProperty(propertyName, propertyMetaData.validPropertyTypes());
        }
    }

    public Class<?> getType() {
        return type;
    }

    public Class<?> getRealClass() {
        return realClass;
    }

    public boolean isKeyable() {
        return keyable;
    }

    public Object getKeyForBean(Object bean) {
        return keyable ? strategy.getKeyForBean(bean) : null;
    }

    public synchronized void setHidden(String propertyName, boolean hidden) {
        boolean oldHiddenStatus = hiddenProperties.contains(propertyName);
        if (hidden) {
            hiddenProperties.add(propertyName);
        } else {
            hiddenProperties.remove(propertyName);
        }
        if (oldHiddenStatus != hidden) fireHiddenStatusChanged(propertyName, hidden);

        if (subClasses != null) {
            for (Class<?> subClass : subClasses) {
                PropertyModel.getInstance(subClass).setHidden(propertyName, hidden);
            }
        }
    }

    public boolean isHidden(String propertyName) {
        return hiddenProperties.contains(propertyName);
    }

    public synchronized void setSelectable(boolean selectable) {
        this.selectable = selectable;

        if (subClasses != null) {
            for (Class<?> subClass : subClasses) {
                PropertyModel.getInstance(subClass).setSelectable(selectable);
            }
        }
    }

    public boolean isSelectable() {
        if (selectable) return true;
        return LookupValue.class.isAssignableFrom(realClass) || !Observable.class.isAssignableFrom(realClass);
    }

    public boolean isInlined() {
        return inlined;
    }

    public void setInlined(boolean inlined) {
        this.inlined = inlined;
    }

    public synchronized void setDisplayName(String propertyName, String displayName) {
        if (log.isDebug()) log.debug("Setting display name of property: " + type.getName() + "." + propertyName + " to: " + displayName);
        propertyToDisplayNames.put(propertyName, displayName);
        fireDisplayNameChanged(propertyName, displayName);

        if (subClasses != null) {
            for (Class<?> subClass : subClasses) {
                PropertyModel.getInstance(subClass).setDisplayName(propertyName, displayName);
            }
        }
    }

    public synchronized String getDisplayName(String property) {
        String alias = propertyToDisplayNames.get(property);

        if (alias == null) {
            alias = Utilities.getDisplayName(property);
            propertyToDisplayNames.put(property, alias);
        }

        return alias;
    }

    public synchronized void setDescription(String propertyName, String description) {
        if (log.isDebug()) log.debug("Setting description of a property: " + type.getName() + "." + propertyName + " to: " + description);
        propertyToDescriptions.put(propertyName, description);
        //fireDescriptionChanged(propertyName, description);

        if (subClasses != null) {
            for (Class<?> subClass : subClasses) {
                PropertyModel.getInstance(subClass).setDescription(propertyName, description);
            }
        }
    }

    public synchronized String getDescription(String property) {
        return propertyToDescriptions.get(property);
    }

    private void fireDisplayNameChanged(String propertyName, String newDisplayName) {
        for (PropertyChangeListener listener : propertyChangeListeners) {
            listener.displayNameChanged(type, propertyName, newDisplayName);
        }
    }

    private void fireHiddenStatusChanged(String propertyName, boolean hidden) {
        for (PropertyChangeListener listener : propertyChangeListeners) {
            listener.hiddenStatusChanged(type, propertyName, hidden);
        }
    }

    public synchronized static void addPropertyDisplayNameChangeListener(PropertyChangeListener l) {
        propertyChangeListeners.add(l);
    }

    public synchronized static void removePropertyDisplayNameChangeListener(PropertyChangeListener l) {
        propertyChangeListeners.remove(l);
    }

    public Class<?>[] getSubClasses() {
        return subClasses;
    }

    public Type getGenericType() {
        return genericType;
    }

    public Attribute getAttribute(String[] propertyPath) {
        Class<?>[] classes = findMatchingSubclasses(propertyPath[0]);
        if (classes == null || classes.length == 0) return null;
        if (propertyPath.length == 1) {
            return Generic.getType(classes[0]).getAttribute(propertyPath[0]);
        }
        for (Class<?> aClass : classes) {
            String[] newPropertyPath = (String[]) Utilities.subSection(propertyPath, 1, propertyPath.length - 1);
            Class<?> propertyType = Generic.getType(aClass).getAttribute(propertyPath[0]).getType();
            Attribute attribute = PropertyModel.getInstance(propertyType).getAttribute(newPropertyPath);
            if (attribute != null) return attribute;
        }
        return null;
    }

    /**
     * Returns all the subclasses with this property.
     */
    public Class<?>[] findMatchingSubclasses(String propertyName) {
        if (Generic.getType(realClass).attributeExists(propertyName)) return new Class[] {realClass};

        Class<?>[] subClasses = PropertyModel.getInstance(type).getSubClasses();
        ArrayList<Class<?>> matchingSubClasses = new ArrayList<Class<?>>();

        // breadth first search
        for (Class<?> subClass : subClasses) {
            if (Generic.getType(subClass).attributeExists(propertyName)) {
                matchingSubClasses.add(subClass);
            }
        }

        // now increase depth by one
        for (Class<?> subClass : subClasses) {
            Class<?> subSubClass = PropertyModel.getInstance(subClass).findMatchingSubclass(propertyName);
            if (subSubClass != null) matchingSubClasses.add(subSubClass);
        }

        if (matchingSubClasses.size() == 0) {
            return null;
        } else {
            return matchingSubClasses.toArray(new Class<?>[matchingSubClasses.size()]);
        }
    }

    public Class<?> findMatchingSubclass(String propertyName) {
        if (Generic.getType(realClass).attributeExists(propertyName)) return realClass;

        Class<?>[] subClasses = PropertyModel.getInstance(type).getSubClasses();

        // breadth first search
        for (Class<?> subClass : subClasses) {
            if (Generic.getType(subClass).attributeExists(propertyName)) {
                return subClass;
            }
        }

        // now increase depth by one
        for (Class<?> subClass : subClasses) {
            Class<?> subSubClass = PropertyModel.getInstance(subClass).findMatchingSubclass(propertyName);
            if (subSubClass != null) return subSubClass;
        }

        return null;
    }

    public Attribute[] getAttributes() {
        return genericType.getAttributes(); // returns attributes of realClass, which may be more than those of type
    }

    public Attribute[] getDeclaredAttributes() {
        if (declaredAttributes == null) {
            if (superType != null) {
                Type superGenericType = PropertyModel.getInstance(superType).getGenericType();

                //now remove attributes common with the supertype
                List<Attribute> attributes = new ArrayList<Attribute>(Arrays.asList(genericType.getAttributes()));
                for (Iterator<Attribute> iterator = attributes.iterator(); iterator.hasNext();) {
                    Attribute attribute = iterator.next();
                    if (superGenericType.attributeExists(attribute.getName())) {
                        iterator.remove();
                    }
                }
                declaredAttributes = new Attribute[attributes.size()];
                declaredAttributes = attributes.toArray(declaredAttributes);
            } else {
                declaredAttributes = genericType.getAttributes();
            }
        }
        return declaredAttributes;
    }

    public static void setVersion(String version) {
        PropertyModel.version = version;
    }

    public void setValidTypesForProperty(String property, Class<?>[] validTypes) {
        validTypesForProperty.put(property, validTypes);
    }

    public Class<?>[] getValidTypesForProperty(String property) {
        Class<?>[] validTypes = validTypesForProperty.get(property);
        if (validTypes == null) {
            Attribute attribute = getAttribute(new String[]{property});
            if (attribute != null) {
                Class<?> propertyType = attribute.getType();
                validTypes = PropertyModel.getInstance(propertyType).getSubClasses();
            }
        }
        return validTypes;
    }

    public static interface Strategy {
        public boolean isRelevant(Class<?> clazz);

        public Class<?> getType(Class<?> clazz);

        public Class<?> getRealClass(Class<?> clazz);

        public Class<?>[] findSubClasses(Class<?> clazz);

        public boolean isKeyable(Class<?> clazz);

        public Object getKeyForBean(Object bean);
    }

    public static class DefaultStrategy implements Strategy {
        public boolean isRelevant(Class<?> clazz) {
            return true;
        }

        public Class<?> getType(Class<?> clazz) {
            return clazz;
        }

        public boolean isKeyable(Class<?> clazz) {
            return false;
        }

        public Object getKeyForBean(Object bean) {
            return null;
        }

        public Class<?> getRealClass(Class<?> clazz) {
            return clazz;
        }

        public Class<?>[] findSubClasses(Class<?> clazz) {
            return EMPTY_CLASS_ARRAY;
        }
    }


    public static class ClassPathStrategy extends DefaultStrategy {
        private static final String[] EMPTY_STRING_ARRAY = new String[]{};

        public ClassPathStrategy() {
            readSubclassMappingsFromFile();
        }

        public Class<?> getType(Class<?> clazz) {
            Class<?> superClass = clazz.getSuperclass();
            // if superclass has the same unqualified name then return it
            if (superClass != null && Utilities.equals(ClassUtilities.getUnqualifiedClassName(clazz), ClassUtilities.getUnqualifiedClassName(superClass))) {
                return superClass;
            } else {
                return clazz;
            }
        }

        public Class<?> getRealClass(Class<?> clazz) {
            if (clazz == null) return clazz;
            // find subclass of clazz with the same unqualified name
            File path = ClassUtilities.getJarFileOrClasspathDir(clazz);
            if (path == null) return clazz;
            Class<?>[] classesForPath = getClassesForPath(path);
            for (int i = 0; i < classesForPath.length; i++) {
                Class<?> possibleRealClass = classesForPath[i];
                Class<?> superclass = possibleRealClass.getSuperclass();
                if (superclass != null && superclass == clazz && Utilities.equals(ClassUtilities.getUnqualifiedClassName(clazz), ClassUtilities.getUnqualifiedClassName(possibleRealClass))) {
                    clazz = possibleRealClass;
                }
                if ((i % 20) == 0) Thread.yield();
            }

            return clazz;
        }

        public Class<?>[] findSubClasses(Class<?> type) {
            if (type == null) return EMPTY_CLASS_ARRAY;
            List<Class<?>> subClassesForClassList = new ArrayList<Class<?>>();

            try {
                File path = ClassUtilities.getJarFileOrClasspathDir(type);
                if (path == null) return EMPTY_CLASS_ARRAY;
                Class<?>[] classesForPath = getClassesForPath(path);

                for (int i = 0; i < classesForPath.length; i++) {
                    Class<?> possibleSubclass = classesForPath[i];
                    Class<?> superclass = possibleSubclass.getSuperclass();
                    if (type.isInterface()) {
                        if (type != possibleSubclass && type.isAssignableFrom(possibleSubclass)) { // implements the interface
                            if (superclass == null || !type.isAssignableFrom(superclass)) { // we want only direct implementations (not inherited ones)
                                subClassesForClassList.add(strategy.getRealClass(possibleSubclass));
                            }
                        }
                    } else if (superclass != null && superclass == type) {
                        if (!Utilities.equals(ClassUtilities.getUnqualifiedClassName(type), ClassUtilities.getUnqualifiedClassName(possibleSubclass))) {
                            subClassesForClassList.add(strategy.getRealClass(possibleSubclass));
                        }
                    }
                    if ((i % 20) == 0) Thread.yield();
                }
            } catch (Exception e) {
                log.error(e);
            }

            return subClassesForClassList.toArray(new Class<?>[subClassesForClassList.size()]);
        }

        protected static Class<?>[] getClassesForPath(File path) {
            Class<?>[] classes = classesInPathByClass.get(path.getAbsolutePath());

            if (classes == null) {
                log.info("Searching for classes in path: " + path);
                Log.getSecondaryLoadingLog().info("Scanning classpath...");
                long starttime = System.currentTimeMillis();
                if (!path.exists()) throw new RuntimeException("File not found: " + path);
                if (!path.canRead()) throw new RuntimeException("File is not readable: " + path);
                List<Class> classesList = new ArrayList<Class>();

                String[] files;
                if (path.isDirectory()) {
                    files = listClassesInDir(path, path, null);
                } else {
                    files = listClassesInJar(path);
                }

                for (int i = 0; i < files.length; i++) {
                    String classname = files[i];
                    try {
                        // prevent any class load order effects while loading schemas
                        Class<?> clazz = ClassUtilities.getClass(classname, true, false);
                        classesList.add(clazz);
                    } catch (Throwable e) {
                        log.info("Not adding class: " + classname + " for path: " + path);
                    }
                    if ((i % 20) == 0) Thread.yield();
                }

                classes = classesList.toArray(new Class[classesList.size()]);
                classesInPathByClass.put(path.getAbsolutePath(), classes);
                log.info("Search took: " + (System.currentTimeMillis() - starttime) + " millis");
            }
            return classes;
        }


        protected static String[] listClassesInDir(File ancestor, File parent, List<String> initialFileList) {
            if (!parent.isDirectory()) throw new RuntimeException("Parent is not a directory: " + parent);

            List<String> fileList;
            if (initialFileList == null) {
                fileList = new ArrayList<String>();
            } else {
                fileList = initialFileList;
            }

            File[] files = parent.listFiles();
            File target;
            String targetName;


            for (int i = 0; i < files.length; i++) {
                target = files[i];
                targetName = target.getAbsolutePath();

                if (target.isFile() && target.getName().endsWith(".class")) {
                    targetName = targetName.substring(ancestor.getAbsolutePath().length(), targetName.length());
                    fileList.add(ClassUtilities.filenameToClassname(targetName));
                } else if (target.isDirectory()) {
                    listClassesInDir(ancestor, target, fileList);
                }
                if ((i % 20) == 0) Thread.yield();
            }

            return fileList.toArray(EMPTY_STRING_ARRAY);
        }

        protected static String[] listClassesInJar(File source) {
            List<String> fileList = null;
            JarFile file = null;
            try {
                fileList = new ArrayList<String>();
                file = new JarFile(source);
                for (Enumeration<JarEntry> enumeration = file.entries(); enumeration.hasMoreElements();) {
                    JarEntry entry = enumeration.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) fileList.add(ClassUtilities.filenameToClassname(entry.getName()));
                }
            } catch (IOException e) {
                log.error(e);
            } finally {
                if (file != null) {
                    try {
                        file.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
            return fileList.toArray(new String[fileList.size()]);
        }

    }

    public static class SchemaStrategy extends DefaultStrategy {
        public boolean isRelevant(Class<?> clazz) {
            return Schema.hasInstance(clazz);
        }

        public Class<?> getType(Class<?> clazz) {
            if (!isRelevant(clazz)) return super.getType(clazz);
            return Schema.getInstance(clazz).getType();
        }

        public Class<?> getRealClass(Class<?> clazz) {
            if (!isRelevant(clazz)) return super.getRealClass(clazz);
            return Schema.getInstance(clazz).getGeneratedClass();
        }

        public Class<?>[] findSubClasses(Class<?> clazz) {
            if (!isRelevant(clazz)) return super.findSubClasses(clazz);
            return Schema.getInstance(clazz).getSubClasses();
        }

        public boolean isKeyable(Class<?> clazz) {
            if (!isRelevant(clazz)) return super.isKeyable(clazz);
            return Schema.hasInstance(clazz);
        }

        public Object getKeyForBean(Object bean) {
            if (bean == null || !isRelevant(bean.getClass())) return super.getKeyForBean(bean);
            return BeanFactory.getKeyForBean(bean);
        }
    }

    public static class CompoundStrategy implements Strategy {
        private Strategy[] strategies;

        public CompoundStrategy(Strategy[] strategies) {
            this.strategies = strategies;
        }

        public boolean isRelevant(Class<?> clazz) {
            for (Strategy strategy : strategies) {
                if (strategy.isRelevant(clazz)) return true;
            }
            return false;
        }

        public Class<?> getType(Class<?> clazz) {
            for (Strategy strategy : strategies) {
                if (strategy.isRelevant(clazz)) return strategy.getType(clazz);
            }
            return clazz;
        }

        public Class<?> getRealClass(Class<?> clazz) {
            for (Strategy strategy : strategies) {
                if (strategy.isRelevant(clazz)) return strategy.getRealClass(clazz);
            }
            return clazz;
        }

        public Class<?>[] findSubClasses(Class<?> clazz) {
            for (Strategy strategy : strategies) {
                if (strategy.isRelevant(clazz)) return strategy.findSubClasses(clazz);
            }
            return EMPTY_CLASS_ARRAY;
        }

        public boolean isKeyable(Class<?> clazz) {
            for (Strategy strategy : strategies) {
                if (strategy.isRelevant(clazz)) return strategy.isKeyable(clazz);
            }
            return false;
        }

        public Object getKeyForBean(Object bean) {
            if (bean == null) return null;
            for (Strategy strategy : strategies) {
                if (strategy.isRelevant(bean.getClass())) return strategy.getKeyForBean(bean);
            }
            return null;
        }

    }

    public interface PropertyChangeListener {
        public void displayNameChanged(Class<?> parentType, String propertyName, String newDisplayName);
        public void hiddenStatusChanged(Class<?> parentType, String propertyName, boolean hidden);
    }

    public static abstract class WeakPropertyChangeListener implements PropertyChangeListener {
        private static final ReferenceQueue<WeakPropertyChangeListenerReference> referenceQueue = new ReferenceQueue<WeakPropertyChangeListenerReference>();
        private Reference listenerOwnerReference;

        static {
            TimerTask referenceQueuePoller = new ReferenceQueuePollerRunnable();
            Utilities.getApplicationTimer().schedule(referenceQueuePoller, 2000, 2000);
        }

        public abstract void displayNameChanged(Object listenerOwner, Class<?> parentType, String propertyName, String newDisplayName);

        public abstract void hiddenStatusChanged(Object listenerOwner, Class<?> parentType, String propertyName, boolean hidden);

        protected WeakPropertyChangeListener(Object listenerOwner) {
            this.listenerOwnerReference = new WeakPropertyChangeListenerReference(listenerOwner, referenceQueue, this);
        }

        public void displayNameChanged(Class<?> parentType, String propertyName, String newDisplayName) {
            Object listenerOwner = listenerOwnerReference.get();
            if (listenerOwner == null) {
                return;
            }

            displayNameChanged(listenerOwner, parentType, propertyName, newDisplayName);
        }

        public void hiddenStatusChanged(Class<?> parentType, String propertyName, boolean hidden) {
            Object listenerOwner = listenerOwnerReference.get();
            if (listenerOwner == null) {
                return;
            }

            hiddenStatusChanged(listenerOwner, parentType, propertyName, hidden);
        }

        private static class WeakPropertyChangeListenerReference<T> extends WeakReference<T> {
            private WeakPropertyChangeListener propertyChangeListener;

            public WeakPropertyChangeListenerReference(T listenerOwner, ReferenceQueue<? super T> queue, WeakPropertyChangeListener propertyChangeListener) {
                super(listenerOwner, queue);
                this.propertyChangeListener = propertyChangeListener;
            }

            public WeakPropertyChangeListener getPropertyChangeListener() {
                return propertyChangeListener;
            }
        }

        private static class ReferenceQueuePollerRunnable extends TimerTask {
            public void run() {
                WeakPropertyChangeListenerReference<?> reference = (WeakPropertyChangeListenerReference) referenceQueue.poll();
                int i = 0;
                while (reference != null) {
                    i++;
                    if (i % 50 == 0) Thread.yield(); // be kind to other threads
                    WeakPropertyChangeListener listener = reference.getPropertyChangeListener();
                    PropertyModel.removePropertyDisplayNameChangeListener(listener);
                    reference =  (WeakPropertyChangeListenerReference) referenceQueue.poll();
                }
            }
        }
    }

    private static String getZipFileName() {
        return IOUtilities.getUserCacheDirectory() + "/" + "subclassmapping" + "." + ApplicationInfo.getInstance().getEnvironmentId() + ".zip";
    }

    public static void writeSubclassMappingsToFile() {
        try {
            long startTime = System.currentTimeMillis();
            String zipFileName = getZipFileName();
            log.info("Writing subclass mapping file: " + zipFileName);
            File file = new File(zipFileName);
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }

            ZipOutputStream stream = null;
            try {
                stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                stream.setLevel(9);

                for (String path : classesInPathByClass.keySet()) {
                    Class<?>[] classes = classesInPathByClass.get(path);
                    if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                    ZipEntry zipEntry = new ZipEntry(path + "/classes.xml");
                    zipEntry.setComment(version);
                    log.info("Writing subclass mappings file entry: " + zipEntry.getName());
                    stream.putNextEntry(zipEntry);
                    ByteArrayOutputStream entryStream = new ByteArrayOutputStream(4096);
                    BeanUtilities.writeObjectToStream(classes, entryStream);
                    stream.write(entryStream.toByteArray());
                    stream.closeEntry();
                }
            } finally {
                if (stream != null) stream.close();
            }
            log.info("Wrote subclass mappings file: " + zipFileName + " in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
        } catch (Throwable e) {
            log.error(e);
        }
    }

    private static void readSubclassMappingsFromFile() {
        try {
            long startTime = System.currentTimeMillis();
            String zipFileName = getZipFileName();
            File file = new File(zipFileName);
            if (!file.exists()) {
                if (log.isDebug())log.debug("Could not find subclass mappings file: " + zipFileName);
                return;
            }
            log.info("Reading subclass mappings file: " + zipFileName);
            ZipFile zipFile = new ZipFile(file);

            HashMap<String, Class<?>[]> tempMap = new HashMap<String, Class<?>[]>();
            for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
                ZipEntry zipEntry = entries.nextElement();
                String entryName = zipEntry.getName();
                String entryVersion = zipEntry.getComment();
                long entryTime = zipEntry.getTime();

                if (log.isDebug())log.debug("Reading subclass mapping entry: " + entryName + ", version: " + entryVersion + ", timestamp: " + new java.util.Date(entryTime));

                if (Utilities.equals(version, entryVersion)) {
                    String pathname = entryName.substring(0, entryName.lastIndexOf("/"));
                    InputStream stream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                    Class<?>[] classes = (Class<?>[]) BeanUtilities.readObjectFromStream(stream);
                    tempMap.put(pathname, classes);
                } else {
                    log.info("Ignoring file entry: " + entryName + ", it is a different version");
                }
            }
            log.info("Read subclass mappings file: " + zipFileName + " in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
            classesInPathByClass = tempMap;
        } catch (Throwable t) {
            log.warn("Cached subclass mapping caused error.  Will rebuild cache.");
        }
    }
}