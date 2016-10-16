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
import offishell.macro.Key;

/**
 * @version 2016/10/06 17:42:19
 */
public class Victor extends LoLMacro {

    /**
     * 
     */
    private Victor() {
        Events<Key> press = when(Key.MouseRight).press();
        Events<Key> release = when(Key.MouseRight).release();
        Events<Key> drag = press.takeUntil(release).repeat();

        drag.to(e -> {
            System.out.println("Draged");
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void combo() {
        cast(Skill.E);
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
