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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Configuration is a persistable configuration data store that is organised into a tree hierarchy to
 * help with namespace conflicts.  The class is basically a hashtable of hashtables which get persisted to a
 * single file.
 *
 * @author Brendon McLean
 * @version $Revision: 1.25 $
 */
public class Configuration {
    // A few default configuration files.
    public static final String STARTUP = "startup";
    public static final String DEFAULT = "config";

    private static Log log = Log.getCategory(Configuration.class);
    private static Log primaryLoadingLog = Log.getPrimaryLoadingLog();
    private static Log secondaryLoadingLog = Log.getSecondaryLoadingLog();

    private static final String CONFIG_MIGRATION_STRATEGY_FILE = "configMigrationStrategies.xml";
    private static final String CONFIG_MIGRATION_STRATEGIES = "strategy";
    private static final String VERSION_TARGET = "versionTarget";
    private static final String STRATEGY_CLASS = "strategyClass";
    private static final String ARGUMENTS = "arguments";

    private static final Map instanceMap = new HashMap();
    private static final Map loadedFilesByConfigName = new HashMap();
    private static final List saveTasks = new ArrayList();
    private static final SortedMap configMigrations = new TreeMap(); // sorted by migration number
    private static final List configurationSources = new ArrayList();
    private static final List configurationSinks = new ArrayList();
    private static final Set loadStack = new HashSet();
    private static long expectedVersion;

    private static String migrationStrategyFile = null;

    private static boolean inited = false;

    private String name;
    private long versionNumber = -1;
    private Configuration parent;
    private Map childNodes = new HashMap();
    private Map data = new LinkedHashMap();

    private static void staticInit() {
        if (!inited) {
            inited = true;
            FileConfigurationSourceSink.migrateShivaToBhavaya(); // a once-off migration
        }
    }

    public static final synchronized void configureDefaultSourcesAndSinks() {
        setMigrationStrategyFile(IOUtilities.RESOURCE_DIR + "/" + CONFIG_MIGRATION_STRATEGY_FILE);
        FileConfigurationSourceSink fileConfigurationSourceSink = new FileConfigurationSourceSink(new String[]{IOUtilities.getUserConfigDirectory()});
        addConfigurationSource(fileConfigurationSourceSink);
        addConfigurationSource(new ClasspathConfigurationSource());

        addConfigurationSink(fileConfigurationSourceSink);

        loadConfigMigrations();
    }

    public static final synchronized void addConfigurationSource(ConfigurationSource configurationSource) {
        configurationSources.add(configurationSource);
    }

    public static final synchronized void removeConfigurationSource(ConfigurationSource configurationSource) {
        configurationSources.remove(configurationSource);
    }

    public static final synchronized void addConfigurationSink(ConfigurationSink configurationSink) {
        configurationSinks.add(configurationSink);
    }

    public static final synchronized void removeConfigurationSink(ConfigurationSink configurationSink) {
        configurationSinks.remove(configurationSink);
    }

    public static final synchronized void clearConfigurationSourcesAndSinks() {
        configurationSources.clear();
        configurationSinks.clear();
    }

