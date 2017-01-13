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

import org.bhavaya.util.ClassUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.Transform;

import java.util.List;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class Operation {
    private static final Log log = Log.getCategory(Operation.class);

    private String parentTypeName;
    private String name;
    private String indexName;
    private String transformClassName;
    private Transform transform;
    private Index index;
    private Parameter[] parameters;


    public Operation(String parentTypeName, String name, String indexName, String transformClassName) {
        this.parentTypeName = parentTypeName;
        this.name = name;
        if (indexName != null && indexName.length() == 0) indexName = null; // blank is null
        this.indexName = indexName;
        this.transformClassName = transformClassName;
    }

    public String getParentTypeName() {
        return parentTypeName;
    }

    public String getName() {
        return name;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setParameters(List parameters) {
        this.parameters = (Parameter[]) parameters.toArray(new Parameter[parameters.size()]);
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public String getTransformClassName() {
        return transformClassName;
    }

    public Transform getTransform() {
        if (transformClassName != null && transform == null) {
            try {
                transform = (Transform) ClassUtilities.getClass(transformClassName).newInstance();
            } catch (Exception e) {
                log.error(e);
            }
        }
        return transform;
    }

    public Index getIndex() {
        if (indexName != null && index == null) {
            index = Schema.getInstance(parentTypeName).getIndex(indexName);
        }
        return index;
    }

    public Object[] getArguments(Object key) {
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter instanceof KeyTransformParameter) {
                arguments[i] = ((KeyTransformParameter) parameter).getValue(key);
            } else if (parameter instanceof DefaultValueParameter) {
                arguments[i] = ((DefaultValueParameter) parameter).getValue();
            }
        }

        return arguments;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operation)) return false;

        final Operation operation = (Operation) o;

        if (indexName != null ? !indexName.equals(operation.indexName) : operation.indexName != null) return false;
        if (!name.equals(operation.name)) return false;
        if (!parentTypeName.equals(operation.parentTypeName)) return false;
        if (transformClassName != null ? !transformClassName.equals(operation.transformClassName) : operation.transformClassName != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = parentTypeName.hashCode();
        result = 29 * result + name.hashCode();
        result = 29 * result + (indexName != null ? indexName.hashCode() : 0);
        result = 29 * result + (transformClassName != null ? transformClassName.hashCode() : 0);
        return result;
    }


    static interface Parameter {
    }

    static class KeyTransformParameter implements Parameter {
        private String keyTransformClassName;
        private Transform keyTransform;

        public KeyTransformParameter(String keyTransformClassName) {
            this.keyTransformClassName = keyTransformClassName;
        }

        private Transform getKeyTransform() {
            if (keyTransform == null && keyTransformClassName != null && keyTransformClassName.length() > 0) {
                try {
                    // prevent any class load order effects while loading schemas
                    keyTransform = (Transform) ClassUtilities.getClass(keyTransformClassName, true, false).newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return keyTransform;
        }

        public String getKeyTransformClassName() {
            return keyTransformClassName;
        }

        public Object getValue(Object key) {
            final Transform keyTransform = getKeyTransform();
            if (keyTransform == null) return key;
            return keyTransform.execute(key);
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof KeyTransformParameter)) return false;

            final KeyTransformParameter keyTransformParameter = (KeyTransformParameter) o;

            if (keyTransformClassName != null ? !keyTransformClassName.equals(keyTransformParameter.keyTransformClassName) : keyTransformParameter.keyTransformClassName != null) return false;

            return true;
        }

        public int hashCode() {
            return (keyTransformClassName != null ? keyTransformClassName.hashCode() : 0);
        }
    }

    static class DefaultValueParameter extends DefaultValue implements Parameter {
        public DefaultValueParameter(String typeName, String valueString) {
            super(typeName, valueString);
        }
    }
}
