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

import static java.util.concurrent.TimeUnit.*;

import kiss.I;
import offishell.macro.Key;

/**
 * @version 2016/10/30 14:08:15
 */
public class Kayle extends LoLMacro {

    /**
     * 
     */
    private Kayle() {
        attackMotionRatio = 0.95;

        when(Key.W).consume().press().to(e -> {
            cast(Skill.W);
            selfCast(Skill.W);
        });

        when(Key.R).consume().press().interval(3, SECONDS).to(e -> {
            cast(Skill.R);

            if (isCenter() || isLowHealth()) {
                I.signalInfinite(1, 100, MILLISECONDS).take(30).scan(0, (p, n) -> p + 1).to(skill -> {
                    selfCast(Skill.R);
                    System.out.println("TryCast " + skill);
                });
            }
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

        if (isLowHealth()) {
            selfCast(Skill.R);
        }
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
