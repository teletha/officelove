/*
 * Copyright (C) 2019 offishell Development Team
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
        require("net.java.dev.jna", "jna", "4.2.2");
        require("net.java.dev.jna", "jna-platform", "4.2.2");
        require("org.apache.poi", "poi-ooxml");
        require("org.apache.poi", "ooxml-schemas");
        require("javax.xml.bind", "jaxb-api");
        require("com.sun.xml.bind", "jaxb-core");
        require("com.sun.xml.bind", "jaxb-impl");
        require("org.openjfx", "javafx-controls");
        require("org.openjfx", "javafx-media");
        require("org.immutables", "value", "2.7.5").atAnnotation();

        versionControlSystem("https://github.com/teletha/offishell");
    }
}
