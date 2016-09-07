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
 * @version 2016/06/17 15:45:01
 */
class LineExpression implements ExpressionResolver<String> {

    /** The line format. */
    private static final Pattern line = Pattern.compile("line(\\d+)");

    /**
     * {@inheritDoc}
     */
    @Override
    public Matcher match(String expression) {
        return line.matcher(expression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolve(Matcher matcher, String value) {
        int number = parse(matcher.group(1), 0);
        String[] lines = value.split("[\r\n]");

        if (number <= lines.length) {
            return lines[number - 1];
        } else {
            return "";
        }
    }
}
