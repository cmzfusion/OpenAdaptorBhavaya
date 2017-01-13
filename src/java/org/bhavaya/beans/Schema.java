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

package org.bhavaya.beans;

import org.bhavaya.db.*;
import org.bhavaya.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Holds schema mapping data model to object model.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.29 $
 */
public class Schema {
    private static final Log log = Log.getCategory(Schema.class);

    public static final String LOW = "LOW";
    public static final String HIGH = "HIGH";

    public static final String STRONG_REFERENCE = "STRONG";
    public static final String SOFT_REFERENCE = "SOFT";
    public static final String WEAK_REFERENCE = "WEAK";

    private static final String SCHEMA_PROPERTY = "schema";
    private static final String SCHEMA_STRICT_VALIDATION_PROPERTY = "schemaStrictValidation";
    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    private static final Property[] EMPTY_PROPERTY_ARRAY = new Property[0];

    private static volatile boolean initialised = false; // this is not synced on initLock, it is a volatile variable
    private static final Object initLock = new Object();
    private static final Set invalidInstances = new LinkedHashSet();
    private static final Map instancesByGeneratedName = new LinkedHashMap(); // sorted by entry order
    private static final Map instancesByGeneratedAndTypeName = new HashMap(); // sorted by entry order
    private static final Map typeToGeneratedClass = new HashMap();
    private static final Map classToGeneratedClass = new HashMap();
    private static boolean GENERATION_MODE = false;
    private static boolean strictValidation = true;
    private static String defaultReferenceType;
    private static int defaultMaximumJoinTableCount;

    private Class type;
    private String typeName;
    private Class generatedClass;
    private String generatedClassName;
    private boolean generate;
    private Class superType;
    private String superTypeName;
    private String beanFactoryTypeName;
    private Class beanFactoryType;
    private Class ancestorType;
    private Class[] subClasses;
    private Set subClassNames;
    private Transform transform;
    private String transformClassName;
    private String schemaFilename;
    private String referenceType;
    private boolean valid = true; // default to valid
    private SubClassMappingsForTable defaultSubClassMappingsForTable;
    private Map subClassMappingsByTable;
    private String toStringCode;
    private String sqlString;
    private SQL sql;
    private String storedProcedureString;
    private StoredProcedure storedProcedure;
    private String kxSqlString;
    private Column[] primaryKey;
    private Column[] unionOfKeyColumns;
    private Map indiciesMap;
    private Index[] indicies;
    private String dataVolatility = HIGH;
    private String dataQuantity = HIGH;
    private Map propertiesByName = new LinkedHashMap(); //maintain insertion order
    private Property[] properties;
    private Map propertiesByColumn = new LinkedHashMap(); //maintain insertion order
    private Map derivedPropertiesByName = new LinkedHashMap();
    private DerivedProperty[] derivedProperties;
    private Map defaultPropertiesByColumn = new LinkedHashMap();
    private String defaultDataSourceName;
    private OperationGroup operationGroup;
    private Set validColumnNames;
    private Type derivedPropertyColumnsType;
    private Map columnTypes = new HashMap();

    public static boolean isGenerationMode() {
        return GENERATION_MODE;
    }

    public static void setGenerationMode(boolean GENERATION_MODE) {
        Schema.GENERATION_MODE = GENERATION_MODE;
        ClassUtilities.setGenerateClassesDynamically(false);
    }

    public static boolean hasInstance(Class type) {
        if (type == null) return false;
        String typeName = type.getName();
        return hasInstance(typeName);
    }

    public static boolean hasInstance(String typeName) {
        return hasInstance(typeName, true);
    }

    public static boolean hasInstance(String typeName, boolean init) {
        if (init) init();
        if (typeName == null) return false;
        return instancesByGeneratedAndTypeName.containsKey(typeName);
    }

    public static Schema getInstance(Class type) {
        String typeName = type.getName();
        return getInstance(typeName);
    }

    public static Schema getInstance(String typeName) {
        return getInstance(typeName, true);
    }

    public static Schema getInstance(String typeName, boolean init) {
        if (init) init();
        Schema instance = (Schema) instancesByGeneratedAndTypeName.get(typeName);
        if (instance == null) {
            RuntimeException runtimeException = new RuntimeException("No Schema exists for type: " + typeName);
            log.error(runtimeException);
            throw runtimeException;
        }
        return instance;
    }

    public static Schema[] getInstances() {
        init();
        return (Schema[]) instancesByGeneratedName.values().toArray(new Schema[instancesByGeneratedName.values().size()]);
    }

    protected static Class mapTypeToGeneratedClass(Class type) {
        Class generatedClass = (Class) classToGeneratedClass.get(type);
        if (generatedClass == null) {
            String typeName = type.getName();
            String generatedClassName = getGeneratedClassForType(typeName);
            if (generatedClassName == null) {
                generatedClass = type;
            } else {
                // prevent any class load order effects while loading schemas
                generatedClass = ClassUtilities.getClass(generatedClassName, true, false);
            }
            classToGeneratedClass.put(type, generatedClass);
        }
        return generatedClass;
    }

    protected static String getGeneratedClassForType(String type) {
        String generatedClass = (String) typeToGeneratedClass.get(type);
        if (generatedClass == null) return type;
        return generatedClass;
    }

    private static void addInstance(String typeName, String generatedClassName, Schema schema) {
        if (instancesByGeneratedName.get(generatedClassName) != null) {
            log.warn("Schema already exists for type: " + typeName + " ignoring duplicate");
        } else if (schema.isValid()) {
            if (log.isDebugEnabled())log.debug("Adding schema for type: " + typeName);
            instancesByGeneratedName.put(generatedClassName, schema);
            instancesByGeneratedAndTypeName.put(generatedClassName, schema);
            instancesByGeneratedAndTypeName.put(typeName, schema);
            typeToGeneratedClass.put(typeName, generatedClassName);
        } else {
            log.warn("Not adding schema for type: " + typeName);
            invalidInstances.add(generatedClassName);
            invalidInstances.add(typeName);
        }
    }

    public Schema(String schemaFile,
                  boolean generate,
                  String typeName,
                  String generatedClassName,
                  String superTypeName,
                  String defaultDataSourceName,
                  String beanFactoryTypeName,
                  String referenceType,
                  String transformClassName,
                  SubClassMappingsForTable[] subClassMappingsForTables,
                  String toStringCode,
                  Column[] primaryKey,
                  Index[] indicies,
                  String dataVolatility,
                  String dataQuantity,
                  Property[] properties,
                  String sqlString,
                  String storedProcedureString,
                  String kxSqlString, 
                  Operation[] operations) {
        if (log.isDebug()) log.debug("Instantiating schema: " + typeName + " with generated class: " + generatedClassName);
        if (typeName == null) {
            throw new RuntimeException("type is null");
        }
        if (generatedClassName == null) {
            throw new RuntimeException("generatedClass is null");
        }

        setType(typeName, generatedClassName);

        if (!GENERATION_MODE) {
            validate(typeName, superTypeName, defaultDataSourceName, sqlString);
        }

        if (valid) {
            setSuperTypeName(superTypeName);
            setGenerate(generate);
            // Set all data after setting super type as setting supertype copies data from supertype into this type,
            // need to add overridden data.
            setSchemaFilename(schemaFile);
            setDefaultDataSourceName(defaultDataSourceName);
            setBeanFactoryTypeName(beanFactoryTypeName);
            setReferenceType(referenceType);
            setSubClassMappings(subClassMappingsForTables);
            setTransformClassName(transformClassName);
            setToStringCode(toStringCode);
            setPrimaryKey(primaryKey);
            setIndicies(indicies);
            setDataVolatility(dataVolatility);
            setDataQuantity(dataQuantity);
            setSqlString(sqlString);
            setStoredProcedureString(storedProcedureString);
            setKxSqlString(kxSqlString);
            setOperationGroup(operations);
            addProperties(properties);
        }
    }

    private void setGenerate(boolean generate) {
        this.generate = generate;
    }

    public boolean isValid() {
        return valid;
    }

    private String getSchemaFilename() {
        return schemaFilename;
    }

    private void setSchemaFilename(String schemaFilename) {
        this.schemaFilename = schemaFilename;
    }

    public boolean isGenerate() {
        return generate;
    }

    private void validate(String typeName, String superTypeName, String dataSourceName, String sqlString) {
        boolean validTemp = true;

        Schema superSchema = null;
        if (superTypeName != null && Schema.hasInstance(superTypeName, false)) superSchema = Schema.getInstance(superTypeName, false);
        if (dataSourceName == null && superSchema != null) dataSourceName = superSchema.defaultDataSourceName;

        if (sqlString == null && superSchema != null) sqlString = superSchema.sqlString;
        if (sqlString != null) {
            validTemp = validateSql(sqlString, dataSourceName, typeName);
        }

        if (strictValidation) {
            valid = validTemp; // note we still want all validation code to run, but only affects behaviour if strictValidation is on
        }
    }

    private boolean validateSql(String sqlString, String dataSourceName, String typeName) {
        if (dataSourceName == null) {
            log.warn("Cannot validate sql for type: " + typeName + " cannot determine dataSourceName, superType may be invalid");
            return false;
        }

        try {
            validColumnNames = new HashSet(Arrays.asList(MetaDataSource.getInstance(dataSourceName).getValidColumnNames(sqlString)));
        } catch (Exception e) {
            log.warn(typeName + " has invalid sql: " + sqlString, e);
            return false;
        }

        return true;
    }

