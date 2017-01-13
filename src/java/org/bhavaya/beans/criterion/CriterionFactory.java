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

import org.bhavaya.collection.IndexedSet;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;
import org.bhavaya.util.Utilities;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */

public class CriterionFactory {
    private static final Log log = Log.getCategory(CriterionFactory.class);
    private static final String TYPE_PROPERTY = "type";
    private static final String CRITERION_FILE_SYSTEM_PROPERTY = "CRITERION_PATH";
    private static Map compulsoryCriteriaByClass = new HashMap();
    private static Map defaultCriteriaByClass = new HashMap();
    private static String[] criterionPath = null;

    private static String[] getCriterionPath() {
        if(criterionPath == null) {
            String criterionPathString = System.getProperty(CRITERION_FILE_SYSTEM_PROPERTY);
            if (criterionPathString == null || criterionPathString.length() == 0) {
                criterionPathString = Criterion.CRITERION_XML_FILE;
            }
            criterionPath = Utilities.appendArrays(criterionPathString.split(","), ApplicationProperties.getEnvironmentPath());
        }
        return criterionPath;
    }

    public static PropertyGroup getCriterionPropertyGroup() {
        return ApplicationProperties.getInstance(getCriterionPath());
    }

    /**
     * Returns a TreeModel representing Criterion with key of <code>key</code> in criterion.xml.
     * Only Criterion that can operate on instances of beanType are added to the TreeModel.  If beanType is null
     * then all Criterion with key of <code>key</code> are added.
     *
     * @param key
     * @param rootName
     * @param beanType
     * @return
     */
    public static TreeModel getCriterionTree(String key, String rootName, Class beanType) {
        PropertyGroup root = getCriterionPropertyGroup().getGroup(key);
        DefaultTreeModel treeModel = new DefaultTreeModel(createTree(root, key, rootName, beanType));

        return treeModel;
    }

    private static MutableTreeNode createTree(PropertyGroup node, String key, String rootName, Class beanType) {
        DefaultMutableTreeNode rootNode = new CriterionTreeNode(rootName);
        Map criterionByName = new TreeMap(); // we are going to sort them by name
        PropertyGroup[] properties = node.getGroups();

        for (int i = 0; i < properties.length; i++) {
            PropertyGroup criterionPropertyGroup = properties[i];
            String type = criterionPropertyGroup.getProperty(TYPE_PROPERTY);
            String id = key + "." + criterionPropertyGroup.getName();

            assert (type != null) : "type null for: " + id;

            Criterion criterion = newCriterion(type, id, null, null);
            if (beanType == null || criterion.isValidForBeanType(beanType)) {
                criterionByName.put(criterion.getName(), criterion);
            }
        }

        for (Iterator iterator = criterionByName.values().iterator(); iterator.hasNext();) {
            Criterion criterion = (Criterion) iterator.next();
            DefaultMutableTreeNode criterionNode = new DefaultMutableTreeNode(criterion.getName());
            criterionNode.setUserObject(criterion);
            rootNode.add(criterionNode);
        }

        return rootNode;
    }

    public static SqlCriterion newCriterion(String type, String id, String operator, Object rightOperand) {
        SqlCriterion criterion = null;
        if (type.equals(BasicCriterion.BASIC)) {
            criterion = new BasicCriterion(id, operator, rightOperand);
        } else if (type.equals(BasicCriterion.FUNCTION)) {
            criterion = new FunctionCriterion(id, operator, rightOperand);
        } else if (type.equals(BasicCriterion.ENUMERATION)) {
            criterion = new EnumerationCriterion(id, operator, (EnumerationCriterion.EnumElement[]) rightOperand);
        } else if (type.equals(BasicCriterion.TREE)) {
            criterion = new TreeCriterion(id, (TreeCriterion.SelectableEnumElement[]) rightOperand);
        } else if (type.equals(BasicCriterion.SUBTREE)) {
            criterion = new SubtreeCriterion(id, rightOperand);
        } else if (type.equals(BasicCriterion.LIST)) {
            if (rightOperand == null) {
                criterion = new OrCriterion(new BasicCriterion[]{new BasicCriterion(id, operator, rightOperand)});
            } else {
                criterion = new OrCriterion((BasicCriterion[]) rightOperand);
            }
        } else {
            log.error("Invalid criterion type: " + type);
        }
        return criterion;
    }

    public static void addDefaultCriterion(Class beanType, Criterion criterion) {
        IndexedSet defaultCriterion = getDefaultCriterion(beanType);
        defaultCriterion.add(criterion);
    }

    public static void removeDefaultCriterion(Class beanType, Criterion criterion) {
        IndexedSet defaultCriterion = getDefaultCriterion(beanType);
        defaultCriterion.remove(criterion);
    }

    public static IndexedSet getDefaultCriterion(Class beanType) {
        IndexedSet defaultCriterion = (IndexedSet) defaultCriteriaByClass.get(beanType);
        if (defaultCriterion == null) {
            defaultCriterion = new IndexedSet();
            defaultCriteriaByClass.put(beanType, defaultCriterion);
        }
        return defaultCriterion;
    }

    public static void addCompulsoryCriterion(Class beanType, Criterion criterion) {
        // DON'T use CriterionGroup as a second parameter as it doesn't work reliably 
        IndexedSet compulsoryCriterion = getCompulsoryCriterion(beanType);
        compulsoryCriterion.add(criterion);
    }

    public static void removeCompulsoryCriterion(Class beanType, Criterion criterion) {
        IndexedSet compulsoryCriterion = getCompulsoryCriterion(beanType);
        compulsoryCriterion.remove(criterion);
    }

    public static IndexedSet getCompulsoryCriterion(Class beanType) {
        IndexedSet compulsoryCriterion = (IndexedSet) compulsoryCriteriaByClass.get(beanType);
        if (compulsoryCriterion == null) {
            compulsoryCriterion = new IndexedSet();
            compulsoryCriteriaByClass.put(beanType, compulsoryCriterion);
        }
        return compulsoryCriterion;
    }

    public static boolean isCompulsoryCriterion(Class beanType, Criterion criterion) {
        IndexedSet compulsoryCriterion = getCompulsoryCriterion(beanType);
        if (compulsoryCriterion == null) return false;
        return compulsoryCriterion.contains(criterion);
    }

    public static class CriterionTreeNode extends DefaultMutableTreeNode {
        private String nodeName;

        public CriterionTreeNode(String nodeName) {
            this.nodeName = nodeName;
        }

        public String toString() {
            return nodeName;
        }
    }
}
