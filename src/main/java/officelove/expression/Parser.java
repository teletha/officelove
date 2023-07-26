/*
 * Copyright (C) 2023 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

import kiss.I;
import kiss.model.Model;
import kiss.model.Property;

/**
 * Parser for the expression language.
 */
public class Parser implements UnaryOperator<String> {

    /** The file name. */
    private final String fileName;

    /** The context state. */
    private final boolean isVertical;

    /** The model object. */
    private final List models;

    /** The extractor. */
    private final Extractor extractor;

    /** The processing state. */
    private boolean inVariable = false;

    /** The buffer. */
    private StringBuilder replace = new StringBuilder();

    /** The buffer. */
    private StringBuilder variable = new StringBuilder();

    /** Cache for {@link ExpressionResolver} */
    private List<ExpressionResolver> expressionResolvers = I.find(ExpressionResolver.class);

    /** Cache for {@link Variable} */
    private List<Variable> variables = I.find(Variable.class);

    public Parser(Object... models) {
        this(null, false, List.of(models));
    }

    /**
     * Create new context with validation mode.
     * 
     * @param fileName
     */
    public Parser(String fileName, List<Class> models) {
        this.fileName = fileName;
        this.isVertical = false;
        this.models = models == null ? Collections.EMPTY_LIST : models.stream().map(Model::of).toList();
        this.extractor = new ModelExtractor();
    }

    /**
     * Create new context.
     * 
     * @param fileName
     * @param isVertical
     * @param models
     */
    public Parser(String fileName, boolean isVertical, List models) {
        this.fileName = fileName;
        this.isVertical = isVertical;
        this.models = models == null ? Collections.EMPTY_LIST : models.stream().filter(Objects::nonNull).toList();
        this.extractor = new ValueExtractor();
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
     * Compute the specified built-in variable.
     * 
     * @param name A variable name.
     * @return
     */
    private Object resolveBuiltinVariable(String name) {
        for (Variable var : variables) {
            if (var.test(name)) {
                return extractor.extract(var, name);
            }
        }
        throw new Error("Can't resolve the variable {$" + name + "} in [" + fileName + "]. Please implement the custom " + Variable.class + " class.");
    }

    /**
     * Compute the specified property variable.
     * 
     * @param expression
     * @return
     */
    public Object resolve(String expression) {
        Set<String> errors = new HashSet();

        boolean optional = expression.endsWith("?");
        if (optional) {
            expression = expression.substring(0, expression.length() - 1);
        }

        List<String> expressions = Arrays.stream(expression.split("\\.")).map(x -> x.strip()).toList();
        if (expressions.isEmpty()) {
            return "";
        }

        if (expressions.get(0).charAt(0) == '$') {
            return resolve(expressions, 1, resolveBuiltinVariable(expression.substring(1)));
        }

        // resolve value from various sources
        for (int i = 0; i < models.size(); i++) {
            try {
                return resolve(expressions, 0, models.get(i));
            } catch (ExpressionException e) {
                if (!optional) {
                    errors.add(e.getMessage());
                }
            }
        }

        if (errors.isEmpty()) {
            return ""; // optional
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("There are several problems with this expression. {" + expression + "}");
            for (String error : errors) {
                builder.append("\n\t").append(error);
            }
            throw new ExpressionException(builder.toString());
        }
    }

    /**
     * Compute the specified property variable.
     * 
     * @param expressions
     * @param index
     * @param value
     * @return
     */
    private Object resolve(List<String> expressions, int index, Object value) {
        if (value == null || value == "") {
            return "";
        }

        if (expressions.size() == index) {
            return value;
        }

        String expression = expressions.get(index);

        for (ExpressionResolver resolver : expressionResolvers) {
            Matcher matcher = resolver.match(expression);

            if (matcher.matches()) {
                return resolve(expressions, index + 1, extractor.extract(resolver, matcher, value));
            }
        }

        Model model = extractor.model(value);
        Property property = model.property(expression);

        // Search from properties
        if (property != null) {
            return resolve(expressions, index + 1, extractor.extract(model, property, value));
        }

        // Search from methods
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
                    return resolve(expressions, index + 1, extractor.extract(method, params, value));
                }
            }
            throw errorInVariableResolve(value, expressions, expression);
        } catch (ExpressionException e) {
            throw e;
        } catch (Exception e) {
            ExpressionException error = errorInVariableResolve(value, expressions, expression);
            error.addSuppressed(e);
            return error;
        }
    }

    /**
     * Describe the error in detail.
     * 
     * @param model
     * @param expressions
     * @param expression
     * @return
     */
    private ExpressionException errorInVariableResolve(Object model, List<String> expressions, String expression) {
        return new ExpressionException("Class [" + model.getClass()
                .getName() + "] can't resolve the variable [" + expression + "] in {" + String
                        .join(".", expressions) + "} at file [" + fileName + "].");
    }

    /**
     * Convert text for vertical alignment.
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