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

import offishell.platform.Location;

/**
 * @version 2016/10/16 10:26:38
 */
public interface KeyEvent {

    /**
     * <p>
     * Compute the mouse location when event was occured.
     * </p>
     * 
     * @return
     */
    Location location();
}
