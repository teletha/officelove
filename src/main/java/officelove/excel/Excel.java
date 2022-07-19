/*
 * Copyright (C) 2022 The OFFISHELL Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.excel;

import java.awt.Desktop;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPhoneticRun;

import kiss.Disposable;
import kiss.I;
import kiss.Signal;
import kiss.WiseBiConsumer;
import kiss.WiseSupplier;
import kiss.model.Model;
import kiss.model.Property;
import officelove.LibreOffice;
import officelove.expression.VariableContext;
import psychopath.File;
import psychopath.Locator;

public class Excel {

    static {
        I.load(LibreOffice.class);
    }

    /** The actual file path. */
    public final File file;

    /** The main excel file. */
    public final XSSFWorkbook book;

    /** The main excel sheet. */
    public final XSSFSheet sheet;

    /** The base cell style. */
    private final CellStyle baseStyle;

    /** The date cell style. */
    private final CellStyle dateStyle;

    /**
     * Create empty {@link Excel}.
     */
    public Excel() {
        this(Locator.temporaryFile(), null, () -> new XSSFWorkbook());
    }

    /**
     * Create {@link Excel} wrapper.
     * 
     * @param file
     */
    public Excel(File file) {
        this(file, null, () -> new XSSFWorkbook(file.newInputStream()));
    }

    /**
     * Create {@link Excel} wrapper.
     * 
     * @param file
     */
    public Excel(File file, String sheetName) {
        this(file, sheetName, () -> new XSSFWorkbook(file.newInputStream()));
    }

    /**
     * Actual constructor.
     * 
     * @param file
     * @param bookSupplier
     */
    private Excel(File file, String name, WiseSupplier<XSSFWorkbook> bookSupplier) {
        this.file = file;
        this.book = bookSupplier.get();

        this.sheet = book.getNumberOfSheets() == 0 ? book.createSheet() : book.getSheetAt(name == null ? 0 : book.getSheetIndex(name));
        this.baseStyle = book.createCellStyle();
        this.dateStyle = book.createCellStyle();

        CreationHelper helper = book.getCreationHelper();
        DataFormat dateFormat = helper.createDataFormat();

        Font font = book.createFont();
        font.setFontName("游ゴシック Medium");
        font.setFontHeightInPoints((short) 10);
        baseStyle.setFont(font);
        baseStyle.setAlignment(HorizontalAlignment.CENTER);
        baseStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        baseStyle.setShrinkToFit(true);
        baseStyle.setWrapText(true);

        dateStyle.cloneStyleFrom(baseStyle);
        dateStyle.setDataFormat(dateFormat.getFormat("yyyy/mm/dd"));
    }

    /**
     * 指定したセルが空でない行を全て列挙します。
     * 
     * @param cellName 列名
     * @return
     */
    public Signal<Row> takeBy(String cellName) {
        int index = indexOfHeader(cellName);

        return new Signal<Row>((observer, disposer) -> {
            for (int i = 1; i < sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);

                if (row != null) {
                    XSSFCell cell = row.getCell(index);

                    if (cell != null) {
                        switch (cell.getCellType()) {
                        case BLANK:
                            break;

                        case STRING:
                            String value = cell.getStringCellValue();

                            if (value != null && !value.isEmpty()) {
                                observer.accept(new Row(row));
                            }
                            break;

                        default:
                            observer.accept(new Row(row));
                            break;
                        }
                    }
                }
            }
            observer.complete();
            return disposer;
        });
    }

    /**
     * ヘッダを除いた行を全て返します。
     * 
     * @return
     */
    public Signal<XSSFRow> rowsWithCellBy(String name) {
        return rowsWithCellAt(indexOfHeader(name));
    }

    /**
     * 指定した位置番号のセルにデータが入っている行を全て返します。
     * 
     * @param columnIndex zero-based index.
     * @return
     */
    public Signal<XSSFRow> rowsWithCellAt(int columnIndex) {
        return new Signal<XSSFRow>((observer, disposer) -> {
            for (int i = 1; i < sheet.getLastRowNum(); i++) {
                XSSFRow row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                XSSFCell cell = row.getCell(columnIndex);

                if (cell != null) {
                    observer.accept(row);
                } else {
                    break;
                }
            }
            return Disposable.empty();
        });
    }

    /**
     * <p>
     * Search column index by the specified header text.
     * </p>
     * 
     * @param name
     * @return
     */
    private int indexOfHeader(String name) {
        XSSFRow header = sheet.getRow(0);

        for (Cell cell : header) {
            if (name.equals(cell.getStringCellValue())) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    /**
     * Open excel file.
     * 
     * @return Chainable API
     */
    public Excel open() {
        try {
            Desktop.getDesktop().open(file.asJavaFile());
        } catch (Throwable e) {
            throw I.quiet(e);
        }

        // API definition
        return this;
    }

    /**
     * 指定のモデルに対応する行への操作を記述します。
     * 
     * @param models
     * @param operation
     * @return
     */
    public <M> Excel write(Signal<M> models, BiConsumer<M, Row> operation) {
        update(models, items -> {
            items.to(model -> {
                operation.accept(model, Row.rows.computeIfAbsent(model, key -> {
                    return new Row(findFirstBlankRow());
                }));
            });
        });
        return this;
    }

    /**
     * 指定のモデルに対応する行を更新します。
     * 
     * @param models
     * @return
     */
    public Excel update(Signal models) {
        return update(models.toList());
    }

    /**
     * 指定のモデルに対応する行を更新します。
     * 
     * @param models
     * @return
     */
    public Excel update(List models) {
        return update(models, items -> {
            for (Object item : items) {
                Row row = Row.rows.get(item);

                if (row != null) {
                    Model model = Model.of(item);

                    for (Entry<String, Integer> entry : row.header().entrySet()) {
                        Property property = model.property(entry.getKey());

                        if (property != null) {
                            row.write(entry.getValue(), model.get(item, property));
                        }
                    }
                }
            }
        });
    }

    /**
     * @param models
     * @param operation
     * @return
     */
    private <T> Excel update(T models, Consumer<T> operation) {
        operation.accept(models);
        return this;
    }

    public Excel save() {
        save(file);

        return this;
    }

    public Excel save(File file) {
        try {
            book.write(file.newOutputStream());
        } catch (IOException e) {
            throw I.quiet(e);
        }
        return this;
    }

    private XSSFRow findFirstBlankRow() {
        XSSFRow head = sheet.getRow(0);

        // compute head size
        int headerSize = 0;

        if (head != null) {
            for (; headerSize < head.getLastCellNum(); headerSize++) {
                Cell cell = head.getCell(headerSize);

                if (cell == null || cell.getCellType() == CellType.BLANK) {
                    headerSize--;
                    break;
                }
            }
        }

        row: for (int i = 1; i < sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);

            if (row == null) {
                row = sheet.createRow(i);
                row.setHeightInPoints(30f);
            }

            for (int j = 0; j < headerSize; j++) {
                XSSFCell cell = row.getCell(j);

                if (cell == null) {
                    XSSFCell created = row.createCell(j);
                    created.setCellStyle(baseStyle);
                } else if (cell.getCellType() != CellType.BLANK) {
                    continue row;
                }
            }
            return row;
        }

        XSSFRow row = sheet.getRow(sheet.getLastRowNum());

        if (row == null) {
            int last = sheet.getPhysicalNumberOfRows();
            System.out.println(last);
            row = sheet.createRow(last);
            row.setHeightInPoints(30f);
        }

        for (int j = 0; j < headerSize; j++) {
            XSSFCell cell = row.getCell(j);

            if (cell == null) {
                XSSFCell created = row.createCell(j);
                created.setCellStyle(baseStyle);
            }
        }
        return row;
    }

    public Excel calculate(Object model) {
        Map<CellAddress, XSSFComment> cellComments = sheet.getCellComments();

        VariableContext context = new VariableContext(file.name(), false, List.of(model));

        for (Iterator<Entry<CellAddress, XSSFComment>> iterator = cellComments.entrySet().iterator(); iterator.hasNext();) {
            Entry<CellAddress, XSSFComment> entry = iterator.next();

            CellAddress address = entry.getKey();
            String comment = entry.getValue().getString().getString().strip();

            entry.getValue().setVisible(false);
            XSSFCell cell = sheet.getRow(address.getRow()).getCell(address.getColumn());

            cell.setCellValue(context.apply(comment));
            cell.removeCellComment();
        }
        return save(Locator.temporaryFile("calculated.xlsx"));
    }

    /**
     * Helper method to normalize text.
     * 
     * @param text
     * @return
     */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        text = Normalizer.normalize(text, Form.NFKC).trim();

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char n;

            switch (c) {
            case ' ':
                n = '　';
                break;

            case '~':
                n = '～';
                break;

            case '-':
            case '－':
            case '―':
                n = '-';
                break;

            case '[':
                n = '［';
                break;

            case ']':
                n = '］';
                break;

            case '{':
                n = '｛';
                break;

            case '}':
                n = '｝';
                break;

            case '(':
                n = '（';
                break;

            case ')':
                n = '）';
                break;

            case '+':
                n = '＋';
                break;

            default:
                n = c;
                break;
            }
            builder.append(n);
        }
        return builder.toString();
    }

    /**
     * Enhanced {@link XSSFRow}.
     */
    public class Row {

        private static final Map<Object, Row> rows = new HashMap();

        private static final Map<XSSFSheet, Map<String, Integer>> nameToIndex = new HashMap();

        /** The actual row. */
        private final XSSFRow row;

        /**
         * Create wrapped row.
         * 
         * @param row
         */
        private Row(XSSFRow row) {
            Objects.requireNonNull(row);

            this.row = row;
        }

        /**
         * Read the specified named cell's value and convert to the target model.
         * 
         * @param columnName
         * @param model
         * @return
         */
        public <M> M value(String columnName, M... model) {
            return value(indexOf(columnName, false), model);
        }

        /**
         * Read the specified named cell's value and convert to the target model.
         * 
         * @param columnName
         * @param model
         * @return
         */
        public <M> M value(String columnName, Class<M> model) {
            return value(indexOf(columnName, false), model);
        }

        /**
         * Read the specified indexed cell's value and convert to the target model.
         * 
         * @param columnIndex
         * @param modelClass
         * @return
         */
        public <M> M value(int columnIndex, M... modelClass) {
            return value(row.getCell(columnIndex), (Class<M>) modelClass.getClass().getComponentType());
        }

        /**
         * Read the specified indexed cell's value and convert to the target model.
         * 
         * @param columnIndex
         * @param modelClass
         * @return
         */
        public <M> M value(int columnIndex, Class<M> modelClass) {
            return value(row.getCell(columnIndex), modelClass);
        }

        public void write(int columnIndex, Object value) {
            XSSFCell cell = row.getCell(columnIndex);
            if (cell == null) {
                cell = row.createCell(columnIndex);
            }

            if (value instanceof LocalDate) {
                cell.setCellValue(Date.from(((LocalDate) value).atTime(0, 0).toInstant(ZoneOffset.UTC)));
                cell.setCellStyle(dateStyle);
            } else if (value instanceof Integer) {
                cell.setCellValue(((Integer) value).doubleValue());
            } else {
                cell.setCellValue(String.valueOf(value));
            }
        }

        /**
         * Create name-index header map.
         * 
         * @return
         */
        private Map<String, Integer> header() {
            return nameToIndex.computeIfAbsent(row.getSheet(), key -> {
                HashMap<String, Integer> map = new HashMap();
                XSSFRow header = row.getSheet().getRow(0);

                for (int i = 0; i < header.getLastCellNum(); i++) {
                    XSSFCell cell = header.getCell(i);

                    if (cell != null) {
                        Integer columnIndex = cell.getColumnIndex();
                        String normalized = normalize(value(cell, String.class));

                        map.put(normalized, columnIndex);
                    }
                }
                return map;
            });
        }

        /**
         * Helper method to convert cell name to index.
         * 
         * @param name A cell name.
         * @return A cell index.
         */
        private int indexOf(String name, boolean ignore404) {
            int index = header().get(normalize(name));

            if (index == -1) {
                if (ignore404) {
                    return -1;
                }
                throw new Error(row.getSheet().getSheetName() + "には『" + name + "』という名称の列が存在しません。");
            }
            return index;
        }

        /**
         * Retrieve the cell's value.
         * 
         * @param cell
         * @param modelClass
         * @return
         */
        private static <M> M value(XSSFCell cell, Class<M> modelClass) {
            if (cell == null) {
                return initial(modelClass);
            }

            switch (cell.getCellType()) {
            case BLANK:
                return blank(cell, modelClass);

            case STRING:
                return string(cell, cell.getStringCellValue(), modelClass);

            case NUMERIC:
                return numeric(cell, cell.getNumericCellValue(), modelClass);

            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                case BLANK:
                    return blank(cell, modelClass);

                case STRING:
                    return string(cell, cell.getStringCellValue(), modelClass);

                case NUMERIC:
                    return numeric(cell, cell.getNumericCellValue(), modelClass);

                default:
                    break;
                }

            default:
                break;
            }
            return initial(modelClass);
        }

        /**
         * Retrieve value from the blank cell.
         * 
         * @param cell
         * @param value
         * @param modelClass
         * @return
         */
        private static <M> M blank(XSSFCell cell, Class<M> modelClass) {
            int rowIndex = cell.getRowIndex();
            int columnIndex = cell.getColumnIndex();

            XSSFSheet sheet = cell.getSheet();
            int size = sheet.getNumMergedRegions();

            for (int i = 0; i < size; i++) {
                CellRangeAddress range = sheet.getMergedRegion(i);

                if (range.isInRange(rowIndex, columnIndex)) {
                    return value(sheet.getRow(range.getFirstRow()).getCell(range.getFirstColumn()), modelClass);
                }
            }
            return initial(modelClass);
        }

        /**
         * Retrieve value from the string cell.
         * 
         * @param cell
         * @param value
         * @param modelClass
         * @return
         */
        private static <M> M string(XSSFCell cell, String value, Class<M> modelClass) {
            M model = I.transform(value, modelClass);

            PhoneticAware aware = I.find(PhoneticAware.class, modelClass);

            if (aware != null) {
                aware.setPhonetic(model, ruby(cell));
            }
            return model;
        }

        /**
         * Retrieve value from the numeric cell.
         * 
         * @param cell
         * @param value
         * @param modelClass
         * @return
         */
        private static <M> M numeric(XSSFCell cell, double numeric, Class<M> modelClass) {
            if (modelClass == int.class || modelClass == Integer.class) {
                return (M) Integer.valueOf((int) numeric);
            }

            if (modelClass == long.class || modelClass == Long.class) {
                return (M) Long.valueOf((long) numeric);
            }

            if (modelClass == float.class || modelClass == Float.class) {
                return (M) Float.valueOf((float) numeric);
            }

            if (modelClass == double.class || modelClass == Double.class) {
                return (M) Double.valueOf(numeric);
            }

            if (modelClass == LocalDate.class) {
                return (M) cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }

            if (modelClass == LocalTime.class) {
                return (M) cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
            }

            String numericText = String.valueOf(numeric);

            if (numericText.endsWith(".0")) {
                numericText = numericText.substring(0, numericText.length() - 2);
            }
            return I.transform(numericText, modelClass);
        }

        /**
         * Normalize text.
         * 
         * @param text
         * @return
         */
        private static String normalize(String text) {
            text = normalize(text);
            text = text.replaceAll("\\s", "");

            return text;
        }

        /**
         * Return the initial value for the specified type.
         * 
         * @param type
         * @return
         */
        private static <T> T initial(Class<T> type) {
            if (type == int.class || type == Integer.class) {
                return (T) Integer.valueOf(0);
            }

            if (type == double.class || type == Double.class) {
                return (T) Double.valueOf(0);
            }

            if (type == long.class || type == Long.class) {
                return (T) Long.valueOf(0);
            }

            if (type == float.class || type == Float.class) {
                return (T) Float.valueOf(0);
            }

            if (type == boolean.class || type == Boolean.class) {
                return (T) Boolean.FALSE;
            }

            if (type == String.class) {
                return (T) "";
            }
            return null;
        }

        /**
         * Helper method to retrieve the cell value as {@link String}.
         * 
         * @param cell
         * @return
         */
        private static String ruby(XSSFCell cell) {
            StringBuilder builder = new StringBuilder();

            cell.getRichStringCellValue().getCTRst();

            for (CTPhoneticRun run : cell.getRichStringCellValue().getCTRst().getRPhArray()) {
                builder.append(run.getT());
            }
            return builder.toString();
        }
    }

    /**
     * Write out the new excel file.
     * 
     * @param <T>
     * @param output
     * @param items
     * @param writer
     */
    public static <T> void write(File output, List<T> items, WiseBiConsumer<RowWriter, T> writer) {
        XSSFWorkbook book = new XSSFWorkbook();
        XSSFSheet sheet = book.createSheet();

        try {
            int count = 0;
            for (int i = 0; i < items.size(); i++) {
                T item = items.get(i);
                if (item != null) {
                    writer.accept(new RowWriter(sheet.createRow(count++)), item);
                }
            }

            book.write(output.newOutputStream());
        } catch (Exception e) {
            throw I.quiet(e);
        } finally {
            try {
                book.close();
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * 
     */
    public static class RowWriter {

        private final XSSFRow row;

        private int count;

        /**
         * @param row
         */
        private RowWriter(XSSFRow row) {
            this.row = row;
        }

        /**
         * Write the new cell.
         * 
         * @param value
         * @return
         */
        public RowWriter add(String value) {
            XSSFCell cell = row.createCell(count++);
            cell.setCellValue(value);

            return this;
        }
    }
}