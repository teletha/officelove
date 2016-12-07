/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.task;

import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyles;

import kiss.Events;
import kiss.Extensible;
import kiss.I;
import offishell.Date;
import offishell.excel.Excel;
import offishell.file.Directory;
import offishell.word.Word;

/**
 * @version 2016/06/30 16:16:12
 */
public interface Task extends Extensible {

    /**
     * <p>
     * 担当係を返す。
     * </p>
     * 
     * @return
     */
    default String category() {
        String name = getClass().getPackage().getName();
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /**
     * <p>
     * 仕事の名前を返す。
     * </p>
     * 
     * @return
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * <p>
     * 関連する月を取得する。
     * </p>
     * 
     * @return
     */
    default Date month() {
        List<XWPFParagraph> paras = mainWord().paragraphs.toList();
        List<String> methods = Events.from(new Error().getStackTrace())
                .take(e -> e.getClassName().equals(getClass().getName()))
                .map(e -> e.getMethodName())
                .toList();

        XWPFStyles styles = paras.get(0).getDocument().getStyles();
        String heading = "";

        for (XWPFParagraph para : paras) {
            String text = para.getText();
            String id = para.getStyleID();

            if (id != null && styles.getStyle(id).getName().toLowerCase().contains("heading")) {
                heading = text;
            }

            if (methods.stream().anyMatch(text::contains)) {
                // parse heading text
                heading = Normalizer.normalize(heading, Form.NFKC);

                int start = heading.indexOf("(");
                int end = heading.indexOf(")");

                if (start != -1 && end != -1) {
                    heading = heading.substring(start + 1, end);

                    Matcher matcher = Pattern.compile("((\\d+)年)?(\\d+)月.*").matcher(heading);

                    if (matcher.matches()) {
                        int year = matcher.group(1) == null ? LocalDate.now().getYear() : Integer.parseInt(matcher.group(2));

                        return Date.of(year, Integer.parseInt(matcher.group(3)), 1);
                    }
                }
                return Date.now();
            }
        }
        return Date.now();
    }

    /**
     * <p>
     * 作業ディレクトリを返す。
     * </p>
     * 
     * @return
     */
    default Path directory() {
        return Directory.by(category()).resolve(name());
    }

    /**
     * <p>
     * 年度用ディレクトリを返す。
     * </p>
     * 
     * @return
     */
    default Path directoryFor(Date date) {
        return directory().resolve(String.valueOf(date.period()) + "年度");
    }

    /**
     * <p>
     * Find file from the current task directory properly.
     * </p>
     * 
     * @return
     */
    default Path file(String name) {
        return directory().resolve(name);
    }

    /**
     * <p>
     * Find Word file from the current task directory properly.
     * </p>
     * 
     * @return
     */
    default Excel excel(String name) {
        return Excel.of(directory(), name);
    }

    /**
     * <p>
     * Find Word file from the current task directory properly.
     * </p>
     * 
     * @return
     */
    default Excel excel(int year, String name) {
        return Excel.of(directory().resolve(String.valueOf(year)), name);
    }

    /**
     * <p>
     * まとめファイルを返す。
     * </p>
     * 
     * @return
     */
    default Excel mainExcel() {
        return excel("まとめ（" + name() + "一覧）");
    }

    /**
     * <p>
     * Find Word file from the current task directory properly.
     * </p>
     * 
     * @return
     */
    default Word word(String name) {
        return Word.of(directory(), name);
    }

    /**
     * <p>
     * Find Word file from the specified form.
     * </p>
     * 
     * @return
     */
    default <T extends FiniteForm> Word word(Class<T> type, Consumer<T> setting) {
        T form = I.make(type);
        setting.accept(form);

        return Word.of(form.locate()).calculate(form);
    }

    /**
     * <p>
     * Find Word file from the specified form.
     * </p>
     * 
     * @return
     */
    default <T extends FiniteForm, M> Word word(Class<T> type, Events<M> models, BiConsumer<T, M> setting) {
        return word(type, models.toList(), setting);
    }

    /**
     * <p>
     * Find Word file from the specified form.
     * </p>
     * 
     * @return
     */
    default <T extends FiniteForm, M> Word word(Class<T> type, List<M> models, BiConsumer<T, M> setting) {
        List<T> forms = new ArrayList();

        for (M model : models) {
            T form = I.make(type);
            setting.accept(form, model);

            forms.add(form);
        }
        return Word.of(forms.get(0).locate()).calculateAndMerge(forms);
    }

    /**
     * <p>
     * 業務タスクを記述したまとめファイルを返す。
     * </p>
     * 
     * @return
     */
    default Word mainWord() {
        return word("まとめ（" + name() + "関連業務）");
    }
}
