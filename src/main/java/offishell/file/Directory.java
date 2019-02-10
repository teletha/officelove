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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import filer.Filer;
import javafx.stage.FileChooser.ExtensionFilter;
import kiss.I;
import kiss.Manageable;
import kiss.Singleton;
import kiss.Storable;
import offishell.UI;
import psychopath.File;
import psychopath.Location;
import psychopath.Locator;

/**
 * @version 2016/07/16 17:03:37
 */
public class Directory {

    /** The root directory to search. */
    private Path directory;

    private String findingText;

    private List<ExtensionFilter> findingFilter = new ArrayList();

    private Path findingDirectory;

    private boolean delete;

    private String memorize;

    /**
     * 
     */
    private Directory() {
    }

    /**
     * @param directory2
     */
    private Directory(Path directory) {
        try {
            if (!Files.isDirectory(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }

        this.directory = directory;
    }

    /**
     * <p>
     * Specify finding UI text.
     * </p>
     * 
     * @param text
     * @return
     */
    public Directory searchText(String text) {
        this.findingText = text;

        return this;
    }

    /**
     * <p>
     * Specify finding UI filters.
     * </p>
     * 
     * @param text
     * @return
     */
    public Directory searchFilter(String description, String... filters) {
        this.findingFilter.add(new ExtensionFilter(description, filters));

        return this;
    }

    /**
     * <p>
     * Specify finding directory.
     * </p>
     * 
     * @param text
     * @return
     */
    public Directory searchDirectory(Path directory) {
        this.findingDirectory = directory;

        return this;
    }

    /**
     * <p>
     * 検索ディレクトリを記憶して次回に使用します。
     * </p>
     * 
     * @param key
     * @return
     */
    public Directory memorizeSearchDirectory() {
        this.memorize = new Error().getStackTrace()[1].getMethodName();

        return this;
    }

    /**
     * <p>
     * Setting delete mode.
     * </p>
     * 
     * @return
     */
    public Directory deleteOriginalOnCopy() {
        this.delete = true;

        return this;
    }

    /**
     * <p>
     * Specify the file name which you want to select.
     * </p>
     * 
     * @param fileNameWithoutExtension
     * @return
     */
    public Path file(String fileName, FileType... types) {
        try {
            List<Location<?>> candidates = Locator.directory(directory).children().take(file -> match(file, fileName, types)).toList();

            if (!candidates.isEmpty()) {
                return candidates.get(0).asJavaPath();
            } else {
                MemorizedDirectory directories = I.make(MemorizedDirectory.class);

                if (findingText == null) {
                    findingText = fileName + "を選択してください";
                }

                Path selected = UI.selectFile(findingText, directories.get(memorize), findingFilter);
                File selectedFile = Locator.file(selected);

                Path output = directory.resolve(fileName + "." + selectedFile.extension());
                Filer.copy(selected, output);
                directories.put(memorize, selected.getParent());
                directories.store();

                if (delete) {
                    Filer.delete(selected);
                }
                return output;
            }
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * @param file
     * @param fileName
     * @param types
     * @return
     */
    private boolean match(Location file, String fileName, FileType[] types) {
        if (file.base().equals(fileName)) {
            for (FileType type : types) {
                if (type.match(file.extension())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * Create {@link Directory} for the specified directory.
     * </p>
     * 
     * @param directory
     * @return
     */
    public static Directory of(Path directory) {
        return new Directory(directory);
    }

    /**
     * @param category
     * @return
     */
    public static Path by(String category) {
        MemorizedDirectory dir = I.make(MemorizedDirectory.class).restore();
        Path path = dir.computeIfAbsent(category, key -> UI.selectDirectory(category));
        dir.store();

        return path;
    }

    /**
     * @version 2017/04/20 9:27:09
     */
    @Manageable(lifestyle = Singleton.class)
    private static class MemorizedDirectory extends HashMap<String, Path> implements Storable<MemorizedDirectory> {

        /**
        * 
        */
        private MemorizedDirectory() {
            restore();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String locate() {
            return Filer.locate("preferences").resolve(getClass().getName() + ".xml").toString();
        }
    }
}
