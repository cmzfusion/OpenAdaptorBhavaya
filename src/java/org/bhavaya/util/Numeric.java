package org.bhavaya.util;

/**
 * Simple interface to able to implement classes, which will be handled like java numeric values 
 * by the framework tables. Will give as to flexibility, to use the decimal renderers and 
 * settings by choice, without the need to introduce them into the framework.
 * 
 * @author <a href="mailto:Sabine.Haas@drkw.com">Sabine Haas, Dresdner Kleinwort</a>
 * @version $Revision: 1.1 $
 */
public interface Numeric {
    double doubleValue();
}