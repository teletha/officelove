/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.macro;

import java.awt.Font;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.KEYBDINPUT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;

import kiss.I;
import kiss.Observer;
import kiss.Signal;

/**
 * @version 2016/10/04 3:22:04
 */
public abstract class Macro {

    /** The application font. */
    private static final Font font = new Font("MeiryoKe_UIGothic", Font.PLAIN, 12);

    /** The display scale. */
    private static final long ScaleX = 65536 / User32.INSTANCE.GetSystemMetrics(User32.SM_CXSCREEN);

    /** The display scale. */
    private static final long ScaleY = 65536 / User32.INSTANCE.GetSystemMetrics(User32.SM_CYSCREEN);

    /** Acceptable condition. */
    private static final Predicate ANY = new Predicate() {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean test(Object o) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Predicate and(Predicate other) {
            return other;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Predicate or(Predicate other) {
            return other;
        }
    };

    /** The macro name. */
    protected final String name;

    /** The window condition. */
    private Predicate<Window> windowCondition = ANY;

    /** The keyboard hook. */
    private NativeKeyboardHook keyboardHook = new NativeKeyboardHook();

    /** The keyboard hook. */
    private NativeMouseHook mouseHook = new NativeMouseHook();

    /** The tray icon. */
    private final TrayIcon tray;

    /**
     * 
     */
    protected Macro() {
        keyboardHook.install();
        mouseHook.install();

        try {
            name = getClass().getSimpleName();
            tray = new TrayIcon(ImageIO.read(Macro.class.getResource("icon.png")));
            tray.setImageAutoSize(true);
            tray.setToolTip(name);
            tray.setPopupMenu(menu());
            SystemTray.getSystemTray().add(tray);
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Create popup menu.
     * </p>
     */
    private PopupMenu menu() {
        PopupMenu popup = new PopupMenu();
        popup.add(item("Reload", this::restart));
        popup.add(item("Quit", this::suspend));
        popup.setFont(font);

        return popup;
    }

    /**
     * <p>
     * Create menu item.
     * </p>
     * 
     * @param name
     * @param action
     * @return
     */
    private MenuItem item(String name, Runnable action) {
        MenuItem item = new MenuItem(name);
        item.addActionListener(e -> action.run());
        item.setFont(font);

        return item;
    }

    /**
     * <p>
     * Declare key related event.
     * </p>
     * 
     * @param key
     * @return
     */
    protected final MacroDSL when(Key key) {
        return new KeyMacro().key(key);
    }

    /**
     * <p>
     * Restart this macro with JVM.
     * </p>
     */
    protected final void restart() {
        ArrayList<String> commands = new ArrayList();

        // Java
        commands.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        commands.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());

        // classpath
        commands.add("-cp");
        commands.add(ManagementFactory.getRuntimeMXBean().getClassPath());

        // Class to be executed
        commands.add(getClass().getName());

        try {
            new ProcessBuilder(commands).start();
            suspend();
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * Suspend this macro.
     * </p>
     */
    protected final void suspend() {
        keyboardHook.uninstall();
        mouseHook.uninstall();

        delay(300);
        System.exit(0);
    }

    /**
     * <p>
     * Declare mouse related event.
     * </p>
     * 
     * @param mouse
     * @return
     */
    protected final Signal<KeyEvent> when(Mouse mouse) {
        return new KeyMacro().register(this.mouseHook.moves);
    }

    /**
     * <p>
     * Emulate press event.
     * </p>
     * 
     * @param key
     * @return
     */
    protected final Macro press(Key key) {
        return emulate(key, true, false);
    }

    /**
     * <p>
     * Emulate release event.
     * </p>
     * 
     * @param key
     * @return
     */
    protected final Macro release(Key key) {
        return emulate(key, false, true);
    }

    /**
     * <p>
     * Emulate press and release event in series.
     * </p>
     * 
     * @param keys
     * @return
     */
    protected final Macro input(Key... keys) {
        for (Key key : keys) {
            emulate(key, true, true);
        }
        return this;
    }

    /**
     * <p>
     * Emulate press and release event in parallel.
     * </p>
     * 
     * @param keys
     * @return
     */
    protected final Macro inputParallel(Key... keys) {
        for (int i = 0; i < keys.length; i++) {
            emulate(keys[i], true, false);
        }

        for (int i = keys.length - 1; 0 <= i; i--) {
            emulate(keys[i], false, true);
        }
        return this;
    }

    /**
     * <p>
     * Retrieve the active window.
     * </p>
     * 
     * @return
     */
    protected final Window window() {
        return Window.now();
    }

    /**
     * <p>
     * Emulate input event.
     * </p>
     * 
     * @param key
     * @param press
     * @param release
     * @return
     */
    private final Macro emulate(Key key, boolean press, boolean release) {
        if (key.mouse) {
            INPUT ip = new INPUT();
            ip.type = new DWORD(INPUT.INPUT_MOUSE);
            ip.input.setType("mi");

            if (press) {
                ip.input.mi.dwFlags = new DWORD(key.on | 0x8000); // MOUSEEVENTF_ABSOLUTE
                User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());
            }

            if (release) {
                ip.input.mi.dwFlags = new DWORD(key.off | 0x8000); // MOUSEEVENTF_ABSOLUTE
                User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());
            }
        } else {
            INPUT ip = new INPUT();
            ip.type = new DWORD(INPUT.INPUT_KEYBOARD);
            ip.input.setType("ki");
            ip.input.ki.wVk = new WORD(key.virtualCode);
            ip.input.ki.wScan = new WORD(key.scanCode);

            if (press) {
                ip.input.ki.dwFlags = new DWORD(KEYBDINPUT.KEYEVENTF_SCANCODE);
                User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());
            }

            if (release) {
                ip.input.ki.dwFlags = new DWORD(KEYBDINPUT.KEYEVENTF_KEYUP | KEYBDINPUT.KEYEVENTF_SCANCODE);
                User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());
            }
        }
        return this;
    }

