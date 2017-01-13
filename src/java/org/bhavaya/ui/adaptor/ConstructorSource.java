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

import org.bhavaya.util.Transform;

import java.lang.reflect.Constructor;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */

public class ConstructorSource implements Source {
    private Constructor constructor;
    private Source[] sourceArray;
    private Transform[] transformArray;

    public ConstructorSource(Class clazz, Class[] parameterTypes, Source[] sourceArray, Transform[] transformArray) {
        try {
            init(clazz.getConstructor(parameterTypes), sourceArray, transformArray);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ConstructorSource(Constructor constructor, Source[] sourceArray, Transform[] transformArray) {
        init(constructor, sourceArray, transformArray);
    }

    private void init(Constructor constructor, Source[] sourceArray, Transform[] transformArray) {
        this.constructor = constructor;
        this.sourceArray = sourceArray;
        this.transformArray = transformArray;
    }

    public Object getData() {
        try {
            return constructor.newInstance(getSourceData());
        } catch (Exception e) {
            return null;
        }
    }

    private Object[] getSourceData() {
        Object[] sourceData = new Object[sourceArray.length];

        for (int i = 0; i < sourceArray.length; i++) {
            sourceData[i] = transformArray[i].execute(sourceArray[i].getData());
        }

        return sourceData;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("new ").append(constructor.getName()).append("(");
        for (int i = 0; i < sourceArray.length; i++) {
            buf.append(sourceArray[i]).append(", ");
        }
        buf.setLength(buf.length() - 2);

        return buf.append(")").toString();
    }
}
