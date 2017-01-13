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

import org.bhavaya.beans.criterion.*;
import org.bhavaya.util.DateFunction;
import org.bhavaya.util.SymbolicDateFunction;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class CriterionGroupDateTextField extends DateTextField implements CriteriaSelector {
    private CriterionGroup criterionGroup;
    private boolean nullable;
    private String criterionId;

    public CriterionGroupDateTextField(final String criterionGroupName, String criterionId) {
        this(DEFAULT_FROM_VALUE, false, criterionGroupName, criterionId);
    }

    public CriterionGroupDateTextField(DateFunction dateFunction, boolean nullable, final String criterionGroupName, String criterionId) {
        super(dateFunction);
        this.nullable = nullable;
        this.criterionId = criterionId;
        criterionGroup = new CriterionGroup(criterionGroupName, null);
        updateCriterionGroup();
    }

    protected String getTitle() {
        return criterionGroup.getName();
    }

    protected void update() {
        updateCriterionGroup();
        super.update();
    }

    private void updateCriterionGroup() {
        if (criterionGroup == null) {
            return;
        }


        Criterion[] criteria;
        if (dateFunction == SymbolicDateFunction.TIME_BEGIN) {
            // nothing
            criteria = null;
        } else if (dateFunction == SymbolicDateFunction.TIME_END) {
            // all
            criteria = Criterion.ALL_CRITERION;
        } else {
            if (nullable) {
                criteria = new Criterion[]{new OrCriterion(new BasicCriterion[]{
                    new BasicCriterion(criterionId, "=", dateFunction),
                    new FunctionCriterion(criterionId, "=", Boolean.TRUE, FunctionCriterion.IS_NULL),
                })};
            } else {
                criteria = new Criterion[]{new BasicCriterion(criterionId, "=", dateFunction)};
            }
        }
        criterionGroup.setCriteria(criteria);
    }

    public CriterionGroup getCriterionGroup() {
        return criterionGroup;
    }

    public void selectMatchingCriteria(CriterionGroup criterionGroup) {
        if (criterionGroup == null) {
            dateFunction = DEFAULT_FROM_VALUE;
        } else if (criterionGroup.getCriteria() == null) {
            // nothing
            dateFunction = SymbolicDateFunction.TIME_BEGIN;
        } else if (criterionGroup.getCriteria().length == 0) {
            // all
            dateFunction = SymbolicDateFunction.TIME_END;
        } else {
            BasicCriterion basicCriterion;

            Criterion criterion = criterionGroup.getCriteria()[0];
            if (criterion instanceof OrCriterion) {
                OrCriterion orCriterion = (OrCriterion) criterion;
                basicCriterion = orCriterion.getCriteria()[0];
            } else {
                basicCriterion = (BasicCriterion) criterion;
            }

            final Object rightOperand = basicCriterion.getRightOperand();
            dateFunction = (DateFunction) rightOperand;
        }
        update();
    }
}
