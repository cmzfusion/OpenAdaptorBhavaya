package org.bhavaya.ui.compass;

import org.bhavaya.beans.Schema;
import org.bhavaya.beans.criterion.*;
import org.bhavaya.collection.BeanCollectionGroup;
import org.bhavaya.util.Filter;
import org.bhavaya.util.Log;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.2 $
 */
public class CriterionCompassLaunchTarget extends CompassLaunchTarget {
    private static final Log log = Log.getCategory(CriterionCompassLaunchTarget.class);

    private BeanCollectionGroup beanCollectionGroup;
    private Criterion[] extraCriterion;
    private Map typeToGroupMap;
    private String viewConfig;

    public CriterionCompassLaunchTarget(Icon icon, String name, BeanCollectionGroup beanCollectionGroup,
                                        Criterion[] extraCriterion, String viewConfig) {
        super(icon, name);
        this.beanCollectionGroup = beanCollectionGroup;
        this.viewConfig = viewConfig;
        this.extraCriterion = extraCriterion != null ? extraCriterion : new Criterion[0];
        this.typeToGroupMap = createTypeToGroupMap(beanCollectionGroup.getBeanType());
    }

    private Map createTypeToGroupMap(Class type) {
        BeanPathTransformerGroup[] groups = BeanPathTransformerGroup.getInstances("dataset", type);
        Map joinTypes = new HashMap();
        for (int i = 0; i < groups.length; i++) {
            BeanPathTransformerGroup group = groups[i];
            Class joinClass = group.getToBeanType();
            if (Schema.hasInstance(joinClass)) {
                joinTypes.put(joinClass, group);
            }
        }
        return joinTypes;
    }

    public boolean handlesResult(Object key, Class type) {
        return typeToGroupMap.get(type) != null;
    }

    public void launch(Object key, String description, Class type) {
        Criterion[] extraCriterion = filterExtraCriterionForType(beanCollectionGroup.getBeanType());

        BeanPathTransformerGroup transformerGroup = (BeanPathTransformerGroup) typeToGroupMap.get(type);
        Criterion joinCriterion = createCriterion(transformerGroup, key, description);

        Criterion[] mergedCriterion = Utilities.unionArrays(new Criterion[]{joinCriterion}, extraCriterion);
        CriterionGroup criterionGroup = new CriterionGroup(description, mergedCriterion);

        beanCollectionGroup.viewBeanCollectionAsTable(description, description, description,
                beanCollectionGroup.newBeanCollection(criterionGroup), viewConfig);
    }

    private Criterion[] filterExtraCriterionForType(final Class type) {
        return (Criterion[]) Utilities.filterArray(extraCriterion, new Filter() {
            public boolean evaluate(Object obj) {
                Criterion criterion = (Criterion) obj;
                return criterion.isValidForBeanType(type);
            }
        });
    }

    protected Criterion createCriterion(BeanPathTransformerGroup transformerGroup, Object key, String description) {
        String type = transformerGroup.getCriterionType();
        String id = transformerGroup.getId();
        BasicCriterion criterion = null;
        if (type.equals(BasicCriterion.BASIC)) {
            criterion = new BasicCriterion(id, "=", key);
        } else if (type.equals(BasicCriterion.FUNCTION)) {
            criterion = new FunctionCriterion(id, "=", key);
        } else if (type.equals(BasicCriterion.ENUMERATION)) {
            EnumerationCriterion.EnumElement enumElement = new EnumerationCriterion.EnumElement(key, description);
            criterion = new EnumerationCriterion(id, new EnumerationCriterion.EnumElement[]{enumElement});
        } else if (type.equals(BasicCriterion.TREE)) {
            TreeCriterion.SelectableEnumElement selectableEnumElement = new TreeCriterion.SelectableEnumElement(key, description, true);
            criterion = new TreeCriterion(id, new TreeCriterion.SelectableEnumElement[]{selectableEnumElement});
        } else if (type.equals(BasicCriterion.SUBTREE)) {
            criterion = new SubtreeCriterion(id, key);
        } else {
            throw new RuntimeException("Cannot create launch criterion for transformerGroup with id: " + id);
        }
        return criterion;
    }
}
