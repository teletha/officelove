/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.macro;

import offishell.macro.lol.Talon;

/**
 * @version 2016/10/02 17:12:41
 */
public class SampleMacro extends Macro {

    /**
     * Entry point.
     * 
     * @param args
     */
    public static void main(String[] args) {
        Macro.use(Talon.class);
    }
}
