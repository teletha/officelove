/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.word;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

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
import kiss.Events;
import kiss.I;
import kiss.Observer;
import kiss.model.Model;
import kiss.model.Property;
import lombok.Setter;
import lombok.experimental.Accessors;
import offishell.Problem;
import offishell.UI;
import offishell.file.Directory;
import offishell.file.FileType;

/**
 * @version 2016/05/28 9:53:59
 */
public class Word {

    static {
        I.load(Word.class, false);
    }

    /** The template file. */
    private final Path path;

    /** The culculated document. */
    XWPFDocument calculated;

    /**
     * <p>
     * Collect all paragraphs in this document.
     * </p>
     * 
     * @return A list of {@link XWPFParagraph}.
     */
    public final Events<XWPFParagraph> paragraphs = new Events<XWPFParagraph>(observer -> {
        for (IBodyElement element : calculated.getBodyElements()) {
            collectParagraph(element, observer);
        }
        return Disposable.Φ;
    });

    /** The context. */
    private CalculationContext context = new CalculationContext();

    /** The text direction. */
    private boolean textIsVerticalAlign;

    /** The next break type. */
    private BreakType breakType = BreakType.None;

    private Section section = new Section();

    /**
     * 
     */
    private Word() {
        this.path = I.locateTemporary();
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
            context.variable = new VariableContext(models);
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
    public Word calculateAndMerge(Events models, Object... additions) {
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
            try {
                calculated.write(Files.newOutputStream(output));
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
        } catch (IOException e) {
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
     * @param root
     * @param string
     */
    public static Word of(Path directory, String fineName) {
        return of(Directory.of(directory).file(fineName, FileType.Word));
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
    @Setter
    @Accessors(fluent = true)
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
    }

    /**
     * @version 2016/06/04 18:04:12
     */
    private class VariableContext implements UnaryOperator<String> {

        /** The model object. */
        private final List models;

        private boolean inVariable = false;

        private StringBuilder replace = new StringBuilder();

        private StringBuilder variable = new StringBuilder();

        /** Cache for {@link ExpressionResolver} */
        private List<ExpressionResolver> expressionResolvers = I.find(ExpressionResolver.class);

        /** Cache for {@link Variable} */
        private List<Variable> variables = I.find(Variable.class);

        /**
         * @param model
         */
        private VariableContext(Object model) {
            this(Collections.singletonList(model));
        }

        /**
         * @param model
         */
        private VariableContext(List models) {
            if (models == null || models.size() == 0) {
                throw new Error("モデルを指定してください。");
            }
            this.models = models;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String apply(String text) {
            if (text != null) {
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);

                    switch (c) {
                    case '{':
                        inVariable = true;
                        break;

                    case '}':
                        inVariable = false;

                        replace.append(I.transform(resolve(variable.toString()), String.class));

                        // clear variable info
                        variable = new StringBuilder();
                        break;

                    default:
                        if (inVariable) {
                            variable.append(c);
                        } else {
                            replace.append(c);
                        }
                        break;
                    }
                }
            }

            if (textIsVerticalAlign) {
                convertForVerticalText(replace);
            }

            String replaced = replace.toString();
            replace = new StringBuilder();
            return replaced;
        }

        /**
         * <p>
         * Compute the specified built-in variable.
         * </p>
         * 
         * @param variable
         * @return
         */
        private Object resolveBuiltinVariable(String variable) {
            for (Variable var : variables) {
                if (var.test(variable)) {
                    return var.apply(variable);
                }
            }
            throw Problem.of("変数『$" + variable + "』は使用できません。 [" + path + "]").solution(Variable.class + "を実装したクラスを作成してください。");
        }

        /**
         * <p>
         * Compute the specified property variable.
         * </p>
         * 
         * @param paths
         * @return
         */
        private Object resolve(String paths) {
            Error error = null;

            boolean optional = paths.endsWith("?");

            if (optional) {
                paths = paths.substring(0, paths.length() - 1);
            }

            for (Object value : models) {
                try {
                    return resolve(paths.split("\\."), 0, value);
                } catch (Error e) {
                    if (error == null) {
                        error = e;
                    } else {
                        error.addSuppressed(e);
                    }
                }
            }

            if (optional) {
                return "";
            }

            if (error == null) {
                error = new Error();
            }
            throw error;
        }

        /**
         * <p>
         * Compute the specified property variable.
         * </p>
         * 
         * @param expressions
         * @param index
         * @param value
         * @return
         */
        private Object resolve(String[] expressions, int index, Object value) {
            if (value == null) {
                return "";
            }

            if (expressions.length == index) {
                return value;
            }

            String expression = expressions[index];

            if (expression.charAt(0) == '$') {
                return resolve(expressions, index + 1, resolveBuiltinVariable(expression.substring(1)));
            }

            for (ExpressionResolver resolver : expressionResolvers) {
                Matcher matcher = resolver.match(expression);

                if (matcher.matches()) {
                    return resolve(expressions, index + 1, resolver.resolve(matcher, value));
                }
            }

            Model model = Model.of(value);
            Property property = model.property(expression);

            // プロパティから検索
            if (property != null) {
                return resolve(expressions, index + 1, model.get(value, property));
            }

            // プロパティがないのでメソッドを検索して実行する
            try {
                int start = expression.indexOf("(");
                int end = expression.lastIndexOf(")");

                String name;
                String[] parametersText;

                if (start == -1 && end == -1) {
                    // without parameter
                    name = expression;
                    parametersText = new String[0];
                } else if (start != -1 && end != -1) {
                    // with parameter
                    name = expression.substring(0, start);
                    parametersText = expression.substring(start + 1, end).split(",");
                } else {
                    throw errorInVariableResolve(value, expressions, expression);
                }

                for (Method method : model.type.getMethods()) {
                    // exclude void type
                    if (method.getReturnType() == void.class) {
                        continue;
                    }

                    if (method.getName().equals(name) && method.getParameterCount() == parametersText.length) {
                        Object[] params = new Object[parametersText.length];

                        for (int i = 0; i < parametersText.length; i++) {
                            params[i] = I.transform(parametersText[i], method.getParameterTypes()[i]);
                        }
                        method.setAccessible(true);
                        return resolve(expressions, index + 1, method.invoke(value, params));
                    }
                }
                throw errorInVariableResolve(value, expressions, expression);
            } catch (Exception e) {
                throw errorInVariableResolve(value, expressions, expression).error(e);
            }
        }

        /**
         * <p>
         * エラーの詳細を記述します。
         * </p>
         * 
         * @param model
         * @param expressions
         * @param expression
         * @return
         */
        private Problem errorInVariableResolve(Object model, String[] expressions, String expression) {
            return Problem.of("文書 [" + path + "] の変数 [" + String.join(".", expressions) + "] で使われている [" + expression + "] は" + model
                    .getClass().getSimpleName() + "クラスでは解決できません。");
        }

        /**
         * <p>
         * Convert text for vertical alignment.
         * </p>
         * 
         * @param replace2
         */
        private void convertForVerticalText(StringBuilder builder) {
            for (int i = 0; i < builder.length(); i++) {
                switch (builder.charAt(i)) {
                case '1':
                case '１':
                    builder.setCharAt(i, '一');
                    break;
                case '2':
                case '２':
                    builder.setCharAt(i, '二');
                    break;
                case '3':
                case '３':
                    builder.setCharAt(i, '三');
                    break;
                case '4':
                case '４':
                    builder.setCharAt(i, '四');
                    break;
                case '5':
                case '５':
                    builder.setCharAt(i, '五');
                    break;
                case '6':
                case '６':
                    builder.setCharAt(i, '六');
                    break;
                case '7':
                case '７':
                    builder.setCharAt(i, '七');
                    break;
                case '8':
                case '８':
                    builder.setCharAt(i, '八');
                    break;
                case '9':
                case '９':
                    builder.setCharAt(i, '九');
                    break;
                case '0':
                case '０':
                    builder.setCharAt(i, '〇');
                    break;
                }
            }
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

                    if (value instanceof Events) {
                        value = ((Events) value).toList();
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
                        WordHeleper.copy(para, doc.insertNewParagraph(index.newCursor()), new VariableContext(item));
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
                        WordHeleper.copy(para, cell.insertNewParagraph(index.newCursor()), new VariableContext(item));
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
         * @version 2016/06/04 15:29:43
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
                                .insertNewTableRow(start + count * rows.size() + offset), new VariableContext(items.get(count)));
                    }
                }

                for (XWPFTableRow row : rows) {
                    table.removeRow(table.getRows().indexOf(row));
                }
            }
        }
    }
}
