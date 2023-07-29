/*
 * Copyright (C) 2023 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.word;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.apache.poi.xwpf.usermodel.BodyType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.impl.values.XmlValueDisconnectedException;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTextDirection;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTextDirection;

import kiss.I;
import kiss.Observer;
import kiss.Signal;
import kiss.WiseSupplier;
import officelove.LibreOffice;
import officelove.expression.Parser;
import psychopath.File;
import psychopath.Locator;

public class Word {

    static {
        I.load(LibreOffice.class);
    }

    /** The culculated document. */
    protected XWPFDocument calculated;

    /** The template file name. */
    private final String name;

    /** The context. */
    private final CalculationContext context = new CalculationContext();

    /** The text direction. */
    private boolean textIsVerticalAlign;

    /** The next break type. */
    private BreakType breakType = BreakType.None;

    /** The current section. */
    private Section section = new Section();

    /**
     * Create empty doument.
     */
    public Word() {
        this("empty.docx");
    }

    /**
     * Create template for word.
     */
    private Word(String name) {
        this.name = name;
        this.calculated = new XWPFDocument();

        CTSectPr sect = calculated.getDocument().getBody().getSectPr();
        section.getSize(sect);
        section.getMargin(sect);
    }

    /**
     * Create template for word.
     * 
     * @param file
     */
    public Word(File file) {
        this(file.absolutize().toString(), file::newInputStream);
    }

    /**
     * Create template for word.
     * 
     * @param file
     */
    public Word(URL file) {
        this(URLDecoder.decode(file.toString(), StandardCharsets.UTF_8), file::openStream);
    }

    /**
     * Create template for word.
     * 
     * @param name
     * @param input
     */
    private Word(String name, WiseSupplier<InputStream> input) {
        try {
            this.name = name;
            this.calculated = new XWPFDocument(input.get());

            CTTextDirection direction = calculated.getDocument().getBody().getSectPr().getTextDirection();

            if (direction != null) {
                this.textIsVerticalAlign = direction.getVal() == STTextDirection.TB_RL;
            } else {
                this.textIsVerticalAlign = false;
            }
        } catch (Throwable e) {
            throw I.quiet(e);
        }
    }

    /**
     * Expose POI API.
     * 
     * @return POI document.
     */
    public XWPFDocument docment() {
        return calculated;
    }

    public boolean validate(List<Class> models) {
        context.parser = new Parser(models);

        replace(calculated);

        return true;
    }

    /**
     * Collect paragraphs.
     * 
     * @return
     */
    public Signal<XWPFParagraph> paragraphs() {
        return new Signal<XWPFParagraph>((observer, disposer) -> {
            for (IBodyElement element : calculated.getBodyElements()) {
                collectParagraph(element, observer);
            }
            return disposer;
        });
    }

    /**
     * Helper method to find {@link XWPFParagraph} which contains the specified text.
     * 
     * @param text A text to search.
     * @return A serach result.
     */
    public XWPFParagraph paragraphWith(String text) {
        List<XWPFParagraph> list = paragraphs().take(p -> p.getText().contains(text)).toList();

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Helper method to find {@link XWPFParagraph} which contains the specified text.
     * 
     * @param text A text to search.
     * @param start A start position to search.
     * @return A serach result.
     */
    public XWPFParagraph paragraphWith(String text, XWPFParagraph start) {
        List<XWPFParagraph> list = paragraphs().skipUntil(start::equals).take(p -> p.getText().contains(text)).toList();

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Copy all pages.
     */
    public Word copy(int number) {
        List<IBodyElement> copy = copy(calculated.getBodyElements());

        for (int i = 0; i < number - 1; i++) {
            merge(copy);
        }
        return this;
    }

    /**
     * Helper method to get cusor.
     * 
     * @param e
     * @return
     */
    private XmlCursor cursorAfter(IBodyElement e) {
        if (e instanceof XWPFParagraph) {
            XmlCursor cursor = ((XWPFParagraph) e).getCTP().newCursor();
            cursor.toNextSibling();
            return cursor;
        } else if (e instanceof XWPFTable) {
            XmlCursor cursor = ((XWPFTable) e).getCTTbl().newCursor();
            cursor.toNextSibling();
            return cursor;
        } else {
            // If this exception will be thrown, it is bug of this program. So we must rethrow the
            // wrapped error in here.
            throw new Error();
        }
    }

    /**
     * Helper method to get cusor.
     * 
     * @param e
     * @return
     */
    private XmlCursor cursorLast() {
        List<IBodyElement> elements = calculated.getBodyElements();

        if (elements.isEmpty()) {
            XmlCursor cursor = calculated.getDocument().newCursor();
            cursor.toStartDoc();
            return cursor;
        } else {
            return cursorAfter(elements.get(elements.size() - 1));
        }
    }

    /**
     * Calculate variables by the given model.
     * 
     * @param model One model.
     * @param others Other models.
     * @return
     */
    public Word evaluate(Object model, Object... others) {
        ArrayList list = new ArrayList();
        list.add(model);
        list.addAll(Arrays.asList(others));

        return evaluate(list);
    }

    /**
     * Calculate variables by the given model.
     * 
     * @param models
     * @return
     */
    public Word evaluate(List models) {
        // calculate variables
        context.parser = new Parser(textIsVerticalAlign, models);

        try {
            replace(calculated);

            // clear all comments
            WordHeleper.clearComment(calculated);

            // API definition
            return this;
        } catch (Throwable e) {
            I.error(e);
            throw e;
        }
    }

    /**
     * Calculate variables by the given model list and merge them all.
     * 
     * @param models
     * @return
     */
    public Word evaluateAndMerge(Signal models, Object... additions) {
        return evaluateAndMerge(models.toList(), additions);
    }

    /**
     * Calculate variables by the given model list and merge them all.
     * 
     * @param models
     * @return
     */
    public Word evaluateAndMerge(List models, Object... additions) {
        if (models == null || models.isEmpty()) {
            return this;
        }

        if (models.size() == 1) {
            return evaluate(models.get(0), additions);
        }

        evaluate(models.get(0), additions);

        for (int i = 1; i < models.size(); i++) {
            merge(new Word(name).evaluate(models.get(i), additions));
        }
        return this;
    }

    /**
     * Merge the specified {@link Word} to this document.
     * 
     * @param after
     * @return
     */
    public Word merge(Word after) {
        if (calculated.getBodyElements().isEmpty()) {
            calculated = after.calculated;
        } else {
            merge(after.calculated.getBodyElements());
        }
        return this;
    }

    /**
     * Merge the specified {@link Word} to this document.
     * 
     * @param after
     */
    private void merge(List<IBodyElement> elements) {
        XmlCursor cursor = cursorLast();

        // copy children
        for (int i = 0; i < elements.size(); i++) {
            IBodyElement element = elements.get(i);

            if (element instanceof XWPFParagraph) {
                XWPFParagraph para = (XWPFParagraph) element;
                XWPFParagraph created = createParagraph(cursor);
                WordHeleper.copy(para, created, v -> v);
                cursor = cursorAfter(created);

                if (i == 0) {
                    created.setPageBreak(true);

                    CTSectPr inSec = WordHeleper.section(para.getDocument());
                    CTSectPr outSec = WordHeleper.section(created.getDocument());
                    CTPageMar inMargin = inSec.getPgMar();
                    CTPageMar outMargin = outSec.addNewPgMar();
                    outMargin.setBottom(inMargin.getBottom());
                    outMargin.setLeft(inMargin.getLeft());
                    outMargin.setRight(inMargin.getRight());
                    outMargin.setTop(inMargin.getTop());

                    // List<XWPFFooter> inFooter = para.getDocument().getFooterList();
                    // List<XWPFFooter> outFooter = created.getDocument().getFooterList();
                    // XWPFHeaderFooterPolicy headerFooterPolicy =
                    // created.getDocument().getHeaderFooterPolicy();
                }
            } else if (element instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) element;
                XWPFTable created = calculated.createTable();
                created.removeRow(0); // new table has one row and one column, so we must remove it
                WordHeleper.copy(table, created, v -> v);
                cursor = cursorAfter(created);
            }
        }
    }

    /**
     * Print document with the given variables.
     * 
     * @return Chainable API
     */
    public Word print() {
        try {
            File temp = Locator.temporaryFile("calculated.docx");
            save(temp);
            Desktop.getDesktop().print(temp.asJavaFile());
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return this;
    }

    /**
     * Open document with the given variables.
     * 
     * @return
     */
    public Word open() {
        try {
            File temp = Locator.temporaryFile("calculated.docx");
            save(temp);
            Desktop.getDesktop().open(temp.asJavaFile());
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return this;
    }

    /**
     * Save this document to the specified {@link Path}.
     * 
     * @param output
     * @return Chainable API.
     */
    public Word save(File output) {
        return save(output, true);
    }

    /**
     * Save this document to the specified {@link Path}.
     * 
     * @param output
     * @return Chainable API.
     */
    public Word save(File output, boolean overwrite) {
        if (overwrite == true || output.isAbsent()) {
            String extension = output.extension();
            if (extension.equals("pdf")) {
                File input = Locator.temporaryFile();
                save(input);
                LibreOffice.convert(input, output);
            } else {
                try (OutputStream stream = output.newOutputStream()) {
                    calculated.write(stream);
                } catch (IOException e) {
                    throw I.quiet(e);
                }
            }
        }

        return this;
    }

    /**
     * Replace variable text.
     * 
     * @param doc
     * @param object
     */
    private void replace(XWPFDocument doc) {
        // for paragraph
        for (XWPFParagraph para : copy(doc.getParagraphs())) {
            replace(para);
        }

        // for table
        for (XWPFTable table : copy(doc.getTables())) {
            replace(table);
        }

        // for header
        for (XWPFHeader header : copy(doc.getHeaderList())) {
            for (XWPFParagraph para : copy(header.getParagraphs())) {
                replace(para);
            }
        }

        // for footer
        for (XWPFFooter footer : copy(doc.getFooterList())) {
            for (XWPFParagraph para : copy(footer.getParagraphs())) {
                replace(para);
            }
        }

        // for textbox
    }

    /**
     * Replace variable text.
     * 
     * @param table
     */
    private void replace(XWPFTable table) {
        for (XWPFTableRow row : copy(table.getRows())) {
            for (XWPFTableCell cell : copy(row.getTableCells())) {
                context.cell = cell;
                for (XWPFParagraph para : copy(cell.getParagraphs())) {
                    replace(para);
                }
                context.cell = null;

                for (XWPFTable innerTable : copy((cell.getTables()))) {
                    replace(innerTable);
                }
            }
        }
    }

    /**
     * Replace variable text.
     * 
     * @param para
     * @param object
     */
    private void replace(XWPFParagraph para) {
        context.isStartConditinalBlock(para);
        context.block.process(para);
        context.isEndConditionalBlock(para);
    }

    /**
     * Helper method to collect all paragraphs in this document.
     * 
     * @param observer A list of {@link XWPFParagraph}.
     */
    private void collectParagraph(IBodyElement element, Observer<? super XWPFParagraph> observer) {
        switch (element.getElementType()) {
        case PARAGRAPH:
            observer.accept((XWPFParagraph) element);
            break;

        case TABLE:
            XWPFTable table = (XWPFTable) element;
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph para : cell.getParagraphs()) {
                        collectParagraph(para, observer);
                    }

                    for (XWPFTable innerTable : cell.getTables()) {
                        collectParagraph(innerTable, observer);
                    }
                }
            }
            break;

        default:
            break;
        }
    }

    /**
     * Create new paragraph at the specified location.
     * 
     * @param cursor A location.
     * @return
     */
    private final XWPFParagraph createParagraph(XmlCursor cursor) {
        XWPFParagraph created = null;

        if (cursor != null) {
            created = calculated.insertNewParagraph(cursor);
        }

        if (created == null) {
            created = calculated.createParagraph();
        }

        switch (breakType) {
        case Page:
            breakType = BreakType.None;
            created.setPageBreak(true);
            break;

        case Section:
            breakType = BreakType.None;
            XWPFParagraph before = calculated.getParagraphArray(calculated.getParagraphs().size() - 2);

            if (before != null) {
                CTSectPr section = calculated.getDocument().getBody().addNewSectPr();
                WordHeleper.ppr(before).setSectPr(section);

                this.section.setSize(section);
                this.section.setMargin(section);
            }
            break;

        default:
            break;
        }
        return created;
    }

    /**
     * Avoid {@link ConcurrentModificationException}.
     * 
     * @param list A copy list items.
     * @return A copied list.
     */
    private static <T> List<T> copy(List<T> list) {
        return new ArrayList(list);
    }

    /**
     * Avoid {@link ConcurrentModificationException}.
     * 
     * @param list A copy list items.
     * @return A copied list.
     */
    private static <T> List<T> copy(List<T> list, T start, T end) {
        boolean take = false;
        List<T> copy = new ArrayList();

        for (T item : list) {
            if (!take && start == item) {
                take = true;
            }

            if (take) {
                copy.add(item);
            }

            if (take && end == item) {
                break;
            }
        }
        return copy;
    }

    /**
     * @version 2016/09/06 18:37:44
     */
    private static enum BreakType {
        Page, Section, None;
    }

    /**
     * <p>
     * Section configurator.
     * </p>
     * 
     * @version 2016/09/06 19:12:18
     */
    public class Section {

        private STPageOrientation.Enum orientation;

        private int height;

        private int width;

        private int marginTop;

        private int marginBottom;

        private int marginLeft;

        private int marginRight;

        /**
         * Set margin on all side.
         * 
         * @param size
         * @return
         */
        public Section marginHorizontal(int size) {
            return marginLeft(size).marginRight(size);
        }

        /**
         * Set margin on all side.
         * 
         * @param size
         * @return
         */
        public Section marginVertical(int size) {
            return marginTop(size).marginBottom(size);
        }

        /**
         * Set margin on all side.
         * 
         * @param size
         * @return
         */
        public Section margin(int size) {
            return marginHorizontal(size).marginVertical(size);
        }

        /**
         * Set page orientation.
         * 
         * @return Chainable API.
         */
        public Section landscape() {
            orientation = STPageOrientation.LANDSCAPE;

            return this;
        }

        /**
         * Set page orientation.
         * 
         * @return Chainable API.
         */
        public Section portrait() {
            orientation = STPageOrientation.PORTRAIT;

            return this;
        }

        public void head(String text) {

        }

        /**
         * Set page size.
         * 
         * @param section
         */
        private void setSize(CTSectPr section) {
            if (section != null) {
                CTPageSz size = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
                size.setOrient(orientation);
                size.setW(BigInteger.valueOf(width * 20));
                size.setH(BigInteger.valueOf(height * 20));
            }
        }

        /**
         * Get page size.
         * 
         * @param section
         */
        private void getSize(CTSectPr section) {
            if (section != null) {
                CTPageSz size = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
                orientation = size.getOrient();
                width = ((BigInteger) size.getW()).divide(BigInteger.valueOf(20)).intValue();
                height = ((BigInteger) size.getH()).divide(BigInteger.valueOf(20)).intValue();
            }
        }

        /**
         * Set page margin.
         * 
         * @param section
         */
        private void setMargin(CTSectPr section) {
            if (section != null) {
                CTPageMar margin = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
                margin.setBottom(BigInteger.valueOf(marginBottom * 20));
                margin.setTop(BigInteger.valueOf(marginTop * 20));
                margin.setRight(BigInteger.valueOf(marginRight * 20));
                margin.setLeft(BigInteger.valueOf(marginLeft * 20));
            }
        }

        /**
         * Get page margin.
         * 
         * @param section
         */
        private void getMargin(CTSectPr section) {
            if (section != null) {
                CTPageMar margin = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
                marginTop = ((BigInteger) margin.getTop()).divide(BigInteger.valueOf(20)).intValue();
                marginBottom = ((BigInteger) margin.getBottom()).divide(BigInteger.valueOf(20)).intValue();
                marginLeft = ((BigInteger) margin.getLeft()).divide(BigInteger.valueOf(20)).intValue();
                marginRight = ((BigInteger) margin.getRight()).divide(BigInteger.valueOf(20)).intValue();
            }
        }

        /**
         * Get the orientation property of this {@link Word.Section}.
         * 
         * @return The orientation property.
         */
        public STPageOrientation.Enum orientation() {
            return orientation;
        }

        /**
         * Set the orientation property of this {@link Word.Section}.
         * 
         * @param orientation The orientation value to set.
         */
        public Section orientation(STPageOrientation.Enum orientation) {
            this.orientation = orientation;
            return this;
        }

        /**
         * Get the height property of this {@link Word.Section}.
         * 
         * @return The height property.
         */
        public int height() {
            return height;
        }

        /**
         * Set the height property of this {@link Word.Section}.
         * 
         * @param height The height value to set.
         */
        public Section height(int height) {
            this.height = height;
            return this;
        }

        /**
         * Get the width property of this {@link Word.Section}.
         * 
         * @return The width property.
         */
        public int width() {
            return width;
        }

        /**
         * Set the width property of this {@link Word.Section}.
         * 
         * @param width The width value to set.
         */
        public Section width(int width) {
            this.width = width;
            return this;
        }

        /**
         * Get the marginTop property of this {@link Word.Section}.
         * 
         * @return The marginTop property.
         */
        public int marginTop() {
            return marginTop;
        }

        /**
         * Set the marginTop property of this {@link Word.Section}.
         * 
         * @param marginTop The marginTop value to set.
         */
        public Section marginTop(int marginTop) {
            this.marginTop = marginTop;
            return this;
        }

        /**
         * Get the marginBottom property of this {@link Word.Section}.
         * 
         * @return The marginBottom property.
         */
        public int marginBottom() {
            return marginBottom;
        }

        /**
         * Set the marginBottom property of this {@link Word.Section}.
         * 
         * @param marginBottom The marginBottom value to set.
         */
        public Section marginBottom(int marginBottom) {
            this.marginBottom = marginBottom;
            return this;
        }

        /**
         * Get the marginLeft property of this {@link Word.Section}.
         * 
         * @return The marginLeft property.
         */
        public int marginLeft() {
            return marginLeft;
        }

        /**
         * Set the marginLeft property of this {@link Word.Section}.
         * 
         * @param marginLeft The marginLeft value to set.
         */
        public Section marginLeft(int marginLeft) {
            this.marginLeft = marginLeft;
            return this;
        }

        /**
         * Get the marginRight property of this {@link Word.Section}.
         * 
         * @return The marginRight property.
         */
        public int marginRight() {
            return marginRight;
        }

        /**
         * Set the marginRight property of this {@link Word.Section}.
         * 
         * @param marginRight The marginRight value to set.
         */
        public Section marginRight(int marginRight) {
            this.marginRight = marginRight;
            return this;
        }
    }

    /**
     * @version 2016/06/04 15:29:53
     */
    private interface Block {

        default void start(XWPFParagraph paragraph) {
        };

        void process(XWPFParagraph paragraph);

        default void end(XWPFParagraph paragraph) {
        }
    }

    /**
     * @version 2016/06/04 14:33:19
     */
    private class CalculationContext {

        /** The block processor. */
        private Block block = new Normal();

        /** The variable context. */
        private Parser parser;

        /** The current processing cell. */
        private XWPFTableCell cell;

        /**
         * @param paragraph
         */
        public void isStartConditinalBlock(XWPFParagraph paragraph) {
            try {
                CTP context = paragraph.getCTP();
                List<CTMarkupRange> starts = context.getCommentRangeStartList();
                if (starts.size() != 0) {
                    String special = "";
                    String condition = paragraph.getDocument().getCommentByID(starts.get(0).getId().toString()).getText();
                    int index = condition.indexOf("#");

                    if (index != -1) {
                        special = condition.substring(index + 1);
                        condition = condition.substring(0, index);
                    }

                    Object value = parser.resolve(condition);

                    if (value instanceof Signal) {
                        value = ((Signal) value).toList();
                    }

                    if (value instanceof Boolean) {
                        block = new If((Boolean) value);
                    } else if (value instanceof List) {
                        switch (paragraph.getPartType()) {
                        case DOCUMENT:
                            block = new Loop((List) value);
                            break;

                        case TABLECELL:
                            XWPFTableCell cell = (XWPFTableCell) paragraph.getBody();

                            if (cell.getParagraphs().indexOf(paragraph) != 0) {
                                block = new LoopInCell((List) value, special);
                            } else {
                                block = new TableRowLoop((List) value);
                            }
                            break;

                        default:
                            // If this exception will be thrown, it is bug of this program. So we
                            // must rethrow the wrapped error in here.
                            throw new Error();
                        }
                    } else {
                        String text = paragraph.getText();

                        if (text.equals("$") || text.equals("{$}")) {
                            block = new Replace(value);
                        } else {
                            block = new If(value != null && !value.toString().trim().isEmpty());
                        }
                    }
                    block.start(paragraph);
                }
            } catch (XmlValueDisconnectedException e) {
                // ignore
            }
        }

        /**
         */
        public void isEndConditionalBlock(XWPFParagraph paragraph) {
            try {
                CTP context = paragraph.getCTP();

                if (context.getCommentRangeEndList().size() != 0) {
                    block.end(paragraph);
                    block = new Normal();
                }
            } catch (XmlValueDisconnectedException e) {
                // ignore
            }
        }

        /**
         * @version 2016/06/04 16:08:00
         */
        private class Normal implements Block {

            private void ruby() {

            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void process(XWPFParagraph paragraph) {
                for (int i = 0; i < paragraph.getRuns().size(); i++) {
                    XWPFRun run = paragraph.getRuns().get(i);

                    try {
                        String text = parser.apply(run.getText(0));
                        text = WordCellStyle.apply(context.cell, text);

                        int start = 0;
                        int end = 0;
                        while ((start = text.indexOf('｛', start)) != -1 && (end = text.indexOf('｝', start)) != -1) {
                            String before = text.substring(0, start);
                            String ruby = text.substring(start + 1, end);
                            text = text.substring(end + 1);

                            WordHeleper.copy(run, paragraph.insertNewRun(i++), x -> before);

                            XWPFRun rubys = paragraph.insertNewRun(i++);
                            WordHeleper.copy(run, rubys, x -> ruby);
                            rubys.setFontSize(7);
                            rubys.setColor("989898");
                            rubys.setTextPosition(2);
                        }
                        WordHeleper.write(run, text);
                    } catch (Throwable e) {
                        // ignore
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * @version 2016/06/22 15:11:14
         */
        private class Replace implements Block {

            /** The replaced value. */
            private final Object value;

            /**
             * @param value
             */
            private Replace(Object value) {
                this.value = value;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void process(XWPFParagraph paragraph) {
                List<XWPFRun> runs = paragraph.getRuns();

                for (int i = runs.size() - 1; 0 <= i; i--) {
                    String text = runs.get(i).text();

                    if (!text.equals("$")) {
                        paragraph.removeRun(i);
                    }
                }

                if (!paragraph.getIRuns().isEmpty()) {
                    paragraph.getRuns().get(0).setText(I.transform(value, String.class), 0);
                }
            }
        }

        /**
         * @version 2016/06/04 17:55:05
         */
        private class If implements Block {

            /** The actual condition. */
            private final boolean condition;

            /** The start index. */
            private XWPFParagraph start;

            /** The end index. */
            private XWPFParagraph end;

            /**
             * @param condition
             */
            private If(boolean condition) {
                this.condition = condition;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void start(XWPFParagraph paragraph) {
                start = paragraph;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void process(XWPFParagraph paragraph) {
                // do nothing
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void end(XWPFParagraph paragraph) {
                end = paragraph;

                if (!condition) {
                    BodyType type = paragraph.getPartType();

                    if (type == BodyType.DOCUMENT) {
                        XWPFDocument doc = paragraph.getDocument();

                        for (XWPFParagraph para : copy(doc.getParagraphs(), start, end)) {
                            doc.removeBodyElement(doc.getBodyElements().indexOf(para));
                        }
                    } else if (type == BodyType.TABLECELL) {
                        XWPFTableCell cell = (XWPFTableCell) paragraph.getBody();

                        for (XWPFParagraph para : copy(cell.getParagraphs(), start, end)) {
                            WordHeleper.clearText(para);
                        }
                    }
                } else {
                    XWPFDocument doc = paragraph.getDocument();

                    for (XWPFParagraph para : copy(doc.getParagraphs(), start, end)) {
                        for (XWPFRun run : para.getRuns()) {
                            WordHeleper.write(run, parser.apply(run.getText(0)));
                        }
                    }
                }
            }
        }

        /**
         * @version 2016/06/04 16:12:34
         */
        private class Loop implements Block {

            /** The loop items. */
            private final List items;

            /** The start index. */
            private XWPFParagraph start;

            /** The end index. */
            private XWPFParagraph end;

            /**
             * @param items
             */
            private Loop(List items) {
                this.items = items;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void start(XWPFParagraph paragraph) {
                start = paragraph;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void process(XWPFParagraph paragraph) {
                // do nothing
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void end(XWPFParagraph paragraph) {
                end = paragraph;

                XWPFDocument doc = paragraph.getDocument();
                List<XWPFParagraph> paragraphs = copy(doc.getParagraphs(), start, end);
                XmlCursor index = paragraph.getCTP().newCursor();

                for (Object item : items) {
                    for (XWPFParagraph para : paragraphs) {
                        WordHeleper.copy(para, doc.insertNewParagraph(index.newCursor()), new Parser(textIsVerticalAlign, List.of(item)));
                    }
                }

                for (XWPFParagraph para : paragraphs) {
                    doc.removeBodyElement(doc.getPosOfParagraph(para));
                }
            }
        }

        /**
         * @version 2016/06/27 9:17:30
         */
        private class LoopInCell implements Block {

            /** The loop items. */
            private final List items;

            /** The special command. */
            private final boolean keep;

            /** The start index. */
            private XWPFParagraph start;

            /** The end index. */
            private XWPFParagraph end;

            /**
             * @param items
             */
            private LoopInCell(List items, String command) {
                this.items = items;
                this.keep = command.equals("keepLine");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void start(XWPFParagraph paragraph) {
                start = paragraph;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void process(XWPFParagraph paragraph) {
                // do nothing
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void end(XWPFParagraph paragraph) {
                end = paragraph;

                XWPFTableCell cell = (XWPFTableCell) paragraph.getBody();
                List<XWPFParagraph> paragraphs = copy(cell.getParagraphs(), start, end);
                XmlCursor index = paragraph.getCTP().newCursor();

                for (Object item : items) {
                    for (XWPFParagraph para : paragraphs) {
                        WordHeleper.copy(para, cell.insertNewParagraph(index.newCursor()), new Parser(textIsVerticalAlign, List.of(item)));
                    }
                }

                for (XWPFParagraph para : paragraphs) {
                    cell.removeParagraph(cell.getParagraphs().indexOf(para));
                }

                if (keep) {
                    for (int i = 0; i < items.size() - 1; i++) {
                        List<XWPFParagraph> list = cell.getParagraphs();
                        int pos = list.size() - 1;
                        XWPFParagraph last = list.get(pos);
                        String text = last.getText();

                        if (text.equals("")) {
                            cell.removeParagraph(pos);
                        }
                    }
                }
            }
        }

        /**
         * 
         */
        private class TableRowLoop implements Block {

            /** The loop items. */
            private final List items;

            /** The start row. */
            private XWPFTableRow start;

            /** The end row. */
            private XWPFTableRow end;

            /**
             * @param element
             * @param items
             */
            private TableRowLoop(List items) {
                this.items = items;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void start(XWPFParagraph paragraph) {
                start = ((XWPFTableCell) paragraph.getBody()).getTableRow();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void process(XWPFParagraph paragraph) {
                // do nothing
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void end(XWPFParagraph paragraph) {
                end = ((XWPFTableCell) paragraph.getBody()).getTableRow();

                XWPFTable table = start.getTable();
                List<XWPFTableRow> rows = copy(table.getRows(), start, end);
                int start = table.getRows().indexOf(end);

                for (int count = 0; count < items.size(); count++) {
                    for (int offset = 0; offset < rows.size(); offset++) {
                        WordHeleper.copy(rows.get(offset), table
                                .insertNewTableRow(start + count * rows.size() + offset), new Parser(textIsVerticalAlign, List
                                        .of(items.get(count))));
                    }
                }

                for (XWPFTableRow row : rows) {
                    table.removeRow(table.getRows().indexOf(row));
                }
            }
        }
    }
}