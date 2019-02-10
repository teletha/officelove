/*
 * Copyright (C) 2019 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.word;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
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

import kiss.Disposable;
import kiss.I;
import kiss.Observer;
import kiss.Signal;
import officeman.model.FileType;
import offishell.UI;
import offishell.expression.Variable;
import offishell.expression.VariableContext;
import psychopath.Directory;
import psychopath.Locator;

/**
 * @version 2016/05/28 9:53:59
 */
public class Word {

    static {
        I.load(Variable.class, false);
    }

    /** The template file. */
    final Path path;

    /** The culculated document. */
    XWPFDocument calculated;

    /**
     * <p>
     * Collect all paragraphs in this document.
     * </p>
     * 
     * @return A list of {@link XWPFParagraph}.
     */
    public final Signal<XWPFParagraph> paragraphs = new Signal<XWPFParagraph>((observer, disposer) -> {
        for (IBodyElement element : calculated.getBodyElements()) {
            collectParagraph(element, observer);
        }
        return Disposable.empty();
    });

    /** The context. */
    private CalculationContext context = new CalculationContext();

    /** The text direction. */
    boolean textIsVerticalAlign;

    /** The next break type. */
    private BreakType breakType = BreakType.None;

    private Section section = new Section();

    /**
     * 
     */
    private Word() {
        this.path = Locator.temporaryFile().asJavaPath();
        this.calculated = new XWPFDocument();

        CTSectPr sect = calculated.getDocument().getBody().getSectPr();
        section.getSize(sect);
        section.getMargin(sect);
    }

