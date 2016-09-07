/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.task;

import java.nio.file.Path;

/**
 * @version 2016/07/22 12:54:00
 */
public interface FiniteForm<M> {

    /**
     * <p>
     * Locate form document.
     * </p>
     * 
     * @return
     */
    Path locate();

}
