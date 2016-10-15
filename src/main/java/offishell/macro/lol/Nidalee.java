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

import java.util.concurrent.TimeUnit;

import offishell.macro.Key;
import offishell.platform.Color;

/**
 * @version 2016/10/05 17:05:25
 */
public class Nidalee extends LoLMacro {

    /** The mode manager. */
    private boolean human;

    /**
     * 
     */
    private Nidalee() {
        when(Key.R).release().delay(30, TimeUnit.MILLISECONDS).to(e -> {
            Color color = window().color(837, 994);

            if (color.is(8737282)) {
                human = true;
            } else if (color.is(8737537)) {
                human = false;
                cast(Skill.W);
            }
        });

        when(Key.E).consume().press().to(e -> {
            cast(Skill.E);

            if (human) {
                selfCast(Skill.E);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeCastTime(Skill skill) {
        switch (skill) {
        case W:
            return human ? 0 : 250;

        case E:
            return human ? 200 : 400;
        default:
            return super.computeCastTime(skill);
        }
    }

    /**
     * p {@inheritDoc}
     */
    @Override
    protected void combo() {
        if (human) {
            cast(Skill.AM);
            selfCast(Skill.E);
        } else {
            cast(Skill.W);
            cast(Skill.E);
            cast(Skill.AA);
            cast(Skill.Q);
        }
        cast(Skill.SS2);
    }

    /**
     * <p>
     * Try to transform.
     * </p>
     */
    private boolean transformToHuman() {
        if (human) {
            return true;
        }

        if (!cast(Skill.R)) {
            return false;
        }

        human = true;
        return true;
    }

    /**
     * <p>
     * Try to transform.
     * </p>
     */
    private boolean transformToCougar() {
        if (!human) {
            return true;
        }

        if (!cast(Skill.R)) {
            return false;
        }

        human = false;
        return true;
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
