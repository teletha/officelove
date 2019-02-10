/*
 * Copyright (C) 2019 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.platform;

/**
 * @version 2016/10/05 14:06:48
 */
public class Color {

    /** The original color code. */
    public final int code;

    /**
     * @param color
     */
    private Color(int color) {
        this.code = color;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        int red = code & 0x000000FF;
        int green = (code & 0x0000FF00) >> 8;
        int blue = (code & 0x00FF0000) >> 16;
        return Color.class
                .getSimpleName() + " [ " + format(code) + "\t\tR: " + format(red) + "\tG: " + format(green) + "\tB: " + format(blue) + "]";
    }

    /**
     * Format value.
     * 
     * @param value
     * @return
     */
    private String format(int value) {
        String text = String.valueOf(value);
        int remaing = 6 - text.length();

        for (int i = 0; i < remaing; i++) {
            text += " ";
        }
        return text;
    }

    /**
     * <p>
     * Create color.
     * </p>
     * 
     * @param color
     * @return
     */
    static Color of(int color) {
        return new Color(color);
    }

    /**
     * Test color code.
     */
    public boolean is(int code) {
        return this.code == code;
    }
}
