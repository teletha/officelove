/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package task;

import java.nio.file.Path;

import bee.util.ZipArchiver;
import kiss.I;

/**
 * @version 2016/07/01 12:53:08
 */
public class Exe extends bee.task.Exe {

    /**
     * {@inheritDoc}
     */
    @Override
    public Path build() {
        Path zip = super.build();

        ZipArchiver.unpack(zip, I.locate("E:\\布教"));

        return zip;
    }
}
