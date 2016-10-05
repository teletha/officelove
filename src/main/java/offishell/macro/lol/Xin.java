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
 * @version 2016/10/05 16:59:32
 */
public class Xin extends LoLMacro {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void combo() {
        cast(Skill.SS2);
        cast(Skill.W);
        cast(Skill.AA, 200);
        cast(Skill.Q);
        cast(Skill.Item2);
        cast(Skill.Item6);

        if (!canCast(Skill.E)) {
            cast(Skill.R);
        }
    }
}
