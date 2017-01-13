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

package org.bhavaya.ui.builder;

import org.bhavaya.beans.generator.Application;
import org.bhavaya.collection.DefaultBeanCollection;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.ValidationException;
import org.bhavaya.util.BhavayaPersistenceDelegate;

import java.beans.Encoder;
import java.beans.Statement;
import java.util.Iterator;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class ApplicationCollection extends DefaultBeanCollection {
    private static final Log log = Log.getCategory(ApplicationCollection.class);

    static {
        BeanUtilities.addPersistenceDelegate(ApplicationCollection.class, new BhavayaPersistenceDelegate(new String[]{"type"}) {
            protected void initialize(Class type, Object oldInstance, Object newInstance, Encoder out) {
                ApplicationCollection oldBeanCollection = (ApplicationCollection) oldInstance;
                for (Iterator iterator = oldBeanCollection.iterator(); iterator.hasNext();) {
                    out.writeStatement(new Statement(oldInstance, "add", new Object[]{iterator.next()}));
                }
            }
        });
    }



    public ApplicationCollection() {
        super(Application.class);
    }

    public void amend(Application application, Application originalApplication) throws ValidationException {
        log.info("Amending: " + application);
        application.validate();
        remove(originalApplication, false);
        add(application, false);
        fireCommit();
    }


    public void remove(Application application, String baseDir) {
        log.info("Removing: " + application);
        application.deleteDirectories(baseDir);
        remove(application);
    }

}
