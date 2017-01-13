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

package org.bhavaya.ui;

import org.bhavaya.util.Log;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;

/**
 * Sinful lift of javax.swing.plaf.basic.BasicTransferable
 * You'd have thought that this class might actually be in one of the public packages, since it is not UI specific at all.
 *
 * @author Daniel van Enckevort
 * @author Nick Ebbutt - add support for transferable bean data for drag and drop
 * @version $Revision: 1.2 $
 */
public class BeanTableTransferable implements Transferable {
    private static final Log log = Log.getCategory(BeanTableTransferable.class);

        protected String plainData;
        protected String htmlData;
        protected BeanClipboardContents beanClipboardContentsData;

        private static DataFlavor[] htmlFlavors;
        private static DataFlavor[] stringFlavors;
        private static DataFlavor[] plainFlavors;

        private DataFlavor[] beanFlavors = new DataFlavor[0];

        static {
            try {
                htmlFlavors = new DataFlavor[3];
                htmlFlavors[0] = new DataFlavor("text/html;class=java.lang.String");
                htmlFlavors[1] = new DataFlavor("text/html;class=java.io.Reader");
                htmlFlavors[2] = new DataFlavor("text/html;charset=unicode;class=java.io.InputStream");

                plainFlavors = new DataFlavor[3];
                plainFlavors[0] = new DataFlavor("text/plain;class=java.lang.String");
                plainFlavors[1] = new DataFlavor("text/plain;class=java.io.Reader");
                plainFlavors[2] = new DataFlavor("text/plain;charset=unicode;class=java.io.InputStream");

                stringFlavors = new DataFlavor[2];
                stringFlavors[0] = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=java.lang.String");
                stringFlavors[1] = DataFlavor.stringFlavor;

            } catch (ClassNotFoundException cle) {
                log.error("error initializing javax.swing.plaf.basic.BasicTranserable");
            }
        }

        /**
         * @param plainData
         * @param htmlData
         * @param localObjects, a fully populated array of Objects to transfer within the local jvm which must all be of the same class type
         */
        public BeanTableTransferable(String plainData, String htmlData, Object[] localObjects, boolean isDragToDeleteSupported) {
            this.plainData = plainData;
            this.htmlData = htmlData;
            createLocalObjectDataFlavor(localObjects, isDragToDeleteSupported);
        }

        private void createLocalObjectDataFlavor(Object[] localObjects, boolean isDragToDeleteSupported) {

            if (localObjects != null && localObjects.length > 0) {
                beanClipboardContentsData = new BeanClipboardContents(localObjects, isDragToDeleteSupported);
                beanFlavors = beanClipboardContentsData.getDataFlavors();
            }
        }


    /**
         * Returns an array of DataFlavor objects indicating the flavors the data
         * can be provided in.  The array should be ordered according to preference
         * for providing the data (from most richly descriptive to least descriptive).
         * @return an array of data flavors in which this data can be transferred
         */
        public DataFlavor[] getTransferDataFlavors() {
            DataFlavor[] richerFlavors = getRicherFlavors();
            int nRicher = (richerFlavors != null) ? richerFlavors.length : 0;
            int nHTML = (isHTMLSupported()) ? htmlFlavors.length : 0;
            int nPlain = (isPlainSupported()) ? plainFlavors.length: 0;
            int nString = (isPlainSupported()) ? stringFlavors.length : 0;
            int nLocalObject = beanFlavors.length;

            int nFlavors = nRicher + nHTML + nPlain + nString + nLocalObject;
            DataFlavor[] flavors = new DataFlavor[nFlavors];

            // fill in the array
            int nDone = 0;
            if (nLocalObject > 0) {
                System.arraycopy(beanFlavors, 0, flavors, nDone, nLocalObject);
                nDone += nLocalObject;
            }
            if (nRicher > 0) {
                System.arraycopy(richerFlavors, 0, flavors, nDone, nRicher);
                nDone += nRicher;
            }
            if (nHTML > 0) {
                System.arraycopy(htmlFlavors, 0, flavors, nDone, nHTML);
                nDone += nHTML;
            }
            if (nPlain > 0) {
                System.arraycopy(plainFlavors, 0, flavors, nDone, nPlain);
                nDone += nPlain;
            }
            if (nString > 0) {
                System.arraycopy(stringFlavors, 0, flavors, nDone, nString);
                nDone += nString;
            }
            return flavors;
        }

