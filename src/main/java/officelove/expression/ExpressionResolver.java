/*
 * Copyright (C) 2024 The OFFICELOVE Development Team
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

public interface ExpressionResolver<T> extends Extensible {

    /**
     * Test the specified expression.
     * 
     * @param expression
     * @return
     */
    Matcher match(String expression);

    /**
     * Resolve the specified value by the expression.
     * 
     * @param matcher
     * @param value
     * @return
     */
    Object resolve(Matcher matcher, T value);

    /**
     * Helper method to parse value as {@link Integer}.
     * 
     * @param value
     * @param defalutValue
     * @return
     */
    default int parse(String value, int defalutValue) {
        return parse(value, defalutValue, v -> true);
    }

    /**
     * Helper method to parse value as {@link Integer}.
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