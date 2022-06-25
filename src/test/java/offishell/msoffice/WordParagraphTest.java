/*
 * Copyright (C) 2022 The OFFISHELL Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.msoffice;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import offishell.word.Word;

public class WordParagraphTest implements WordTestHelper {

    @Test
    @Disabled
    public void testname() throws IOException {
        Word.blank().header("Head1").text("Text1").text("Text1A").section(section -> {
            section.landscape().height(400).width(595).margin(50).head("Head2");
        }).text("Text2").open();
    }

    @Test
    public void singleLine() {
        Word word = Word.blank().text("Single Line");
        Word expected = expect("Paragraph - Single Line.docx");

        checkBody(word, expected);
    }
}