    protected final Macro mouseMoveTo(int x, int y) {
        INPUT ip = new INPUT();
        ip.type = new DWORD(INPUT.INPUT_MOUSE);
        ip.input.setType("mi");
        ip.input.mi.dx = new LONG(x * ScaleX);
        ip.input.mi.dy = new LONG(y * ScaleY);
        ip.input.mi.dwFlags = new DWORD(0x0001 | 0x8000); // MOUSEEVENTF_MOVE | MOUSEEVENTF_ABSOLUTE
        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());
        return this;
    }

    /**
     * <p>
     * Stop macro temporary.
     * </p>
     * 
     * @param ms A time to stop.
     * @return
     */
    protected final Macro delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw I.quiet(e);
        }
        return this;
    }

    /**
     * <p>
     * Declare the condition of macro activation.
     * </p>
     * 
     * @param condition
     */
    protected final void require(Predicate<Window> condition, Runnable definitions) {
        Predicate<Window> stored = windowCondition;

        windowCondition = windowCondition.and(condition);
        definitions.run();

        windowCondition = stored;
    }

    /**
     * <p>
     * Declare the condition of macro activation.
     * </p>
     * 
     * @param condition
     */
    protected final void requireTitle(String title, Runnable definitions) {
        require(window -> window.title().contains(title), definitions);
    }

    /**
     * <p>
     * Use the specified macro.
     * </p>
     */
    public static <M extends Macro> void use(Class<M> clazz) {
        I.make(clazz);
    }

    /**
     * @version 2016/10/02 18:01:25
     */
    public interface MacroDSL {

        /**
         * <p>
         * Consume the native event.
         * </p>
         * 
         * @return
         */
        MacroDSL consume();

        /**
         * <p>
         * Declare press event.
         * </p>
         * 
         * @return
         */
        Signal<KeyEvent> press();

        /**
         * <p>
         * Declare release event.
         * </p>
         * 
         * @return
         */
        Signal<KeyEvent> release();

        /**
         * <p>
         * Declare key modifier.
         * </p>
         * 
         * @return Chainable DSL.
         */
        MacroDSL withAlt();

        /**
         * <p>
         * Declare key modifier.
         * </p>
         * 
         * @return Chainable DSL.
         */
        MacroDSL withCtrl();

        /**
         * <p>
         * Declare key modifier.
         * </p>
         * 
         * @return Chainable DSL.
         */
        MacroDSL withShift();
    }

    /**
     * @version 2016/10/04 15:57:53
     */
    private class KeyMacro implements MacroDSL {

        /** The window condition. */
        private Predicate<Window> window = windowCondition;

        /** The acceptable event type. */
        private Predicate condition = ANY;

        /** The event should be consumed or not. */
        private boolean consumable;

        /** The associated key. */
        private Key key;

        /** The modifier state. */
        private boolean alt;

        /** The modifier state. */
        private boolean ctrl;

        /** The modifier state. */
        private boolean shift;

        /** The observers. */
        private final List<Observer<? super KeyEvent>> observers = new CopyOnWriteArrayList();

        /**
         * <p>
         * Set key type.
         * </p>
         * 
         * @param key
         * @return
         */
        private KeyMacro key(Key key) {
            this.key = key;
            condition = condition.and(e -> e == this.key);

            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MacroDSL consume() {
            consumable = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MacroDSL withAlt() {
            alt = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MacroDSL withCtrl() {
            ctrl = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MacroDSL withShift() {
            shift = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<KeyEvent> press() {
            return register((key.mouse ? mouseHook : keyboardHook).presses);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Signal<KeyEvent> release() {
            return register((key.mouse ? mouseHook : keyboardHook).releases);
        }

        /**
         * <p>
         * Register this macro.
         * </p>
         * 
         * @param macros
         * @return
         */
        private Signal<KeyEvent> register(List<KeyMacro> macros) {
            macros.add(this);

            return new Signal<KeyEvent>((observer, disposer) -> {
                observers.add(observer);
                return () -> observers.remove(observer);
            });
        }

        /**
         * <p>
         * Test modifier state.
         * </p>
         * 
         * @param alt The modifier state.
         * @param ctrl The modifier state.
         * @param shift The modifier state.
         * @return
         */
        private boolean modifier(boolean alt, boolean ctrl, boolean shift) {
            return this.alt == alt && this.ctrl == ctrl && this.shift == shift;
        }
    }

    /**
     * @version 2016/10/04 4:20:39
     */
    protected static abstract class NativeHook<T> implements Runnable, HOOKPROC {

        /** The actual executor. */
        private static final ExecutorService executor = new ThreadPoolExecutor(4, 256, 30, TimeUnit.SECONDS, new SynchronousQueue(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(NativeKeyboardHook.class.getSimpleName());
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(false);
            return thread;
        });

        /** The event listeners. */
        protected final List<KeyMacro> presses = new ArrayList();

        /** The event listeners. */
        protected final List<KeyMacro> releases = new ArrayList();

        /** The native hook. */
        protected HHOOK hook;

        /**
         * <p>
         * Install service.
         * </p>
         */
        void install() {
            executor.execute(this);
            Runtime.getRuntime().addShutdownHook(new Thread(this::uninstall));
        }

        /**
         * <p>
         * Uninstall service.
         * </p>
         */
        void uninstall() {
            executor.shutdown();
            User32.INSTANCE.UnhookWindowsHookEx(hook);
        }

        /**
         * <p>
         * Configure hook type.
         * </p>
         * 
         * @return
         */
        protected abstract int hookType();

        /**
         * {@inheritDoc}
         */
        @Override
        public final void run() {
            hook = User32.INSTANCE.SetWindowsHookEx(hookType(), this, Kernel32.INSTANCE.GetModuleHandle(null), 0);

            int result;
            MSG message = new MSG();

            while ((result = User32.INSTANCE.GetMessage(message, null, 0, 0)) != 0) {
                if (result == -1) {
                    break;
                } else {
                    User32.INSTANCE.TranslateMessage(message);
                    User32.INSTANCE.DispatchMessage(message);
                }
            }
        }

        /**
         * <p>
         * Helper method to retrieve the key state.
         * </p>
         * 
         * @param key
         * @return
         */
        private boolean with(Key key) {
            return (User32.INSTANCE.GetAsyncKeyState(key.virtualCode) & 0x8000) != 0;
        }

        /**
         * <p>
         * Handle key event.
         * </p>
         * 
         * @param key
         */
        protected final boolean handle(T key, List<KeyMacro> macros, KeyEvent event) {
            boolean consumed = false;

            if (!macros.isEmpty()) {
                Window now = Window.now();
                boolean alt = with(Key.Alt);
                boolean ctrl = with(Key.Control);
                boolean shift = with(Key.Shift);

                for (KeyMacro macro : macros) {
                    if (macro.window.test(now) && macro.modifier(alt, ctrl, shift) && macro.condition.test(key)) {
                        executor.execute(() -> {
                            for (Observer<? super KeyEvent> observer : macro.observers) {
                                observer.accept(event);
                            }
                        });

                        if (macro.consumable) {
                            consumed = true;
                        }
                    }
                }
            }
            return consumed;
        }
    }

    /**
     * @version 2016/10/03 12:31:30
     */
    private static class NativeKeyboardHook extends NativeHook implements LowLevelKeyboardProc {

        /**
         * <p>
         * Specifies whether the event was injected. The value is 1 if that is the case; otherwise,
         * it is 0. Note that bit 1 is not necessarily set when bit 4 is set. <a href=
         * "https://msdn.microsoft.com/ja-jp/library/windows/desktop/ms644967(v=vs.85).aspx">REF
         * </a>
         * </p>
         */
        private static final int InjectedEvent = 1 << 4;

        /** The key mapper. */
        private static final Key[] keys = new Key[256];

        static {
            for (Key key : Key.values()) {
                keys[key.virtualCode] = key;
            }
        }

        /** The record for the last downed key to decimate a flood of key down events. */
        private Key downLatest;

        /**
         * {@inheritDoc}
         */
        @Override
        protected int hookType() {
            return WinUser.WH_KEYBOARD_LL;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT info) {
            boolean consumed = false;
            boolean userInput = (info.flags & InjectedEvent) == 0;

            if (0 <= nCode && userInput) {
                Key key = keys[info.vkCode];

                switch (wParam.intValue()) {
                case WinUser.WM_KEYDOWN:
                case WinUser.WM_SYSKEYDOWN:
                    if (downLatest != key) {
                        downLatest = key;
                        consumed = handle(key, presses, KeyEvent.of(key));
                    }
                    break;

                case WinUser.WM_KEYUP:
                case WinUser.WM_SYSKEYUP:
                    downLatest = null;
                    consumed = handle(key, releases, KeyEvent.of(key));
                    break;
                }
            }
            return consumed ? new LRESULT(-1)
                    : User32.INSTANCE.CallNextHookEx(hook, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
        }
    }

    /**
     * @version 2016/10/03 12:31:30
     */
    private static class NativeMouseHook extends NativeHook implements LowLevelMouseProc {

        /**
         * <p>
         * The event-injected flags. An application can use the following values to test the flags.
         * Testing LLMHF_INJECTED (bit 0) will tell you whether the event was injected. If it was,
         * then testing LLMHF_LOWER_IL_INJECTED (bit 1) will tell you whether or not the event was
         * injected from a process running at lower integrity level. <a href=
         * "https://msdn.microsoft.com/en-us/library/windows/desktop/ms644970(v=vs.85).aspx">REF
         * </a>
         * </p>
         */
        private static final int InjectedEvent = 1;

        /** The event listeners. */
        private final List<KeyMacro> moves = new ArrayList();

        /**
         * {@inheritDoc}
         */
        @Override
        protected int hookType() {
            return WinUser.WH_MOUSE_LL;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LRESULT callback(int nCode, WPARAM wParam, MSLLHOOKSTRUCT info) {
            boolean consumed = false;
            boolean userInput = (info.flags & InjectedEvent) == 0;

            if (0 <= nCode && userInput) {
                info.pt.time = info.time;

                switch (wParam.intValue()) {
                case 513: // WM_LBUTTONDOWN
                    consumed = handle(Key.MouseLeft, presses, info.pt);
                    break;

                case 514: // WM_LBUTTONUP
                    consumed = handle(Key.MouseLeft, releases, info.pt);
                    break;

                case 516: // WM_RBUTTONDOWN
                    consumed = handle(Key.MouseRight, presses, info.pt);
                    break;

                case 517: // WM_RBUTTONUP
                    consumed = handle(Key.MouseRight, releases, info.pt);
                    break;

                case 519: // WM_MBUTTONDOWN
                    consumed = handle(Key.MouseMiddle, presses, info.pt);
                    break;

                case 520: // WM_MBUTTONDOWN
                    consumed = handle(Key.MouseMiddle, releases, info.pt);
                    break;
                }
            }
            return consumed ? new LRESULT(-1)
                    : User32.INSTANCE.CallNextHookEx(hook, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
        }
    }

    /**
     * @version 2016/10/04 3:49:48
     */
    private static interface LowLevelMouseProc extends HOOKPROC {

        LRESULT callback(int nCode, WPARAM wParam, MSLLHOOKSTRUCT lParam);
    }

    /**
     * @version 2016/10/16 10:34:57
     */
    public static class Point extends Structure implements KeyEvent {

        public NativeLong x;

        public NativeLong y;

        private long time;

        /**
         * {@inheritDoc}
         */
        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] {"x", "y"});
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int x() {
            return x.intValue();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int y() {
            return y.intValue();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long time() {
            return time;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((x == null) ? 0 : x.hashCode());
            result = prime * result + ((y == null) ? 0 : y.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point) {
                Point other = (Point) obj;
                return x() == other.x() && y() == other.y();
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Point [x=" + x + ", y=" + y + ", time=" + time + "]";
        }
    }

    /**
     * @version 2016/10/04 14:06:51
     */
    public static class MSLLHOOKSTRUCT extends Structure {

        public Point pt;

        public int mouseData;

        public int flags;

        public int time;

        public int dwExtraInfo;

        /**
         * {@inheritDoc}
         */
        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] {"pt", "mouseData", "flags", "time", "dwExtraInfo"});
        }
    }
}