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

import org.bhavaya.beans.*;
import org.bhavaya.db.*;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;

import java.util.*;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public class SchemaGenerator {
    private static final Log log = Log.getCategory(SchemaGenerator.class);

    private CatalogSchema catalogSchema;
    private String dataSourceName;
    private String outputFile;
    private GeneratorUtil generator;

    public SchemaGenerator(Application application, String outputFile) {
        this(application.getCatalogSchema(),
                application.getDataSourceName(),
                application.getGeneratedBeansPackage(),
                outputFile);
    }

    public SchemaGenerator(CatalogSchema catalogSchema, String dataSourceName, String generatedBeansPackage, String outputFile) {
        Schema.setGenerationMode(true);
        this.catalogSchema = catalogSchema;
        this.dataSourceName = dataSourceName;
        this.outputFile = outputFile;
        this.generator = new GeneratorUtil(generatedBeansPackage, generatedBeansPackage, dataSourceName);
    }

    public void run() throws Exception {
        Map tables = Database.getInstance(catalogSchema, dataSourceName).getTables();

        List schemas = new ArrayList(tables.size());
        for (Iterator iterator = tables.values().iterator(); iterator.hasNext();) {
            Table table = (Table) iterator.next();
            Schema schema = createSchema(table);
            if (schema != null) {
                schemas.add(schema);
            }
        }

        if (schemas.size() > 0) {
            Schema.SchemaWriter schemaWriter = new Schema.SchemaWriter((Schema[]) schemas.toArray(new Schema[schemas.size()]));
            schemaWriter.write();
        } else {
            Schema.SchemaWriter.writeBlankSchema(outputFile);
        }
    }

    private Schema createSchema(Table table) {
        try {
            CatalogSchemaTable catalogSchemaTable = table.getCatalogSchemaTable();
            String type = generator.getTypeFromTable(table);
            String generatedClass = generator.getGeneratedClassFromTable(table);

            TableColumn[] primaryKey = generator.getPrimaryKey(table);
            if (primaryKey == null || primaryKey.length == 0) {
                log.warn("Not creating schema for: " + table + " as it has no primary key");
                return null;
            }

            TableColumn[] columns = table.getColumns();
            ForeignKey[] importedForeignKeys = table.getImportedForeignKeys();
            ForeignKey[] exportedForeignKeys = table.getExportedForeignKeys();
            String sql = "SELECT " + catalogSchemaTable.getRepresentation() + ".* FROM " + catalogSchemaTable.getTableRepresentation();

            List properties = new ArrayList(columns.length);
            addDefaultPropertiesToList(columns, type, properties);
            addForeignKeyPropertiesToList(importedForeignKeys, type, properties, Property.ONE);
            addForeignKeyPropertiesToList(exportedForeignKeys, type, properties, Property.MANY);
            Index[] indicies = createIndicies(importedForeignKeys, primaryKey);

            log.info("Creating schema for: " + table);
            Schema schema = new Schema(outputFile, true, type, generatedClass, null, dataSourceName, "org.bhavaya.db.SqlBeanFactory", null, null, null, null, primaryKey, indicies, "HIGH", "HIGH", (Property[]) properties.toArray(new Property[properties.size()]), sql, null, null, null);
            return schema;
        } catch (Exception e) {
            log.error("Could not create schema for table: " + table, e);
            return null;
        }
    }

    private void addDefaultPropertiesToList(TableColumn[] columns, String parentTypeName, List properties) {
        for (int i = 0; i < columns.length; i++) {
            TableColumn column = columns[i];
            if (!column.isForeignKey()) {
                DefaultProperty defaultProperty = createDefaultProperty(parentTypeName, column);
                properties.add(defaultProperty);
            }
        }
    }

    private DefaultProperty createDefaultProperty(String parentTypeName, Column column) {
        String propertyName = GeneratorUtil.getPropertyNameFromColumnName(column.getName());
        String propertyType = ClassUtilities.classToType(column.getType()).getName();
        DefaultProperty defaultProperty = new DefaultProperty(parentTypeName, propertyName, propertyType, Property.ONE, column);
        return defaultProperty;
    }

    private void addForeignKeyPropertiesToList(ForeignKey[] foreignKeys, String parentTypeName, List properties, String cardinality) {
        for (int i = 0; i < foreignKeys.length; i++) {
            ForeignKey foreignKey = foreignKeys[i];
            ForeignKeyProperty foreignKeyProperty;
            if (cardinality.equals(Property.ONE)) {
                foreignKeyProperty = createOneToOneForeignKeyProperty(parentTypeName, foreignKey);
            } else {
                foreignKeyProperty = createOneToManyForeignKeyProperty(parentTypeName, foreignKey);
            }
            if (foreignKeyProperty != null) properties.add(foreignKeyProperty);
        }
    }

    private ForeignKeyProperty createOneToOneForeignKeyProperty(String parentTypeName, ForeignKey foreignKey) {
        CatalogSchemaTable otherCatalogSchemaTable = foreignKey.getOtherCatalogSchemaTable();
        String propertyType = generator.getTypeFromTable(otherCatalogSchemaTable);
        String propertyName = generator.getPropertyNameFromTable(otherCatalogSchemaTable);

        ForeignKeyProperty foreignKeyProperty = new SqlForeignKeyProperty(parentTypeName, propertyName, propertyType, Property.ONE, null, true, -1);
        addParameters(foreignKey, foreignKeyProperty);

        return foreignKeyProperty;
    }

    private ForeignKeyProperty createOneToManyForeignKeyProperty(String parentTypeName, ForeignKey foreignKey) {
        // is really a one-to-one relationship
        TableColumn[] otherPrimaryKey = generator.getPrimaryKey(foreignKey.getOtherCatalogSchemaTable());
        if (Arrays.equals(foreignKey.getOtherColumns(), otherPrimaryKey)) {
            return createOneToOneForeignKeyProperty(parentTypeName, foreignKey);
        }

        CatalogSchemaTable otherCatalogSchemaTable = foreignKey.getOtherCatalogSchemaTable();
        String propertyType = generator.getTypeFromTable(otherCatalogSchemaTable);
        String propertyName = generator.getPropertyNameFromTable(otherCatalogSchemaTable);

        String foreignIndexName = createIndexNameForForeignKeyProperty(parentTypeName);
        ForeignKeyProperty foreignKeyProperty = new SqlForeignKeyProperty(parentTypeName, propertyName, propertyType, Property.MANY, foreignIndexName, true, -1);
        addParameters(foreignKey, foreignKeyProperty);

        return foreignKeyProperty;
    }

    private void addParameters(ForeignKey foreignKey, ForeignKeyProperty foreignKeyProperty) {
        Column[] foreignKeyColumns = foreignKey.getThisColumns();
        for (int i = 0; i < foreignKeyColumns.length; i++) {
            foreignKeyProperty.addParameter(new DerivedProperty.ColumnParameter(foreignKeyColumns[i]));
        }
    }


    private Index[] createIndicies(ForeignKey[] foreignKeys, TableColumn[] primaryKey) {
        if (foreignKeys == null || foreignKeys.length == 0) return null;
        List indicies = new ArrayList(foreignKeys.length);

        for (int i = 0; i < foreignKeys.length; i++) {
            ForeignKey foreignKey = foreignKeys[i];
            if (!Arrays.equals(foreignKey.getThisColumns(), primaryKey)) {
                String indexName = createIndexNameForIndex(foreignKey);
                TableColumn[] indexColumns = (TableColumn[]) foreignKey.getThisColumns().clone();
                Arrays.sort(indexColumns);
                Index index = new Index(indexName, false, indexColumns);
                indicies.add(index);
            }
        }

        return (Index[]) indicies.toArray(new Index[indicies.size()]);
    }

    private String createIndexNameForIndex(ForeignKey foreignKey) {
        CatalogSchemaTable otherCatalogSchemaTable = foreignKey.getOtherCatalogSchemaTable();
        String foreignIndexName = "by" + generator.getUnqualifiedTypeFromTable(otherCatalogSchemaTable);
        return foreignIndexName;
    }

    private String createIndexNameForForeignKeyProperty(String parentTypeName) {
        String foreignIndexName = "by" + ClassUtilities.getUnqualifiedClassName(parentTypeName);
        return foreignIndexName;
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            log.error("Usage: SchemaGenerator <data source> <catalog name> <schema name> <generated beans package> <output file>");
            System.exit(1);
        }
        String datasource = args[0];
        String catalogName = args[1];
        String schemaName = args[2];
        String generatedBeansPackage = args[3];
        String outputFile = args[4];

        catalogName = catalogName.equals("NULL") ? null : catalogName;
        schemaName = schemaName.equals("NULL") ? null : schemaName;

        CatalogSchema catalogSchema = CatalogSchema.getInstance(catalogName, schemaName);
        SchemaGenerator schemaGenerator = new SchemaGenerator(catalogSchema, datasource, generatedBeansPackage, outputFile);
        try {
            schemaGenerator.run();
        } catch (Exception e) {
            log.error(e);
        }
    }
}
