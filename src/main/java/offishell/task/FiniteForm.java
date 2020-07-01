/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
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