/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officelove.excel;

import org.junit.jupiter.api.Test;

import officelove.excel.Excel;

class ExcelTest {

    @Test
    void normalize() {
        assert Excel.normalize("ａｂｃ１２３").equals("abc123");
        assert Excel.normalize("［］[]").equals("［］［］");
        assert Excel.normalize("｛｝{}").equals("｛｝｛｝");
        assert Excel.normalize("（）()").equals("（）（）");
        assert Excel.normalize("＠").equals("@");
        assert Excel.normalize("+＋").equals("＋＋");
        assert Excel.normalize("~～").equals("～～");
        assert Excel.normalize("-－ー―").equals("--ー-");
        assert Excel.normalize("ｷﾞｮﾋﾟﾁｬﾝ").equals("ギョピチャン");
        assert Excel.normalize("空 　白").equals("空　　白");

        assert Excel.normalize("髙").equals("高") == false;
    }
}
