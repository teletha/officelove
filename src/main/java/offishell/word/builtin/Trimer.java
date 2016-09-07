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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import offishell.word.ExpressionResolver;

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
