package org.bhavaya.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Replaced by PropertyMetaData and ClassMetaData annotations
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.4 $
 */
@Deprecated
public class PropertyModelReader {
    private static final Log log = Log.getCategory(PropertyModelReader.class);

    private String filename;
    private boolean reconcileOnly;

    /**
     * @param reconcileOnly will not actually use properties in file, just reconcile them against those already loaded
     */
    public PropertyModelReader(String filename, boolean reconcileOnly) {
        this.filename = filename;
        this.reconcileOnly = reconcileOnly;
    }

    public void read() {
        Log.getPrimaryLoadingLog().info("Loading property model definitions");
        Log.getSecondaryLoadingLog().info(" ");

        if (log.isDebug()) log.debug("Attempting to read " + filename);
        InputStream xmlStream = IOUtilities.getResourceAsStream(filename);

        if (xmlStream == null) {
            throw new RuntimeException("Could not find file " + filename);
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
            Node node = types.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            String nodeName = node.getNodeName();
            if (nodeName.equalsIgnoreCase("type")) {
                parseType(node);
            }
        }
    }

    private void parseType(Node node) {
        String className = getAttributeValue(node, "className");
        Class type = ClassUtilities.getClass(className, false, false);
        if (type == null) {
            log.error("Unknown class name: " + className);
            return;
        }
        PropertyModel propertyModel = PropertyModel.getInstance(type);

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;

            String childNodeName = childNode.getNodeName();
            if (childNodeName.equalsIgnoreCase("property")) {
                parseProperty(propertyModel, childNode);
            }
        }
    }

    private void parseProperty(PropertyModel parentTypePropertyModel, Node propertyNode) {
        Class parentType = parentTypePropertyModel.getType();

        String propertyName = getAttributeValue(propertyNode, "name");
        String displayName = getAttributeValue(propertyNode, "displayName");
        boolean hidden = getBooleanAttribute(propertyNode, "hidden", Boolean.FALSE);
        String description = getAttributeValue(propertyNode, "description");

        ArrayList validTypes = new ArrayList();
        NodeList children = propertyNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;

            String childNodeName = childNode.getNodeName();
            if (childNodeName.equalsIgnoreCase("description")) {
                description = getNodeTextValue(childNode);
                if (description != null) description = description.trim();
            } else if (childNodeName.equalsIgnoreCase("validType")) {
                String validTypeName = getAttributeValue(childNode, "className");
                Class validType = ClassUtilities.getClass(validTypeName, false, false);
                if (validType != null) validTypes.add(validType);
            }
        }

        if (displayName != null && displayName.length() > 0) {
            if (reconcileOnly) {
                assert Utilities.equals(parentTypePropertyModel.getDisplayName(propertyName), displayName) : "" + parentType + ":" + displayName;
            } else {
                parentTypePropertyModel.setDisplayName(propertyName, displayName);
            }
        }

        if (hidden) {
            if (reconcileOnly) {
                PropertyModel propertyModel = parentTypePropertyModel;
                do {
                    if (propertyModel.isHidden(propertyName) == hidden) break;
                    if (propertyModel.getType() == null || propertyModel.getType().getSuperclass() == Object.class)
                        assert false: parentType.toString();
                    propertyModel = PropertyModel.getInstance(propertyModel.getType().getSuperclass());
                } while (true);
            } else {
                parentTypePropertyModel.setHidden(propertyName, hidden);
            }
        }

        if (validTypes.size() > 0) {
            if (reconcileOnly) {
                Class[] alreadyLoadedTypes = parentTypePropertyModel.getValidTypesForProperty(propertyName);
                assert alreadyLoadedTypes != null : "" + parentType + ":validTypes null";
                for (Class<?> aClass : alreadyLoadedTypes) {
                    validTypes.remove(aClass);
                }
                assert validTypes.size() == 0 : "" + parentType + ":validTypeMismatch";
            } else {
                Class[] validTypesArray = (Class[]) validTypes.toArray(new Class[validTypes.size()]);
                parentTypePropertyModel.setValidTypesForProperty(propertyName, validTypesArray);
            }
        }

        if (description != null) {
            if (reconcileOnly) {
                if (!Utilities.equals(parentTypePropertyModel.getDescription(propertyName), description)) {
                    log.error("Mismatch for " + parentType + "descriptions\n" +
                            "Old: " + description +
                            "\nNew: " + parentTypePropertyModel.getDescription(propertyName));

                }
            } else {
                parentTypePropertyModel.setDescription(propertyName, description);
            }
        }
    }

    private boolean getBooleanAttribute(Node parentNode, String attribute, Boolean defaultValue) {
        String booleanString = getAttributeValue(parentNode, attribute);
        if (defaultValue != null && booleanString == null) {
            return defaultValue.booleanValue();
        } else if (booleanString != null && booleanString.equalsIgnoreCase("TRUE")) {
            return true;
        } else if (booleanString != null && booleanString.equalsIgnoreCase("FALSE")) {
            return false;
        } else {
            throw new IllegalArgumentException("Invalid value for: " + attribute + ". Valid values are TRUE or FALSE");
        }
    }

    private String getAttributeValue(Node node, String attribute) {
        Node attributeNode = node.getAttributes().getNamedItem(attribute);
        if (attributeNode == null) return null;
        return attributeNode.getNodeValue();
    }

    private String getNodeTextValue(Node node) {
        NodeList subNodes = node.getChildNodes();
        for (int i = 0; i < subNodes.getLength(); i++) {
            Node subNode = subNodes.item(i);
            if (subNode.getNodeType() == Node.TEXT_NODE) {
                return subNode.getNodeValue();
            }
        }
        return null;
    }
}
