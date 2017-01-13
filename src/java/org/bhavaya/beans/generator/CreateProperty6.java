package org.bhavaya.beans.generator;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;
import org.bhavaya.util.Utilities;

public class CreateProperty6 implements ByteCodePropertyGenerator {
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
        BranchInstruction ifnonnull_4 = _factory.createBranchInstruction(Constants.IFNONNULL, null);
        il.append(ifnonnull_4);
        InstructionHandle ih_7 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "getKeyForBean", Type.OBJECT, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        il.append(_factory.createStore(Type.OBJECT, 1));
        InstructionHandle ih_12 = il.append(new PUSH(_cp, propertyTypeName));
        il.append(_factory.createInvoke("org.bhavaya.util.ClassUtilities", "getClass", new ObjectType("java.lang.Class"), new Type[]{Type.STRING}, Constants.INVOKESTATIC));
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "getDatasource", Type.STRING, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "getInstance", new ObjectType("org.bhavaya.beans.BeanFactory"), new Type[]{new ObjectType("java.lang.Class"), Type.STRING}, Constants.INVOKESTATIC));
        il.append(_factory.createStore(Type.OBJECT, 2));
        InstructionHandle ih_25 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(Type.OBJECT, 2));
        il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(new PUSH(_cp, indexName));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "get", Type.OBJECT, new Type[]{Type.OBJECT, Type.STRING}, Constants.INVOKEVIRTUAL));
        il.append(_factory.createCheckCast(new ObjectType("org.bhavaya.collection.BeanCollection")));
        il.append(_factory.createInvoke(_className, "set" + Utilities.capitalise(propertyName) + "Collection", Type.VOID, new Type[]{new ObjectType("org.bhavaya.collection.BeanCollection")}, Constants.INVOKEVIRTUAL));
        InstructionHandle ih_39;
        BranchInstruction goto_39 = _factory.createBranchInstruction(Constants.GOTO, null);
        ih_39 = il.append(goto_39);
        InstructionHandle ih_42 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.GETFIELD));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "isLazy", Type.BOOLEAN, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        BranchInstruction ifeq_49 = _factory.createBranchInstruction(Constants.IFEQ, null);
        il.append(ifeq_49);
        InstructionHandle ih_52 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.GETFIELD));
        il.append(_factory.createCheckCast(new ObjectType("org.bhavaya.util.LoadClosure")));
        il.append(_factory.createInvoke("org.bhavaya.util.LoadClosure", "load", Type.OBJECT, Type.NO_ARGS, Constants.INVOKEINTERFACE));
        il.append(_factory.createCheckCast(new ObjectType("org.bhavaya.collection.BeanCollection")));
        il.append(_factory.createInvoke(_className, "set" + Utilities.capitalise(propertyName) + "Collection", Type.VOID, new Type[]{new ObjectType("org.bhavaya.collection.BeanCollection")}, Constants.INVOKEVIRTUAL));
        InstructionHandle ih_71 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.GETFIELD));
        il.append(_factory.createFieldAccess("org.bhavaya.beans.LazyBeanCollection", "NULL_COLLECTION", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.GETSTATIC));
        BranchInstruction if_acmpne_78 = _factory.createBranchInstruction(Constants.IF_ACMPNE, null);
        il.append(if_acmpne_78);
        InstructionHandle ih_81 = il.append(InstructionConstants.ACONST_NULL);
        il.append(_factory.createReturn(Type.OBJECT));
        InstructionHandle ih_83 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.GETFIELD));
        InstructionHandle ih_87 = il.append(_factory.createReturn(Type.OBJECT));
        ifnonnull_4.setTarget(ih_42);
        goto_39.setTarget(ih_71);
        ifeq_49.setTarget(ih_71);
        if_acmpne_78.setTarget(ih_83);
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

        InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "isLazy", Type.BOOLEAN, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        il.append(_factory.createStore(Type.INT, 2));
        InstructionHandle ih_5 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.GETFIELD));
        il.append(_factory.createStore(Type.OBJECT, 3));
        InstructionHandle ih_10 = il.append(_factory.createLoad(Type.OBJECT, 3));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "isLazy", Type.BOOLEAN, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        il.append(_factory.createStore(Type.INT, 4));
        InstructionHandle ih_16 = il.append(_factory.createLoad(Type.OBJECT, 3));
        BranchInstruction ifnull_17 = _factory.createBranchInstruction(Constants.IFNULL, null);
        il.append(ifnull_17);
        il.append(_factory.createLoad(Type.INT, 4));
        BranchInstruction ifne_22 = _factory.createBranchInstruction(Constants.IFNE, null);
        il.append(ifne_22);
        il.append(_factory.createLoad(Type.INT, 2));
        BranchInstruction ifeq_26 = _factory.createBranchInstruction(Constants.IFEQ, null);
        il.append(ifeq_26);
        InstructionHandle ih_29 = il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createCheckCast(new ObjectType("org.bhavaya.util.LoadClosure")));
        il.append(_factory.createInvoke("org.bhavaya.util.LoadClosure", "load", Type.OBJECT, Type.NO_ARGS, Constants.INVOKEINTERFACE));
        il.append(_factory.createCheckCast(new ObjectType("org.bhavaya.collection.BeanCollection")));
        il.append(_factory.createStore(Type.OBJECT, 1));
        InstructionHandle ih_42 = il.append(new PUSH(_cp, 0));
        il.append(_factory.createStore(Type.INT, 2));
        InstructionHandle ih_44 = il.append(_factory.createLoad(Type.OBJECT, 3));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "isLazyNull", Type.BOOLEAN, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        BranchInstruction ifeq_48 = _factory.createBranchInstruction(Constants.IFEQ, null);
        il.append(ifeq_48);
        il.append(InstructionConstants.ACONST_NULL);
        BranchInstruction goto_52 = _factory.createBranchInstruction(Constants.GOTO, null);
        il.append(goto_52);
        InstructionHandle ih_55 = il.append(_factory.createLoad(Type.OBJECT, 3));
        InstructionHandle ih_56 = il.append(_factory.createStore(Type.OBJECT, 5));
        InstructionHandle ih_58 = il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "isLazyNull", Type.BOOLEAN, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        BranchInstruction ifeq_62 = _factory.createBranchInstruction(Constants.IFEQ, null);
        il.append(ifeq_62);
        il.append(InstructionConstants.ACONST_NULL);
        BranchInstruction goto_66 = _factory.createBranchInstruction(Constants.GOTO, null);
        il.append(goto_66);
        InstructionHandle ih_69 = il.append(_factory.createLoad(Type.OBJECT, 1));
        InstructionHandle ih_70 = il.append(_factory.createStore(Type.OBJECT, 6));
        InstructionHandle ih_72 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createFieldAccess(_className, propertyName + "Collection", new ObjectType("org.bhavaya.collection.BeanCollection"), Constants.PUTFIELD));
        InstructionHandle ih_77 = il.append(_factory.createLoad(Type.INT, 4));
        BranchInstruction ifne_79 = _factory.createBranchInstruction(Constants.IFNE, null);
        il.append(ifne_79);
        il.append(_factory.createLoad(Type.INT, 2));
        BranchInstruction ifne_83 = _factory.createBranchInstruction(Constants.IFNE, null);
        il.append(ifne_83);
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(new PUSH(_cp, propertyName + "Collection"));
        il.append(_factory.createLoad(Type.OBJECT, 5));
        il.append(_factory.createLoad(Type.OBJECT, 6));
        il.append(_factory.createInvoke(_className, "firePropertyChange", Type.VOID, new Type[]{Type.STRING, Type.OBJECT, Type.OBJECT}, Constants.INVOKEVIRTUAL));
        InstructionHandle ih_96 = il.append(_factory.createReturn(Type.VOID));
        ifnull_17.setTarget(ih_44);
        ifne_22.setTarget(ih_44);
        ifeq_26.setTarget(ih_44);
        ifeq_48.setTarget(ih_55);
        goto_52.setTarget(ih_56);
        ifeq_62.setTarget(ih_69);
        goto_66.setTarget(ih_70);
        ifne_79.setTarget(ih_96);
        ifne_83.setTarget(ih_96);
        method.setMaxStack();
        method.setMaxLocals();
        _cg.addMethod(method.getMethod());
        il.dispose();
    }

}
