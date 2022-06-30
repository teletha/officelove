/*
 * Copyright (C) 2022 The OFFISHELL Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression.builtin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import officelove.expression.ExpressionResolver;

/**
 * @version 2016/06/17 15:35:07
 */
class ListExpression implements ExpressionResolver<List> {

    /** The range format. */
    private static final Pattern range = Pattern.compile("(\\d+)");

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
    public Object resolve(Matcher matcher, List list) {
        int index = parse(matcher.group(1), 1, v -> 1 <= v);

        return list.size() < index ? null : list.get(index - 1);
    }
}