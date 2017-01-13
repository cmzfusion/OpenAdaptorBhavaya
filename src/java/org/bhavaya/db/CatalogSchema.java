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

import org.bhavaya.util.BeanUtilities;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.util.HashMap;
import java.util.Map;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class CatalogSchema implements Comparable {
    private static final Map instancesByCatalog = new HashMap();

    static {
        BeanUtilities.addPersistenceDelegate(CatalogSchema.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                CatalogSchema oldCatalogSchema = (CatalogSchema) oldInstance;
                return new Expression(oldInstance, CatalogSchema.class, "getInstance", new Object[]{oldCatalogSchema.getCatalogName(), oldCatalogSchema.getSchemaName()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    private String catalogName;
    private String schemaName;
    private String representation;

    public static synchronized CatalogSchema getInstance(String catalogName, String schemaName) {
        Map instancesBySchema = (Map) instancesByCatalog.get(catalogName);
        if (instancesBySchema == null) {
            instancesBySchema = new HashMap();
            instancesByCatalog.put(catalogName, instancesBySchema);
        }

        CatalogSchema catalogSchema = (CatalogSchema) instancesBySchema.get(schemaName);
        if (catalogSchema == null) {
            catalogSchema = new CatalogSchema(catalogName, schemaName);
            instancesBySchema.put(schemaName, catalogSchema);
        }

        return catalogSchema;
    }

    private CatalogSchema(String catalogName, String schemaName) {
        this.catalogName = catalogName;
        this.schemaName = schemaName;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Warning this method has been changed so that a null or blank catalog/schema can match any other catalog/schema
     *
     * @param o
     * @return
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogSchema)) return false;

        final CatalogSchema catalogSchema = (CatalogSchema) o;

        if (catalogName != null && catalogName.length() > 0 && catalogSchema.catalogName != null && catalogSchema.catalogName.length() > 0 && !catalogName.equals(catalogSchema.catalogName)) return false;
        if (schemaName != null && schemaName.length() > 0 && catalogSchema.schemaName != null && catalogSchema.schemaName.length() > 0 && !schemaName.equals(catalogSchema.schemaName)) return false;

        return true;
    }

    /**
     * Warning this method has been changed so that a null or blank catalog/schema can match any other catalog/schema
     *
     * @return
     */
    public int hashCode() {
        return 0;
    }

    public String toString() {
        return getRepresentation();
    }

    public String getRepresentation() {
        if (representation == null) {
            if (catalogName != null && catalogName.length() > 0 && schemaName != null && schemaName.length() > 0) {
                representation = catalogName + "." + schemaName;
            } else if (catalogName != null && catalogName.length() > 0) {
                representation = catalogName;
            } else if (schemaName != null && schemaName.length() > 0) {
                representation = schemaName;
            } else {
                representation = "";
            }
        }
        return representation;
    }

    public int compareTo(Object o) {
        if (!(o instanceof CatalogSchema)) return -1;
        final CatalogSchema catalogSchema = (CatalogSchema) o;
        int compareCatalogName = catalogName != null && catalogName.length() > 0 && catalogSchema.catalogName != null && catalogSchema.catalogName.length() > 0 ? catalogName.compareToIgnoreCase(catalogSchema.catalogName) : 0;
        int compareSchemaName = schemaName != null && schemaName.length() > 0 && catalogSchema.schemaName != null && catalogSchema.schemaName.length() > 0 ? schemaName.compareToIgnoreCase(catalogSchema.schemaName) : 0;
        return compareCatalogName != 0 ? compareCatalogName : compareSchemaName;
    }
}
