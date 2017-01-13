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
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.5 $
 */
public abstract class CodeGenerator {
    private static final Log log = Log.getCategory(CodeGenerator.class);

    protected abstract void startGenerateClass(String classToGenerate);

    protected abstract void endGenerateClass(String classToGenerate);

    protected abstract void appendHeader(String classToGenerate, String superClassOfGeneratedBean, String superClassOfBean, Class[] interfacesToImplement);

    protected abstract void appendConstructor(String classToGenerate, Class[] constructorParameters);

    protected abstract void appendField(String propertyName, String propertyType);

    protected abstract void appendGetter(String propertyName, String propertyType, boolean lazy);

    protected abstract void appendCollectionGetter(String propertyName, String componentPropertyType, boolean lazy, String foreignIndex);

    protected abstract void appendArrayGetter(String propertyName, String propertyType, String componentPropertyType, String collectionPropertyName);

    protected abstract void appendSetter(String propertyName, String propertyType, String superClass, boolean lazy, boolean many, boolean hasSuperSetter);

    protected abstract void appendToString(String toStringCode);

    public CodeGenerator() {
    }

    public void run() {
        Schema[] schemas = Schema.getInstances();
        for (int i = 0; i < schemas.length; i++) {
            Schema schema = schemas[i];
            String classToGenerate = schema.getGeneratedClassName();
            log.info(">>>>>>>>");
            if (schema.isGenerate()) {
                generateClass(schema);
            } else {
                log.info("Not writing " + classToGenerate + " as generate is set to false");
            }
        }
    }

    protected void generateClass(Schema schema) {
        String classToGenerate = schema.getGeneratedClassName();
        startGenerateClass(classToGenerate);

        String superClassOfGeneratedBean = schema.getSuperClassNameOfGeneratedBean();
        String superClassOfBean = schema.getSuperClassNameOfBean();
        Property[] properties = schema.getProperties();
        String toStringCode = schema.getToStringCode();

        Set interfacesToImplement = getInterfacesToImplement(superClassOfGeneratedBean);
        appendHeader(classToGenerate, superClassOfGeneratedBean, superClassOfBean, (Class[]) interfacesToImplement.toArray(new Class[interfacesToImplement.size()]));
        appendConstructors(classToGenerate, superClassOfGeneratedBean);
        appendProperties(classToGenerate, superClassOfGeneratedBean, properties);

        if (toStringCode == null) {
            if (log.isDebug()) log.debug("Not adding toString method as it is not defined in Schema");
        } else if (hasToString(superClassOfGeneratedBean)) {
            log.info("Not adding toString method as it already exists in: " + superClassOfGeneratedBean);
        } else {
            appendToString(toStringCode);
        }

        endGenerateClass(classToGenerate);
    }

    public static Set getInterfacesToImplement(String superClassName) {
        Class superClass = ClassUtilities.getClass(superClassName, false, false);

        Set interfacesToImplement = new LinkedHashSet();
        if (superClass == null || !Lazy.class.isAssignableFrom(superClass) || !ClassUtilities.implementsClassOrInterface(superClass, Lazy.class)) {
            interfacesToImplement.add(Lazy.class);
        }

        if (superClass == null || !Indexable.class.isAssignableFrom(superClass) || !ClassUtilities.implementsClassOrInterface(superClass, Indexable.class)) {
            interfacesToImplement.add(Indexable.class);
        }
        return interfacesToImplement;
    }

    protected void appendConstructors(String classToGenerate, String superClassName) {
        // default constructor
        appendConstructor(classToGenerate, new Class[0]);

        // other constructors
        Class superClass = ClassUtilities.getClass(superClassName, false, false);
        if (superClass != null) {
            Constructor[] constructors = superClass.getConstructors();
            for (int i = 0; i < constructors.length; i++) {
                Constructor constructor = constructors[i];
                Class[] constructorParameters = constructor.getParameterTypes();
                if (constructorParameters.length > 0) {
                    appendConstructor(classToGenerate, constructorParameters);
                }
            }
        }
    }

    private void appendProperties(String classToGenerate, String superClass, Property[] properties) {
        for (int i = 0; i < properties.length; i++) {
            appendProperty(classToGenerate, superClass, properties[i]);
        }
    }

    protected void appendProperty(String classToGenerate, String superClass, Property property) {
        String propertyName = property.getName();
        String propertyType = property.getTypeName();

        if (hasGetter(propertyName, propertyType, superClass, property.getCardinality())) {
            if (log.isDebugEnabled())log.debug("Getter for " + propertyName + " already exists in: " + superClass + " not adding property to: " + classToGenerate);
            return;
        }

        boolean callSuperSetter = hasSetter(propertyName, propertyType, superClass, property.getCardinality());

        if (callSuperSetter) {
            if (log.isDebugEnabled())log.debug("Setter for " + propertyName + " already exists in: " + superClass + " adding call to super.set in: " + classToGenerate);
        }

        appendProperty(superClass, property, callSuperSetter);
    }

