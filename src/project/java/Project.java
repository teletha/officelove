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

        require("com.github.teletha", "sinobu", "1.0");
        require("com.github.teletha", "psychopath", "[0.9,)");
        require("com.github.teletha", "antibug", "0.6").atTest();
        require("net.java.dev.jna", "jna", "4.2.2");
        require("net.java.dev.jna", "jna-platform", "4.2.2");
        require("org.apache.poi", "poi-ooxml", "4.0.0");
        require("org.apache.poi", "ooxml-schemas", "1.4");
        require("javax.xml.bind", "jaxb-api", "2.3.0");
        require("com.sun.xml.bind", "jaxb-core", "2.3.0");
        require("com.sun.xml.bind", "jaxb-impl", "2.3.0");
        require("org.openjfx", "javafx-controls", "11");
        require("org.openjfx", "javafx-media", "11");
        require("org.immutables", "value", "2.7.5").atAnnotation();

        versionControlSystem("https://github.com/teletha/offishell");
    }
}
