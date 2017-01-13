/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.beans.criterion;

import org.bhavaya.beans.Schema;
import org.bhavaya.db.SQL;
import org.bhavaya.util.*;

/**
 * @author Brendon McLean
 * @version $Revision: 1.11 $
 */

public class CriterionGroup extends DefaultObservable implements Filter, Describeable, Criterion, Cloneable {
    private static final Log log = Log.getCategory(CriterionGroup.class);

    public static final CriterionGroup ALL_CRITERION_GROUP = new CriterionGroup("All", Criterion.ALL_CRITERION);

    static {
        BeanUtilities.addPersistenceDelegate(CriterionGroup.class, new BhavayaPersistenceDelegate(new String[]{"name", "criteria"}));
    }

    private String name;
    protected Criterion[] criteria;
    private SQL selectStatement = null;
    private CriterionGroupSqlOptimiser criterionGroupSqlOptimiser;

    /**
     * a criterion group with "null" criteria will reject all beans
     * a criterion group with an empty criteria array will accept all beans
     *
     * @param criteria
     */
    public CriterionGroup(String name, Criterion... criteria) {
        assert name != null;
        this.name = name;
        this.criteria = criteria;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    public void setCriterionGroupSQLOptimiser(CriterionGroupSqlOptimiser criterionGroupSqlOptimiser) {
        if (this.criterionGroupSqlOptimiser != criterionGroupSqlOptimiser) {
            this.criterionGroupSqlOptimiser = criterionGroupSqlOptimiser;
            this.selectStatement = null;
        }
    }

    public String getDescription() {
        if (criteria == null) return name + ": None";
        if (criteria.length == 0) return name + ": All";
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < criteria.length; i++) {
            if (i > 0) buffer.append("\n");
            Criterion criterion = criteria[i];
            buffer.append(name).append(": ").append(criterion.getDescription());
        }
        return buffer.toString();
    }

    public Criterion[] getCriteria() {
        if (criteria == null) return null;
        Criterion[] criteriaCopy = new Criterion[criteria.length];
        System.arraycopy(criteria, 0, criteriaCopy, 0, criteria.length);
        return criteriaCopy;
    }

    /**
     * a criterion group with "null" criteria will reject all beans
     * a criterion group with an empty criteria array will accept all beans
     *
     * @param criteria
     */
    public void setCriteria(Criterion[] criteria) {
        Criterion[] oldValue = this.criteria;
        this.criteria = criteria;

        this.selectStatement = null;

        firePropertyChange("criteria", oldValue, criteria);
    }

    /**
     * Evaluates from left to right, aborting as soon as a failed criterion is met,
     * i.e. perform a lazy AND operation.
     */
    public boolean evaluate(Object bean) {
        if (log.isDebug()) log.debug("CriterionGroup: " + getName() + " is evaluating bean");

        if (criteria == null) return false;
        if (criteria.length == 0) return true;

        for (int i = 0; i < criteria.length; i++) {
            Criterion criterion = criteria[i];
            boolean result = criterion.evaluate(bean);
            if (!result) {
                if (log.isDebug()) log.debug("Failed test: " + criterion.getName());
                return false;
            }
        }

        if (log.isDebug()) log.debug("Passed all " + criteria.length + " tests");
        return true;
    }


    public SQL getSQL(Class beanType) {
        if (selectStatement == null) {
            SQL beanSQL = Schema.getInstance(beanType).getSql();

            if (criteria == null) {
                selectStatement = beanSQL.joinWhereClause("1=2");
            } else if (criteria.length == 0) {
                selectStatement = beanSQL;
            } else if (criterionGroupSqlOptimiser != null) {
                selectStatement = criterionGroupSqlOptimiser.getOptimalSql(beanType, criteria);
            } else {
                selectStatement = beanSQL;

                for (int i = 0; i < criteria.length; i++) {
                    if (criteria[i] instanceof SqlCriterion) {
                        SqlCriterion criterion = (SqlCriterion) criteria[i];
                        selectStatement = criterion.joinSql(beanType, selectStatement);
                    }
                }
            }
        }
        return selectStatement;
    }

    public static CriterionGroup mergeCriterionGroups(CriterionGroup firstGroup, CriterionGroup secondGroup) {
        CriterionGroup newGroup;

        if (firstGroup != null && secondGroup != null) {
            Criterion[] mergedCriteria = Utilities.appendArrays(firstGroup.getCriteria(), secondGroup.getCriteria());
            String newName = firstGroup.getName() + "+" + secondGroup.getName();
            newGroup = new CriterionGroup(newName, mergedCriteria);
        } else if (firstGroup == null && secondGroup == null) {
            newGroup = null;
        } else if (firstGroup != null) {
            newGroup = firstGroup;
        } else {
            newGroup = secondGroup;
        }
        return newGroup;
    }

    public String toString() {
        return name;
    }

    public boolean isValidForBeanType(Class beanType) {
        boolean applicable = true;
        for (int j = 0; j < criteria.length && applicable; j++) {
            Criterion criterion = criteria[j];
            if (!criterion.isValidForBeanType(beanType)) applicable = false;
        }
        return applicable;
    }

    public Object clone() {
        return new CriterionGroup(name, criteria);
    }
}
