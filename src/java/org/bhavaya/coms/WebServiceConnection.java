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

package org.bhavaya.coms;

import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.PropertyGroup;

import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceFactory;
import java.beans.Expression;
import java.lang.reflect.Constructor;
import java.net.URL;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class WebServiceConnection {
    private static final Log log = Log.getCategory(WebServiceConnection.class);

    private String dataSourceName;
    private String endPoint;
    private Class serviceStubClass;
    private Object serviceStub;

    public WebServiceConnection(PropertyGroup propertyGroup) throws Exception {
        dataSourceName = propertyGroup.getProperty("name");
        String namespace = propertyGroup.getMandatoryProperty("namespace");
        String wsdlLocation = propertyGroup.getMandatoryProperty("wsdlLocation");
        String endPoint = propertyGroup.getMandatoryProperty("endPoint");
        String serviceLocalPart = propertyGroup.getMandatoryProperty("service");
        String serviceStubClassName = propertyGroup.getMandatoryProperty("serviceStubClass");

        init(endPoint, namespace, serviceLocalPart, wsdlLocation, serviceStubClassName);

    }

    private void init(String endPoint, String namespace, String serviceLocalPart, String wsdlLocation, String serviceStubClassName) throws Exception {
        log.info("Instantiating: " + serviceStubClassName + " for wsdlLocation: " + wsdlLocation + ", service: " + serviceLocalPart + ", namespace: " + namespace + ", endPoint: " + endPoint);
        this.endPoint = endPoint;
        QName serviceName = new QName(namespace, serviceLocalPart);

        System.setProperty(ServiceFactory.SERVICEFACTORY_PROPERTY, "org.apache.axis.client.ServiceFactory");
        ServiceFactory serviceFactory = ServiceFactory.newInstance();
        Service service = serviceFactory.createService(new URL(wsdlLocation), serviceName);

        serviceStubClass = ClassUtilities.getClass(serviceStubClassName);
        Constructor constructor = serviceStubClass.getConstructor(new Class[]{URL.class, Service.class});
        serviceStub = constructor.newInstance(new Object[]{new URL(endPoint), service});
        log.info("Instantiated: " + serviceStubClassName);
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Object invoke(String methodName, Object[] arguments) throws Exception {
        log.info("Executing webservice: " + serviceStubClass.getName() + "." + methodName + " on: " + endPoint);
        Expression expression = new Expression(serviceStub, methodName, arguments);
        return expression.getValue();
    }

    public Object getServiceStub() {
        return serviceStub;
    }
}
