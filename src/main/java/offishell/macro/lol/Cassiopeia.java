/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php\p
 */
package offishell.macro.lol;

/**
 * @version 2016/10/05 16:59:32
 */
public class Cassiopeia extends LoLMacro {

    /**
     * 
     */
    private Cassiopeia() {
        championOnly = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeCastTime(Skill skill) {
        switch (skill) {
        case Q:
            return 170;
        case W:
            return 180;
        case E:
            return 90;
        }
        return super.computeCastTime(skill);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void combo() {
        cast(Skill.R);
        cast(Skill.W);
        cast(Skill.Q);
        cast(Skill.E);
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
