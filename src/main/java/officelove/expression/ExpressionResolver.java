/*
 * Copyright (C) 2023 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression;

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
}