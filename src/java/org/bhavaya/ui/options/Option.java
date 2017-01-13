package org.bhavaya.ui.options;

import org.bhavaya.util.Transform;

/**
 * Description
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.3 $
 */
public class Option {

    private Object target;
    private String propertyBeanPath;
    private String optionLabel;
    private String optionDescription;
    private Object[] beanCollection;
    private Transform getTransform;
    private Transform setTransform;

    public Option(Object target, String propertyBeanPath, String optionLabel, String optionDescription) {
        this(target, propertyBeanPath, optionLabel, optionDescription, null, null);
    }

    public Option(Object target, String propertyBeanPath, String optionLabel, String optionDescription, Transform getTransform, Transform setTransform) {
        this(target, propertyBeanPath, optionLabel, optionDescription, null, getTransform, setTransform);
    }

    public Option(Object target, String propertyBeanPath, String optionLabel, String optionDescription, Object[] beanCollection) {
        this(target, propertyBeanPath, optionLabel, optionDescription, beanCollection, null, null);
    }

    public Option(Object target, String propertyBeanPath, String optionLabel, String optionDescription, Object[] beanCollection, Transform getTransform, Transform setTransform) {
        this.target = target;
        this.propertyBeanPath = propertyBeanPath;
        this.optionLabel = optionLabel;
        this.optionDescription = optionDescription;
        this.beanCollection = beanCollection;
        this.getTransform = getTransform;
        this.setTransform = setTransform;
    }

    public String toString() {
        return optionLabel;
    }

    public Object getTarget() {
        return target;
    }

    public String getPropertyBeanPath() {
        return propertyBeanPath;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public String getOptionDescription() {
        return optionDescription;
    }

    public Object[] getBeanCollection() {
        return beanCollection;
    }

    public Transform getGetTransform() {
        return getTransform;
    }

    public Transform getSetTransform() {
        return setTransform;
    }
}
