/*
 * Copyright (C) 2019 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell;

import java.lang.reflect.InvocationTargetException;
import java.util.StringJoiner;

/**
 * @version 2016/07/08 17:03:40
 */
@SuppressWarnings("serial")
public class Problem extends Error {

    private static final String EOL = System.lineSeparator();

    private String problem;

    private String solution;

    /**
     * 
     */
    private Problem(Throwable cause) {
        super(cause);

        StringJoiner joiner = new StringJoiner(EOL);

        for (StackTraceElement element : cause.getStackTrace()) {
            joiner.add(element.toString());
        }
        this.problem = joiner.toString();
    }

    /**
     * 
     */
    private Problem(String problem, String solution) {
        this.problem = problem;
        this.solution = solution;
    }

    /**
     * <p>
     * エラー内容を記述します。
     * </p>
     * 
     * @param description
     * @return
     */
    public Problem problem(String description) {
        this.problem = description;
        return this;
    }

    /**
     * <p>
     * 解決方法を記述します。
     * </p>
     * 
     * @param description
     * @return
     */
    public Problem solution(String description) {
        this.solution = description;
        return this;
    }

    /**
     * <p>
     * 関連するエラーを追記します。
     * </p>
     * 
     * @param e
     * @return
     */
    public Problem error(Throwable e) {
        if (e != null) {
            addSuppressed(e);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder();

        if (problem != null) {
            builder.append(EOL).append("Problem").append(EOL).append(problem).append(EOL);
        }

        if (solution != null) {
            builder.append(EOL).append("Solution").append(EOL).append(solution).append(EOL);
        }
        return builder.toString();
    }

    /**
     * <p>
     * Create {@link Problem} for null input.
     * </p>
     * 
     * @param error
     * @return
     */
    public static Problem inputNull(String error) {
        return new Problem(error, null);
    }

    public static Problem of(String error) {
        return new Problem(error, null);
    }

    /**
     * <p>
     * Create {@link Problem} for the specified {@link Throwable}.
     * </p>
     * 
     * @param cause
     * @return
     */
    public static Problem of(Throwable cause) {
        if (cause instanceof Problem) {
            return (Problem) cause;
        }

        if (cause instanceof InvocationTargetException) {
            return of(((InvocationTargetException) cause).getTargetException());
        }
        return new Problem(cause);
    }
}
