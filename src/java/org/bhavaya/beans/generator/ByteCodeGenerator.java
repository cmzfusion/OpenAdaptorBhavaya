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

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;
import org.bhavaya.beans.ForeignKeyProperty;
import org.bhavaya.beans.Property;
import org.bhavaya.beans.Schema;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Set;

public class ByteCodeGenerator {
    private static final Log log = Log.getCategory(ByteCodeGenerator.class);

    private static final boolean DEBUG = false;

    private static final int COLLECTION = 4;
    private static final int LAZY = 2;
    private static final int CALL_SUPER_SETTER = 1;

    private static final String PACKAGE = ClassUtilities.getPackageName(ByteCodeGenerator.class) + ".";
    private static final String ESCAPED_PACKAGE = PACKAGE.replaceAll("\\.", "\\\\.");
    private static final String PATH = PACKAGE.replace('.', '/');

    private static final ByteCodePropertyGenerator[] GENERATORS = new ByteCodePropertyGenerator[8];

    public static ClassLoader getClassLoader(ClassLoader parent) {
        return new ByteCodeGeneratorClassLoader(parent);
    }

    private static byte[] generateClass(String className) throws InstantiationException, ClassNotFoundException, IllegalAccessException {
        if (!Schema.hasInstance(className, false)) { // pass false, to prevent deadlocks (we dont want to hold Schema.init lock), as we are already holding ClassLoader lock, and we are sure we have already initialised Schema
            throw new ClassNotFoundException(className);
        }

        Schema schema = Schema.getInstance(className, false);// pass false, to prevent deadlocks (we dont want to hold Schema.init lock), as we are already holding ClassLoader lock, and we are sure we have already initialised Schema

        if (!schema.isGenerate()) {
            throw new ClassNotFoundException(className);
        }

        String superClassName = schema.getSuperClassNameOfGeneratedBean();
        ClassGen classGenerator = new ClassGen(className, superClassName, "<generated>", Constants.ACC_PUBLIC, null);

        Set interfacesToImplement = CodeGenerator.getInterfacesToImplement(superClassName);
        final boolean lazy = interfacesToImplement.contains(org.bhavaya.beans.Lazy.class);
        final boolean indexable = interfacesToImplement.contains(org.bhavaya.beans.Indexable.class);

        if (lazy) appendLazy(classGenerator);
        if (indexable) appendIndexable(classGenerator);
        appendConstructors(classGenerator, className, superClassName);
        appendProperties(classGenerator, schema, superClassName);

        return classGenerator.getJavaClass().getBytes();
    }

    private static void appendLazy(ClassGen classGenerator) {
        classGenerator.addInterface("org.bhavaya.beans.Lazy");
        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();

        FieldGen field = new FieldGen(Constants.ACC_PRIVATE, new ObjectType("org.bhavaya.util.LoadClosure"), "load", constantPoolGen);
        classGenerator.addField(field.getField());

        field = new FieldGen(Constants.ACC_PRIVATE | Constants.ACC_STATIC , new ObjectType(classGenerator.getClassName()), "lazyNullInstance", constantPoolGen);
        classGenerator.addField(field.getField());

        createMethodLoad(classGenerator);
        createMethodSetLoad(classGenerator);
        createMethodIsLazy(classGenerator);

        InstructionFactory instructionFactory = new InstructionFactory(classGenerator, classGenerator.getConstantPool());
        createMethodIsLazyNullInstance(classGenerator, instructionFactory);
        createMethodSetLazyNullInstance(classGenerator, instructionFactory);
    }

    private static void createMethodSetLazyNullInstance(ClassGen classGenerator, InstructionFactory instructionFactory) {
        InstructionList il = new InstructionList();
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {  }, "setThisInstanceAsLazyNull", classGenerator.getClassName(), il, classGenerator.getConstantPool());

