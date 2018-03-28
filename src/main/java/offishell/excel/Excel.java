/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.excel;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPhoneticRun;

import kiss.Disposable;
import kiss.I;
import kiss.Signal;
import kiss.model.Model;
import kiss.model.Property;
import offishell.Date;
import offishell.Problem;
import offishell.Recoverable;
import offishell.Text;
import offishell.UI;
import offishell.file.Directory;
import offishell.file.FileName;
import offishell.file.FileType;
import offishell.macro.Window;

/**
 * @version 2016/07/16 14:38:19
 */
public class Excel {

    /** The cache. */
    private static final Map<Path, Excel> byPath = new HashMap();

    /** The cache. */
    private static final Map<XSSFWorkbook, Path> byBook = new HashMap();

    /** The actual file path. */
    public final Path path;

    /** The actual file name. */
    private final FileName excelName;

    /** The main excel file. */
    public final XSSFWorkbook book;

    /** The main excel sheet. */
    public final XSSFSheet sheet;

    /** The base cell style. */
    private final CellStyle baseStyle;

    /** The date cell style. */
    private final CellStyle dateStyle;

    /**
     * <p>
     * Create {@link Excel} wrapper.
     * </p>
     * 
     * @param path
     * @param book
     */
    private Excel(Path path, XSSFWorkbook book) {
        this.path = path;
        this.book = book;
        this.excelName = new FileName(path);
        this.sheet = book.getSheetAt(0);
        this.baseStyle = book.createCellStyle();
        this.dateStyle = book.createCellStyle();

        CreationHelper helper = book.getCreationHelper();
        DataFormat dateFormat = helper.createDataFormat();

        Font font = book.createFont();
        font.setFontName("游ゴシック Medium");
        font.setFontHeightInPoints((short) 10);
        baseStyle.setFont(font);
        baseStyle.setAlignment(CellStyle.ALIGN_CENTER);
        baseStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        baseStyle.setShrinkToFit(true);
        baseStyle.setWrapText(true);

        dateStyle.cloneStyleFrom(baseStyle);
        dateStyle.setDataFormat(dateFormat.getFormat("yyyy/mm/dd"));
    }

    /**
     * <p>
     * 指定したセルが空でない行を全て列挙します。
     * </p>
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
                        observer.accept(new Row(row));
                    }
                }
            }
            return disposer;
        });
    }

    /**
     * <p>
     * ヘッダを除いた行を全て返します。
     * </p>
     * 
     * @return
     */
    public Signal<XSSFRow> rowsWithCellBy(String name) {
        return rowsWithCellAt(indexOfHeader(name));
    }

    /**
     * <p>
     * 指定した位置番号のセルにデータが入っている行を全て返します。
     * </p>
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
     * <p>
     * Open excel file.
     * </p>
     * 
     * @return Chainable API
     */
    public Excel open() {
        if (!Window.existByTitle(path.getFileName().toString())) {
            UI.open(path);
        }

        // API definition
        return this;
    }

    /**
     * <p>
     * 指定のモデルに対応する行への操作を記述します。
     * </p>
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
     * <p>
     * 指定のモデルに対応する行を更新します。
     * </p>
     * 
     * @param models
     * @param operation
     * @return
     */
    public Excel update(Signal models) {
        return update(models.toList());
    }

    /**
     * <p>
     * 指定のモデルに対応する行を更新します。
     * </p>
     * 
     * @param models
     * @param operation
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
        save(path);

        return this;
    }

    public Excel save(Path path) {
        Recoverable.write(path, output -> {
            try {
                book.write(output);
            } catch (Throwable e) {
                throw I.quiet(e);
            }
        });
        return of(path);
    }

    public Excel save(String name) {
        return save(path.resolveSibling(new FileName(name).name + "." + excelName.extension));
    }

    private XSSFRow findFirstBlankRow() {
        XSSFRow head = sheet.getRow(0);

        // compute head size
        int headerSize = 0;

        for (; headerSize < head.getLastCellNum(); headerSize++) {
            Cell cell = head.getCell(headerSize);

            if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
                headerSize--;
                break;
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
                } else if (cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                    continue row;
                }
            }
            return row;
        }

        XSSFRow row = sheet.getRow(sheet.getLastRowNum());

        if (row == null) {
            row = sheet.createRow(sheet.getLastRowNum());
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

    /**
     * @param path
     * @return
     */
    public static Excel of(Path path) {
        return byPath.computeIfAbsent(path, key -> {
            try (InputStream input = Files.newInputStream(path)) {
                XSSFWorkbook book = new XSSFWorkbook(input);
                byBook.put(book, path);

                return new Excel(path, book);
            } catch (Exception e) {
                throw I.quiet(e);
            }
        });
    }

    /**
     * @param root
     * @param string
     */
    public static Excel of(Path directory, String fineName) {
        return of(Directory.of(directory).file(fineName, FileType.Excel));
    }

    /**
     * <p>
     * Retrieve {@link Excel} object for the specified {@link XSSFWorkbook}.
     * </p>
     * 
     * @param book
     * @return
     */
    private static Excel of(XSSFWorkbook book) {
        return Objects.requireNonNull(byPath.get(byBook.get(book)));
    }

