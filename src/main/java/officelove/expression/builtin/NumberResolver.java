/*
 * Copyright (C) 2025 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression.builtin;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import officelove.expression.ExpressionResolver;

public class NumberResolver implements ExpressionResolver<Number> {

    /** The range format. */
    private static final Pattern PATTERN = Pattern.compile("([+\\-*/%])([0-9.]+)");

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
    public Object resolve(Matcher matcher, Number value) {
        char op = matcher.group(1).charAt(0);
        BigDecimal one = BigDecimal.ZERO;
        BigDecimal other = new BigDecimal(matcher.group(2));

        if (value instanceof Integer num) {
            one = new BigDecimal(num);
        } else if (value instanceof Long num) {
            one = new BigDecimal(num);
        } else if (value instanceof Float num) {
            one = new BigDecimal(num);
        } else if (value instanceof Double num) {
            one = new BigDecimal(num);
        } else if (value instanceof BigInteger num) {
            one = new BigDecimal(num);
        } else if (value instanceof BigDecimal num) {
            one = num;
        }

        switch (op) {
        case '+':
            value = one.add(other);
            break;
        case '-':
            value = one.subtract(other);
            break;
        case '*':
            value = one.multiply(other);
            break;
        case '/':
            value = one.divide(other, RoundingMode.HALF_DOWN);
            break;
        case '%':
            value = one.remainder(other);
            break;
        }
        return value;
    }
}