    protected void appendProperty(String superClass, Property property, boolean callSuperSetter) {
        String propertyName = property.getName();
        String propertyType = property.getTypeName();

        boolean lazy = false;
        if (property instanceof DerivedProperty) {
            DerivedProperty derivedProperty = (DerivedProperty) property;
            lazy = derivedProperty.isLazy();
        }

        if (property.getCardinality().equals(Property.ONE)) {
            appendField(propertyName, propertyType);
            appendGetter(propertyName, propertyType, lazy);
            appendSetter(propertyName, propertyType, superClass, lazy, false, callSuperSetter);
        } else {
            ForeignKeyProperty foreignKeyProperty = (ForeignKeyProperty) property;
            String foreignIndex = foreignKeyProperty.getForeignIndex();

            if (foreignIndex == null) {
                log.error("No foreign index for " + propertyName + " not adding to generated type");
                return;
            }

            String collectionPropertyName = propertyName + "Collection";
            appendField(collectionPropertyName, BeanCollection.class.getName());
            appendCollectionGetter(collectionPropertyName, propertyType, lazy, foreignIndex);
            appendArrayGetter(propertyName, propertyType + "[]", propertyType, collectionPropertyName);
            appendSetter(collectionPropertyName, BeanCollection.class.getName(), superClass, lazy, true, callSuperSetter);
        }
    }

    protected static String getGetMethodName(String propertyType, String propertyName, boolean includeBrackets) {
        String getMethodName;
        if (!propertyType.equals("boolean")) {
            getMethodName = "get" + Utilities.capitalise(propertyName);
        } else {
            getMethodName = "is" + Utilities.capitalise(propertyName);
        }
        if (includeBrackets) {
            getMethodName = getMethodName + "()";
        }
        return getMethodName;
    }

    protected static String getSetMethodName(String propertyName) {
        return "set" + Utilities.capitalise(propertyName);
    }

    public static boolean hasGetter(String propertyName, String propertyType, String superClass, String cardinality) {
        if (cardinality.equals(Property.ONE)) {
            String methodName = getGetMethodName(propertyType, propertyName, false);
            return hasMethod(superClass, methodName, null, null);
        } else {
            String collectionGetterMethodName = getGetMethodName(BeanCollection.class.getName(), propertyName + "Collection", false);
            boolean hasCollectionGetterMethod = hasMethod(superClass, collectionGetterMethodName, null, null);
            if (hasCollectionGetterMethod) return true;

            String arrayGetterMethodName = getGetMethodName(propertyType + "[]", propertyName, false);
            boolean hasArrayGetterMethod = hasMethod(superClass, arrayGetterMethodName, null, null);
            if (hasArrayGetterMethod) return true;

            return false;
        }
    }

    private static boolean hasToString(String superClass) {
        String methodName = "toString";
        return hasMethod(superClass, methodName, null, Object.class);
    }

    public static boolean hasSetter(String propertyName, String propertyType, String superClass, String cardinality) {
        String modifiedPropertyName = cardinality.equals(Property.ONE) ? propertyName : propertyName + "Collection";
        String modifiedPropertyType = cardinality.equals(Property.ONE) ? propertyType : BeanCollection.class.getName();

        String methodName = "set" + Utilities.capitalise(modifiedPropertyName);
        // prevent any class load order effects while loading schemas
        final Class propertyTypeClass = ClassUtilities.getClass(modifiedPropertyType, false, false);
        if (propertyTypeClass == null) return false;
        return hasMethod(superClass, methodName, new Class[]{propertyTypeClass}, null);
    }

    private static boolean hasMethod(String type, String methodName, Class[] parameterTypesToCheck, Class stopClass) {
        // prevent any class load order effects while loading schemas
        Class aClass = ClassUtilities.getClass(type, false, false);
        return ClassUtilities.getMethod(aClass, methodName, parameterTypesToCheck, stopClass) != null;
    }

    protected static void writeStringToFile(String filename, String text) throws IOException {
        if (filename == null) throw new IllegalArgumentException("Filename is null");
        if (text == null) throw new IllegalArgumentException("Text is null");

        FileWriter outfile;
        try {
            outfile = new FileWriter(filename);
        } catch (FileNotFoundException e) {
            log.error("Not writing file: " + filename + " could not find path");
            return;
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(outfile), true); //autoflush

        try {
            out.println(text);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
