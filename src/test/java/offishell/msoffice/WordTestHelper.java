/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.msoffice;

import java.util.List;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import offishell.word.Word;
import psychopath.Locator;

/**
 * @version 2016/09/07 11:05:14
 */
public interface WordTestHelper extends AssertionHelper {

    public default Word expect(String name) {
        return new Word(Locator.file("src/test/resources/offishell/msoffice/" + name));
    }

    /**
     * <p>
     * Assertion helper.
     * </p>
     * 
     * @param word
     * @param expectedWord
     */
    public default void checkBody(Word word, Word expectedWord) {
        assertin(word, expectedWord, (w, value) -> {
            XWPFDocument document = w.docment();
            List<IBodyElement> elements = document.getBodyElements();

            assert elements != null;
            assert value.of(elements.size());

            for (IBodyElement element : elements) {
                if (element instanceof XWPFParagraph) {
                    assert value.of((XWPFParagraph) element, this::checkParagraph);
                }
            }
        });
    }

    /**
     * <p>
     * Assertion helper.
     * </p>
     * 
     * @param para
     * @param value
     */
    public default void checkParagraph(XWPFParagraph para, Assertion value) {
        List<XWPFRun> runs = para.getRuns();

        assert runs != null;
        assert value.of(runs.size());

        for (XWPFRun run : runs) {
            assert value.of(run, this::checkRun);
        }
    }

    /**
     * <p>
     * Assertion helper.
     * </p>
     * 
     * @param value
     */
    public default void checkRun(XWPFRun run, Assertion value) {
        assert value.of(run.text());
        assert value.of(run.getFontFamily());
        assert value.of(run.getFontSize());
        assert value.of(run.isBold());
        assert value.of(run.isCapitalized());
        assert value.of(run.isDoubleStrikeThrough());
        assert value.of(run.isEmbossed());
        assert value.of(run.isHighlighted());
        assert value.of(run.isImprinted());
    }
}