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

import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.6 $
 */
public abstract class AbstractBhavayaDataSource implements BhavayaDataSource {
    private static final Log log = Log.getCategory(AbstractBhavayaDataSource.class);

    private String dataSourceName;
    private DatabaseDialect dialect;
    private Boolean supportsSchema;
    private Boolean supportsCatalog;
    private String defaultCatalog;
    private String defaultSchema;
    private Map jdbcTypeToNativeColumnTypes;
    private String sqlNotificationSubject;
    protected static String appName;

    public void configure(PropertyGroup propertyGroup) {
        dataSourceName = propertyGroup.getMandatoryProperty("name");
        defaultCatalog = propertyGroup.getProperty("defaultCatalog");
        defaultSchema = propertyGroup.getProperty("defaultSchema");
        sqlNotificationSubject = propertyGroup.getProperty("sqlNotificationSubject");
        String dialectClassName = propertyGroup.getProperty("dialectClass");
        if (dialectClassName == null || dialectClassName.length() == 0) {
            dialect = new DefaultDatabaseDialect();
        } else {
            try {
                dialect = (DatabaseDialect) ClassUtilities.getClass(dialectClassName).newInstance();
            } catch (Exception e) {
                log.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public DatabaseDialect getDialect() {
        return dialect;
    }

    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public boolean isCatalogSupported() {
        if (supportsCatalog == null) {
            try {
                supportsCatalog = new Boolean(MetaDataSource.getInstance(dataSourceName).isCatalogSupported());
            } catch (Exception e) {
                log.error(e);
                supportsCatalog = Boolean.TRUE;
            }
        }
        return supportsCatalog.booleanValue();
    }

    public boolean isSchemaSupported() {
        if (supportsSchema == null) {
            try {
                supportsSchema = new Boolean(MetaDataSource.getInstance(dataSourceName).isSchemaSupported());
            } catch (Exception e) {
                log.error(e);
                supportsSchema = Boolean.TRUE;
            }
        }
        return supportsSchema.booleanValue();
    }

    public CatalogSchema[] getCatalogSchemas() throws SQLException {
        Connection connection = null;
        try {
            connection = getConnection();
            return DBUtilities.getCatalogSchemas(connection);
        } finally {
            DBUtilities.close(connection);
        }
    }

    public String getNativeColumnDescription(int jdbcType, int columnSize, int decimalDigits) throws Exception {
        Map jdbcTypeToNativeColumnTypes = getJdbcTypeToNativeColumnTypes();

        String nativeType = (String) jdbcTypeToNativeColumnTypes.get(new Integer(jdbcType));

        switch (jdbcType) {
            case Types.CHAR:
                return "CHAR(" + columnSize + ")";

            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                return "VARCHAR(" + columnSize + ")";

            case Types.DECIMAL:
            case Types.NUMERIC:
                return "NUMERIC(" + columnSize + ", " + decimalDigits + ")";

            default:
                return nativeType;
        }
    }

    private Map getJdbcTypeToNativeColumnTypes() throws Exception {
        if (jdbcTypeToNativeColumnTypes == null) {
            jdbcTypeToNativeColumnTypes = MetaDataSource.getInstance(dataSourceName).getNativeColumnTypes();
        }
        return jdbcTypeToNativeColumnTypes;
    }

    public String getSqlNotificationSubject() {
        return sqlNotificationSubject;
    }
}
