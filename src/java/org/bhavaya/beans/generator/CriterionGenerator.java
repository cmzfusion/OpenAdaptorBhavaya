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

import org.bhavaya.beans.Column;
import org.bhavaya.beans.Schema;
import org.bhavaya.db.*;
import org.bhavaya.util.IOUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class CriterionGenerator {
    private static final Log log = Log.getCategory(CriterionGenerator.class);

    private CatalogSchema catalogSchema;
    private String dataSourceName;
    private String outputFile;
    private GeneratorUtil generator;

    public CriterionGenerator(Application application, String outputFile) {
        this(application.getCatalogSchema(),
                application.getDataSourceName(),
                application.getGeneratedBeansPackage(),
                outputFile);
    }

    public CriterionGenerator(CatalogSchema catalogSchema, String dataSourceName, String beansPackage, String outputFile) {
        Schema.setGenerationMode(true);
        this.catalogSchema = catalogSchema;
        this.dataSourceName = dataSourceName;
        this.outputFile = outputFile;
        this.generator = new GeneratorUtil(beansPackage, null, dataSourceName);
    }

    public void run() throws Exception {
        StringBuffer buffer = new StringBuffer(10000);
        buffer.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        buffer.append("\n");
        buffer.append("\n");
        ApplicationGenerator.writeOpenPropertyGroup(buffer, 0, "criterion");
        buffer.append("\n");
        ApplicationGenerator.writeOpenPropertyGroup(buffer, 1, "dataset");
        buffer.append("\n");
        populateBuffer(buffer);
        buffer.append("\n");
        ApplicationGenerator.writeClosePropertyGroup(buffer, 1);
        buffer.append("\n");
        ApplicationGenerator.writeClosePropertyGroup(buffer, 0);
        IOUtilities.writeStringToFile(outputFile, buffer.toString());
    }

    private void populateBuffer(StringBuffer buffer) throws Exception {
        Map tables = Database.getInstance(catalogSchema, dataSourceName).getTables();

        for (Iterator iterator = tables.values().iterator(); iterator.hasNext();) {
            Table table = (Table) iterator.next();

            Column[] primaryKey = generator.getPrimaryKey(table);
            if (primaryKey == null || primaryKey.length == 0) {
                log.warn("Not creating criterion for: " + table + " as it has no primary key");
            } else {
                log.info("Creating criterion for: " + table);
                EnumerationCriterionBean enumerationCriterion = createEnumerationCriterion(table);
                if (enumerationCriterion != null) {
                    enumerationCriterion.write(buffer, 2);
                }
                BasicCriterionBean[] basicCriteria = createBasicCriteria(table);
                if (basicCriteria != null) {
                    for (int i = 0; i < basicCriteria.length; i++) {
                        BasicCriterionBean basicCriterionBean = basicCriteria[i];
                        basicCriterionBean.write(buffer, 2);
                    }
                }
            }
        }
    }


    private EnumerationCriterionBean createEnumerationCriterion(Table table) {
        try {
            Column[] primaryKey = generator.getPrimaryKey(table);
            if (primaryKey.length > 1) {
                log.info("Not creating enumeration criterion for: " + table + " as it has a compound primary key");
                return null;
            }

            String key = generator.getUnqualifiedTypeFromTable(table);
            String name = Utilities.getDisplayName(key);
            String enumDescriptionColumn = primaryKey[0].getRepresentation();

            List beanTypes = new ArrayList();
            ForeignKey[] exportedForeignKeys = table.getExportedForeignKeys();
            for (int i = 0; i < exportedForeignKeys.length; i++) {
                ForeignKey exportedForeignKey = exportedForeignKeys[i];
                Column[] otherColumns = exportedForeignKey.getOtherColumns();
                if (otherColumns.length > 1) {
                    log.info("Not creating enumeration criterion for: " + exportedForeignKey.getOtherCatalogSchemaTable() + " to: " + table + " as it is a compound constraint");
                } else {
                    String beanPath = generator.getPropertyNameFromTable(table);
                    addBeanTypes(exportedForeignKey, beanTypes, beanPath);
                }
            }

            if (beanTypes.size() == 0) {
                return null;
            } else {
                return new EnumerationCriterionBean(key, name, generator.getTypeFromTable(table), beanTypes, enumDescriptionColumn, dataSourceName);
            }

        } catch (Exception e) {
            log.error("Could not create enumeration criterion for table: " + table, e);
            return null;
        }
    }

    private void addBeanTypes(ForeignKey exportedForeignKey, List beanTypeBeans, String beanPath) {
        CatalogSchemaTable otherCatalogSchemaTable = exportedForeignKey.getOtherCatalogSchemaTable();
        String beanType = generator.getTypeFromTable(otherCatalogSchemaTable);
        BeanTypeBean beanTypeBean = new BeanTypeBean(beanType, beanPath);
        if (beanTypeBeans.contains(beanTypeBean)) return;
        beanTypeBeans.add(beanTypeBean);

        ForeignKey[] exportedForeignKeysLevel2 = Table.getInstance(otherCatalogSchemaTable, dataSourceName).getExportedForeignKeys();
        for (int j = 0; j < exportedForeignKeysLevel2.length; j++) {
            ForeignKey exportedForeignKeyLevel2 = exportedForeignKeysLevel2[j];
            String beanPathLevel2 = generator.getPropertyNameFromTable(otherCatalogSchemaTable) + (beanPath != null ? "." + beanPath : "");
            addBeanTypes(exportedForeignKeyLevel2, beanTypeBeans, beanPathLevel2);
        }
    }


    private BasicCriterionBean[] createBasicCriteria(Table table) {
        TableColumn[] columns = table.getColumns();
        List criteria = new ArrayList(columns.length);

        for (int i = 0; i < columns.length; i++) {
            TableColumn column = columns[i];
            if (!column.isForeignKey()) {
                String columnName = column.getName();

                String key = generator.getUnqualifiedTypeFromTable(table) + "_" + Utilities.capitalise(GeneratorUtil.getPropertyNameFromColumnName(columnName));
                String name = Utilities.getDisplayName(GeneratorUtil.getPropertyNameFromColumnName(columnName));

                List beanTypes = new ArrayList();
                String beanType = generator.getTypeFromTable(table);
                String beanPath = GeneratorUtil.getPropertyNameFromColumnName(columnName);
                beanTypes.add(new BeanTypeBean(beanType, beanPath));

                criteria.add(new BasicCriterionBean(key, name, column.getType().getName(), beanTypes));
            }
        }

        return (BasicCriterionBean[]) criteria.toArray(new BasicCriterionBean[criteria.size()]);
    }

    private static class BasicCriterionBean {
        protected String key;
        protected String name;
        protected String type;
        protected String toBeanType;
        protected List beanTypeBeans;

        public BasicCriterionBean(String key, String name, String toBeanType, List beanTypeBeans) {
            this.key = key;
            this.name = name;
            this.toBeanType = toBeanType;
            this.beanTypeBeans = beanTypeBeans;
            this.type = "basic";
        }


        public void write(StringBuffer buffer, int indent) {
            ApplicationGenerator.writeOpenPropertyGroup(buffer, indent, key);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "type", type);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "name", name);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "toBeanType", toBeanType);
            for (Iterator iterator = beanTypeBeans.iterator(); iterator.hasNext();) {
                ApplicationGenerator.writeOpenPropertyGroup(buffer, indent + 1, "beanTypes");
                BeanTypeBean beanTypeBean = (BeanTypeBean) iterator.next();
                beanTypeBean.write(buffer, indent + 2);
                ApplicationGenerator.writeClosePropertyGroup(buffer, indent + 1);
            }
            ApplicationGenerator.writeClosePropertyGroup(buffer, indent);
            buffer.append("\n");
        }
    }

    private static class EnumerationCriterionBean extends BasicCriterionBean {
        private String enumDescriptionColumn;
        private String datasource;

        public EnumerationCriterionBean(String key, String name, String toBeanType, List beanTypeBeans, String enumDescriptionColumn, String datasource) {
            super(key, name, toBeanType, beanTypeBeans);
            this.enumDescriptionColumn = enumDescriptionColumn;
            this.datasource = datasource;
            this.type = "enumeration";
        }

        public void write(StringBuffer buffer, int indent) {
            ApplicationGenerator.writeOpenPropertyGroup(buffer, indent, key);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "type", type);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "name", name);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "toBeanType", toBeanType);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "descriptionColumn", enumDescriptionColumn);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "datasource", datasource);

            ApplicationGenerator.writeOpenPropertyGroup(buffer, indent + 1, "beanTypes");
            for (Iterator iterator = beanTypeBeans.iterator(); iterator.hasNext();) {
                BeanTypeBean beanTypeBean = (BeanTypeBean) iterator.next();
                beanTypeBean.write(buffer, indent + 2);
            }
            ApplicationGenerator.writeClosePropertyGroup(buffer, indent + 1);

            ApplicationGenerator.writeClosePropertyGroup(buffer, indent);
            buffer.append("\n");
        }
    }

    private static class BeanTypeBean {
        private String beanType;
        private String beanPath;

        public BeanTypeBean(String beanType, String beanPath) {
            this.beanType = beanType;
            this.beanPath = beanPath;
        }

        public void write(StringBuffer buffer, int indent) {
            ApplicationGenerator.writeOpenPropertyGroup(buffer, indent, "beanType");
            ApplicationGenerator.writeProperty(buffer, indent + 1, "beanType", beanType);
            ApplicationGenerator.writeProperty(buffer, indent + 1, "beanPath", beanPath);
            ApplicationGenerator.writeClosePropertyGroup(buffer, indent);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BeanTypeBean)) return false;

            final BeanTypeBean beanTypeBean = (BeanTypeBean) o;

            if (!beanType.equals(beanTypeBean.beanType)) return false;

            return true;
        }

        public int hashCode() {
            return beanType.hashCode();
        }
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            log.error("Usage: CriterionGenerator <data source> <catalog name> <schema name> <beans package> <output file>");
            System.exit(1);
        }
        String datasource = args[0];
        String catalogName = args[1];
        String schemaName = args[2];
        String beansPackage = args[3];
        String outputFile = args[4];

        catalogName = catalogName.equals("NULL") ? null : catalogName;
        schemaName = schemaName.equals("NULL") ? null : schemaName;

        CatalogSchema catalogSchema = CatalogSchema.getInstance(catalogName, schemaName);
        CriterionGenerator criterionGenerator = new CriterionGenerator(catalogSchema, datasource, beansPackage, outputFile);
        try {
            criterionGenerator.run();
        } catch (Exception e) {
            log.error(e);
        }
    }
}
