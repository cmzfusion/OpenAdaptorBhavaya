package org.bhavaya.util;

//import com.sun.beans.ObjectHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.beans.XMLDecoder;
import java.beans.ExceptionListener;
import java.io.InputStream;
import java.io.IOException;

import org.xml.sax.AttributeList;
import org.xml.sax.SAXException;

/**
 * Override the default XMLDecoder to supply an object handler which stores some details of the current
 * element being processed, which we can then use when logging an exception.
 *
 * Otherwise we can too easily end up with decoder errors which contain no contextual information at all.
 *
 * This class has been upgraded to work with Java 1.6.
 * A bug was introduced as getHandler() is now called in close() - not quite sure why but it should now be
 * compatible with Java 1.5 and 1.6
 */
class ElementDetailsLoggingXMLDecoder {
}

/*
class ElementDetailsLoggingXMLDecoder extends XMLDecoder {

    private ObjectHandler handler;
    private final InputStream in;
    private final StringBuilder elementDetails;

    private ElementDetailsLoggingXMLDecoder(InputStream in, StringBuilder elementDetails) {
        super(in, null, new ElementDetailsLoggingExceptionListener(elementDetails));
        this.in = in;
        this.elementDetails = elementDetails;
    }

    public static ElementDetailsLoggingXMLDecoder createDecoder(InputStream in) {
        StringBuilder sb = new StringBuilder();
        return new ElementDetailsLoggingXMLDecoder(in, sb);
    }

    // Override to ensure the correct version of getHandler() is called in 1.6
    // The implementation should be exactly the same as the 1.6 superclass
    public Object readObject() {
        if (in == null) {
            return null;
        }
        return getHandler().dequeueResult();
    }

    // Override to ensure the correct version of getHandler() is called in 1.6
    // The implementation should be exactly the same as the 1.6 superclass
    public void close() { 
        if (in != null) {
            getHandler();
            try {
                in.close();
            }
            catch (IOException e) {
                getExceptionListener().exceptionThrown(e);
            }
        }
    }

    private class ElementDetailsObjectHandler extends ObjectHandler {

        public ElementDetailsObjectHandler() {
            super(ElementDetailsLoggingXMLDecoder.this, null);
        }

        public void startElement(String s, AttributeList attributelist) throws SAXException {
            elementDetails.setLength(0);
            elementDetails.append(" while reading element ").append(s).append(" with attributes ");
            for (int loop = 0; loop < attributelist.getLength(); loop++) {
                elementDetails.append(
                    attributelist.getName(loop)).append(": {").append(attributelist.getValue(loop)).append("},"
                );
            }
            super.startElement(s, attributelist);
        }
    }

    static class ElementDetailsLoggingExceptionListener implements ExceptionListener {

        private final StringBuilder elementDetails;

        public ElementDetailsLoggingExceptionListener(StringBuilder elementDetails) {
            this.elementDetails = elementDetails;
        }

        public void exceptionThrown(Exception e) {
            throw new RuntimeException("Error loading object from XML stream " + elementDetails, e);
        }
    }

    // getHandler() is copied from the 1.6 superclass, in order to create an alterative ObjectHandler
    // (ElementDetailsObjectHandler) which populates a StringBuilder with the xml attribute details for logging.
    // The ElementDetailsLoggingExceptionListener can then use this attribute information when logging an error
    // This method should be the same as the superclass in all other respects
    // Note a subtle change between 1.5 and 1.6 is that getHandler() is called in close().
    // Unfortunately as this method is private in the superclass we need to override readObject() and close()
    // to make sure the correct implementation of getHandler() is called when running in 1.6
    private ObjectHandler getHandler() {
        if (handler == null) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            try {
                SAXParser saxParser = factory.newSAXParser();
                handler = new ElementDetailsObjectHandler();
                saxParser.parse(in, handler);
            }
            catch (ParserConfigurationException e) {
                getExceptionListener().exceptionThrown(e);
            }
            catch (SAXException se) {
                Exception e = se.getException();
                getExceptionListener().exceptionThrown((e == null) ? se : e);
            }
            catch (IOException ioe) {
                getExceptionListener().exceptionThrown(ioe);
            }
        }
        return handler;
    }
}
*/
