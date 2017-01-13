package org.bhavaya.beans.generator;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;
import org.bhavaya.util.Utilities;

public class CreateProperty3 implements ByteCodePropertyGenerator {
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
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, new ObjectType(propertyTypeName), Type.NO_ARGS, new String[]{}, (propertyType == Type.BOOLEAN ? "is" : "get") + Utilities.capitalise(propertyName), _className, il, _cp);

        InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName, new ObjectType(propertyTypeName), Constants.GETFIELD));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "isLazy", Type.BOOLEAN, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        BranchInstruction ifeq_7 = _factory.createBranchInstruction(Constants.IFEQ, null);
        il.append(ifeq_7);
        InstructionHandle ih_10 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName, new ObjectType(propertyTypeName), Constants.GETFIELD));
        il.append(_factory.createCheckCast(new ObjectType("org.bhavaya.util.LoadClosure")));
        il.append(_factory.createInvoke("org.bhavaya.util.LoadClosure", "load", Type.OBJECT, Type.NO_ARGS, Constants.INVOKEINTERFACE));
        il.append(_factory.createCheckCast(new ObjectType(propertyTypeName)));
        il.append(_factory.createInvoke(_className, "set" + Utilities.capitalise(propertyName), Type.VOID, new Type[]{new ObjectType(propertyTypeName)}, Constants.INVOKEVIRTUAL));
        InstructionHandle ih_29 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName, new ObjectType(propertyTypeName), Constants.GETFIELD));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "isLazyNull", Type.BOOLEAN, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        BranchInstruction ifeq_36 = _factory.createBranchInstruction(Constants.IFEQ, null);
        il.append(ifeq_36);
        InstructionHandle ih_39 = il.append(InstructionConstants.ACONST_NULL);
        il.append(_factory.createReturn(Type.OBJECT));
        InstructionHandle ih_41 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName, new ObjectType(propertyTypeName), Constants.GETFIELD));
        InstructionHandle ih_45 = il.append(_factory.createReturn(Type.OBJECT));
        ifeq_7.setTarget(ih_29);
        ifeq_36.setTarget(ih_41);
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
        MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, new Type[]{new ObjectType(propertyTypeName)}, new String[]{"arg0"}, "set" + Utilities.capitalise(propertyName), _className, il, _cp);

        InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createInvoke("org.bhavaya.beans.BeanFactory", "isLazy", Type.BOOLEAN, new Type[]{Type.OBJECT}, Constants.INVOKESTATIC));
        il.append(_factory.createStore(Type.INT, 2));
        InstructionHandle ih_5 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createFieldAccess(_className, propertyName, new ObjectType(propertyTypeName), Constants.GETFIELD));
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
        il.append(_factory.createCheckCast(new ObjectType(propertyTypeName)));
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
        InstructionHandle ih_72 = il.append(_factory.createLoad(Type.INT, 4));
        BranchInstruction ifeq_74 = _factory.createBranchInstruction(Constants.IFEQ, null);
        il.append(ifeq_74);
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(InstructionConstants.ACONST_NULL);
        il.append(_factory.createFieldAccess(_className, propertyName, new ObjectType(propertyTypeName), Constants.PUTFIELD));
        InstructionHandle ih_82 = il.append(_factory.createLoad(Type.INT, 2));
        BranchInstruction ifne_83 = _factory.createBranchInstruction(Constants.IFNE, null);
        il.append(ifne_83);
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(Type.OBJECT, 6));
        il.append(_factory.createInvoke(_superclassName, "set" + Utilities.capitalise(propertyName), Type.VOID, new Type[]{new ObjectType(propertyTypeName)}, Constants.INVOKESPECIAL));
        InstructionHandle ih_92 = il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(_factory.createLoad(Type.OBJECT, 1));
        il.append(_factory.createFieldAccess(_className, propertyName, new ObjectType(propertyTypeName), Constants.PUTFIELD));
        InstructionHandle ih_97 = il.append(_factory.createLoad(Type.INT, 4));
        BranchInstruction ifne_99 = _factory.createBranchInstruction(Constants.IFNE, null);
        il.append(ifne_99);
        il.append(_factory.createLoad(Type.INT, 2));
        BranchInstruction ifne_103 = _factory.createBranchInstruction(Constants.IFNE, null);
        il.append(ifne_103);
        il.append(_factory.createLoad(Type.OBJECT, 0));
        il.append(new PUSH(_cp, propertyName));
        il.append(_factory.createLoad(Type.OBJECT, 5));
        il.append(_factory.createLoad(Type.OBJECT, 6));
        il.append(_factory.createInvoke(_className, "firePropertyChange", Type.VOID, new Type[]{Type.STRING, Type.OBJECT, Type.OBJECT}, Constants.INVOKEVIRTUAL));
        InstructionHandle ih_116 = il.append(_factory.createReturn(Type.VOID));
        ifnull_17.setTarget(ih_44);
        ifne_22.setTarget(ih_44);
        ifeq_26.setTarget(ih_44);
        ifeq_48.setTarget(ih_55);
        goto_52.setTarget(ih_56);
        ifeq_62.setTarget(ih_69);
        goto_66.setTarget(ih_70);
        ifeq_74.setTarget(ih_82);
        ifne_83.setTarget(ih_92);
        ifne_99.setTarget(ih_116);
        ifne_103.setTarget(ih_116);
        method.setMaxStack();
        method.setMaxLocals();
        _cg.addMethod(method.getMethod());
        il.dispose();
    }

}
