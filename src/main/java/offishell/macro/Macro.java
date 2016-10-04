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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.POINT;
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

import kiss.Events;
import kiss.I;
import kiss.Observer;

/**
 * @version 2016/10/04 3:22:04
 */
public abstract class Macro {

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

    /** The window condition. */
    private Predicate<Window> windowCondition = ANY;

    /** The keyboard hook. */
    private NativeKeyboardHook keyboard = new NativeKeyboardHook();

    /** The keyboard hook. */
    private NativeMouseHook mouse = new NativeMouseHook();

    /**
     * 
     */
    protected Macro() {
        keyboard.install();
        mouse.install();
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
     * Declare mouse related event.
     * </p>
     * 
     * @param mouse
     * @return
     */
    protected final Events<Mouse> when(Mouse mouse) {
        return new KeyMacro().mouse(mouse).register(this.mouse.moves);
    }

    /**
     * <p>
     * Declare press event hook.
     * </p>
     * 
     * @param key
     * @return
     */
    protected final MacroDSL whenPress(Key key) {
        return new KeyMacro().when(keyboard.presses).key(key);
    }

    /**
     * <p>
     * Declare press event hook.
     * </p>
     * 
     * @param mouse
     * @return
     */
    protected final MacroDSL whenPress(Mouse mouseButton) {
        return new KeyMacro().when(mouse.presses).mouse(mouseButton);
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
        INPUT ip = new INPUT();
        ip.type = new DWORD(INPUT.INPUT_KEYBOARD);
        ip.input.setType("ki");
        ip.input.ki.wVk = new WORD(key.virtualCode);
        ip.input.ki.wScan = new WORD(key.scanCode);

        // press
        ip.input.ki.dwFlags = new DWORD(KEYBDINPUT.KEYEVENTF_SCANCODE);
        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());

        // release
        ip.input.ki.dwFlags = new DWORD(KEYBDINPUT.KEYEVENTF_KEYUP | KEYBDINPUT.KEYEVENTF_SCANCODE);
        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());
        return this;
    }

    /**
     * <p>
     * Emulate press event.
     * </p>
     * 
     * @param mouse
     * @return
     */
    protected final Macro press(Mouse mouse) {
        INPUT ip = new INPUT();
        ip.type = new DWORD(INPUT.INPUT_MOUSE);
        ip.input.setType("mi");

        // press
        ip.input.mi.dwFlags = new DWORD(mouse.startAction);
        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());

        // release
        ip.input.mi.dwFlags = new DWORD(mouse.endAction);
        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());
        return this;
    }

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
    protected final void require(Predicate<Window> condition) {
        windowCondition = windowCondition.and(condition);
    }

    /**
     * <p>
     * Declare the condition of macro activation.
     * </p>
     * 
     * @param condition
     */
    protected final void requireTitle(String title) {
        require(window -> window.title().contains(title));
    }

    /**
     * <p>
     * Use the specified macro.
     * </p>
     */
    public static <M extends Macro> void use(Class<M> clazz) {
        Macro macro = I.make(clazz);
    }

    /**
     * @version 2016/10/02 18:01:25
     */
    public interface MacroDSL<V> {

        /**
         * <p>
         * Declare actual action.
         * </p>
         * 
         * @param action
         */
        void run(Runnable action);

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
        Events<V> isPressed();

        /**
         * <p>
         * Declare release event.
         * </p>
         * 
         * @return
         */
        Events<V> isReleased();
    }

    /**
     * @version 2016/10/02 17:30:48
     */
    private class KeyMacro<V> implements MacroDSL<V> {

        /** The window condition. */
        private Predicate<Window> window = windowCondition;

        /** The acceptable event type. */
        private Predicate<Key> condition = ANY;

        /** The event should be consumed or not. */
        private boolean consumable;

        /** The associated key. */
        private Key key;

        private final List<Observer<? super V>> observers = new CopyOnWriteArrayList();

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
         * <p>
         * Set mouse type.
         * </p>
         * 
         * @param mouse
         * @return
         */
        private KeyMacro mouse(Mouse mouse) {
            condition = condition.and(e -> e == mouse.key);

            return this;
        }

        /**
         * @param presses
         * @return
         */
        public KeyMacro when(List<KeyMacro> type) {
            type.add(this);

            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run(Runnable action) {
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
        public Events<V> isPressed() {
            return register((key.mouse ? mouse : keyboard).presses);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Events<V> isReleased() {
            return register((key.mouse ? mouse : keyboard).releases);
        }

        private Events<V> register(List<KeyMacro<V>> macros) {
            macros.add(this);

            return new Events<V>(observer -> {
                observers.add(observer);
                return () -> observers.remove(observer);
            });
        }
    }

    /**
     * @version 2016/10/04 4:20:39
     */
    protected static abstract class NativeHook<T> implements Runnable, HOOKPROC {

        /**
         * <p>
         * Specifies whether the event was injected. The value is 1 if that is the case; otherwise,
         * it is 0. Note that bit 1 is not necessarily set when bit 4 is set. <a href=
         * "https://msdn.microsoft.com/ja-jp/library/windows/desktop/ms644967(v=vs.85).aspx">REF
         * </a>
         * </p>
         */
        protected static final int InjectedEvent = 1 << 4;

        /** The actual executor. */
        protected final ExecutorService executor = new ThreadPoolExecutor(4, 256, 30, TimeUnit.SECONDS, new SynchronousQueue(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(NativeKeyboardHook.class.getSimpleName());
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(false);
            return thread;
        });

        /** The event listeners. */
        protected final List<KeyMacro<T>> presses = new ArrayList();

        /** The event listeners. */
        protected final List<KeyMacro<T>> releases = new ArrayList();

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
        public void run() {
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
         * Handle key event.
         * </p>
         * 
         * @param key
         */
        protected final boolean handle(Key key, List<KeyMacro<T>> macros) {
            boolean consumed = false;

            if (!macros.isEmpty()) {
                Window now = Window.now();

                for (KeyMacro<T> macro : macros) {
                    if (macro.window.test(now) && macro.condition.test(key)) {
                        executor.execute(() -> {
                            for (Observer observer : macro.observers) {
                                observer.accept(key);
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

        /**
         * <p>
         * Handle key event.
         * </p>
         * 
         * @param key
         */
        protected final boolean handle(Mouse key, List<KeyMacro<T>> macros) {
            boolean consumed = false;

            if (!macros.isEmpty()) {
                Window now = Window.now();

                for (KeyMacro<T> macro : macros) {
                    if (macro.window.test(now)) {
                        executor.execute(() -> {
                            for (Observer observer : macro.observers) {
                                observer.accept(key);
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

        /** The key mapper. */
        private static final Key[] keys = new Key[250];

        static {
            for (Key key : Key.values()) {
                keys[key.virtualCode] = key;
            }
        }

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
                    consumed = handle(key, presses);
                    break;

                case WinUser.WM_KEYUP:
                case WinUser.WM_SYSKEYUP:
                    consumed = handle(key, releases);
                    break;
                }
            }
            return consumed ? new LRESULT(1)
                    : User32.INSTANCE.CallNextHookEx(hook, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
        }
    }

    /**
     * @version 2016/10/03 12:31:30
     */
    private static class NativeMouseHook extends NativeHook implements LowLevelMouseProc {

        private static final int WM_MOUSEMOVE = 512;

        private static final int WM_LBUTTONDOWN = 513;

        private static final int WM_LBUTTONUP = 514;

        private static final int WM_RBUTTONDOWN = 516;

        private static final int WM_RBUTTONUP = 517;

        private static final int WM_MBUTTONDOWN = 519;

        private static final int WM_MBUTTONUP = 520;

        private static final int WM_MOUSEWHEEL = 522;

        /** The event listeners. */
        protected final List<KeyMacro> moves = new ArrayList();

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
        public LRESULT callback(int nCode, WPARAM wParam, MOUSEHOOKSTRUCT info) {
            boolean consumed = false;

            if (0 <= nCode) {
                switch (wParam.intValue()) {
                case WM_LBUTTONDOWN:
                    handle(Key.MouseLeft, presses);
                    break;

                case WM_LBUTTONUP:
                    handle(Key.MouseLeft, releases);
                    break;

                case WM_RBUTTONDOWN:
                    handle(Key.MouseRight, presses);
                    break;

                case WM_RBUTTONUP:
                    handle(Key.MouseRight, releases);
                    break;

                case WM_MBUTTONDOWN:
                    handle(Key.MouseMiddle, presses);
                    break;

                case WM_MBUTTONUP:
                    handle(Key.MouseMiddle, releases);
                    break;

                case WM_MOUSEMOVE:
                    handle(Mouse.Move, moves);
                    break;
                }
            }
            return consumed ? new LRESULT(1)
                    : User32.INSTANCE.CallNextHookEx(hook, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
        }
    }

    /**
     * @version 2016/10/04 3:49:48
     */
    private static interface LowLevelMouseProc extends HOOKPROC {

        LRESULT callback(int nCode, WPARAM wParam, MOUSEHOOKSTRUCT lParam);
    }

    /**
     * @version 2016/10/04 3:53:27
     */
    public static class Point extends Structure {

        public NativeLong x;

        public NativeLong y;

        /**
         * {@inheritDoc}
         */
        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] {"x", "y"});
        }
    }

    /**
     * @version 2016/10/04 3:54:39
     */
    public static class MOUSEHOOKSTRUCT extends Structure {

        public POINT pt;

        public HWND hwnd;

        public int wHitTestCode;

        public ULONG_PTR dwExtraInfo;

        /**
         * {@inheritDoc}
         */
        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[] {"pt", "hwnd", "wHitTestCode", "dwExtraInfo"});
        }
    }
}
