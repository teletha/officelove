/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officelove.word;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import kiss.I;
import officelove.expression.ExpressionException;
import officelove.expression.Parser;
import officelove.expression.Variable;

class ParserTest {

    static {
        I.load(ParserTest.class);
    }

    @Test
    void property() {
        Parser parser = new Parser(new Person("one", 1));
        assert parser.resolve("name").equals("one");
        assert parser.resolve("age").equals(1);
    }

    @Test
    void list() {
        Parser parser = new Parser(new Group(new Person("one", 1), new Person("two", 2)));
        assert parser.resolve("0.name").equals("one");
        assert parser.resolve("1.age").equals(2);
    }

    @Test
    void notFound() {
        Parser parser = new Parser(new Person("one", 1));
        Assertions.assertThrows(ExpressionException.class, () -> parser.resolve("none"));
    }

    @Test
    void optional() {
        Parser parser = new Parser(new Person("one", 1));
        assert parser.resolve("none?").equals("");
    }

    @Test
    void space() {
        Parser parser = new Parser(new Group(new Person("one", 1), new Person("two", 2)));
        assert parser.resolve(" 0 . name ").equals("one");
    }

    @Test
    void variable() {
        Parser parser = new Parser();
        assert parser.resolve("$var").equals("variable");
    }

    /**
     * Test model.
     */
    static class Person {

        public String name;

        public int age;

        private Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * Test model.
     */
    @SuppressWarnings("serial")
    static class Group extends ArrayList<Person> {

        Group(Person... member) {
            for (Person mem : member) {
                add(mem);
            }
        }
    }

    public static class Var implements Variable<String> {

        /**
         * {@inheritDoc}
         */
        @Override
        public String apply(String expression) {
            return "variable";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean test(String expression) {
            return expression.equals("var");
        }
    }
}
