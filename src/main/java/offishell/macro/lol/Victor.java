/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.macro.lol;

import kiss.Events;
import kiss.Variable;
import offishell.macro.Key;
import offishell.macro.KeyEvent;
import offishell.platform.Location;

/**
 * @version 2016/10/06 17:42:19
 */
public class Victor extends LoLMacro {

    /**
     * 
     */
    private Victor() {
        Events<KeyEvent> startCastE = when(Key.E).consume().press();
        Events<KeyEvent> press = when(Key.MouseRight).press().skipUntil(startCastE).take(1).repeat();
        Events<KeyEvent> release = when(Key.MouseRight).release().skipUntil(startCastE).take(1).repeat();

        Variable<Location> location = press.map(KeyEvent::location).to();

        press.combine(release).to(e -> {
            int diffX = Math.abs(e.ⅰ.x() - e.ⅱ.x());
            int diffY = Math.abs(e.ⅰ.y() - e.ⅱ.y());
            long diffTime = Math.abs(e.ⅰ.time() - e.ⅱ.time());

            if ((100 < diffX || 100 < diffY) && 180 < diffTime) {

                System.out.println("RELESE");
                release(Key.E);
            } else {
                System.out.println("Cancel");
                input(Key.Escape);
                release(Key.E);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void combo() {
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
