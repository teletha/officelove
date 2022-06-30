/*
 * Copyright (C) 2022 The OFFISHELL Development Team
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

import org.apache.poi.xwpf.usermodel.XWPFRun;

public class WordStyle {

    /** The secret prefix. */
    private static final String PREFIX = "LOVOFFICE_WORD_STYLE_";

    /** The magic number. */
    private static int NUM = 0;

    /** The managed styles. */
    private static final Map<String, WordStyle> styles = new HashMap();

    /** The identifier for this style. */
    private final String id;

    /** The actual style builder. */
    private final Consumer<XWPFRun> styling;

    /**
     * @param styling
     */
    private WordStyle(Consumer<XWPFRun> styling) {
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
    public static WordStyle of(Consumer<XWPFRun> styling) {
        return new WordStyle(styling);
    }

    /**
     * Apply style if needed.
     * 
     * @param run
     * @param text
     * @return
     */
    static String apply(XWPFRun run, String text) {
        if (text.startsWith(PREFIX)) {
            String id = text.substring(0, PREFIX.length() + 6);
            text = text.substring(PREFIX.length() + 6);
            styles.get(id).styling.accept(run);
        }
        return text;
    }
}