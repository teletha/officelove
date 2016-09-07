/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.file;

import java.nio.file.Path;
import java.util.Objects;

import javafx.stage.FileChooser.ExtensionFilter;

import offishell.Text;

/**
 * @version 2016/07/18 14:23:30
 */
public class FileName {

    public final String name;

    public final String extension;

    /**
     * @param fileName
     */
    public FileName(Path fileName) {
        this(fileName.getFileName().toString());
    }

    /**
     * @param fileName
     */
    public FileName(String fileName) {
        if (fileName == null) {
            name = extension = "";
        } else {
            fileName = Text.normalize(fileName);
            int index = fileName.lastIndexOf(".");

            if (index == -1) {
                name = fileName;
                extension = "";
            } else {
                name = fileName.substring(0, index);
                extension = fileName.substring(index + 1).toLowerCase();
            }
        }
    }

    /**
     * <p>
     * Test file name equality.
     * </p>
     * 
     * @param path
     * @return
     */
    public boolean match(Path path) {
        return new FileName(path).match(this);
    }

    /**
     * <p>
     * Test file name with extension.
     * </p>
     * 
     * @param other
     * @param types
     * @return
     */
    public boolean match(FileName other, FileType... types) {
        if (Objects.equals(name, other.name) == false) {
            return false;
        }

        if (types.length == 0) {
            return true;
        } else {
            for (FileType type : types) {
                if (type.match(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * @return
     */
    public ExtensionFilter extensionFilter() {
        if (extension.isEmpty()) {
            return new ExtensionFilter("All", "*.*");
        } else {
            return new ExtensionFilter(name, toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (extension.isEmpty()) {
            return name;
        } else {
            return name + "." + extension;
        }
    }
}