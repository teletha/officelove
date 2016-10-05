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

import offishell.macro.lol.LoLMacro;

/**
 * @version 2016/10/02 17:12:41
 */
public class SampleMacro extends LoLMacro {

    /**
     * 
     */
    private SampleMacro() {
        // requireTitle("League of Legends (TM) Client");
    }

    /**
     * Entry point.
     * 
     * @param args
     */
    public static void main(String[] args) {
        Macro.use(SampleMacro.class);
    }
}
