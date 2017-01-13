package org.bhavaya.ui.view;

import org.bhavaya.collection.EfficientArrayList;
import org.bhavaya.ui.diagnostics.ApplicationDiagnostics;
import org.bhavaya.ui.table.TableCellRendererFactory;
import org.bhavaya.ui.table.ColumnConfig;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * builds column configs from application.xml
 *
 * @author Daniel van Enckevort
 * @version $Revision: 1.6 $
 */
public class ApplicationColumnConfigMap {
    private final static Log log = Log.getCategory(ApplicationColumnConfigMap.class);

    private HashMap rendererFactories = new HashMap();
    private HashMap columnConfigs = new HashMap();

    public ApplicationColumnConfigMap() {
        // build all renderer factories by ID
        inflateRendererFactories();

        // Load up type specific column config groups
        PropertyGroup typeSpecificPropertyGroup = ApplicationProperties.getApplicationProperties().getGroup("columnConfigRules.typeSpecific");
        PropertyGroup[] typeSpecificPropertySubGroups = typeSpecificPropertyGroup.getGroups();
        for (int i = 0; i < typeSpecificPropertySubGroups.length; i++) {
            try {
                String className = typeSpecificPropertySubGroups[i].getName();
                String beanPath = typeSpecificPropertySubGroups[i].getProperty("beanPath");
                assert className != null;
                ColumnConfig columnConfig = createColumnConfig(typeSpecificPropertySubGroups[i]);
                Object key = getKey(className, beanPath);
                columnConfigs.put(key, columnConfig);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    private Object getKey(String className, String beanPath) {
        return new EfficientArrayList(new Object[]{className, beanPath != null ? beanPath : ""});
    }

    public ColumnConfig getColumnConfig(String className, String beanPath) {
        Object key = getKey(className, beanPath);
        return (ColumnConfig) columnConfigs.get(key);
    }

    private ColumnConfig createColumnConfig(PropertyGroup columnConfigProperties) {
        String rendererFactoryName = columnConfigProperties.getProperty("rendererFactory");

        TableCellRendererFactory rendererFactory = getRendererFactory(rendererFactoryName);
        return new ColumnConfig(rendererFactory,
                columnConfigProperties.getProperties("bucketTypes"),
                columnConfigProperties.getProperty("defaultBucket"),
                columnConfigProperties.getProperties("cellValueTransforms")
                );
    }

    public TableCellRendererFactory getRendererFactory(String factoryName) {
        if (factoryName == null) return null;   // null is ok, but asking for a non-existant factory should be flagged as
                                                // it means we have made a mistake in our config files
        TableCellRendererFactory rendererFactory = (TableCellRendererFactory) rendererFactories.get(factoryName);
        ApplicationDiagnostics.getInstance().productionAssert(rendererFactory != null, "Could not find renderer factory for id: "+factoryName);
        return rendererFactory;
    }

    private void inflateRendererFactories() {
        PropertyGroup precisionRenderersGroup = ApplicationProperties.getApplicationProperties().getGroup("columnRendererFactories");
        if (precisionRenderersGroup != null) {
            PropertyGroup[] factories = precisionRenderersGroup.getGroups();
            for (int i = 0; i < factories.length; i++) {
                try {
                    PropertyGroup factoryAttributes = factories[i];
                    String factoryClassString = factoryAttributes.getProperty("factoryClass");
                    String[] rendererConstructorArgumentValueStrings = factoryAttributes.getProperties("factoryConstructorArguments");
                    TableCellRendererFactory rendererFactory = createRendererFactory(factoryClassString, rendererConstructorArgumentValueStrings);
                    rendererFactories.put(factoryAttributes.getName(), rendererFactory);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

    private TableCellRendererFactory createRendererFactory(String factoryClassString, String[] constructorArguments) throws Exception {
        Class renderClass = ClassUtilities.getClass(factoryClassString);
        Class[] constructorArgumentTypes = new Class[]{String[].class};
        Constructor constructor;

        Object[] params;
        //try String[] constructor, then null constructor
        try {
            constructor = renderClass.getConstructor(constructorArgumentTypes);
            params = new Object[] {constructorArguments};
        } catch (NoSuchMethodException e) {
            constructor = renderClass.getConstructor(new Class[]{});
            params = new Object[0];
        }
        return (TableCellRendererFactory) constructor.newInstance(params );
    }

}
