/*
 * Copyright (C) 2025 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import kiss.I;

class ParserTest {

    static {
        I.load(Parser.class);
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
    void none() {
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
        Parser parser = new Parser(new Person("one", 1));
        assert parser.resolve("$var").equals("variable");
    }

    @Test
    void method() {
        Parser parser = new Parser(new MethodCall());
        assert parser.resolve("text").equals("text");
        assert parser.resolve("text(World)").equals("Hello World");
        assert parser.resolve("sum(1,2)").equals(3);
        assert parser.resolve("sum(1, 2)").equals(3);
        assert parser.resolve("sum( 1 , 2 )").equals(3);
    }

    @Test
    void intResolver() {
        Parser parser = new Parser(new Person("one", 10));
        assert parser.resolve("age + 1").equals(BigDecimal.valueOf(11));
        assert parser.resolve("age - 1").equals(BigDecimal.valueOf(9));
        assert parser.resolve("age * 10").equals(BigDecimal.valueOf(100));
        assert parser.resolve("age / 2").equals(BigDecimal.valueOf(5));
        assert parser.resolve("age % 3").equals(BigDecimal.valueOf(1));

        assert parser.resolve("age + 1 * 10").equals(BigDecimal.valueOf(110));
    }

    @Test
    void decimalResolver() {
        Parser parser = new Parser(new Person("one", 10));
        assert parser.resolve("age + 1.1").equals(BigDecimal.valueOf(11.1));
        assert parser.resolve("age - 1.1").equals(BigDecimal.valueOf(8.9));
        assert parser.resolve("age * 1.1").equals(BigDecimal.valueOf(11d));
        assert parser.resolve("age / 0.5").equals(BigDecimal.valueOf(20));
    }

    @Test
    void temporalResolver() {
        Parser parser = new Parser(new TemporalValue(LocalTime.of(10, 30)));
        assert parser.resolve("time - 10min").equals(LocalTime.of(10, 20));
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

    /**
     * Test model.
     */
    static class Var implements Variable<String> {

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

    /**
     * Test model.
     */
    static class MethodCall {

        public String text() {
            return "text";
        }

        public String text(String name) {
            return "Hello " + name;
        }

        public int sum(int first, int second) {
            return first + second;
        }
    }

    /**
     * Test model.
     */
    static class TemporalValue {

        public LocalTime time;

        /**
         * @param time
         */
        TemporalValue(LocalTime time) {
            this.time = time;
        }
    }
}