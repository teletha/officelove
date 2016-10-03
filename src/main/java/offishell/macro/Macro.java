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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.KEYBDINPUT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;

import kiss.Events;
import kiss.I;

/**
 * @version 2016/10/02 17:12:29
 */
public abstract class Macro {

    // initialization
    static {
        try {
            // Create custom logger and level.
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);

            GlobalScreen.setEventDispatcher(new VoidDispatchService());
            GlobalScreen.registerNativeHook();
        } catch (Exception e) {
            throw I.quiet(e);
        }
    }

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

    /** The event listener list. */
    private Repository listeners = new Repository();

    /** The window condition. */
    private Predicate<Window> windowCondition = ANY;

    /**
     * 
     */
    protected Macro() {
        GlobalScreen.addNativeKeyListener(listeners);
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
        return new KeyMacro().when(listeners.keyPresses).key(key);
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
        ip.input.ki.wVk = new WORD(key.nativeCode);
        ip.input.ki.wScan = new WORD(key.scanCode);

        // press
        ip.input.ki.dwFlags = new DWORD(KEYBDINPUT.KEYEVENTF_SCANCODE);
        User32.INSTANCE.SendInput(new DWORD(1), new INPUT[] {ip}, ip.size());

        // release
        ip.input.ki.dwFlags = new DWORD(KEYBDINPUT.KEYEVENTF_KEYUP | KEYBDINPUT.KEYEVENTF_SCANCODE);
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
    public interface MacroDSL {

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
    }

    /**
     * @version 2016/10/02 17:24:52
     */
    private class Repository implements NativeKeyListener {

        private final List<KeyMacro> keyPresses = new ArrayList();

        /**
         * {@inheritDoc}
         */
        @Override
        public void nativeKeyPressed(NativeKeyEvent e) {
            Window now = Window.now();

            for (KeyMacro macro : keyPresses) {
                if (macro.window.test(now) && macro.condition.test(e)) {
                    macro.isActive.set(true);
                    macro.action.run();
                    macro.isActive.set(false);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void nativeKeyReleased(NativeKeyEvent e) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void nativeKeyTyped(NativeKeyEvent e) {
        }
    }

    /**
     * @version 2016/10/02 17:30:48
     */
    private class KeyMacro implements MacroDSL {

        /** The macro activation state. */
        private AtomicBoolean isActive = new AtomicBoolean();

        /** The window condition. */
        private Predicate<Window> window = windowCondition;

        /** The acceptable event type. */
        private Predicate<NativeKeyEvent> condition = e -> {
            return isActive.get() == false;
        };

        /** The actual action. */
        private Runnable action = () -> {
        };

        /**
         * <p>
         * Set key type.
         * </p>
         * 
         * @param key
         * @return
         */
        private KeyMacro key(Key key) {
            condition = condition.and(key::match);

            return this;
        }

        /**
         * @param keyPresses
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
            if (action != null) {
                this.action = action;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MacroDSL consume() {
            condition = condition.and(key -> {
                key.consume();
                return true;
            });
            return this;
        }
    }

    /**
     * @version 2016/10/02 18:08:33
     */
    private static class VoidDispatchService extends ThreadPoolExecutor {

        /**
         * 
         */
        public VoidDispatchService() {
            super(4, 20, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("JNativeHook Dispatch Thread");
                thread.setDaemon(true);

                return thread;
            });
        }
    }

    /**
     * @version 2016/10/03 12:31:30
     */
    private static class NativeKeyboardHook implements Runnable, LowLevelKeyboardProc {

        /** The kye mapper. */
        private static Key[] keys;

        static {
            Key[] values = Key.values();
            keys = new Key[Events.from(values).map(key -> key.nativeCode).scan(0, Math::max).to().getValue() + 1];

            for (Key key : values) {
                keys[key.nativeCode] = key;
            }
        }

        /** The actual executor. */
        private final ExecutorService executor = new ThreadPoolExecutor(4, 256, 30, TimeUnit.SECONDS, new SynchronousQueue(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(NativeKeyboardHook.class.getSimpleName());
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(true);
            return thread;
        });

        /** The clean up. */
        private final Thread cleaner = new Thread(this::uninstall);

        /** The actual native hook. */
        private HHOOK nativeHook;

        /** The event listeners. */
        private final List<KeyMacro> keyPresses = new ArrayList();

        /**
         * <p>
         * Install service.
         * </p>
         */
        private void install() {
            executor.execute(this);
            Runtime.getRuntime().addShutdownHook(cleaner);
        }

        /**
         * <p>
         * Uninstall service.
         * </p>
         */
        private void uninstall() {
            executor.shutdown();
            Runtime.getRuntime().removeShutdownHook(cleaner);
            User32.INSTANCE.UnhookWindowsHookEx(nativeHook);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            nativeHook = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, this, Kernel32.INSTANCE.GetModuleHandle(null), 0);

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
         * {@inheritDoc}
         */
        @Override
        public LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT info) {
            if (nCode >= 0) {
                switch (wParam.intValue()) {
                case WinUser.WM_KEYDOWN:
                    System.out.println(info.vkCode + "  " + info.scanCode + "  " + keys[info.vkCode] + "   " + keys[info.scanCode]);

                case WinUser.WM_KEYUP:
                case WinUser.WM_SYSKEYUP:
                case WinUser.WM_SYSKEYDOWN:
                    System.err.println("in callback, key=" + info.vkCode);
                    if (info.vkCode == 81) {
                        uninstall();
                    }
                    break;
                }
            }

            Pointer ptr = info.getPointer();
            long peer = Pointer.nativeValue(ptr);
            LRESULT result = User32.INSTANCE.CallNextHookEx(nativeHook, nCode, wParam, new WinDef.LPARAM(peer));
            return new WinDef.LRESULT(1);
        }

        private void nativeKeyPressed(NativeKeyEvent e) {
            Window now = Window.now();

            for (KeyMacro macro : keyPresses) {
                if (macro.window.test(now) && macro.condition.test(e)) {
                    macro.isActive.set(true);
                    macro.action.run();
                    macro.isActive.set(false);
                }
            }
        }
    }

    public static void main(String[] args) {
        new NativeKeyboardHook().install();
    }
}
