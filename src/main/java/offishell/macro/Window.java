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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;

import kiss.Disposable;
import kiss.Events;
import kiss.I;
import offishell.UI;

/**
 * @version 2016/08/01 13:59:45
 */
public class Window {

    /** Robot. */
    private final Robot robot;

    /** ID */
    private final HWND id;

    /**
     * @param id
     */
    private Window(HWND id) {
        try {
            this.id = id;
            this.robot = new Robot();
        } catch (AWTException e) {
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * ウインドウタイトルを返します。
     * </p>
     * 
     * @return
     */
    public String title() {
        return text(id, User32.INSTANCE::GetWindowText);
    }

    /**
     * <p>
     * ウインドウを閉じます。（同期）
     * </p>
     */
    public void close() {
        User32.INSTANCE.PostMessage(id, WinUser.WM_CLOSE, null, null);

        while (User32.INSTANCE.IsWindowVisible(id)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw I.quiet(e);
            }
        }
    }

    public void input(Key key) {
        robot.keyPress(key.nativeCode);
        robot.keyRelease(key.nativeCode);
    }

    /**
     * 
     */
    public void color(int x, int y) {
        Rectangle window = position();
        Rectangle rect = new Rectangle(window.x + x, window.y + y, 1, 1);
        BufferedImage image = robot.createScreenCapture(rect);
        Color color = new Color(image.getRGB(0, 0));
        System.out.println(color);
        Icon icon = new ImageIcon(image);

        JOptionPane.showMessageDialog(null, "←イメージ", // メッセージ
                "イメージ確認", // タイトル
                JOptionPane.PLAIN_MESSAGE, icon // 画像のアイコン
        );
    }

    /**
     * <p>
     * ウインドウの位置及びサイズと取得。
     * </p>
     */
    public Rectangle position() {
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(id, rect);
        return rect.toRectangle();
    }

    /**
     * <p>
     * Check whetner the specified title window exists or not.
     * </p>
     * 
     * @param string
     */
    public static boolean existByTitle(String title) {
        return findByTitle(title).to().getValue() != null;
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
        Window.findByTitle(path).to().getValue().close();
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
     * @param title A part of title.
     * @return
     */
    public static Window now() {
        return new Window(User32.INSTANCE.GetForegroundWindow());
    }

    /**
     * <p>
     * List up all {@link Window}.
     * </p>
     * 
     * @return
     */
    public static Events<Window> find() {
        return new Events<Window>(observer -> {
            User32.INSTANCE.EnumWindows((hWnd, ponter) -> {
                observer.accept(new Window(hWnd));
                return true;
            }, null);
            return Disposable.Φ;
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
    public static Events<Window> findByTitle(String title) {
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
    public static Events<Window> findByTitle(Path title) {
        return findByTitle(title.getFileName().toString());
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
    private static String text(HWND id, TriFunction<HWND, char[], Integer, Integer> consumer) {
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
}
