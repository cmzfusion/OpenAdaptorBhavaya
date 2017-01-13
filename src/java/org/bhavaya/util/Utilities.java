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

import org.bhavaya.collection.BeanCollection;
import org.bhavaya.collection.EfficientArrayList;
import org.bhavaya.ui.table.formula.FormulaUtils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions.
 *
 * @author Philip Milne
 * @version $Revision: 1.36.4.1 $
 */
public final class Utilities {
    private static String localhost;
    private static long idCounter = 0;
    private static Timer utilTimer;
    private static Random random = new Random();

    public static final String MBEANSERVER_DOMAIN = "org.bhavaya";

    private static final Pattern argumentPattern = Pattern.compile("(%([\\w.]*)%)");
    private static final Pattern escapedMarkerPattern = Pattern.compile("\\\\%");
    private static final Pattern dollarPattern = Pattern.compile("\\$");

    private static final String URL_MATCHER_PATTERN = "((www\\.|(http|https|ftp|news|file)+\\:\\/\\/)[_.a-zA-Z0-9-]+\\.[a-zA-Z0-9\\/_:;@=.+?,##%&~-]*[^.|\\'|\\# |!|\\(|?|,| |>|<|;|\\)])";

    private static final Comparator COMPARABLE_COMPARATOR = new ComparableComparator();
    private static final Comparator TO_STRING_COMPARATOR = ToStringComparator.CASE_SENSITIVE_COMPARATOR;
    public static final Comparator COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return Utilities.compare(o1, o2);
        }
    };
    private static final java.util.regex.Pattern spaceSeperatarPattern = java.util.regex.Pattern.compile("\\s+");

    public static final Double DOUBLE_NAN = Double.NaN;
    public static final Float FLOAT_NAN = Float.NaN;
    public static final Double DOUBLE_ZERO =  0d;
    public static final Long LONG_ZERO = 0l;
    public static final Integer INTEGER_ZERO = 0;
    public static final Short SHORT_ZERO = 0;
    public static final Byte BYTE_ZERO = 0;

    public static synchronized Timer getApplicationTimer() {
        if (utilTimer == null) {
            utilTimer = new Timer(true);
        }
        return utilTimer;
    }


    public static boolean equals(Object o1, Object o2) {
        return (o1 == null) ? (o2 == null) : o1.equals(o2);
    }

    public static boolean equalsIgnoreCase(String s1, String s2) {
        return (s1 == null) ? (s2 == null) : s1.equalsIgnoreCase(s2);
    }

    public static int compare(Object o1, Object o2) {
        if (o1 == o2) return 0;

        int comparison;
        if (o1 != null) {
            Comparator comparator = getComparator(o1);
            comparison = comparator.compare(o1, o2);
        } else {
            Comparator comparator = getComparator(o2);
            comparison = comparator.compare(o1, o2);
        }
        return comparison;
    }

    public static Comparator getComparator(Object o) {
        if (o == null) return TO_STRING_COMPARATOR;
        return getComparator(o.getClass());
    }

    public static Comparator getComparator(Class clazz) {
        return Comparable.class.isAssignableFrom(clazz) ? COMPARABLE_COMPARATOR : TO_STRING_COMPARATOR;
    }

    public static String getPluralName(String name) {
        if (name == null) return null;
        if (name.endsWith("y")) {
            return (name.substring(0, name.length() - 1) + "ies");
        } else if (name.endsWith("a") || name.endsWith("us")) {
            return name;
        } else if (name.endsWith("s")) {
            return name + "es";
        } else {
            return name + "s";
        }
    }

    /**
     * Now modified to escape all characters above the 7bit ASCII range.  Should keep our German friends happy.
     */
    public static String escapeHtmlCharacters(String string) {
        StringBuffer stringBuffer = new StringBuffer(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (((int) c) < 128) {
                stringBuffer.append(c);
            } else {
                stringBuffer.append("&#" + Integer.toString((int) c) + ";");
            }
        }
        return stringBuffer.toString();
    }

    public static String wrapWithSplitOnNewLine(String string, int lengthPerLine) {
        if (string == null) return null;
        String[] lines = string.split("\n");

        int length = string.length();
        StringBuffer stringBuffer = new StringBuffer(length + 5);

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) stringBuffer.append("\n");
            String line = lines[i];
            stringBuffer.append(wrap(line, lengthPerLine));

        }
        return stringBuffer.toString();
    }

    public static String wrap(String string, int lengthPerLine) {
        if (string == null) return null;
        int length = string.length();
        if (length <= lengthPerLine) return string;

        int numberOfFullComponents = length / lengthPerLine;
        int spillOverLength = length % lengthPerLine;
        int totalNumberOfComponents = numberOfFullComponents + (spillOverLength > 0 ? 1 : 0);

        StringBuffer stringBuffer = new StringBuffer(length + 5);

        for (int i = 0; i < numberOfFullComponents; i++) {
            int beginIndex = i * lengthPerLine;
            if (i > 0) stringBuffer.append('\n');
            stringBuffer.append(string.substring(beginIndex, beginIndex + lengthPerLine));
        }

        if (spillOverLength > 0) {
            int i = totalNumberOfComponents - 1;
            int beginIndex = i * lengthPerLine;
            stringBuffer.append('\n');
            stringBuffer.append(string.substring(beginIndex, beginIndex + spillOverLength));
        }

        return stringBuffer.toString();
    }

    public static String truncate(String string, int length) {
        if (string == null) return null;
        if (string.length() <= length) {
            return string;
        } else {
            return string.substring(0, length);
        }
    }

    public static String pad(String string, int length, char padChar) {
        if (string != null && string.length() == length) {
            return string;
        } else if (string != null && string.length() > length) {
            return string.substring(0, length);
        } else {
            StringBuffer buffer = new StringBuffer(length);
            if (string != null) buffer.append(string);

            for (int i = buffer.length(); i < length; i++) {
                buffer.append(padChar);
            }
            return buffer.toString();
        }
    }

    /**
     * Change String from property name format to display name format
     * e.g. "transactionTypeId" is converted to "Transaction LookupValue Id"
     *
     * @return String display name
     */
    public static String getDisplayName(String propertyName) {
        if (propertyName == null) {
            return null;
        }

        int stringLength = propertyName.length();

        // Empty string will break routines below to capitalise it.
        if (stringLength == 0) {
            return propertyName;
        }

        StringBuffer displayName = new StringBuffer(stringLength);

        // capitalise first character
        displayName.append(Character.toUpperCase(propertyName.charAt(0)));

        // process remaining characters
        for (int i = 1; i < stringLength; i++) {
            char c = propertyName.charAt(i);
            boolean thisCharIsUpper = Character.isUpperCase(c);
            boolean atEnd = (i == (stringLength - 1));
            if (!atEnd) {
                boolean previousCharIsLower = i > 0 && Character.isLowerCase(propertyName.charAt(i - 1));
                boolean nextCharIsLower = Character.isLowerCase(propertyName.charAt(i + 1));
                if (thisCharIsUpper && (previousCharIsLower || nextCharIsLower)) {
                    displayName.append(' ');
                }
            }
            displayName.append(c);
        }

        return displayName.toString();
    }

    public static String getDisplayNameForPropertyPath(String path) {
        if(FormulaUtils.isFormulaPath(path)) {
            return FormulaUtils.getDisplayNameForPropertyPath(path);
        }

        String[] beanPath = Generic.beanPathStringToArray(path);
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<beanPath.length; i++) {
            if(i>0) {
                sb.append(" - ");
            }
            sb.append(getDisplayName(beanPath[i]));
        }
        return sb.toString();
    }

    public static String getPropertyName(String displayName) {
        if(displayName == null) {
            return null;
        }
        String[] words = displayName.split("\\s");
        StringBuilder sb = new StringBuilder();
        sb.append(words[0].toLowerCase());
        for(int i=1; i<words.length; i++) {
            sb.append(capitalise(words[i]));
        }
        return sb.toString();
    }

    public static String getPropertyPathForDisplayName(String displayName) {
        if(displayName == null) {
            return null;
        }
        String[] groups = displayName.split("\\s*-\\s*");
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<groups.length; i++) {
            if(i>0) {
                sb.append(".");
            }
            sb.append(getPropertyName(groups[i]));
        }
        return sb.toString();
    }


    public static String acronym(String string) {
        String[] words = spaceSeperatarPattern.split(string);
        StringBuffer buf = new StringBuffer(words.length);
        for (String word : words) {
            buf.append(word.charAt(0));
        }
        return buf.toString();
    }

    public static String capitalise(String string) {
        if (string == null) {
            return null;
        }

        int stringLength = string.length();

        // Empty string will break routines below to capitalise it.
        if (stringLength == 0) {
            return string;
        }

        StringBuffer capitalisedString = new StringBuffer(stringLength);
        capitalisedString.append(Character.toUpperCase(string.charAt(0)));
        for (int i = 1; i < stringLength; i++) {
            capitalisedString.append(string.charAt(i));
        }

        return capitalisedString.toString();
    }

    public static String decapitalise(String string) {
        if (string == null) {
            return null;
        }

        int stringLength = string.length();

        // Empty string will break routines below to capitalise it.
        if (stringLength == 0) {
            return string;
        }

        StringBuffer decapitalisedString = new StringBuffer(stringLength);
        decapitalisedString.append(Character.toLowerCase(string.charAt(0)));
        for (int i = 1; i < stringLength; i++) {
            decapitalisedString.append(string.charAt(i));
        }

        return decapitalisedString.toString();
    }

    public static boolean isUpperCase(String string) {
        if (string == null || string.length() == 0) return true;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (Character.isLetter(c) && Character.isLowerCase(c)) {
                return false;
            }
        }

        return true;
    }

    public static boolean contains(String[] strings, String stringToFind, boolean ignoreCase) {
        for (String string : strings) {
            if (ignoreCase && string.equalsIgnoreCase(stringToFind) || !ignoreCase && string.equals(stringToFind)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(Object[] objects, Object objectToFind) {
        assert objectToFind == null ||
                objects.getClass().getComponentType().isAssignableFrom(objectToFind.getClass()) ||
                objectToFind.getClass().getComponentType().isAssignableFrom(objects.getClass().getComponentType())
                : "Invalid class";

        for (Object object : objects) {
            if (object.equals(objectToFind)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(char[] chars, char charToFind) {
        for (char character : chars) {
            if (character == charToFind) {
                return true;
            }
        }
        return false;
    }

    public static int[] asPrimitiveArray(Integer[] classArray) {
        if (classArray == null) return null;
        int length = classArray.length;
        int[] primitiveArray = new int[length];
        for (int i = 0; i < length; i++) {
            primitiveArray[i] = classArray[i];

        }
        return primitiveArray;
    }

    public static long[] asPrimitiveArray(Long[] classArray) {
        if (classArray == null) return null;
        int length = classArray.length;
        long[] primitiveArray = new long[length];
        for (int i = 0; i < length; i++) {
            primitiveArray[i] = classArray[i];

        }
        return primitiveArray;
    }

    public static Object[][] splitArray(Object[] array, int maxArrayLength) {
        int length = array.length;
        int numberOfFullArrays = length / maxArrayLength;
        int spillOverArrayLength = length % maxArrayLength;
        int totalNumberOfArrays = numberOfFullArrays + (spillOverArrayLength > 0 ? 1 : 0);

        Object[][] splitArrays = (Object[][]) Array.newInstance(array.getClass().getComponentType(), new int[]{totalNumberOfArrays, 0});

        for (int i = 0; i < numberOfFullArrays; i++) {
            Object[] splitArray = (Object[]) Array.newInstance(array.getClass().getComponentType(), maxArrayLength);
            System.arraycopy(array, i * maxArrayLength, splitArray, 0, maxArrayLength);
            splitArrays[i] = splitArray;
        }

        if (spillOverArrayLength > 0) {
            int i = totalNumberOfArrays - 1;
            Object[] splitArray = (Object[]) Array.newInstance(array.getClass().getComponentType(), spillOverArrayLength);
            System.arraycopy(array, i * maxArrayLength, splitArray, 0, spillOverArrayLength);
            splitArrays[i] = splitArray;
        }

        return splitArrays;
    }

    public static <T> T[] unionArrays(T[] array1, T[] array2) {
        if (array1 == null && array2 == null) return null;
        if (array1 == null) return array2;
        if (array2 == null) return array1;

        assert array1.getClass().getComponentType().isAssignableFrom(array2.getClass().getComponentType()) ||
                array2.getClass().getComponentType().isAssignableFrom(array1.getClass().getComponentType())
                : "Invalid class";

        int array1Length = array1.length;
        int array2Length = array2.length;

        // use LinkedHashSet to maintain order
        // set the initial capacity of union to the maximum it is likely to be
        Set<T> union = new LinkedHashSet<T>(array1Length + array2Length);
        for (T anArray1 : array1) union.add(anArray1);
        for (T anArray2 : array2) union.add(anArray2);

        T[] unionedArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), union.size());
        return union.toArray(unionedArray);
    }

    public static <T> T[] strengthenArrayType(Object[] array, Class<T> newType) {
        T[] newArray = (T[]) Array.newInstance(newType, array.length);
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static <T> T[] appendArrays(T[]... arrays) {
        Class clazz = null;
        int mergedLength = 0;
        for(T[] array : arrays) {
            if(array != null) {
                if(clazz == null) {
                    clazz = array.getClass().getComponentType();
                } else {
                    assert clazz.isAssignableFrom(array.getClass().getComponentType()) ||
                            array.getClass().getComponentType().isAssignableFrom(clazz)
                            : "Invalid class";
                }
                mergedLength += array.length;
            }
        }

        T[] mergedArray = (T[]) Array.newInstance(clazz, mergedLength);
        int start = 0;
        for(T[] array : arrays) {
            if(array != null && array.length > 0) {
                System.arraycopy(array, 0, mergedArray, start, array.length);
                start += array.length;
            }
        }
        return mergedArray;
    }

    public static <T extends Comparable> T[] appendAndSortArrays(T[]... arrays) {
        T[] mergedArray = appendArrays(arrays);
        Arrays.sort(mergedArray);
        return mergedArray;
    }

    public static <T extends Comparable> T[] appendAndSortArrays(Comparator<T> comparator, T[]... arrays) {
        T[] mergedArray = appendArrays(arrays);
        Arrays.sort(mergedArray, comparator);
        return mergedArray;
    }


    @SuppressWarnings("unchecked")
    public static <S, T> T[] getSubPropertyArray(S[] sourceArray, T[] targetArray, String property) {
        String[] beanPath = Generic.beanPathStringToArray(property);
        for (int i = 0; i < sourceArray.length; i++) {
            targetArray[i] = (T) Generic.get(sourceArray[i], beanPath, 0, false);
        }
        return targetArray;
    }

    public static <T> T[] filterArray(T[] sourceArray, Filter filter) {
        ArrayList<T> filteredObjects = new ArrayList<T>(sourceArray.length);
        for (T o : sourceArray) {
            if (filter.evaluate(o)) filteredObjects.add(o);
        }
        return filteredObjects.toArray((T[]) Array.newInstance(sourceArray.getClass().getComponentType(), filteredObjects.size()));
    }

    public static <T> T[] copyArray(T[] array1) {
        if (array1 == null) return null;
        int array1Length = array1.length;
        T[] copiedArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), array1Length);
        System.arraycopy(array1, 0, copiedArray, 0, array1Length);
        return copiedArray;
    }

    /**
     * Not very efficient if both array1 and array2 contain many entries.
     * Assummes both array1 and array2 are of the same component type.
     */
    public static <T> T[] intersection(T[] array1, T[] array2) {
        if (array1 == null || array2 == null) return null;
        Set<T> intersection = new HashSet<T>(array1.length + array2.length);

        assert array1.getClass().getComponentType().isAssignableFrom(array2.getClass().getComponentType()) ||
                array2.getClass().getComponentType().isAssignableFrom(array1.getClass().getComponentType())
                : "Invalid class";

        for (T o1 : array1) {
            for (T o2 : array2) {
                if (equals(o1, o2)) intersection.add(o1);
            }
        }

        T[] intersectionArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), intersection.size());
        return intersection.toArray(intersectionArray);
    }

    public static <T> T[] subSection(T[] array, int start, int length) {
        T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), length);
        System.arraycopy(array, start, newArray, 0, length);
        return newArray;
    }

    public static <T> T[] difference(T[] leftArray, T[] rightArray) {
        assert leftArray.getClass().getComponentType().isAssignableFrom(rightArray.getClass().getComponentType()) ||
                rightArray.getClass().getComponentType().isAssignableFrom(leftArray.getClass().getComponentType())
                : "Invalid class";

        Set<T> rightSet = new HashSet<T>(rightArray.length);
        for (T right : rightArray) {
            rightSet.add(right);
        }

        Set<T> differenceSet = new LinkedHashSet<T>(leftArray.length);
        for (T left : leftArray) {
            if (!rightSet.contains(left)) differenceSet.add(left);
        }

        T[] differenceArray = (T[]) Array.newInstance(leftArray.getClass().getComponentType(), differenceSet.size());
        differenceSet.toArray(differenceArray);
        return differenceArray;
    }


    public static int[] difference(int[] leftIntArray, int[] rightIntArray) {
        Set<Integer> rightSet = new HashSet<Integer>(rightIntArray.length);
        for (int rightInt : rightIntArray) {
            rightSet.add(rightInt);
        }

        Set<Integer> leftSet = new LinkedHashSet<Integer>(leftIntArray.length);
        for (int leftInt : leftIntArray) {
            if (!rightSet.contains(leftInt)) leftSet.add(leftInt);
        }

        int[] differenceIntArray = new int[leftSet.size()];
        int i = 0;
        for (Integer differenceInt : leftSet) {
            differenceIntArray[i] = differenceInt;
            i++;
        }
        return differenceIntArray;
    }

    public static <T> T[] ensureCapacity(T[] array, int capacity) {
        if (array.length >= capacity) return array;
        T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), capacity);
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public static String getUnqualifiedName(String fullyQualifiedName, char qualifierToken) {
        int lastIndexOfQualifierToken = fullyQualifiedName.lastIndexOf(qualifierToken);
        if (lastIndexOfQualifierToken != -1) {
            return fullyQualifiedName.substring(lastIndexOfQualifierToken + 1);
        } else {
            return fullyQualifiedName;
        }
    }

    public static String getQualifier(String fullyQualifiedName, char qualifierToken) {
        int lastIndexOfQualifierToken = fullyQualifiedName.lastIndexOf(qualifierToken);
        if (lastIndexOfQualifierToken != -1) {
            return fullyQualifiedName.substring(0, lastIndexOfQualifierToken);
        } else {
            return fullyQualifiedName;
        }
    }

    public static final Iterator EMPTY_ITERATOR = new Iterator() {
        public boolean hasNext() {
            return false;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public Object next() {
            return null;
        }
    };

    public static String getNextId() {
        StringBuffer id = new StringBuffer(49);

        if (localhost == null) {
            try {
                InetAddress localhostInetAddress = InetAddress.getLocalHost();
                localhost = localhostInetAddress.getHostAddress();
            } catch (Throwable e) {
                System.err.println(e);
                e.printStackTrace();
                localhost = new Object().toString();
            }
        }

        id.append(localhost); // prevent applications on different machines creating the same id
        id.append('-');
        id.append(Math.abs(System.identityHashCode(new Object()))); // prevent two VMs on the machine creating the same id
        id.append('-');
        id.append(Math.abs(System.currentTimeMillis())); // prevent the same VM on the machine creating the same id over time
        id.append('-');
        id.append(Math.abs(++idCounter)); // prevent the same VM on the machine creating the same id in the same millisecond

        return id.toString();
    }

    public static Map collectionToMap(String[] keyPropertyNames, Collection values) {
        Map result = new HashMap(values.size());
        for (Iterator i = values.iterator(); i.hasNext();) {
            Object value = i.next();
            Object key = createKey(keyPropertyNames, value);
            result.put(key, value);
        }
        return result;
    }

    public static Object createKey(String[] keyPropertyNames, Object value) {
        Object key;

        if (keyPropertyNames.length == 1) {
            key = Generic.get(value, keyPropertyNames[0]);
        } else {
            key = new EfficientArrayList(keyPropertyNames.length);
            for (int j = 0; j < keyPropertyNames.length; j++) {
                String name = keyPropertyNames[j];
                ((List) key).add(Generic.get(value, name));
            }
        }
        return key;
    }

    public static String asString(int[] objects, String separator) {
        if (objects == null) return "";

        StringBuffer buffer = new StringBuffer(objects.length * 10);
        int j = 0;
        for (int i = 0; i < objects.length; i++) {
            if (j > 0) buffer.append(separator);
            buffer.append(objects[i]);
            j++;
        }
        return buffer.toString();
    }

    public static String asString(Object[] objects, String separator) {
        if (objects == null) return "";

        StringBuffer buffer = new StringBuffer(objects.length * 10);
        int j = 0;
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] != null) {
                if (j > 0) buffer.append(separator);
                buffer.append(objects[i]);
                j++;
            }
        }
        return buffer.toString();
    }

    public static String asString(Collection objects, String separator) {
        if (objects == null) return "";

        StringBuffer buffer = new StringBuffer(objects.size() * 10);
        int i = 0;
        for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            if (object != null) {
                if (i > 0) buffer.append(separator);
                buffer.append(object);
                i++;
            }
        }
        return buffer.toString();
    }

    public static String asString(Enumeration objects, String separator) {
        if (objects == null) return "";

        StringBuffer buffer = new StringBuffer();
        int i = 0;

        while (objects.hasMoreElements()) {
            Object object = objects.nextElement();
            if (object != null) {
                if (i > 0) buffer.append(separator);
                buffer.append(object);
                i++;
            }
        }
        return buffer.toString();
    }

    public static boolean booleanValue(String stringValue) {
        return stringValue != null && (
                stringValue.equals("1") ||
                stringValue.equalsIgnoreCase("t") ||
                stringValue.equalsIgnoreCase("true") ||
                stringValue.equalsIgnoreCase("y") ||
                stringValue.equalsIgnoreCase("yes"));
    }

    public static Object changeType(Class expectedClass, Object value) {
        if (value == null) return value;
        Class actualClass = value.getClass();
        if (actualClass == expectedClass) return value;
        if (ClassUtilities.classToType(actualClass) == expectedClass) return value;

        try {
            if (actualClass == String.class) {
                if (expectedClass == Character.class || expectedClass == char.class) {
                    // if setting a char from a String
                    value = new Character(((String) value).charAt(0));
                } else if (expectedClass == Boolean.class || expectedClass == boolean.class) {
                    // if setting a boolean from a String
                    value = Boolean.valueOf(booleanValue((String) value));
                } else if (expectedClass == Integer.class || expectedClass == int.class) {
                    value = ((String) value).trim();
                    if (value.equals("") || value.equals("0")) {
                        value = INTEGER_ZERO;
                    } else {
                        value = new Integer((String) value);
                    }
                } else if (expectedClass == Long.class || expectedClass == long.class) {
                    value = ((String) value).trim();
                    if (value.equals("") || value.equals("0")) {
                        value = LONG_ZERO;
                    } else {
                        value = new Long((String) value);
                    }
                } else if (expectedClass == Double.class || expectedClass == double.class) {
                    value = ((String) value).trim();
                    if (value.equals("") || value.equals("0")) {
                        value = DOUBLE_ZERO;
                    } else {
                        value = new Double((String) value);
                    }
                }
                //else leave as String
            } else if (Number.class.isAssignableFrom(actualClass)) {

                Number number = (Number) value;
                if (expectedClass == Double.class || expectedClass == double.class) {
                    value = new Double(number.doubleValue());
                } else if (expectedClass == Float.class || expectedClass == float.class) {
                    value = new Float(number.doubleValue());
                } else if (expectedClass == Long.class || expectedClass == long.class) {
                    value = new Long(number.longValue());
                } else if (expectedClass == Integer.class || expectedClass == int.class) {
                    value = new Integer(number.intValue());
                } else if (expectedClass == Short.class || expectedClass == short.class) {
                    value = new Short(number.shortValue());
                } else if (expectedClass == BigDecimal.class) {
                    double doubleValue = number.doubleValue();
                    if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                        value = null;
                    } else {
                        value = new BigDecimal(doubleValue);
                    }
                } else if (expectedClass == BigInteger.class) {
                    value = new BigInteger(number.toString());
                }
            } else if (expectedClass == java.sql.Date.class && actualClass != java.sql.Date.class && java.util.Date.class.isAssignableFrom(actualClass)) {
                // if setting a java.sql.Date from sub class of java.util.Date
                // this will also clip the time component off.
                value = DateUtilities.newDate((Date) value);

            } else if (expectedClass == java.util.Date.class && actualClass != java.util.Date.class && java.util.Date.class.isAssignableFrom(actualClass)) {
                // if setting a java.util.Date from sub class of java.util.Date
                value = new java.util.Date(((Date) value).getTime());
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not map object type: " + actualClass.getName() + ", value: " + value + " to an instance of: " + expectedClass.getName(), e);
        }

        return value;
    }

    public static Object[] deriveFromTransform(Object[] source, Object[] target, Transform transform) {
        for (int i = 0; i < source.length; i++) {
            target[i] = transform.execute(source[i]);
        }
        return target;
    }

    public static Thread newThread(String name, boolean daemon) {
        return newThread(null, name, daemon);
    }

    public static Thread newThread(Runnable runnable, String name, boolean daemon) {
        Thread thread = new Thread(runnable, name);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.setDaemon(daemon);
        thread.setContextClassLoader(ClassUtilities.getApplicationClassLoader());
        return thread;
    }

    public static Object getNullValue(Class clazz) {
        if (clazz == double.class) {
            return DOUBLE_NAN;
        } else if (clazz == float.class) {
            return FLOAT_NAN;
        } else if (clazz == long.class) {
            return LONG_ZERO;
        } else if (clazz == int.class) {
            return INTEGER_ZERO;
        } else if (clazz == short.class) {
            return SHORT_ZERO;
        } else if (clazz == byte.class) {
            return BYTE_ZERO;
        } else {
            return null;
        }
    }

    public static List add(Object o, List oldList) {
        List newList;
        if (oldList == null) {
            newList = new ArrayList(1);
        } else {
            newList = new ArrayList(oldList.size() + 1);  // dont operate on the oldList, to avoid ConcurrentModifications
            newList.addAll(oldList);
        }

        newList.add(o);
        return newList;
    }

    public static List remove(Object o, List oldList) {
        List newList;
        if (oldList == null) {
            newList = new ArrayList(0);
        } else {
            newList = new ArrayList(oldList.size()); // dont operate on the oldList, to avoid ConcurrentModifications
            newList.addAll(oldList);
            newList.remove(o);
        }
        return newList;
    }

    public static Map asMap(Object[][] o) {
        Map m = new HashMap(o.length);
        assert o.length == 0 || o[0].length == 2 : "Second array dimension should be 2";
        for (int i = 0; i < o.length; i++) {
            Object[] pair = o[i];
            m.put(pair[0], pair[1]);
        }
        return m;
    }

    public static String substituteTokens(String unsubstitutedString, Transform tokenToValueTransform) {
        StringBuffer substitutedString = new StringBuffer(unsubstitutedString.length());
        Matcher argumentMatcher = argumentPattern.matcher(unsubstitutedString);
        while (argumentMatcher.find()) {
            String group = argumentMatcher.group(2);
            String property = (String) tokenToValueTransform.execute(group);
            argumentMatcher.appendReplacement(substitutedString, escapePatternMatchingChars(property));
        }
        argumentMatcher.appendTail(substitutedString);
        return escapeEscapeChar(substitutedString.toString());
    }

    public static String substituteTokens(String unsubstituedString, final Map<String, String> tokenMap) {
        return substituteTokens(unsubstituedString, new Transform<String, String>() {
            public String execute(String sourceData) {
                return tokenMap.get(sourceData);
            }
        });
    }

    private static String escapeEscapeChar(String string) {
        Matcher escapedMarkerMatcher = escapedMarkerPattern.matcher(string);

        StringBuffer substitutedString = new StringBuffer(string.length());
        while (escapedMarkerMatcher.find()) {
            escapedMarkerMatcher.appendReplacement(substitutedString, "%");
        }
        escapedMarkerMatcher.appendTail(substitutedString);

        return substitutedString.toString();
    }

    /**
     * Escape any characters that are interpreted by RegEx, e.g. tokens for back-references.
     */
    private static String escapePatternMatchingChars(String string) {
        Matcher dollarMatcher = dollarPattern.matcher(string);

        StringBuffer substitutedString = new StringBuffer(string.length());
        while (dollarMatcher.find()) {
            dollarMatcher.appendReplacement(substitutedString, "\\\\\\$");
        }
        dollarMatcher.appendTail(substitutedString);

        return substitutedString.toString();
    }

    public static int getCommonStartLength(String a, String b) {
        int max = Math.min(a.length(), b.length());
        for (int i = 0; i < max; i++) {
            if (a.charAt(i) != b.charAt(i)) return i;
        }
        return max;
    }

    public static void sort(List list) {
        sort(list, null);
    }

    /**
     * A very unsporting method that gives the finger to Collections.sort and has decided to do the sorting itself.
     * This is because Collections.sort is very slow, causing elements to be removed and added many times.
     */
    public static void sort(List list, Comparator comparator) {
        Object a[] = list.toArray();
        list.clear();
        if (comparator != null) {
            Arrays.sort(a, comparator);
        } else {
            Arrays.sort(a);
        }
        for (int i = 0; i < a.length; i++) {
            list.add(a[i]);
        }
    }

    public static void sort(BeanCollection beanCollection) {
        sort(beanCollection, null);
    }

    /**
     * A very unsporting method that gives the finger to Collections.sort and has decided to do the sorting itself.
     * This is because Collections.sort is very slow, causing elements to be removed and added many times.
     */
    public static void sort(BeanCollection beanCollection, Comparator comparator) {
        Object a[] = beanCollection.toArray();
        beanCollection.clear(false);
        if (comparator != null) {
            Arrays.sort(a, comparator);
        } else {
            Arrays.sort(a);
        }
        for (int i = 0; i < a.length; i++) {
            beanCollection.add(a[i], false);
        }
        beanCollection.fireCommit();
    }

    public static void reverse(List list) {
        Object a[] = list.toArray();
        list.clear();
        for (int i = a.length - 1; i >= 0; i--) {
            Object o = a[i];
            list.add(o);
        }
    }

    public static int getRandomInt(int min, int max) {
        if (min > max) throw new IllegalArgumentException("min > max");

        if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) { // special case
            return random.nextInt();
        } else {
            return random.nextInt(max - min + 1) + min;
        }
    }

    public static double getRandomDouble(double min, double max) {
        if (min > max) throw new IllegalArgumentException("min > max");
        return (random.nextDouble() * (max - min)) + min;
    }

    public static String cutTextFromString(String typedText, int startTextCrop, int endTextCrop) {
        StringBuffer resultBuffer = new StringBuffer(typedText.substring(0, startTextCrop));
        resultBuffer.append(typedText.substring(endTextCrop));
        return resultBuffer.toString();
    }

    /**
     * The same as String.lastIndexOf() except that it returns the mirror; the index backwards from the end
     * of the end of the string.
     */
    public static int lastIndexFromEndOf(String typedText, char c) {
        int firstMatchForwards = typedText.indexOf(c);
        return firstMatchForwards == -1 ? -1 : typedText.length() - firstMatchForwards;
    }

    public static Object[] stripNulls(Object[] objects) {
        int nonNullStart = 0;
        int nonNullLength;
        Object[] temp = null;
        int insertIndex = 0;

        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                nonNullLength = i - nonNullStart;
                if (temp == null) {
                    temp = (Object[]) java.lang.reflect.Array.newInstance(objects.getClass().getComponentType(), objects.length - 1);
                }
                System.arraycopy(objects, nonNullStart, temp, insertIndex, nonNullLength);
                insertIndex += nonNullLength;
                nonNullStart = i + 1;
            }
        }
        if (temp != null) {
            nonNullLength = objects.length - nonNullStart;
            System.arraycopy(objects, nonNullStart, temp, insertIndex, nonNullLength);
            insertIndex += nonNullLength;
            if (insertIndex == temp.length) {
                return temp;
            } else {
                Object[] newTemp = (Object[]) java.lang.reflect.Array.newInstance(objects.getClass().getComponentType(), insertIndex);
                System.arraycopy(temp, 0, newTemp, 0, insertIndex);
                return newTemp;
            }
        } else {
            return objects;
        }
    }

    public static String createSpaceFilledString(int length) {
        return createFilledString(length, ' ');
    }

    public static String createFilledString(int length, char filler) {
        StringBuffer stringBuffer = new StringBuffer(length);
        for (int i=0; i<length; i++) {
            stringBuffer.append(filler);
        }
        return stringBuffer.toString();
    }

    public static Throwable getRootCause(Throwable ex) {
        while (ex.getCause() != null) ex = ex.getCause();
        return ex;
    }

    public static class KeyColumnsToKeyTransform implements Transform {
        private final String[] keyColumns;

        public KeyColumnsToKeyTransform(String[] keyColumns) {
            this.keyColumns = keyColumns;
        }

        public Object execute(Object sourceData) {
            return Utilities.createKey(keyColumns, sourceData);
        }
    }

    public static int identityIndexOf(Object[] array, Object searchObject) {
        for (int i = 0; i < array.length; i++) {
            if (searchObject == array[i]) return i;
        }
        return -1;
    }

    public static String surroundUrlsWithHrefs(String stringWithUrls) {
        return stringWithUrls.replaceAll(URL_MATCHER_PATTERN, "<a href=\"$1\">$1</a>");
    }

    public static double round(double valueToTruncate, int requiredDecimalPlaces) {
        assert requiredDecimalPlaces >= 0 : "Can't round to a negative number of decimal places!";
        double scalingFactor = Math.pow(10d, requiredDecimalPlaces);
        double scaledValueToTruncate = valueToTruncate * scalingFactor;
        int roundedScaledValueToTruncate = (int) (scaledValueToTruncate + 0.5d); // rely on int just chopping off values after decimal place
        return ((double) roundedScaledValueToTruncate) / scalingFactor;
    }

    /**
     * Rounds the number to the nearest step.
     * <p>
     * When the step size is 0.05 for instance,
     * number 1.435 will be rounded to 1.45 and
     * number 1.423 will be rounded to 1.40
     *
     * @param valueToRound
     * @param step
     */
    public static double roundToNearestStep(double valueToRound, double step) {
        double modulo = valueToRound % step;
        double result;
        if (modulo > (step / 2.0)) {
            result = valueToRound - modulo + step;
        } else {
            result = valueToRound - modulo;
        }
        int decimalPlaces = getNumberOfDecimalPlaces(step);
        if (decimalPlaces == -1) {
            String msg = "Cannot get the number of decimal places for tick size: " + step;
            assert false : msg;
            Log.getCategory(Utilities.class).error(msg, new RuntimeException());
            return valueToRound;
        }
        double scalingFactor = Math.pow(10.0, decimalPlaces);
        result = Math.round(result * scalingFactor) / scalingFactor;
        return result;
    }

    public static int getNumberOfDecimalPlaces(double number) {
        for (int decimalPlaces = 1; decimalPlaces < 20; decimalPlaces++) {
            number *= 10.0;
            if ((double)((int)number) == number) {
                return decimalPlaces;
            }
        }
        return -1;
    }

    public static void internStringsInArray(String[] s) {
        for ( int loop=0; loop < s.length; loop ++) {
            if ( s[loop] != null) {
                s[loop] = s[loop].intern();
            }
        }
    }
}

