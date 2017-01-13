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

package org.bhavaya.util;

import org.bhavaya.beans.generator.ByteCodeGenerator;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.10 $
 */
public class ClassUtilities {

    private static final Log log = Log.getCategory(ClassUtilities.class);
    private static boolean generateClassesDynamically = true;
    private static ClassLoader applicationClassLoader;
    private static ClassLoader parentClassLoader = ClassUtilities.class.getClassLoader();
    private static final Map fieldsPerClass = new HashMap();
    private static boolean firstCallToGetJarFileOrClasspathDirCompleted;

    public static final Class getClass(String className) {
        return getClass(className, true, true);
    }

    public static final Class getClass(String className, boolean failIfClassNotFound, boolean initialize) {
        if (className == null) return null;
        if (className.equals("boolean")) return boolean.class;
        if (className.equals("byte")) return byte.class;
        if (className.equals("char")) return char.class;
        if (className.equals("short")) return short.class;
        if (className.equals("int")) return int.class;
        if (className.equals("long")) return long.class;
        if (className.equals("float")) return float.class;
        if (className.equals("double")) return double.class;
        if (className.equals("void")) return void.class;
        try {
            return Class.forName(className, initialize, getApplicationClassLoader());
        } catch (Throwable e) {
            if (failIfClassNotFound) {
                throw new RuntimeException("Cannot create class: " + className, e);
            } else {
                return null;
            }
        }
    }

    public static final Class classToType(Class aClass) {
        if (aClass == Boolean.class) return boolean.class;
        if (aClass == Byte.class) return byte.class;
        if (aClass == Character.class) return char.class;
        if (aClass == Short.class) return short.class;
        if (aClass == Integer.class) return int.class;
        if (aClass == Long.class) return long.class;
        if (aClass == Float.class) return float.class;
        if (aClass == Double.class) return double.class;
        if (aClass == Void.class) return void.class;
        return aClass;
    }

