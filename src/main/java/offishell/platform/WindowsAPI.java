/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.platform;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.StringJoiner;
import java.util.function.Consumer;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import kiss.I;

/**
 * @version 2016/10/04 20:51:38
 */
class WindowsAPI implements offishell.platform.Native<HWND> {

    /** The clipboard. */
    private static final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();

    /** Instance of USER32.DLL for use in accessing native functions. */
    private static final GDI GDI = (GDI) Native.loadLibrary("gdi32", GDI.class, W32APIOptions.DEFAULT_OPTIONS);

    /** Instance of USER32.DLL for use in accessing native functions. */
    private static final Shell32 Shell = (Shell32) Native.loadLibrary("shell32", Shell32.class, W32APIOptions.DEFAULT_OPTIONS);

    /** Instance of USER32.DLL for use in accessing native functions. */
    private static final User User = (User) Native.loadLibrary("user32", User.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getColor(int x, int y) {
        return Color.of(GDI.GetPixel(User.GetDC(null), x, y));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getColor(Location location) {
        return getColor(location.x, location.y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getCursorPosition() {
        POINT point = new POINT();
        User.GetCursorPos(point);
        return new Location(point.x, point.y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getActiveClientRect() {
        RECT rect = new RECT();
        User.GetClientRect(User.GetForegroundWindow(), rect);
        return new int[] {rect.top, rect.left, rect.bottom, rect.right};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getWindowPosition(HWND windowID) {
        RECT rect = new RECT();
        User.GetWindowRect(windowID, rect);
        return new Location(rect.left, rect.top, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWindowTitle(HWND windowID) {
        return text(windowID, User::GetWindowText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeWindow(HWND windowID) {
        User.PostMessage(windowID, WinUser.WM_CLOSE, null, null);

        while (User.IsWindowVisible(windowID)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HWND activeWindow() {
        return User.GetForegroundWindow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enumWindows(Consumer<HWND> process) {
        User.EnumWindows((hwnd, pointer) -> {
            process.accept(hwnd);
            return true;
        }, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Object... command) {
        if (0 < command.length) {
            StringJoiner joiner = new StringJoiner(" ");

            for (int i = 1; i < command.length; i++) {
                joiner.add(String.valueOf(command[i]));
            }
            Shell.ShellExecute(null, "open", String.valueOf(command[0]), joiner.toString(), null, User32.SW_HIDE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String ocr(int x, int y, int width, int height) {
        try {
            execute("Capture2Text.exe", x, y, x + width, y + height);
            return (String) clip.getData(DataFlavor.stringFlavor);
        } catch (Throwable e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Helper method to read text from the specified handle.
     * </p>
     * 
     * @param id
     * @param consumer
     * @return
     */
    private static <T> String text(T id, TriFunction<T, char[], Integer, Integer> consumer) {
        char[] text = new char[512];
        int size = consumer.apply(id, text, text.length);
        return new String(text, 0, size);
    }

    /**
     * @version 2016/08/01 14:43:30
     */
    private static interface TriFunction<Param1, Param2, Param3, Return> {

        Return apply(Param1 param1, Param2 param2, Param3 param3);
    }

    /**
     * @version 2016/10/04 21:28:46
     */
    private static interface User extends StdCallLibrary, User32 {

        /**
         * Retrieves the position of the mouse cursor, in screen coordinates.
         *
         * @param p lpPoint [out]<br>
         *            Type: LPPOINT<br>
         *            A pointer to a POINT structure that receives the screen coordinates of the
         *            cursor.
         * @return Type: BOOL.<br>
         *         Returns nonzero if successful or zero otherwise. To get extended error
         *         information, call GetLastError.
         */
        boolean GetCursorPos(POINT p);

        /**
         * This function retrieves the coordinates of a window's client area. The client coordinates
         * specify the upper-left and lower-right corners of the client area. Because client
         * coordinates are relative to the upper-left corner of a window's client area, the
         * coordinates of the upper-left corner are (0,0).
         *
         * @param hWnd Handle to the window.
         * @param rect Long pointer to a RECT structure that structure that receives the client
         *            coordinates. The left and top members are zero. The right and bottom members
         *            contain the width and height of the window.
         * @return If the function succeeds, the return value is nonzero. If the function fails, the
         *         return value is zero.
         */
        boolean GetClientRect(HWND hWnd, RECT rect);
    }

    /**
     * @version 2016/10/03 9:28:46
     */
    private static interface GDI extends StdCallLibrary, GDI32 {

        /**
         * The GetPixel function retrieves the red, green, blue (RGB) color value of the pixel at
         * the specified coordinates.
         *
         * @param uCode The virtual key code or scan code for a key.
         * @param uMapType The translation to be performed.
         * @return The return value is either a scan code, a virtual-key code, or a character value,
         *         depending on the value of uCode and uMapType. If there is no translation, the
         *         return value is zero.
         */
        int GetPixel(HDC hdc, int x, int y);
    }
}
