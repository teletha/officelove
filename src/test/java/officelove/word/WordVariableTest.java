/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officelove.word;

import org.junit.jupiter.api.Test;

import kiss.I;
import officelove.expression.Variable;
import psychopath.Directory;
import psychopath.Locator;

class WordVariableTest {

    static {
        I.load(WordVariableTest.class);
    }

    private static Directory base = Locator.directory("src/test/resources/officelove/word");

    @Test
    void field() {
        Word word = new Word(base.file("variable.docx"));
        assert word.paragraphWith("value") == null;
        assert word.evaluate(new Field()).paragraphWith("value") != null;
    }

    @Test
    void table() {
        Word word = new Word(base.file("variable.docx"));
        assert word.paragraphWith("table value") == null;
        assert word.evaluate(new Field()).paragraphWith("table value") != null;
    }

    static class Field {

        public String publicField = "value";

        public String inTable = "table value";
    }

    @Test
    void builtin() {
        Word word = new Word(base.file("builtinVariable.docx"));
        assert word.evaluate(new Field()).paragraphWith("built-in-value") != null;
    }

    static class Builin implements Variable<String> {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean test(String name) {
            return name.equalsIgnoreCase("builtin");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String apply(String name) {
            return "built-in-value";
        }
    }

    @Test
    void conditionTrue() {
        assert new Word(base.file("condition.docx")).evaluate(new Yes()).paragraphWith("show") != null;
    }

    @Test
    void conditionFalse() {
        assert new Word(base.file("condition.docx")).evaluate(new No()).paragraphWith("show") == null;
    }

    static class Yes {

        public boolean condition = true;
    }

    static class No {

        public boolean condition = false;
    }
}
