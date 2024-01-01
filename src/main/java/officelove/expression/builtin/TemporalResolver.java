/*
 * Copyright (C) 2024 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression.builtin;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import officelove.expression.ExpressionResolver;

public class TemporalResolver implements ExpressionResolver<Temporal> {

    /** The range format. */
    private static final Pattern PATTERN = Pattern.compile("([+-]\\d+)(year|month|day|hour|min|sec|年|月|日|時間|時|分|秒)");

    /**
     * {@inheritDoc}
     */
    @Override
    public Matcher match(String expression) {
        return PATTERN.matcher(expression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolve(Matcher matcher, Temporal value) {
        int num = Integer.parseInt(matcher.group(1));
        ChronoUnit unit = switch (matcher.group(2)) {
        case "sec", "秒" -> ChronoUnit.SECONDS;
        case "min", "分" -> ChronoUnit.MINUTES;
        case "hour", "時間", "時" -> ChronoUnit.HOURS;
        case "day", "日" -> ChronoUnit.DAYS;
        case "month", "月" -> ChronoUnit.MONTHS;
        case "year", "年" -> ChronoUnit.YEARS;
        default -> throw new IllegalArgumentException("Unexpected value: " + matcher.group(2));
        };
        return value.plus(num, unit);
    }
}