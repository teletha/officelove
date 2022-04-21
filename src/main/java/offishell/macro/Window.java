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

import java.nio.file.Path;

import kiss.Disposable;
import kiss.Signal;
import offishell.UI;
import offishell.platform.Color;
import offishell.platform.Location;
import offishell.platform.Native;

/**
 * @version 2016/08/01 13:59:45
 */
public class Window {

    /** ID */
    private final Object windowID;

    /**
     * @param windowID
     */
    private Window(Object windowID) {
        this.windowID = windowID;
    }

    /**
     * <p>
     * ウインドウタイトルを返します。
     * </p>
     * 
     * @return
     */
    public String title() {
        return Native.API.getWindowTitle(windowID);
    }

    /**
     * <p>
     * ウインドウを閉じます。（同期）
     * </p>
     */
    public void close() {
        Native.API.closeWindow(windowID);
    }

    public void input(Key key) {
    }

    /**
     */
    public Color color(int locationX, int locationY) {
        Location window = windowPosition();
        return color(window.slide(locationX, locationY));
    }

    /**
     */
    public Color color(Location location) {
        return Native.API.getColor(location);
    }

    /**
     * <p>
     * Locate the window related position.
     * </p>
     * 
     * @param locationX
     * @param locationY
     * @return
     */
    public Location locate(int locationX, int locationY) {
        return windowPosition().slide(locationX, locationY);
    }

    /**
     * <p>
     * ウインドウの位置及びサイズと取得。
     * </p>
     */
    public Location windowPosition() {
        return Native.API.getWindowPosition(windowID);
    }

    /**
     * <p>
     * Retrieve the current mouse relative position form this window.
     * </p>
     * 
     * @return
     */
    public Location mousePosition() {
        return windowPosition().relative(Native.API.getCursorPosition());
    }

    /**
     * <p>
     * Check whetner the specified title window exists or not.
     * </p>
     */
    public static boolean existByTitle(String title) {
        return findByTitle(title).to().v != null;
    }

    /**
     * @param path
     */
    public static boolean existByTitle(Path path) {
        return existByTitle(path.getFileName().toString());
    }

    /**
     * @param path
     */
    public static void close(Path path) {
        Window.findByTitle(path).to().v.close();
    }

    /**
     * @param path
     */
    public static void open(Path path) {
        UI.open(path);
    }

    /**
     * <p>
     * Get the current window.
     * </p>
     * 
     * @return
     */
    public static Window now() {
        return new Window(Native.API.activeWindow());
    }

    /**
     * <p>
     * List up all {@link Window}.
     * </p>
     * 
     * @return
     */
    public static Signal<Window> find() {
        return new Signal<Window>((observer, disposer) -> {
            Native.API.enumWindows(id -> {
                observer.accept(new Window(id));
            });
            return Disposable.empty();
        });
    }

    /**
     * <p>
     * List up all {@link Window} which contains the specified title.
     * </p>
     * 
     * @param title A part of title.
     * @return
     */
    public static Signal<Window> findByTitle(String title) {
        return find().take(window -> window.title().contains(title));
    }

    /**
     * <p>
     * List up all {@link Window} which contains the specified title.
     * </p>
     * 
     * @param title A part of title.
     * @return
     */
    public static Signal<Window> findByTitle(Path title) {
        return findByTitle(title.getFileName().toString());
    }

}