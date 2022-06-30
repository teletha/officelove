/*
 * Copyright (C) 2022 The OFFISHELL Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression;

import java.util.function.IntPredicate;
import java.util.regex.Matcher;

import kiss.Extensible;

/**
 * @version 2016/06/17 15:32:28
 */
public interface ExpressionResolver<T> extends Extensible {

    /**
     * <p>
     * Test the specified expression.
     * </p>
     * 
     * @param expression
     * @return
     */
    Matcher match(String expression);

    /**
     * <p>
     * Resolve the specified value by the expression.
     * </p>
     * 
     * @param matcher
     * @param value
     * @return
     */
    Object resolve(Matcher matcher, T value);

    /**
     * <p>
     * Helper method to parse value as {@link Integer}.
     * </p>
     * 
     * @param value
     * @param defalutValue
     * @return
     */
    default int parse(String value, int defalutValue) {
        return parse(value, defalutValue, v -> true);
    }

    /**
     * <p>
     * Helper method to parse value as {@link Integer}.
     * </p>
     * 
     * @param value
     * @param defalutValue
     * @param restriction
     * @return
     */
    default int parse(String value, int defalutValue, IntPredicate restriction) {
        try {
            int parsed = Integer.parseInt(value);

            return restriction.test(parsed) ? parsed : defalutValue;
        } catch (NumberFormatException e) {
            return defalutValue;
        }
    }
}