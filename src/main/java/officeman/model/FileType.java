/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officeman.model;

import java.util.StringJoiner;
import java.util.function.Predicate;

import psychopath.Location;

/**
 * @version 2016/07/18 16:51:35
 */
public enum FileType {

    Word("doc", "docx", "docm"),

    Excel("ｘｌｓ", "xlsx", "xlsm");

    /** The list of extensions. */
    private final String[] extensions;

    /**
     * @param extensions
     */
    private FileType(String... extensions) {
        this.extensions = extensions;
    }

    /**
     * @param extension
     * @return
     */
    public boolean match(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }

        extension = extension.toLowerCase();

        for (String candidate : extensions) {
            if (candidate.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        for (String extension : extensions) {
            joiner.add(extension);
        }
        return joiner.toString();
    }

    /**
     * Create naming filter.
     * 
     * @param fileName
     * @return
     */
    public Predicate<Location<?>> by(String fileName) {
        return file -> file.base().equals(fileName) && match(file.extension());
    }

}
