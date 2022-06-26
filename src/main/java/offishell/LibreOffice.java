/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell;

import static java.nio.file.StandardOpenOption.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kiss.I;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

public class LibreOffice {

    /** The initialization flag. */
    private static boolean initialized;

    /**
     * Convert file.
     * 
     * @param input
     * @param output
     */
    public static void convert(File input, File output) {
        String inputFilePath = input.absolutize().toString();
        Directory outputDirectory = output.absolutize().parent();

        executeLibreOffice("--convert-to", output.extension(), "--outdir", outputDirectory.toString(), inputFilePath);

        outputDirectory.file(input.base()).extension(output.extension()).moveTo(output);
    }

    /**
     * Print file.
     * 
     * @param input
     */
    public static void print(File input) {
        executeLibreOffice("-p", input.absolutize().toString());
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
            executeLibreOffice("--pt", printer, input.absolutize().toString());
        }
    }

    /**
     * Execute the libereoffice command.
     * 
     * @param commands
     */
    private static synchronized void executeLibreOffice(String... commands) {
        if (initialized == false) {
            initialized = true;

            List<String> command = new ArrayList();
            command.add(searchLibreOffice().absolutize().toString());
            command.add("--quickstart");

            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectError();
                builder.redirectOutput();
                builder.redirectInput();
                Process process = builder.start();
                process.waitFor(20, TimeUnit.MICROSECONDS);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        List<String> command = new ArrayList();
        command.add(searchLibreOffice().absolutize().toString());
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

            Process process = builder.start();
            process.waitFor();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * Initialize LibreOffice.
     */
    private static synchronized void initialize() {
        if (initialized == false) {
            try {
                initialized = true;

                FileChannel channel = FileChannel.open(Path.of(".libreoffice"), CREATE_NEW, WRITE, DELETE_ON_CLOSE);
                FileLock lock = channel.tryLock();
                if (lock != null) {
                    List<String> command = new ArrayList();
                    command.add(searchLibreOffice().absolutize().toString());
                    command.add("--quickstart");

                    try {
                        ProcessBuilder builder = new ProcessBuilder(command);
                        builder.redirectError();
                        builder.redirectOutput();
                        builder.redirectInput();
                        Process process = builder.start();
                        process.waitFor(20, TimeUnit.MICROSECONDS);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Search libreoffice application.
     * 
     * @return
     */
    private static File searchLibreOffice() {
        File file = Locator.file("soffice.exe");

        if (file.isPresent()) {
            return file;
        }

        file = Locator.file("F:\\Application\\LibreOffice\\program\\soffice.exe");

        if (file.isPresent()) {
            return file;
        }

        file = Locator.file("D:\\Application\\LibreOffice\\program\\soffice.exe");

        if (file.isPresent()) {
            return file;
        }

        file = Locator.file("C:\\Program Files\\LibreOffice\\program\\soffice.exe");

        if (file.isPresent()) {
            return file;
        }

        throw new Error("Libre Office is not found.");
    }
}
