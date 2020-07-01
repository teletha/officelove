/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell;

/**
 * @version 2016/06/22 17:38:33
 */
public class Padding {

    /** The padding size. */
    private int max;

    /** The padding text. */
    private String text = "";

    /** The padding type. */
    private boolean wide = false;

    /** The alignment. */
    private AlignType align = AlignType.Left;

    /**
     * <p>
     * Padder
     * </p>
     * 
     * @param text
     * @param max
     */
    private Padding(int max) {
        this.max = max;
    }

    /**
     * 字詰めの位置を指定する。
     * 
     * @param type
     * @return
     */
    public Padding pad(AlignType type) {
        this.align = type;

        return this;
    }

    /**
     * <p>
     * Format text with padding.
     * </p>
     * 
     * @param value
     * @return
     */
    public String format(Object value) {
        String text = String.valueOf(value);
        int length = text.length();

        if (wide) {
            text = convert(text);
        }

        if (max < length) {
            return text;
        } else {
            StringBuilder builder = new StringBuilder();

            if (align == AlignType.Left) {
                for (int i = max - length; 0 < i; i--) {
                    builder.append(this.text);
                }
            }

            builder.append(text);

            if (align == AlignType.Right) {
                for (int i = max - length; 0 < i; i--) {
                    builder.append(this.text);
                }
            }

            return builder.toString();
        }
    }

    /**
     * <p>
     * Convert to wide character.
     * </p>
     * 
     * @param value
     * @return
     */
    private String convert(String value) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if ('0' <= c && c <= '9') {
                builder.append((char) (c - '0' + '０'));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * @param maxNameSize
     * @return
     */
    public static Padding size(int size) {
        return new Padding(size);
    }

    /**
     * @param string
     * @return
     */
    public Padding text(String text) {
        this.text = text;
        this.wide = text.equals("　");

        return this;
    }

    /**
     * @return
     */
    public Padding onRight() {
        this.align = AlignType.Right;

        return this;
    }

    /**
     * @version 2016/06/27 11:40:53
     */
    private static enum AlignType {
        Left, Center, Right;
    }
}