package org.bhavaya.ui;

import org.bhavaya.beans.criterion.CriterionGroup;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Apr-2008
 * Time: 11:26:10
 *
 * Interface to be implemented by components which allow the user to choose or edit the criteria in a CriterionGroup
 */
public interface CriteriaSelector
{
    CriterionGroup getCriterionGroup();

    void selectMatchingCriteria(CriterionGroup criterionGroup);
}
