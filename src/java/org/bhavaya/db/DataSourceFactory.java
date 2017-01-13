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

package org.bhavaya.db;

import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates database connections.  Ensures that there is only a single database
 * connection of each type.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public class DataSourceFactory {
    private static final Log log = Log.getCategory(DataSourceFactory.class);

    private static volatile boolean initialised = false;
    private static final Object initLock = new Object();
    private static Map instances = new LinkedHashMap();
    private static String[] dataSourceNames;
    private static String defaultDataSourceName;
    private static Map databaseMetaDataMappings = new HashMap();

    public static void reinit() {
        synchronized (initLock) {
            initialised = false;
            instances.clear();
            dataSourceNames = null;
            defaultDataSourceName = null;
            databaseMetaDataMappings.clear();
        }
    }

    private static void init() {
        // Only run once
        synchronized (initLock) {
            if (initialised) return;
            initialised = true;

            org.bhavaya.util.PropertyGroup propertyGroup = ApplicationProperties.getApplicationProperties().getGroup("dataSources");
            if (propertyGroup != null) {
                org.bhavaya.util.PropertyGroup[] dataSourcesPropertyGroup = propertyGroup.getGroups("datasource");
                if (dataSourcesPropertyGroup != null) {
                    for (int i = 0; i < dataSourcesPropertyGroup.length; i++) {
                        org.bhavaya.util.PropertyGroup dataSourcePropertyGroup = dataSourcesPropertyGroup[i];
                        String dataSourceType = dataSourcePropertyGroup.getMandatoryProperty("type");

                        BhavayaDataSource dataSource;

                        try {
                            dataSource = (BhavayaDataSource) ClassUtilities.getClass(dataSourceType).newInstance();
                            dataSource.configure(dataSourcePropertyGroup);
                        } catch (Exception e) {
                            log.error(e);
                            throw new RuntimeException(e);
                        }
                        log.info("Created DataSource " + dataSource.getDataSourceName() + " of type " + dataSourceType);
                        instances.put(dataSource.getDataSourceName(), dataSource);
                    }
                }
                defaultDataSourceName = propertyGroup.getMandatoryProperty("defaultDataSource");
                if (instances.get(defaultDataSourceName) == null) {
                    throw new RuntimeException("Default DataSource does not exist");
                }
            }
            dataSourceNames = (String[]) instances.keySet().toArray(new String[instances.keySet().size()]);


            org.bhavaya.util.PropertyGroup databaseMetaDataPropertyGroup = ApplicationProperties.getApplicationProperties().getGroup("databaseMetaData");
            if (databaseMetaDataPropertyGroup != null) {
                org.bhavaya.util.PropertyGroup[] datasourceMappingPropertyGroups = databaseMetaDataPropertyGroup.getGroups("datasourceMapping");
                if (datasourceMappingPropertyGroups != null) {
                    for (int i = 0; i < datasourceMappingPropertyGroups.length; i++) {
                        org.bhavaya.util.PropertyGroup datasourceMappingPropertyGroup = datasourceMappingPropertyGroups[i];
                        String fromDatasource = datasourceMappingPropertyGroup.getProperty("fromDatasource");
                        String toDatasource = datasourceMappingPropertyGroup.getProperty("toDatasource");
                        log.info("Adding database metadata datasource mapping: " + fromDatasource + " -> " + toDatasource);
                        databaseMetaDataMappings.put(fromDatasource, toDatasource);
                    }
                }
            }
        }
    }

    public static BhavayaDataSource getInstance() {
        init();
        return getInstance(defaultDataSourceName);
    }

    public static BhavayaDataSource getInstance(String dataSourceName) {
        init();
        BhavayaDataSource instance = (BhavayaDataSource) instances.get(dataSourceName);
        if (instance == null) throw new RuntimeException("No datasource defined for: " + dataSourceName);
        return instance;
    }

    public static synchronized void closeAll() {
        init();
        for (Iterator iterator = instances.values().iterator(); iterator.hasNext();) {
            BhavayaDataSource dataSource = (BhavayaDataSource) iterator.next();
            dataSource.close();
        }
    }

    public static String getDefaultDataSourceName() {
        init();
        return defaultDataSourceName;
    }

    public static String[] getDataSourceNames() {
        init();
        return dataSourceNames;
    }

    public static String getDatabaseMetadataDatasource(String realDatasource) {
        init();
        String mappedDatasource = (String) databaseMetaDataMappings.get(realDatasource);
        if (mappedDatasource == null) return realDatasource;
        return mappedDatasource;
    }

    public static void main(String[] args) {
        try {
            DataSourceFactory.getInstance("database").getConnection();
            DataSourceFactory.getInstance("sqlbroadcasterDatabase").getConnection();
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public static BhavayaDataSource[] getInstances() {
        return (BhavayaDataSource[]) instances.values().toArray(new BhavayaDataSource[instances.values().size()]);
    }
}