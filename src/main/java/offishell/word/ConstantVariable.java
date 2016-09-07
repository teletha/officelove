/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.word;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import kiss.I;

/**
 * @version 2016/07/13 14:04:13
 */
public interface ConstantVariable extends Variable<Object> {

    /**
     * <p>
     * Retrieve the constant value.
     * </p>
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
            return false;
        }
    }

    /**
     * <p>
     * Retrieve the constant value.
     * </p>
     * 
     * @param name A value name.
     * @return A constant value.
     */
    @Override
    default Object apply(String name) {
        try {
            return getClass().getField(name).get(null);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }
}
