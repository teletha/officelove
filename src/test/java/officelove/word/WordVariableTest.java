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

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
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

    @Test
    void loopInTableRow() {
        Word word = new Word(base.file("loopTable.docx")).evaluate(new Items());
        find(word, "keyTop1", "valueTop1", "keyBottom1", "valueBottom1", "keyTop2", "valueTop2", "keyBottom2", "valueBottom2", "keyTop3", "valueTop3", "keyBottom3", "valueBottom3");
    }

    /**
     * <p>
     * Helper method to find paragraph sequencially.
     * </p>
     * 
     * @param word
     * @param texts
     */
    private static XWPFParagraph find(Word word, String... texts) {
        return find(word, word.paragraphs().take(1).to().get(), texts);
    }

    /**
     * <p>
     * Helper method to find paragraph sequencially.
     * </p>
     * 
     * @param word
     * @param texts
     */
    private static XWPFParagraph find(Word word, XWPFParagraph start, String... texts) {
        for (String text : texts) {
            start = word.paragraphWith(text, start);
            assert start != null;
        }
        return start;
    }

    /**
     * @version 2016/06/04 14:19:39
     */
    private static class Items {

        public List<Item> items = new ArrayList();

        private Items() {
            items.add(new Item(1));
            items.add(new Item(2));
            items.add(new Item(3));
        }
    }

    /**
     * @version 2016/06/04 10:15:18
     */
    @SuppressWarnings("unused")
    private static class Item {

        public int id;

        /**
         * @param id
         */
        private Item(int id) {
            this.id = id;
        }
    }
}
