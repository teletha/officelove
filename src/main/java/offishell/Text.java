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

import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * @version 2016/07/18 12:41:15
 */
public class Text {

    /**
     * <p>
     * Helper method to normalize text.
     * </p>
     * 
     * @param text
     * @return
     */
    public static String normalize(Path fileName) {
        if (fileName == null) {
            return "";
        }
        return normalize(fileName.toString());
    }

    /**
     * <p>
     * Helper method to normalize text.
     * </p>
     * 
     * @param text
     * @return
     */
    public static String normalize(String text) {
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
}