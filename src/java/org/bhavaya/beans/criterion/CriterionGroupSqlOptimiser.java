package org.bhavaya.beans.criterion;

import org.bhavaya.db.SQL;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Jun 7, 2006
 * Time: 10:38:07 AM
 * To change this template use File | Settings | File Templates.
 */
public interface CriterionGroupSqlOptimiser {
    SQL getOptimalSql(Class beanType, Criterion[] criteria);
}