    public static final Class typeToClass(Class type) {
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == void.class) return Void.class;
        return type;
    }

    public static final boolean isPrimitiveTypeOrClass(Class type) {
        if (type.isPrimitive()) {
            return true;
        } else if (type == Boolean.class ||
                type == Byte.class ||
                type == Character.class ||
                type == Short.class ||
                type == Integer.class ||
                type == Long.class ||
                type == Float.class ||
                type == Double.class ||
                type == Void.class) {
            return true;
        } else {
            return false;
        }
    }

    public static final String getDisplayName(Class clazz) {
        return Utilities.getDisplayName(getUnqualifiedClassName(clazz));
    }

    public static final String getUnqualifiedClassName(Class clazz) {
        return getUnqualifiedClassName(clazz.getName());
    }

    public static final String getUnqualifiedClassName(String fullyQualifiedClassName) {
        return Utilities.getUnqualifiedName(fullyQualifiedClassName, '.');
    }

    public static final String getPackageName(Class clazz) {
        return Utilities.getQualifier(clazz.getName(), '.');
    }

    public static final String getPackageName(String fullyQualifiedClassName) {
        return Utilities.getQualifier(fullyQualifiedClassName, '.');
    }

    public static String classToFilename(Class clazz) {
        return classnameToFilename(clazz.getName());
    }

    public static String classnameToFilename(String classname) {
        if (classname == null) throw new IllegalArgumentException("Class name is null");

        if (!classname.startsWith("/")) {
            classname = "/" + classname;
        }
        classname = classname.replace('.', '/'); // do not use File.separator, this does not work with jars or zips
        classname = classname + ".class";
        return classname;
    }

    public static String filenameToClassname(String filename) {
        if (filename == null) throw new IllegalArgumentException("File name is null");
        filename = filename.replace('/', '.');
        filename = filename.replace('\\', '.');
        if (filename.startsWith(".")) filename = filename.substring(1, filename.length());
        filename = filename.substring(0, filename.length() - 6); // remove ".class"
        return filename;
    }

    public static File getPathForClass(String classname) {
        if (classname == null) return null;
        Class clazz = getClass(classname, false, false);
        return getJarFileOrClasspathDir(clazz);
    }

    /**
     * @deprecated use getJarFileOrClasspathDir
     */
    public static File getPathForClass(Class clazz) {
        return getJarFileOrClasspathDir(clazz);
    }

    /**
     * @return the jar file or classpath directory from which this class was loaded
     * (or null if it is a generated class file (bcel)?
     */
    public static File getJarFileOrClasspathDir(Class clazz) {
        if (clazz == null) return null;
        String classname = classnameToFilename(clazz.getName());
        logFirstTime("Classname: " + classname);

        URL url = clazz.getResource(classname);
        logFirstTime("URL " + url);

        File result = null;
        if ( url != null ) {
            if ( url.getProtocol().equals("file")) {
                result = getClasspathDir(classname, url);
            } else  {
                result = getJar(url);
            }
            logFirstTime("jar or classpath dir for resource " + clazz + " is " + result);
        }
        
        //after this method is called the first time some logging will turn off
        firstCallToGetJarFileOrClasspathDirCompleted = true;

        return result;
    }

    private static File getClasspathDir(String className, URL url) {
        String path = url.getFile();
        logFirstTime("full path to file" + path);
        path = removeClassNameFromPath(className, path);
        logFirstTime("path to classpath dir " + path);
        return new File(path);
    }

    private static String removeClassNameFromPath(String className, String path) {
        logFirstTime("getClasspathDirForUrl classname " + className);
        path = path.substring(0, path.length() - className.length());
        return path;
    }

    private static File getJar(URL jarUrl) {
        //actual version we are running with
        String jdkVersion = System.getProperty("java.version");
        logFirstTime("java.version: " + jdkVersion);

        //java version of browser plugin
        String deployVersion = System.getProperty("deployment.version", "");
        logFirstTime("deployment.version: " + deployVersion);

        //version of webstart.jar being used
        String webstartVersion = System.getProperty("javawebstart.version");
        logFirstTime("javawebstart.version " + webstartVersion);

        String name = getJarFilePath(jarUrl);
        return new File(name);
    }

     /**
     * This method will return the path to the jar file which this URL references
     * It should work with URLs returned by class.getResource() under java 1.5.0_16
     * and 1.6.0_07, as well as maintaining backwards compatibility with previous jres
     *
     * The two jre above contain security patches which make the file path of the jar
     * inaccessible under webstart. This patch works around that by using reflection to access
     * private fields in the webstart.jar where required. This will only work for signed webstart
     * apps running with all security permissions
     *
     * @param jarUrl - url which has jar as the protocol
     * @return path to Jar file for this jarURL
     */
    private static String getJarFilePath(URL jarUrl) {
        JarFile jarFile = getJarFile(jarUrl);
        return findJarPath(jarFile);
    }

    public static JarFile getJarFile(URL jarUrl) {
        try {
            JarURLConnection jarUrlConnection = (JarURLConnection)jarUrl.openConnection();
            logFirstTime("jarUrlConnection class " + jarUrlConnection.getClass());
            logFirstTime("jarUrlConnection instance " + jarUrlConnection);

            //try the getJarFile method first.
            //Under webstart in 1.5.0_16 this is overriden to return null
            JarFile jarFile = jarUrlConnection.getJarFile();
            logFirstTime("cachedJarFile by getJarFileMethod " + jarFile);

            if ( jarFile == null) {
                jarFile = getJarFileByReflection(jarUrlConnection);
            }

            logFirstTime("cachedJarFile " + jarFile);
            return jarFile;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get JarFile from jarUrlConnection", t);
        }
    }

    private static JarFile getJarFileByReflection(JarURLConnection jarUrlConnection) throws Exception {
        //this class only exists in webstart.jar for 1.5.0_16 and later
        Class jnlpConnectionClass = Class.forName("com.sun.jnlp.JNLPCachedJarURLConnection");
        Field jarFileField;
        try {
            logFirstTime("using reflection to read jarFile field");
            jarFileField = jnlpConnectionClass.getDeclaredField("jarFile");
        } catch ( Throwable t) {
            logFirstTime("using reflection to read  _jarFile field");
            jarFileField = jnlpConnectionClass.getDeclaredField("_jarFile");
        }
        jarUrlConnection.connect(); //this causes the connection to set the jarFile field
        logFirstTime("tryJarURLConnection jarFileField " + jarFileField);
        jarFileField.setAccessible(true);
        return (JarFile)jarFileField.get(jarUrlConnection);
    }

    private static String findJarPath(JarFile cachedJarFile) {
        try {
            String name = cachedJarFile.getName();
            logFirstTime("findJarName getName method returns " + name);

            //getName is overridden to return "" under 1.6.0_7 so use reflection
            if ( name == null || name.trim().equals("")) {
                Class c = ZipFile.class;
                Field field = c.getDeclaredField("name");
                field.setAccessible(true);
                name = (String)field.get(cachedJarFile);
            }
            logFirstTime("JarFile name " + name);
            return name;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get find name from jarFile", t);
        }
    }

    private static void logFirstTime(String text) {
        if (! firstCallToGetJarFileOrClasspathDirCompleted) {
            log.info(text);
        }
    }

    public static URL getURLForClass(String classname) {
        Class clazz = getClass(classname, false, false);
        return getURLForClass(clazz);
    }

    public static URL getURLForClass(Class clazz) {
        if (clazz == null) return null;
        String classname = classnameToFilename(clazz.getName());
        return clazz.getResource(classname);
    }

    private static Map getFieldsForClass(Class clazz) {
        synchronized(fieldsPerClass) {
            Map fieldsForClass = (Map) fieldsPerClass.get(clazz);
            Class startingClass = clazz;

            if (fieldsForClass == null) {
                fieldsForClass = new HashMap();
                while (clazz != null && !clazz.equals(Object.class)) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (int i = 0; i < fields.length; i++) {
                        Field field = fields[i];
                        String fieldName = field.getName();
                        if (!fieldsForClass.containsKey(fieldName)) {
                            field.setAccessible(true);
                            fieldsForClass.put(fieldName, field);
                        }
                    }

                    clazz = clazz.getSuperclass();
                }
                fieldsPerClass.put(startingClass, fieldsForClass);
            }

            return fieldsForClass;
        }
    }

    public static Field[] getFields(Class clazz) {
        final Collection fieldsCollection = getFieldsForClass(clazz).values();
        return (Field[]) fieldsCollection.toArray(new Field[fieldsCollection.size()]);
    }

    public static Field getField(Class clazz, String propertyName) {
        Field field = (Field) getFieldsForClass(clazz).get(propertyName);
        if (field == null) throw new RuntimeException("Cannot find field: " + propertyName + " on class " + clazz.getName());
        return field;
    }

    public static void setGenerateClassesDynamically(boolean generateClassesDynamically) {
        assert generateClassesDynamically == ClassUtilities.generateClassesDynamically || applicationClassLoader == null;
        ClassUtilities.generateClassesDynamically = generateClassesDynamically;
    }

    public static ClassLoader getParentClassLoader() {
        return parentClassLoader;
    }

    public static void setParentClassLoader(ClassLoader parentClassLoader) {
        ClassUtilities.parentClassLoader = parentClassLoader;
    }

    public static ClassLoader getApplicationClassLoader() {
        if (applicationClassLoader == null) {
            ClassLoader parentClassLoader = getParentClassLoader();

            if (generateClassesDynamically) {
                applicationClassLoader = ByteCodeGenerator.getClassLoader(parentClassLoader);
                Thread.currentThread().setContextClassLoader(applicationClassLoader);
            } else {
                applicationClassLoader = parentClassLoader;
            }
        }
        return applicationClassLoader;
    }

    public static Method getMethod(Class aClass, String methodName, Class[] parameterTypesToCheck, Class stopClass) {
        while (aClass != null && aClass != stopClass) {
            Method[] methods = aClass.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (method.getName().equals(methodName) && !Modifier.isAbstract(method.getModifiers())) {
                    Class[] parameterTypes = method.getParameterTypes();

                    if (parameterTypes.length == 0 && parameterTypesToCheck == null) {
                        return method;
                    }

                    if (parameterTypesToCheck != null && parameterTypes.length == parameterTypesToCheck.length) {
                        boolean allParametersOfSameType = true;
                        for (int j = 0; j < parameterTypesToCheck.length && allParametersOfSameType; j++) {
                            Class parameterTypeToCheck = parameterTypesToCheck[j];
                            if (!parameterTypes[j].equals(parameterTypeToCheck)) allParametersOfSameType = false;
                        }
                        if (allParametersOfSameType) return method;
                    }
                }
            }
            aClass = aClass.getSuperclass();
        }
        return null;
    }


    public static boolean implementsClassOrInterface(Class classToCheck, Class classOrInterfaceToImplement) {
        Method[] methods = classOrInterfaceToImplement.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            Method foundMethod = getMethod(classToCheck, method.getName(), method.getParameterTypes(), null);
            if (foundMethod == null || Modifier.isAbstract(foundMethod.getModifiers())) {
                return false;
            }
        }
        return true;
    }

}