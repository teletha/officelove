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

import static java.awt.event.KeyEvent.*;
import static org.jnativehook.keyboard.NativeKeyEvent.*;

import org.jnativehook.keyboard.NativeKeyEvent;

/**
 * @version 2016/10/02 17:14:34
 */
public enum Key {
    /** Virtual Key Code */
    N1('1', '1'),

    /** Virtual Key Code */
    N2('2', '2'),

    /** Virtual Key Code */
    N3('3', '3'),

    /** Virtual Key Code */
    N4('4', '4'),

    /** Virtual Key Code */
    N5('5', '5'),

    /** Virtual Key Code */
    N6('6', '6'),

    /** Virtual Key Code */
    N7('7', '7'),

    /** Virtual Key Code */
    N8('8', '8'),

    /** Vitual Key Code */
    N9('9', '9'),

    /** Virtual Key Code */
    N0('0', '0'),

    /** Virtual Key Code */
    A('a'),

    /** Virtual Key Code */
    B('b'),

    /** Virtual Key Code */
    C('c'),

    /** Virtual Key Code */
    D('d'),

    /** Virtual Key Code */
    E('e'),

    /** Virtual Key Code */
    F('f'),

    /** Virtual Key Code */
    G('g'),

    /** Virtual Key Code */
    H('h'),

    /** Virtual Key Code */
    I('i'),

    /** Virtual Key Code */
    J('j'),

    /** Virtual Key Code */
    K('k'),

    /** Virtual Key Code */
    L('l'),

    /** Virtual Key Code */
    M('m'),

    /** Virtual Key Code */
    N('n'),

    /** Virtual Key Code */
    O('o'),

    /** Virtual Key Code */
    P('p'),

    /** Virtual Key Code */
    Q('q'),

    /** Virtual Key Code */
    R('r'),

    /** Virtual Key Code */
    S('s'),

    /** Virtual Key Code */
    T('t'),

    /** Virtual Key Code */
    U('u'),

    /** Virtual Key Code */
    V('v'),

    /** Virtual Key Code */
    W('w'),

    /** Virtual Key Code */
    X('x'),

    /** Virtual Key Code */
    Y('y'),

    /** Virtual Key Code */
    Z('z'),

    /** Virtual Key Code */
    Up(VC_UP, VK_UP, false, true),

    /** Virtual Key Code */
    Down(VC_DOWN, VK_DOWN, false, true),

    /** Virtual Key Code */
    Right(VC_RIGHT, VK_RIGHT, false, true),

    /** Virtual Key Code */
    Left(VC_LEFT, VK_LEFT, false, true),

    /** Virtual Key Code */
    Space(VC_SPACE, VK_SPACE),

    /** Virtual Key Code */
    Backspace(VC_BACKSPACE, VK_BACK_SPACE),

    /** Virtual Key Code */
    Enter(VC_ENTER, VK_ENTER),

    /** Virtual Key Code */
    EnterInTenKey(VC_KP_SEPARATOR, VK_SEPARATOR),

    /** Virtual Key Code */
    Delete(VC_DELETE, VK_DELETE),

    /** Virtual Key Code */
    Escape(VC_ESCAPE, VK_ESCAPE),

    /** Virtual Key Code */
    Insert(VC_INSERT, VK_INSERT),

    /** Virtual Key Code */
    Tab(VC_TAB, VK_TAB),

    /** Virtual Key Code */
    Home(VC_HOME, VK_HOME),

    /** Virtual Key Code */
    End(VC_END, VK_END),

    /** Virtual Key Code */
    PageUp(VC_PAGE_UP, VK_PAGE_UP),

    /** Virtual Key Code */
    PageDown(VC_PAGE_DOWN, VK_PAGE_DOWN),

    /** Virtual Key Code */
    ControlRight(VC_CONTROL_R, VK_CONTROL, false, true),

    /** Virtual Key Code */
    ControlLeft(VC_CONTROL_L, VK_CONTROL, false, true),

    /** Virtual Key Code */
    ShiftRight(VC_SHIFT_R, VK_SHIFT, false, false),

    /** Virtual Key Code */
    ShiftLeft(VC_SHIFT_L, VK_SHIFT, false, false),

    /** Virtual Key Code */
    AltRight(VC_ALT_R, VK_ALT, true),

    /** Virtual Key Code */
    AltLeft(VC_ALT_L, VK_ALT, true),

    /** Virtual Key Code */
    F1(VC_F1, VK_F1, true),

    /** Virtual Key Code */
    F2(VC_F2, VK_F2, true),

    /** Virtual Key Code */
    F3(VC_F3, VK_F3, true),

    /** Virtual Key Code */
    F4(VC_F4, VK_F4, true),

    /** Virtual Key Code */
    F5(VC_F5, VK_F5, true),

    /** Virtual Key Code */
    F6(VC_F6, VK_F6, true),

    /** Virtual Key Code */
    F7(VC_F7, VK_F7, true),

    /** Virtual Key Code */
    F8(VC_F8, VK_F8, true),

    /** Virtual Key Code */
    F9(VC_F9, VK_F9, true),

    /** Virtual Key Code */
    F10(VC_F10, VK_F10, true),

    /** Virtual Key Code */
    F11(VC_F11, VK_F11, true),

    /** Virtual Key Code */
    F12(VC_F12, VK_F12, true);

    /** The {@link NativeKeyEvent} code. */
    public final int cqde;

    /** The native virtual key code. */
    public final int nativeCode;

    /** Is this key is system related? */
    final boolean system;

    /** Is this key is extended key? */
    final boolean extended;

    /**
     * <p>
     * Native key.
     * </p>
     * 
     * @param code
     */
    private Key(int code) {
        this(code, code - 32, false);
    }

    /**
     * <p>
     * Native key.
     * </p>
     * 
     * @param nativeCode
     */
    private Key(int code, int nativeCode) {
        this(code, nativeCode, false);
    }

    /**
     * <p>
     * Native key.
     * </p>
     * 
     * @param nativeCode
     */
    private Key(int code, int nativeCode, boolean system) {
        this(code, nativeCode, system, false);
    }

    /**
     * <p>
     * Native key.
     * </p>
     * 
     * @param nativeCode
     */
    private Key(int code, int nativeCode, boolean system, boolean extended) {
        this.cqde = code;
        this.nativeCode = nativeCode;
        this.system = system;
        this.extended = extended;
    }

    /**
     * <p>
     * Test key code.
     * </p>
     * 
     * @param e
     * @return
     */
    public boolean match(NativeKeyEvent e) {
        return e.getRawCode() == nativeCode;
    }
}
