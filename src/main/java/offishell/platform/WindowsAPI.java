/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.platform;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.TypeMapper;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinReg.HKEY;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.win32.W32APITypeMapper;

import kiss.I;
import kiss.Observer;
import kiss.Signal;
import kiss.Variable;
import offishell.platform.WindowsAPI.ShellAPI.SHELLEXECUTEINFO;

/**
 * @version 2016/10/04 20:51:38
 */
class WindowsAPI implements offishell.platform.Native<HWND> {

    /** Instance of USER32.DLL for use in accessing native functions. */
    private static final GDI GDI = (GDI) Native.loadLibrary("gdi32", GDI.class, W32APIOptions.DEFAULT_OPTIONS);

    /** Instance of USER32.DLL for use in accessing native functions. */
    private static final Shell Shell = (Shell) Native.loadLibrary("shell32", Shell.class, W32APIOptions.DEFAULT_OPTIONS);

    /** Instance of USER32.DLL for use in accessing native functions. */
    private static final User User = (User) Native.loadLibrary("user32", User.class, W32APIOptions.DEFAULT_OPTIONS);

    /** Instance of USER32.DLL for use in accessing native functions. */
    private static final Kernel Kernel = (Kernel) Native.loadLibrary("kernel32", Kernel.class, W32APIOptions.DEFAULT_OPTIONS);

    /** The native clipboard manager. */
    private static final Clipboard clipboard = new Clipboard();

    private final Variable<String> ocr = clipboard().startWith("0.8").to();

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

