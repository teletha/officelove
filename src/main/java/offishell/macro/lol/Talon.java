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

import offishell.macro.Macro;

/**
 * @version 2016/10/06 17:42:19
 */
public class Talon extends LoLMacro {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void combo() {
        cast(Skill.Q);
        cast(Skill.E);
        cast(Skill.Item6);
        cast(Skill.AA);

        if (!canCast(Skill.E)) {
            cast(Skill.W, 150);
            cast(Skill.Item2, 50);
            cast(Skill.SS2);
            cast(Skill.R, 1000);
        }
    }

    /**
     * 
     */
    public static void main(String[] args) {
        Macro.use(Talon.class);
    }
}
