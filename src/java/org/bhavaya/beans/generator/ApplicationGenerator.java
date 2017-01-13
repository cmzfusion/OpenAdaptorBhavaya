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

import org.bhavaya.beans.Schema;
import org.bhavaya.db.DataSourceFactory;
import org.bhavaya.util.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class ApplicationGenerator {
    private static final Log log = Log.getCategory(ApplicationGenerator.class);
    private static final String INDENT = "    ";

    private Application application;
    private String baseDir;

    public ApplicationGenerator(String baseDir, Application application) {
        Schema.setGenerationMode(true);
        this.baseDir = baseDir;
        this.application = application;
    }

    public void run() throws Exception {
        if (application.getDatabaseDriverClasspath() != null) {
            File file = new File(application.getDatabaseDriverClasspath());
            if (file.exists()) ((MutableUrlClassLoader) ClassUtilities.getApplicationClassLoader()).addURL(file.toURL());
        }
        bootstrapConfig();
        SchemaGenerator schemaGenerator = new SchemaGenerator(application, application.getResourceDir(baseDir) + File.separator + "schema.xml");
        schemaGenerator.run();
        CriterionGenerator criterionGenerator = new CriterionGenerator(application, application.getResourceDir(baseDir) + File.separator + "criterion.xml");
        criterionGenerator.run();
        updateApplicationXmlWithBeanCollectionGroups();
        DataSourceFactory.closeAll();
    }

    private void bootstrapConfig() throws IOException {
        ApplicationProperties.clearApplicationProperties();
        Schema.reinit();
        DataSourceFactory.reinit();
        application.createDirectories(baseDir);
        writeApplicationXml();
        System.setProperty("ENVIRONMENT_PATH", "application.xml");
        System.setProperty("OVERRIDE_RESOURCE_DIR", application.getResourceDir(baseDir));
    }

    private void writeApplicationXml() throws IOException {
        String applicationXml = IOUtilities.convertStreamToString(IOUtilities.getResourceAsStream("applicationTemplate.xml"));
        applicationXml = applicationXml.replaceAll("%APPLICATION_NAME%", application.getName());
        applicationXml = applicationXml.replaceAll("%APPLICATION_ID%", application.getId());
        applicationXml = applicationXml.replaceAll("%DATABASE_DIALECT%", application.getDatabaseDialect());
        applicationXml = applicationXml.replaceAll("%DATABASE_DRIVER%", application.getDatabaseDriver());
        applicationXml = applicationXml.replaceAll("%DATABASE_URL%", application.getDatabaseUrl());
        applicationXml = applicationXml.replaceAll("%DATABASE_USER%", application.getDatabaseUser());
        applicationXml = applicationXml.replaceAll("%DATABASE_PASSWORD%", application.getDatabasePassword());
        IOUtilities.writeStringToFile(application.getResourceDir(baseDir) + File.separator + "application.xml", applicationXml);
    }

    private void updateApplicationXmlWithBeanCollectionGroups() throws IOException {
        String applicationXml = IOUtilities.readFile(application.getResourceDir(baseDir) + File.separator + "application.xml");
        Schema[] schemas = Schema.getInstances();
        StringBuffer beanCollectionGroups = new StringBuffer(1000);
        beanCollectionGroups.append("\n");
        for (int i = 0; i < schemas.length; i++) {
            Schema schema = schemas[i];
            String beanType = schema.getTypeName();
            String key = Utilities.getPluralName(Utilities.decapitalise(ClassUtilities.getUnqualifiedClassName(beanType)));
            String displayName = Utilities.getDisplayName(ClassUtilities.getUnqualifiedClassName(beanType));
            String pluralDisplayName = Utilities.getPluralName(displayName);

            writeOpenPropertyGroup(beanCollectionGroups, 2, key);
            writeProperty(beanCollectionGroups, 3, "displayName", displayName);
            writeProperty(beanCollectionGroups, 3, "pluralDisplayName", pluralDisplayName);
            writeProperty(beanCollectionGroups, 3, "beanType", beanType);
            writeProperty(beanCollectionGroups, 3, "default", "true");
            writeClosePropertyGroup(beanCollectionGroups, 2);
        }

        applicationXml = applicationXml.replaceAll("%BEAN_COLLECTION_GROUPS%", beanCollectionGroups.toString());
        IOUtilities.writeStringToFile(application.getResourceDir(baseDir) + File.separator + "application.xml", applicationXml);
    }

    public static void writeOpenPropertyGroup(StringBuffer buffer, int indent, String key) {
        for (int i = 0; i < indent; i++) {
            buffer.append(INDENT);
        }
        buffer.append("<propertyGroup key=\"").append(key).append("\">").append("\n");
    }

    public static void writeClosePropertyGroup(StringBuffer buffer, int indent) {
        for (int i = 0; i < indent; i++) {
            buffer.append(INDENT);
        }
        buffer.append("</propertyGroup>").append("\n");
    }

    public static void writeProperty(StringBuffer buffer, int indent, String key, List values) {
        if (values == null || values.size() == 0) return;

        if (values.size() == 1) {
            writeProperty(buffer, indent, key, values.iterator().next());
        } else {
            writeOpenPropertyGroup(buffer, indent, key);
            for (Iterator iterator = values.iterator(); iterator.hasNext();) {
                for (int i = 0; i < indent + 1; i++) {
                    buffer.append(INDENT);
                }
                buffer.append("<value>").append(iterator.next()).append("</value>").append("\n");
            }
            writeClosePropertyGroup(buffer, indent);
        }

    }

    public static void writeProperty(StringBuffer buffer, int indent, String key, Object value) {
        if (value == null) return;

        for (int i = 0; i < indent; i++) {
            buffer.append(INDENT);
        }
        buffer.append("<property key=\"").append(key).append("\" value=\"").append(value).append("\"/>").append("\n");
    }

    public static void main(String[] args) {
        if (args.length < 9) {
            log.error("Usage: ApplicationGenerator <application id> <application name> <database dialect> <database driver> <database url> <catalog name> <schema name> <database user> <database password>");
            System.exit(1);
        }

        String applicationId = args[0];
        String applicationName = args[1];
        String databaseDialectClassName = args[2];
        String databaseDriver = args[3];
        String databaseUrl = args[4];
        String catalogName = args[5];
        String schemaName = args[6];
        String databaseUser = args[7];
        String databasePassword = args[8];

        catalogName = catalogName.equals("NULL") ? null : catalogName;
        schemaName = schemaName.equals("NULL") ? null : schemaName;

        Application application = new Application(applicationId, applicationName, databaseDialectClassName, databaseDriver, databaseUrl, catalogName, schemaName, databaseUser, databasePassword, null);
        ApplicationGenerator applicationGenerator = new ApplicationGenerator(".", application);
        try {
            applicationGenerator.run();
        } catch (Exception e) {
            log.error(e);
        }
    }
}
