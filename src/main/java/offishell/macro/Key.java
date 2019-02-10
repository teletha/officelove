/*
 * Copyright (C) 2019 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.macro;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * @version 2016/10/03 17:00:40
 */
public enum Key {

    MouseLeft(1, 0x0002, 0x0004),

    MouseRight(2, 0x0008, 0x0010),

    MouseMiddle(4, 0x0020, 0x0040),

    MouseX1(5, 0x0080, 0x0100),

    MouseX2(6, 0x0080, 0x0100),

    BackSpace(8),

    Tab(9),

    Clear(12),

    Return(13),

    Command(15),

    Shift(16),

    Control(17),

    Alt(18),

    Pause(19),

    CapsLock(20),

    IMEかな(21),

    IMEJunja(23),

    IMEFinal(24),

    IME漢字(25),

    Escape(27),

    IMEConvert(28),

    IMENonConvert(29),

    IMEAccept(30),

    IMEModeChange(31),

    Space(32),

    PageUp(33),

    PageDown(34),

    End(35),

    Home(36),

    Left(37),

    Up(38),

    Right(39),

    Down(40),

    PrintScreen(44),

    Insert(45),

    Delete(46),

    N0(48),

    N1(49),

    N2(50),

    N3(51),

    N4(52),

    N5(53),

    N6(54),

    N7(55),

    N8(56),

    N9(57),

    A(65),

    B(66),

    C(67),

    D(68),

    E(69),

    F(70),

    G(71),

    H(72),

    I(73),

    J(74),

    K(75),

    L(76),

    M(77),

    N(78),

    O(79),

    P(80),

    Q(81),

    R(82),

    S(83),

    T(84),

    U(85),

    V(86),

    W(87),

    X(88),

    Y(89),

    Z(90),

    WinLeft(91),

    WinRigth(92),

    Apps(93),

    NumPad0(96),

    NumPad1(97),

    NumPad2(98),

    NumPad3(99),

    NumPad4(100),

    NumPad5(101),

    NumPad6(102),

    NumPad7(103),

    NumPad8(104),

    NumPad9(105),

    Multiply(106),

    Add(107),

    Subtract(109),

    Decimal(110),

    Divide(111),

    F1(112),

    F2(113),

    F3(114),

    F4(115),

    F5(116),

    F6(117),

    F7(118),

    F8(119),

    F9(120),

    F10(121),

    F11(122),

    F12(123),

    F13(124),

    F14(125),

    F15(126),

    F16(127),

    NumLock(144),

    ScrollLock(145),

    ShiftLeft(160),

    ShiftRight(161),

    ControlLeft(162),

    ControlRight(163),

    AltLeft(164),

    AltRight(165),

    VolumeMute(173),

    VolumeDown(174),

    VolumeUp(175),

    MediaNextTrack(176),

    MediaPreviousTrac(177),

    MediaStop(178),

    MediaPlayPause(179),

    LaunchMail(180),

    LaunchMediaSelect(181),

    LaunchApp1(182),

    LaunchApp2(183),

    Colon(186),

    SemiColon(187),

    Comma(188),

    Minus(189),

    Period(190),

    Slash(191),

    AtMark(192),

    LeftBrace(219),

    BackSlash(220),

    RightBrace(221),

    Apostrophe(222),

    BackSlashJP(226),

    CapsLockJP(240),

    カタカナひらがな(242),

    半角全角(244),

    BackTab(245);

    /** The native virtual key code. */
    public final int virtualCode;

    /** The native scan code. */
    public final int scanCode;

    /** Mouse related event. */
    final boolean mouse;

    /** The mouse related button. */
    final int on;

    /** The mouse related button. */
    final int off;

    /**
     * <p>
     * Native key.
     * </p>
     * 
     * @param code
     */
    private Key(int virtualCode) {
        this(virtualCode, 0, 0);
    }

    /**
     * <p>
     * Native key.
     * </p>
     * 
     * @param virtualCode
     * @param on
     * @param off
     */
    private Key(int virtualCode, int on, int off) {
        this.virtualCode = virtualCode;
        this.scanCode = WindowsKeyCodeHelper.INSTANCE.MapVirtualKey(virtualCode, 0);
        this.on = on;
        this.off = off;
        this.mouse = on != 0 && off != 0;
    }

    /**
     * @version 2016/10/03 9:28:46
     */
    private interface WindowsKeyCodeHelper extends StdCallLibrary {

        /** Instance of USER32.DLL for use in accessing native functions. */
        WindowsKeyCodeHelper INSTANCE = (WindowsKeyCodeHelper) Native
                .loadLibrary("user32", WindowsKeyCodeHelper.class, W32APIOptions.DEFAULT_OPTIONS);

        /**
         * Translates (maps) a virtual-key code into a scan code or character value, or translates a
         * scan code into a virtual-key code.
         *
         * @param uCode The virtual key code or scan code for a key.
         * @param uMapType The translation to be performed.
         * @return The return value is either a scan code, a virtual-key code, or a character value,
         *         depending on the value of uCode and uMapType. If there is no translation, the
         *         return value is zero.
         */
        int MapVirtualKey(int uCode, int uMapType);
    }
}
