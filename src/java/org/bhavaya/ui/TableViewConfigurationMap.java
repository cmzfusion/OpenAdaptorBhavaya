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

package org.bhavaya.ui;

import org.bhavaya.ui.view.TableView;
import org.bhavaya.ui.view.Workspace;
import org.bhavaya.util.*;

import javax.swing.*;
import java.beans.Encoder;
import java.beans.Expression;
import java.util.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.10 $
 */

public class TableViewConfigurationMap {
    private static final Log log = Log.getCategory(TableViewConfigurationMap.class);
    private static ArrayList mapSaveListeners = new ArrayList();

    public static interface ViewChangeListener {
        public void viewChanged(TableViewConfiguration tableViewConfiguration);
    }

    public static interface MapChangedListener {
        public void mapChanged();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Static

    static {
        BeanUtilities.addPersistenceDelegate(TableViewConfigurationMap.class, new BhavayaPersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                return new Expression(oldInstance, TableViewConfigurationMap.class, "new", new Object[]{((TableViewConfigurationMap) oldInstance).namedViewConfigMap});
            }

            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
            }
        });
    }

    private static final String VIEW_CONFIGURATIONS = "ViewConfigurations";
    private static final String TYPE_TO_NAMED_VIEWCONFIG_MAP = "TypeToNamedViewConfigMap";
    public static final String DEFAULT_CONFIG_ID = "Default";

    // TODO: Delete 20 February
    private static final String NAMED_VIEWCONFIG_MAP = "NamedViewConfigMap";

    // beanType:String -> TableViewConfigurationMap
    private static Map typeInstanceMap;


    /**
     * @deprecated class is a bit restrictive.  Use string keys instead.
     */
    public static synchronized TableViewConfigurationMap getInstance(Class type) {
        return getInstance(type.getName());
    }

    public static synchronized TableViewConfigurationMap getInstance(String key) {
        if (typeInstanceMap == null) {
            init();
        }

        // Now do the actual lookup
        TableViewConfigurationMap viewConfigurationMap = (TableViewConfigurationMap) typeInstanceMap.get(key);
        if (viewConfigurationMap == null) {
            viewConfigurationMap = new TableViewConfigurationMap(getDefaultNamedViewConfigMap());
            typeInstanceMap.put(key, viewConfigurationMap);
        }
        return viewConfigurationMap;
    }

    public static void init() {
        /* There's a bit of a migration issue here.  First we test for the new bug free way of loading up the Map
           of maps, then we resort to the old way.  Finally, there's the third case of a brand new config.
           TODO: Delete 20 February 2003
        */

        // Try the new way
        Map testMap = (Map) Configuration.getRoot().getConfiguration(VIEW_CONFIGURATIONS).getObject(TYPE_TO_NAMED_VIEWCONFIG_MAP, null, Map.class);
        if (testMap != null) {
            typeInstanceMap = testMap;
        }
        // Else its the old way (or new config), and this is a bit more tricky.
        else {
            // Test for the old way
            testMap = Configuration.getRoot().getConfiguration(VIEW_CONFIGURATIONS).getChildNodes();
            if (testMap.size() > 0) {
                typeInstanceMap = new HashMap();
                for (Iterator iterator = testMap.values().iterator(); iterator.hasNext();) {
                    Configuration oldConfigurationNode = (Configuration) iterator.next();
                    String typeName = oldConfigurationNode.getName();
                    Map namedViewConfigMap = (Map) oldConfigurationNode.getObject(NAMED_VIEWCONFIG_MAP, getDefaultNamedViewConfigMap(), Map.class);
                    typeInstanceMap.put(typeName, new TableViewConfigurationMap(namedViewConfigMap));
                }
            }
            // Else this is just a new/blank config so create the default
            else {
                typeInstanceMap = new HashMap();
            }
        }

        removeFixedViews(typeInstanceMap);

        Configuration.addSaveTask(new Task("View Configurations") {
            public void run() throws Task.AbortTaskException, Throwable {
                TableView.setSaveAll(false);
                for (Iterator iterator = mapSaveListeners.iterator(); iterator.hasNext();) {
                    Task task = (Task) iterator.next();
                    try {
                        task.run();
                    } catch (Throwable throwable) {
                        log.error("Error saving the view: " + task, throwable);
                        
                        final String DISCARD_VIEW = "Discard and Continue";
                        final String CANCEL = "Cancel";
                        final String[] options = new String[]{DISCARD_VIEW, CANCEL};
                        int option = JOptionPane.showOptionDialog((JFrame) Workspace.getInstance().getApplicationFrame(),
                                "There was an error saving the view: " + task + ".\nWould you like to discard the view" + " and continue saving, or cancel and try to correct the situation?",
                                "Error saving Table View",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                options,
                                DISCARD_VIEW);
                        if (option != 0) {
                            throw new Task.AbortTaskException();
                        }
                    }
                }
                Configuration.getRoot().getConfiguration(VIEW_CONFIGURATIONS).putObject(TYPE_TO_NAMED_VIEWCONFIG_MAP, typeInstanceMap);
            }
        });
    }

    /**
     * This provides some safety against invalid user configurations and is not strictly necessary.
     *
     * @param typedInstanceMap
     */
    private static void removeFixedViews(Map typedInstanceMap) {
        for (Iterator typeIterator = typedInstanceMap.entrySet().iterator(); typeIterator.hasNext();) {
            Map.Entry typedInstanceEntry = (Map.Entry) typeIterator.next();
            String typeName = (String) typedInstanceEntry.getKey();
            TableViewConfigurationMap viewConfigMap = (TableViewConfigurationMap) typedInstanceEntry.getValue();

            for (Iterator viewIterator = viewConfigMap.namedViewConfigMap.entrySet().iterator(); viewIterator.hasNext();) {
                Map.Entry viewEntry = (Map.Entry) viewIterator.next();
                String viewName = (String) viewEntry.getKey();
                if (FixedViewConfigurationMap.isFixedView(viewName)) {
                    log.info("Removing user view: " + viewName + " from type: " + typeName);
                    viewIterator.remove();
                }
            }
        }

    }

    public static Collection getAllTableViewConfigurations() {
        Set allViews = new HashSet();
        if (typeInstanceMap != null) {
            Iterator iterator = typeInstanceMap.values().iterator();
            while (iterator.hasNext()) {
                TableViewConfigurationMap tableViewConfMap = (TableViewConfigurationMap) iterator.next();
                for (String viewName : tableViewConfMap.getNames()) {
                    allViews.add(tableViewConfMap.getViewConfiguration(viewName));
                }
            }
        }
        return allViews;
    }

    public static void removeAllUserDefinedViewConfigurations() {
        for (Object o : typeInstanceMap.values()) {
            TableViewConfigurationMap viewConfigMap = (TableViewConfigurationMap) o;
            for (String viewName : viewConfigMap.getNames()) {
                if (!FixedViewConfigurationMap.isFixedView(viewName) && !viewName.equals(viewConfigMap.getDefaultViewConfigurationId())) {
                    viewConfigMap.removeViewConfiguration(viewName);
                }
            }
        }
    }

    public static void removeUserDefinedViewConfiguration(String type, String viewName) {
        TableViewConfigurationMap viewConfigMap = (TableViewConfigurationMap)typeInstanceMap.get(type);
        if(viewConfigMap != null) {
            if (viewConfigMap.getViewConfiguration(viewName) != null &&
                !FixedViewConfigurationMap.isFixedView(viewName) &&
                !viewName.equals(viewConfigMap.getDefaultViewConfigurationId())) {
                viewConfigMap.removeViewConfiguration(viewName);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Instance

    // name:String -> TableViewConfiguration
    private Map namedViewConfigMap;
    private String defaultViewConfigurationId;

    private List mapChangedListeners = new ArrayList();
    private Map viewChangeListeners = new HashMap();


    /**
     * Only for use by the persistence delegate.
     */
    public TableViewConfigurationMap(Map namedViewConfigMap) {
        this.namedViewConfigMap = namedViewConfigMap;
        defaultViewConfigurationId = DEFAULT_CONFIG_ID;
    }

    private static Map getDefaultNamedViewConfigMap() {
        Map map = new LinkedHashMap();
        map.put(DEFAULT_CONFIG_ID, new TableViewConfiguration());
        return map;
    }

    public static void addMapSaveTask(Task t) {
        mapSaveListeners.add(t);
    }

    public static void removeMapSaveTask(Task t) {
        mapSaveListeners.remove(t);
    }

    public TableViewConfiguration getViewConfiguration(String viewConfigurationId) {
        return (TableViewConfiguration) namedViewConfigMap.get(viewConfigurationId);
    }

    public String getDefaultViewConfigurationId() {
        if (!namedViewConfigMap.containsKey(defaultViewConfigurationId)) { // TODO: this works because a defaultViewConfiguration is not a fixed view
            defaultViewConfigurationId = DEFAULT_CONFIG_ID;
        }
        return defaultViewConfigurationId;
    }

    public void setViewConfiguration(String viewConfigurationId, TableViewConfiguration tableViewConfiguration, boolean fireChanged) {
        namedViewConfigMap.put(viewConfigurationId, tableViewConfiguration);
        if (fireChanged) {
            fireMapChanged();
            fireConfigChanged(viewConfigurationId);
        }
    }

    public void addConfigChangeListener(String configurationId, ViewChangeListener listener) {
        List listenerList = (List) viewChangeListeners.get(configurationId);
        if (listenerList == null) {
            listenerList = new ArrayList();
            viewChangeListeners.put(configurationId, listenerList);
        }
        listenerList.add(listener);
    }

    public void removeConfigChangeListener(String configurationId, ViewChangeListener listener) {
        List listenerList = (List) viewChangeListeners.get(configurationId);
        if (listenerList != null) {
            listenerList.remove(listener);
        }
    }

    public void addMapChangedListener(MapChangedListener listener) {
        mapChangedListeners.add(listener);
    }

    public void removeMapChangedListener(MapChangedListener listener) {
        mapChangedListeners.remove(listener);
    }

    public void removeViewConfiguration(String viewConfigurationId) {
        namedViewConfigMap.remove(viewConfigurationId);
        fireMapChanged();
    }

    public String[] getNames() {
        return (String[]) namedViewConfigMap.keySet().toArray(new String[namedViewConfigMap.keySet().size()]);
    }

    public void fireMapChanged() {
        for (Iterator iterator = mapChangedListeners.iterator(); iterator.hasNext();) {
            MapChangedListener listener = (MapChangedListener) iterator.next();
            listener.mapChanged();
        }
    }

    private void fireConfigChanged(String viewConfigurationId) {
        List listeners = (List) viewChangeListeners.get(viewConfigurationId);
        if (listeners != null) {
            //take a copy of the listener list so that the listeners can add and remove themselves without concurrent modifications
            Object[] listenerArray = listeners.toArray();
            for (int i = 0; i < listenerArray.length; i++) {
                ViewChangeListener listener = (ViewChangeListener) listenerArray[i];
                listener.viewChanged((TableViewConfiguration) namedViewConfigMap.get(viewConfigurationId));
            }
        }
    }
}
