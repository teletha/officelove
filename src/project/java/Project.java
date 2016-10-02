/*
 * Copyright (C) 2016 Worker Helper Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
public class Project extends bee.api.Project {

    {
        product("offishell", "offishell", "1.0");

        require("npc", "sinobu", "1.0");
        require("net.java.dev.jna", "jna", "4.2.2");
        require("net.java.dev.jna", "jna-platform", "4.2.2");
        require("org.apache.poi", "poi-ooxml", "3.15-beta1");
        require("org.apache.poi", "ooxml-schemas", "1.3");
        require("com.1stleg", "jnativehook", "2.0.3");
        require("npc", "antibug", "0.3").atTest();
        requireLombok();
    }
}
