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

import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import kiss.Extensible;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @version 2016/09/06 16:35:24
 */
public class WordStyleManager implements Extensible {

    /**
     * <p>
     * Define base style.
     * </p>
     * 
     * @return
     */
    public StyleDefinition base() {
        return new StyleDefinition();
    }

    /**
     * <p>
     * Style definition.
     * </p>
     * 
     * @version 2016/09/06 16:39:36
     */
    @Setter
    @Accessors(fluent = true, chain = true)
    public static class StyleDefinition {

        /** The font size. */
        private int size = -1;

        /** The font family name. */
        private String family;

        /** The font style. */
        private boolean bold;

        /** The font style. */
        private boolean italic;

        /** The font decoration. */
        private UnderlinePatterns underline = UnderlinePatterns.NONE;

        /**
         * <p>
         * Apply this style to the specified {@link XWPFRun}.
         * </p>
         * 
         * @param run
         */
        public void apply(XWPFRun run) {
            if (run != null) {
                run.setBold(bold);
                run.setFontFamily(family);
                run.setFontSize(size);
                run.setItalic(italic);
                run.setUnderline(underline);
            }
        }
    }
}
