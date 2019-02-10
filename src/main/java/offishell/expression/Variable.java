/*
 * Copyright (C) 2019 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.expression;

import java.util.function.Function;
import java.util.function.Predicate;

import kiss.Extensible;

/**
 * @version 2016/06/04 18:54:18
 */
public interface Variable<T> extends Extensible, Function<String, T>, Predicate<String> {

}
