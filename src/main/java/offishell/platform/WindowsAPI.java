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
import java.util.StringJoiner;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Consumer;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.MSG;
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

    /** Instance of USER32.DLL for use in accessing native functions. */
    private static final Kernel Kernel = (Kernel) Native.loadLibrary("kernel32", Kernel.class, W32APIOptions.DEFAULT_OPTIONS);

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
            return read();
        } catch (Throwable e) {
            throw I.quiet(e);
        }
    }

    private String read() {
        User.OpenClipboard(null);
        Pointer globalData = User.GetClipboardData(1);
        Pointer data = Kernel.GlobalLock(globalData);

        // if (CLibrary.strlen(data) <= maxTextLength)
        String text = data.getString(0);
        // else
        // text = new String(data.getCharArray(0, maxTextLength));
        System.out.println(text);
        User.CloseClipboard(null);

        return text;
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
    private static interface Kernel extends StdCallLibrary, Kernel32 {

        int GMEM_MOVEABLE = 0x2;

        Pointer GlobalAlloc(int uFlags, int dwBytes);

        Pointer GlobalLock(Pointer hMem);

        boolean GlobalUnlock(Pointer hMem);
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

        boolean OpenClipboard(Pointer hWnd);

        boolean CloseClipboard(Pointer hWnd);

        boolean EmptyClipboard();

        Pointer GetClipboardData(int format);

        Pointer CreateWindowEx(int dwExStyle, WString lpClassName, WString lpWindowName, int dwStyle, int x, int y, int nWidth, int nHeight, int hWndParent, int hMenu, int hInstance, int lpParam);

        boolean AddClipboardFormatListener(Pointer hWnd);

        boolean GetMessage(MSG lpMsg, Pointer hWnd, int wMsgFilterMin, int wMsgFilterMax);

        boolean IsClipboardFormatAvailable(int format);

        Pointer GetClipboardOwner();

        Pointer SetClipboardData(int format, Pointer hMem);
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

    public static void main(String[] args) {
        new Clipboards(1000);
    }

    public static class Clipboards {

        static public final int MOD_ALT = 0x1;

        static public final int MOD_CONTROL = 0x2;

        static public final int MOD_SHIFT = 0x4;

        static public final int MOD_WIN = 0x8;

        static public final int MOD_NOREPEAT = 0x4000;

        static public final byte VK_SHIFT = 0x10;

        static public final byte VK_CONTROL = 0x11;

        static public final byte VK_MENU = 0x12;

        static public final byte VK_LWIN = 0x5b;

        static public final byte VK_RWIN = 0x5c;

        static public final int KEYEVENTF_KEYUP = 2;

        static public final int GWL_WNDPROC = -4;

        static public final int WM_HOTKEY = 0x312;

        static public final int WM_CLIPBOARDUPDATE = 0x31D;

        static public final int WM_USER = 0x400;

        static public final int WM_LBUTTONDOWN = 0x201;

        static public final int WM_LBUTTONUP = 0x202;

        static public final int WM_RBUTTONDOWN = 0x204;

        static public final int WM_RBUTTONUP = 0x205;

        static public final int CF_TEXT = 1;

        static public final int CF_UNICODETEXT = 13;

        static public final int CF_HDROP = 15;

        static public final int IMAGE_ICON = 1;

        static public final int LR_LOADFROMFILE = 0x10;

        static public final int MONITOR_DEFAULTTONEAREST = 2;

        Pointer hwnd;

        final char[] chars = new char[2048];

        final int maxTextLength;

        public Clipboards(int maxTextLength) {
            System.out.println("create");
            this.maxTextLength = maxTextLength;

            final CyclicBarrier barrier = new CyclicBarrier(2);

            new Thread("Clipboard") {

                @Override
                public void run() {
                    hwnd = User.CreateWindowEx(0, new WString("STATIC"), new WString(""), 0, 0, 0, 0, 0, 0, 0, 0, 0);
                    if (hwnd == null) {
                        System.exit(0);
                    }

                    if (!User.AddClipboardFormatListener(hwnd)) {
                        System.exit(0);
                    }

                    try {
                        barrier.await();
                    } catch (Exception ignored) {
                    }

                    MSG msg = new MSG();
                    while (User.GetMessage(msg, (Pointer) null, WM_CLIPBOARDUPDATE, WM_CLIPBOARDUPDATE)) {
                        if (msg.message != WM_CLIPBOARDUPDATE) continue;
                        if (hwnd.equals(User.GetClipboardOwner())) {
                            continue;
                        }
                        changed();
                    }
                }
            }.start();

            try {
                barrier.await();
            } catch (Exception ignored) {
            }
        }

        protected void changed() {
            System.out.println("Changed ");
        }

        private boolean open(int millis) {
            int i = 0;
            while (!User.OpenClipboard(hwnd)) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                }
                i += 5;
                if (i > millis) {
                    return false;
                }
            }
            return true;
        }

        /** @return May be null. */
        public String getContents() {
            if (!open(500)) return null;
            try {
                int format;
                if (User.IsClipboardFormatAvailable(CF_UNICODETEXT)) {
                    format = CF_UNICODETEXT;
                } else if (User.IsClipboardFormatAvailable(CF_TEXT)) {
                    format = CF_TEXT;
                } else if (User.IsClipboardFormatAvailable(CF_HDROP)) {
                    format = CF_HDROP;
                } else {
                    return null;
                }

                Pointer globalData = User.GetClipboardData(format);
                if (globalData == null) {
                    return null;
                }

                Pointer data = Kernel.GlobalLock(globalData);
                if (data == null) {
                    return null;
                }

                String text = null;
                switch (format) {
                case CF_UNICODETEXT:
                    if (CLibrary.wcslen(data) <= maxTextLength)
                        text = data.getWideString(0);
                    else
                        text = new String(data.getCharArray(0, maxTextLength));
                    break;

                case CF_TEXT:
                    if (CLibrary.strlen(data) <= maxTextLength)
                        text = data.getString(0);
                    else
                        text = new String(data.getCharArray(0, maxTextLength));
                    break;
                //
                // case CF_HDROP:
                // int fileCount = DragQueryFile(data, -1, null, 0);
                // if (fileCount == 0) {
                // if (WARN) warn("Unable to query file count.");
                // return null;
                // }
                // StringBuilder buffer = new StringBuilder(512);
                // for (int i = 0; i < fileCount; i++) {
                // int charCount = DragQueryFile(data, i, chars, chars.length);
                // if (charCount == 0) {
                // if (WARN) warn("Unable to query file name.");
                // return null;
                // }
                // buffer.append(chars, 0, charCount);
                // buffer.append('\n');
                // }
                // buffer.setLength(buffer.length() - 1);
                // text = buffer.toString();
                // break;
                }

                Kernel.GlobalUnlock(globalData);

                return text;
            } finally {
                if (!User.CloseClipboard(hwnd)) {
                    return null;
                }
            }
        }

        public DataType getDataType() {
            if (User.IsClipboardFormatAvailable(CF_UNICODETEXT)) return DataType.text;
            if (User.IsClipboardFormatAvailable(CF_TEXT)) return DataType.text;
            if (User.IsClipboardFormatAvailable(CF_HDROP)) return DataType.files;
            return DataType.unknown;
        }

        public boolean setContents(String text) {
            if (!open(150)) return false;

            try {
                if (!User.EmptyClipboard()) {
                    return false;
                }

                Pointer data = Kernel.GlobalAlloc(Kernel.GMEM_MOVEABLE, (text.length() + 1) * 2); // 2
                                                                                                  // is
                // sizeof(WCHAR)
                if (data == null) {
                    return false;
                }

                Pointer buffer = Kernel.GlobalLock(data);
                if (buffer == null) {
                    return false;
                }
                buffer.setWideString(0, text);
                Kernel.GlobalUnlock(data);

                if (User.SetClipboardData(CF_UNICODETEXT, buffer) == null) {
                    return false;
                }
                return true;
            } finally {
                if (!User.CloseClipboard(hwnd)) {
                }
            }
        }

        static public enum DataType {
            unknown, text, files
        }
    }

    /**
     * @version 2016/10/12 2:55:40
     */
    static class CLibrary {

        public static native int strlen(Pointer p);

        public static native int wcslen(Pointer p);

        static {
            Native.register(Platform.C_LIBRARY_NAME);
        }
    }
}
