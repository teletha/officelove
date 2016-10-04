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

        // whenPress(Key.P).run(() -> {
        // press(Mouse.Left);
        // });
        //
        // whenPress(Key.O).consume().run(() -> {
        // System.out.println("Press O");
        // press(Mouse.Right);
        // });
        //
        // whenPress(Key.W).consume().run(() -> {
        // press(Key.E).delay(2000);
        // press(Key.Q);
        // });
        //
        // whenPress(Mouse.Left).run(() -> {
        // System.out.println("Clicl Left");
        // });
        //
        // whenPress(Mouse.Right).consume().run(() -> {
        // System.out.println("Clicl Right");
        // });
        //
        // whenPress(Mouse.Middle).run(() -> {
        // System.out.println("Clicl Middle");
        // });
        //
        // whenPress(Key.Escape).consume().run(() -> {
        // System.exit(0);
        // });

        when(Key.Q).isPressed().to(e -> {
            System.out.println("Press " + e);
        });

        when(Key.Q).isReleased().to(e -> {
            System.out.println("Release " + e);
        });

        when(Mouse.Move).skipUntil(when(Key.MouseLeft).isPressed()).takeUntil(when(Key.MouseLeft).isReleased()).repeat().to(e -> {
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
