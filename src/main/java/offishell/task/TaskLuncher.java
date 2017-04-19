/*
 * Copyright (C) 2016 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package offishell.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

import filer.Filer;
import kiss.I;
import offishell.Problem;

/**
 * @version 2016/07/08 15:36:28
 */
public class TaskLuncher {

    /**
     * <p>
     * Lunch Java application
     * </p>
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        try {
            I.$working = Filer.locate(System.getProperty("java.application.path"));
            // I.load(Filer.locate("F:\\Development\\Worker\\target\\classes"));
            if (true) {
                // FIXME
                throw new Error();
            }

            switch (args[0]) {
            case "InvokeMethod":
                invokeMethod(Filer.locate(args[1]), args[2]);
                break;

            default:
                break;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw I.quiet(e);
        }
    }

    /**
     * <p>
     * ワードのからメソッドを起動する
     * </p>
     * 
     * @param file
     * @param methodName
     */
    private static void invokeMethod(Path file, String methodName) {
        for (Task task : I.find(Task.class)) {
            if (file.startsWith(task.directory())) {
                Class clazz = task.getClass();

                try {
                    Method method = clazz.getMethod(methodName);

                    if (Modifier.isStatic(method.getModifiers())) {
                        method.invoke(null);
                    } else {
                        method.invoke(task);
                    }
                    break;
                } catch (NoSuchMethodException e) {
                    throw Problem.of(e)
                            .problem("指定されたメソッドが未定義です。")
                            .solution("メソッド " + clazz.getName() + "#" + methodName + "() を実装してしてください。");
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw Problem.of(e);
                }
            }
        }
    }
}
