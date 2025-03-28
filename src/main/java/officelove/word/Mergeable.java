/*
 * Copyright (C) 2025 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.word;

import java.util.function.Supplier;

/**
 * Marker interface.
 */
public interface Mergeable<T> extends Iterable<T> {

    /**
     * Wrap {@link Iterable} as {@link Mergeable}.
     * 
     * @param <T>
     * @param iterable
     * @return
     */
    static <T> Mergeable<T> of(Iterable<T> iterable) {
        return iterable::iterator;
    }

    /**
     * Wrap {@link Iterable} as {@link Mergeable}.
     * 
     * @param <T>
     * @param iterable
     * @return
     */
    static <T> Supplier<Mergeable<T>> by(Iterable<T> iterable) {
        return () -> of(iterable);
    }
}