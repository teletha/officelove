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
 * @version 2016/10/05 17:05:25
 */
public class Kayle extends LoLMacro {

    /**
     * 
     */
    Kayle() {
    }

    /**
     * p {@inheritDoc}
     */
    @Override
    protected void combo() {
        cast(Skill.Q);
        cast(Skill.E);
        cast(Skill.AM);
    }
}
