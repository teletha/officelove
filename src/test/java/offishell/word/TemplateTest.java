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

import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class TemplateTest extends WordTestSupport {

    @TestFactory
    List<DynamicTest> preserveEmptyLine() {
        return verifyAllDocx("preserveEmptyLine");
    }

    @TestFactory
    List<DynamicTest> style() {
        return verifyAllDocx("style", new Text("success"));
    }

    @TestFactory
    List<DynamicTest> styleInLoop() {
        return verifyAllDocx("loop/style", new ListLoop(new Text("item1"), new Text("item2"), new Text("item3")));
    }

    /**
     * Simple text value.
     */
    static class Text {

        public String text;

        Text(String text) {
            this.text = text;
        }
    }

    /**
     * Loop by list.
     */
    static class ListLoop {
        public List<Text> list;

        ListLoop(Text... items) {
            this.list = List.of(items);
        }
    }
}
