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

import static com.sun.jna.Platform.*;

import java.util.function.Consumer;

import kiss.Signal;

/**
 * @version 2016/10/04 21:02:26
 */
public interface Native<ID> {

    /** The actual API for the current platform. */
    Native API = isWindows() ? new WindowsAPI() : new UnknownAPI();

    /**
     * <p>
     * Retrieve the absolute cursor position.
     * </p>
     * 
     * @return
     */
    Location getCursorPosition();

    /**
     * <p>
     * Retrieve color in the specified pixel.
     * </p>
     * 
     * @param x
     * @param y
     * @return
     */
    Color getColor(int x, int y);

    /**
     * <p>
     * Retrieve the color in the specified pixel.
     * </p>
     * 
     * @param location
     */
    Color getColor(Location location);

    /**
     * <p>
     * Retrieve the acttive client rect.
     * </p>
     * 
     * @return
     */
    int[] getActiveClientRect();

    /**
     * <p>
     * Retrieve the specified window rect.
     * </p>
     * 
     * @param windowID
     * @return
     */
    Location getWindowPosition(ID windowID);

    /**
     * <p>
     * Retrieve the specified window title.
     * </p>
     * 
     * @param windowID
     * @return
     */
    String getWindowTitle(ID windowID);

    /**
     * <p>
     * Close the specified window.
     * </p>
     * 
     * @param windowID
     */
    void closeWindow(ID windowID);

    /**
     * <p>
     * Retrieve the active window id.
     * </p>
     * 
     * @return
     */
    ID activeWindow();

    /**
     * <p>
     * List up all windows.
     * </p>
     * 
     * @param process
     */
    void enumWindows(Consumer<ID> process);

    /**
     * <p>
     * Execute the command.
     * </p>
     * 
     * @param command
     */
    void execute(Object... command);

    /**
     * <p>
     * Execute OCR.
     * </p>
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    String ocr(int x, int y, int width, int height);

    /**
     * <p>
     * Listen the change of clipboard.
     * </p>
     * 
     * @return
     */
    Signal<String> clipboard();
}