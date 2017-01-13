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

package org.bhavaya.collection;

import org.bhavaya.beans.criterion.CriteriaBeanCollection;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.ui.UIUtilities;
import org.bhavaya.ui.dataset.BeanCollectionEditor;
import org.bhavaya.ui.dataset.CriteriaBeanCollectionEditor;
import org.bhavaya.ui.view.BeanCollectionTableView;
import org.bhavaya.ui.view.View;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.*;

import javax.swing.*;
import java.awt.*;
import java.beans.Encoder;
import java.beans.Statement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;


/**
 * Description
 *
 * @author Brendon McLean
 */
public class BeanCollectionGroup extends DefaultBeanCollection implements Comparable {
    private static final Log log = Log.getCategory(BeanCollectionGroup.class);

    private static final String DATASETS_CONFIG_KEY = "DataSets";
    private static final Map beanCollectionGroupsById = new LinkedHashMap();
    private static final Map defaultBeanCollectionGroupsByBeanType = new HashMap();
    private static final List enabledBeanCollectionGroupList = new ArrayList();
    private static Configuration configuration;
    private static volatile boolean initialised = false;
    private static final Object initLock = new Object();

    protected String id;
    private String displayName;
    private String pluralDisplayName;
    private Class beanType;
    private Class collectionType;
    private Class collectionViewClass;
    private Class collectionEditorClass;
    private boolean defaultViewForBeanType;
    protected boolean enabled;
    private boolean viewAllEnabled;
    private boolean addEnabled;
    private List nonPersistentBeanCollections = new ArrayList();
    private String defaultViewConfiguration;

