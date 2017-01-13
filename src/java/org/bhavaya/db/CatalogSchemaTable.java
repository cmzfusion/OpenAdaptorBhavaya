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
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.Utilities;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public class CatalogSchemaTable {
    private static final Map instancesByCatalogSchema = new IdentityHashMap(); // CatalogSchema has a strange equals that allows null to match a non-null String, so use instance
    private static final java.util.regex.Pattern dotSeperatarPattern = java.util.regex.Pattern.compile("\\.");
    private static final java.util.regex.Pattern spaceSeperatarPattern = java.util.regex.Pattern.compile("\\s+");

    private CatalogSchema catalogSchema;
    private String tableName;
    private String originalRepresentation;
    private String representation;

    static {
        BeanUtilities.addPersistenceDelegate(CatalogSchemaTable.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                CatalogSchemaTable oldCatalogSchemaTable = (CatalogSchemaTable) oldInstance;
                return new Expression(oldInstance, CatalogSchemaTable.class, "getInstance", new Object[]{oldCatalogSchemaTable.getCatalogSchema(), oldCatalogSchemaTable.getTableName(), oldCatalogSchemaTable.getOriginalRepresentation()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    public static CatalogSchemaTable getInstance(String catalogSchemaTableString, String dataSourceName) {
        if (dataSourceName == null) dataSourceName = DataSourceFactory.getDefaultDataSourceName();
        BhavayaDataSource dataSource = DataSourceFactory.getInstance(dataSourceName);
        return getInstance(catalogSchemaTableString, dataSource.getDefaultCatalog(), dataSource.getDefaultSchema(), dataSource.isCatalogSupported(), dataSource.isSchemaSupported());
    }

    public static CatalogSchemaTable getInstance(String catalogSchemaTableString, String defaultCatalog, String defaultSchema, boolean supportsCatalog, boolean supportsSchema) {
        String substitutedCatalogSchemaTableString = ApplicationProperties.substituteApplicationProperties(catalogSchemaTableString);

        String[] catalogSchemaTablePartsAndAlias = spaceSeperatarPattern.split(substitutedCatalogSchemaTableString);
        String[] catalogSchemaTableParts = dotSeperatarPattern.split(catalogSchemaTablePartsAndAlias[0], 3);

        String catalogName = null;
        String schemaName = null;
        String tableName = null;

        if (!supportsCatalog && !supportsSchema) {
            tableName = catalogSchemaTableParts[catalogSchemaTableParts.length - 1];

        } else if (catalogSchemaTableParts.length == 1 && supportsCatalog && supportsSchema) {
            catalogName = defaultCatalog;
            schemaName = defaultSchema;
            tableName = catalogSchemaTableParts[0];
        } else if (catalogSchemaTableParts.length == 1 && supportsCatalog && !supportsSchema) {
            catalogName = defaultCatalog;
            tableName = catalogSchemaTableParts[0];
        } else if (catalogSchemaTableParts.length == 1 && !supportsCatalog && supportsSchema) {
            schemaName = defaultSchema;
            tableName = catalogSchemaTableParts[0];

        } else if (catalogSchemaTableParts.length == 2 && supportsCatalog && supportsSchema) {
            catalogName = "".equals(catalogSchemaTableParts[0]) ? defaultCatalog : catalogSchemaTableParts[0];
            schemaName = defaultSchema;
            tableName = catalogSchemaTableParts[1];
        } else if (catalogSchemaTableParts.length == 2 && supportsCatalog && !supportsSchema) {
            catalogName = "".equals(catalogSchemaTableParts[0]) ? defaultCatalog : catalogSchemaTableParts[0];
            tableName = catalogSchemaTableParts[1];
        } else if (catalogSchemaTableParts.length == 2 && !supportsCatalog && supportsSchema) {
            schemaName = "".equals(catalogSchemaTableParts[0]) ? defaultSchema : catalogSchemaTableParts[0];
            tableName = catalogSchemaTableParts[1];

        } else if (catalogSchemaTableParts.length == 3 && supportsCatalog && supportsSchema) {
            catalogName = "".equals(catalogSchemaTableParts[0]) ? defaultCatalog : catalogSchemaTableParts[0];
            schemaName = "".equals(catalogSchemaTableParts[1]) ? defaultSchema : catalogSchemaTableParts[1];
            tableName = catalogSchemaTableParts[2];
        } else if (catalogSchemaTableParts.length == 3 && supportsCatalog && !supportsSchema) {
            catalogName = "".equals(catalogSchemaTableParts[0]) ? defaultCatalog : catalogSchemaTableParts[0];
            tableName = catalogSchemaTableParts[2];
        } else if (catalogSchemaTableParts.length == 3 && !supportsCatalog && supportsSchema) {
            schemaName = "".equals(catalogSchemaTableParts[1]) ? defaultSchema : catalogSchemaTableParts[1];
            tableName = catalogSchemaTableParts[2];
        }

        CatalogSchemaTable catalogSchemaTable = getInstance(catalogName, schemaName, tableName, catalogSchemaTableString);
        if (catalogSchemaTablePartsAndAlias.length > 1) {
            String alias = catalogSchemaTablePartsAndAlias[catalogSchemaTablePartsAndAlias.length - 1];
            catalogSchemaTable = new AliasedCatalogSchemaTable(catalogSchemaTable, alias);
        }

        return catalogSchemaTable;
    }

    public static CatalogSchemaTable getInstance(String catalogName, String schemaName, String tableName, String originalRepresentation) {
        return getInstance(CatalogSchema.getInstance(catalogName, schemaName), tableName, originalRepresentation);
    }

    public static synchronized CatalogSchemaTable getInstance(CatalogSchema catalogSchema, String tableName, String originalRepresentation) {
        Map instancesByTable = (Map) instancesByCatalogSchema.get(catalogSchema);
        if (instancesByTable == null) {
            instancesByTable = new HashMap();
            instancesByCatalogSchema.put(catalogSchema, instancesByTable);
        }

        CatalogSchemaTable catalogSchemaTable = (CatalogSchemaTable) instancesByTable.get(tableName);
        if (catalogSchemaTable == null) {
            catalogSchemaTable = new CatalogSchemaTable(catalogSchema, tableName, originalRepresentation);
            instancesByTable.put(tableName, catalogSchemaTable);
        }

        return catalogSchemaTable;
    }

    public CatalogSchemaTable(CatalogSchema catalogSchema, String tableName, String originalRepresentation) {
        this.catalogSchema = catalogSchema;
        this.tableName = tableName;
        this.originalRepresentation = originalRepresentation;
    }

    public CatalogSchema getCatalogSchema() {
        return catalogSchema;
    }

    public String getTableName() {
        return tableName;
    }

    public String getOriginalRepresentation() {
        if (originalRepresentation == null) originalRepresentation = getRepresentation();
        return originalRepresentation;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogSchemaTable)) return false;

        final CatalogSchemaTable catalogSchemaTable = (CatalogSchemaTable) o;

        if (!Utilities.equals(catalogSchema, catalogSchemaTable.catalogSchema)) return false;
        if (!tableName.equals(catalogSchemaTable.tableName)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = catalogSchema != null ? catalogSchema.hashCode() : 0;
        result = 29 * result + tableName.hashCode();
        return result;
    }

    public String toString() {
        return getRepresentation();
    }

    public String getTableRepresentation() {
        return getRepresentation();
    }

    public String getRepresentation() {
        if (representation == null) {
            String catalogSchemaRepresentation = catalogSchema != null ? catalogSchema.getRepresentation() : null;
            if (catalogSchemaRepresentation != null && catalogSchemaRepresentation.length() > 0) {
                representation = catalogSchemaRepresentation + "." + tableName;
            } else {
                representation = tableName;
            }
        }
        return representation;
    }
}
