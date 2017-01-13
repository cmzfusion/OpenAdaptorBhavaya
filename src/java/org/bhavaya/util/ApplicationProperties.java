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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * WARNING!: DO NOT USE LOGGING IN THIS CLASS.  IF THINK THIS IS A JOKE OR NO LONGER RELEVENT, YOU JUST HAVEN'T
 * UNDERSTOOD THE CHICKEN AND EGG PROBLEM YET.  GO AWAY AND COME BACK WHEN YOU DO.
 * <p/>
 * I wonder if this is still relevant? - Dan
 *
 * @author Brendon McLean
 * @version $Revision: 1.12 $
 */

public class ApplicationProperties {
    private static ApplicationProperties instance;

    private static final String ENVIRONMENT_FILE_SYSTEM_PROPERTY = "ENVIRONMENT_PATH";
    private static final String PROPERTY_GROUP_TAG = "propertyGroup";
    private static final String PROPERTY_GROUP_KEY_ATTR = "key";
    private static final String PROPERTY_TAG = "property";
    private static final String PROPERTY_KEY_ATTR = "key";
    private static final String PROPERTY_VALUE_ATTR = "value";
    private static final String PROPERTY_CRYPTO_ATTR = "encrypted";
    private static final String VALUE_TAG = "value";
    private static final String PROPERTY_EXTERNAL_REF_ATTR = "external_ref";
    private static final String PROPERTY_INTERNAL_REF_ATTR = "internal_ref";
    private static final String PROPERTY_CHECK_OVERRIDE = "check_for_override";

    private static final PropertyGroup EMPTY_ROOT_PROPERTY_GROUP = new PropertyGroup(null, "root");

    private static final Map instanceMap = new HashMap();
    private static String[] environmentPath;
    private static PropertyGroup rootEnvironmentProperties;


    public static String[] getEnvironmentPath() {
        if (environmentPath == null) {
            String environmentPathString = System.getProperty(ENVIRONMENT_FILE_SYSTEM_PROPERTY);
            if (environmentPathString == null || environmentPathString.length() == 0) {
                environmentPath = new String[0];
            } else {
                environmentPath = environmentPathString.split(",");
            }
        }
        return environmentPath;
    }

    public synchronized static PropertyGroup getApplicationProperties() {
        if (rootEnvironmentProperties == null) {
            String[] environmentPath = getEnvironmentPath();
            if (environmentPath.length == 0) {
                rootEnvironmentProperties = EMPTY_ROOT_PROPERTY_GROUP;
            } else {
                rootEnvironmentProperties = getInstance(environmentPath[0], environmentPath, 0);
            }
        }
        return rootEnvironmentProperties;
    }

    public static String substituteApplicationProperties(String templateString) {
        if (templateString == null) return null;
        if (getEnvironmentPath().length == 0) return templateString;
        PropertyGroup rootProperties = getApplicationProperties();
        return substituteTokens(templateString, rootProperties, getEnvironmentPath(), 0);
    }

    public static void clearApplicationProperties() {
        environmentPath = null;
        rootEnvironmentProperties = null;
        instanceMap.clear();
    }

    public static PropertyGroup getInstance(String filename) {
        if (getEnvironmentPath().length == 0) {
            return getUnsubstitutedInstance(filename);
        } else {
            return getInstance(filename, getEnvironmentPath(), 0);
        }
    }

    public static PropertyGroup getInstance(String[] filenames) {
        return getInstance(filenames[0], filenames, 0);
    }

    public static PropertyGroup getUnsubstitutedInstance(String filename) {
        return getInstance(filename, null, 0);
    }

