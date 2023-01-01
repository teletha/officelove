/*
 * Copyright (C) 2023 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
public class Project extends bee.api.Project {

    {
        product("com.github.teletha", "officelove", ref("version.txt"));

        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "psychopath");
        require("com.github.teletha", "conjure");
        require("org.apache.poi", "poi-ooxml");
        require("com.github.teletha", "antibug").atTest();

        versionControlSystem("https://github.com/teletha/officelove");
    }
}