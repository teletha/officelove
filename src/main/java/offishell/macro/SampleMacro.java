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

        when(Key.Escape).press().to(e -> {
            System.exit(0);
        });

        when(Key.Q).press().to(e -> {
            System.out.println("Press Q");
            press(Mouse.Right);
        });

        when(Key.Q).release().to(e -> {
            System.out.println("Release " + e);
        });

        when(Key.W).press(true).to(e -> {
            press(Key.E).delay(1000);
            press(Key.Q);
        });

        when(Mouse.Move).skipUntil(when(Key.MouseLeft).press()).takeUntil(when(Key.MouseLeft).release()).repeat().to(e -> {
            System.out.println("Drag Left");
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
