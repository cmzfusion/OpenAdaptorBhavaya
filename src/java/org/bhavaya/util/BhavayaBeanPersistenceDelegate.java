package org.bhavaya.util;

import org.bhavaya.beans.BeanFactory;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;

/**
 * Use this class to add persistence delegate for generated Bhavaya classes. Will store a key of the bean on write (persist) and
 * will query BeanFactory for this bean on read (unpersist).
 * <p>
 * See {@link BeanUtilities#addBhavayaBeanPersistenceDelegate(Class)} for example how to use it.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.2 $
 */
public class BhavayaBeanPersistenceDelegate extends PersistenceDelegate {

    private Class beanType;

    public BhavayaBeanPersistenceDelegate(Class beanType) {
        this.beanType = beanType;
    }

    protected Expression instantiate(Object oldInstance, Encoder out) {
        return new Expression(oldInstance, BeanFactory.getInstance(beanType), "get", new Object[]{BeanFactory.getKeyForBean(oldInstance)});
    }

    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        return oldInstance.equals(newInstance);
    }

    protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
        // don't call any of the setters and getters after construction        
    }
}
