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
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
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
    protected final MacroDSL<Key> when(Key key) {
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
        return new KeyMacro().register(this.mouse.moves);
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
     * Emulate press and release event.
     * </p>
     * 
     * @param key
     * @return
     */
    protected final Macro input(Key key) {
        return emulate(key, true, true);
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
         * Consume the native event.
         * </p>
         * 
         * @return
         */
        MacroDSL<V> consume();

        /**
         * <p>
         * Declare press event.
         * </p>
         * 
         * @return
         */
        Events<V> press();

        /**
         * @param consumeEvent
         * @return
         */
        Events<V> press(boolean consumeEvent);

        /**
         * <p>
         * Declare release event.
         * </p>
         * 
         * @return
         */
        Events<V> release();

        /**
         * <p>
         * Declare key modifier.
         * </p>
         * 
         * @return Chainable DSL.
         */
        MacroDSL<V> withAlt();

        /**
         * <p>
         * Declare key modifier.
         * </p>
         * 
         * @return Chainable DSL.
         */
        MacroDSL<V> withCtrl();

        /**
         * <p>
         * Declare key modifier.
         * </p>
         * 
         * @return Chainable DSL.
         */
        MacroDSL<V> withShift();
    }

    /**
     * @version 2016/10/04 15:57:53
     */
    private class KeyMacro<V> implements MacroDSL<V> {

        /** The window condition. */
        private Predicate<Window> window = windowCondition;

        /** The acceptable event type. */
        private Predicate<V> condition = ANY;

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
         * {@inheritDoc}
         */
        @Override
        public MacroDSL<V> consume() {
            consumable = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MacroDSL<V> withAlt() {
            alt = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MacroDSL<V> withCtrl() {
            ctrl = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MacroDSL<V> withShift() {
            shift = true;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Events<V> press() {
            return press(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Events<V> press(boolean consumeEvent) {
            consumable = consumeEvent;
            return register((key.mouse ? mouse : keyboard).presses);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Events<V> release() {
            return register((key.mouse ? mouse : keyboard).releases);
        }

        /**
         * <p>
         * Register this macro.
         * </p>
         * 
         * @param macros
         * @return
         */
        private Events<V> register(List<KeyMacro<V>> macros) {
            macros.add(this);

            return new Events<V>(observer -> {
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
        protected final boolean handle(T key, List<KeyMacro<T>> macros) {
            boolean consumed = false;

            if (!macros.isEmpty()) {
                Window now = Window.now();
                boolean alt = with(Key.Alt);
                boolean ctrl = with(Key.Control);
                boolean shift = with(Key.Shift);

                for (KeyMacro<T> macro : macros) {
                    if (macro.window.test(now) && macro.modifier(alt, ctrl, shift) && macro.condition.test(key)) {
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
                switch (wParam.intValue()) {
                case 513: // WM_LBUTTONDOWN
                    consumed = handle(Key.MouseLeft, presses);
                    break;

                case 514: // WM_LBUTTONUP
                    consumed = handle(Key.MouseLeft, releases);
                    break;

                case 516: // WM_RBUTTONDOWN
                    consumed = handle(Key.MouseRight, presses);
                    break;

                case 517: // WM_RBUTTONUP
                    consumed = handle(Key.MouseRight, releases);
                    break;

                case 519: // WM_MBUTTONDOWN
                    consumed = handle(Key.MouseMiddle, presses);
                    break;

                case 520: // WM_MBUTTONDOWN
                    consumed = handle(Key.MouseMiddle, releases);
                    break;

                case 512: // WM_MOUSEMOVE
                    consumed = handle(Mouse.Move, moves);
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

        LRESULT callback(int nCode, WPARAM wParam, MSLLHOOKSTRUCT lParam);
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
     * @version 2016/10/04 14:06:51
     */
    public static class MSLLHOOKSTRUCT extends Structure {

        public POINT pt;

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
