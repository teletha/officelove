/*
 * Copyright (C) 2024 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officelove.word;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("serial")
public class MergeableList<T> extends ArrayList<T> {

    /**
     * @param c
     */
    private MergeableList(Collection<? extends T> c) {
        super(c);
    }

    public static <T> List<T> of(List<T> list) {
        return new MergeableList(list);
    }
}
