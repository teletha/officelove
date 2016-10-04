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

/**
 * @version 2016/10/04 9:41:24
 */
public enum Mouse {

    Move(0x0001, 0x0001, null),

    Wheel(0x0800, 0x0800, null),

    WheelTilt(0x01000, 0x01000, null);

    final int startAction;

    final int endAction;

    public final Key key;

    /**
     * <p>
     * Define mouse action.
     * </p>
     * 
     * @param startAction
     * @param endAction
     */
    private Mouse(int startAction, int endAction, Key key) {
        this.startAction = startAction;
        this.endAction = endAction;
        this.key = key;
    }

}