    private void setType(String typeName, String generatedClassName) {
        this.typeName = typeName;
        this.generatedClassName = generatedClassName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String toString() {
        return typeName;
    }

    public Class getType() {
        if (type == null) {
            // prevent any class load order effects while loading schemas
            type = ClassUtilities.getClass(typeName, true, false);
        }
        return type;
    }

    public String getGeneratedClassName() {
        return generatedClassName;
    }

    public Class getGeneratedClass() {
        if (generatedClass == null) {
            // prevent any class load order effects while loading schemas
            generatedClass = ClassUtilities.getClass(generatedClassName, true, false);
        }
        return generatedClass;
    }

    public String getSuperTypeName() {
        return superTypeName;
    }

    private void setSuperTypeName(String superTypeName) {
        if (superTypeName != null) {
            this.superTypeName = superTypeName;

            if (Schema.hasInstance(superTypeName, false)) {
                Schema superSchema = getInstance(superTypeName, false);

                // add ability to navigate from superType to subClass
                superSchema.addSubClassName(this.typeName);

                // copy schema from superType
                setGenerate(superSchema.generate);
                setDefaultDataSourceName(superSchema.getDefaultDataSourceName());
                setBeanFactoryTypeName(superSchema.getBeanFactoryTypeName());
                setReferenceType(superSchema.getReferenceType());
                setTransformClassName(superSchema.getTransformClassName());
                setToStringCode(superSchema.getToStringCode());
                setSubClassMappings(superSchema.getSubClassMappingsForTables());
                setPrimaryKey(superSchema.primaryKey);
                setIndicies(superSchema.indicies);
                setDataVolatility(superSchema.getDataVolatility());
                setDataQuantity(superSchema.getDataQuantity());
                addProperties(superSchema.getProperties());
                setSqlString(superSchema.sqlString);
                setStoredProcedureString(superSchema.storedProcedureString);
                setKxSqlString(superSchema.kxSqlString);
                setOperationGroup(superSchema.operationGroup);
            } else if (!superTypeName.equals("org.bhavaya.util.LookupValue")) {
                log.info(superTypeName + " has no schema");
            }
        }
    }

    public Class getSuperType() {
        if (superType == null && superTypeName != null) {
            // prevent any class load order effects while loading schemas
            superType = ClassUtilities.getClass(superTypeName, true, false);
        }
        return superType;
    }

    public String getSuperClassNameOfGeneratedBean() {
        return getSuperClassName(true);
    }

    public String getSuperClassNameOfBean() {
        return getSuperClassName(false);
    }

    private String getSuperClassName(boolean generatedBean) {
        String generatedClass = getGeneratedClassName();
        String beanType = getTypeName();
        String superType = getSuperTypeName();

        String superClass;
        if (generatedBean && !(beanType.equals(generatedClass))) {
            superClass = beanType;
        } else if (superType != null) {
            superClass = superType;
        } else {
            superClass = Bean.class.getName();
        }
        return superClass;
    }

    public Class getAncestorType() {
        if (ancestorType == null) {
            Class superType = getSuperType();
            while (superType != null) {
                ancestorType = superType;
                superType = Schema.getInstance(superType).getSuperType();
            }
        }
        return ancestorType;
    }


    private void setBeanFactoryTypeName(String beanFactoryTypeName) {
        if (beanFactoryTypeName != null) {
            this.beanFactoryTypeName = beanFactoryTypeName;
        }
    }

    private String getBeanFactoryTypeName() {
        return beanFactoryTypeName;
    }

    public Class getBeanFactoryType() {
        if (beanFactoryType == null && beanFactoryTypeName != null && beanFactoryTypeName.length() > 0) beanFactoryType = ClassUtilities.getClass(beanFactoryTypeName, true, false);
        return beanFactoryType;
    }

    public String getReferenceType() {
        if (referenceType == null) return defaultReferenceType;
        return referenceType;
    }

    public boolean isDefaultReferenceType() {
        return referenceType == null;
    }

    public void setReferenceType(String referenceType) {
        if (referenceType != null) {
            if (!isValidReferenceType(referenceType)) throw new RuntimeException("Invalid value for reference type: " + this.referenceType);
            this.referenceType = referenceType;
        }
    }

    private static boolean isValidReferenceType(String referenceType) {
        return referenceType.equalsIgnoreCase(Schema.STRONG_REFERENCE) || referenceType.equalsIgnoreCase(Schema.SOFT_REFERENCE) || referenceType.equalsIgnoreCase(Schema.WEAK_REFERENCE);
    }

    private void addSubClassName(String subClassName) {
        if (subClassNames == null) subClassNames = new TreeSet();
        subClassNames.add(subClassName);
    }

    public Class[] getSubClasses() {
        if (subClassNames == null) return EMPTY_CLASS_ARRAY;
        if (subClasses == null) {
            List subClassesList = new ArrayList(subClassNames.size());
            for (Iterator iterator = subClassNames.iterator(); iterator.hasNext();) {
                String subClassName = (String) iterator.next();
                // prevent any class load order effects while loading schemas
                subClassesList.add(ClassUtilities.getClass(subClassName, true, false));
            }
            subClasses = (Class[]) subClassesList.toArray(new Class[subClassesList.size()]);
        }
        return subClasses;
    }

    public boolean hasBeanFactory() {
        return getBeanFactoryTypeName() != null;
    }

    private void setSubClassMappings(SubClassMappingsForTable[] subClassMappingsForTables) {
        if (subClassMappingsForTables != null && subClassMappingsForTables.length > 0) {
            if (subClassMappingsByTable == null) subClassMappingsByTable = new LinkedHashMap();
            for (int i = 0; i < subClassMappingsForTables.length; i++) {
                SubClassMappingsForTable subClassMappingsForTable = subClassMappingsForTables[i];
                subClassMappingsByTable.put(subClassMappingsForTable.getCatalogSchemaTable(), subClassMappingsForTable);
                if (subClassMappingsForTable.isDefault()) defaultSubClassMappingsForTable = subClassMappingsForTable;
            }
            if (defaultSubClassMappingsForTable == null) throw new RuntimeException("At least one SubClassMappingsForTable must be default");
        }
    }

    private SubClassMappingsForTable[] getSubClassMappingsForTables() {
        if (subClassMappingsByTable == null) return null;
        return (SubClassMappingsForTable[]) subClassMappingsByTable.values().toArray(new SubClassMappingsForTable[subClassMappingsByTable.size()]);
    }

    public boolean hasSubClasses() {
        return (subClassNames != null && subClassNames.size() > 0);
    }

    public Class getSubClass(TabularData.Row tabularDataRow) {
        return getSubClass(tabularDataRow, defaultSubClassMappingsForTable);
    }

    public Class getSubClass(TabularData.Row tabularDataRow, CatalogSchemaTable catalogSchemaTable) {
        if (catalogSchemaTable == null) getSubClass(tabularDataRow);
        if (subClassMappingsByTable == null) return generatedClass;
        SubClassMappingsForTable subClassMappingsForTable = (SubClassMappingsForTable) subClassMappingsByTable.get(catalogSchemaTable);
        return getSubClass(tabularDataRow, subClassMappingsForTable);
    }

    private Class getSubClass(TabularData.Row tabularDataRow, SubClassMappingsForTable subClassMappingsForTable) {
        if (subClassMappingsForTable == null) return generatedClass;
        Class subClass = subClassMappingsForTable.getSubClass(tabularDataRow);
        if (subClass == null) subClass = subClassMappingsForTable.getDefaultSubClass();
        if (subClass == null && subClassMappingsForTable != defaultSubClassMappingsForTable) subClass = defaultSubClassMappingsForTable.getDefaultSubClass();
        if (subClass == null) subClass = generatedClass;
        return subClass;
    }

    private void setTransformClassName(String transformClassName) {
        this.transformClassName = transformClassName;
    }

    private String getTransformClassName() {
        return transformClassName;
    }

    protected Transform getTransform() {
        if (transformClassName != null && transform == null) {
            try {
                // prevent any class load order effects while loading schemas
                transform = (Transform) ClassUtilities.getClass(transformClassName, true, false).newInstance();
            } catch (Exception e) {
                log.error(e);
            }
        }
        return transform;
    }

    private void setSqlString(String sqlString) {
        if (sqlString != null) {
            this.sqlString = sqlString;
            this.sql = createSQL(sqlString, defaultDataSourceName);
        }
    }

    private static SQL createSQL(String sqlString, String dataSourceName) {
        if (sqlString == null) return null;
        sqlString = ApplicationProperties.substituteApplicationProperties(sqlString);
        return new SQL(sqlString, dataSourceName);
    }

    public String getSqlString() {
        return sqlString;
    }

    public SQL getSql() {
        if (sql == null) throw new RuntimeException("No sql for class: " + typeName);
        return sql;
    }

    public StoredProcedure getStoredProcedure() {
        if (storedProcedure == null) throw new RuntimeException("No storedProcedure for class: " + typeName);
        return storedProcedure;
    }

    private void setStoredProcedureString(String storedProcedureString) {
        if (storedProcedureString != null) {
            this.storedProcedureString = storedProcedureString;
            this.storedProcedure = new StoredProcedure(storedProcedureString, defaultDataSourceName);
        }
    }

    private String getStoredProcedureString() {
        return storedProcedureString;
    }

    public String getKxSqlString() {
        return kxSqlString;
    }

    public void setKxSqlString(String kxSqlString) {
        this.kxSqlString = kxSqlString;
    }

    public OperationGroup getOperationGroup() {
        return operationGroup;
    }

    private void setOperationGroup(OperationGroup operationGroup) {
        if (operationGroup != null) {
            setOperationGroup(operationGroup.getOperations());
        } else {
            setOperationGroup((Operation[]) null);
        }
    }

    private void setOperationGroup(Operation[] operations) {
        if (operations != null && operations.length > 0) {
            operationGroup = new OperationGroup();
            operationGroup.setOperations(operations);
        } else {
            operationGroup = null;
        }
    }

    private void setPrimaryKey(Column[] primaryKey) {
        if (primaryKey != null && primaryKey.length > 0) {
            this.primaryKey = primaryKey;
        }
    }

    public Column[] getPrimaryKey() {
        if (primaryKey == null || primaryKey.length == 0) {
            throw new RuntimeException("Invalid primary key for type: " + typeName);
        }
        return primaryKey;
    }

    public Column[] getUnionOfKeyColumns() {
        if (unionOfKeyColumns == null) {
            Set keyColumns = new LinkedHashSet();

            for (int i = 0; i < primaryKey.length; i++) {
                Column column = primaryKey[i];
                keyColumns.add(column);
            }

            if (indicies != null) {
                for (int i = 0; i < indicies.length; i++) {
                    Index index = indicies[i];
                    Column[] columnsForIndex = index.getColumns();
                    for (int j = 0; j < columnsForIndex.length; j++) {
                        Column column = columnsForIndex[j];
                        keyColumns.add(column);
                    }
                }
            }
            unionOfKeyColumns = (Column[]) keyColumns.toArray(new Column[keyColumns.size()]);
        }
        return unionOfKeyColumns;
    }

    protected Class getType(Column column) {
        Class columnType = (Class) columnTypes.get(column);

        if (columnType == null && !columnTypes.containsKey(column)) {
            // try default property
            Property defaultProperty = getDefaultPropertyByColumn(column);
            if (defaultProperty != null) columnType = defaultProperty.getType();

            if (columnType == null) {
                // try looking in key for type
                for (int i = 0; i < primaryKey.length && columnType == null; i++) {
                    Column primaryKeyColumn = primaryKey[i];
                    if (primaryKeyColumn.equals(column)) {
                        columnType = primaryKeyColumn.getType();
                    }
                }

                if (columnType == null) {
                    // try looking in derived properties for type
                    Property[] properties = getPropertiesByColumn(column);
                    if (properties != null) {
                        for (int i = 0; i < properties.length && columnType == null; i++) {
                            DerivedProperty property = (DerivedProperty) properties[i];
                            columnType = property.getColumnType(column);
                        }
                    }

                    if (columnType == null) {
                        columnType = column.getType();
                    }

                    if (columnType == null) {
                        log.warn("Cannot determine column type for: " + typeName + "." + column.getName());
                    }
                }
            }

            columnTypes.put(column, columnType);
        }
        return columnType;
    }

    public Type getDerivedPropertyColumnsType() {
        if (derivedPropertyColumnsType == null && derivedProperties != null) {
            Set columns = new HashSet();
            for (int i = 0; i < derivedProperties.length; i++) {
                DerivedProperty derivedProperty = derivedProperties[i];
                Column[] columnsForProperty = derivedProperty.getColumns();
                for (int j = 0; j < columnsForProperty.length; j++) {
                    Column column = columnsForProperty[j];
                    columns.add(new Column(column.getName(), getType(column)));
                }
            }
            derivedPropertyColumnsType = Generic.getType((Column[]) columns.toArray(new Column[columns.size()]));
        }
        return derivedPropertyColumnsType;
    }


    protected Class getKeyType(int componentPosition, String indexName) {
        Column[] keyColumns = indexName == null ? primaryKey : getIndex(indexName).getColumns();
        Column keyColumn = keyColumns[componentPosition];
        return getType(keyColumn);
    }

    public Object changeKeyType(Object currentKey, String indexName) {
        Column[] keyColumns = indexName == null ? primaryKey : getIndex(indexName).getColumns();

        if (keyColumns.length == 1) {
            currentKey = changeKeyType(keyColumns[0], currentKey);
        } else {
            List compoundKey = (List) currentKey;
            for (int i = 0; i < keyColumns.length; i++) {
                Object keyComponent = compoundKey.get(i);
                keyComponent = changeKeyType(keyColumns[i], keyComponent);
                compoundKey.set(i, keyComponent);
            }
        }
        return currentKey;
    }

    private Object changeKeyType(Column keyColumn, Object keyComponent) {
        Class expectedClass = getType(keyColumn);
        if (expectedClass != null) {
            keyComponent = Utilities.changeType(expectedClass, keyComponent);
        }
        return keyComponent;
    }


    private void setIndicies(Index[] indexes) {
        if (indexes != null && indexes.length > 0) {
            if (indiciesMap == null) indiciesMap = new LinkedHashMap();
            for (int i = 0; i < indexes.length; i++) {
                Index index = indexes[i];
                indiciesMap.put(index.getName(), index);
            }
            indicies = (Index[]) indiciesMap.values().toArray(new Index[indiciesMap.size()]);
        }
    }

    public Index getIndex(String indexName) {
        if (indiciesMap == null) throw new RuntimeException("No indicies for class: " + typeName);
        return (Index) indiciesMap.get(indexName);
    }

    public Index[] getIndicies() {
        return indicies;
    }

    private void setDataVolatility(String dataVolatility) {
        if (this.dataVolatility != null && dataVolatility == null) return; // assume set by superType

        if (dataVolatility == null || (!dataVolatility.equalsIgnoreCase(LOW) && !dataVolatility.equalsIgnoreCase(HIGH))) {
            throw new RuntimeException("Invalid dataVolatility: " + dataVolatility + " for type: " + typeName);
        }
        this.dataVolatility = dataVolatility.toUpperCase();
    }

    public String getDataVolatility() {
        return dataVolatility;
    }

    private void setDataQuantity(String dataQuantity) {
        if (this.dataQuantity != null && dataQuantity == null) return; // assume set by superType

        if (dataQuantity == null || (!dataQuantity.equalsIgnoreCase(LOW) && !dataQuantity.equalsIgnoreCase(HIGH))) {
            throw new RuntimeException("Invalid dataQuantity: " + dataQuantity + " for type: " + typeName);
        }
        this.dataQuantity = dataQuantity.toUpperCase();
    }

    public String getDataQuantity() {
        return dataQuantity;
    }

    private void setToStringCode(String toStringCode) {
        if (toStringCode != null) {
            this.toStringCode = toStringCode;
        }
    }

    public String getToStringCode() {
        return toStringCode;
    }

    private void setDefaultDataSourceName(String defaultDataSourceName) {
        if (defaultDataSourceName != null) {
            this.defaultDataSourceName = defaultDataSourceName;
        }
    }

    public String getDefaultDataSourceName() {
        return defaultDataSourceName;
    }

    private void addProperties(Property[] properties) {
        if (properties != null && properties.length > 0) {
            for (int i = 0; i < properties.length; i++) {
                Property property = properties[i];
                addProperty(property);
            }
        }
    }

    private void addProperty(Property property) {
        if (!isValid(property)) return;

        String propertyName = property.getName();

        if (propertiesByName.containsKey(propertyName)) {
            Property oldProperty = (Property) propertiesByName.get(propertyName);
            log.warn("Replacing property: " + oldProperty + " with " + property);
            removeProperty(propertyName);
        }

        propertiesByName.put(propertyName, property);
        properties = (Property[]) propertiesByName.values().toArray(new Property[propertiesByName.values().size()]);

        if (property instanceof DerivedProperty) {
            derivedPropertiesByName.put(propertyName, property);
            derivedProperties = (DerivedProperty[]) derivedPropertiesByName.values().toArray(new DerivedProperty[derivedPropertiesByName.size()]);
        }

        Column[] columns = property.getColumns();
        for (int j = 0; j < columns.length; j++) {
            Column column = columns[j];

            if (property instanceof DefaultProperty) defaultPropertiesByColumn.put(column, property);
            Property[] existingProperties = (Property[]) propertiesByColumn.get(column);
            Property[] mergedProperties = Utilities.unionArrays(existingProperties, new Property[]{property});
            propertiesByColumn.put(column, mergedProperties);
        }

    }

    private boolean isValid(Property property) {
        if (GENERATION_MODE || sqlString == null || defaultDataSourceName == null || 
                beanFactoryType != null && !SqlBeanFactory.class.isAssignableFrom(beanFactoryType)) {
            return true;
        }

        SQL sql = createSQL(sqlString, defaultDataSourceName);

        Column[] columns = property.getColumns();
        for (int i = 0; i < columns.length; i++) {
            Column column = columns[i];
            if (!validColumnNames.contains(column.getName())) {
                log.warn("Cannot find column: " + column + " for: " + property + " in sql: " + sql.toString());
                if (strictValidation) {
                    return false;
                }
            }
        }
        return true;
    }

    private void removeProperty(String propertyName) {
        Property property = (Property) propertiesByName.get(propertyName);

        propertiesByName.remove(propertyName);
        properties = (Property[]) propertiesByName.values().toArray(new Property[propertiesByName.values().size()]);

        if (property instanceof DerivedProperty) {
            derivedPropertiesByName.remove(propertyName);
            derivedProperties = (DerivedProperty[]) derivedPropertiesByName.values().toArray(new DerivedProperty[derivedPropertiesByName.size()]);
        }

        Column[] columns = property.getColumns();
        for (int i = 0; i < columns.length; i++) {
            Column column = columns[i];

            if (property instanceof DefaultProperty) defaultPropertiesByColumn.remove(column);

            Property[] properties = (Property[]) propertiesByColumn.get(column);
            Set propertySet = new HashSet(Arrays.asList(properties));
            propertySet.remove(property);
            properties = (Property[]) propertySet.toArray(new Property[propertySet.size()]);
            propertiesByColumn.put(column, properties);
        }
    }

    public Property getProperty(String propertyName) {
        return (Property) propertiesByName.get(propertyName);
    }

    public Property[] getPropertiesByColumn(Column column) {
        return (Property[]) propertiesByColumn.get(column);
    }

    public Property[] getProperties() {
        if (properties == null) return EMPTY_PROPERTY_ARRAY;
        return properties;
    }

    public DerivedProperty[] getDerivedProperties() {
        return derivedProperties;
    }

    public DefaultProperty getDefaultPropertyByColumn(Column column) {
        return (DefaultProperty) defaultPropertiesByColumn.get(column);
    }

    private static class SubClassMappingsForTable {
        private CatalogSchemaTable catalogSchemaTable;
        private boolean defaultMapping;
        private SubClassMapping[] subClassMappings;
        private String defaultSubClassName;
        private Class defaultSubClass;

        public SubClassMappingsForTable(CatalogSchemaTable catalogSchemaTable, SubClassMapping[] subClassMappings, boolean defaultMapping, String defaultSubClassName) {
            this.catalogSchemaTable = catalogSchemaTable;
            this.subClassMappings = subClassMappings;
            this.defaultMapping = defaultMapping;
            this.defaultSubClassName = defaultSubClassName;
        }

        public CatalogSchemaTable getCatalogSchemaTable() {
            return catalogSchemaTable;
        }

        public SubClassMapping[] getSubClassMappings() {
            return subClassMappings;
        }

        public boolean isDefault() {
            return defaultMapping;
        }

        public Class getDefaultSubClass() {
            if (defaultSubClass == null && defaultSubClassName != null) {
                defaultSubClass = Schema.getInstance(defaultSubClassName).getGeneratedClass();
            }
            return defaultSubClass;
        }

        private String getDefaultSubClassName() {
            return defaultSubClassName;
        }

        public Class getSubClass(TabularData.Row tabularDataRow) {
            for (int i = 0; i < subClassMappings.length; i++) {
                SubClassMapping subClassMapping = subClassMappings[i];
                if (subClassMapping.evaluate(tabularDataRow)) {
                    return subClassMapping.getSubClass();
                }
            }
            return null;
        }

        public boolean equals(Object obj) {
            SubClassMappingsForTable other = (SubClassMappingsForTable) obj;
            return Utilities.equals(catalogSchemaTable, other.catalogSchemaTable) &&
                    Arrays.equals(subClassMappings, other.subClassMappings) &&
                    (defaultMapping == other.defaultMapping);
        }

        public int hashCode() {
            int hashCode = 1;
            hashCode = 31 * hashCode + catalogSchemaTable.hashCode();
            hashCode = 31 * hashCode + (defaultMapping ? 1 : 0);
            for (int i = 0; i < subClassMappings.length; i++) {
                SubClassMapping subClassMapping = subClassMappings[i];
                hashCode = 31 * hashCode + (subClassMapping == null ? 0 : subClassMapping.hashCode());
            }
            return hashCode;
        }
    }

    private static class SubClassMapping {
        private String subClassName;
        private Class subClass;
        private SubClassMappingCriterion[] criteria;

        public SubClassMapping(String subClassName, SubClassMappingCriterion[] criteria) {
            this.subClassName = subClassName;
            this.criteria = criteria;
        }

        public boolean evaluate(TabularData.Row tabularDataRow) {
            // all criterion must evaluate to true

            if (criteria == null || criteria.length == 0) return true;

            for (int i = 0; i < criteria.length; i++) {
                SubClassMappingCriterion criterion = criteria[i];
                if (!criterion.evaluate(tabularDataRow)) return false;
            }
            return true;
        }

        public Class getSubClass() {
            if (subClass == null) {
                subClass = Schema.getInstance(subClassName).getGeneratedClass();
            }
            return subClass;
        }

        public SubClassMappingCriterion[] getCriteria() {
            return criteria;
        }

        public String getSubClassName() {
            return subClassName;
        }

        public boolean equals(Object obj) {
            SubClassMapping other = (SubClassMapping) obj;
            return Utilities.equals(subClassName, other.subClassName) && Arrays.equals(criteria, other.criteria);
        }

        public int hashCode() {
            int hashCode = 1;
            hashCode = 31 * hashCode + subClassName.hashCode();
            for (int i = 0; i < criteria.length; i++) {
                SubClassMappingCriterion criterion = criteria[i];
                hashCode = 31 * hashCode + (criterion == null ? 0 : criterion.hashCode());
            }
            return hashCode;
        }
    }

    private static class SubClassMappingCriterion {
        private static final int OPERATOR_EQUALS = 1;
        private static final int OPERATOR_NOT_EQUALS = 2;
        private static final int OPERATOR_IN = 3;
        private static final int OPERATOR_NOT_IN = 4;
        private static final Pattern COMMA_DELIMITED_PATTERN = Pattern.compile(",");
        private Column leftOperand;
        private String rightOperand;
        private Object rightOperandToUse;
        private String operator;
        private int operatorType;

        public SubClassMappingCriterion(Column leftOperand, String operator, String rightOperand) {
            this.leftOperand = leftOperand;
            operator = operator.toUpperCase().intern();
            this.operator = operator;
            rightOperand = rightOperand.intern();
            this.rightOperand = rightOperand;
            init();
        }

        private void init() {
            if (operator.equals("=") || operator.equals("==")) {
                operatorType = OPERATOR_EQUALS;
                rightOperandToUse = rightOperand;
            } else if (operator.equals("!=") || operator.equals("<>")) {
                operatorType = OPERATOR_NOT_EQUALS;
                rightOperandToUse = rightOperand;
            } else if (operator.equals("IN")) {
                operatorType = OPERATOR_IN;
                rightOperandToUse = splitRightOperand();
            } else if (operator.equals("NOT IN")) {
                operatorType = OPERATOR_NOT_IN;
                rightOperandToUse = splitRightOperand();
            } else {
                throw new RuntimeException("Operator: " + operator + " not yet supported");
            }
        }

        private String[] splitRightOperand() {
            String[] strings = COMMA_DELIMITED_PATTERN.split(rightOperand);
            for (int i = 0; i < strings.length; i++) {
                strings[i] = strings[i].intern();
            }
            return strings;
        }

        public boolean evaluate(TabularData.Row tabularDataRow) {
            String leftOperandValue = tabularDataRow.getColumnValue(leftOperand).toString();

            if (operatorType == OPERATOR_EQUALS) {
                return leftOperandValue.equals(rightOperandToUse);
            } else if (operatorType == OPERATOR_NOT_EQUALS) {
                return !leftOperandValue.equals(rightOperandToUse);
            } else if (operatorType == OPERATOR_IN) {
                String[] rightOperands = (String[]) rightOperandToUse;
                for (int i = 0; i < rightOperands.length; i++) {
                    String rightOperand = rightOperands[i];
                    if (leftOperandValue.equals(rightOperand)) return true;
                }
                return false;
            } else if (operatorType == OPERATOR_NOT_IN) {
                String[] rightOperands = (String[]) rightOperandToUse;
                for (int i = 0; i < rightOperands.length; i++) {
                    String rightOperand = rightOperands[i];
                    if (leftOperandValue.equals(rightOperand)) return false;
                }
                return true;
            } else {
                throw new RuntimeException("Operator: " + operator + " not yet supported");
            }
        }

        public Column getLeftOperand() {
            return leftOperand;
        }

        public String getRightOperand() {
            return rightOperand;
        }

        public String getOperator() {
            return operator;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SubClassMappingCriterion)) return false;

            final SubClassMappingCriterion subClassMappingCriterion = (SubClassMappingCriterion) o;

            if (leftOperand != null ? !leftOperand.equals(subClassMappingCriterion.leftOperand) : subClassMappingCriterion.leftOperand != null) return false;
            if (operator != null ? !operator.equals(subClassMappingCriterion.operator) : subClassMappingCriterion.operator != null) return false;
            if (rightOperand != null ? !rightOperand.equals(subClassMappingCriterion.rightOperand) : subClassMappingCriterion.rightOperand != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (leftOperand != null ? leftOperand.hashCode() : 0);
            result = 29 * result + (rightOperand != null ? rightOperand.hashCode() : 0);
            result = 29 * result + (operator != null ? operator.hashCode() : 0);
            return result;
        }
    }