        /**
         * Returns whether or not the specified data flavor is supported for
         * this object.
         * @param flavor the requested flavor for the data
         * @return boolean indicating whether or not the data flavor is supported
         */
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            DataFlavor[] flavors = getTransferDataFlavors();
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(flavor)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns an object which represents the data to be transferred.  The class
         * of the object returned is defined by the representation class of the flavor.
         *
         * @param flavor the requested flavor for the data
         * @see DataFlavor#getRepresentationClass
         * @exception java.io.IOException                if the data is no longer available
         *              in the requested flavor.
         * @exception java.awt.datatransfer.UnsupportedFlavorException if the requested data flavor is
         *              not supported.
         */
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            DataFlavor[] richerFlavors = getRicherFlavors();
            if (isRicherFlavor(flavor)) {
                return getRicherData(flavor);
            } else if (isHTMLFlavor(flavor)) {
                String data = getHTMLData();
                data = (data == null) ? "" : data;
                if (String.class.equals(flavor.getRepresentationClass())) {
                    return data;
                } else if (Reader.class.equals(flavor.getRepresentationClass())) {
                    return new StringReader(data);
                } else if (InputStream.class.equals(flavor.getRepresentationClass())) {
                    return new ByteArrayInputStream(data.getBytes());
                }
                // fall through to unsupported
            } else if (isPlainFlavor(flavor)) {
                String data = getPlainData();
                data = (data == null) ? "" : data;
                if (String.class.equals(flavor.getRepresentationClass())) {
                    return data;
                } else if (Reader.class.equals(flavor.getRepresentationClass())) {
                    return new StringReader(data);
                } else if (InputStream.class.equals(flavor.getRepresentationClass())) {
                    return new ByteArrayInputStream(data.getBytes());
                }
                // fall through to unsupported

            } else if (isStringFlavor(flavor)) {
                String data = getPlainData();
                data = (data == null) ? "" : data;
                return data;
            } else if ( flavor instanceof BeanClipboardContents.BeanClipboardContentsDataFlavor ) {
                BeanClipboardContents.BeanClipboardContentsDataFlavor requestedFlavor = (BeanClipboardContents.BeanClipboardContentsDataFlavor)flavor;
                Class requestedClass = requestedFlavor.getBeanClass();
                for ( DataFlavor dataFlavor : beanFlavors ) {
                    //if the user did not specify a class return the first bean flavor which matches
                    if ( requestedClass == null || dataFlavor.getClass() == flavor.getClass()) {
                        return beanClipboardContentsData;
                    }
                }
            }
            throw new UnsupportedFlavorException(flavor);
        }

        // --- richer subclass flavors ----------------------------------------------

        protected boolean isRicherFlavor(DataFlavor flavor) {
            DataFlavor[] richerFlavors = getRicherFlavors();
            int nFlavors = (richerFlavors != null) ? richerFlavors.length : 0;
            for (int i = 0; i < nFlavors; i++) {
                if (richerFlavors[i].equals(flavor)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Some subclasses will have flavors that are more descriptive than HTML
         * or plain text.  If this method returns a non-null value, it will be
         * placed at the start of the array of supported flavors.
         */
        protected DataFlavor[] getRicherFlavors() {
            return null;
        }

        protected Object getRicherData(DataFlavor flavor) throws UnsupportedFlavorException {
            return null;
        }

        // --- html flavors ----------------------------------------------------------

        /**
         * Returns whether or not the specified data flavor is an HTML flavor that
         * is supported.
         * @param flavor the requested flavor for the data
         * @return boolean indicating whether or not the data flavor is supported
         */
        protected boolean isHTMLFlavor(DataFlavor flavor) {
            DataFlavor[] flavors = htmlFlavors;
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(flavor)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Should the HTML flavors be offered?  If so, the method
         * getHTMLData should be implemented to provide something reasonable.
         */
        protected boolean isHTMLSupported() {
            return htmlData != null;
        }

        /**
         * Fetch the data in a text/html format
         */
        protected String getHTMLData() {
            return htmlData;
        }

        // --- plain text flavors ----------------------------------------------------

        /**
         * Returns whether or not the specified data flavor is an plain flavor that
         * is supported.
         * @param flavor the requested flavor for the data
         * @return boolean indicating whether or not the data flavor is supported
         */
        protected boolean isPlainFlavor(DataFlavor flavor) {
            DataFlavor[] flavors = plainFlavors;
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(flavor)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Should the plain text flavors be offered?  If so, the method
         * getPlainData should be implemented to provide something reasonable.
         */
        protected boolean isPlainSupported() {
            return plainData != null;
        }

        /**
         * Fetch the data in a text/plain format.
         */
        protected String getPlainData() {
            return plainData;
        }

        // --- string flavorss --------------------------------------------------------

        /**
         * Returns whether or not the specified data flavor is a String flavor that
         * is supported.
         * @param flavor the requested flavor for the data
         * @return boolean indicating whether or not the data flavor is supported
         */
        protected boolean isStringFlavor(DataFlavor flavor) {
            DataFlavor[] flavors = stringFlavors;
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(flavor)) {
                    return true;
                }
            }
            return false;
        }

}
