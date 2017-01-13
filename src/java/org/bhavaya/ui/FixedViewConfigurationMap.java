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

import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.IOUtilities;
import org.bhavaya.util.Log;

import java.io.InputStream;
import java.util.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.13 $
 */
public class FixedViewConfigurationMap {
    private static final Log log = Log.getCategory(FixedViewConfigurationMap.class);

    private static final String FIXED_VIEW_DIRECTORY = "fixedViews";

    private static Map typeInstanceMap;
    public static final String PREFIX = ".#!";

    public static synchronized FixedViewConfigurationMap getInstance(Class type) {
        return getInstance(type.getName());
    }

    public static synchronized FixedViewConfigurationMap getInstance(String key) {
        if (typeInstanceMap == null) {
            init();
        }

        // Now do the actual lookup
        FixedViewConfigurationMap viewConfigurationMap = (FixedViewConfigurationMap) typeInstanceMap.get(key);
        if (viewConfigurationMap == null) {
            viewConfigurationMap = new FixedViewConfigurationMap(Collections.EMPTY_MAP);
            typeInstanceMap.put(key, viewConfigurationMap);
        }
        return viewConfigurationMap;
    }

    static void init() {
        typeInstanceMap = new HashMap();

        String[] subDirectories = IOUtilities.getResourceSubDirectoriesInDir(FIXED_VIEW_DIRECTORY);
        for (int i = 0; i < subDirectories.length; i++) {
            addConfigsInSubDirectory(subDirectories[i]);
        }
    }

    private static void addConfigsInSubDirectory(String subDirectory) {
        String[] matchingFiles = IOUtilities.getResourceFilesInDir(FIXED_VIEW_DIRECTORY + "/" + subDirectory);

        HashMap instance = new HashMap();
        for (int i = 0; i < matchingFiles.length; i++) {
            if (matchingFiles[i].toLowerCase().endsWith(".xml")) {
                String viewConfigName = matchingFiles[i].substring(0, matchingFiles[i].length() - 4);
                String resourceName = FIXED_VIEW_DIRECTORY + "/" + subDirectory + "/" + matchingFiles[i];
                if (log.isDebug()) log.debug("Loading fixed view: " + resourceName);
                InputStream resourceStream = IOUtilities.getResourceAsStream(resourceName);
                if (resourceStream != null) {
                    TableViewConfiguration viewConfiguration = (TableViewConfiguration) BeanUtilities.readObjectFromStream(resourceStream);
                    instance.put(PREFIX + viewConfigName, viewConfiguration);
                } else {
                    log.error("Cannot find fixed view for resource: " + resourceName + " in subdirectory: " + subDirectory);
                }
            }
        }
        if (log.isDebug())log.debug("Loaded " + instance.size() + " fixed views for " + subDirectory);

        typeInstanceMap.put(subDirectory, new FixedViewConfigurationMap(instance));
    }

    public static Collection getAllTableViewConfigurations() {
        Set allViews = new HashSet();
        if (typeInstanceMap != null) {
            Iterator iterator = typeInstanceMap.values().iterator();
            while (iterator.hasNext()) {
                FixedViewConfigurationMap tableViewConfMap = (FixedViewConfigurationMap) iterator.next();
                for (String viewName : tableViewConfMap.getNames()) {
                    allViews.add(tableViewConfMap.getViewConfiguration(viewName));
                }
            }
        }
        return allViews;
    }

    private Map namedViewConfigMap;

    public FixedViewConfigurationMap(Map namedViewConfigMap) {
        this.namedViewConfigMap = namedViewConfigMap;
    }

    public TableViewConfiguration getViewConfiguration(String viewConfigurationId) {
        return (TableViewConfiguration) namedViewConfigMap.get(viewConfigurationId);
    }

    public String[] getNames() {
        return (String[]) namedViewConfigMap.keySet().toArray(new String[namedViewConfigMap.keySet().size()]);
    }

    public static boolean isFixedView(String viewConfigurationId) {
        return viewConfigurationId != null && viewConfigurationId.startsWith(PREFIX);
    }

    public static String getFilename(Class recordType, String viewConfigurationId) {
        return recordType.getName() + "." + viewConfigurationId + ".xml";
    }

    public static String getDisplayNameForViewId(String selectedViewConfigurationId) {
        return selectedViewConfigurationId.substring(PREFIX.length());
    }

    public static String getInternalNameForViewId(String viewId) {
        return PREFIX + viewId;
    }
}
