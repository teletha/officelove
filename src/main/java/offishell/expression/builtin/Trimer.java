/*
 * Copyright (C) 2022 The OFFISHELL Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.expression.builtin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import offishell.expression.ExpressionResolver;

/**
 * @version 2016/07/13 15:50:59
 */
class Trimer implements ExpressionResolver<String> {

    /** The range format. */
    private static final Pattern range = Pattern.compile("noBreak");

    /**
     * {@inheritDoc}
     */
    @Override
    public Matcher match(String expression) {
        return range.matcher(expression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolve(Matcher matcher, String value) {
        return value.replaceAll("\\s+", "　");
    }
}