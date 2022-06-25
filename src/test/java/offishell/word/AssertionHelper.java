/*
 * Copyright (C) 2022 The OFFISHELL Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.word;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * @version 2016/09/07 11:03:11
 */
public interface AssertionHelper {

    /**
     * <p>
     * Assertion helper.
     * </p>
     * 
     * @param tester
     * @param expected
     * @param checker
     */
    public default <T> void assertin(T tester, T expected, BiConsumer<T, Assertion> checker) {
        assert tester != null;
        assert expected != null;
        assert checker != null;

        Assertion assertion = new Assertion();
        checker.accept(tester, assertion);

        assertion.collectMode = false;
        checker.accept(expected, assertion);
    }

    /**
     * @version 2016/09/07 9:30:46
     */
    public static class Assertion {

        /** Assertion mode. */
        private boolean collectMode = true;

        /** The value store. */
        private Deque values = new LinkedList();

        /** The latest checked value. */
        private Object latest;

        /**
         * Assert value.
         * 
         * @param expected
         * @return
         */
        public boolean of(Object expected) {
            if (collectMode) {
                return values.add(expected);
            } else {
                return Objects.equals(latest = values.pop(), expected);
            }
        }

        /**
         * Assert value.
         * 
         * @param expected
         * @return
         */
        public <T> boolean of(T expected, BiConsumer<T, Assertion> checker) {
            checker.accept(expected, this);

            return true;
        }

        /**
         * Assert value.
         * 
         * @param expected
         * @return
         */
        public <T> boolean of(T expected, EqualityChecker<T> checker) {
            if (collectMode) {
                return values.add(expected);
            } else {
                checker.equal((T) (latest = values.pop()), expected);

                return true;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.valueOf(latest);
        }
    }

    public static interface EqualityChecker<T> {

        public void equal(T one, T expected);
    }
}