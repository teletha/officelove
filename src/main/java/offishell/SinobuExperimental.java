/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell;

import java.lang.reflect.Method;

import sun.reflect.ConstantPool;

import jdk.internal.org.objectweb.asm.Type;
import kiss.ClassVariable;
import kiss.I;

/**
 * @version 2016/07/29 11:29:32
 */
class SinobuExperimental {

    /** The holder for lambda parameter names. */
    private static final ClassVariable<String> methods = new ClassVariable();

    /** The accessible internal method for lambda info. */
    private static final Method findConstants;

    static {
        try {
            // reflect lambda info related methods
            findConstants = Class.class.getDeclaredMethod("getConstantPool");
            findConstants.setAccessible(true);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Findthe first parameter name of lambda method.
     * </p>
     * 
     * @param object A lambda instance.
     * @return A parameter name.
     */
    static String method(Object object) {
        Class clazz = object.getClass();
        String name = methods.get(clazz);

        if (name == null) {
            try {
                ConstantPool constantPool = (ConstantPool) findConstants.invoke(clazz);

                // MethodInfo
                // [0] : Declared Class Name (internal qualified name)
                // [1] : Method Name
                // [2] : Method Descriptor (internal qualified signature)
                String[] info = constantPool.getMemberRefInfoAt(constantPool.getSize() - 3);
                Class lambda = I.type(info[0].replaceAll("/", "."));
                Type[] types = Type.getArgumentTypes(info[2]);
                Class[] params = new Class[types.length];

                for (int i = 0; i < params.length; i++) {
                    params[i] = I.type(types[i].getClassName());
                }
                name = lambda.getDeclaredMethod(info[1], params).getParameters()[0].getName();

                methods.set(clazz, name);
            } catch (Exception e) {
                throw I.quiet(e);
            }
        }
        return name;
    }
}
