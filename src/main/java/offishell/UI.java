/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell;

import java.awt.Desktop;
import java.nio.file.Path;

import kiss.I;

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
}