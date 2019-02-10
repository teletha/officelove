/*
 * Copyright (C) 2019 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell;

import org.junit.jupiter.api.Test;

/**
 * @version 2016/07/18 13:01:49
 */
public class TextTest {

    @Test
    public void normalize() {
        assert Text.normalize("ａｂｃ１２３").equals("abc123");
        assert Text.normalize("［］[]").equals("［］［］");
        assert Text.normalize("｛｝{}").equals("｛｝｛｝");
        assert Text.normalize("（）()").equals("（）（）");
        assert Text.normalize("＠").equals("@");
        assert Text.normalize("+＋").equals("＋＋");
        assert Text.normalize("~～").equals("～～");
        assert Text.normalize("-－ー―").equals("--ー-");
        assert Text.normalize("ｷﾞｮﾋﾟﾁｬﾝ").equals("ギョピチャン");
        assert Text.normalize("空 　白").equals("空　　白");

        assert Text.normalize("髙").equals("高") == false;
    }
}
