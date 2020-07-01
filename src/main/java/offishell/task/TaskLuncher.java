/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import kiss.I;
import offishell.Problem;
import psychopath.File;
import psychopath.Locator;

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
            // I.load(Filer.locate("F:\\Development\\Worker\\target\\classes"));
            if (true) {
                // FIXME
                throw new Error();
            }

            switch (args[0]) {
            case "InvokeMethod":
                invokeMethod(Locator.file(args[1]), args[2]);
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
    private static void invokeMethod(File file, String methodName) {
        for (Task task : I.find(Task.class)) {
            if (file.asJavaPath().startsWith(task.directory())) {
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