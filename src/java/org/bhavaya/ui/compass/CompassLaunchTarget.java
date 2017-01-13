package org.bhavaya.ui.compass;

import javax.swing.*;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public abstract class CompassLaunchTarget {
    private Icon icon;
    private String name;
//    private BeanCollection beanCollection;
//    private String viewConfigurationId;

    public CompassLaunchTarget(Icon icon, String name) {
        this.icon = icon;
//        this.beanCollection = beanCollection;
        this.name = name;
//        this.viewConfigurationId = viewConfigurationId;
    }

//    public BeanCollection getBeanCollection() {
//        return beanCollection;
//    }

    public abstract void launch(Object key, String description, Class type);

    public Icon getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

//    public String getViewConfigurationId() {
//        return viewConfigurationId;
//    }

    public String toString() {
        return name;
    }

    /**
     * todo: for flexibility, maybe this should be passed a single parameter of type Compass.SearchResult
     * (of course in that case then "launch" should take the same param)
     * I guess bren was looking for interface simplicity. So I hope that simply adding the result key does not stray from
     * those objectives
     *
     * @param key
     * @param type
     * @return
     */
    public abstract boolean handlesResult(Object key, Class type);
}
