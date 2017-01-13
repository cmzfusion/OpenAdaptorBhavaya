package org.bhavaya.ui.view;

import org.bhavaya.collection.BeanCollection;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 02-Apr-2008
 * Time: 14:49:06
 *
 * Interface to be implemented by classes which have logic to restrict the data in a bean collection
 * (e.g. by adding extra criteria to a criteria bean collection)
 */
public interface CollectionRestrictor {

    void restrictCollection(BeanCollection parentBeanCollection);
}
