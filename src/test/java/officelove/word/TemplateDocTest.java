/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officelove.word;

import java.util.List;

import org.junit.jupiter.api.Test;

class TemplateDocTest {

    @Test
    void template() {
        TemplateDoc doc = new TemplateDoc("src/test/resources/word/none.fodt");
        List<DocExpression> list = doc.getExpression();
        assert list.size() == 0;
    }
}
