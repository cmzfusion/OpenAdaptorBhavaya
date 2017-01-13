package org.bhavaya.ui.table;

import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;

import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;

/**
 * a column config stores information about options that may be used to configure a column
 */
public class ColumnConfig {

    private static final Log log = Log.getCategory(ColumnConfig.class);

    private TableCellRendererFactory rendererFactory;
    private Class[] bucketTypes;
    private Class defaultBucketType;
    private Class[] cellValueTransforms;

    public ColumnConfig(TableCellRendererFactory rendererFactory, String[] bucketTypeNames, String defaultBucketTypeName, String[] transformNames) {

        Class[] buckets = null;
        if (bucketTypeNames != null) {
            ArrayList bucketClasses = new ArrayList();
            for (int i = 0; i < bucketTypeNames.length; i++) {
                String bucketTypeName = bucketTypeNames[i];
                try {
                    bucketClasses.add(ClassUtilities.getClass(bucketTypeName));
                } catch (Exception e) {
                    log.error("Could not get bucket class: " + bucketTypeName, e);
                }
            }
            buckets = (Class[]) bucketClasses.toArray(new Class[bucketClasses.size()]);
        }

        Class defaultBucket = null;
        if (defaultBucketTypeName != null) {
            try {
                defaultBucket = ClassUtilities.getClass(defaultBucketTypeName);
            } catch (Exception e) {
                log.error("Could not get default bucket class: " + defaultBucketTypeName, e);
            }
        }

        Class[] transforms = null;
        if (transformNames != null) {
            ArrayList classes = new ArrayList();
            for (int i = 0; i < transformNames.length; i++) {
                String className = transformNames[i];
                try {
                    classes.add(ClassUtilities.getClass(className));
                } catch (Exception e) {
                    log.error("Could not get transform class: " + className, e);
                }
            }
            transforms = (Class[]) classes.toArray(new Class[classes.size()]);
        }

        setProperties(rendererFactory, buckets, defaultBucket, transforms);
    }

    public ColumnConfig(TableCellRenderer renderer, Class[] bucketTypes, Class defaultBucketType) {
        this(new SimpleTableCellRendererFactory(renderer), bucketTypes, defaultBucketType, null);
    }

    public ColumnConfig(TableCellRendererFactory rendererFactory, Class[] bucketTypes, Class defaultBucketType, Class[] cellValueTransforms) {
        setProperties(rendererFactory, bucketTypes, defaultBucketType, cellValueTransforms);
    }

    protected void setProperties(TableCellRendererFactory rendererFactory, Class[] bucketTypes, Class defaultBucketType, Class[] cellValueTransforms) {
        this.rendererFactory = rendererFactory;
        this.bucketTypes = bucketTypes;
        this.defaultBucketType = defaultBucketType;
        this.cellValueTransforms = cellValueTransforms;
    }

    public TableCellRendererFactory getRendererFactory() {
        return rendererFactory;
    }

    public TableCellRenderer getRendererForId(String rendererId) {
        TableCellRendererFactory rendererFactory = getRendererFactory();
        if (rendererFactory == null) return null;
        return rendererFactory.getInstance(rendererId);
    }

    public Class[] getBucketTypes() {
        return bucketTypes;
    }

    public Class getDefaultBucketType() {
        return defaultBucketType;
    }

    public Class[] getCellValueTransforms() {
        return cellValueTransforms;
    }
}
