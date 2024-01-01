/*
 * Copyright (C) 2024 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.word;

import org.junit.jupiter.api.Test;

class TemplateTypeTest extends WordTestSupport {

    @Test
    void detectPortraitSize() {
        assert TemplateType.parse(word("portrait/A3").docment()).name.equals("A3");
        assert TemplateType.parse(word("portrait/A4").docment()).name.equals("A4");
        assert TemplateType.parse(word("portrait/A5").docment()).name.equals("A5");

        assert TemplateType.parse(word("portrait/B4").docment()).name.equals("B4_ISO");
        assert TemplateType.parse(word("portrait/B5").docment()).name.equals("B5_ISO");

        assert TemplateType.parse(word("portrait/B4_JIS").docment()).name.equals("B4_JIS");
        assert TemplateType.parse(word("portrait/B5_JIS").docment()).name.equals("B5_JIS");
    }

    @Test
    void detectLandscapeSize() {
        assert TemplateType.parse(word("landscape/A3").docment()).name.equals("A3");
        assert TemplateType.parse(word("landscape/A4").docment()).name.equals("A4");
        assert TemplateType.parse(word("landscape/A5").docment()).name.equals("A5");

        assert TemplateType.parse(word("landscape/B4").docment()).name.equals("B4_ISO");
        assert TemplateType.parse(word("landscape/B5").docment()).name.equals("B5_ISO");

        assert TemplateType.parse(word("landscape/B4_JIS").docment()).name.equals("B4_JIS");
        assert TemplateType.parse(word("landscape/B5_JIS").docment()).name.equals("B5_JIS");
    }

    @Test
    void detectPortrait() {
        assert TemplateType.parse(word("portrait/A3").docment()).direction.equals("Portrait");
        assert TemplateType.parse(word("portrait/A4").docment()).direction.equals("Portrait");
        assert TemplateType.parse(word("portrait/A5").docment()).direction.equals("Portrait");

        assert TemplateType.parse(word("portrait/B4").docment()).direction.equals("Portrait");
        assert TemplateType.parse(word("portrait/B5").docment()).direction.equals("Portrait");

        assert TemplateType.parse(word("portrait/B4_JIS").docment()).direction.equals("Portrait");
        assert TemplateType.parse(word("portrait/B5_JIS").docment()).direction.equals("Portrait");
    }

    @Test
    void detectLandscape() {
        assert TemplateType.parse(word("landscape/A3").docment()).direction.equals("Landscape");
        assert TemplateType.parse(word("landscape/A4").docment()).direction.equals("Landscape");
        assert TemplateType.parse(word("landscape/A5").docment()).direction.equals("Landscape");

        assert TemplateType.parse(word("landscape/B4").docment()).direction.equals("Landscape");
        assert TemplateType.parse(word("landscape/B5").docment()).direction.equals("Landscape");

        assert TemplateType.parse(word("landscape/B4_JIS").docment()).direction.equals("Landscape");
        assert TemplateType.parse(word("landscape/B5_JIS").docment()).direction.equals("Landscape");
    }
}