    /**
     * <p>
     * Retrieve {@link Excel} object for the specified {@link XSSFSheet}.
     * </p>
     * 
     * @param model A target excel model.
     * @return An associated {@link Excel} instance.
     */
    private static Excel of(XSSFSheet model) {
        return of(Objects.requireNonNull(model).getWorkbook());
    }

    /**
     * <p>
     * Retrieve {@link Excel} object for the specified {@link XSSFRow}.
     * </p>
     * 
     * @param model A target excel model.
     * @return An associated {@link Excel} instance.
     */
    private static Excel of(XSSFRow model) {
        return of(Objects.requireNonNull(model).getSheet());
    }

    /**
     * <p>
     * Retrieve {@link Excel} object for the specified {@link XSSFCell}.
     * </p>
     * 
     * @param model A target excel model.
     * @return An associated {@link Excel} instance.
     */
    private static Excel of(XSSFCell model) {
        return of(Objects.requireNonNull(model).getSheet());
    }

    /**
     * <p>
     * Enhanced {@link XSSFRow}.
     * </p>
     * 
     * @version 2016/07/28 13:08:23
     */
    public static class Row {

        private static final Map<Object, Row> rows = new HashMap();

        private static final Map<XSSFSheet, Map<String, Integer>> nameToIndex = new HashMap();

        /** The source. */
        private final Excel excel;

        /** The actual row. */
        private final XSSFRow row;

        /**
         * <p>
         * Create wrapped row.
         * </p>
         * 
         * @param row
         */
        private Row(XSSFRow row) {
            Objects.requireNonNull(row);

            this.excel = Excel.of(row);
            this.row = row;
        }

        /**
         * <p>
         * Read the specified named cell's value and convert to the target model.
         * </p>
         * 
         * @param columnName
         * @param model
         * @return
         */
        public <M> M value(String columnName, M... model) {
            return value(indexOf(columnName, false), model);
        }

        /**
         * <p>
         * Read the specified named cell's value and convert to the target model.
         * </p>
         * 
         * @param columnName
         * @param model
         * @return
         */
        public <M> M value(String columnName, Class<M> model) {
            return value(indexOf(columnName, false), model);
        }

        /**
         * <p>
         * Read the specified indexed cell's value and convert to the target model.
         * </p>
         * 
         * @param columnIndex
         * @param modelClass
         * @return
         */
        public <M> M value(int columnIndex, M... modelClass) {
            return value(row.getCell(columnIndex), (Class<M>) modelClass.getClass().getComponentType());
        }

        /**
         * <p>
         * Read the specified indexed cell's value and convert to the target model.
         * </p>
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

            if (value instanceof Date) {
                cell.setCellValue(java.util.Date.from(Instant.from(((Date) value).date.atTime(0, 0).toInstant(ZoneOffset.UTC))));
                cell.setCellStyle(excel.dateStyle);
            } else if (value instanceof Integer) {
                cell.setCellValue(((Integer) value).doubleValue());
            } else {
                cell.setCellValue(String.valueOf(value));
            }
        }

        /**
         * <p>
         * Create name-index header map.
         * </p>
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
         * <p>
         * Helper method to convert cell name to index.
         * </p>
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
                throw Problem.of(row.getSheet().getSheetName() + "には『" + name + "』という名称の列が存在しません。");
            }
            return index;
        }

        /**
         * <p>
         * Retrieve the cell's value.
         * </p>
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
            case Cell.CELL_TYPE_BLANK:
                return blank(cell, modelClass);

            case Cell.CELL_TYPE_STRING:
                return string(cell, cell.getStringCellValue(), modelClass);

            case Cell.CELL_TYPE_NUMERIC:
                return numeric(cell, cell.getNumericCellValue(), modelClass);

            case Cell.CELL_TYPE_FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                case Cell.CELL_TYPE_BLANK:
                    return blank(cell, modelClass);

                case Cell.CELL_TYPE_STRING:
                    return string(cell, cell.getStringCellValue(), modelClass);

                case Cell.CELL_TYPE_NUMERIC:
                    return numeric(cell, cell.getNumericCellValue(), modelClass);
                }
            }
            return initial(modelClass);
        }

        /**
         * <p>
         * Retrieve value from the blank cell.
         * </p>
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
         * <p>
         * Retrieve value from the string cell.
         * </p>
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
         * <p>
         * Retrieve value from the numeric cell.
         * </p>
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

            if (modelClass == Date.class) {
                return (M) Date.of(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
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
         * <p>
         * Normalize text.
         * </p>
         * 
         * @param text
         * @return
         */
        private static String normalize(String text) {
            text = Text.normalize(text);
            text = text.replaceAll("\\s", "");

            return text;
        }

        /**
         * <p>
         * Return the initial value for the specified type.
         * </p>
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
         * <p>
         * Helper method to retrieve the cell value as {@link String}.
         * </p>
         * 
         * @param cell
         * @return
         */
        private static String ruby(XSSFCell cell) {
            StringBuilder builder = new StringBuilder();

            for (CTPhoneticRun run : cell.getRichStringCellValue().getCTRst().getRPhArray()) {
                builder.append(run.getT());
            }
            return builder.toString();
        }
    }
}
