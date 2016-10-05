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

import java.util.function.Consumer;

/**
 * @version 2016/10/04 21:11:29
 */
class UnknownAPI implements Native {

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getCursorPosition() {
        throw error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getColor(int x, int y) {
        throw error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getColor(Location location) {
        throw error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getActiveClientRect() {
        throw error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getWindowPosition(Object windowID) {
        throw error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWindowTitle(Object windowID) {
        throw error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeWindow(Object windowID) {
        throw error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object activeWindow() {
        throw error();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enumWindows(Consumer process) {
        throw error();
    }

    /**
     * <p>
     * Throw error.
     * </p>
     * 
     * @return
     */
    private RuntimeException error() {
        return new UnsupportedOperationException(API.getClass().getSimpleName() + " is not supported in this platform.");
    }
}
