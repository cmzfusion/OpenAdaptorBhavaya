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

package org.bhavaya.beans.generator;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;

import java.io.OutputStream;
import java.io.PrintWriter;

public class BCELifier extends EmptyVisitor {
    private static int IGNORE_CONSTRUCTOR = 1;

    public BCELifier(JavaClass clazz, OutputStream out) {
        _clazz = clazz;
        _out = new PrintWriter(out);
        _cp = new ConstantPoolGen(_clazz.getConstantPool());
    }

    public void start() {
        visitJavaClass(_clazz);
        _out.flush();
    }

    public void visitJavaClass(JavaClass clazz) {
        String class_name = clazz.getClassName();
        String super_name = clazz.getSuperclassName();
        String package_name = clazz.getPackageName();
        String inter = Utility.printArray(clazz.getInterfaceNames(), false, true);
        if (!"".equals(package_name)) {
            class_name = class_name.substring(package_name.length() + 1);
            _out.println("package " + package_name + ";");
        }
        _out.println();
        _out.println("import org.apache.bcel.generic.*;");
        _out.println("import org.apache.bcel.classfile.*;");
        _out.println("import org.apache.bcel.*;");
        _out.println("import org.bhavaya.util.Utilities; ");
        _out.println("import java.io.*;");
        _out.println();
        _out.println("public class " + "Create" + class_name + " implements ByteCodePropertyGenerator {");
        Method methods[] = clazz.getMethods();

        _out.println("  public void add" + "(ClassGen _cg, Type propertyType, String propertyTypeName, String propertyName, String indexName) {");
        for (int i = IGNORE_CONSTRUCTOR; i < methods.length; i++) {
            _out.println("    gen" + i + "(_cg,  propertyType, propertyTypeName, propertyName, indexName); ");
        }
        _out.println("  }");
        _out.println("");

        for (int i = IGNORE_CONSTRUCTOR; i < methods.length; i++) {
            String methodName = "gen" + i;
            _out.println("  public void " + methodName + "(ClassGen _cg, Type propertyType, String propertyTypeName, String propertyName, String indexName) {");
            _out.println("    String _className = _cg.getJavaClass().getClassName(); ");
            _out.println("    String _superclassName = _cg.getJavaClass().getSuperclassName(); ");
            _out.println("    Type _propertyTypeForFireMethod = (propertyType instanceof ReferenceType) ? Type.OBJECT : propertyType; ");
            _out.println("    ConstantPoolGen _cp = _cg.getConstantPool(); ");
            _out.println("    InstructionFactory _factory = new InstructionFactory(_cg, _cp); ");
//            _out.println("    String propertyTypeWithDollars = \"class$\" + propertyTypeName.replace('.', '$');");
            methods[i].accept(this);
            _out.println("  }");
            _out.println("");
        }
        _out.println("}");
    }

    public void visitField(Field field) {
        _out.println("\n    field = new FieldGen(" + printFlags(field.getAccessFlags()) + ", " + printType(field.getSignature()) + ", \"" + field.getName() + "\", _cp);");
        ConstantValue cv = field.getConstantValue();
        if (cv != null) {
            String value = cv.toString();
            _out.println("    field.setInitValue(" + value + ")");
        }
        _out.println("    _cg.addField(field.getField());");
    }

    public void visitMethod(Method method) {
        MethodGen mg = new MethodGen(method, _clazz.getClassName(), _cp);
        Type result_type = mg.getReturnType();
        Type arg_types[] = mg.getArgumentTypes();
        _out.println("    InstructionList il = new InstructionList();");
        _out.println("    MethodGen method = new MethodGen(" + printFlags(method.getAccessFlags()) + ", " + printType(result_type) + ", " + printArgumentTypes(arg_types) + ", " + "new String[] { " + Utility.printArray(mg.getArgumentNames(), false, true) + " }, \"" + method.getName() + "\", \"" + _clazz.getClassName() + "\", il, _cp);\n");
        BCELFactory factory = new BCELFactory(mg, _out);
        factory.start();
        _out.println("    method.setMaxStack();");
        _out.println("    method.setMaxLocals();");
        _out.println("    _cg.addMethod(method.getMethod());");
        _out.println("    il.dispose();");
    }

    static String printFlags(int flags) {
        return printFlags(flags, false);
    }

    static String printFlags(int flags, boolean for_class) {
        if (flags == 0)
            return "0";
        StringBuffer buf = new StringBuffer();
        int i = 0;
        int pow = 1;
        for (; i <= 2048; i++) {
            if ((flags & pow) != 0)
                if (pow == 32 && for_class)
                    buf.append("Constants.ACC_SUPER | ");
                else
                    buf.append("Constants.ACC_" + Constants.ACCESS_NAMES[i].toUpperCase() + " | ");
            pow <<= 1;
        }

        String str = buf.toString();
        return str.substring(0, str.length() - 3);
    }

    static String printArgumentTypes(Type arg_types[]) {
        if (arg_types.length == 0)
            return "Type.NO_ARGS";
        StringBuffer args = new StringBuffer();
        for (int i = 0; i < arg_types.length; i++) {
            args.append(printType(arg_types[i]));
            if (i < arg_types.length - 1)
                args.append(", ");
        }

        return "new Type[] { " + args.toString() + " }";
    }

    static String printType(Type type) {
        return printType(type.getSignature());
    }

    static String printType(String signature) {
        Type type = Type.getType(signature);
        byte t = type.getType();
        if (t <= 12)
            return "Type." + Constants.TYPE_NAMES[t].toUpperCase();
        if (type.toString().equals("java.lang.String"))
            return "Type.STRING";
        if (type.toString().equals("java.lang.Object"))
            return "Type.OBJECT";
        if (type.toString().equals("java.lang.StringBuffer"))
            return "Type.STRINGBUFFER";
        if (type instanceof ArrayType) {
            ArrayType at = (ArrayType) type;
            return "new ArrayType(" + printType(at.getBasicType()) + ", " + at.getDimensions() + ")";
        } else {
            return "new ObjectType(\"" + Utility.signatureToString(signature, false) + "\")";
        }
    }

    public static void main(String argv[]) throws Exception {
        String name = argv[0];
        JavaClass java_class;
        if ((java_class = Repository.lookupClass(name)) == null)
            java_class = (new ClassParser(name)).parse();
        BCELifier bcelifier = new BCELifier(java_class, System.out);
        bcelifier.start();
    }

    private JavaClass _clazz;
    private PrintWriter _out;
    private ConstantPoolGen _cp;
}