    public static void reinit() {
        synchronized (initLock) {
            initialised = false;
            instancesByGeneratedAndTypeName.clear();
            instancesByGeneratedName.clear();
            typeToGeneratedClass.clear();
            classToGeneratedClass.clear();
            invalidInstances.clear();
        }
    }

    public static void init() {
        // Only run once
        synchronized (initLock) {
            if (initialised) return;
            initialised = true;
            long startTime = System.currentTimeMillis();
            initImpl();
            if (log.isDebugEnabled())log.debug("init took: " + (System.currentTimeMillis() - startTime) + " millis");
        }
    }

    /**
     * N
     */
    private static void initImpl() {
        PropertyGroup propertyGroup = ApplicationProperties.getApplicationProperties().getGroup("beanFactory");

        defaultReferenceType = Schema.STRONG_REFERENCE; // default
        if (propertyGroup != null) defaultReferenceType = propertyGroup.getProperty("referenceType");
        if (!isValidReferenceType(defaultReferenceType)) throw new RuntimeException("Invalid value for reference type: " + defaultReferenceType);
        if (log.isDebugEnabled())log.debug("Default reference type has been set to: " + defaultReferenceType);

        defaultMaximumJoinTableCount = -1; // default
        Number maximumJoinTableCount = propertyGroup != null ? propertyGroup.getNumericProperty("maximumJoinTableCount") : null;
        if (maximumJoinTableCount != null) defaultMaximumJoinTableCount = maximumJoinTableCount.intValue();
        if (log.isDebugEnabled())log.debug("Default maximum join depth has been set to: " + defaultMaximumJoinTableCount);

        String schemaFileName = ApplicationProperties.getApplicationProperties().getProperty(SCHEMA_PROPERTY);
        if (schemaFileName == null) {
            log.warn("Not loading schema as no schema file defined in application properties");
        } else {
            String strictValidationString = ApplicationProperties.getApplicationProperties().getProperty(SCHEMA_STRICT_VALIDATION_PROPERTY);
            if (strictValidationString != null && strictValidationString.length() > 0) strictValidation = Boolean.valueOf(strictValidationString).booleanValue();

            SchemaReader schemaReader = new SchemaReader(schemaFileName);

            try {
                schemaReader.read();
            } catch (RuntimeException e) {
                log.error(e);
                throw e;
            }

            if (!GENERATION_MODE && strictValidation) {
                // remove properties which have a type for which the schema was invalid
                for (Iterator iterator = instancesByGeneratedName.values().iterator(); iterator.hasNext();) {
                    Schema schema = (Schema) iterator.next();
                    Property[] properties = schema.getProperties();
                    for (int i = 0; i < properties.length; i++) {
                        Property property = properties[i];
                        if (invalidInstances.contains(property.getTypeName())) {
                            schema.removeProperty(property.getName());
                            log.warn("Removing property: " + property + " type is invalid");
                        } else if (property instanceof ForeignKeyProperty && !instancesByGeneratedAndTypeName.containsKey(property.getTypeName())) {
                            schema.removeProperty(property.getName());
                            log.warn("Removing property: " + property + " cannot find type");
                        }
                    }
                }
            }
        }
    }

