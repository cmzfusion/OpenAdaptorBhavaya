package org.bhavaya.beans;

/**
 * Currently this is used by the BeanFactory to ensure that if a reference to a bean exists,
 * then any indexed collection it is in is not garbage collected.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public interface Indexable {
    public void addIndexedValue(Object index, Object object);

    public Object getIndexedValue(Object index);
}