    private static void loadConfigMigrations() {
        if (migrationStrategyFile == null) return;
        try {
            Enumeration urls = Configuration.class.getClassLoader().getResources(migrationStrategyFile);
            while (urls.hasMoreElements()) {
                URL url = (URL) urls.nextElement();
                InputStream stream = null;
                try {
                    stream = url.openStream();
                    PropertyGroup[] versions = ApplicationProperties.createRootPropertyGroup(stream, null, 0).getGroups(CONFIG_MIGRATION_STRATEGIES);

                    for (int i = 0; i < versions.length; i++) {
                        PropertyGroup strategy = versions[i];
                        Long versionTarget = Long.valueOf(strategy.getProperty(VERSION_TARGET));
                        String className = strategy.getProperty(STRATEGY_CLASS);
                        String[] constructorArguments = strategy.getProperties(ARGUMENTS);
                        List configMigrationForVersionTarget = (List) configMigrations.get(versionTarget);
                        if (configMigrationForVersionTarget == null) {
                            configMigrationForVersionTarget = new ArrayList();
                            configMigrations.put(versionTarget, configMigrationForVersionTarget);
                        }
                        ConfigMigrationStategy configMigration = createMigrationStrategy(versionTarget, className, constructorArguments);
                        configMigrationForVersionTarget.add(configMigration);
                    }
                } finally {
                    IOUtilities.closeStream(stream);
                }
            }
            if (configMigrations.size() > 0) {
                expectedVersion = ((Long) configMigrations.lastKey()).longValue();
                log.info("Expected config Version number: " + expectedVersion);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    private static ConfigMigrationStategy createMigrationStrategy(Long versionTarget, String className, String[] constructorArguments) {
        try {
            Object[] arguments = new Object[]{versionTarget, constructorArguments};
            ConfigMigrationStategy stategy = (ConfigMigrationStategy) ClassUtilities.getClass(className).getConstructor(new Class[]{long.class, String[].class}).newInstance(arguments);
            return stategy;
        } catch (Exception e) {
            throw new RuntimeException("Error loading strategy delegates for config migration", e);
        }
    }

    public static synchronized String[] getConfigRootNames() {
        return (String[]) instanceMap.keySet().toArray(new String[instanceMap.keySet().size()]);
    }

    public synchronized static Configuration getRoot(String name) {
        staticInit();
        Configuration configuration = (Configuration) instanceMap.get(name);
        if (configuration == null) {
            configuration = load(name);
            if (configuration == null) {
                log.info("Creating empty " + name + " configuration");
                configuration = new Configuration(null, name);
            }

            instanceMap.put(name, configuration);
        }
        return configuration;
    }

    public synchronized static Configuration getRoot() {
        return getRoot(DEFAULT);
    }

    public static String getConfigXMLString(String configName) throws IOException {
        String loadedFile = getLoadedConfigFileName(configName);
        if (loadedFile == null) return null;
        return IOUtilities.readFile(loadedFile);
    }

    public static String getLoadedConfigFileName(String configName) {
        return (String) loadedFilesByConfigName.get(configName);
    }

    private static Configuration load(String configName) {
        primaryLoadingLog.info("Loading configuration");
        secondaryLoadingLog.info(" ");

        if (loadStack.contains(configName)) {
            log.error(new RuntimeException("Infinite recursion detected while loading " + configName + " configuration"));
            return null; // prevent inifinite recursion
        }

        loadStack.add(configName);

        Configuration configuration = null;

        for (Iterator iterator = configurationSources.iterator(); iterator.hasNext();) {
            ConfigurationSource configurationSource = (ConfigurationSource) iterator.next();
            configuration = configurationSource.loadConfiguration(configName, expectedVersion);
            if (configuration != null) return configuration;
        }

        return configuration;
    }

    public String convertToString() {
        if (getVersionNumber() == -1) setVersionNumber(expectedVersion); // for usage outside of this class

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1000);
        XMLEncoder encoder = BeanUtilities.getDefaultEncoder(byteArrayOutputStream);
        BeanUtilities.writeObjectToStream(new Long(getVersionNumber()), encoder);
        BeanUtilities.writeObjectToStream(this, encoder);
        encoder.close();
        String configurationString = new String(byteArrayOutputStream.toByteArray());
        return configurationString;
    }

    private static Configuration stringToConfiguration(String configName, long version, long actualVersion, String configurationString) throws Exception {
        if (actualVersion < 0) { // source cannot determine actualVersion without loading config, so determine from config
            XMLDecoder decoder = BeanUtilities.getDefaultDecoder(new ByteArrayInputStream(configurationString.getBytes()), false);
            actualVersion = ((Number) decoder.readObject()).longValue();
            decoder.close();
        }

        if (actualVersion < version) {
            configurationString = patchConfig(configName, actualVersion, version, configurationString);
        }

        XMLDecoder decoder = BeanUtilities.getDefaultDecoder(new ByteArrayInputStream(configurationString.getBytes()), true);
        decoder.readObject(); // read actualVersion
        Configuration configuration = (Configuration) decoder.readObject();
        decoder.close();
        return configuration;
    }

    private static String patchConfig(String configName, long fromVersion, long toVersion, String configurationString) {
        SortedMap migrationsToRun = configMigrations.tailMap(new Long(fromVersion)); // this is inclusive of the fromVersion

        for (Iterator iterator = migrationsToRun.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Long version = (Long) entry.getKey();
            if (version.longValue() > toVersion) break;
            if (version.longValue() > fromVersion) {
                List configMigrationForVersion = (List) entry.getValue();
                for (Iterator iterator2 = configMigrationForVersion.iterator(); iterator2.hasNext();) {
                    ConfigMigrationStategy configMigration = (ConfigMigrationStategy) iterator2.next();
                    configurationString = configMigration.migrate(configName, configurationString);
                }
            }
        }
        if (log.isDebug()) log.debug("Patched " + configName + " from " + fromVersion
                + " to " + toVersion + " results in:\n" + configurationString);
        return configurationString;
    }

    public static final synchronized void addSaveTask(Task saveTask) {
        saveTasks.add(saveTask);
    }

    public static final synchronized void removeSaveTask(Task saveTask) {
        saveTasks.remove(saveTask);
    }

    public static final Task[] getSaveTasks() {
        staticInit();
        Task[] saveTasksSnapshot;

        synchronized (Configuration.class) {
            Task initialTask = new Task("Clear original configuration...") {
                public void run() throws Throwable {
                    synchronized (Configuration.class) {
                        log.info("Saving configuration");
                        // clear old config before saving new config
                        for (Iterator iterator = instanceMap.entrySet().iterator(); iterator.hasNext();) {
                            Map.Entry entry = (Map.Entry) iterator.next();
                            Configuration configuration = (Configuration) entry.getValue();
                            clearData(configuration);
                        }
                    }
                }
            };

            // add this to existing save tasks so if they fail, this is not executed
            Task saveTask = new Task("Saving configuration...") {
                public void run() throws Throwable {
                    synchronized (Configuration.class) {
                        for (Iterator iterator = instanceMap.entrySet().iterator(); iterator.hasNext();) {
                            Map.Entry entry = (Map.Entry) iterator.next();
                            String configName = (String) entry.getKey();
                            Configuration configuration = (Configuration) entry.getValue();
                            saveConfiguration(configName, configuration);
                            Log.getUserCategory().info(configName + " configuration saved");
                        }
                    }
                }
            };


            saveTasksSnapshot = new Task[saveTasks.size() + 2];
            saveTasksSnapshot[0] = initialTask;

            int i = 1;
            for (Iterator iterator = saveTasks.iterator(); iterator.hasNext();) {
                Task task = (Task) iterator.next();
                saveTasksSnapshot[i] = task;
                i++;
            }
            saveTasksSnapshot[saveTasksSnapshot.length - 1] = saveTask;
        }
        return saveTasksSnapshot;
    }

    private static void clearData(Configuration configuration) {
        configuration.getData().clear();
        Iterator childIter = configuration.childNodes.values().iterator();
        while (childIter.hasNext()) {
            Configuration childConfiguration = (Configuration) childIter.next();
            clearData(childConfiguration);
        }
    }

    public static void saveConfiguration(String configName, Configuration configuration) {
        configuration.setVersionNumber(expectedVersion);
        configuration.setName(configName);

        for (Iterator iterator = configurationSinks.iterator(); iterator.hasNext();) {
            ConfigurationSink configurationSink = (ConfigurationSink) iterator.next();
            configurationSink.saveConfiguration(configuration);
        }
    }

    public static void save() throws Task.AbortTaskException, Throwable {
        Task[] saveTasks = Configuration.getSaveTasks();
        for (int i = 0; i < saveTasks.length; i++) {
            saveTasks[i].run();
        }
    }


    public Configuration() {
    }

    private Configuration(Configuration parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public Configuration getConfiguration(String key) {
        Configuration config = (Configuration) childNodes.get(key);

        if (config == null) {
            childNodes.put(key, config = new Configuration(this, key));
        }

        return config;
    }

    public <T> T getObject(String key, T defaultObject, Class<T> expectedClassType) {
        Object returnValue = data.get(key) != null ? data.get(key) : defaultObject;
        if (returnValue != null && !expectedClassType.isInstance(returnValue)) {
            log.warn("Detected imminent class cast mismatch; reverting to default value. (" + getFullyQualifiedName() + ") was expecting " + expectedClassType + " but got " + returnValue.getClass());
            return defaultObject;
        }
        return (T) returnValue;
    }

    public Object getObjectAndRemove(String key, Object defaultObject, Class expectedClassType) {
        Object returnValue = data.get(key) != null ? data.remove(key) : defaultObject;
        if (returnValue != null && !expectedClassType.isInstance(returnValue)) {
            log.warn("Detected imminent class cast mismatch; reverting to default value. (" + getFullyQualifiedName() + ") was expecting " + expectedClassType + " but got " + returnValue.getClass());
            return defaultObject;
        }
        return returnValue;
    }

    private StringBuffer getFullyQualifiedName() {
        String name = this.name != null ? this.name : "";
        return parent != null ? parent.getFullyQualifiedName().append("/").append(name) : new StringBuffer(name);
    }

    public void putObject(String key, Object object) {
        data.put(key, object);
    }

    public Map getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(Map childNodes) {
        this.childNodes = childNodes;
    }

    public Configuration getParent() {
        return parent;
    }

    public void setParent(Configuration parent) {
        this.parent = parent;
    }

    public Map getData() {
        return data;
    }

    public void setData(Map data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(long versionNumber) {
        this.versionNumber = versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public static long getExpectedVersion() {
        return expectedVersion;
    }

    private static class ConfigFileKey {
        private String name;
        private long version;

        public ConfigFileKey(String name, long version) {
            this.name = name;
            this.version = version;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConfigFileKey)) return false;

            final ConfigFileKey configFileKey = (ConfigFileKey) o;

            if (version != configFileKey.version) return false;
            if (!name.equals(configFileKey.name)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = name.hashCode();
            result = 29 * result + (int) (version ^ (version >>> 32));
            return result;
        }

        public String toString() {
            return name + "/" + version;
        }
    }

    public static interface ConfigurationSource {
        public Configuration loadConfiguration(String configName, long version);
    }

    public static interface ConfigurationSink {
        public boolean saveConfiguration(Configuration configuration);
    }

    public static class FileConfigurationSourceSink implements ConfigurationSource, ConfigurationSink {
        private String[] directories;

        public FileConfigurationSourceSink(String[] directories) {
            this.directories = directories;
        }

        public Configuration loadConfiguration(String configName, long version) {
            for (int i = 0; i < directories.length; i++) {
                String directory = directories[i];
                Configuration configuration = loadConfiguration(configName, version, directory);
                if (configuration != null) return configuration;
            }
            return null;
        }

        private Configuration loadConfiguration(String configName, long version, String directory) {
            log.info("Searching for " + configName + " configuration in: " + directory);

            Configuration configuration = null;
            List validVersions = new ArrayList(configMigrations.keySet());
            validVersions.add(0, new Integer(0));
            for (int i = validVersions.size(); i > 0 && configuration == null; i--) {
                long aVersion = ((Number) validVersions.get(i - 1)).longValue();

                if (expectedVersion >= aVersion) {
                    ConfigFileKey configFileKey = new ConfigFileKey(configName, aVersion);
                    InputStream configInputStream = findFileInDir(directory, configFileKey);
                    try {
                        configuration = loadConfiguration(configInputStream, configName, version, aVersion);
                    } catch (Exception e) {
                        log.error("Error loading " + configFileKey + " configuration", e);
                        return null;
                    }
                }
            }
            return configuration;
        }

        private static Configuration loadConfiguration(InputStream inputStream, String configName, long version, long actualVersion) throws Exception {
            if (inputStream == null) return null;
            String configurationString = IOUtilities.convertStreamToString(inputStream);
            return stringToConfiguration(configName, version, actualVersion, configurationString);
        }

        public static Configuration loadConfiguration(InputStream inputStream) throws Exception {
            if (inputStream == null) return null;
            String configurationString = IOUtilities.convertStreamToString(inputStream);
            return stringToConfiguration(null, expectedVersion, -1, configurationString);
        }

        private static InputStream findFileInDir(String directory, ConfigFileKey configFileKey) {
            InputStream configInputStream = findFile(directory + "/" + configFileKey.name + "." + ApplicationInfo.getInstance().getEnvironmentId() + "." + configFileKey.version + ".xml", configFileKey);
            if (configInputStream != null) return configInputStream;

            configInputStream = findFile(directory + "/" + configFileKey.name + "." + configFileKey.version + ".xml", configFileKey);
            if (configInputStream != null) return configInputStream;

            return configInputStream;
        }

        private static InputStream findFile(String filename, ConfigFileKey configFileKey) {
            InputStream configInputStream = null;

            if (fileExists(filename)) {
                log.info("Found configuration file: " + filename);
                secondaryLoadingLog.info(filename);
                try {
                    configInputStream = new FileInputStream(filename);
                    loadedFilesByConfigName.put(configFileKey.name, filename);
                } catch (FileNotFoundException e) {
                    log.warn("Could not load configuration file: " + filename, e);
                }
            } else {
                if (log.isDebug())log.debug("Could not find configuration file: " + filename);
            }

            return configInputStream;
        }

        private static boolean fileExists(String filename) {
            return new File(filename).exists();
        }

        public boolean saveConfiguration(Configuration configuration) {
            boolean successful = true;

            for (int i = 0; i < directories.length; i++) {
                String directory = directories[i];
                if (!saveConfiguration(configuration.getName(), configuration, directory)) {
                    successful = false;
                }
            }

            return successful;
        }

        private static boolean saveConfiguration(String configName, Configuration configuration, String directory) {
            long version = configuration.getVersionNumber();
            String configFileName = directory + "/" + configName + "." + ApplicationInfo.getInstance().getEnvironmentId() + "." + version + ".xml";
            return saveConfiguration(configFileName, configuration);
        }

        public static boolean saveConfiguration(String configFileName, Configuration configuration) {
            File configFile = new File(configFileName);
            File backupFile = new File(configFileName + ".bak");
            File tempConfigFile = null;
            String configurationString = configuration.convertToString();

            try {
                if (!configFile.getParentFile().exists()) {
                    log.info("Creating directory: " + configFile.getParentFile());
                    configFile.getParentFile().mkdirs();
                }

                if (log.isDebug()) log.debug("About to create temp file");
                tempConfigFile = File.createTempFile("tempConfig", ".xml", configFile.getParentFile());
                tempConfigFile.deleteOnExit();

                if (log.isDebug()) log.debug("About to write: " + tempConfigFile);
                IOUtilities.writeStringToFile(tempConfigFile, configurationString);
                if (log.isDebug()) log.debug("Written: " + tempConfigFile);

                if (log.isDebug()) log.debug("About to delete " + backupFile);
                backupFile.delete();

                if (log.isDebug()) log.debug("About to rename old config: " + configFile + " to " + backupFile);
                configFile.renameTo(backupFile);

                if (log.isDebug()) log.debug("About to rename temp config: " + tempConfigFile + " to " + configFile);
                if (!tempConfigFile.renameTo(configFile)) {
                    throw new IOException("Unable to rename temp config: " + tempConfigFile + " to new config: " + configFile);
                }
            } catch (IOException e) {
                log.error("Unable to write: " + configFile, e);      
                return false;
            } finally {
                if (tempConfigFile != null && tempConfigFile.exists()) {
                    /*Yes this looks strange, but if there's been a problem closing
                    the file, then the underlying outputstream is not closed
                    which means that the file cannot be deleted because the stream
                    still has a lock on it.  I've submitted a bug report to Sun about
                    this.  One easy way to hit the bug if to have a full hard drive
                     - saving the configuration then results in the temporary file not
                    being cleaned up.*/ 
                    System.gc();
                    tempConfigFile.delete();
                 }
            }
            return true;
        }

        public static void migrateShivaToBhavaya() {
            String newConfigDirName = IOUtilities.getUserConfigDirectory();
            File newConfigDir = new File(IOUtilities.getUserConfigDirectory());

            if (!newConfigDir.exists() && newConfigDirName.contains(IOUtilities.CONFIG_BASE_DIR)) {
                String oldConfigDirName = IOUtilities.getOldUserConfigDirectory();
                if (oldConfigDirName != null) {
                    File oldConfigDir = new File(oldConfigDirName);
                    try {
                        log.info("Copying " + oldConfigDir + " to " + newConfigDir);
                        IOUtilities.copy(oldConfigDir, newConfigDir, true);
                    } catch (IOException e) {
                        log.error(e);
                    }

                    File[] configFiles = newConfigDir.listFiles();
                    for (int i = 0; i < configFiles.length; i++) {
                        try {
                            File configFile = configFiles[i];
                            if (configFile.isFile()) {
                                String configFileString = IOUtilities.readFile(configFile);
                                log.info("Bootstrap config migration for " + configFile);
                                String newConfigFileString = configFileString.replaceAll("com\\.drkw\\.shiva\\.util\\.Configuration", "org.bhavaya.util.Configuration");
                                IOUtilities.writeStringToFile(configFile, newConfigFileString);
                            }
                        } catch (IOException e) {
                            log.error(e);
                        }
                    }
                }
            }
        }
    }

    public static class ClasspathConfigurationSource implements ConfigurationSource {
        public Configuration loadConfiguration(String configName, long version) {
            InputStream configInputStream = IOUtilities.getResourceAsStream(configName + ".xml");
            if (configInputStream != null) {
                log.info("Found " + configName + " configuration in classpath.");

                try {
                    String configurationString = IOUtilities.convertStreamToString(configInputStream);
                    Configuration configuration = stringToConfiguration(configName, version, -1, configurationString);
                    return configuration;
                } catch (Exception e) {
                    log.error("Error loading " + configName + " configuration from classpath", e);
                }
            } else {
                log.warn("Could not find " + configName + " configuration in classpath");
            }
            return null;
        }
    }

    public static void setMigrationStrategyFile(String migrationStrategyFile) {
        Configuration.migrationStrategyFile = migrationStrategyFile;
    }

}
