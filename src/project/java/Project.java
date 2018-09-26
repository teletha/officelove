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
        product("com.github.teletha", "offishell", "1.0");
        repository("https://oss.sonatype.org/content/repositories/snapshots");

        require("com.github.teletha", "sinobu", "1.0");
        require("com.github.teletha", "filer", "0.5");
        require("com.github.teletha", "antibug", "0.6").atTest();
        require("net.java.dev.jna", "jna", "4.2.2");
        require("net.java.dev.jna", "jna-platform", "4.2.2");
        require("org.apache.poi", "poi-ooxml", "3.17");
        require("org.apache.poi", "ooxml-schemas", "1.3");
        require("javax.xml.bind", "jaxb-api", "2.3.0");
        require("com.sun.xml.bind", "jaxb-core", "2.3.0");
        require("com.sun.xml.bind", "jaxb-impl", "2.3.0");
        require("org.openjfx", "javafx-controls", "11");
        require("org.openjfx", "javafx-media", "11");
    }
}