            SHELLEXECUTEINFO info = new SHELLEXECUTEINFO();
            info.fMask = 0x00000040; // SEE_MASK_NOCLOSEPROCESS
            info.lpFile = String.valueOf(command[0]);
            info.lpParameters = joiner.toString();
            info.nShow = User32.SW_HIDE;
            Shell.ShellExecuteEx(info);
            Kernel32.INSTANCE.WaitForSingleObject(info.hProcess, 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String ocr(int x, int y, int width, int height) {
        try {
            execute("Capture2Text.exe", x, y, x + width, y + height);
            return ocr.v;
        } catch (Throwable e) {
            throw I.quiet(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Signal<String> clipboard() {
        return clipboard.read().skip(e -> e == null).diff();
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
     * @version 2016/10/12 12:34:21
     */
    private static interface Shell extends StdCallLibrary, Shell32 {

        /**
         * @param lpExecInfo
         *            <p>
         *            Type: <strong>SHELLEXECUTEINFO*</strong>
         *            </p>
         *            <p>
         *            A pointer to a <a href=
         *            "https://msdn.microsoft.com/en-us/library/windows/desktop/bb759784(v=vs.85).aspx">
         *            <strong xmlns="http://www.w3.org/1999/xhtml">SHELLEXECUTEINFO </strong></a>
         *            structure that contains and receives information about the application being
         *            executed.
         *            </p>
         * @return
         *         <p>
         *         Returns <strong>TRUE</strong> if successful; otherwise, <strong>FALSE</strong>.
         *         Call <a href=
         *         "https://msdn.microsoft.com/en-us/library/windows/desktop/ms679360(v=vs.85).aspx">
         *         <strong xmlns="http://www.w3.org/1999/xhtml">GetLastError </strong></a> for
         *         extended error information.
         *         </p>
         */
        boolean ShellExecuteEx(SHELLEXECUTEINFO lpExecInfo);
    }

    /**
     * @version 2016/10/04 21:28:46
     */
    private static interface Kernel extends StdCallLibrary, Kernel32 {

        /**
         * <p>
         * Locks a global memory object and returns a pointer to the first byte of the object's
         * memory block.
         * </p>
         * 
         * @param hMem A handle to the global memory object. This handle is returned by either the
         *            GlobalAlloc or GlobalReAlloc function.
         * @return If the function succeeds, the return value is a pointer to the first byte of the
         *         memory block. If the function fails, the return value is NULL. To get extended
         *         error information, call GetLastError.
         */
        Pointer GlobalLock(Pointer hMem);

        /**
         * <p>
         * Decrements the lock count associated with a memory object that was allocated with
         * GMEM_MOVEABLE. This function has no effect on memory objects allocated with GMEM_FIXED.
         * </p>
         * <p>
         * Note The global functions have greater overhead and provide fewer features than other
         * memory management functions. New applications should use the heap functions unless
         * documentation states that a global function should be used. For more information, see
         * Global and Local Functions.
         * </p>
         * 
         * @param hMem A handle to the global memory object. This handle is returned by either the
         *            GlobalAlloc or GlobalReAlloc function.
         * @return If the memory object is still locked after decrementing the lock count, the
         *         return value is a nonzero value. If the memory object is unlocked after
         *         decrementing the lock count, the function returns zero and GetLastError returns
         *         NO_ERROR. If the function fails, the return value is zero and GetLastError
         *         returns a value other than NO_ERROR.
         */
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

        /**
         * <p>
         * Opens the clipboard for examination and prevents other applications from modifying the
         * clipboard content.
         * </p>
         * 
         * @param hWnd A handle to the window to be associated with the open clipboard. If this
         *            parameter is NULL, the open clipboard is associated with the current task.
         * @return If the function succeeds, the return value is nonzero. If the function fails, the
         *         return value is zero. To get extended error information, call GetLastError.
         */
        boolean OpenClipboard(HWND hWnd);

        /**
         * <p>
         * Closes the clipboard.
         * </p>
         * 
         * @return If the function succeeds, the return value is nonzero. If the function fails, the
         *         return value is zero. To get extended error information, call GetLastError.
         */
        boolean CloseClipboard();

        /**
         * <p>
         * Retrieves data from the clipboard in a specified format. The clipboard must have been
         * opened previously.
         * </p>
         * 
         * @param format A clipboard format. For a description of the standard clipboard formats,
         *            see Standard Clipboard Formats.
         * @return If the function succeeds, the return value is the handle to a clipboard object in
         *         the specified format. If the function fails, the return value is NULL. To get
         *         extended error information, call GetLastError.
         */
        Pointer GetClipboardData(int format);

        /**
         * <p>
         * Places the given window in the system-maintained clipboard format listener list.
         * </p>
         * 
         * @param hWnd A handle to the window to be placed in the clipboard format listener list.
         * @return Returns TRUE if successful, FALSE otherwise. Call GetLastError for additional
         *         details.
         */
        boolean AddClipboardFormatListener(HWND hWnd);

        /**
         * <p>
         * Removes the given window from the system-maintained clipboard format listener list.
         * </p>
         * 
         * @param hWnd A handle to the window to remove from the clipboard format listener list.
         * @return Returns TRUE if successful, FALSE otherwise. Call GetLastError for additional
         *         details.
         */
        boolean RemoveClipboardFormatListener(HWND hWnd);

        /**
         * <p>
         * Determines whether the clipboard contains data in the specified format.
         * </p>
         * 
         * @param format A standard or registered clipboard format. For a description of the
         *            standard clipboard formats, see Standard Clipboard Formats .
         * @return If the clipboard format is available, the return value is nonzero. If the
         *         clipboard format is not available, the return value is zero. To get extended
         *         error information, call GetLastError.
         */
        boolean IsClipboardFormatAvailable(int format);
    }

    /**
     * @version 2016/10/03 9:28:46
     */
    private static interface GDI extends StdCallLibrary, GDI32 {

        /**
         * The GetPixel function retrieves the red, green, blue (RGB) color value of the pixel at
         * the specified coordinates.
         *
         * @return The return value is either a scan code, a virtual-key code, or a character value,
         *         depending on the value of uCode and uMapType. If there is no translation, the
         *         return value is zero.
         */
        int GetPixel(HDC hdc, int x, int y);
    }

    /**
     * @version 2016/10/12 9:47:45
     */
    private static class Clipboard implements Runnable {

        /** The actual executor. */
        private static final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(Clipboard.class.getSimpleName());
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(true);
            return thread;
        });

        /** The clipboard event listener manager. */
        private static final List<Observer> observers = new CopyOnWriteArrayList();

        /** The thread manager. */
        private static Future switcher;

        /**
         * <p>
         * Listen the change of clipboard.
         * </p>
         * 
         * @return
         */
        private Signal<String> read() {
            return new Signal<String>((observer, disposer) -> {
                if (observers.add(observer) && observers.size() == 1) {
                    switcher = executor.submit(this);
                }

                return () -> {
                    if (observers.remove(observer) && observers.isEmpty()) {
                        switcher.cancel(false);
                    }
                };
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            HWND handle = User.CreateWindowEx(0, new WString("STATIC"), "", 0, 0, 0, 0, 0, null, null, null, null);

            if (User.AddClipboardFormatListener(handle)) {
                MSG message = new MSG();

                // WM_CLIPBOARDUPDATE == 0x31D
                while (User.GetMessage(message, null, 0x31D, 0x31D) != 0) {
                    try {
                        // accept unicode text only (CF_UNICODETEXT == 13)
                        if (User.OpenClipboard(null) && User.IsClipboardFormatAvailable(13)) {
                            Pointer data = User.GetClipboardData(13);
                            Pointer locked = Kernel.GlobalLock(data);
                            String text = locked.getWideString(0);
                            Kernel.GlobalUnlock(data);

                            for (Observer observer : observers) {
                                observer.accept(text);
                            }
                        }
                    } finally {
                        User.CloseClipboard();
                    }
                }
            }
        }
    }

    /**
     * Ported from ShellAPI.h. Microsoft Windows SDK 6.0A.
     * 
     * @author dblock[at]dblock.org
     */
    public interface ShellAPI extends StdCallLibrary {

        TypeMapper TYPE_MAPPER = Boolean.getBoolean("w32.ascii") ? W32APITypeMapper.ASCII : W32APITypeMapper.UNICODE;

        /**
         * <p>
         * Contains information used by <a href=
         * "https://msdn.microsoft.com/en-us/library/windows/desktop/bb762154(v=vs.85).aspx">
         * <strong xmlns="http://www.w3.org/1999/xhtml">ShellExecuteEx</strong></a>.
         * </p>
         * <pre>
         * <span style="color:Blue;">typedef</span> <span style=
        "color:Blue;">struct</span> _SHELLEXECUTEINFO {
         *   DWORD &nbsp;&nbsp;&nbsp;&nbsp;cbSize;
         *   ULONG &nbsp;&nbsp;&nbsp;&nbsp;fMask;
         *   HWND &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;hwnd;
         *   LPCTSTR &nbsp;&nbsp;lpVerb;
         *   LPCTSTR &nbsp;&nbsp;lpFile;
         *   LPCTSTR &nbsp;&nbsp;lpParameters;
         *   LPCTSTR &nbsp;&nbsp;lpDirectory;
         *   <span style="color:Blue;">int</span> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;nShow;
         *   HINSTANCE hInstApp;
         *   LPVOID &nbsp;&nbsp;&nbsp;lpIDList;
         *   LPCTSTR &nbsp;&nbsp;lpClass;
         *   HKEY &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;hkeyClass;
         *   DWORD &nbsp;&nbsp;&nbsp;&nbsp;dwHotKey;
         *   <span style="color:Blue;">union</span> {
         *     HANDLE hIcon;
         *     HANDLE hMonitor;
         *   }&nbsp;DUMMYUNIONNAME;
         *   HANDLE &nbsp;&nbsp;&nbsp;hProcess;
         * } SHELLEXECUTEINFO, *LPSHELLEXECUTEINFO;
         * </pre>
         * <h2>Remarks</h2>
         * <p>
         * The <strong>SEE_MASK_NOASYNC</strong> flag must be specified if the thread calling
         * <a href=
         * "https://msdn.microsoft.com/en-us/library/windows/desktop/bb762154(v=vs.85).aspx">
         * <strong xmlns="http://www.w3.org/1999/xhtml">ShellExecuteEx</strong></a> does not have a
         * message loop or if the thread or process will terminate soon after
         * <strong>ShellExecuteEx</strong> returns. Under such conditions, the calling thread will
         * not be available to complete the DDE conversation, so it is important that
         * <strong>ShellExecuteEx</strong> complete the conversation before returning control to the
         * calling application. Failure to complete the conversation can result in an unsuccessful
         * launch of the document.
         * </p>
         * <p>
         * If the calling thread has a message loop and will exist for some time after the call to
         * <a href=
         * "https://msdn.microsoft.com/en-us/library/windows/desktop/bb762154(v=vs.85).aspx">
         * <strong xmlns="http://www.w3.org/1999/xhtml">ShellExecuteEx</strong></a> returns, the
         * <strong>SEE_MASK_NOASYNC</strong> flag is optional. If the flag is omitted, the calling
         * thread's message pump will be used to complete the DDE conversation. The calling
         * application regains control sooner, since the DDE conversation can be completed in the
         * background.
         * </p>
         * <p>
         * When populating the most frequently used program list using the
         * <strong>SEE_MASK_FLAG_LOG_USAGE</strong> flag in <strong>fMask</strong>, counts are made
         * differently for the classic and Windows&nbsp;XP-style Start menus. The classic style menu
         * only counts hits to the shortcuts in the Program menu. The Windows&nbsp;XP-style menu
         * counts both hits to the shortcuts in the Program menu and hits to those shortcuts'
         * targets outside of the Program menu. Therefore, setting <strong>lpFile</strong> to
         * myfile.exe would affect the count for the Windows&nbsp;XP-style menu regardless of
         * whether that file was launched directly or through a shortcut. The classic style-which
         * would require <strong>lpFile</strong> to contain a .lnk file name-would not be affected.
         * </p>
         * <p>
         * To include double quotation marks in <strong>lpParameters</strong>, enclose each mark in
         * a pair of quotation marks, as in the following example.
         * </p>
         * <div id="code-snippet-2" class="codeSnippetContainer" xmlns="">
         * <div class= "codeSnippetContainerTabs"> </div>
         * <div class="codeSnippetContainerCodeContainer"> <div class= "codeSnippetToolBar">
         * <div class="codeSnippetToolBarText">
         * <a name= "CodeSnippetCopyLink" style= "display: none;" title= "Copy to clipboard." href=
         * "javascript:if
         * (window.epx.codeSnippet)window.epx.codeSnippet.copyCode('CodeSnippetContainerCode_3de148bb-edf3-4344-8ecf-c211304bfa9e');"
         * >Copy</a> </div> </div>
         * <div id="CodeSnippetContainerCode_3de148bb-edf3-4344-8ecf-c211304bfa9e" class=
         * "codeSnippetContainerCode" dir="ltr"> <div style="color:Black;"> <pre>
         * sei.lpParameters = &quot;An example: \&quot;\&quot;\&quot;quoted text\&quot;\&quot;\&quot;&quot;;
         * </pre> </div> </div> </div> </div>
         * <p>
         * In this case, the application receives three parameters: <em>An</em>, <em>example:</em>,
         * and <em>"quoted text"</em>.
         * </p>
         */
        public static class SHELLEXECUTEINFO extends Structure {

            public static final List<String> FIELDS = Arrays
                    .asList("cbSize", "fMask", "hwnd", "lpVerb", "lpFile", "lpParameters", "lpDirectory", "nShow", "hInstApp", "lpIDList", "lpClass", "hKeyClass", "dwHotKey", "hMonitor", "hProcess");

            /**
             * <p>
             * Type: <strong>DWORD</strong>
             * </p>
             * <p>
             * Required. The size of this structure, in bytes.
             * </p>
             */
            public int cbSize = size();

            /**
             * <p>
             * Type: <strong>ULONG</strong>
             * </p>
             * <p>
             * Flags that indicate the content and validity of the other structure members; a
             * combination of the following values:
             * </p>
             * <dl class="indent">
             * <dt>
             * <p>
             * <strong>SEE_MASK_DEFAULT</strong> (0x00000000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use default values.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_CLASSNAME</strong> (0x00000001)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use the class name given by the <strong>lpClass</strong> member. If both
             * SEE_MASK_CLASSKEY and SEE_MASK_CLASSNAME are set, the class key is used.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_CLASSKEY</strong> (0x00000003)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use the class key given by the <strong>hkeyClass</strong> member. If both
             * SEE_MASK_CLASSKEY and SEE_MASK_CLASSNAME are set, the class key is used.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_IDLIST</strong> (0x00000004)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use the item identifier list given by the <strong>lpIDList</strong> member. The
             * <strong>lpIDList</strong> member must point to an structure.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_INVOKEIDLIST</strong> (0x0000000C)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use the interface of the selected item's . Use either <strong>lpFile</strong> to
             * identify the item by its file system path or <strong>lpIDList</strong> to identify
             * the item by its PIDL. This flag allows applications to use to invoke verbs from
             * shortcut menu extensions instead of the static verbs listed in the registry.
             * </p>
             * <div class="note"><strong>Note</strong> &nbsp;&nbsp;SEE_MASK_INVOKEIDLIST overrides
             * and implies SEE_MASK_IDLIST.</div></dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_ICON</strong> (0x00000010)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use the icon given by the <strong>hIcon</strong> member. This flag cannot be combined
             * with SEE_MASK_HMONITOR.
             * </p>
             * <div class="note"><strong>Note</strong>&nbsp;&nbsp;This flag is used only in
             * Windows&nbsp;XP and earlier. It is ignored as of Windows&nbsp;Vista.</div></dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_HOTKEY</strong> (0x00000020)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use the keyboard shortcut given by the <strong>dwHotKey</strong> member.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_NOCLOSEPROCESS</strong> (0x00000040)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use to indicate that the <strong>hProcess</strong> member receives the process
             * handle. This handle is typically used to allow an application to find out when a
             * process created with terminates. In some cases, such as when execution is satisfied
             * through a DDE conversation, no handle will be returned. The calling application is
             * responsible for closing the handle when it is no longer needed.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_CONNECTNETDRV</strong> (0x00000080)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Validate the share and connect to a drive letter. This enables reconnection of
             * disconnected network drives. The <strong>lpFile</strong> member is a UNC path of a
             * file on a network.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_NOASYNC</strong> (0x00000100)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Wait for the execute operation to complete before returning. This flag should be used
             * by callers that are using ShellExecute forms that might result in an async
             * activation, for example DDE, and create a process that might be run on a background
             * thread. (Note: runs on a background thread by default if the caller's threading model
             * is not Apartment.) Calls to <strong>ShellExecuteEx</strong> from processes already
             * running on background threads should always pass this flag. Also, applications that
             * exit immediately after calling <strong>ShellExecuteEx</strong> should specify this
             * flag.
             * </p>
             * <p>
             * If the execute operation is performed on a background thread and the caller did not
             * specify the SEE_MASK_ASYNCOK flag, then the calling thread waits until the new
             * process has started before returning. This typically means that either has been
             * called, the DDE communication has completed, or that the custom execution delegate
             * has notified that it is done. If the SEE_MASK_WAITFORINPUTIDLE flag is specified,
             * then <strong>ShellExecuteEx</strong> calls and waits for the new process to idle
             * before returning, with a maximum timeout of 1 minute.
             * </p>
             * <p>
             * For further discussion on when this flag is necessary, see the Remarks section.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_FLAG_DDEWAIT</strong> (0x00000100)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Do not use; use SEE_MASK_NOASYNC instead.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_DOENVSUBST</strong> (0x00000200)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Expand any environment variables specified in the string given by the
             * <strong>lpDirectory</strong> or <strong>lpFile</strong> member.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_FLAG_NO_UI</strong> (0x00000400)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Do not display an error message box if an error occurs.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_UNICODE</strong> (0x00004000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use this flag to indicate a Unicode application.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_NO_CONSOLE</strong> (0x00008000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use to inherit the parent's console for the new process instead of having it create a
             * new console. It is the opposite of using a CREATE_NEW_CONSOLE flag with .
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_ASYNCOK</strong> (0x00100000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * The execution can be performed on a background thread and the call should return
             * immediately without waiting for the background thread to finish. Note that in certain
             * cases ignores this flag and waits for the process to finish before returning.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_NOQUERYCLASSSTORE</strong> (0x01000000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Not used.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_HMONITOR</strong> (0x00200000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Use this flag when specifying a monitor on multi-monitor systems. The monitor is
             * specified in the <strong>hMonitor</strong> member. This flag cannot be combined with
             * SEE_MASK_ICON.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_NOZONECHECKS</strong> (0x00800000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * <strong>Introduced in Windows&nbsp;XP</strong>. Do not perform a zone check. This
             * flag allows to bypass zone checking put into place by .
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_WAITFORINPUTIDLE</strong> (0x02000000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * After the new process is created, wait for the process to become idle before
             * returning, with a one minute timeout. See for more details.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_FLAG_LOG_USAGE</strong> (0x04000000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * <strong>Introduced in Windows&nbsp;XP</strong>. Keep track of the number of times
             * this application has been launched. Applications with sufficiently high counts appear
             * in the Start Menu's list of most frequently used programs.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SEE_MASK_FLAG_HINST_IS_SITE</strong> (0x08000000)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * <strong>Introduced in Windows&nbsp;8</strong>. The <strong>hInstApp</strong> member
             * is used to specify the of an object that implements . This object will be used as a
             * site pointer. The site pointer is used to provide services to the function, the
             * handler binding process, and invoked verb handlers.
             * </p>
             * </dd>
             * </dl>
             */
            public int fMask;

            /**
             * <p>
             * Type: <strong>HWND</strong>
             * </p>
             * <p>
             * Optional. A handle to the parent window, used to display any message boxes that the
             * system might produce while executing this function. This value can be
             * <strong>NULL</strong>.
             * </p>
             */
            public HWND hwnd;

            /**
             * <p>
             * Type: <strong>LPCTSTR</strong>
             * </p>
             * </dd>
             * <dd>
             * <p>
             * A string, referred to as a <em>verb</em>, that specifies the action to be performed.
             * The set of available verbs depends on the particular file or folder. Generally, the
             * actions available from an object's shortcut menu are available verbs. This parameter
             * can be <strong>NULL</strong>, in which case the default verb is used if available. If
             * not, the "open" verb is used. If neither verb is available, the system uses the first
             * verb listed in the registry. The following verbs are commonly used:
             * </p>
             * <dl class="indent">
             * <dt>
             * <p>
             * <strong>edit</strong>
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Launches an editor and opens the document for editing. If <strong>lpFile</strong> is
             * not a document file, the function will fail.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>explore</strong>
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Explores the folder specified by <strong>lpFile</strong>.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>find</strong>
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Initiates a search starting from the specified directory.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>open</strong>
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Opens the file specified by the <strong>lpFile</strong> parameter. The file can be an
             * executable file, a document file, or a folder.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>print</strong>
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Prints the document file specified by <strong>lpFile</strong>. If
             * <strong>lpFile</strong> is not a document file, the function will fail.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>properties</strong>
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Displays the file or folder's properties.
             * </p>
             * </dd>
             * </dl>
             */
            public String lpVerb;

            /**
             * <p>
             * Type: <strong>LPCTSTR</strong>
             * </p>
             * <p>
             * The address of a null-terminated string that specifies the name of the file or object
             * on which will perform the action specified by the <strong>lpVerb</strong> parameter.
             * The system registry verbs that are supported by the <strong>ShellExecuteEx</strong>
             * function include "open" for executable files and document files and "print" for
             * document files for which a print handler has been registered. Other applications
             * might have added Shell verbs through the system registry, such as "play" for .avi and
             * .wav files. To specify a Shell namespace object, pass the fully qualified parse name
             * and set the <strong>SEE_MASK_INVOKEIDLIST</strong> flag in the <strong>fMask</strong>
             * parameter.
             * </p>
             * <div class="note"><strong>Note</strong>&nbsp;&nbsp;If the
             * <strong>SEE_MASK_INVOKEIDLIST</strong> flag is set, you can use either
             * <strong>lpFile</strong> or <strong>lpIDList</strong> to identify the item by its file
             * system path or its PIDL respectively. One of the two values-<strong>lpFile</strong>
             * or <strong>lpIDList</strong>-must be set.</div>
             * <div class="note"><strong>Note</strong>&nbsp;&nbsp;If the path is not included with
             * the name, the current directory is assumed.</div>
             */
            public String lpFile;

            /**
             * <p>
             * Type: <strong>LPCTSTR</strong>
             * </p>
             * <p>
             * Optional. The address of a null-terminated string that contains the application
             * parameters. The parameters must be separated by spaces. If the
             * <strong>lpFile</strong> member specifies a document file,
             * <strong>lpParameters</strong> should be <strong>NULL</strong>.
             * </p>
             */
            public String lpParameters;

            /**
             * <p>
             * Type: <strong>LPCTSTR</strong>
             * </p>
             * <p>
             * Optional. The address of a null-terminated string that specifies the name of the
             * working directory. If this member is <strong>NULL</strong>, the current directory is
             * used as the working directory.
             * </p>
             */
            public String lpDirectory;

            /**
             * <p>
             * Type: <strong>int</strong>
             * </p>
             * <p>
             * Required. Flags that specify how an application is to be shown when it is opened; one
             * of the SW_ values listed for the <a href=
             * "https://msdn.microsoft.com/en-us/library/windows/desktop/bb762153(v=vs.85).aspx">
             * <strong xmlns="http://www.w3.org/1999/xhtml">ShellExecute</strong> </a> function. If
             * <strong>lpFile</strong> specifies a document file, the flag is simply passed to the
             * associated application. It is up to the application to decide how to handle it.
             * </p>
             */
            public int nShow;

            /**
             * <p>
             * Type: <strong>HINSTANCE</strong>
             * </p>
             * <p>
             * [out] If SEE_MASK_NOCLOSEPROCESS is set and the call succeeds, it sets this member to
             * a value greater than 32. If the function fails, it is set to an SE_ERR_XXX error
             * value that indicates the cause of the failure. Although <strong>hInstApp</strong> is
             * declared as an HINSTANCE for compatibility with 16-bit Windows applications, it is
             * not a true HINSTANCE. It can be cast only to an <strong>int</strong> and compared to
             * either 32 or the following SE_ERR_XXX error codes.
             * </p>
             * <dl class="indent">
             * <dt>
             * <p>
             * <strong>SE_ERR_FNF</strong> (2)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * File not found.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_PNF</strong> (3)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Path not found.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_ACCESSDENIED</strong> (5)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Access denied.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_OOM</strong> (8)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Out of memory.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_DLLNOTFOUND</strong> (32)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Dynamic-link library not found.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_SHARE</strong> (26)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * Cannot share an open file.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_ASSOCINCOMPLETE</strong> (27)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * File association information not complete.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_DDETIMEOUT</strong> (28)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * DDE operation timed out.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_DDEFAIL</strong> (29)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * DDE operation failed.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_DDEBUSY</strong> (30)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * DDE operation is busy.
             * </p>
             * </dd>
             * <dt>
             * <p>
             * <strong>SE_ERR_NOASSOC</strong> (31)
             * </p>
             * </dt>
             * <dd>
             * <p>
             * File association not available.
             * </p>
             * </dd>
             * </dl>
             */
            public HINSTANCE hInstApp;

            /**
             * <p>
             * Type: <strong>LPVOID</strong>
             * </p>
             * <p>
             * The address of an absolute <a href=
             * "https://msdn.microsoft.com/en-us/library/windows/desktop/bb773321(v=vs.85).aspx">
             * <strong xmlns="http://www.w3.org/1999/xhtml">ITEMIDLIST</strong></a> structure
             * (PCIDLIST_ABSOLUTE) to contain an item identifier list that uniquely identifies the
             * file to execute. This member is ignored if the <strong>fMask</strong> member does not
             * include <strong>SEE_MASK_IDLIST</strong> or <strong>SEE_MASK_INVOKEIDLIST</strong>.
             * </p>
             */
            public Pointer lpIDList;

            /**
             * <p>
             * Type: <strong>LPCTSTR</strong>
             * </p>
             * <p>
             * The address of a null-terminated string that specifies one of the following:
             * </p>
             * <ul>
             * <li>A ProgId. For example, "Paint.Picture".</li>
             * <li>A URI protocol scheme. For example, "http".</li>
             * <li>A file extension. For example, ".txt".</li>
             * <li>A registry path under HKEY_CLASSES_ROOT that names a subkey that contains one or
             * more Shell verbs. This key will have a subkey that conforms to the Shell verb
             * registry schema, such as
             * <p>
             * <strong>shell</strong>\<em>verb name</em>
             * </p>
             * .</li>
             * </ul>
             * <p>
             * This member is ignored if <strong>fMask</strong> does not include
             * <strong>SEE_MASK_CLASSNAME</strong>.
             * </p>
             */
            public String lpClass;

            /**
             * <p>
             * Type: <strong>HKEY</strong>
             * </p>
             * <p>
             * A handle to the registry key for the file type. The access rights for this registry
             * key should be set to KEY_READ. This member is ignored if <strong>fMask</strong> does
             * not include <strong>SEE_MASK_CLASSKEY</strong>.
             * </p>
             */
            public HKEY hKeyClass;

            /**
             * <p>
             * Type: <strong>DWORD</strong>
             * </p>
             * <p>
             * A keyboard shortcut to associate with the application. The low-order word is the
             * virtual key code, and the high-order word is a modifier flag (HOTKEYF_). For a list
             * of modifier flags, see the description of the <a href=
             * "https://msdn.microsoft.com/en-us/library/windows/desktop/ms646284(v=vs.85).aspx">
             * <strong xmlns="http://www.w3.org/1999/xhtml">WM_SETHOTKEY</strong> </a> message. This
             * member is ignored if <strong>fMask</strong> does not include
             * <strong>SEE_MASK_HOTKEY</strong>.
             * </p>
             */
            public int dwHotKey;

            /**
             * This is actually a union: <pre>
             * <code>union { HANDLE hIcon; HANDLE hMonitor; } DUMMYUNIONNAME;</code> </pre>
             * <strong>DUMMYUNIONNAME</strong>
             * <dl>
             * <dt><strong>hIcon</strong></dt>
             * <dd>
             * <p>
             * <strong>Type: <strong>HANDLE</strong></strong>
             * </p>
             * </dd>
             * <dd>
             * <p>
             * A handle to the icon for the file type. This member is ignored if
             * <strong>fMask</strong> does not include <strong>SEE_MASK_ICON</strong>. This value is
             * used only in Windows&nbsp;XP and earlier. It is ignored as of Windows&nbsp;Vista.
             * </p>
             * </dd>
             * <dt><strong>hMonitor</strong></dt>
             * <dd>
             * <p>
             * <strong>Type: <strong>HANDLE</strong></strong>
             * </p>
             * </dd>
             * <dd>
             * <p>
             * A handle to the monitor upon which the document is to be displayed. This member is
             * ignored if <strong>fMask</strong> does not include <strong>SEE_MASK_HMONITOR</strong>
             * .
             * </p>
             * </dd>
             * </dl>
             */
            public HANDLE hMonitor;

            /**
             * <p>
             * Type: <strong>HANDLE</strong>
             * </p>
             * <p>
             * A handle to the newly started application. This member is set on return and is always
             * <strong>NULL</strong> unless <strong>fMask</strong> is set to
             * <strong>SEE_MASK_NOCLOSEPROCESS</strong>. Even if <strong>fMask</strong> is set to
             * <strong>SEE_MASK_NOCLOSEPROCESS</strong>, <strong>hProcess</strong> will be
             * <strong>NULL</strong> if no process was launched. For example, if a document to be
             * launched is a URL and an instance of Internet Explorer is already running, it will
             * display the document. No new process is launched, and <strong>hProcess</strong> will
             * be <strong>NULL</strong>.
             * </p>
             * <div class="note"><strong>Note</strong>&nbsp;&nbsp;<a href=
             * "https://msdn.microsoft.com/en-us/library/windows/desktop/bb762154(v=vs.85).aspx">
             * <strong xmlns="http://www.w3.org/1999/xhtml">ShellExecuteEx</strong> </a> does not
             * always return an <strong>hProcess</strong>, even if a process is launched as the
             * result of the call. For example, an <strong>hProcess</strong> does not return when
             * you use <strong>SEE_MASK_INVOKEIDLIST</strong> to invoke <a href=
             * "https://msdn.microsoft.com/en-us/library/windows/desktop/bb776095(v=vs.85).aspx">
             * <strong xmlns="http://www.w3.org/1999/xhtml">IContextMenu</strong> </a>.</div>
             */
            public HANDLE hProcess;

            /**
             * {@inheritDoc}
             */
            @Override
            protected List<String> getFieldOrder() {
                return FIELDS;
            }
        }
    }
}