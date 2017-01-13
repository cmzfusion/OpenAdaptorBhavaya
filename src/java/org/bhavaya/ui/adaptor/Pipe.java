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

package org.bhavaya.ui.adaptor;

import org.bhavaya.util.Log;
import org.bhavaya.util.Transform;
import org.bhavaya.util.Utilities;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.Statement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;


/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.5 $
 */

public class Pipe {
    private static Log log = Log.getCategory(Pipe.class);
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};

    private Source source;
    private Sink sink;
    private Transform transform;

    private Object oldTransformedData;
    private Object dummy = new Object();

    private Pipe reversePipe;

    public Pipe(Source source, Sink sink) {
        this(source, sink, null, false);
    }

    public Pipe(Source source, Sink sink, Transform transform, boolean execute) {
        if (transform == null) transform = Transform.IDENTITY_TRANSFORM;
        this.source = source;
        this.sink = sink;
        this.transform = transform;
        oldTransformedData = dummy;

        if (execute) execute();
    }

    public void execute() {
        Object sourceData = source.getData();
        Object transformData = transform(sourceData);
        if (!Utilities.equals(transformData, oldTransformedData)) {
            if (log.isDebug()) log.debug("Executing pipe: " + source + " -> " + sink + "  value = " + sourceData+" transformedValue = "+transformData);

            if (reversePipe != null) {
                //this pipe takes "sourceData" and transforms it into "transformData"
                //the reverse operation is for the reversePipe to take "transformData" and transform it back into "sourceData"
                //i.e. reversePipe.transform(transformData).equals(source) == true
                reversePipe.oldTransformedData = sourceData;
            }
            this.oldTransformedData = transformData;

            sink.setData(transformData);
        } else {
            if (log.isDebug()) log.debug("Ignoring pipe: " + source + " -> " + sink + "  value = " + sourceData);
        }
    }

    public void setReversePipe(Pipe reversePipe) {
        this.reversePipe = reversePipe;
    }

    protected Object transform(Object sourceData) {
        return transform.execute(sourceData);
    }

    public void executeIfFocusTemporary(Boolean isTemporary) {
        if (isTemporary) {
            execute();
        }
    }

    public <T> T getListenerInterface(Class<T> listenerClass) {
        return getListenerInterface(listenerClass, EMPTY_STRING_ARRAY);
    }

    public <T> T getListenerInterface(Class<T> listenerClass, String method) {
        return getListenerInterface(listenerClass, new String[]{method});
    }

    @SuppressWarnings("unchecked")
    public <T> T getListenerInterface(Class<T> listenerClass, String[] methods) {
        final List<String> immutableMethodNames = Arrays.asList(methods);

        if (listenerClass == FocusListener.class) {
            return (T) new FocusListener() {
                public void focusGained(FocusEvent e) {
                    if (!e.isTemporary()) {
                        if (immutableMethodNames.size() == 0 || immutableMethodNames.contains("focusGained")) {
                            execute();
                        }
                    }
                }

                public void focusLost(FocusEvent e) {
                    if (!e.isTemporary()) {
                        if (immutableMethodNames.size() == 0 || immutableMethodNames.contains("focusLost")) {
                            execute();
                        }
                    }
                }
            };
        } else {
            InvocationHandler handler = new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.equals(Object.class.getMethod("equals", Object.class))) {
                        return proxy == args[0];
                    } else if (method.getDeclaringClass().equals(Object.class)) {
                        return method.invoke(this, args);
                    }

                    if (immutableMethodNames.size() == 0 || immutableMethodNames.contains(method.getName())) {
                        Statement pipeExecute = new Statement(Pipe.this, "execute", EMPTY_OBJECT_ARRAY);
                        pipeExecute.execute();
                    }
                    return null;
                }
            };
            return (T) Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, handler);
        }
    }

    /**
     * Primes this pipe with a particular value.  Pipe will act as if this is the value that was last executed effectively
     * preventing updates from happening for setters firing on this value.
     */
    public void prime(Object primedValue) {
        this.oldTransformedData = transform.execute(primedValue);
    }
}