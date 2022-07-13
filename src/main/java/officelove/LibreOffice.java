/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officelove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

public class LibreOffice {

    /** The initialization flag. */
    private static boolean initialized;

    /** The path to executable office command. */
    private static String soffice;

    /** The program cache. */
    private static boolean cached;

    /**
     * Convert file.
     * 
     * @param input
     * @param output
     */
    public static void convert(File input, File output) {
        String inputFilePath = input.absolutize().toString();
        Directory outputDirectory = output.absolutize().parent();

        execute("--convert-to", output.extension(), "--outdir", outputDirectory.toString(), inputFilePath);

        outputDirectory.file(input.base()).extension(output.extension()).moveTo(output);
    }

    /**
     * Print file.
     * 
     * @param input
     */
    public static void print(File input) {
        execute("-p", input.absolutize().toString());
    }

    /**
     * Print file by the specified printer.
     * 
     * @param input
     * @param printer
     */
    public static void print(File input, String printer) {
        if (printer == null) {
            print(input);
        } else {
            execute("--pt", printer, input.absolutize().toString());
        }
    }

    /**
     * Execute the libereoffice command.
     * 
     * @param commands
     */
    private static synchronized void execute(String... commands) {
        search();

        List<String> command = new ArrayList();
        command.add(soffice.toString());
        command.add("--nolockcheck");
        command.add("--nologo");
        command.add("--headless");
        command.add("--norestore");
        command.add("--nofirststartwizard");
        command.addAll(Arrays.asList(commands));

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectError();
            builder.redirectOutput();
            builder.start().waitFor();

            // There is a reason why Quick Start mode is not activated before the first run of
            // Libereoffice. If another task (e.g., converting to PDF) is executed immediately after
            // Quick Start mode is launched, the task's exit signal will be notified first and all
            // processes will be forced to terminate. So we avoid this by running the quick start
            // mode after the task is finished.
            if (cached == false) {
                cached = true;
                new ProcessBuilder(soffice, "--quickstart").start().waitFor(250, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * Search libreoffice application.
     */
    private static void search() {
        if (initialized == false) {
            initialized = true;

            // search from 'path' environment variable
            for (Entry<String, String> entry : System.getenv().entrySet()) {
                if (entry.getKey().equalsIgnoreCase("PATH")) {
                    for (String path : entry.getValue().split(java.io.File.pathSeparator)) {
                        if (path.contains("soffice") && check(path)) {
                            soffice = path;
                            return;
                        }
                    }
                }
            }

            // search from 'LibreOffice' environment variable
            String path = I.env("LibreOffice");
            if (check(path)) {
                soffice = path;
                return;
            }

            // search from typical location
            if (System.getProperty("os.name").contains("Windows")) {
                String[] drives = {"C", "D", "E", "F", "G", "H", "I"};
                String[] dirs = {"Program Files", "Application", "Program", "Software"};
                for (String drive : drives) {
                    for (String dir : dirs) {
                        File file = Locator.file(drive + ":/" + dir + "/LibreOffice/program/soffice.exe");
                        if (check(file)) {
                            soffice = file.toString();
                            return;
                        }
                    }
                }
            }

            throw new Error("Libre Office is not found.");
        }
    }

    /**
     * Chech file validity.
     * 
     * @param path
     * @return
     */
    private static boolean check(String path) {
        return path != null && path.length() != 0 && Locator.file(path).isPresent();
    }

    /**
     * Chech file validity.
     * 
     * @param path
     * @return
     */
    private static boolean check(File file) {
        return file != null && file.isPresent();
    }
}
