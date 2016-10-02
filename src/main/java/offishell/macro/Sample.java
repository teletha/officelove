/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.macro;

/**
 * @version 2016/10/02 13:11:15
 */
public class Sample {

    public static void main(String[] args) {
        Window window = Window.findByTitle("Java").to().getValue();
        window.color(10, 10);
    }
}
