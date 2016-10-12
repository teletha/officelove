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
public class Vi extends LoLMacro {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void combo() {
        cast(Skill.AM);
        cast(Skill.E);
    }

    /**
     * 
     */
    public static void main(String[] args) {
        LoLMacro.active();
    }
}
