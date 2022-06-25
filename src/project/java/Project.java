/*
 * Copyright (C) 2022 The OFFISHELL Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
public class Project extends bee.api.Project {

    {
        product("com.github.teletha", "offishell", "1.0");
        repository("https://oss.sonatype.org/content/repositories/snapshots");

        require("com.github.teletha", "sinobu");
        require("com.github.teletha", "psychopath");
        require("com.github.teletha", "antibug").atTest();
        require("org.apache.poi", "poi-ooxml", "4.1.2");
        require("org.apache.poi", "ooxml-schemas", "1.4");
        require("javax.xml.bind", "jaxb-api");
        require("com.sun.xml.bind", "jaxb-core");
        require("com.sun.xml.bind", "jaxb-impl");
        require("fr.opensagres.xdocreport", "org.apache.poi.xwpf.converter.core");
        require("fr.opensagres.xdocreport", "org.apache.poi.xwpf.converter.pdf");

        versionControlSystem("https://github.com/teletha/offishell");
    }
}