package org.bhavaya.ui.compass;

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.util.Generic;
import org.bhavaya.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * There are not too many real world cases where this be used.  What is required is that doubly-linked tree of beans;
 * parents must know their children, but children must also know their parents.  The latter is often not the case
 * in many object graphs.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class OneToManyBeanTypeHierarchy extends CompassTypeHierarchy {
    private static final Log log = Log.getCategory(OneToManyBeanTypeHierarchy.class);

    private String manyOfThisTypeProperty;
    private String descriptionProperty;
    private String manyOfNextTypeProperty;
    private String parentOfThisTypeProperty;
    private String parentOfNextTypeProperty;

    public OneToManyBeanTypeHierarchy(String name, Class type, String descriptionProperty, String parentOfThisTypeProperty,
                                      String parentOfNextTypeProperty, String manyOfNextTypeProperty, String manyOfThisTypeProperty) {
        super(name, type);
        this.descriptionProperty = descriptionProperty;
        this.parentOfThisTypeProperty = parentOfThisTypeProperty;
        this.parentOfNextTypeProperty = parentOfNextTypeProperty;
        this.manyOfNextTypeProperty = manyOfNextTypeProperty;
        this.manyOfThisTypeProperty = manyOfThisTypeProperty;
    }

    public List getChildrenOfThisType(Object key) {
        if (manyOfThisTypeProperty == null) return Collections.EMPTY_LIST;
        Object bean = BeanFactory.getInstance(getType()).get(key);
        Object result = Generic.get(bean, manyOfThisTypeProperty);
        return convertResultToCollection(result);
    }

    private List convertResultToCollection(Object result) {
        if (result instanceof List) {
            return (List) result;
        } else if (result == null) {
            return Collections.EMPTY_LIST;
        } else if (result.getClass().isArray()) {
            return Arrays.asList((Object[]) result);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public List getChildrenOfNextType(Object key) {
        if (manyOfNextTypeProperty == null) return Collections.EMPTY_LIST;

        Object bean = BeanFactory.getInstance(getType()).get(key);
        Object result = Generic.get(bean, manyOfNextTypeProperty);
        return convertResultToCollection(result);
    }

    public List findMatchesForString(String searchString) {
        // Yikes!  We might not have available??!!!
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object findParentKeyOfThisType(Object key) {
        if (parentOfThisTypeProperty == null) return null;

        Object bean = BeanFactory.getInstance(getType()).get(key);
        return BeanFactory.getKeyForBean(Generic.get(bean, parentOfThisTypeProperty));
    }

    public Object findParentKeyOfNextType(Object key) {
        if (parentOfNextTypeProperty == null) return null;

        Object bean = BeanFactory.getInstance(getType()).get(key);
        return BeanFactory.getKeyForBean(Generic.get(bean, parentOfNextTypeProperty));
    }

    public Boolean getNodeIsLeafOverride() {
        return new Boolean(manyOfNextTypeProperty != null || manyOfThisTypeProperty != null);
    }
}
