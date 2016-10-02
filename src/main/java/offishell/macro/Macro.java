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
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import kiss.I;
import kiss.Manageable;
import kiss.Singleton;

/**
 * @version 2016/10/02 17:12:29
 */
public abstract class Macro {

    /** The single repository. */
    private static final Repository macros = I.make(Repository.class);

    // initialization
    static {
        try {
            // Create custom logger and level.
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);

            GlobalScreen.setEventDispatcher(new VoidDispatchService());
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(macros);
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

    /**
     * <p>
     * Use the specified macro.
     * </p>
     */
    public static <M extends Macro> void use(Class<M> clazz) {
        Macro macro = I.make(clazz);
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
        return new KeyMacro().when(macros.keyPresses).key(key);
    }

    /**
     * <p>
     * Emulate press event.
     * </p>
     * 
     * @param key
     * @return
     */
    protected final void press(Key key) {
        // Bring the window to the front
        Window window = Window.now();
        window.input(key);
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
    @Manageable(lifestyle = Singleton.class)
    private static class Repository implements NativeKeyListener {

        private final List<KeyMacro> keyPresses = new ArrayList();

        /**
         * {@inheritDoc}
         */
        @Override
        public void nativeKeyPressed(NativeKeyEvent e) {
            for (KeyMacro macro : keyPresses) {
                if (macro.condition.test(e)) {
                    macro.action.run();
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
    private static class KeyMacro implements MacroDSL {

        /** The acceptable event type. */
        private Predicate<NativeKeyEvent> condition = ANY;

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
    private static class VoidDispatchService extends AbstractExecutorService {

        private boolean running = false;

        /**
         * 
         */
        public VoidDispatchService() {
            running = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shutdown() {
            running = false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Runnable> shutdownNow() {
            running = false;
            return new ArrayList<Runnable>(0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isShutdown() {
            return !running;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isTerminated() {
            return !running;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }
    }
}
