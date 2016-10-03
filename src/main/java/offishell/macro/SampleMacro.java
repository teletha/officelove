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
 * @version 2016/10/02 17:12:41
 */
public class SampleMacro extends Macro {

    /**
     * 
     */
    private SampleMacro() {
        // requireTitle("League of Legends (TM) Client");

        whenPress(Key.P).run(() -> {
            System.out.println("Press P");
        });

        whenPress(Key.O).consume().run(() -> {
            System.out.println("Press O");
            press(Key.Q);
        });

        whenPress(Key.W).consume().run(() -> {
            press(Key.E).delay(2000);
            press(Key.Q);
        });
    }

    /**
     * Entry point.
     * 
     * @param args
     */
    public static void main(String[] args) {
        Macro.use(SampleMacro.class);
    }
}
