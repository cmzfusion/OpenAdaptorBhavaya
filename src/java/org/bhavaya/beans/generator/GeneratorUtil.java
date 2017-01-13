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

import org.bhavaya.db.CatalogSchemaTable;
import org.bhavaya.db.Table;
import org.bhavaya.db.TableColumn;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class GeneratorUtil {
    private static final Log log = Log.getCategory(GeneratorUtil.class);
    private static boolean GENERATE_DUMMY_PRIMARY_KEY = true;

    private static final Set keywords = new HashSet();

    static {
        keywords.add("abstract");
        keywords.add("else");
        keywords.add("interface");
        keywords.add("super");
        keywords.add("boolean");
        keywords.add("extends");
        keywords.add("long");
        keywords.add("switch");
        keywords.add("break");
        keywords.add("final");
        keywords.add("native");
        keywords.add("synchronized");
        keywords.add("byte");
        keywords.add("finally");
        keywords.add("new");
        keywords.add("this");
        keywords.add("case");
        keywords.add("float");
        keywords.add("package");
        keywords.add("throw");
        keywords.add("catch");
        keywords.add("for");
        keywords.add("private");
        keywords.add("throws");
        keywords.add("char");
        keywords.add("if");
        keywords.add("protected");
        keywords.add("transient");
        keywords.add("class");
        keywords.add("implements");
        keywords.add("public");
        keywords.add("try");
        keywords.add("continue");
        keywords.add("import");
        keywords.add("return");
        keywords.add("void");
        keywords.add("default");
        keywords.add("instanceof");
        keywords.add("short");
        keywords.add("volatile");
        keywords.add("do");
        keywords.add("int");
        keywords.add("static");
        keywords.add("while");
        keywords.add("double");
    }

    private String beansPackage;
    private String generatedBeansPackage;
    private String dataSourceName;

    public GeneratorUtil(String beansPackage, String generatedBeansPackage, String dataSourceName) {
        this.beansPackage = beansPackage;
        this.generatedBeansPackage = generatedBeansPackage;
        this.dataSourceName = dataSourceName;
    }

    public String getTypeFromTable(Table table) {
        return getTypeFromTable(table.getCatalogSchemaTable());
    }

    public String getTypeFromTable(CatalogSchemaTable catalogSchemaTable) {
        if (catalogSchemaTable == null) return null;
        return beansPackage + "." + getUnqualifiedTypeFromTable(catalogSchemaTable);
    }

    public String getGeneratedClassFromTable(Table table) {
        if (table == null) return null;
        return generatedBeansPackage + "." + getUnqualifiedTypeFromTable(table.getCatalogSchemaTable());
    }

    public String getUnqualifiedTypeFromTable(Table table) {
        return getUnqualifiedTypeFromTable(table.getCatalogSchemaTable());
    }

    public String getUnqualifiedTypeFromTable(CatalogSchemaTable catalogSchemaTable) {
        return convertTableNameToJavaName(catalogSchemaTable.getTableName(), true);
    }

    private String convertTableNameToJavaName(String tableName, boolean capitaliseFirstChar) {
        if (tableName == null) return null;
        // remove any catalog or schema name prefixes
        tableName = Utilities.getUnqualifiedName(tableName, '.');
        // remove any other common prefixes
        if (tableName.startsWith("tbl")) tableName = tableName.substring(3);
        return convertDbNameToJavaName(tableName, capitaliseFirstChar);
    }

    public static String getPropertyNameFromColumnName(String columnName) {
        if (columnName == null) return null;
        // remove standard prefixes
        if (columnName.startsWith("fld")) columnName = columnName.substring(3);
        String propertyName = convertDbNameToJavaName(columnName, false);
        if (keywords.contains(propertyName)) propertyName = propertyName + "1";
        if (Character.isDigit(propertyName.charAt(0))) propertyName = "_" + propertyName;
        return propertyName;
    }

    public String getPropertyNameFromTable(Table table) {
        return getPropertyNameFromTable(table.getCatalogSchemaTable());
    }

    public String getPropertyNameFromTable(CatalogSchemaTable otherCatalogSchemaTable) {
        return convertTableNameToJavaName(otherCatalogSchemaTable.getTableName(), false);
    }

    private static String convertDbNameToJavaName(String dbName, boolean capitaliseFirstChar) {
        int javaNameCharIndex = 0;
        int dbNameLength = dbName.length();
        char[] javaNameChars = new char[dbNameLength];

        boolean upperCase = true;
        for (int i = 0; i < dbNameLength && upperCase; i++) {
            char c = dbName.charAt(i);
            upperCase = !Character.isLowerCase(c);
        }
        if (upperCase) dbName = dbName.toLowerCase();

        for (int i = 0; i < dbNameLength; i++) {
            char c = dbName.charAt(i);
//            boolean currentCharUpperCase = Character.isUpperCase(c);
//            boolean currentCharLowerCase = Character.isLowerCase(c);
//            boolean previousCharUpperCase = javaNameCharIndex > 0 && Character.isUpperCase(javaNameChars[javaNameCharIndex - 1]);
//            boolean previousCharLowerCase = javaNameCharIndex > 0 && Character.isLowerCase(javaNameChars[javaNameCharIndex - 1]);
//            boolean nextCharUpperCase = (i + 1 < dbNameLength) && Character.isUpperCase(dbName.charAt(i + 1));
            boolean nextCharLowerCase = (i + 1 < dbNameLength) && Character.isLowerCase(dbName.charAt(i + 1));

            if (c == '_') {
                i++;
                if (i < dbNameLength) {
                    c = dbName.charAt(i);
                    javaNameChars[javaNameCharIndex] = Character.toUpperCase(c);
                    javaNameCharIndex++;

                    if (i + 1 < dbNameLength) {
                        if (Character.toLowerCase(dbName.charAt(i + 1)) != '_') {
                            i++;
                            c = dbName.charAt(i);
                            javaNameChars[javaNameCharIndex] = Character.toLowerCase(c);
                            javaNameCharIndex++;
                        }
                    }
                }
            } else if (javaNameCharIndex == 0 && capitaliseFirstChar) {
                javaNameChars[javaNameCharIndex] = Character.toUpperCase(c);
                javaNameCharIndex++;
            } else if (javaNameCharIndex == 0 && (dbNameLength == 1 || nextCharLowerCase)) {
                javaNameChars[javaNameCharIndex] = Character.toLowerCase(c);
                javaNameCharIndex++;
            } else {
                javaNameChars[javaNameCharIndex] = c;
                javaNameCharIndex++;
            }
        }

        return new String(javaNameChars, 0, javaNameCharIndex);
    }

    public TableColumn[] getPrimaryKey(CatalogSchemaTable catalogSchemaTable) {
        return getPrimaryKey(Table.getInstance(catalogSchemaTable, dataSourceName));
    }

    public TableColumn[] getPrimaryKey(Table table) {
        TableColumn[] primaryKey = table.getPrimaryKey();
        if (GENERATE_DUMMY_PRIMARY_KEY && (primaryKey.length == 0)) {
            log.error("Adding dummy primary key for schema for table: " + table + " as no primary key exists");
            primaryKey = new TableColumn[]{TableColumn.getInstance("DUMMY", table.getCatalogSchemaTable(), dataSourceName)};
            table.setPrimaryKey(primaryKey);
        }
        return primaryKey;
    }
}
