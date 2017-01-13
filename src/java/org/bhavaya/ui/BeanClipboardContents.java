package org.bhavaya.ui;

import org.bhavaya.util.Log;

import java.awt.datatransfer.DataFlavor;

/**
* Created by IntelliJ IDEA.
* User: ebbuttn
* Date: 24-Jan-2008
* Time: 17:25:27
* To change this template use File | Settings | File Templates.
*
* Stores an array of beans representing data on the clipboard during a drag and drop or copy/cut/paste operation
*/
public class BeanClipboardContents
{
    private static final Log log = Log.getCategory(BeanClipboardContents.class);
    private Object[] beanData;
    private boolean isDragToDeleteSupported;
    private DataFlavor[] dataFlavors = new DataFlavor[0];

    public BeanClipboardContents( Object[] beanData, boolean isDragToDeleteEnabled ) {
        this.beanData = beanData;
        isDragToDeleteSupported = isDragToDeleteEnabled;
        createDataFlavors();
    }

    //create just data flavor for now, based on the root bean type in the bean array
    private void createDataFlavors() {
        if (beanData != null && beanData[0] != null) {
            try {
                dataFlavors = new DataFlavor[] { new BeanClipboardContentsDataFlavor(beanData[0].getClass(), isDragToDeleteSupported) };
            } catch (ClassNotFoundException cnfe) {
                log.error(getClass().getName() + " Failed create localObjectFlavors for class " + beanData[0].getClass().getName(), cnfe);
            }
        }
    }

    public Object[] getBeanData() {
            return beanData;
    }

    public DataFlavor[] getDataFlavors() {
        return dataFlavors;
    }

    public static DataFlavor createDataFlavor(Class clazz) {
        return BeanClipboardContentsDataFlavor.createDataFlavor(clazz);
    }

    /**
     * A data flavor for BeanClipboardContents, with properties to indicate the class of the bean data and
     * whether the data can be dragged to shredder/recycle bin
     */
    public static class BeanClipboardContentsDataFlavor extends DataFlavor {
        private Class beanClass;
        private boolean isDragToDeleteSupported;

        public BeanClipboardContentsDataFlavor() {}

        public BeanClipboardContentsDataFlavor(Class beanClass) throws ClassNotFoundException {
            super(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + BeanClipboardContents.class.getName());
            this.beanClass = beanClass;
        }

        public BeanClipboardContentsDataFlavor(Class beanClass, boolean isDragToDeleteSupported) throws ClassNotFoundException {
            super(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + BeanClipboardContents.class.getName());
            this.beanClass = beanClass;
            this.isDragToDeleteSupported = isDragToDeleteSupported;
        }

        public Class getBeanClass() {
            return beanClass;
        }

        public boolean isDragToDeleteSupported() {
            return isDragToDeleteSupported;
        }

        public static BeanClipboardContentsDataFlavor createDataFlavor(Class clazz) {
            BeanClipboardContentsDataFlavor result = null;
            try {
                result = new BeanClipboardContents.BeanClipboardContentsDataFlavor(clazz);
            } catch (ClassNotFoundException e) {
                log.error("Failed to create BeanClipboardContentsDataFlavor for class " + clazz.getName(), e);
            }
            return result;
        }

    }
}
