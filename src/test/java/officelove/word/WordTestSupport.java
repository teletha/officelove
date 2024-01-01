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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.DynamicTest;

import psychopath.Locator;

public class WordTestSupport {

    /**
     * Create tester file.
     * 
     * @param fileName
     * @return
     */
    public Word word(String fileName) {
        return new Word(Locator.file("src/test/resources/officelove/word/" + fileName + ".docx"));
    }

    /**
     * Assertion helper.
     */
    public List<DynamicTest> verifyAllDocx(String directoryName, Object... context) {
        return verifyAllDocx(directoryName, context, false);
    }

    /**
     * Assertion helper.
     */
    public List<DynamicTest> verifyAllDocxReportable(String directoryName, Object... context) {
        return verifyAllDocx(directoryName, context, true);
    }

    /**
     * Assertion helper.
     */
    private List<DynamicTest> verifyAllDocx(String directoryName, Object[] context, boolean openErrorDoc) {
        return Locator.directory("src/test/resources/officelove/word/" + directoryName).walkFile("*.docx", "!*.expected.docx").map(file -> {
            return DynamicTest.dynamicTest(file.base(), () -> {
                assert verifyDocx(directoryName + "/" + file.base(), context);
            });
        }).toList();
    }

    /**
     * Assertion helper.
     * 
     * @param fileName
     * @param context
     */
    public boolean verifyDocx(String fileName, Object... context) {
        return verifyBody(word(fileName).evaluate(null, context), word(fileName.concat(".expected")));
    }

    /**
     * Assertion helper.
     * 
     * @param actual
     * @param expected
     */
    public boolean verifyBody(Word actual, Word expected) {
        return assertion(actual, expected, (w, value) -> {
            XWPFDocument document = w.docment();
            List<IBodyElement> elements = document.getBodyElements();

            assert elements != null;
            assert value.of(elements.size());

            for (IBodyElement element : elements) {
                if (element instanceof XWPFParagraph para) {
                    assert value.of(para, this::verifyParagraph);
                } else if (element instanceof XWPFTable table) {
                    assert value.of(table.getNumberOfRows());
                    for (XWPFTableRow row : table.getRows()) {
                        assert value.of(row.getTableCells().size());
                        for (XWPFTableCell cell : row.getTableCells()) {
                            assert value.of(cell.getParagraphs().size());
                            for (XWPFParagraph para : cell.getParagraphs()) {
                                assert value.of(para, this::verifyParagraph);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Assertion helper.
     * 
     * @param para
     * @param value
     */
    private void verifyParagraph(XWPFParagraph para, Assertion value) {
        List<XWPFRun> runs = para.getRuns();

        assert runs != null;
        assert value.of(runs.size());

        for (XWPFRun run : runs) {
            assert value.of(run, this::verifyRun);
        }
    }

    /**
     * Assertion helper.
     * 
     * @param value
     */
    private void verifyRun(XWPFRun run, Assertion value) {
        assert value.of(run.text());
        assert value.of(run.getFontFamily());
        assert value.of(run.getFontSizeAsDouble());
        assert value.of(run.isBold());
        assert value.of(run.isCapitalized());
        assert value.of(run.isDoubleStrikeThrough());
        assert value.of(run.isEmbossed());
        assert value.of(run.isHighlighted());
        assert value.of(run.isImprinted());
        assert value.of(run.isItalic());
        assert value.of(run.isShadowed());
        assert value.of(run.isSmallCaps());
        assert value.of(run.isStrikeThrough());
        assert value.of(run.isVanish());
        assert value.of(run.getColor());
        assert value.of(run.getUnderlineColor());
    }

    /**
     * Assertion helper.
     * 
     * @param actual
     * @param expected
     * @param checker
     */
    private boolean assertion(Word actual, Word expected, BiConsumer<Word, Assertion> checker) {
        assert actual != null;
        assert expected != null;
        assert checker != null;

        Assertion assertion = new Assertion();
        checker.accept(actual, assertion);

        assertion.collectMode = false;
        checker.accept(expected, assertion);

        return true;
    }

    /**
     * @version 2016/09/07 9:30:46
     */
    static class Assertion {

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
                latest = values.pop();
                assert Objects.equals(latest, expected);
                return true;
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

    /**
     * 
     */
    static interface EqualityChecker<T> {
        public void equal(T one, T expected);
    }
}