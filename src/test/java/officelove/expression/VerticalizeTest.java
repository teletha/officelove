/*
 * Copyright (C) 2025 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression;

import org.junit.jupiter.api.Test;

class VerticalizeTest {

    @Test
    void verticalize() {
        assert verricalize("alphabet").equals("alphabet");
        assert verricalize("ひらがなカタカナ漢字").equals("ひらがなカタカナ漢字");
    }

    @Test
    void number() {
        assert verricalize("123456789").equals("一二三四五六七八九");
    }

    // @Test
    // void one() {
    // assert verricalize("0").equals("零");
    // assert verricalize("1").equals("一");
    // assert verricalize("2").equals("二");
    // assert verricalize("3").equals("三");
    // assert verricalize("4").equals("四");
    // assert verricalize("5").equals("五");
    // assert verricalize("6").equals("六");
    // assert verricalize("7").equals("七");
    // assert verricalize("8").equals("八");
    // assert verricalize("9").equals("九");
    // }
    //
    // @Test
    // void ten() {
    // assert verricalize("10").equals("十");
    // assert verricalize("11").equals("十一");
    // assert verricalize("12").equals("十二");
    // assert verricalize("13").equals("十三");
    // assert verricalize("14").equals("十四");
    // assert verricalize("15").equals("十五");
    // assert verricalize("16").equals("十六");
    // assert verricalize("17").equals("十七");
    // assert verricalize("18").equals("十八");
    // assert verricalize("19").equals("十九");
    // }
    //
    // @Test
    // void twenty() {
    // assert verricalize("20").equals("廿");
    // assert verricalize("21").equals("廿一");
    // assert verricalize("22").equals("廿二");
    // assert verricalize("23").equals("廿三");
    // assert verricalize("24").equals("廿四");
    // assert verricalize("25").equals("廿五");
    // assert verricalize("26").equals("廿六");
    // assert verricalize("27").equals("廿七");
    // assert verricalize("28").equals("廿八");
    // assert verricalize("29").equals("廿九");
    // }
    //
    // @Test
    // void thirty() {
    // assert verricalize("30").equals("卅");
    // assert verricalize("31").equals("卅一");
    // assert verricalize("32").equals("卅二");
    // assert verricalize("33").equals("卅三");
    // assert verricalize("34").equals("卅四");
    // assert verricalize("35").equals("卅五");
    // assert verricalize("36").equals("卅六");
    // assert verricalize("37").equals("卅七");
    // assert verricalize("38").equals("卅八");
    // assert verricalize("39").equals("卅九");
    // }

    private String verricalize(String text) {
        StringBuilder builder = new StringBuilder(text);
        Parser.verticalize(builder);
        return builder.toString();
    }
}