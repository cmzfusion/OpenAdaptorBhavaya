package org.bhavaya.util;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 11-May-2010
 * Time: 15:39:49
 *
 * Add the ability for SetStatements to match a bean path via a regular expression
 *
 * SetStatements which wish to use this feature should implement this interface instead of SetStatement
 * Old SetStatements should be unaffected
 */
public interface PatternMatchingSetStatement extends SetStatement {

    /**
     * @return a List of Pattern which can be used to determine whether a bean path is editable with this SetStatement
     */
    public List<java.util.regex.Pattern> getBeanPathPatterns();
}
