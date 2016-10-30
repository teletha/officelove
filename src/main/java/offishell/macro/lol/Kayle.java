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

import offishell.macro.Key;

/**
 * @version 2016/10/05 17:05:25
 */
public class Kayle extends LoLMacro {

    /**
     * 
     */
    private Kayle() {
        championOnly = false;
        attackMotionRatio = 0.95;

        when(Key.W).consume().press().to(e -> {
            cast(Skill.W);
            selfCast(Skill.W);
        });

        when(Key.R).consume().press().to(e -> {
            cast(Skill.R);
            selfCast(Skill.R);
        });
    }

    /**
     * p {@inheritDoc}
     */
    @Override
    protected void combo() {
        cast(Skill.SS2);
        cast(Skill.E);
        cast(Skill.AM);
        cast(Skill.Q);
        selfCast(Skill.W);
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