    public synchronized static PropertyGroup getInstance(String file, String[] filenames, int filenameDepth) {
        try {
            if (filenames == null) {
                filenames = new String[0];
            }
            if (instance == null) {
                instance = new ApplicationProperties();
            }

            ArrayList key = new ArrayList(filenames.length + 1);
            key.add(file);
            key.addAll(Arrays.asList(filenames));

            PropertyGroup propertyGroup = (PropertyGroup) instanceMap.get(key);
            if (propertyGroup == null) {
                propertyGroup = createRootPropertyGroup(file, filenames, filenameDepth);
                instanceMap.put(key, propertyGroup);
            }

            return propertyGroup;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PropertyGroup createRootPropertyGroup(String filename, String[] filenames, int filenameDepth) {
        InputStream is = IOUtilities.getResourceAsStream(filename);
        if (is == null) {
            return EMPTY_ROOT_PROPERTY_GROUP;
        }

        try {
            return createRootPropertyGroup(is, filenames, filenameDepth);
        } finally {
            IOUtilities.closeStream(is);
        }
    }

    public static PropertyGroup createRootPropertyGroup(InputStream is, String[] filenames, int filenameDepth) {
        Node root;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setCoalescing(true);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xmlCriterion = builder.parse(is);
            root = xmlCriterion.getDocumentElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return createPropertyGroupFromNode(root, null, filenames, filenameDepth);
    }

    private static PropertyGroup createPropertyGroupFromNode(Node node, final PropertyGroup parent, String[] filenames, int filenameDepth) {
        NamedNodeMap attributes = node.getAttributes();

        String name = attributes.getNamedItem(PROPERTY_GROUP_KEY_ATTR).getNodeValue();
        if (name == null) {
            throw new RuntimeException("Encountered a null name in file: " + Utilities.asString(filenames, ","));
        }
        PropertyGroup propertyGroup = new PropertyGroup(parent, name);

        NodeList childNodeList = node.getChildNodes();
        for (int i = 0; i < childNodeList.getLength(); i++) {
            Node child = childNodeList.item(i);
            String childTag = child.getNodeName();

            // A leaf property
            if (childTag.equals(PROPERTY_TAG)) {
                NamedNodeMap propertyAttributes = child.getAttributes();
                Node keyNode = propertyAttributes.getNamedItem(PROPERTY_KEY_ATTR);
                String propertyName = keyNode.getNodeValue();

                boolean encrypted = false;
                Node cryptoNode = propertyAttributes.getNamedItem(PROPERTY_CRYPTO_ATTR);
                if (cryptoNode != null) {
                    encrypted = cryptoNode.getNodeValue().equalsIgnoreCase("true");
                }

                boolean checkForOverride = false;
                Node overrideNode = propertyAttributes.getNamedItem(PROPERTY_CHECK_OVERRIDE);
                if (overrideNode != null) {
                    checkForOverride = overrideNode.getNodeValue().equalsIgnoreCase("true");
                }

                // Check to see if its an internal reference first
                if (propertyAttributes.getNamedItem(PROPERTY_INTERNAL_REF_ATTR) != null) {
                    final String reference = propertyAttributes.getNamedItem(PROPERTY_INTERNAL_REF_ATTR).getNodeValue();
                    propertyGroup.addLazyProperty(propertyName, new PropertyGroup.LazyProperty() {
                        public String[] getProperties() {
                            return getInternalReferencedProperties(reference, getRoot(parent));
                        }
                    });
                }
                // Else, check for external reference
                if (propertyAttributes.getNamedItem(PROPERTY_EXTERNAL_REF_ATTR) != null) {
                    String reference = propertyAttributes.getNamedItem(PROPERTY_EXTERNAL_REF_ATTR).getNodeValue();
                    String[] properties = getExternalReferencedProperties(reference, filenames, filenameDepth, true);
                    for (int j = 0; j < properties.length; j++) {
                        propertyGroup.addProperty(propertyName, properties[j]);
                    }
                }
                // Else, check for scalar value
                else if (propertyAttributes.getNamedItem(PROPERTY_VALUE_ATTR) != null) {
                    String propertyValue = propertyAttributes.getNamedItem(PROPERTY_VALUE_ATTR).getNodeValue();
                    propertyValue = substituteTokens(propertyValue, propertyGroup, filenames, filenameDepth);
                    if (encrypted) propertyValue = Crypto.decrypt(propertyValue);

                    if (checkForOverride) {
                        String fqn = propertyGroup.getFQN();
                        if (fqn.length() > 0) fqn += ".";
                        String[] properties = getExternalReferencedProperties(fqn + propertyName,
                                filenames, filenameDepth, false);
                        if (properties.length > 0) {
                            for (int j = 0; j < properties.length; j++) {
                                propertyGroup.addProperty(propertyName, properties[j]);
                            }
                        } else {
                            propertyGroup.addProperty(propertyName, propertyValue);
                        }
                    } else {
                        propertyGroup.addProperty(propertyName, propertyValue);
                    }
                }
                // Else, it's an array value
                else {
                    NodeList valueChildern = child.getChildNodes();
                    for (int j = 0; j < valueChildern.getLength(); j++) {
                        Node value = valueChildern.item(j);
                        if (value.getNodeName().equals(VALUE_TAG)) {
                            String propertyValue;
                            Node valueNode = value.getChildNodes().item(0);
                            if (valueNode == null) {
                                propertyValue = "";
                            } else {
                                propertyValue = valueNode.getNodeValue();
                            }
                            propertyValue = substituteTokens(propertyValue, parent, filenames, filenameDepth);
                            if (encrypted) propertyValue = Crypto.decrypt(propertyValue);
                            propertyGroup.addProperty(propertyName, propertyValue);
                        }
                    }
                }
            }
            // Else, another subPropertyGroup
            else if (childTag.equals(PROPERTY_GROUP_TAG)) {
                NamedNodeMap propertyGroupAttributes = child.getAttributes();

                Node propertyGroupKey = propertyGroupAttributes.getNamedItem(PROPERTY_GROUP_KEY_ATTR);

                boolean checkForOverride = false;
                Node overrideNode = propertyGroupAttributes.getNamedItem(PROPERTY_CHECK_OVERRIDE);
                if (overrideNode != null) {
                    checkForOverride = overrideNode.getNodeValue().equalsIgnoreCase("true");
                }

                // Check to see if its an internal reference first
                if (propertyGroupAttributes.getNamedItem(PROPERTY_INTERNAL_REF_ATTR) != null) {
                    throw new RuntimeException("Cannot have internally referenced groups");
                }
                // Else, check to see if its an external reference
                else if (propertyGroupAttributes.getNamedItem(PROPERTY_EXTERNAL_REF_ATTR) != null) {
                    String propertyGroupName = propertyGroupAttributes.getNamedItem(PROPERTY_EXTERNAL_REF_ATTR).getNodeValue();
                    PropertyGroup[] referencedPropertyGroups = getReferencedPropertyGroups(propertyGroupName, filenames, filenameDepth, true);
                    for (int j = 0; j < referencedPropertyGroups.length; j++) {
                        propertyGroup.addPropertyGroup(propertyGroupKey.getNodeValue(), referencedPropertyGroups[j]);
                    }
                }
                // Else, its just a straight inline group
                else if (propertyGroupKey != null) {
                    PropertyGroup subGroup = createPropertyGroupFromNode(child, propertyGroup, filenames, filenameDepth);

                    if (checkForOverride) {
                        PropertyGroup[] referencedPropertyGroups = getReferencedPropertyGroups(subGroup.getFQN(), filenames, filenameDepth, false);
                        if (referencedPropertyGroups.length > 0) {
                            for (int j = 0; j < referencedPropertyGroups.length; j++) {
                                propertyGroup.addPropertyGroup(propertyGroupKey.getNodeValue(), referencedPropertyGroups[j]);
                            }
                        } else {
                            propertyGroup.addPropertyGroup(propertyGroupKey.getNodeValue(), subGroup);
                        }
                    } else {
                        propertyGroup.addPropertyGroup(propertyGroupKey.getNodeValue(), subGroup);
                    }
                }
            }
        }

        return propertyGroup;
    }

    private static PropertyGroup getRoot(PropertyGroup propertyGroup) {
        do {
            PropertyGroup parent = propertyGroup.getParent();
            if (parent == null) return propertyGroup;
            propertyGroup = parent;
        } while (true);
    }

    private static String[] getInternalReferencedProperties(String propertyName, PropertyGroup thisRoot) {
        String[] properties = thisRoot.getProperties(propertyName);
        if (properties != null) return properties;

        // Finding externally referenced properties is mandatory.  I think.  Not sure.
        throw new RuntimeException("Cannot find internally referenced properties: " + propertyName);
    }

    private static String[] getExternalReferencedProperties(String propertyName, String[] filenames, int filenameDepth, boolean mandatory) {
        if (filenames != null && filenames.length > 0 && filenameDepth < (filenames.length - 1)) {
            for (int i = filenameDepth + 1; i < filenames.length; i++) {
                String filename = filenames[i];
                PropertyGroup root = getInstance(filename, filenames, i);
                String[] properties = root.getProperties(propertyName, true);
                if (properties != null) return properties;
            }
        } else {
            // This is okay if there are no files to search
            return new String[0];
        }

        if (mandatory) {
            // Finding externally referenced properties is mandatory.  I think.  Not sure.
            throw new RuntimeException("Cannot find externally referenced properties: " + propertyName +
                    ".  Searched in: " + Utilities.asString(filenames, ","));
        } else {
            return new String[0];
        }
    }

    private static PropertyGroup[] getReferencedPropertyGroups(String propertyGroupName, String[] filenames, int filenameDepth, boolean mandatory) {
        if (filenames != null && filenames.length > 0 && filenameDepth < (filenames.length - 1)) {
            for (int i = filenameDepth + 1; i < filenames.length; i++) {
                String filename = filenames[i];
                PropertyGroup root = getInstance(filename, filenames, i);
                PropertyGroup[] matchingGroups = root.getGroups(PropertyGroup.stringToArray(propertyGroupName), !mandatory);
                if (matchingGroups != null) return matchingGroups;
            }
        } else {
            // this is okay if there are no files to search in.
            return new PropertyGroup[0];
        }

        if (mandatory) {
            // Finding externally referenced groups is mandatory.  I think.  Not sure.
            throw new RuntimeException("Cannot find externally referenced propertyGroup: " + propertyGroupName +
                    ".  Searched in: " + Utilities.asString(filenames, ","));
        } else {
            return new PropertyGroup[0];
        }
    }

    /**
     * Takes a String which contains references to PropertyGroup keys and returns a String
     * replacing the keys with the values.  Keys are enclosed by the '%' character.
     * To include a real '%' character it may be escaped using the backslash character.
     * E.g. "Connect to %DB.Url%" might return "Connect to jdbc:sybase:Tds:10.140.91.44:7000"
     */
    private static String substituteTokens(String unsubstitutedString, PropertyGroup parentGroup, String[] filenames, int filenameDepth) {
        if (unsubstitutedString.indexOf('%') == -1) return unsubstitutedString;
        Transform substitutions = new PropertyTransform(parentGroup, filenames, filenameDepth);
        return Utilities.substituteTokens(unsubstitutedString, substitutions);
    }

    private static String findProperty(String propertyName, PropertyGroup parentGroup, String[] filenames, int filenameDepth) {
        String property;

        PropertyGroup currentGroup = parentGroup;
        while (currentGroup != null) {
            property = currentGroup.getProperty(propertyName);
            if (property == null) {
                currentGroup = currentGroup.getParent();
            } else {
                return property;
            }
        }

        // cant find in the same instance, so find in environmentPath
        if (filenames != null && filenames.length > 0 && filenameDepth < (filenames.length - 1)) {
            for (int i = filenameDepth + 1; i < filenames.length; i++) {
                String filename = filenames[i];
                PropertyGroup root = getInstance(filename, getEnvironmentPath(), i);
                property = root.getProperty(propertyName);
                if (property != null) return property;
            }
        }

        throw new RuntimeException("Could not find back-referenced property: " + propertyName);
    }

    private static class PropertyTransform implements Transform {
        private PropertyGroup parentGroup;
        private String[] filenames;
        private int filenameDepth;

        public PropertyTransform(PropertyGroup parentGroup, String[] filenames, int filenameDepth) {
            this.parentGroup = parentGroup;
            this.filenames = filenames;
            this.filenameDepth = filenameDepth;
        }

        public Object execute(Object token) {
            return findProperty((String) token, parentGroup, filenames, filenameDepth);
        }
    }
}
