package org.bhavaya.beans;

import org.bhavaya.beans.criterion.CriterionGroup;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 15-Aug-2008
 * Time: 16:31:34
 *
 * Classes may reference a CriteriaBeanSource rather than a BeanFactory directly - to allow a mock implementation to
 * be injected for unit testing.
 */
public interface CriterionBeanSource<K,V> extends Map<K,V> {

    public abstract V[] getObjects(CriterionGroup criterionGroup);
}
