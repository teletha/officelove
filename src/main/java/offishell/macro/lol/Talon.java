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
 * @version 2016/10/06 17:42:19
 */
public class Talon extends LoLMacro {

    /**
     * 
     */
    protected Talon() {
        championOnly = false;

        when(Skill.E, () -> {
            if (cast(Skill.E)) {
                press(Key.Space);
                delay(1000);
                release(Key.Space);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void combo() {
        power();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeCastTime(Skill skill) {
        switch (skill) {
        case Q:
            return 120;
        case W:
            return 80;
        case R:
            return 1000;
        }
        return super.computeCastTime(skill);
    }

    /**
     * パワースパイク
     */
    void power() {
        cast(Skill.Item6);
        cast(Skill.W);

        if (cast(Skill.Q)) {
            cast(Skill.SS2);
            cast(Skill.AA);
            cast(Skill.Item2);
            cast(Skill.R);
        }
    }

    /**
     * パワースパイク
     */
    void noULT() {
        cast(Skill.Item6);

        if (cast(Skill.Q)) {
            cast(Skill.SS2);
            cast(Skill.AA);
            cast(Skill.Item2);
        }
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
