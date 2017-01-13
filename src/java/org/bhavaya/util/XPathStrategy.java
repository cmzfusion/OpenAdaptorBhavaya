package org.bhavaya.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


/**
 * This strategy allows you to find a section of XML using an XPath query and then replace it's
 *
 * @author James Langley
 */
public class XPathStrategy implements ConfigMigrationStategy {

    private static final Log log = Log.getCategory(XPathStrategy.class);

    private static final String REMOVE_NODE = "REMOVE_NODE";

    private long versionTarget;
    private String targetConfig;
    private String xpathExpression;
    private String replacementString;
    private boolean removeNode;

    public XPathStrategy(long versionTarget, String[] arguments) {
        this.versionTarget = versionTarget;
        this.targetConfig = arguments[0];
        this.xpathExpression = arguments[1];
        this.replacementString = arguments[2];
        removeNode = REMOVE_NODE.equals(replacementString);
    }

    public String migrate(String configKey, String source) {
        // If there is a config key and it doesn't match, just pass the source on (identity)
        if (!(this.targetConfig == null || this.targetConfig.length() == 0) && !this.targetConfig.equals(configKey))
            return source;
        log.info("Patching " + configKey + " configuration to version " + versionTarget + " with XPath strategy");
        Node rootNode = null;
        try {

            rootNode = stringToXML(source);
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xpath.evaluate(xpathExpression, rootNode, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                if(removeNode) {
                    Node node = nodeList.item(i);
                    node.getParentNode().removeChild(node);
                } else {
                    nodeList.item(i).setNodeValue(replacementString);
                }
            }
        } catch (XPathExpressionException e) {
            log.error(e);
        }

        return xmlToString(rootNode);
    }

    private Node stringToXML(String source) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new ByteArrayInputStream(source.getBytes()));
            return document.getDocumentElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String xmlToString(Node node) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(node), new StreamResult(bos));
            return bos.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