    static {
        BeanUtilities.addPersistenceDelegate(BeanCollectionGroup.class, new BhavayaPersistenceDelegate(new String[]{"id", "defaultViewConfiguration"}) {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                BeanCollectionGroup oldBeanCollectionGroup = (BeanCollectionGroup) oldInstance;
                for (Iterator iterator = oldBeanCollectionGroup.iterator(); iterator.hasNext();) {
                    BeanCollection beanCollection = (BeanCollection) iterator.next();
                    if (!oldBeanCollectionGroup.isNonPersistentBeanCollection(beanCollection)) {
                        out.writeStatement(new Statement(oldInstance, "add", new Object[]{beanCollection}));
                    }
                }
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return (newInstance != null && oldInstance.getClass() == newInstance.getClass());
            }
        });
    }

    private static void init() {
        // Only run once
        synchronized (initLock) {
            if (initialised) return;
            initialised = true;

            configuration = Configuration.getRoot().getConfiguration(DATASETS_CONFIG_KEY);

            Configuration.addSaveTask(new Task("Collections") {
                public void run() {
                    for (Iterator iterator = beanCollectionGroupsById.values().iterator(); iterator.hasNext();) {
                        BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) iterator.next();
                        configuration.putObject(beanCollectionGroup.getId(), beanCollectionGroup);
                    }
                }
            });

            loadBeanCollectionGroups();
        }
    }

    private static void loadBeanCollectionGroups() {
        PropertyGroup beanCollectionGroupPropertyGroup = ApplicationProperties.getApplicationProperties().getGroup("beanCollectionGroups");
        PropertyGroup[] beanCollectionGroups = beanCollectionGroupPropertyGroup.getGroups();

        for (int i = 0; i < beanCollectionGroups.length; i++) {
            PropertyGroup propertyGroup = beanCollectionGroups[i];
            String id = "beanCollectionGroups." + propertyGroup.getName();
            BeanCollectionGroup beanCollectionGroup = null;

            Object configObject = configuration.getObject(id, null, BeanCollectionGroup.class);
            if (configObject != null) {
                beanCollectionGroup = (BeanCollectionGroup) configObject;
            } else {
                PropertyGroup group = ApplicationProperties.getApplicationProperties().getGroup(id);
                if (group != null) {
                    try {
                        Class beanCollectionGroupClass;
                        if (group.getProperty("beanCollectionGroupClass") != null) {
                            beanCollectionGroupClass = Class.forName(group.getProperty("beanCollectionGroupClass"));
                        } else {
                            beanCollectionGroupClass = BeanCollectionGroup.class;
                        }
                        Constructor constructor = beanCollectionGroupClass.getConstructor(new Class[]{String.class});
                        beanCollectionGroup = (BeanCollectionGroup) constructor.newInstance(new Object[]{id});
                    } catch (Exception e) {
                        log.error("Failed to create BeanCollectionGroup: " + e.getMessage(), e);
                    }
                }
            }

            if (beanCollectionGroup != null) {
                beanCollectionGroupsById.put(id, beanCollectionGroup);
                if (beanCollectionGroup.isDefaultViewForBeanType()) {
                    defaultBeanCollectionGroupsByBeanType.put(beanCollectionGroup.getBeanType(), beanCollectionGroup);
                }

                if (beanCollectionGroup.isEnabled()) {
                    enabledBeanCollectionGroupList.add(beanCollectionGroup);
//                    BeanCollection allBeanCollection = beanCollectionGroup.newBeanCollection(new CriterionGroup("All", Criterion.ALL_CRITERION));
//                    if (allBeanCollection != null) beanCollectionGroup.addNonPersistentBeanCollection(allBeanCollection);
                }
            }
        }

        Utilities.sort(enabledBeanCollectionGroupList);

        // BeanCollectionGroups may contain null if they contain an invalid BeanCollectionGroup, these can cause errors later so remove nulls
        // also apply any code migrations, this is where the BeanCollection class has a static migrateBeanCollection method
        for (Iterator iterator = enabledBeanCollectionGroupList.iterator(); iterator.hasNext();) {
            BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) iterator.next();
            beanCollectionGroup.remove(null);
            beanCollectionGroup.migrate();
        }
    }

    private void migrate() {
        for (int i = 0; i < size(); i++) {
            BeanCollection beanCollection = (BeanCollection) get(i);
            if (getCollectionType() != beanCollection.getClass()) {
                try {
                    Method migrateMethod = getCollectionType().getMethod("migrateBeanCollection", BeanCollection.class);
                    BeanCollection migratedBeanCollection = (BeanCollection) migrateMethod.invoke(getCollectionType(), beanCollection);
                    if (beanCollection != migratedBeanCollection) {
                        set(i, migratedBeanCollection);
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

    protected synchronized boolean add(Object value, boolean fireCommit, boolean fireCollectionChanged) {
        boolean added = super.add(value, false, false);
        if (added) {
            sort(fireCommit, fireCollectionChanged);
        }
        return added;
    }

    protected synchronized void add(int index, Object value, boolean fireCommit, boolean fireCollectionChanged) {
        super.add(index, value, fireCommit, fireCollectionChanged);
        sort(fireCommit, fireCollectionChanged);
    }

    private void sort(boolean fireCommit, boolean fireCollectionChanged) {
        Object a[] = toArray();
        clear(false, false);
        Arrays.sort(a);
        for (int i = 0; i < a.length; i++) {
            super.add(a[i], false, false);
        }
        if (fireCollectionChanged) fireCollectionChanged();
        if (fireCommit) fireCommit();
    }

    public static BeanCollectionGroup[] getEnabledInstances() {
        init();
        return (BeanCollectionGroup[]) enabledBeanCollectionGroupList.toArray(new BeanCollectionGroup[enabledBeanCollectionGroupList.size()]);
    }

    public static BeanCollectionGroup getInstance(String id) {
        init();
        BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) beanCollectionGroupsById.get(id);
        if (beanCollectionGroup.isEnabled()) return beanCollectionGroup;
        return null;
    }

    public static BeanCollectionGroup getDefaultInstance(Class beanType) {
        init();
        BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) defaultBeanCollectionGroupsByBeanType.get(beanType);

        if (beanCollectionGroup == null) {
            Class superBeanType = beanType.getSuperclass();
            while (beanCollectionGroup == null && superBeanType != null && !superBeanType.equals(Object.class)) {
                beanCollectionGroup = (BeanCollectionGroup) defaultBeanCollectionGroupsByBeanType.get(superBeanType);
                if (beanCollectionGroup == null) superBeanType = superBeanType.getSuperclass();
            }

            if (beanCollectionGroup != null) {
                defaultBeanCollectionGroupsByBeanType.put(superBeanType, beanCollectionGroup);
            } else {
                beanCollectionGroup = new BeanCollectionGroup(null,
                        beanType,
                        ClassUtilities.getDisplayName(beanType),
                        Utilities.getPluralName(ClassUtilities.getDisplayName(beanType)),
                        CriteriaBeanCollection.class,
                        BeanCollectionTableView.class,
                        CriteriaBeanCollectionEditor.class,
                        true,
                        true);
                defaultBeanCollectionGroupsByBeanType.put(beanType, beanCollectionGroup);
            }
        }

        if (beanCollectionGroup.isEnabled()) return beanCollectionGroup;
        return null;
    }

    private BeanCollectionGroup(String id,
                                Class beanType,
                                String displayName,
                                String pluralDisplayName,
                                Class collectionType,
                                Class collectionViewClass,
                                Class collectionEditorClass,
                                boolean defaultViewForBeanType,
                                boolean enabled) {
        super(beanType);
        this.id = id;
        this.beanType = beanType;
        this.displayName = displayName;
        this.pluralDisplayName = pluralDisplayName;
        this.collectionType = collectionType;
        this.collectionViewClass = collectionViewClass;
        this.collectionEditorClass = collectionEditorClass;
        this.defaultViewForBeanType = defaultViewForBeanType;
        this.enabled = enabled;
    }

    public BeanCollectionGroup(String id) {
        this(id, null);
    }

    public BeanCollectionGroup(String id, String defaultViewConfiguration) {
        super(BeanCollection.class);
        this.id = id;

        PropertyGroup propertyGroup = ApplicationProperties.getApplicationProperties().getGroup(id);
        if (propertyGroup == null) {
            enabled = false;
            return;
        }

        String beanTypeName = propertyGroup.getProperty("beanType");
        this.beanType = convertStringToClass(id, beanTypeName, true);
        if (beanType == null) return;

        this.displayName = propertyGroup.getProperty("displayName");
        if (displayName == null) displayName = ClassUtilities.getDisplayName(beanType);
        this.pluralDisplayName = propertyGroup.getProperty("pluralDisplayName");
        if (pluralDisplayName == null) pluralDisplayName = displayName;

        this.collectionType = convertStringToClass(id, propertyGroup.getProperty("collectionType"), false);
        if (collectionType == null) collectionType = CriteriaBeanCollection.class;
        this.collectionViewClass = convertStringToClass(id, propertyGroup.getProperty("collectionViewClass"), false);
        if (collectionViewClass == null) collectionViewClass = BeanCollectionTableView.class;
        this.collectionEditorClass = convertStringToClass(id, propertyGroup.getProperty("collectionEditorClass"), false);
        if (collectionEditorClass == null) collectionEditorClass = CriteriaBeanCollectionEditor.class;

        String defaultString = propertyGroup.getMandatoryProperty("default");
        defaultViewForBeanType = defaultString.equals("true");
        this.defaultViewConfiguration = defaultViewConfiguration;
        String viewAllEnabled = propertyGroup.getProperty("viewAllEnabled");
        if (viewAllEnabled == null) {
            this.viewAllEnabled = true;
        } else {
            this.viewAllEnabled = Boolean.valueOf(viewAllEnabled).booleanValue();
        }

        String addEnabled = propertyGroup.getProperty("addEnabled");
        this.addEnabled = addEnabled == null || Boolean.valueOf(addEnabled).booleanValue();

        String enabledString = propertyGroup.getProperty("enabled");
        enabled = enabledString != null && enabledString.length() > 0 ? enabledString.equals("true") : true; //default to true, if not set
    }

    private Class convertStringToClass(String id, String className, boolean noClassIsError) {
        Class aClass = className != null ? ClassUtilities.getClass(className, false, false) : null;
        if (noClassIsError && aClass == null) {
            enabled = false;
            log.error("Could not find class: " + className + " for BeanCollectionGroup: " + id);
        }
        return aClass;
    }

    private void addNonPersistentBeanCollection(BeanCollection beanCollection) {
        add(beanCollection);
        nonPersistentBeanCollections.add(beanCollection);
    }

    public boolean isNonPersistentBeanCollection(BeanCollection beanCollection) {
        return nonPersistentBeanCollections.contains(beanCollection);
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDefaultViewForBeanType() {
        return defaultViewForBeanType;
    }

    public Class getBeanType() {
        return beanType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPluralDisplayName() {
        return pluralDisplayName;
    }

    public Class getCollectionType() {
        return collectionType;
    }

    public Class getCollectionViewClass() {
        return collectionViewClass;
    }

    public Class getCollectionEditorClass() {
        return collectionEditorClass;
    }

    public String getDefaultViewConfiguration() {
        return defaultViewConfiguration;
    }

    public void setDefaultViewConfiguration(String defaultViewConfiguration) {
        this.defaultViewConfiguration = defaultViewConfiguration;
    }

    public BeanCollection editBeanCollection(BeanCollection beanCollection, JComponent owner, String title) {
        if (!enabled) {
            log.error("Cannot display editor for: " + id + " as it is disabled");
        }

        try {
            Window window = UIUtilities.getWindowParent(owner);
            Class windowType = window instanceof Frame ? Frame.class : Dialog.class;
            Constructor editorClassConstructor = collectionEditorClass.getConstructor(new Class[]{Class.class, collectionType, windowType, String.class});
            BeanCollectionEditor beanCollectionEditor = (BeanCollectionEditor) editorClassConstructor.newInstance(new Object[]{beanType, beanCollection, window, title});
            return beanCollectionEditor.editBeanCollection();
        } catch (Exception e) {
            log.error("Unable to display editor for: " + id, e);
        }
        return null;
    }

    public void viewBeanCollectionAsTable(String viewName, String viewTabTitle, String viewFrameTitle, BeanCollection beanCollection) {
        viewBeanCollectionAsTable(viewName, viewTabTitle, viewFrameTitle, beanCollection, null);
    }

    public void viewBeanCollectionAsTable(String viewName, String viewTabTitle, String viewFrameTitle, BeanCollection beanCollection, String viewConfig) {
        if (!enabled) {
            log.error("Cannot display view for: " + id + " as it is disabled");
        }

        try {
            Constructor viewClassConstructor = collectionViewClass.getConstructor(new Class[]{String.class, String.class, String.class, Class.class, String.class, BeanCollection.class});
            View newView = (View) viewClassConstructor.newInstance(new Object[]{viewName, viewTabTitle, viewFrameTitle, beanCollection.getType(),
                                                                                viewConfig == null ? defaultViewConfiguration : viewConfig, beanCollection});

            // Only new BCTV's should have this propery set.
            if (newView instanceof BeanCollectionTableView) {
                BeanCollectionTableView beanCollectionTableView = (BeanCollectionTableView) newView;
                beanCollectionTableView.setCheckRowCount(true);
            }

            Workspace.getInstance().displayView(newView);
        } catch (Exception e) {
            log.error("Unable to display view for: " + id, e);
        }
    }

    public BeanCollection newBeanCollection(CriterionGroup criterionGroup) {
        if (!enabled) {
            log.error("Cannot create BeanCollection for: " + id + " as it is disabled");
        }

        try {
            Constructor collectionTypeConstructor = collectionType.getConstructor(new Class[]{Class.class, CriterionGroup.class});
            return (BeanCollection) collectionTypeConstructor.newInstance(new Object[]{beanType, criterionGroup});
        } catch (Exception e) {
            log.error("Unable to create beanCollection for: " + id + " using constructor (Class, CriterionGroup)", e);
        }
        return null;
    }

    public boolean isViewAllable() {
        try {
            // assume if the collectionType has a CriterionGroup constructor, can do view all on it.
            collectionType.getConstructor(new Class[]{Class.class, CriterionGroup.class});
            return viewAllEnabled;
        } catch (Exception e) {
        }
        return false;
    }

    public boolean isAddEnabled() {
        return addEnabled;
    }

    public String toString() {
        return pluralDisplayName;
    }

    public int compareTo(Object o) {
        if (o instanceof BeanCollectionGroup) {
            BeanCollectionGroup beanCollectionGroup = (BeanCollectionGroup) o;
            return ToStringComparator.CASE_INSENSITIVE_COMPARATOR.compare(pluralDisplayName, beanCollectionGroup.pluralDisplayName);
        }
        return -1;
    }
}