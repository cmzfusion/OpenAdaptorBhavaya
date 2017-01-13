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

package org.bhavaya.beans.generator;

import org.bhavaya.db.CatalogSchema;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.DefaultDatabaseDialect;
import org.bhavaya.util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public class Application extends DefaultObservable {
    private static final Log log = Log.getCategory(Application.class);
    public static double DEFAULT_MAX_MEMORY = 64;

    private String id;
    private String name;
    private String databaseDialect;
    private String databaseDriver;
    private String databaseUrl;
    private CatalogSchema catalogSchema;
    private String databaseUser;
    private String databasePassword;
    private String databaseDriverClasspath;
    private java.util.Date lastGeneration;
    private double maxMemory;

    static {
        BeanUtilities.addPersistenceDelegate(Application.class, new BhavayaPersistenceDelegate(new String[]{"id",
                                                                                                            "name",
                                                                                                            "databaseDialect",
                                                                                                            "databaseDriver",
                                                                                                            "databaseUrl",
                                                                                                            "catalogSchema",
                                                                                                            "databaseUser",
                                                                                                            "databasePassword",
                                                                                                            "databaseDriverClasspath",
                                                                                                            "lastGeneration",
                                                                                                            "maxMemory"}));
    }

    public Application() {
        // defaults
        databaseDialect = DefaultDatabaseDialect.class.getName();
        maxMemory = DEFAULT_MAX_MEMORY;
    }

    public Application(String applicationId, String applicationName, String databaseDialect, String databaseDriver, String databaseUrl, String catalogName, String schemaName, String databaseUser, String databasePassword, String databaseDriverClasspath) {
        this(applicationId, applicationName, databaseDialect, databaseDriver, databaseUrl, catalogName, schemaName, databaseUser, databasePassword, databaseDriverClasspath, null);
    }

    public Application(String applicationId, String applicationName, String databaseDialect, String databaseDriver, String databaseUrl, String catalogName, String schemaName, String databaseUser, String databasePassword, String databaseDriverClasspath, java.util.Date lastGeneration) {
        this(applicationId, applicationName, databaseDialect, databaseDriver, databaseUrl, catalogName, schemaName, databaseUser, databasePassword, databaseDriverClasspath, lastGeneration, DEFAULT_MAX_MEMORY);
    }

    public Application(String applicationId, String applicationName, String databaseDialect, String databaseDriver, String databaseUrl, String catalogName, String schemaName, String databaseUser, String databasePassword, String databaseDriverClasspath, java.util.Date lastGeneration, double maxMemory) {
        this(applicationId, applicationName, databaseDialect, databaseDriver, databaseUrl, CatalogSchema.getInstance(blankToNull(catalogName), blankToNull(schemaName)), databaseUser, databasePassword, databaseDriverClasspath, lastGeneration, maxMemory);
    }

    public Application(String applicationId, String applicationName, String databaseDialect, String databaseDriver, String databaseUrl, CatalogSchema catalogSchema, String databaseUser, String databasePassword, String databaseDriverClasspath, java.util.Date lastGeneration, double maxMemory) {
        this.id = applicationId;
        this.name = applicationName;
        this.databaseDialect = databaseDialect;
        this.databaseDriver = databaseDriver;
        this.databaseUrl = databaseUrl;
        this.catalogSchema = catalogSchema;
        this.databaseUser = nullToBlank(databaseUser);
        this.databasePassword = nullToBlank(databasePassword);
        this.databaseDriverClasspath = databaseDriverClasspath;
        this.lastGeneration = lastGeneration;
        this.maxMemory = maxMemory;
    }

    private static String blankToNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }

    private static String nullToBlank(String s) {
        return s == null ? "" : s;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        String oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    public String getDatabaseDialect() {
        return databaseDialect;
    }

    public void setDatabaseDialect(String databaseDialect) {
        String oldValue = this.databaseDialect;
        this.databaseDialect = databaseDialect;
        firePropertyChange("databaseDialect", oldValue, databaseDialect);
    }

    public String getDatabaseDriver() {
        return databaseDriver;
    }

    public void setDatabaseDriver(String databaseDriver) {
        String oldValue = this.databaseDriver;
        this.databaseDriver = databaseDriver;
        firePropertyChange("databaseDriver", oldValue, databaseDriver);
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        String oldValue = this.databaseUrl;
        this.databaseUrl = databaseUrl;
        firePropertyChange("databaseUrl", oldValue, databaseUrl);
    }

    public CatalogSchema getCatalogSchema() {
        return catalogSchema;
    }

    public void setCatalogSchema(CatalogSchema catalogSchema) {
        CatalogSchema oldValue = this.catalogSchema;
        this.catalogSchema = catalogSchema;
        firePropertyChange("catalogSchema", oldValue, catalogSchema);
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public void setDatabaseUser(String databaseUser) {
        databaseUser = nullToBlank(databaseUser);
        String oldValue = this.databaseUser;
        this.databaseUser = databaseUser;
        firePropertyChange("databaseUser", oldValue, databaseUser);
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        databasePassword = nullToBlank(databasePassword);
        String oldValue = this.databasePassword;
        this.databasePassword = databasePassword;
        firePropertyChange("databasePassword", oldValue, databasePassword);
    }

    public String getDatabaseDriverClasspath() {
        return databaseDriverClasspath;
    }

    public void setDatabaseDriverClasspath(String databaseDriverClasspath) {
        String oldValue = this.databaseDriverClasspath;
        this.databaseDriverClasspath = databaseDriverClasspath;
        firePropertyChange("databaseDriverClasspath", oldValue, databaseDriverClasspath);
    }

    public java.util.Date getLastGeneration() {
        return lastGeneration;
    }

    public void setLastGeneration(java.util.Date lastGeneration) {
        Date oldValue = this.lastGeneration;
        this.lastGeneration = lastGeneration;
        firePropertyChange("lastGeneration", oldValue, lastGeneration);
    }

    public double getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(double maxMemory) {
        double oldValue = this.maxMemory;
        this.maxMemory = maxMemory;
        firePropertyChange("maxMemory", oldValue, maxMemory);
    }

    public String getGeneratedBeansPackage() {
        return "com." + id + ".generatedBeans";
    }

    public String getResourceDir(String baseDir) {
        return getApplicationDir(baseDir) + File.separator + "resources";
    }

    public String getApplicationDir(String baseDir) {
        return baseDir + File.separator + id;
    }

    public String getDataSourceName() {
        return id + "Database";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Application)) return false;

        final Application application = (Application) o;

        if (id != null ? !id.equals(application.id) : application.id != null) return false;

        return true;
    }

    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    public String toString() {
        return id;
    }

    public void createDirectories(String baseDir) {
        String applicationDir = getApplicationDir(baseDir);
        System.out.println("Creating: " + applicationDir);
        File file = new File(applicationDir);
        file.mkdirs();

        String resourceDir = getResourceDir(baseDir);
        System.out.println("Creating: " + resourceDir);
        file = new File(resourceDir);
        file.mkdirs();
    }

    public void deleteDirectories(String baseDir) {
        File file = new File(getApplicationDir(baseDir));
        IOUtilities.delete(file);
    }

    public void validate() throws ValidationException {
        List errors = new ArrayList();
        if (id == null || id.length() == 0) errors.add("Invalid id");
        if (name == null || name.length() == 0) errors.add("Invalid name");
        if (databaseDialect == null || databaseDialect.length() == 0) errors.add("Invalid database dialect");
        if (databaseDriver == null || databaseDriver.length() == 0) errors.add("Invalid database driver");
        if (databaseUrl == null || databaseUrl.length() == 0) errors.add("Invalid database url");
        if (catalogSchema == null) errors.add("Invalid Catalog/Schema");
        if (maxMemory <= 0) errors.add("Invalid maximum memory");

        if (databaseDriverClasspath != null && databaseDriverClasspath.length() > 0) {
            File file = new File(databaseDriverClasspath);
            if (!file.exists()) {
                errors.add("Invalid database driver classpath");
            } else {
                try {
                    URL url = file.toURL();
                    ((MutableUrlClassLoader) ClassUtilities.getApplicationClassLoader()).addURL(url);
                    ClassUtilities.getClass(databaseDriver);
                } catch (Exception e) {
                    log.error("Cannot find database driver at: " + file.getAbsolutePath(), e);
                    errors.add("Cannot find database driver at: " + file.getAbsolutePath());
                }
            }
        }

        if (errors.size() > 0) {
            String[] errorStrings = (String[]) errors.toArray(new String[errors.size()]);
            throw new ValidationException(errorStrings);
        }
    }

    public boolean isGenerated(String baseDir) {
        File file = new File(getResourceDir(baseDir));
        return getLastGeneration() != null && file.exists();
    }

    public void generate(String baseDir) throws Exception {
        ApplicationGenerator generator = new ApplicationGenerator(baseDir, this);
        generator.run();
        setLastGeneration(DateUtilities.newDateTime());
    }

    public Application copy() {
        log.info("Copying: " + this);
        Application copy = (Application) BeanUtilities.verySlowDeepCopy(this);
        copy.setId(null);
        copy.setName(null);
        copy.setLastGeneration(null);
        return copy;
    }


    public Connection newConnection() throws SQLException {
        addDriverToClassPath();
        return DBUtilities.newConnection(getDatabaseUrl(), getDatabaseUser(), getDatabasePassword(), getDatabaseDriver(), null, true, -1, null);
    }

    private void addDriverToClassPath() {
        if (databaseDriverClasspath != null && databaseDriverClasspath.length() > 0) {
            File file = new File(databaseDriverClasspath);
            if (file.exists()) {
                try {
                    ((MutableUrlClassLoader) ClassUtilities.getApplicationClassLoader()).addURL(file.toURL());
                } catch (Exception e) {
                }
            }
        }
    }

    public Process run(String baseDir, String host, int port) throws IOException {
        log.info("About to run " + getId());

        String java;
        String osName = System.getProperty("os.name");
        if (osName.indexOf("Windows") != -1) {
            java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
        } else {
            java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        }

        String bootstrapClasspath = baseDir + File.separator + "classes";
        String classpath = "-classpath \"" + bootstrapClasspath;
        if (getDatabaseDriverClasspath() != null) {
            classpath += System.getProperty("path.separator") + getDatabaseDriverClasspath();
        }
        classpath += "\"";

        String maxMemoryString = "-mx" + new Double(maxMemory).intValue() + "m";
        String urlClassLoader = "-DurlClassLoader=" + host + ":" + port;
        String environmentPath = "-DENVIRONMENT_PATH=application.xml";
        String overrideResourceDir = "-DOVERRIDE_RESOURCE_DIR=" + getResourceDir(baseDir);
        String mainClass = "org.bhavaya.ui.builder.ApplicationLauncher";
        String command = java + " " + urlClassLoader + " " + maxMemoryString + " " + classpath + " " + environmentPath + " " + overrideResourceDir + " " + mainClass;
//        String remoteDebugParams = "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8787";
//        String command = java + " " + remoteDebugParams + " " + urlClassLoader + " " + maxMemoryString + " " + classpath + " " + environmentPath + " " + overrideResourceDir + " " + mainClass;

        log.info("Running: " + command);

        Process process = Runtime.getRuntime().exec(command);
        return process;
    }
}
