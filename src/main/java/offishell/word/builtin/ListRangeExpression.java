/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.word.builtin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import offishell.word.ExpressionResolver;

/**
 * @version 2016/06/17 15:35:07
 */
class ListRangeExpression implements ExpressionResolver<List> {

    /** The range format. */
    private static final Pattern range = Pattern.compile("(\\d+)?[~～](\\d+)?");

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
        int start = parse(matcher.group(1), 0, v -> 0 <= v);
        int end = parse(matcher.group(2), list.size() - 1, v -> v < list.size());

        return list.subList(start, end + 1);
    }
}