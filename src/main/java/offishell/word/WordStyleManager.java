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

        /**
         * Get the size property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @return The size property.
         */
        public int getSize() {
            return size;
        }

        /**
         * Set the size property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @param size The size value to set.
         */
        public void setSize(int size) {
            this.size = size;
        }

        /**
         * Get the family property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @return The family property.
         */
        public String getFamily() {
            return family;
        }

        /**
         * Set the family property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @param family The family value to set.
         */
        public void setFamily(String family) {
            this.family = family;
        }

        /**
         * Get the bold property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @return The bold property.
         */
        public boolean isBold() {
            return bold;
        }

        /**
         * Set the bold property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @param bold The bold value to set.
         */
        public void setBold(boolean bold) {
            this.bold = bold;
        }

        /**
         * Get the italic property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @return The italic property.
         */
        public boolean isItalic() {
            return italic;
        }

        /**
         * Set the italic property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @param italic The italic value to set.
         */
        public void setItalic(boolean italic) {
            this.italic = italic;
        }

        /**
         * Get the underline property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @return The underline property.
         */
        public UnderlinePatterns getUnderline() {
            return underline;
        }

        /**
         * Set the underline property of this {@link WordStyleManager.StyleDefinition}.
         * 
         * @param underline The underline value to set.
         */
        public void setUnderline(UnderlinePatterns underline) {
            this.underline = underline;
        }
    }
}
