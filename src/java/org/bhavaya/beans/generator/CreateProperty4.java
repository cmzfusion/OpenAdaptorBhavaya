package org.bhavaya.beans.generator;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;
import org.bhavaya.util.Utilities;

public class CreateProperty4 implements ByteCodePropertyGenerator {
    public void add(ClassGen _cg, Type propertyType, String propertyTypeName, String propertyName, String indexName) {
        gen1(_cg, propertyType, propertyTypeName, propertyName, indexName);
        gen2(_cg, propertyType, propertyTypeName, propertyName, indexName);
        gen3(_cg, propertyType, propertyTypeName, propertyName, indexName);
    }

    public void gen1(ClassGen _cg, Type propertyType, String propertyTypeName, String propertyName, String indexName) {
        String _className = _cg.getJavaClass().getClassName();
        String _superclassName = _cg.getJavaClass().getSuperclassName();
        Type _propertyTypeForFireMethod = (propertyType instanceof ReferenceType) ? Type.OBJECT : propertyType;
        ConstantPoolGen _cp = _cg.getConstantPool();
        InstructionFactory _factory = new InstructionFactory(_cg, _cp);
        InstructionList il = new InstructionList();
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, new ObjectType("org.bhavaya.collection.BeanCollection"), Type.NO_ARGS, new String[]{}, "get" + Utilities.capitalise(propertyName) + "Collection", _className, il, _cp);

        InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.GETFIELD));
        InstructionHandle ih_4 = il.append(_factory.createReturn(Type.OBJECT));
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
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, new ArrayType(new ObjectType(propertyTypeName), 1), Type.NO_ARGS, new String[]{}, (propertyType == Type.BOOLEAN ? "is" : "get") + Utilities.capitalise(propertyName), _className, il, _cp);

        InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createInvoke(_className, "get" + Utilities.capitalise(propertyName) + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        BranchInstruction ifnonnull_4 = _factory.createBranchInstruction(Constants.IFNONNULL, null);
        il.append(ifnonnull_4);
        il.append(InstructionConstants.ACONST_NULL);
        il.append(_factory.createReturn(Type.OBJECT));
        InstructionHandle ih_9 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createInvoke(_className, "get" + Utilities.capitalise(propertyName) + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createInvoke(_className, "get" + Utilities.capitalise(propertyName) + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        il.append(_factory.createInvoke("org.bhavaya.collection.BeanCollection", "size", Type.INT, Type.NO_ARGS, Constants.INVOKEINTERFACE));
        il.append(_factory.createNewArray(new ObjectType(propertyTypeName), (short) 1));
        il.append(_factory.createInvoke("org.bhavaya.collection.BeanCollection", "toArray", new ArrayType(Type.OBJECT, 1), new Type[]{new ArrayType(Type.OBJECT, 1)}, Constants.INVOKEINTERFACE));
        il.append(_factory.createCheckCast(new ArrayType(new ObjectType(propertyTypeName), 1)));
        InstructionHandle ih_33 = il.append(_factory.createReturn(Type.OBJECT));
        ifnonnull_4.setTarget(ih_9);
        method.setMaxStack();
        method.setMaxLocals();
        _cg.addMethod(method.getMethod());
        il.dispose();
    }

    public void gen3(ClassGen _cg, Type propertyType, String propertyTypeName, String propertyName, String indexName) {
        String _className = _cg.getJavaClass().getClassName();
        String _superclassName = _cg.getJavaClass().getSuperclassName();
        Type _propertyTypeForFireMethod = (propertyType instanceof ReferenceType) ? Type.OBJECT : propertyType;
        ConstantPoolGen _cp = _cg.getConstantPool();
        InstructionFactory _factory = new InstructionFactory(_cg, _cp);
        InstructionList il = new InstructionList();
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, new Type[]{new ObjectType("org.bhavaya.collection.BeanCollection")}, new String[]{"arg0"}, "set" + Utilities.capitalise(propertyName) + "Collection", _className, il, _cp);

        InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.GETFIELD));
        il.append(_factory.createStore(Type.OBJECT, 2));
        InstructionHandle ih_5 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.PUTFIELD));
        InstructionHandle ih_10 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(new PUSH(_cp, propertyName + "Collection"));
        il.append(_factory.createLoad(Type.OBJECT, 2));
        il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createInvoke(_className, "firePropertyChange", Type.VOID, new Type[]{Type.STRING, Type.OBJECT, Type.OBJECT}, Constants.INVOKEVIRTUAL));
        InstructionHandle ih_18 = il.append(_factory.createReturn(Type.VOID));
        method.setMaxStack();
        method.setMaxLocals();
        _cg.addMethod(method.getMethod());
        il.dispose();
    }

}
