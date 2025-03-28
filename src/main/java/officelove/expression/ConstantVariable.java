/*
 * Copyright (C) 2025 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import kiss.I;

public interface ConstantVariable extends Variable<Object> {

    /**
     * Retrieve the constant value.
     * 
     * @param name A value name.
     * @return A constant value.
     */
    @Override
    default boolean test(String name) {
        try {
            Field field = getClass().getField(name);
            int modifier = field.getModifiers();

            return Modifier.isStatic(modifier) && Modifier.isFinal(modifier);
        } catch (Exception e) {
            try {
                Method method = getClass().getMethod(name);
                int modifier = method.getModifiers();

                return Modifier.isStatic(modifier);
            } catch (Exception x) {
                throw I.quiet(x);
            }
        }
    }

    /**
     * Retrieve the constant value.
     * 
     * @param name A value name.
     * @return A constant value.
     */
    @Override
    default Object apply(String name) {
        try {
            return getClass().getField(name).get(null);
        } catch (Exception e) {
            try {
                return getClass().getMethod(name).invoke(null);
            } catch (Exception x) {
                throw I.quiet(x);
            }
        }
    }
}