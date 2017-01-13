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

package org.bhavaya.ui;

import org.bhavaya.beans.criterion.BasicCriterion;
import org.bhavaya.beans.criterion.Criterion;
import org.bhavaya.beans.criterion.CriterionGroup;
import org.bhavaya.util.DateFunction;
import org.bhavaya.util.DateFunctionInterval;
import org.bhavaya.util.RelativeDateFunction;
import org.bhavaya.util.SymbolicDateFunction;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.4 $
 */
public class CriterionGroupDateIntervalTextField extends DateIntervalTextField implements CriteriaSelector {
    private CriterionGroup criterionGroup;
    private String criterionId;

    public CriterionGroupDateIntervalTextField(final String criterionGroupName, String criterionId) {
        this(new DateFunctionInterval(DEFAULT_FROM_VALUE, DEFAULT_TO_VALUE), criterionGroupName, criterionId);
    }

    public CriterionGroupDateIntervalTextField(DateFunctionInterval interval, final String criterionGroupName, String criterionId) {
        super(interval);
        this.criterionId = criterionId;
        criterionGroup = createCriterionGroup(criterionGroupName, null);
        updateCriterionGroup();
    }

    public CriterionGroupDateIntervalTextField(CriterionGroup criterionGroup, DateFunctionInterval interval, String criterionId) {
        super(interval);
        this.criterionId = criterionId;
        this.criterionGroup = criterionGroup;
        updateCriterionGroup();
    }

    protected CriterionGroup createCriterionGroup(String criterionGroupName, Criterion[] criteria) {
        return new CriterionGroup(criterionGroupName, criteria);
    }

    public String getCriterionId() {
        return criterionId;
    }

    protected BasicCriterion createBasicCriterion(String criterionId, String operator, DateFunction startDateFunction) {
        return new BasicCriterion(criterionId, operator, startDateFunction);
    }

    protected String getTitle() {
        return criterionGroup.getName();
    }

    protected void update() {
        updateCriterionGroup();
        super.update();
    }

    protected void updateCriterionGroup() {
        if (criterionGroup == null) {
            return;
        }

        Criterion[] criteria;
        if (SymbolicDateFunction.TIME_BEGIN.equals(interval.getStartDateFunction()) && SymbolicDateFunction.TIME_BEGIN.equals(interval.getEndDateFunction())) {
            criteria = null; // nothing
        } else if (SymbolicDateFunction.TIME_BEGIN.equals(interval.getStartDateFunction()) && SymbolicDateFunction.TIME_END.equals(interval.getEndDateFunction())) {
            criteria = Criterion.ALL_CRITERION; //all
        } else if (SymbolicDateFunction.TIME_BEGIN.equals(interval.getStartDateFunction())) {
            criteria = new Criterion[]{createBasicCriterion(criterionId, "<", new RelativeDateFunction(interval.getEndDateFunction(), RelativeDateFunction.OFFSET_TYPE_DAYS, RelativeDateFunction.PREPOSITION_AFTER, 1))};
        } else if (SymbolicDateFunction.TIME_END.equals(interval.getEndDateFunction())) {
            criteria = new Criterion[]{createBasicCriterion(criterionId, ">=", interval.getStartDateFunction())};
        } else {
            criteria = new Criterion[]{
                    createBasicCriterion(criterionId, ">=", interval.getStartDateFunction()),
                    createBasicCriterion(criterionId, "<", new RelativeDateFunction(interval.getEndDateFunction(), RelativeDateFunction.OFFSET_TYPE_DAYS, RelativeDateFunction.PREPOSITION_AFTER, 1))
            };
        }
        criterionGroup.setCriteria(criteria);
    }

    public CriterionGroup getCriterionGroup() {
        return criterionGroup;
    }

    public void selectMatchingCriteria(CriterionGroup criterionGroup) {
        if (criterionGroup == null) {
            interval = new DateFunctionInterval(DEFAULT_FROM_VALUE, DEFAULT_TO_VALUE);
        } else if (criterionGroup.getCriteria() == null) {
            // nothing
            interval = new DateFunctionInterval(SymbolicDateFunction.TIME_BEGIN, SymbolicDateFunction.TIME_BEGIN);
        } else if (criterionGroup.getCriteria().length == 0) {
            // all
            interval = new DateFunctionInterval(SymbolicDateFunction.TIME_BEGIN, SymbolicDateFunction.TIME_END);
        } else if (criterionGroup.getCriteria().length == 1) {
            BasicCriterion criterion = (BasicCriterion) criterionGroup.getCriteria()[0];
            DateFunction start;
            DateFunction end;

            if (criterion.getOperator().equals("<")) {
                start = SymbolicDateFunction.TIME_BEGIN;
                end = ((RelativeDateFunction) criterion.getRightOperand()).getReferenceDate(); // see updateCriterionGroup where the end has 1 day added to it
            } else { // >=
                start = (DateFunction) criterion.getRightOperand();
                end = SymbolicDateFunction.TIME_END;
            }
            interval = new DateFunctionInterval(start, end);
        } else {
            BasicCriterion startCriterion = (BasicCriterion) criterionGroup.getCriteria()[0];
            BasicCriterion endCriterion = (BasicCriterion) criterionGroup.getCriteria()[1];
            DateFunction start = (DateFunction) startCriterion.getRightOperand();
            DateFunction end = ((RelativeDateFunction) endCriterion.getRightOperand()).getReferenceDate(); // see updateCriterionGroup where the end has 1 day added to it
            interval = new DateFunctionInterval(start, end);
        }
        update();
    }
}
