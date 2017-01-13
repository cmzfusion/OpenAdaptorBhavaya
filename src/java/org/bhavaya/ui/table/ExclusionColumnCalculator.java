package org.bhavaya.ui.table;

import org.bhavaya.util.PropertyGroup;
import org.bhavaya.util.Log;
import org.bhavaya.util.ApplicationProperties;

import javax.swing.table.TableModel;
import java.util.regex.Pattern;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 20-Aug-2009
 * Time: 17:16:12
 *
 * To solve the problem of me being lazy, this class makes it possible to specifiy regex
 * patterns to match column keys, rather than having to name every excluded column individually
 *
 * The processing is as follows:
 * - first assume all columns are included
 * - then apply the patterns in the patternGroups, in the order they are listed in the configuration
 * - finally, exclude any columns named individually using the legacy mechanism
 *
 * For each patternGroup, we first apply the exclude patterns, and then the include patterns
 * There are two types of pattern:
 *
 * - excludePattern:  exclude from the current set of included columns any columns matching the pattern
 * - includePattern:  add any excluded columns which match the pattern back into the set of included columns
 *
 * This might seem a bit long-winded, but the idea is that we can, for example:
 *
 * <PRE>
 * <propertyGroup key="sortingExclusion">
 *      <propertyGroup key="patternGroup">
 *          excludePattern instrument.inhousePrice.*
 *          includePattern instrument.inhousePrice.ticker*
 *      </propertyGroup>
 * </propertyGroup>
 * </PRE>
 *
 * In the example above, we start by assuming all cols are included.
 * Then the excludePattern is processed - so  we exclude all cols under inhousePrice. These are mostly
 * volatile price values.
 * Then we process the includePattern, and allow back in any cols starting inhousePrice.ticker*,
 * since the ticker values are text and do not change frequently
 */
public class ExclusionColumnCalculator {

    private static final boolean LOG_EXCLUDES_ON_FILTER = true; //change this to make it log more for debugging
    private static final Log log = Log.getCategory(ExclusionColumnCalculator.class);
    public static final String PATTERN_GROUP_PROPERTY_PATH = "patternGroup";
    public static final String EXCLUDE_PATTERN_PROPERTY_PATH = "excludePattern";
    public static final String INCLUDE_PATTERN_PROPERTY_PATH = "includePattern";
    public static final String LEGACY_EXCLUSION_PROPERTY_PATH = "exclusionColumn";

    //once we have applied the patterns to calculate the exclude for a column, store the results
    //for next time, to improve performance
    private static final Map<String, Boolean> cachedColumnResults = new HashMap<String, Boolean>();

    private Set<String> legacyExclusionColumns = new HashSet<String>();
    private List<ColumnPattern> columnPatterns = new ArrayList<ColumnPattern>();
    
    private static ExclusionColumnCalculator singletonInstance;

    private ExclusionColumnCalculator(PropertyGroup sortingExclusionPropertyGroup) {
        if ( sortingExclusionPropertyGroup != null) {
            addPatternGroups(sortingExclusionPropertyGroup);
            addLegacyColumns(sortingExclusionPropertyGroup);
        } else {
            log.info("No sorting exclusion columns found in configuration");
        }
    }

    public synchronized boolean isExcluded(String columnKey) {
        Boolean result = cachedColumnResults.get(columnKey);
        if ( result == null ) {
            result = legacyExclusionColumns.contains(columnKey) || applyPatterns(columnKey);
            cachedColumnResults.put(columnKey, result);
        }
        return result;
    }

    /**
     * Get the column names in this table model which should be excluded for filtering
     * The filter model uses the column name rather than key to identify the column to exclude
     * Since the filter model library does not know about bhavaya KeyedTableModel, there's no way to bind
     * rules to column keys.
     * 
     * This is not in general a problem (although where the path prefix is important we may end up excluding
     * extra columns, which is preferable to excluding too few)
    */
    public List<String> getColumnNamesToExclude(KeyedColumnTableModel tableModel) {
        Set<String> cols = new TreeSet<String>();
        for ( int col = 0; col < tableModel.getColumnCount(); col ++) {
            Object name = tableModel.getColumnKey(col);
            if ( name instanceof String && isExcluded((String)name) ) {
                cols.add(tableModel.getColumnName(col));
            }
        }
        logExcludesOnFilter();

        //just returning the result of Arrays.asList() returns a List instance
        //which doesn't support the optional addAll() operation (which we need)
        return new ArrayList<String>(Arrays.asList(cols.toArray(new String[cols.size()])));
    }

    //an hook to create an instance for testing
    static synchronized ExclusionColumnCalculator getInstance(PropertyGroup p) {
        return new ExclusionColumnCalculator(p);
    }

    public static synchronized ExclusionColumnCalculator getInstance() {
        if ( singletonInstance == null ) {
            singletonInstance = new ExclusionColumnCalculator(
                ApplicationProperties.getApplicationProperties().getGroup("sortingExclusion")
            );
        }
        return singletonInstance;
    }

    private void logExcludesOnFilter() {
        if ( LOG_EXCLUDES_ON_FILTER ) {
            for ( Map.Entry<String,Boolean> e : cachedColumnResults.entrySet()) {
                if ( e.getValue() ) {
                    log.info("Filtering " + e.getKey());
                }
            }
        }
    }

    private boolean applyPatterns(String columnKey) {
        boolean exclude = false; 
        for ( ColumnPattern p : columnPatterns) {
            if ( p.getPattern().matcher(columnKey).matches()) {
                exclude = (p.getType() == ColumnPatternType.EXCLUDE);
            }
        }
        return exclude;
    }

    private void addLegacyColumns(PropertyGroup sortingExclusionPropertyGroup) {
        String[] legacyExcludes = sortingExclusionPropertyGroup.getProperties(LEGACY_EXCLUSION_PROPERTY_PATH);
        legacyExclusionColumns.addAll(Arrays.asList(legacyExcludes));
    }

    private void addPatternGroups(PropertyGroup sortingExclusionPropertyGroup) {
        PropertyGroup[] groups = sortingExclusionPropertyGroup.getGroups(PATTERN_GROUP_PROPERTY_PATH);
        if ( groups != null ) {
            for (PropertyGroup g : groups) {
                processPatternGroup(g);
            }
        }
    }

    private void processPatternGroup(PropertyGroup g) {
        String[] exclusionProperties = g.getProperties(EXCLUDE_PATTERN_PROPERTY_PATH);
        addPatterns(exclusionProperties, ColumnPatternType.EXCLUDE);

        String[] inclusionProperties = g.getProperties(INCLUDE_PATTERN_PROPERTY_PATH);
        addPatterns(inclusionProperties, ColumnPatternType.INCLUDE);
    }

    private void addPatterns(String[] exclusionProperties, ColumnPatternType columnPatternType) {
        for ( String excludeProperty : exclusionProperties) {
            try {
                columnPatterns.add(new ColumnPattern(columnPatternType, Pattern.compile(excludeProperty)));
            } catch (Throwable t) {
                log.error("Cannot add sort exclusion column pattern " + excludeProperty + " this pattern will be skipped", t);
            }
        }
    }

    private class ColumnPattern {
        private ColumnPatternType type;
        private Pattern pattern;

        private ColumnPattern(ColumnPatternType type, Pattern pattern) {
            this.type = type;
            this.pattern = pattern;
        }

        public ColumnPatternType getType() {
            return type;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }

    private static enum ColumnPatternType {
        INCLUDE,
        EXCLUDE
    }
}
