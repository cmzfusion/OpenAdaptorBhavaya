package org.bhavaya.ui.compass;

import org.bhavaya.beans.Schema;
import org.bhavaya.collection.EfficientArrayList;
import org.bhavaya.db.DBUtilities;
import org.bhavaya.db.SQLFormatter;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * When browsing taxonomies, there is a natural type hierarchy.  TaxonomyStructure tables refer to themselves as
 * well as to ParentGroups.  Which in turn refer to themselves and to Instruments.  This class encapsulates that
 * relationship.
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class SqlTypeHierarchy extends CompassTypeHierarchy {
    public static class SqlQueryGroup {
        private String[] keyColumns;
        private String descriptionColumn;
        private String childrenSql;
        private String findParentSql;

        public SqlQueryGroup(String keyColumn, String descriptionColumn, String childrenSql, String findParentSql) {
//            this(new String[]{keyColumn},descriptionColumn, childrenSql, findParentSql);
            this.keyColumns = new String[]{keyColumn};
            this.descriptionColumn = descriptionColumn;
            this.childrenSql = ApplicationProperties.substituteApplicationProperties(childrenSql);
            this.findParentSql = ApplicationProperties.substituteApplicationProperties(findParentSql);
        }

//todo:   getChildrenOfThisType etc need to be changed to support composite keys before this can be uncommented
//        public SqlQueryGroup(String[] keyColumns, String descriptionColumn, String childrenSql, String findParentSql) {
//            this.keyColumns = keyColumns;
//            this.descriptionColumn = descriptionColumn;
//            this.childrenSql = ApplicationProperties.substituteApplicationProperties(childrenSql);
//            this.findParentSql = ApplicationProperties.substituteApplicationProperties(findParentSql);
//        }
    }

    public static final SqlQueryGroup NULL_QUERY_GROUP = new SqlQueryGroup((String) null, null, null, null);

    private SqlQueryGroup thisTypeQueryGroup;
    private SqlQueryGroup nextTypeQueryGroup;
    private String findMatchesForStringSql;
    private String defaultDataSourceName;


    public SqlTypeHierarchy(String name, Class type, SqlQueryGroup thisTypeQueryGroup, SqlQueryGroup nextTypeQueryGroup,
                            String findMatchesForStringSql) {
        super(name, type);
        this.thisTypeQueryGroup = thisTypeQueryGroup;
        this.nextTypeQueryGroup = nextTypeQueryGroup;
        this.findMatchesForStringSql = ApplicationProperties.substituteApplicationProperties(findMatchesForStringSql);
        defaultDataSourceName = Schema.getInstance(type).getDefaultDataSourceName();
    }

    public List getChildrenOfThisType(Object key) {
        if (thisTypeQueryGroup.childrenSql == null) return Collections.EMPTY_LIST;
        String queryWithKeySubstitution = SQLFormatter.getInstance(defaultDataSourceName).replace(thisTypeQueryGroup.childrenSql, key, true);
        return convertToResultCollection(DBUtilities.execute(defaultDataSourceName, queryWithKeySubstitution), thisTypeQueryGroup);
    }

    public List getChildrenOfNextType(Object key) {
        if (nextTypeQueryGroup.childrenSql == null) return Collections.EMPTY_LIST;
        String queryWithKeySubstitution = SQLFormatter.getInstance(defaultDataSourceName).replace(nextTypeQueryGroup.childrenSql, key, true);
        return convertToResultCollection(DBUtilities.execute(defaultDataSourceName, queryWithKeySubstitution), nextTypeQueryGroup);
    }

    public List findMatchesForString(String searchString) {
        String queryWithKeySubstitution = SQLFormatter.getInstance(defaultDataSourceName).replace(findMatchesForStringSql, "%" + searchString + "%", true);
        return convertToResultCollection(DBUtilities.execute(defaultDataSourceName, queryWithKeySubstitution), thisTypeQueryGroup);
    }

    public Object findParentKeyOfThisType(Object key) {
        if (thisTypeQueryGroup.findParentSql == null) return null;
        String queryWithKeySubstitution = SQLFormatter.getInstance(defaultDataSourceName).replace(thisTypeQueryGroup.findParentSql, key, true);
        List resultList = DBUtilities.execute(defaultDataSourceName, queryWithKeySubstitution);
        return resultList.size() == 1 ? getKey(resultList.get(0), thisTypeQueryGroup) : null;
    }

    public Object findParentKeyOfNextType(Object key) {
        if (nextTypeQueryGroup.findParentSql == null) return null;
        String queryWithKeySubstitution = SQLFormatter.getInstance(defaultDataSourceName).replace(nextTypeQueryGroup.findParentSql, key, true);
        List resultList = DBUtilities.execute(defaultDataSourceName, queryWithKeySubstitution);
        return resultList.size() == 1 ? getKey(resultList.get(0), thisTypeQueryGroup) : null;
    }

    public Boolean getNodeIsLeafOverride() {
        return new Boolean(thisTypeQueryGroup.childrenSql == null && nextTypeQueryGroup.childrenSql == null);
    }

    private List convertToResultCollection(List list, SqlQueryGroup sqlQueryGroup) {
        List resultCollection = new ArrayList();
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            resultCollection.add(new Result(getKey(o, sqlQueryGroup), (String) Generic.get(o, sqlQueryGroup.descriptionColumn)));
        }
        return resultCollection;
    }

    private Object getKey(Object o, SqlQueryGroup sqlQueryGroup) {
        String[] keyColumns = sqlQueryGroup.keyColumns;
        if (keyColumns.length == 1) {
            return Generic.get(o, keyColumns[0]);
        } else {
            EfficientArrayList compositeKey = new EfficientArrayList(keyColumns.length);
            for (int i = 0; i < keyColumns.length; i++) {
                String keyColumn = keyColumns[i];
                compositeKey.add(Generic.get(o, keyColumn));
            }
            return compositeKey;
        }
    }
}
