/*
 * Copyright (C) 2024 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.word;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import psychopath.Directory;
import psychopath.Locator;

class TemplateDefinitionTest extends WordTestSupport {

    private Definitions defs = new Definitions();

    @Test
    void name() {
        assert defs.NoContext.name().equals("NoContext");
        assert defs.OneContext.name().equals("OneContext");
    }

    @Test
    void type() {
        assert defs.NoContext.types().isEmpty();
        assert defs.OneContext.types().get(0) == String.class;
    }

    @Test
    void types() {
        assert defs.TwoContexts.types().get(0) == String.class;
        assert defs.TwoContexts.types().get(1) == Locale.class;
    }

    @Test
    void mergeable() {
        assert defs.MergeableContext.types().get(0) == String.class;
        assert defs.MergeableContext.types().get(1) == Locale.class;
    }

    /**
     * For test.
     */
    private class Definitions extends TemplateDefinitions<Object> {

        public final Template NoContext = new Template();

        public final Template1<String> OneContext = new Template1();

        public final Template2<String, Locale> TwoContexts = new Template2();

        public final Template2<Mergeable<String>, Locale> MergeableContext = new Template2();

        /**
         * {@inheritDoc}
         */
        @Override
        protected Directory locate() {
            return Locator.directory("src/test/resources/officelove/word/definitions");
        }
    }
}