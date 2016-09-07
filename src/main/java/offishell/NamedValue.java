/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell;

import java.util.function.Function;

/**
 * @version 2016/07/29 11:30:15
 */
public interface NamedValue<V> extends Function<String, V> {

    /**
     * <p>
     * Retrieve the key.
     * </p>
     * 
     * @return A key string.
     */
    default String name() {
        return SinobuExperimental.method(this);
    }

    /**
     * <p>
     * Retrieve the value.
     * </p>
     * 
     * @return A value.
     */
    default V value() {
        return apply(null);
    }
}
