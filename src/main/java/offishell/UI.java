/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import kiss.I;

/**
 * @version 2016/07/11 16:25:08
 */
public class UI {

    /**
     * <p>
     * Print the specified file.
     * </p>
     */
    public static void print(Path file) {
        try {
            Desktop.getDesktop().print(file.toFile());

            Thread.sleep(1000);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Open the specified file.
     * </p>
     */
    public static void open(Path file) {
        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (Throwable e) {
            throw I.quiet(e);
        }
    }

    /**
     * @param title
     * @return
     */
    public static Path selectDirectory(String title) {
        try {
            DirectoryDialog.title = title;

            Application.launch(DirectoryDialog.class);

            return DirectoryDialog.selected;
        } catch (Throwable e) {
            e.printStackTrace();

            throw e;
        } finally {
            DirectoryDialog.title = null;
            DirectoryDialog.selected = null;
            DirectoryDialog.directory = null;
        }
    }

    /**
     * <p>
     * Create UI for file selection.
     * </p>
     * 
     * @param title
     * @param filters
     * @return
     */
    public static Path selectFile(psychopath.File file) {
        return selectFile("Search " + file.base() + " file.", null, new ExtensionFilter(file.base(), file.name()));
    }

    /**
     * <p>
     * Create UI for file selection.
     * </p>
     * 
     * @param title
     * @param filters
     * @return
     */
    public static Path selectFile(String title, Path directory, ExtensionFilter... filters) {
        return selectFile(title, directory, Arrays.asList(filters));
    }

    /**
     * <p>
     * Create UI for file selection.
     * </p>
     * 
     * @param title
     * @param filters
     * @return
     */
    public static Path selectFile(String title, Path directory, List<ExtensionFilter> filters) {
        try {
            FileDialog.title = title;
            FileDialog.filters = filters;
            FileDialog.directory = directory;

            Application.launch(FileDialog.class);

            return FileDialog.selected;
        } catch (Throwable e) {
            e.printStackTrace();

            throw e;
        } finally {
            FileDialog.title = null;
            FileDialog.filters = null;
            FileDialog.selected = null;
            FileDialog.directory = null;
        }
    }

    /**
     * <p>
     * File chooser.
     * </p>
     * 
     * @version 2016/07/11 16:20:33
     */
    public static class FileDialog extends Application {

        private static String title;

        private static List<ExtensionFilter> filters;

        private static Path selected;

        private static Path directory;

        @Override
        public void start(Stage primaryStage) throws Exception {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(title);

            if (directory != null) {
                chooser.setInitialDirectory(directory.toFile());
            }

            if (filters != null) {
                chooser.getExtensionFilters().addAll(filters);
            }

            File file = chooser.showOpenDialog(primaryStage);

            if (file == null) {
                throw new Error(title);
            }
            selected = file.toPath();
            Platform.exit();
        }
    }

    /**
     * <p>
     * File chooser.
     * </p>
     * 
     * @version 2016/07/11 16:20:33
     */
    public static class DirectoryDialog extends Application {

        private static String title;

        private static Path selected;

        private static Path directory;

        @Override
        public void start(Stage primaryStage) throws Exception {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(title);

            if (directory != null) {
                chooser.setInitialDirectory(directory.toFile());
            }

            File file = chooser.showDialog(primaryStage);

            if (file == null) {
                throw new Error(title);
            }
            selected = file.toPath();
            Platform.exit();
        }
    }

    /**
     * @param title
     * @param items
     * @return
     */
    public static <T> T select(String title, List<T> items) {
        try {
            SelectDialog.title = title;
            SelectDialog.items = items;

            Application.launch(SelectDialog.class);

            return (T) SelectDialog.selected;
        } catch (Throwable e) {
            e.printStackTrace();

            throw e;
        } finally {
            SelectDialog.title = null;
            SelectDialog.items = null;
            SelectDialog.selected = null;
        }
    }

    /**
     * @version 2016/07/22 18:57:21
     */
    public static class SelectDialog extends Application {

        private static String title;

        private static List items;

        private static Object selected;

        @Override
        public void start(Stage stage) {
            ChoiceDialog dialog = new ChoiceDialog<>(null, items);
            dialog.setTitle(title);
            dialog.setContentText(title);

            // Traditional way to get the response value.
            selected = dialog.showAndWait().get();
        }
    }
}
