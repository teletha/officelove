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

/**
 * @version 2016/10/06 17:42:19
 */
public class Talon extends LoLMacro {

    /**
     * 
     */
    protected Talon() {
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
        case W:
            return 280;
        case AA:
            return 310;
        case R:
            return 1000;
        }
        return super.computeCastTime(skill);
    }

    /**
     * スピードスパイク
     */
    void speed() {
        cast(Skill.Move);
        cast(Skill.Item6);

        if (cast(Skill.E)) {
            cast(Skill.W);
            cast(Skill.Q);
            cast(Skill.AA);
            cast(Skill.Item2);
            cast(Skill.SS2);
            cast(Skill.R);
        }
    }

    /**
     * パワースパイク
     */
    void power() {
        cast(Skill.Move);
        cast(Skill.Item6);

        if (cast(Skill.E)) {
            cast(Skill.SS2);
            cast(Skill.W);
            cast(Skill.AA);
            cast(Skill.Q);
            cast(Skill.AA);
            cast(Skill.Item2);
            cast(Skill.R);
        }
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
