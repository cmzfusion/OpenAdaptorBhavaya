package org.bhavaya.beans.generator;

/**
 * Generated by org.bhavaya.beans.generator.SourceCodeGenerator$DefaultGenerator on Thu Feb 19 23:38:22 GMT 2004
 *
 * @author Bhavaya
 * @version $Revision: 1.5 $
 */
public class Property4 extends org.bhavaya.beans.generator.SUPER_CLASS {

    public Property4() {
        super();
    }

    private org.bhavaya.collection.BeanCollection PROPERTY_NAMECollection;

    public org.bhavaya.collection.BeanCollection getPROPERTY_NAMECollection() {
        return PROPERTY_NAMECollection;
    }

    public org.bhavaya.beans.generator.PROPERTY_TYPE[] getPROPERTY_NAME() {
        if (getPROPERTY_NAMECollection() == null) return null;
        return (org.bhavaya.beans.generator.PROPERTY_TYPE[]) getPROPERTY_NAMECollection().toArray(new org.bhavaya.beans.generator.PROPERTY_TYPE[getPROPERTY_NAMECollection().size()]);
    }

    public void setPROPERTY_NAMECollection(org.bhavaya.collection.BeanCollection PROPERTY_NAMECollection) {
        org.bhavaya.collection.BeanCollection oldValue = this.PROPERTY_NAMECollection;
        this.PROPERTY_NAMECollection = PROPERTY_NAMECollection;
        firePropertyChange("PROPERTY_NAMECollection", oldValue, PROPERTY_NAMECollection);
    }

}
