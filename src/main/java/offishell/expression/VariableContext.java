/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package offishell.expression;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

import kiss.I;
import kiss.model.Model;
import kiss.model.Property;
import offishell.Problem;

/**
 * @version 2016/06/04 18:04:12
 */
public class VariableContext implements UnaryOperator<String> {

    private final String fileName;

    private final boolean isVertical;

    /** The model object. */
    private final List models;

    private boolean inVariable = false;

    private StringBuilder replace = new StringBuilder();

    private StringBuilder variable = new StringBuilder();

    /** Cache for {@link ExpressionResolver} */
    private List<ExpressionResolver> expressionResolvers = I.find(ExpressionResolver.class);

    /** Cache for {@link Variable} */
    private List<Variable> variables = I.find(Variable.class);

    /**
     * @param model
     */
    public VariableContext(String fileName, boolean isVertical, Object model) {
        this(fileName, isVertical, Collections.singletonList(model));
    }

    /**
     */
    public VariableContext(String fileName, boolean isVertical, List models) {
        this.fileName = fileName;
        this.isVertical = isVertical;
        if (models == null || models.size() == 0) {
            throw new Error("モデルを指定してください。");
        }
        this.models = models;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String apply(String text) {
        if (text != null) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                switch (c) {
                case '{':
                    inVariable = true;
                    break;

                case '}':
                    inVariable = false;

                    replace.append(I.transform(resolve(variable.toString()), String.class));

                    // clear variable info
                    variable = new StringBuilder();
                    break;

                default:
                    if (inVariable) {
                        variable.append(c);
                    } else {
                        replace.append(c);
                    }
                    break;
                }
            }
        }

        if (isVertical) {
            convertForVerticalText(replace);
        }

        String replaced = replace.toString();
        replace = new StringBuilder();
        return replaced;
    }

    /**
     * <p>
     * Compute the specified built-in variable.
     * </p>
     * 
     * @param variable
     * @return
     */
    private Object resolveBuiltinVariable(String variable) {
        for (Variable var : variables) {
            if (var.test(variable)) {
                return var.apply(variable);
            }
        }
        throw Problem.of("変数『$" + variable + "』は使用できません。 [" + fileName + "]").solution(Variable.class + "を実装したクラスを作成してください。");
    }

    /**
     * <p>
     * Compute the specified property variable.
     * </p>
     * 
     * @param paths
     * @return
     */
    public Object resolve(String paths) {
        Error error = null;

        boolean optional = paths.endsWith("?");

        if (optional) {
            paths = paths.substring(0, paths.length() - 1);
        }

        for (Object value : models) {
            try {
                return resolve(paths.split("\\."), 0, value);
            } catch (Error e) {
                if (error == null) {
                    error = e;
                } else {
                    error.addSuppressed(e);
                }
            }
        }

        if (optional) {
            return "";
        }

        if (error == null) {
            error = new Error();
        }
        throw error;
    }

    /**
     * <p>
     * Compute the specified property variable.
     * </p>
     * 
     * @param expressions
     * @param index
     * @param value
     * @return
     */
    private Object resolve(String[] expressions, int index, Object value) {
        if (value == null || value == "") {
            return "";
        }

        if (expressions.length == index) {
            return value;
        }

        String expression = expressions[index];
        if (expression.charAt(0) == '$') {
            return resolve(expressions, index + 1, resolveBuiltinVariable(expression.substring(1)));
        }

        for (ExpressionResolver resolver : expressionResolvers) {
            Matcher matcher = resolver.match(expression);

            if (matcher.matches()) {
                return resolve(expressions, index + 1, resolver.resolve(matcher, value));
            }
        }

        Model model = Model.of(value);
        Property property = model.property(expression);

        // プロパティから検索
        if (property != null) {
            return resolve(expressions, index + 1, model.get(value, property));
        }

        // プロパティがないのでメソッドを検索して実行する
        try {
            int start = expression.indexOf("(");
            int end = expression.lastIndexOf(")");

            String name;
            String[] parametersText;

            if (start == -1 && end == -1) {
                // without parameter
                name = expression;
                parametersText = new String[0];
            } else if (start != -1 && end != -1) {
                // with parameter
                name = expression.substring(0, start);
                parametersText = expression.substring(start + 1, end).split(",");
            } else {
                throw errorInVariableResolve(value, expressions, expression);
            }

            for (Method method : model.type.getMethods()) {
                // exclude void type
                if (method.getReturnType() == void.class) {
                    continue;
                }

                if (method.getName().equals(name) && method.getParameterCount() == parametersText.length) {
                    Object[] params = new Object[parametersText.length];

                    for (int i = 0; i < parametersText.length; i++) {
                        params[i] = I.transform(parametersText[i], method.getParameterTypes()[i]);
                    }
                    method.setAccessible(true);
                    return resolve(expressions, index + 1, method.invoke(value, params));
                }
            }
            throw errorInVariableResolve(value, expressions, expression);
        } catch (Throwable e) {
            throw errorInVariableResolve(value, expressions, expression).error(e);
        }
    }

    /**
     * <p>
     * エラーの詳細を記述します。
     * </p>
     * 
     * @param model
     * @param expressions
     * @param expression
     * @return
     */
    private Problem errorInVariableResolve(Object model, String[] expressions, String expression) {
        return Problem.of("文書 [" + fileName + "] の変数 [" + String
                .join(".", expressions) + "] で使われている [" + expression + "] は" + model.getClass().getSimpleName() + "クラスでは解決できません。");
    }

    /**
     * <p>
     * Convert text for vertical alignment.
     * </p>
     * 
     * @param replace2
     */
    private void convertForVerticalText(StringBuilder builder) {
        for (int i = 0; i < builder.length(); i++) {
            switch (builder.charAt(i)) {
            case '1':
            case '１':
                builder.setCharAt(i, '一');
                break;
            case '2':
            case '２':
                builder.setCharAt(i, '二');
                break;
            case '3':
            case '３':
                builder.setCharAt(i, '三');
                break;
            case '4':
            case '４':
                builder.setCharAt(i, '四');
                break;
            case '5':
            case '５':
                builder.setCharAt(i, '五');
                break;
            case '6':
            case '６':
                builder.setCharAt(i, '六');
                break;
            case '7':
            case '７':
                builder.setCharAt(i, '七');
                break;
            case '8':
            case '８':
                builder.setCharAt(i, '八');
                break;
            case '9':
            case '９':
                builder.setCharAt(i, '九');
                break;
            case '0':
            case '０':
                builder.setCharAt(i, '〇');
                break;
            }
        }
    }
}