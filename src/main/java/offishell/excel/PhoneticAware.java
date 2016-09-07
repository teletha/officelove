/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.excel;

import kiss.Extensible;

/**
 * @version 2016/07/28 13:42:29
 */
public interface PhoneticAware<T> extends Extensible {

    /**
     * <p>
     * Set phonetic text for the specified model.
     * </p>
     * 
     * @param model
     * @param ruby
     */
    void setPhonetic(T model, String ruby);
}
