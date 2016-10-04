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

    Left(0x0002, 0x0004, Key.MouseLeft),

    Middle(0x0020, 0x0040, Key.MouseMiddle),

    Right(0x0008, 0x0010, Key.MouseRight),

    Move(0x0001, 0x0001, null),

    Wheel(0x0800, 0x0800, null),

    WheelTilt(0x01000, 0x01000, null),

    X1(0x0080, 0x0100, Key.MouseX1),

    X2(0x0080, 0x0100, Key.MouseX2);

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
