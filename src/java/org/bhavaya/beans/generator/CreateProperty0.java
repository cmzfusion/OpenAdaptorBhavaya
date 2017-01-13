package org.bhavaya.beans.generator;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;
import org.bhavaya.util.Utilities;

public class CreateProperty0 implements ByteCodePropertyGenerator {
    public void add(ClassGen _cg, Type propertyType, String propertyTypeName, String propertyName, String indexName) {
        gen1(_cg, propertyType, propertyTypeName, propertyName, indexName);
        gen2(_cg, propertyType, propertyTypeName, propertyName, indexName);
    }

    public void gen1(ClassGen _cg, Type propertyType, String propertyTypeName, String propertyName, String indexName) {
        String _className = _cg.getJavaClass().getClassName();
        String _superclassName = _cg.getJavaClass().getSuperclassName();
        Type _propertyTypeForFireMethod = (propertyType instanceof ReferenceType) ? Type.OBJECT : propertyType;
        ConstantPoolGen _cp = _cg.getConstantPool();
        InstructionFactory _factory = new InstructionFactory(_cg, _cp);
        InstructionList il = new InstructionList();
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, propertyType, Type.NO_ARGS, new String[]{}, (propertyType == Type.BOOLEAN ? "is" : "get") + Utilities.capitalise(propertyName), _className, il, _cp);

        InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName, propertyType, Constants.GETFIELD));
        InstructionHandle ih_4 = il.append(_factory.createReturn(propertyType));
        method.setMaxStack();
        method.setMaxLocals();
        _cg.addMethod(method.getMethod());
        il.dispose();
    }

    public void gen2(ClassGen _cg, Type propertyType, String propertyTypeName, String propertyName, String indexName) {
        String _className = _cg.getJavaClass().getClassName();
        String _superclassName = _cg.getJavaClass().getSuperclassName();
        Type _propertyTypeForFireMethod = (propertyType instanceof ReferenceType) ? Type.OBJECT : propertyType;
        ConstantPoolGen _cp = _cg.getConstantPool();
        InstructionFactory _factory = new InstructionFactory(_cg, _cp);
        InstructionList il = new InstructionList();
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, new Type[]{propertyType}, new String[]{"arg0"}, "set" + Utilities.capitalise(propertyName), _className, il, _cp);

        InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName, propertyType, Constants.GETFIELD));
        il.append(_factory.createStore(propertyType, 3));
        InstructionHandle ih_5 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(propertyType, 1));
        il.append(_factory.createFieldAccess(_className, propertyName, propertyType, Constants.PUTFIELD));
        InstructionHandle ih_10 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(new PUSH(_cp, propertyName));
        il.append(_factory.createLoad(propertyType, 3));
        il.append(_factory.createLoad(propertyType, 1));
        il.append(_factory.createInvoke(_className, "firePropertyChange", Type.VOID, new Type[]{Type.STRING, _propertyTypeForFireMethod, _propertyTypeForFireMethod}, Constants.INVOKEVIRTUAL));
        InstructionHandle ih_18 = il.append(_factory.createReturn(Type.VOID));
        method.setMaxStack();
        method.setMaxLocals();
        _cg.addMethod(method.getMethod());
        il.dispose();
    }

}