        InstructionHandle ih_0 = il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "lazyNullInstance", new ObjectType(classGenerator.getClassName()), Constants.GETSTATIC));
            BranchInstruction ifnull_3 = instructionFactory.createBranchInstruction(Constants.IFNULL, null);
        il.append(ifnull_3);
        InstructionHandle ih_6 = il.append(instructionFactory.createNew("java.lang.UnsupportedOperationException"));
        il.append(InstructionConstants.DUP);
        il.append(new PUSH(classGenerator.getConstantPool(), "Lazy null instance has already been assigned"));
        il.append(instructionFactory.createInvoke("java.lang.UnsupportedOperationException", "<init>", Type.VOID, new Type[] { Type.STRING }, Constants.INVOKESPECIAL));
        il.append(InstructionConstants.ATHROW);
        InstructionHandle ih_16 = il.append(instructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "lazyNullInstance", new ObjectType(classGenerator.getClassName()), Constants.PUTSTATIC));
        InstructionHandle ih_20 = il.append(instructionFactory.createReturn(Type.VOID));
        ifnull_3.setTarget(ih_16);
        method.setMaxStack();
        method.setMaxLocals();
        classGenerator.addMethod(method.getMethod());
        il.dispose();    
    }

    private static void createMethodIsLazyNullInstance(ClassGen classGenerator, InstructionFactory instructionFactory) {
        InstructionList il = new InstructionList();
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.BOOLEAN, Type.NO_ARGS, new String[] {  }, "isLazyNullInstance", classGenerator.getClassName(), il, classGenerator.getConstantPool());

        InstructionHandle ih_0 = il.append(instructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "lazyNullInstance", new ObjectType(classGenerator.getClassName()), Constants.GETSTATIC));
            BranchInstruction if_acmpne_4 = instructionFactory.createBranchInstruction(Constants.IF_ACMPNE, null);
        il.append(if_acmpne_4);
        il.append(new PUSH(classGenerator.getConstantPool(), 1));
            BranchInstruction goto_8 = instructionFactory.createBranchInstruction(Constants.GOTO, null);
        il.append(goto_8);
        InstructionHandle ih_11 = il.append(new PUSH(classGenerator.getConstantPool(), 0));
        InstructionHandle ih_12 = il.append(instructionFactory.createReturn(Type.INT));
        if_acmpne_4.setTarget(ih_11);
        goto_8.setTarget(ih_12);
        method.setMaxStack();
        method.setMaxLocals();
        classGenerator.addMethod(method.getMethod());
        il.dispose();
    }

    private static void createMethodLoad(ClassGen classGenerator) {
        InstructionList il = new InstructionList();
        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();
        InstructionFactory instructionFactory = new InstructionFactory(classGenerator, constantPoolGen);

        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.OBJECT, Type.NO_ARGS, new String[]{}, "load", classGenerator.getClassName(), il, constantPoolGen);

        il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "load", new ObjectType("org.bhavaya.util.LoadClosure"), Constants.GETFIELD));
        il.append(instructionFactory.createInvoke("org.bhavaya.util.LoadClosure", "load", Type.OBJECT, Type.NO_ARGS, Constants.INVOKEINTERFACE));
        il.append(InstructionFactory.createReturn(Type.OBJECT));
        method.setMaxStack();
        method.setMaxLocals();
        classGenerator.addMethod(method.getMethod());
        il.dispose();
    }

    private static void createMethodSetLoad(ClassGen classGenerator) {
        InstructionList il = new InstructionList();
        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();
        InstructionFactory instructionFactory = new InstructionFactory(classGenerator, constantPoolGen);

        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, new Type[]{new ObjectType("org.bhavaya.util.LoadClosure")}, new String[]{"arg0"}, "setLoad", classGenerator.getClassName(), il, constantPoolGen);

        il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        il.append(InstructionFactory.createLoad(Type.OBJECT, 1));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "load", new ObjectType("org.bhavaya.util.LoadClosure"), Constants.PUTFIELD));
        il.append(InstructionFactory.createReturn(Type.VOID));
        method.setMaxStack();
        method.setMaxLocals();
        classGenerator.addMethod(method.getMethod());
        il.dispose();
    }

    private static void createMethodIsLazy(ClassGen classGenerator) {
        InstructionList il = new InstructionList();
        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();
        InstructionFactory instructionFactory = new InstructionFactory(classGenerator, constantPoolGen);

        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.BOOLEAN, Type.NO_ARGS, new String[]{}, "isLazy", classGenerator.getClassName(), il, constantPoolGen);

        il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "load", new ObjectType("org.bhavaya.util.LoadClosure"), Constants.GETFIELD));
        BranchInstruction ifnull_4 = InstructionFactory.createBranchInstruction(Constants.IFNULL, null);
        il.append(ifnull_4);
        il.append(new PUSH(constantPoolGen, 1));
        BranchInstruction goto_8 = InstructionFactory.createBranchInstruction(Constants.GOTO, null);
        il.append(goto_8);
        InstructionHandle ih_11 = il.append(new PUSH(constantPoolGen, 0));
        InstructionHandle ih_12 = il.append(InstructionFactory.createReturn(Type.INT));
        ifnull_4.setTarget(ih_11);
        goto_8.setTarget(ih_12);
        method.setMaxStack();
        method.setMaxLocals();
        classGenerator.addMethod(method.getMethod());
        il.dispose();
    }

    private static void appendIndexable(ClassGen classGenerator) {
        classGenerator.addInterface("org.bhavaya.beans.Indexable");
        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();

        FieldGen field = new FieldGen(Constants.ACC_PRIVATE, new ObjectType("java.util.Map"), "indexedValues", constantPoolGen);
        classGenerator.addField(field.getField());

        createMethodAddIndexedValue(classGenerator);
        createMethodGetIndexedValue(classGenerator);
    }

    private static void createMethodAddIndexedValue(ClassGen classGenerator) {
        InstructionList il = new InstructionList();
        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();
        InstructionFactory instructionFactory = new InstructionFactory(classGenerator, constantPoolGen);

        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, new Type[]{Type.OBJECT, Type.OBJECT}, new String[]{"arg0", "arg1"}, "addIndexedValue", classGenerator.getClassName(), il, constantPoolGen);

        il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "indexedValues", new ObjectType("java.util.Map"), Constants.GETFIELD));
        BranchInstruction ifnonnull_4 = InstructionFactory.createBranchInstruction(Constants.IFNONNULL, null);
        il.append(ifnonnull_4);
        il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createNew("java.util.HashMap"));
        il.append(InstructionConstants.DUP);
        il.append(instructionFactory.createInvoke("java.util.HashMap", "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "indexedValues", new ObjectType("java.util.Map"), Constants.PUTFIELD));
        InstructionHandle ih_18 = il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "indexedValues", new ObjectType("java.util.Map"), Constants.GETFIELD));
        il.append(InstructionFactory.createLoad(Type.OBJECT, 1));
        il.append(InstructionFactory.createLoad(Type.OBJECT, 2));
        il.append(instructionFactory.createInvoke("java.util.Map", "put", Type.OBJECT, new Type[]{Type.OBJECT, Type.OBJECT}, Constants.INVOKEINTERFACE));
        il.append(InstructionConstants.POP);
        il.append(InstructionFactory.createReturn(Type.VOID));
        ifnonnull_4.setTarget(ih_18);
        method.setMaxStack();
        method.setMaxLocals();
        classGenerator.addMethod(method.getMethod());
        il.dispose();
    }

    private static void createMethodGetIndexedValue(ClassGen classGenerator) {
        InstructionList il = new InstructionList();
        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();
        InstructionFactory instructionFactory = new InstructionFactory(classGenerator, constantPoolGen);

        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.OBJECT, new Type[]{Type.OBJECT}, new String[]{"arg0"}, "getIndexedValue", classGenerator.getClassName(), il, constantPoolGen);

        il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "indexedValues", new ObjectType("java.util.Map"), Constants.GETFIELD));
        BranchInstruction ifnonnull_4 = InstructionFactory.createBranchInstruction(Constants.IFNONNULL, null);
        il.append(ifnonnull_4);
        il.append(InstructionConstants.ACONST_NULL);
        il.append(InstructionFactory.createReturn(Type.OBJECT));
        InstructionHandle ih_9 = il.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        il.append(instructionFactory.createFieldAccess(classGenerator.getClassName(), "indexedValues", new ObjectType("java.util.Map"), Constants.GETFIELD));
        il.append(InstructionFactory.createLoad(Type.OBJECT, 1));
        il.append(instructionFactory.createInvoke("java.util.Map", "get", Type.OBJECT, new Type[]{Type.OBJECT}, Constants.INVOKEINTERFACE));
        il.append(InstructionFactory.createReturn(Type.OBJECT));
        ifnonnull_4.setTarget(ih_9);
        method.setMaxStack();
        method.setMaxLocals();
        classGenerator.addMethod(method.getMethod());
        il.dispose();
    }

    private static void appendConstructors(ClassGen classGenerator, String className, String superClassName) {
        classGenerator.addEmptyConstructor(Constants.ACC_PUBLIC);

        // other constructors
        Class superClass = ClassUtilities.getClass(superClassName, false, false);
        if (superClass != null) {
            Constructor[] constructors = superClass.getConstructors();
            for (int i = 0; i < constructors.length; i++) {
                Constructor constructor = constructors[i];
                Class[] constructorParameters = constructor.getParameterTypes();
                if (constructorParameters.length > 0) {
                    appendConstructor(classGenerator, className, superClassName, constructorParameters);
                }
            }
        }
    }

    private static void appendConstructor(ClassGen classGenerator, String className, String superClassName, Class[] parameterTypes) {
        Type[] bcelParameterTypes = getTypes(parameterTypes);
        String[] parameterNames = getNames(parameterTypes);

        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();
        InstructionFactory instructionFactory = new InstructionFactory(classGenerator, constantPoolGen);
        InstructionList il = new InstructionList();

        il.append(InstructionConstants.THIS);

        for (int i = 0; i < bcelParameterTypes.length; i++) {
            il.append(InstructionFactory.createLoad(bcelParameterTypes[i], i + 1));
        }
        il.append(instructionFactory.createInvoke(superClassName, "<init>", Type.VOID, bcelParameterTypes, Constants.INVOKESPECIAL));
        il.append(InstructionConstants.RETURN);

        MethodGen methodGen = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, bcelParameterTypes, parameterNames, "<init>", className, il, constantPoolGen);
        methodGen.setMaxStack();
        methodGen.setMaxLocals();
        classGenerator.addMethod(methodGen.getMethod());
    }

    private static void appendProperties(ClassGen classGenerator, Schema schema, String superClassName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Property[] properties = schema.getProperties();
        for (int i = 0; i < properties.length; i++) {
            Property property = properties[i];
            appendProperty(classGenerator, property, superClassName);
        }
    }

    private static void appendProperty(ClassGen classGenerator, Property property, String superClassName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String propertyName = property.getName();
        String propertyType = property.getTypeName();

        if (CodeGenerator.hasGetter(propertyName, propertyType, superClassName, property.getCardinality())) {
            if (log.isDebugEnabled())log.debug("Getter for " + propertyName + " already exists in: " + superClassName + " not adding property to: " + classGenerator.getClassName());
            return;
        }

        boolean many = !property.getCardinality().equals(Property.ONE);
        boolean lazy = property.isLazy();
        boolean callSuperSetter = CodeGenerator.hasSetter(propertyName, propertyType, superClassName, property.getCardinality());

        if (callSuperSetter) {
            if (log.isDebugEnabled())log.debug("Setter for " + propertyName + " already exists in: " + superClassName + " adding call to super.set in: " + classGenerator.getClassName());
        }

        Type fieldType = getType(propertyType);
        String fieldName = propertyName;
        if (many) {
            fieldType = getType("org.bhavaya.collection.BeanCollection");
            fieldName = fieldName + "Collection";
        }
        ConstantPoolGen constantPoolGen = classGenerator.getConstantPool();
        FieldGen fieldGen = new FieldGen(Constants.ACC_PRIVATE, fieldType, fieldName, constantPoolGen);
        classGenerator.addField(fieldGen.getField());

        String foreignIndex = null;
        if (property instanceof ForeignKeyProperty) {
            ForeignKeyProperty foreignKeyProperty = (ForeignKeyProperty) property;
            foreignIndex = foreignKeyProperty.getForeignIndex();
        }

        int flags = flagsForProperty(many, lazy, callSuperSetter);
        getGeneratorForProperty(flags).add(classGenerator, getType(propertyType), propertyType, propertyName, foreignIndex);
    }

    private static ByteCodePropertyGenerator getGeneratorForProperty(int i) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (GENERATORS[i] == null) {
            Class c = Class.forName(PACKAGE + "CreateProperty" + i);
            GENERATORS[i] = (ByteCodePropertyGenerator) c.newInstance();
        }
        return GENERATORS[i];
    }

    private static Type[] getTypes(Class[] propertyTypes) {
        Type[] result = new Type[propertyTypes.length];
        for (int i = 0; i < propertyTypes.length; i++) {
            String propertyType = propertyTypes[i].getName();
            result[i] = getType(propertyType);
        }
        return result;
    }

    private static String[] getNames(Class[] propertyTypes) {
        String[] result = new String[propertyTypes.length];
        for (int i = 0; i < propertyTypes.length; i++) {
            result[i] = "arg" + i;
        }
        return result;
    }

    private static Type getType(String propertyType) {
        if (propertyType.equals("java.lang.String")) {
            return Type.STRING;
        } else if (propertyType.equals("int")) {
            return Type.INT;
        } else if (propertyType.equals("float")) {
            return Type.FLOAT;
        } else if (propertyType.equals("double")) {
            return Type.DOUBLE;
        } else if (propertyType.equals("long")) {
            return Type.LONG;
        } else if (propertyType.equals("short")) {
            return Type.SHORT;
        } else if (propertyType.equals("boolean")) {
            return Type.BOOLEAN;
        } else if (propertyType.equals("byte")) {
            return Type.BYTE;
        } else if (propertyType.equals("char")) {
            return Type.CHAR;
        } else {
            return new ObjectType(propertyType);
        }
    }

    private static String getNameForProperty(int i) {
        return "Property" + i;
    }

    public static int flagsForProperty(boolean collection, boolean lazy, boolean callSuper) {
        return (collection ? COLLECTION : 0) + (lazy ? LAZY : 0) + (callSuper ? CALL_SUPER_SETTER : 0);
    }

    private static void generateJavaForProperty(int flags) throws Exception {
        String className = getNameForProperty(flags);
        boolean cardinalityOne = (flags & COLLECTION) == 0;
        boolean lazy = (flags & LAZY) != 0;
        boolean callSuperSetter = (flags & CALL_SUPER_SETTER) != 0;
        String propertyType = (flags < 2) ? "long" : PROPERTY_TYPE.class.getName();
        String s = SourceCodeGenerator.generatePropertySourceCode(PACKAGE + className, PACKAGE + "SUPER_CLASS", cardinalityOne, lazy, callSuperSetter, propertyType, "PROPERTY_NAME", "INDEX_NAME");
        String filename = PATH + className + ".java";
        write(filename, s);
    }

    private static void generateBcelSourceForProperty(int flags) throws Exception {
        String className = getNameForProperty(flags);
        String filename = PATH + "Create" + className + ".java";
        String bcelSource = generateBcelSource(className);
        bcelSource = filterPropertyTokens(bcelSource);
        write(filename, bcelSource);
    }

    private static String filterPropertyTokens(String s) {
        s = s.replaceAll("\"setPROPERTY_NAME\"", "\"set\" + Utilities.capitalise(propertyName)");
        s = s.replaceAll("\"getPROPERTY_NAME\"", "\\(propertyType == Type.BOOLEAN \\? \"is\" \\: \"get\"\\) + Utilities.capitalise(propertyName)");
        s = s.replaceAll("\"PROPERTY_NAME\"", "propertyName");

        s = s.replaceAll("\"setPROPERTY_NAMECollection\"", "\"set\" + Utilities.capitalise(propertyName) + \"Collection\"");
        s = s.replaceAll("\"getPROPERTY_NAMECollection\"", "\"get\" + Utilities.capitalise(propertyName) + \"Collection\"");
        s = s.replaceAll("\"PROPERTY_NAMECollection\"", "propertyName + \"Collection\"");

        s = s.replaceAll("Type\\.LONG", "propertyType");
        s = s.replaceAll("\"" + ESCAPED_PACKAGE + "Property.\"", "_className");
        s = s.replaceAll("\"" + ESCAPED_PACKAGE + "SUPER_CLASS\"", "_superclassName");
        s = s.replaceAll("\"" + ESCAPED_PACKAGE + "PROPERTY_TYPE\"", "propertyTypeName");
        s = s.replaceAll("\"firePropertyChange\"\\, Type\\.VOID\\, new Type\\[\\] \\{ Type\\.STRING\\, propertyType\\, propertyType \\}", "\"firePropertyChange\"\\, Type\\.VOID\\, new Type\\[\\] \\{ Type\\.STRING\\, _propertyTypeForFireMethod\\, _propertyTypeForFireMethod \\}");

        s = s.replaceAll("\"INDEX_NAME\"", "indexName");
        return s;
    }

    private static String generateBcelSource(String className) throws IOException {
        Class c = null;
        try {
            c = Class.forName(PACKAGE + className);
        } catch (Exception e) {
            log.error(e);
        }
        JavaClass jc = Repository.lookupClass(c);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BCELifier gen = new BCELifier(jc, os);
        gen.start();
        os.close();
        return os.toString();
    }


    private static void write(String filename, String s) throws IOException {
        write(filename, s.getBytes());
    }

    private static void write(String filename, byte[] b) throws IOException {
        File file = new File(filename);
        log.info("Writing: " + file.getAbsoluteFile());
        file.getParentFile().mkdirs();
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        os.write(b);
        os.close();
    }

    private static class ByteCodeGeneratorClassLoader extends ClassLoader {
        public ByteCodeGeneratorClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class loadClass(String name) throws ClassNotFoundException {
            return loadClassInternal(name);
        }

        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return loadClassInternal(name);
        }

        private Class loadClassInternal(String name) throws ClassNotFoundException {
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
                Class c = findLoadedClass(name);
                if (c != null) return c;

                // Have to initialise Schema to see if this is one of the classes we should be generating from Schema.
                // Schema.init() must not do any classloading, otherwise we are prone to deadlocks during multithreaded usage.
                Schema.init();
                c = findClass(name);
                return c;
            }
        }

        public Class findClass(String name) throws ClassNotFoundException {
            if (log.isDebug()) log.debug("findClass: " + name);
            byte[] b = loadClassData(name);
            return defineClass(name, b, 0, b.length);
        }

        private byte[] loadClassData(String className) throws ClassNotFoundException {
            try {
                byte[] bytes = generateClass(className);

                if (DEBUG) {
                    String filename = Log.getLogDirectory() + "/generatedClasses" + "/" + className.replace('.', '/') + ".class";
                    write(filename, bytes);
                    log.info("Generated: " + className);
                }

                return bytes;
            } catch (Exception e) {
                throw new ClassNotFoundException(className, e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String target = args.length == 0 ? "" : args[0];
        if (target.equals("source")) {
            new File(PATH).mkdirs();
            for (int i = 0; i < 8; i++) {
                generateJavaForProperty(i);
            }
        } else if (target.equals("generators")) {
            for (int i = 0; i < 8; i++) {
                generateBcelSourceForProperty(i);
            }
        }
    }
}
