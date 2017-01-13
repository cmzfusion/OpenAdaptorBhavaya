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

 package org.bhavaya.util;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public class DefaultAttribute implements Attribute {
    private String name;
    private Class type;
    private boolean readable;
    private boolean writable;

    public DefaultAttribute(String name, Class type) {
        this(name, type, true, true);
    }

    public DefaultAttribute(String name, Class type, boolean readable, boolean writable) {
        if (name == null) throw new IllegalArgumentException("Attribute name is null");
        if (type == null) throw new IllegalArgumentException("Attribute type is null");
        this.name = name;
        this.type = type;
        this.readable = readable;
        this.writable = writable;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultAttribute)) return false;

        final DefaultAttribute defaultAttribute = (DefaultAttribute) o;

        if (readable != defaultAttribute.readable) return false;
        if (writable != defaultAttribute.writable) return false;
        if (!name.equals(defaultAttribute.name)) return false;
        if (!type.equals(defaultAttribute.type)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 29 * result + type.hashCode();
        result = 29 * result + (readable ? 1 : 0);
        result = 29 * result + (writable ? 1 : 0);
        return result;
    }

    public String toString() {
        return type.getName() + "." + name;
    }

    public int compareTo(Object o) {
        return getName().compareTo(((Attribute) o).getName());
    }

}
