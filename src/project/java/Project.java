
/*
 * Copyright (C) 2025 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
public class Project extends bee.api.Project {

    {
        product("io.github.teletha", "officelove", ref("version.txt"));

        require("io.github.teletha", "sinobu");
        require("io.github.teletha", "psychopath");
        require("io.github.teletha", "conjure");
        require("org.apache.poi", "poi-ooxml");
        require("io.github.teletha", "antibug").atTest();

        versionControlSystem("https://github.com/teletha/officelove");
    }
}