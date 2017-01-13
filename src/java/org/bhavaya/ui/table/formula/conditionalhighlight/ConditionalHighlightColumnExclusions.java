package org.bhavaya.ui.table.formula.conditionalhighlight;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Set of columns that should be excluded from conditional highlighting.
 * User: ga2mhana
 * Date: 18/03/11
 * Time: 15:37
 */
public class ConditionalHighlightColumnExclusions {
    private List<Pattern> exclusionPatterns = new ArrayList<Pattern>();
    private boolean excludeAll;

    public ConditionalHighlightColumnExclusions() { }

    public void addExclusionPatterns(String... excludedColumnPatterns) {
        for(String excludedColumnPattern : excludedColumnPatterns) {
            exclusionPatterns.add(Pattern.compile(excludedColumnPattern));
        }
    }

    public boolean isExcluded(String columnId) {
        return excludeAll || matchesExclusionPatterns(columnId);
    }

    public void setExcludeAllEnabled(boolean excludeAll) {
        this.excludeAll = excludeAll;
    }

    private boolean matchesExclusionPatterns(String beanPath) {
        for (Pattern pattern : exclusionPatterns) {
            if ( pattern.matcher(beanPath).matches() ) {
                return true;
            }
        }
        return false;
    }
}
