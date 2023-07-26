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

import java.util.function.Function;
import java.util.function.Predicate;

import kiss.Extensible;

/**
 * Define global variables that can be used in the template.
 */
public interface Variable<T> extends Extensible, Function<String, T>, Predicate<String> {
}