    protected static class OperationGroup {
        private Operation[] operations;
        private Set operationsSet = new HashSet();
        private Map operationsByIndex = new HashMap();

        public OperationGroup() {
        }

        public Operation[] getOperations() {
            return operations;
        }

        public void setOperations(Operation[] operations) {
            this.operations = operations;
            operationsByIndex.clear();
            this.operationsSet.clear();

            for (int i = 0; i < operations.length; i++) {
                Operation operation = operations[i];

                Operation existingOperation = (Operation) operationsByIndex.put(operation.getIndexName(), operation);
                if (existingOperation != null) {
                    String message = "Operation: " + operation.getName() + " is using index: " + operation.getIndexName() + " to which operation: " + existingOperation.getName() + " is already mapped";
                    log.fatal(message);
                    System.exit(1);
                }

                operationsSet.add(operation);
            }

        }

        public Operation getOperationByIndex(String indexName) {
            return (Operation) operationsByIndex.get(indexName);
        }

        public boolean containsOperation(Operation operation) {
            return operationsSet.contains(operation);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OperationGroup)) return false;

            final OperationGroup otherOperations = (OperationGroup) o;
            if (!Arrays.equals(operations, otherOperations.operations)) return false;
            return true;
        }

        public int hashCode() {
            int result = 0;
            for (int i = 0; i < operations.length; i++) {
                Operation operation = operations[i];
                result = 29 * result + operation.hashCode();
            }
            return result;
        }
    }

    private static class SchemaReader {
        private String filename;

        public SchemaReader(String filename) {
            this.filename = filename;
        }

        public void read() {
            Log.getPrimaryLoadingLog().info("Loading data definitions");
            Log.getSecondaryLoadingLog().info(" ");

            if (log.isDebug()) log.debug("Attempting to read " + filename);
            InputStream xmlStream = IOUtilities.getResourceAsStream(filename);

            if (xmlStream == null) {
                throw new RuntimeException("Could not find schema " + filename);
            }

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setCoalescing(true);
                factory.setIgnoringElementContentWhitespace(true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document schemaXml = builder.parse(xmlStream);
                Node root = schemaXml.getDocumentElement();
                parse(root);
            } catch (Throwable e) {
                log.error(e);
                throw new RuntimeException("Could not read " + filename, e);
            } finally {
                try {
                    xmlStream.close();
                } catch (IOException e) {
                    log.error(e);
                }

                Log.getPrimaryLoadingLog().info(" ");
                Log.getSecondaryLoadingLog().info(" ");
            }
        }

        private void parse(Node rootNode) {
            NodeList types = rootNode.getChildNodes();

            for (int i = 0; i < types.getLength(); i++) {
                Node typeNode = types.item(i);
                if (typeNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String nodeName = typeNode.getNodeName();
                if (nodeName.equalsIgnoreCase("type")) {
                    parseType(typeNode);
                } else if (nodeName.equalsIgnoreCase("include")) {
                    String schemaFile = getAttributeValue(typeNode, "schema");
                    SchemaReader schemaReader = new SchemaReader(schemaFile);
                    schemaReader.read();
                }
            }
        }

        private void parseType(Node typeNode) {
            boolean generate = readBoolean("generate", getAttributeValue(typeNode, "generate"), Boolean.TRUE);
            String typeName = getAttributeValue(typeNode, "name");
            String generatedClassName = getAttributeValue(typeNode, "generatedClass");

            String superTypeName = getAttributeValue(typeNode, "superType");
            String beanFactoryTypeName = getAttributeValue(typeNode, "beanFactoryType");
            String referenceType = getAttributeValue(typeNode, "referenceType");

            String defaultDataSourceName = getAttributeValue(typeNode, "defaultDataSource");
            if (defaultDataSourceName == null && superTypeName != null && Schema.hasInstance(superTypeName, false)) {
                defaultDataSourceName = Schema.getInstance(superTypeName, false).getDefaultDataSourceName();
            }
            if (defaultDataSourceName == null) defaultDataSourceName = DataSourceFactory.getDefaultDataSourceName();
            String transformerClassName = getAttributeValue(typeNode, "transformClass");

            String sqlString = getAttributeValue(typeNode, "sql");
            if (sqlString == null && superTypeName != null && Schema.hasInstance(superTypeName, false)) {
                sqlString = Schema.getInstance(superTypeName, false).getSqlString();
            }
            String storedProcedure = getAttributeValue(typeNode, "storedProcedure");
            String kxSqlString = getAttributeValue(typeNode, "kxSqlString");
            String dataVolatility = getAttributeValue(typeNode, "dataVolatility");
            String dataQuantity = getAttributeValue(typeNode, "dataQuantity");
            String toStringCode = getAttributeValue(typeNode, "toStringCode");

            Column[] primaryKey = null;
            SubClassMappingsForTable[] subClassMappingsForTables = null;
            List indices = new ArrayList();
            List properties = new ArrayList();
            List operations = new ArrayList();
            NodeList children = typeNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node childNode = children.item(i);
                if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String childNodeName = childNode.getNodeName();

                if (childNodeName.equalsIgnoreCase("primaryKey")) {
                    primaryKey = parsePrimaryKey(childNode, defaultDataSourceName);
                    if (primaryKey == null && !GENERATION_MODE && strictValidation) {
                        log.warn("Not adding schema for type: " + typeName + ", could not parse primary key");
                        invalidInstances.add(typeName);
                        invalidInstances.add(generatedClassName);
                        return;
                    }

                } else if (childNodeName.equalsIgnoreCase("index")) {
                    boolean addedSuccessfully = parseIndex(childNode, indices, defaultDataSourceName);
                    if (!addedSuccessfully && !GENERATION_MODE && strictValidation) {
                        log.warn("Not adding schema for type: " + typeName + ", could not parse indexes");
                        invalidInstances.add(typeName);
                        invalidInstances.add(generatedClassName);
                        return;
                    }
                } else if (childNodeName.equalsIgnoreCase("subClassMappings")) {
                    subClassMappingsForTables = parseSubClassMappingsForTables(childNode, defaultDataSourceName);
                    if (subClassMappingsForTables == null && !GENERATION_MODE && strictValidation) {
                        log.warn("Not adding schema for type: " + typeName + ", could not parse subClassMappings");
                        invalidInstances.add(typeName);
                        invalidInstances.add(generatedClassName);
                        return;
                    }
                } else if (childNodeName.equalsIgnoreCase("property")) {
                    Property property = parseProperty(typeName, childNode, sqlString, defaultDataSourceName);
                    properties.add(property);
                } else if (childNodeName.equalsIgnoreCase("foreignKeyProperty")) {
                    DerivedProperty derivedProperty = parseForeignKeyProperty(typeName, childNode, sqlString, defaultDataSourceName);
                    if (derivedProperty != null) properties.add(derivedProperty);
                } else if (childNodeName.equalsIgnoreCase("constructorProperty")) {
                    DerivedProperty derivedProperty = parseConstructorProperty(typeName, childNode, sqlString, defaultDataSourceName);
                    if (derivedProperty != null) properties.add(derivedProperty);
                } else if (childNodeName.equalsIgnoreCase("factoryProperty")) {
                    DerivedProperty derivedProperty = parseFactoryProperty(typeName, childNode, sqlString, defaultDataSourceName);
                    if (derivedProperty != null) properties.add(derivedProperty);
                } else if (childNodeName.equalsIgnoreCase("operation")) {
                    Operation operation = parseOperation(typeName, childNode);
                    operations.add(operation);
                }
            }

            Log.getSecondaryLoadingLog().info(ClassUtilities.getUnqualifiedClassName(typeName));

            Schema schema = new Schema(this.filename,
                    generate,
                    typeName,
                    generatedClassName,
                    superTypeName,
                    defaultDataSourceName,
                    beanFactoryTypeName,
                    referenceType,
                    transformerClassName,
                    subClassMappingsForTables,
                    toStringCode,
                    primaryKey,
                    (Index[]) indices.toArray(new Index[indices.size()]), dataVolatility, dataQuantity, (Property[]) properties.toArray(new Property[properties.size()]),
                    sqlString,
                    storedProcedure,
                    kxSqlString, 
                    (Operation[]) operations.toArray(new Operation[operations.size()]));

            Schema.addInstance(typeName, generatedClassName, schema);

        }

        private Property parseProperty(String parentType, Node propertyNode, String sql, String datasource) {
            String propertyName = getAttributeValue(propertyNode, "name");
            String type = getAttributeValue(propertyNode, "type");
            String cardinality = getAttributeValue(propertyNode, "cardinality");
            if (cardinality == null) cardinality = Property.ONE;
            String columnName = getAttributeValue(propertyNode, "column");
            Column column = columnName != null ? getColumn(columnName, type, sql, datasource, propertyNode) : null;
            return new DefaultProperty(parentType, propertyName, type, cardinality, column);
        }

        private DerivedProperty parseForeignKeyProperty(String parentType, Node foreignKeyPropertyNode, String sql, String datasource) {
            String propertyName = getAttributeValue(foreignKeyPropertyNode, "name");
            String type = getAttributeValue(foreignKeyPropertyNode, "type");
            String cardinality = getAttributeValue(foreignKeyPropertyNode, "cardinality");
            String foreignIndex = getAttributeValue(foreignKeyPropertyNode, "foreignIndex");
            boolean lazy = readBoolean("lazy", getAttributeValue(foreignKeyPropertyNode, "lazy"), null);

            DerivedProperty foreignKeyProperty;

            if (sql == null) {
                foreignKeyProperty = new ForeignKeyProperty(parentType, propertyName, type, cardinality, foreignIndex, lazy);
            } else {
                foreignKeyProperty = new SqlForeignKeyProperty(parentType, propertyName, type, cardinality, foreignIndex, lazy, defaultMaximumJoinTableCount);
            }

            boolean addedSuccessfully = addParametersToDerivedProperty(foreignKeyPropertyNode, foreignKeyProperty, sql, datasource);
            if (!addedSuccessfully) {
                log.warn("Ignoring: " + propertyName + ", cannot find all parameters");
                return null;
            }

            return foreignKeyProperty;
        }

        private boolean readBoolean(String property, String booleanString, Boolean nullMeans) {
            if (nullMeans != null && booleanString == null) {
                return nullMeans.booleanValue();
            } else if (booleanString != null && booleanString.equalsIgnoreCase("TRUE")) {
                return true;
            } else if (booleanString != null && booleanString.equalsIgnoreCase("FALSE")) {
                return false;
            } else {
                throw new IllegalArgumentException("Invalid value for: " + property + ". Valid values are TRUE or FALSE");
            }
        }

        private Operation parseOperation(String parentType, Node operationNode) {
            String name = getAttributeValue(operationNode, "name");
            String transformClassName = getAttributeValue(operationNode, "transformClass");
            String indexName = getAttributeValue(operationNode, "index");

            Operation operation = new Operation(parentType, name, indexName, transformClassName);
            addParametersToOperation(operationNode, operation);
            return operation;
        }

        private void addParametersToOperation(Node operationNode, Operation operation) {
            // Add components to derived property
            NodeList operationParameterNodes = operationNode.getChildNodes();
            List parameters = new ArrayList();
            for (int i = 0; i < operationParameterNodes.getLength(); i++) {
                Node operationParameterNode = operationParameterNodes.item(i);
                if (operationParameterNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String operationParameterNodeName = operationParameterNode.getNodeName();

                if (operationParameterNodeName.equalsIgnoreCase("keyTransform")) {
                    parameters.add(parseOperationKeyTransformParameter(operationParameterNode));
                } else if (operationParameterNodeName.equalsIgnoreCase("defaultValue")) {
                    parameters.add(parseOperationDefaultValueParameter(operationParameterNode));
                }
            }
            operation.setParameters(parameters);
        }

        private Operation.KeyTransformParameter parseOperationKeyTransformParameter(Node operationParameterNode) {
            String keyTransformClass = getAttributeValue(operationParameterNode, "class");
            return new Operation.KeyTransformParameter(keyTransformClass);
        }

        private Operation.DefaultValueParameter parseOperationDefaultValueParameter(Node operationParameterNode) {
            String valueString = getAttributeValue(operationParameterNode, "value");
            String type = getAttributeValue(operationParameterNode, "type");
            return new Operation.DefaultValueParameter(type, valueString);
        }

        private DerivedProperty parseConstructorProperty(String parentType, Node constructorPropertyNode, String sql, String datasource) {
            String propertyName = getAttributeValue(constructorPropertyNode, "name");
            String type = getAttributeValue(constructorPropertyNode, "type");
            String className = getAttributeValue(constructorPropertyNode, "class");
            boolean lazy = readBoolean("lazy", getAttributeValue(constructorPropertyNode, "lazy"), Boolean.FALSE);
            DerivedProperty constructorProperty = new ConstructorProperty(parentType, propertyName, type, className, lazy);
            boolean addedSuccessfully = addParametersToDerivedProperty(constructorPropertyNode, constructorProperty, sql, datasource);
            if (!addedSuccessfully) {
                log.warn("Ignoring: " + propertyName + ", cannot find all parameters");
                return null;
            }
            return constructorProperty;
        }

        private DerivedProperty parseFactoryProperty(String parentType, Node constructorPropertyNode, String sql, String datasource) {
            String propertyName = getAttributeValue(constructorPropertyNode, "name");
            String type = getAttributeValue(constructorPropertyNode, "type");
            String className = getAttributeValue(constructorPropertyNode, "class");
            String factoryClassName = getAttributeValue(constructorPropertyNode, "factoryClass");
            boolean lazy = readBoolean("lazy", getAttributeValue(constructorPropertyNode, "lazy"), Boolean.FALSE);
            DerivedProperty factoryProperty = new FactoryProperty(parentType, propertyName, type, className, factoryClassName, lazy);
            boolean addedSuccessfully = addParametersToDerivedProperty(constructorPropertyNode, factoryProperty, sql, datasource);
            if (!addedSuccessfully) {
                log.warn("Ignoring: " + propertyName + ", cannot find all parameters");
                return null;
            }
            return factoryProperty;
        }

        private boolean addParametersToDerivedProperty(Node derivedPropertyNode, DerivedProperty derivedProperty, String sql, String datasource) {
            // Add components to derived property
            NodeList derivedPropertyParameterNodes = derivedPropertyNode.getChildNodes();
            for (int i = 0; i < derivedPropertyParameterNodes.getLength(); i++) {
                Node derivedPropertyParameterNode = derivedPropertyParameterNodes.item(i);
                if (derivedPropertyParameterNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String derivedPropertyParameterNodeName = derivedPropertyParameterNode.getNodeName();
                DerivedProperty.Parameter parameter;

                if (derivedPropertyParameterNodeName.equalsIgnoreCase("property")) {
                    parameter = parseDerivedPropertyPropertyParameter(derivedPropertyParameterNode);
                    if (parameter == null) return false;
                    derivedProperty.addParameter(parameter);
                } else if (derivedPropertyParameterNodeName.equalsIgnoreCase("column")) {
                    parameter = parseDerivedPropertyColumnParameter(derivedPropertyParameterNode, sql, datasource);
                    if (parameter == null) return false;
                    derivedProperty.addParameter(parameter);
                } else if (derivedPropertyParameterNodeName.equalsIgnoreCase("defaultValue")) {
                    parameter = parseDerivedPropertyDefaultValueParameter(derivedPropertyParameterNode);
                    if (parameter == null) return false;
                    derivedProperty.addParameter(parameter);
                }
            }
            return true;
        }

        private Column getColumn(String columnName, String typeName, String sqlString, String datasource, Node columnNode) {
            SQL sql = null;
            try {
                sql = createSQL(sqlString, datasource);
            } catch (Throwable e) {
                // only temporary for parsing other components, if fails then null
            }
            if (sql == null) return new Column(columnName, typeName);
            TableColumn tableColumn = sql.getColumn(columnName, datasource);
            addIdFactory(columnNode, tableColumn);
            return tableColumn;
        }

        private TableColumn getColumn(String columnName, CatalogSchemaTable catalogSchemaTable, String dataSourceName) {
            return TableColumn.getInstance(columnName, catalogSchemaTable, dataSourceName);
        }

        private DerivedProperty.ColumnParameter parseDerivedPropertyColumnParameter(Node derivedPropertyParameterNode, String sql, String datasource) {
            String columnName = getAttributeValue(derivedPropertyParameterNode, "name");
            String type = getAttributeValue(derivedPropertyParameterNode, "type");
            String source = getAttributeValue(derivedPropertyParameterNode, "source");
            Column column = getColumn(columnName, type, sql, datasource, derivedPropertyParameterNode);
            if (column == null) return null;

            if (source == null) {
                return new DerivedProperty.ColumnParameter(column);
            } else {
                return new DerivedProperty.ColumnParameter(column, source, type);
            }
        }

        private DerivedProperty.DefaultValueParameter parseDerivedPropertyDefaultValueParameter(Node derivedPropertyParameterNode) {
            String valueString = getAttributeValue(derivedPropertyParameterNode, "value");
            String type = getAttributeValue(derivedPropertyParameterNode, "type");
            return new DerivedProperty.DefaultValueParameter(type, valueString);
        }

        private DerivedProperty.PropertyParameter parseDerivedPropertyPropertyParameter(Node derivedPropertyParameterNode) {
            String propertyName = getAttributeValue(derivedPropertyParameterNode, "name");
            String type = getAttributeValue(derivedPropertyParameterNode, "type");
            return new DerivedProperty.PropertyParameter(propertyName, type);
        }


        private Column[] parsePrimaryKey(Node primaryKeyNode, String dataSourceName) {
            NodeList primaryKeyColumnNodes = primaryKeyNode.getChildNodes();
            List primaryKeyColumns = new ArrayList();
            for (int i = 0; i < primaryKeyColumnNodes.getLength(); i++) {
                Node primaryKeyColumnNode = primaryKeyColumnNodes.item(i);
                if (primaryKeyColumnNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String primaryKeyColumnNodeName = primaryKeyColumnNode.getNodeName();

                if (primaryKeyColumnNodeName.equalsIgnoreCase("column")) {
                    Column primaryKeyColumn = createColumn(primaryKeyColumnNode, dataSourceName);
                    if (primaryKeyColumn == null) return null;
                    primaryKeyColumns.add(primaryKeyColumn);
                }
            }

            return (Column[]) primaryKeyColumns.toArray(new Column[primaryKeyColumns.size()]);
        }

        private boolean parseIndex(Node indexNode, List indicies, String dataSourceName) {
            NodeList indexColumnNodes = indexNode.getChildNodes();
            String indexName = getAttributeValue(indexNode, "name");
            boolean unique = readBoolean("unique", getAttributeValue(indexNode, "unique"), null);

            List indexColumns = new ArrayList();
            for (int i = 0; i < indexColumnNodes.getLength(); i++) {
                Node indexColumnNode = indexColumnNodes.item(i);
                if (indexColumnNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String indexColumnNodeName = indexColumnNode.getNodeName();

                if (indexColumnNodeName.equalsIgnoreCase("column")) {
                    Column indexColumn = createColumn(indexColumnNode, dataSourceName);
                    if (indexColumn == null) return false;
                    indexColumns.add(indexColumn);
                }
            }

            Index index = new Index(indexName, unique, (Column[]) indexColumns.toArray(new Column[indexColumns.size()]));
            indicies.add(index);
            return true;
        }

        private Column createColumn(Node columnNode, String dataSourceName) {
            String columnName = getAttributeValue(columnNode, "name");
            String tableName = getAttributeValue(columnNode, "tableName");
            Column column;

            if (tableName != null) {
                CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(tableName, dataSourceName);
                TableColumn tableColumn = TableColumn.getInstance(columnName, catalogSchemaTable, dataSourceName);
                column = tableColumn;
                addIdFactory(columnNode, tableColumn);
            } else {
                column = new Column(columnName);
            }

            return column;
        }

        private void addIdFactory(Node columnNode, TableColumn tableColumn) {
            String idFactoryName = getAttributeValue(columnNode, "idFactory");
            try {
                // prevent any class load order effects while loading schemas, keep as string
                if (tableColumn != null) tableColumn.setIdFactoryName(idFactoryName);
            } catch (Exception e) {
                log.error(e);
                throw new RuntimeException(e);
            }
        }

        private SubClassMappingsForTable[] parseSubClassMappingsForTables(Node subClassMappingsNode, String dataSourceName) {
            NodeList subClassMappingsForTablesNodes = subClassMappingsNode.getChildNodes();
            List subClassMappingsForTables = new ArrayList();
            for (int i = 0; i < subClassMappingsForTablesNodes.getLength(); i++) {
                Node subClassMappingsForTableNode = subClassMappingsForTablesNodes.item(i);
                if (subClassMappingsForTableNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String subClassMappingsForTableNodeName = subClassMappingsForTableNode.getNodeName();
                if (subClassMappingsForTableNodeName.equalsIgnoreCase("table")) {
                    String tableName = getAttributeValue(subClassMappingsForTableNode, "name");
                    CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(tableName, dataSourceName);
                    boolean defaultMapping = readBoolean("default", getAttributeValue(subClassMappingsForTableNode, "default"), null);
                    String defaultSubClassName = getAttributeValue(subClassMappingsForTableNode, "defaultSubClass");
                    SubClassMapping[] subClassMappings = parseSubClassMappingsForTable(subClassMappingsForTableNode, catalogSchemaTable, dataSourceName);
                    if (subClassMappings == null) return null;
                    SubClassMappingsForTable subClassMappingsForTable = new SubClassMappingsForTable(catalogSchemaTable, subClassMappings, defaultMapping, defaultSubClassName);
                    subClassMappingsForTables.add(subClassMappingsForTable);
                }
            }

            return (SubClassMappingsForTable[]) subClassMappingsForTables.toArray(new SubClassMappingsForTable[subClassMappingsForTables.size()]);
        }

        private SubClassMapping[] parseSubClassMappingsForTable(Node subClassMappingsForTableNode, CatalogSchemaTable catalogSchemaTable, String dataSourceName) {
            NodeList subClassMappingsForTableNodes = subClassMappingsForTableNode.getChildNodes();
            List subClassMappingsForTable = new ArrayList();
            for (int i = 0; i < subClassMappingsForTableNodes.getLength(); i++) {
                Node subClassMappingNode = subClassMappingsForTableNodes.item(i);
                if (subClassMappingNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String subClassMappingNodeName = subClassMappingNode.getNodeName();
                if (subClassMappingNodeName.equalsIgnoreCase("subClassMapping")) {
                    String subClassName = getAttributeValue(subClassMappingNode, "name");
                    SubClassMappingCriterion[] criteria = parseSubClassMappingCriteria(subClassMappingNode, catalogSchemaTable, dataSourceName);
                    if (criteria == null) return null;
                    SubClassMapping subClassMapping = new SubClassMapping(subClassName, criteria);
                    subClassMappingsForTable.add(subClassMapping);
                }
            }

            return (SubClassMapping[]) subClassMappingsForTable.toArray(new SubClassMapping[subClassMappingsForTable.size()]);
        }


        private SubClassMappingCriterion[] parseSubClassMappingCriteria(Node subClassMappingCriteriaNode, CatalogSchemaTable catalogSchemaTable, String dataSourceName) {
            List criteria = new ArrayList();
            NodeList criterionNodes = subClassMappingCriteriaNode.getChildNodes();
            for (int p = 0; p < criterionNodes.getLength(); p++) {
                Node criterionNode = criterionNodes.item(p);
                if (criterionNode.getNodeType() != Node.ELEMENT_NODE) continue;

                String criterionNodeName = criterionNode.getNodeName();
                if (criterionNodeName.equalsIgnoreCase("criterion")) {
                    String leftOperand = getAttributeValue(criterionNode, "leftOperand");
                    Column leftOperandColumn = getColumn(leftOperand, catalogSchemaTable, dataSourceName);
                    if (leftOperandColumn == null) return null;
                    String operator = getAttributeValue(criterionNode, "operator");
                    String rightOperand = getAttributeValue(criterionNode, "rightOperand");
                    SubClassMappingCriterion criterion = new SubClassMappingCriterion(leftOperandColumn, operator, rightOperand);
                    criteria.add(criterion);
                }
            }
            return (SubClassMappingCriterion[]) criteria.toArray(new SubClassMappingCriterion[criteria.size()]);
        }

        private String getAttributeValue(Node node, String attribute) {
            Node attributeNode = node.getAttributes().getNamedItem(attribute);
            if (attributeNode == null) return null;
            return attributeNode.getNodeValue();
        }
    }

    public static class SchemaWriter {
        private static final String INDENT = "    ";
        public static final String NL = "\n";

        private Schema[] schemas;
        private Map buffers = new HashMap();

        public SchemaWriter(Schema[] schemas) {
            this.schemas = schemas;
        }

        public static void writeBlankSchema(String filename) throws IOException {
            StringBuffer buffer = new StringBuffer();
            appendHeader(buffer);
            appendFooter(buffer);
            IOUtilities.writeStringToFile(filename, buffer.toString());
        }

        public void write() throws IOException {
            for (int i = 0; i < schemas.length; i++) {
                Schema schema = schemas[i];
                StringBuffer buffer = getBuffer(schema.getSchemaFilename());
                writeSchema(buffer, schema);
            }

            writeBuffers();
        }

        private StringBuffer getBuffer(String filename) {
            StringBuffer buffer = (StringBuffer) buffers.get(filename);
            if (buffer == null) {
                buffer = new StringBuffer(10000);
                appendHeader(buffer);
                buffers.put(filename, buffer);
            }
            return buffer;
        }

        private void writeBuffers() throws IOException {
            for (Iterator iterator = buffers.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String filename = (String) entry.getKey();
                StringBuffer buffer = (StringBuffer) entry.getValue();
                appendFooter(buffer);
                IOUtilities.writeStringToFile(filename, buffer.toString());
            }
        }

        private void writeAttribute(StringBuffer buffer, String attributeName, boolean attributeValue, int indents, boolean onNewLine) {
            String attributeValueSting = attributeValue ? "true" : "false";
            writeAttribute(buffer, attributeName, attributeValueSting, indents, onNewLine);
        }

        private void writeAttribute(StringBuffer buffer, String attributeName, String attributeValue, int indents, boolean onNewLine) {
            if (attributeValue == null) return;
            if (onNewLine) {
                buffer.append(NL);
                for (int i = 0; i < indents; i++) {
                    buffer.append(INDENT);
                }
            } else {
                buffer.append(" ");
            }
            buffer.append(attributeName).append("=\"").append(attributeValue).append("\"");
        }

        private static void appendHeader(StringBuffer buffer) {
            buffer.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            buffer.append(NL);
            buffer.append(NL);
            buffer.append("<schema>");
            buffer.append(NL);
        }

        private static void appendFooter(StringBuffer buffer) {
            buffer.append("</schema>");
            buffer.append(NL);
        }

        private void writeSchema(StringBuffer buffer, Schema schema) {
            buffer.append(NL);
            buffer.append(INDENT).append("<type");
            writeAttribute(buffer, "name", schema.getTypeName(), 0, false);

            writeAttribute(buffer, "generatedClass", schema.getGeneratedClassName(), 2, true);
            writeAttribute(buffer, "generate", schema.isGenerate(), 2, true);
            writeAttribute(buffer, "superType", schema.getSuperTypeName(), 2, true);
            writeAttribute(buffer, "beanFactoryType", schema.getBeanFactoryTypeName(), 2, true);
            writeAttribute(buffer, "referenceType", schema.referenceType, 2, true); // only if not default reference strength

            String superTypeName = schema.getSuperTypeName();
            Schema superSchema = null;
            if (superTypeName != null) superSchema = Schema.getInstance(superTypeName);

            if (superSchema == null || !Utilities.equals(schema.getDefaultDataSourceName(), superSchema.getDefaultDataSourceName())) writeAttribute(buffer, "defaultDataSource", schema.getDefaultDataSourceName(), 2, true);
            if (superSchema == null || !Utilities.equals(schema.getTransformClassName(), superSchema.getTransformClassName())) writeAttribute(buffer, "transformClass", schema.getTransformClassName(), 2, true);
            if (superSchema == null || !Utilities.equals(schema.getSqlString(), superSchema.getSqlString())) writeAttribute(buffer, "sql", schema.getSqlString(), 2, true);
            if (superSchema == null || !Utilities.equals(schema.getStoredProcedureString(), superSchema.getStoredProcedureString())) writeAttribute(buffer, "storedProcedure", schema.getStoredProcedureString(), 2, true);
            if (superSchema == null || !Utilities.equals(schema.getKxSqlString(), superSchema.getKxSqlString())) writeAttribute(buffer, "kxSqlString", schema.getKxSqlString(), 2, true);
            if (superSchema == null || !Utilities.equals(schema.getDataVolatility(), superSchema.getDataVolatility())) writeAttribute(buffer, "dataVolatility", schema.getDataVolatility(), 2, true);
            if (superSchema == null || !Utilities.equals(schema.getDataQuantity(), superSchema.getDataQuantity())) writeAttribute(buffer, "dataQuantity", schema.getDataQuantity(), 2, true);
            if (superSchema == null || !Utilities.equals(schema.getToStringCode(), superSchema.getToStringCode())) writeAttribute(buffer, "toStringCode", schema.getToStringCode(), 2, true);
            buffer.append(">");

            writeProperties(buffer, schema);
            writeOperations(buffer, schema);
            writePrimaryKey(buffer, schema);
            writeIndicies(buffer, schema);
            writeSubClassMappingsForTables(buffer, schema);

            buffer.append(NL);
            buffer.append(INDENT).append("</type>");
            buffer.append(NL);
        }

        private void writePrimaryKey(StringBuffer buffer, Schema schema) {
            if (schema.primaryKey == null) return;

            String superTypeName = schema.getSuperTypeName();
            Schema superSchema;
            if (superTypeName != null) {
                superSchema = Schema.getInstance(superTypeName);
                if (Arrays.equals(schema.primaryKey, superSchema.primaryKey)) return;
            }

            buffer.append(NL);
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append("<primaryKey>");
            buffer.append(NL);
            for (int i = 0; i < schema.primaryKey.length; i++) {
                writeColumn(buffer, schema.primaryKey[i]);
            }

            buffer.append(INDENT).append(INDENT).append("</primaryKey>");
        }

        private void writeIndicies(StringBuffer buffer, Schema schema) {
            Index[] indicies = schema.getIndicies();
            if (indicies == null) return;

            String superTypeName = schema.getSuperTypeName();
            Schema superSchema = null;
            if (superTypeName != null) {
                superSchema = Schema.getInstance(superTypeName);
            }

            boolean firstIndex = true;
            for (int i = 0; i < indicies.length; i++) {
                Index index = indicies[i];

                if (superSchema != null) {
                    Index superIndex = superSchema.getIndex(index.getName());
                    if (Utilities.equals(index, superIndex)) continue;
                }

                if (firstIndex) {
                    buffer.append(NL);
                    firstIndex = false;
                }
                buffer.append(NL);
                buffer.append(INDENT).append(INDENT).append("<index");
                writeAttribute(buffer, "name", index.getName(), 0, false);
                writeAttribute(buffer, "unique", index.isUnique(), 0, false);
                buffer.append(">");
                buffer.append(NL);
                Column[] indexColumns = index.getColumns();
                for (int j = 0; j < indexColumns.length; j++) {
                    writeColumn(buffer, indexColumns[j]);
                }

                buffer.append(INDENT).append(INDENT).append("</index>");
                buffer.append(NL);
            }
        }

        private void writeColumn(StringBuffer buffer, Column column) {
            buffer.append(INDENT).append(INDENT).append(INDENT).append("<column");
            writeAttribute(buffer, "name", column.getName(), 0, false);
            if (column instanceof TableColumn) {
                TableColumn tableColumn = (TableColumn) column;
                writeAttribute(buffer, "tableName", tableColumn.getCatalogSchemaTable().getOriginalRepresentation(), 0, false);
                if (tableColumn.getIdFactoryName() != null) writeAttribute(buffer, "idFactory", tableColumn.getIdFactoryName(), 0, false);
            }
            buffer.append("/>");
            buffer.append(NL);
        }

        private void writeProperties(StringBuffer buffer, Schema schema) {
            Property[] properties = schema.getProperties();

            if (properties.length > 0) {
                buffer.append(NL);
                String superTypeName = schema.getSuperTypeName();
                Schema superSchema = null;
                if (superTypeName != null) superSchema = Schema.getInstance(superTypeName);

                for (int i = 0; i < properties.length; i++) {
                    Property property = properties[i];

                    if (superSchema != null) {
                        boolean superTypeHasProperty = superSchema.propertiesByName.containsValue(property);
                        if (superTypeHasProperty) continue;
                    }

                    if (property instanceof ConstructorProperty) {
                        writeConstructorProperty(buffer, (ConstructorProperty) property);
                    } else if (property instanceof ForeignKeyProperty) {
                        writeForeignKeyProperty(buffer, (ForeignKeyProperty) property);
                    } else {
                        writeDefaultProperty(buffer, (DefaultProperty) property);
                    }
                }
            }
        }

        private void writeDefaultProperty(StringBuffer buffer, DefaultProperty property) {
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append("<property");
            String propertyName = property.getName();
            writeAttribute(buffer, "name", propertyName, 0, false);
            writeAttribute(buffer, "type", property.getTypeName(), 0, false);
            if (property.getColumns().length == 1) writeAttribute(buffer, "column", property.getColumns()[0].getName(), 0, false);
            if (property.getCardinality().equals(Property.MANY)) writeAttribute(buffer, "cardinality", property.getCardinality(), 0, false);
            buffer.append("/>");
        }

        private void writeForeignKeyProperty(StringBuffer buffer, ForeignKeyProperty property) {
            buffer.append(NL);
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append("<foreignKeyProperty");
            String propertyName = property.getName();
            writeAttribute(buffer, "name", propertyName, 0, false);
            writeAttribute(buffer, "type", property.getTypeName(), 3, true);
            writeAttribute(buffer, "cardinality", property.getCardinality(), 3, true);
            writeAttribute(buffer, "foreignIndex", property.getForeignIndex(), 3, true);
            writeAttribute(buffer, "lazy", property.isLazy(), 3, true);
            buffer.append(">");
            writeDerivedPropertyParameters(buffer, property);
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append("</foreignKeyProperty>");

        }

        private void writeConstructorProperty(StringBuffer buffer, ConstructorProperty property) {
            buffer.append(NL);
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append("<constructorProperty");
            String propertyName = property.getName();
            writeAttribute(buffer, "name", propertyName, 0, false);
            writeAttribute(buffer, "type", property.getTypeName(), 3, true);
            writeAttribute(buffer, "class", property.getPropertyClassName(), 3, true);
            writeAttribute(buffer, "lazy", property.isLazy(), 3, true);
            buffer.append(">");
            writeDerivedPropertyParameters(buffer, property);
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append("</constructorProperty>");
        }

        private void writeDerivedPropertyParameters(StringBuffer buffer, DerivedProperty property) {
            List parameters = property.getParameterList();
            for (Iterator iterator = parameters.iterator(); iterator.hasNext();) {
                DerivedProperty.Parameter parameter = (DerivedProperty.Parameter) iterator.next();
                buffer.append(NL);
                if (parameter instanceof DerivedProperty.ColumnParameter) {
                    DerivedProperty.ColumnParameter columnParameter = (DerivedProperty.ColumnParameter) parameter;
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("<column");
                    writeAttribute(buffer, "name", columnParameter.getColumn().getName(), 0, false);
                    writeAttribute(buffer, "source", columnParameter.getBeanPathString(), 0, false);
                    writeAttribute(buffer, "type", columnParameter.getTypeName(), 0, false);
                    buffer.append("/>");
                } else if (parameter instanceof DerivedProperty.PropertyParameter) {
                    DerivedProperty.PropertyParameter propertyParameter = (DerivedProperty.PropertyParameter) parameter;
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("<property");
                    writeAttribute(buffer, "name", propertyParameter.getName(), 0, false);
                    writeAttribute(buffer, "type", propertyParameter.getTypeName(), 0, false);
                    buffer.append("/>");

                } else if (parameter instanceof DerivedProperty.DefaultValueParameter) {
                    DerivedProperty.DefaultValueParameter defaultValueParameter = (DerivedProperty.DefaultValueParameter) parameter;
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("<defaultValue");
                    writeAttribute(buffer, "value", defaultValueParameter.getValueString(), 0, false);
                    writeAttribute(buffer, "type", defaultValueParameter.getTypeName(), 0, false);
                    buffer.append("/>");
                }
            }
        }

        private void writeOperations(StringBuffer buffer, Schema schema) {
            OperationGroup operationGroup = schema.getOperationGroup();
            if (operationGroup != null) {
                buffer.append(NL);

                String superTypeName = schema.getSuperTypeName();
                OperationGroup superOperationGroup = null;
                if (superTypeName != null) superOperationGroup = Schema.getInstance(superTypeName).getOperationGroup();

                Operation[] operations = operationGroup.getOperations();
                for (int i = 0; i < operations.length; i++) {
                    Operation operation = operations[i];
                    if (superOperationGroup != null && superOperationGroup.containsOperation(operation)) continue;
                    writeOperation(buffer, operation);
                }
            }
        }

        private void writeOperation(StringBuffer buffer, Operation operation) {
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append("<operation");
            writeAttribute(buffer, "name", operation.getName(), 0, false);
            writeAttribute(buffer, "index", operation.getIndexName(), 3, true);
            writeAttribute(buffer, "transformClassName", operation.getTransformClassName(), 3, true);
            buffer.append(">");
            writeOperationParameters(buffer, operation);
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append("</operation>");
        }

        private void writeOperationParameters(StringBuffer buffer, Operation operation) {
            Operation.Parameter[] parameters = operation.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Operation.Parameter parameter = parameters[i];
                buffer.append(NL);
                if (parameter instanceof Operation.KeyTransformParameter) {
                    Operation.KeyTransformParameter keyTransformParameter = (Operation.KeyTransformParameter) parameter;
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("<keyTransform");
                    writeAttribute(buffer, "class", keyTransformParameter.getKeyTransformClassName(), 0, false);
                    buffer.append("/>");

                } else if (parameter instanceof Operation.DefaultValueParameter) {
                    Operation.DefaultValueParameter defaultValueParameter = (Operation.DefaultValueParameter) parameter;
                    buffer.append(INDENT).append(INDENT).append(INDENT).append("<defaultValue");
                    writeAttribute(buffer, "value", defaultValueParameter.getValueString(), 0, false);
                    writeAttribute(buffer, "type", defaultValueParameter.getTypeName(), 0, false);
                    buffer.append("/>");
                }
            }
        }

        private void writeSubClassMappingsForTables(StringBuffer buffer, Schema schema) {
            SubClassMappingsForTable[] subClassMappingsForTables = schema.getSubClassMappingsForTables();
            if (subClassMappingsForTables == null) return;

            // remove subClassMappings that the superSchema already has
            String superTypeName = schema.getSuperTypeName();
            Schema superSchema;
            if (superTypeName != null) {
                superSchema = Schema.getInstance(superTypeName);
                List differentSubClassMappingsForTables = new ArrayList();
                for (int i = 0; i < subClassMappingsForTables.length; i++) {
                    SubClassMappingsForTable subClassMappingsForTable = subClassMappingsForTables[i];
                    if (!superSchema.subClassMappingsByTable.containsValue(subClassMappingsForTable)) {
                        differentSubClassMappingsForTables.add(subClassMappingsForTable);
                    }
                }
                subClassMappingsForTables = (SubClassMappingsForTable[]) differentSubClassMappingsForTables.toArray(new SubClassMappingsForTable[differentSubClassMappingsForTables.size()]);
            }

            if (subClassMappingsForTables.length > 0) {
                buffer.append(NL);
                buffer.append(NL);
                buffer.append(INDENT).append(INDENT).append("<subClassMappings>");
                for (int i = 0; i < subClassMappingsForTables.length; i++) {
                    SubClassMappingsForTable subClassMappingsForTable = subClassMappingsForTables[i];
                    writeSubClassMappingForTable(buffer, subClassMappingsForTable);
                }
                buffer.append(NL);
                buffer.append(INDENT).append(INDENT).append("</subClassMappings>");
            }
        }

        private void writeSubClassMappingForTable(StringBuffer buffer, SubClassMappingsForTable subClassMappingsForTable) {
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append(INDENT).append("<table");
            writeAttribute(buffer, "name", subClassMappingsForTable.getCatalogSchemaTable().getOriginalRepresentation(), 0, false);
            writeAttribute(buffer, "default", subClassMappingsForTable.isDefault(), 0, false);
            writeAttribute(buffer, "defaultSubClass", subClassMappingsForTable.getDefaultSubClassName(), 0, false);
            buffer.append(">");
            SubClassMapping[] subClassMappings = subClassMappingsForTable.getSubClassMappings();
            for (int j = 0; j < subClassMappings.length; j++) {
                SubClassMapping subClassMapping = subClassMappings[j];
                writeSubClassMapping(buffer, subClassMapping);
            }
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append(INDENT).append("</table>");
            buffer.append(NL);
        }

        private void writeSubClassMapping(StringBuffer buffer, SubClassMapping subClassMapping) {
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("<subClassMapping");
            writeAttribute(buffer, "name", subClassMapping.getSubClassName(), 0, false);
            buffer.append(">");
            SubClassMappingCriterion[] subClassMappingCriteria = subClassMapping.getCriteria();
            for (int i = 0; i < subClassMappingCriteria.length; i++) {
                SubClassMappingCriterion subClassMappingCriterion = subClassMappingCriteria[i];
                writeSubClassMappingCriterion(buffer, subClassMappingCriterion);
            }
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("</subClassMapping>");
        }

        private void writeSubClassMappingCriterion(StringBuffer buffer, SubClassMappingCriterion subClassMappingCriterion) {
            buffer.append(NL);
            buffer.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("<criterion");
            writeAttribute(buffer, "leftOperand", subClassMappingCriterion.getLeftOperand().getName(), 0, false);
            writeAttribute(buffer, "rightOperand", subClassMappingCriterion.getRightOperand(), 0, false);
            writeAttribute(buffer, "operator", subClassMappingCriterion.getOperator(), 0, false);
            buffer.append("/>");
        }
    }

    public static void main(String[] args) throws IOException {
        SchemaWriter schemaWriter = new SchemaWriter(Schema.getInstances());
        schemaWriter.write();
    }

}