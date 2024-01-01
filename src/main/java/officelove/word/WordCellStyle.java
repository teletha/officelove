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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.poi.xwpf.usermodel.XWPFTableCell;

public class WordCellStyle {

    /** The secret prefix. */
    private static final String PREFIX = "LOVOFFICE_WORD_TABLE_CELL_STYLE_";

    /** The magic number. */
    private static int NUM = 0;

    /** The managed styles. */
    private static final Map<String, WordCellStyle> styles = new HashMap();

    /** The identifier for this style. */
    private final String id;

    /** The actual style builder. */
    private final Consumer<XWPFTableCell> styling;

    /**
     * @param styling
     */
    private WordCellStyle(Consumer<XWPFTableCell> styling) {
        this.id = PREFIX + String.format("%06d", NUM++);
        this.styling = styling;

        styles.put(id, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        return id;
    }

    /**
     * Build new style.
     * 
     * @param styling
     * @return
     */
    public static WordCellStyle of(Consumer<XWPFTableCell> styling) {
        return new WordCellStyle(styling);
    }

    /**
     * Apply style if needed.
     * 
     * @param in
     */
    static String apply(XWPFTableCell in, String text) {
        while (text.startsWith(PREFIX)) {
            String id = text.substring(0, PREFIX.length() + 6);
            text = text.substring(PREFIX.length() + 6);
            styles.get(id).styling.accept(in);
        }
        return text;
    }
}