    /**
     * @param path
     */
    protected Word(Path path) {
        if (Files.notExists(path)) {
            throw new Error("ファイル " + path.toAbsolutePath() + " が見つかりません。");
        }

        try {
            this.path = path;
            this.calculated = new XWPFDocument(Files.newInputStream(path));

            CTTextDirection direction = calculated.getDocument().getBody().getSectPr().getTextDirection();

            if (direction != null) {
                this.textIsVerticalAlign = direction.getVal() == STTextDirection.TB_RL;
            } else {
                this.textIsVerticalAlign = false;
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Expose POI API.
     * </p>
     * 
     * @return POI document.
     */
    public XWPFDocument docment() {
        return calculated;
    }

    /**
     * <p>
     * Helper method to find {@link XWPFParagraph} which contains the specified text.
     * </p>
     * 
     * @param text A text to search.
     * @return A serach result.
     */
    public XWPFParagraph findParagraphWith(String text) {
        List<XWPFParagraph> list = paragraphs.take(p -> p.getText().contains(text)).toList();

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * <p>
     * Helper method to find {@link XWPFParagraph} which contains the specified text.
     * </p>
     * 
     * @param text A text to search.
     * @param start A start position to search.
     * @return A serach result.
     */
    public XWPFParagraph findParagraphWith(String text, XWPFParagraph start) {
        List<XWPFParagraph> list = paragraphs.skipUntil(start).take(p -> p.getText().contains(text)).toList();

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * <p>
     * Copy all pages.
     * </p>
     */
    public Word copy(int number) {
        List<IBodyElement> copy = copy(calculated.getBodyElements());

        for (int i = 0; i < number - 1; i++) {
            merge(copy);
        }
        return this;
    }

    /**
     * <p>
     * Helper method to get cusor.
     * </p>
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
     * <p>
     * Helper method to get cusor.
     * </p>
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
     * <p>
     * Calculate variables by the given model.
     * </p>
     * 
     * @param models
     * @return
     */
    public Word calculate(Object model, Object... models) {
        ArrayList list = new ArrayList();
        list.add(model);
        list.addAll(Arrays.asList(models));

        return calculate(list);
    }

    /**
     * <p>
     * Calculate variables by the given model.
     * </p>
     * 
     * @param models
     * @return
     */
    public Word calculate(List models) {
        if (models == null) {
            throw new Error("ユーザーモデルが指定されていません。");
        }

        try {
            // calculate variables
            context.variable = new VariableContext(path, textIsVerticalAlign, models);
            replace(calculated);

            // clear all comments
            WordHeleper.clearComment(calculated);

            // API definition
            return this;
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Calculate variables by the given model list and merge them all.
     * </p>
     * 
     * @param models
     * @return
     */
    public Word calculateAndMerge(Signal models, Object... additions) {
        return calculateAndMerge(models.toList(), additions);
    }

    /**
     * <p>
     * Calculate variables by the given model list and merge them all.
     * </p>
     * 
     * @param models
     * @return
     */
    public Word calculateAndMerge(List models, Object... additions) {
        if (models == null || models.isEmpty()) {
            return this;
        }

        if (models.size() == 1) {
            return calculate(models.get(0), additions);
        }

        calculate(models.get(0), additions);

        for (int i = 1; i < models.size(); i++) {
            merge(new Word(path).calculate(models.get(i), additions));
        }
        return this;
    }

    /**
     * <p>
     * Merge the specified {@link Word} to this document.
     * </p>
     * 
     * @param after
     * @return
     */
    public Word merge(Word after) {
        merge(after.calculated.getBodyElements());
        return this;
    }

    /**
     * <p>
     * Merge the specified {@link Word} to this document.
     * </p>
     * 
     * @param after
     * @return
     */
    private Word merge(List<IBodyElement> elements) {
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

                    List<XWPFFooter> inFooter = para.getDocument().getFooterList();
                    List<XWPFFooter> outFooter = created.getDocument().getFooterList();
                    XWPFHeaderFooterPolicy headerFooterPolicy = created.getDocument().getHeaderFooterPolicy();
                }
            } else if (element instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) element;
                XWPFTable created = calculated.createTable();
                created.removeRow(0); // new table has one row and one column, so we must remove it
                WordHeleper.copy(table, created, v -> v);
                cursor = cursorAfter(created);
            }
        }
        return this;
    }

    /**
     * <p>
     * Print document with the given variables.
     * </p>
     * 
     * @return Chainable API
     */
    public Word print() {
        try {
            return print(Files.createTempFile("calculated", ".docx"));
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Print document with the given variables.
     * </p>
     * 
     * @return Chainable API
     */
    public Word print(Path path) {
        save(path);

        UI.print(path);

        // API definition
        return this;
    }

    /**
     * <p>
     * Open document with the given variables.
     * </p>
     * 
     * @return
     */
    public Word open() {
        try {
            Path temp = Files.createTempFile("calculated", ".docx");
            save(temp);
            Desktop.getDesktop().open(temp.toFile());
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return this;
    }

    /**
     * <p>
     * Save this document to the specified {@link Path}.
     * </p>
     * 
     * @param output
     * @return Chainable API.
     */
    public Word save(Path output) {
        return save(output, true);
    }

    /**
     * <p>
     * Save this document to the specified {@link Path}.
     * </p>
     * 
     * @param output
     * @return Chainable API.
     */
    public Word save(Path output, boolean overwrite) {
        if (overwrite == true || Files.notExists(output)) {
            try (OutputStream stream = Files.newOutputStream(output)) {
                calculated.write(stream);
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }

        // API definition
        return this;
    }

    /**
     * <p>
     * Replace variable text.
     * </p>
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
            for (XWPFTableRow row : copy(table.getRows())) {
                for (XWPFTableCell cell : copy(row.getTableCells())) {
                    for (XWPFParagraph para : copy(cell.getParagraphs())) {
                        replace(para);
                    }
                }
            }
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
    }

    /**
     * @param para
     * @param object
     */
    private void replace(XWPFParagraph para) {
        context.isStartConditinalBlock(para);
        context.block.process(para);
        context.isEndConditionalBlock(para);
    }

    /**
     * <p>
     * Helper method to collect all paragraphs in this document.
     * </p>
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
                }
            }
            break;

        default:
            break;
        }
    }

    /**
     * <p>
     * Create header with the specified text.
     * </p>
     * 
     * @param headerText A header text.
     */
    public Word header(String headerText) {
        try {
            CTSectPr section = calculated.getDocument().getBody().addNewSectPr();
            XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(calculated, section);
            XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
            XWPFParagraph para = header.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(headerText);
            styles().base().apply(run);
        } catch (Exception e) {
            throw I.quiet(e);
        }
        return this;
    }

    /**
     * <p>
     * Add new paragraph.
     * </p>
     * 
     * @param text A paragraph text.
     * @return
     */
    public Word text(String text) {
        XWPFParagraph para = createParagraph();
        XWPFRun run = para.createRun();
        run.setText(text, 0);
        styles().base().apply(run);

        return this;
    }

    /**
     * <p>
     * Add new page without section break.
     * </p>
     * 
     * @return
     */
    public Word page() {
        return page(false);
    }

    /**
     * <p>
     * Add new page.
     * </p>
     * 
     * @return
     */
    public Word page(boolean withNewSection) {
        if (withNewSection) {
            breakType = BreakType.Section;
        } else {
            breakType = BreakType.Page;
        }
        return this;
    }

    /**
     * <p>
     * Add new page.
     * </p>
     * 
     * @return
     */
    public Word section(Consumer<Section> configurator) {
        breakType = BreakType.Section;

        if (configurator != null) {
            configurator.accept(section = new Section());
        }
        return this;
    }

    /**
     * <p>
     * Search {@link WordStyleManager}.
     * </p>
     * 
     * @return
     */
    private WordStyleManager styles() {
        List<WordStyleManager> list = I.find(WordStyleManager.class);

        return list.get(list.size() - 1);
    }

    /**
     * <p>
     * Create new paragraph.
     * </p>
     * 
     * @return
     */
    private final XWPFParagraph createParagraph() {
        return createParagraph(null);
    }

    /**
     * <p>
     * Create new paragraph at the specified location.
     * </p>
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
     * <p>
     * Avoid {@link ConcurrentModificationException}.
     * </p>
     * 
     * @param list A copy list items.
     * @return A copied list.
     */
    private static <T> List<T> copy(List<T> list) {
        return new ArrayList(list);
    }

    /**
     * <p>
     * Avoid {@link ConcurrentModificationException}.
     * </p>
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
     * @param path
     * @return
     */
    public static Word of(Path path) {
        return new Word(path);
    }

    /**
     * @param string
     * @param fileName
     */
    public static Word of(String directoryPath, String fileName) {
        return of(Locator.directory(directoryPath), fileName);
    }

    /**
     * @param root
     * @param string
     */
    public static Word of(Directory directory, String fileName) {
        return of(directory.walkFile(fileName + "." + FileType.Word).first().to().v.asJavaPath());
    }

    /**
     * <p>
     * Create empty file.
     * </p>
     * 
     * @return
     */
    public static Word blank() {
        return new Word() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Word merge(Word after) {
                if (calculated.getParagraphs().isEmpty()) {
                    calculated = after.calculated;
                } else {
                    super.merge(after);
                }
                return this;
            }
        };
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
         * <p>
         * Set page orientation.
         * </p>
         * 
         * @return Chainable API.
         */
        public Section landscape() {
            orientation = STPageOrientation.LANDSCAPE;

            return this;
        }

        /**
         * <p>
         * Set page orientation.
         * </p>
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
                width = size.getW().divide(BigInteger.valueOf(20)).intValue();
                height = size.getH().divide(BigInteger.valueOf(20)).intValue();
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
                marginTop = margin.getTop().divide(BigInteger.valueOf(20)).intValue();
                marginBottom = margin.getBottom().divide(BigInteger.valueOf(20)).intValue();
                marginLeft = margin.getLeft().divide(BigInteger.valueOf(20)).intValue();
                marginRight = margin.getRight().divide(BigInteger.valueOf(20)).intValue();
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
        private VariableContext variable;

        /**
         * @param paragraph
         * @param model
         */
        public void isStartConditinalBlock(XWPFParagraph paragraph) {
            try {
                CTP context = paragraph.getCTP();
                CTMarkupRange[] starts = context.getCommentRangeStartArray();

                if (starts.length != 0) {
                    String special = "";
                    String condition = paragraph.getDocument().getCommentByID(starts[0].getId().toString()).getText();

                    int index = condition.indexOf("#");

                    if (index != -1) {
                        special = condition.substring(index + 1);
                        condition = condition.substring(0, index);
                    }

                    Object value = variable.resolve(condition);

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
                            // must
                            // rethrow the wrapped error in here.
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
         * @param para
         */
        public void isEndConditionalBlock(XWPFParagraph paragraph) {
            try {
                CTP context = paragraph.getCTP();

                if (context.getCommentRangeEndArray().length != 0) {
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

            /**
             * {@inheritDoc}
             */
            @Override
            public void process(XWPFParagraph paragraph) {
                for (XWPFRun run : paragraph.getRuns()) {
                    try {
                        WordHeleper.write(run, variable.apply(run.getText(0)));
                    } catch (XmlValueDisconnectedException e) {
                        // ignore
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
                            WordHeleper.write(run, variable.apply(run.getText(0)));
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
                        WordHeleper.copy(para, doc
                                .insertNewParagraph(index.newCursor()), new VariableContext(path, textIsVerticalAlign, item));
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
                        WordHeleper.copy(para, cell
                                .insertNewParagraph(index.newCursor()), new VariableContext(path, textIsVerticalAlign, item));
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
                        WordHeleper.copy(rows.get(offset), table.insertNewTableRow(start + count * rows
                                .size() + offset), new VariableContext(path, textIsVerticalAlign, items.get(count)));
                    }
                }

                for (XWPFTableRow row : rows) {
                    table.removeRow(table.getRows().indexOf(row));
                }
            }
        }
    }
}
