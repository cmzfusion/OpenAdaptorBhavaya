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

package org.bhavaya.beans;

import org.bhavaya.coms.WebServiceConnection;
import org.bhavaya.util.ApplicationProperties;
import org.bhavaya.util.BeanUtilities;
import org.bhavaya.util.PropertyGroup;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class WebServiceBeanFactory extends OperationBeanFactory {
    static {
        BeanUtilities.addPersistenceDelegate(WebServiceBeanFactory.class, new PersistenceDelegate() {
            protected Expression instantiate(Object oldInstance, Encoder out) {
                BeanFactory oldBeanFactory = (BeanFactory) oldInstance;
                return new Expression(oldInstance, BeanFactory.class, "getInstance", new Object[]{oldBeanFactory.getType(), oldBeanFactory.getDataSourceName()});
            }

            protected boolean mutatesTo(Object oldInstance, Object newInstance) {
                return oldInstance.equals(newInstance);
            }
        });
    }

    private WebServiceConnection webServiceConnection;

    public WebServiceBeanFactory(Class type, String dataSourceName) throws Exception {
        super(type, dataSourceName);
        webServiceConnection = createWebServiceConnection(dataSourceName);
        if (webServiceConnection == null) throw new RuntimeException("Could not find webserviceDataSource: " + dataSourceName);
    }

    private WebServiceConnection createWebServiceConnection(String dataSourceName) throws Exception {
        WebServiceConnection webServiceConnection = null;
        PropertyGroup propertyGroup = ApplicationProperties.getApplicationProperties().getGroup("dataSources");
        if (propertyGroup != null) {
            PropertyGroup[] dataSourcesPropertyGroup = propertyGroup.getGroups("webserviceDatasource");
            if (dataSourcesPropertyGroup != null) {
                for (int i = 0; i < dataSourcesPropertyGroup.length; i++) {
                    PropertyGroup dataSourcePropertyGroup = dataSourcesPropertyGroup[i];
                    if (dataSourcePropertyGroup.getMandatoryProperty("name").equals(dataSourceName)) {
                        webServiceConnection = new WebServiceConnection(dataSourcePropertyGroup);
                    }
                }
            }
        }
        return webServiceConnection;
    }

    public Object getTarget() {
        return webServiceConnection.getServiceStub();
    }
}
