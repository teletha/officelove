/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officeman.model;

import java.util.ArrayList;
import java.util.List;

public class LinedText {

    private final List<String> lines = new ArrayList();

    /**
     * 文章を新規行で追記します。
     * 
     * @param text
     * @return
     */
    public LinedText line(String text) {
        if (text != null) {
            lines.add(text.strip());
        }
        return this;
    }

    /**
     * 指定した行数目の文章を取得します。
     * 
     * @param row
     * @return
     */
    public String line(int row) {
        row = row - 1;

        if (row < lines.size()) {
            return lines.get(row);
        } else {
            return "";
        }
    }
}