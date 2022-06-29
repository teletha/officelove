/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.word;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class LoopTest implements WordTestSupport {

    @Test
    void loop() {
        assert verifyBody(word("loop").evaluate(items(1, 5)), word("loop.expected"));
    }

    static Items items(int start, int end) {
        Items root = new Items();
        for (int i = start; i <= end; i++) {
            root.items.add(new Item(i));
        }
        return root;
    }

    static class Items {
        public List<Item> items = new ArrayList();
    }

    static class Item {
        public int id;

        private Item(int id) {
            this.id = id;
        }
    }
}
