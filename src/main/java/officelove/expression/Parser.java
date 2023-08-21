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
import java.util.ArrayList;
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

    /** Cache for {@link Variable} */
    private List<Variable> variables = I.find(Variable.class);

    /** Cache for {@link ExpressionResolver} */
    private List<ExpressionResolver> resolvers = I.find(ExpressionResolver.class);

    /** The target type of {@link ExpressionResolver}. */
    private List<Class> resolverTypes = I.signal(resolvers)
            .map(x -> Model.collectParameters(x.getClass(), ExpressionResolver.class)[0])
            .as(Class.class)
            .toList();

    /**
     * Create parser by validation mode.
     */
    public Parser(List<Class> models) {
        this.isVertical = false;
        this.models = models == null ? Collections.EMPTY_LIST : models.stream().map(Model::of).toList();
        this.extractor = new ModelExtractor();
    }

    /**
     * Create parser with context models.
     * 
     * @param models
     */
    public Parser(Object... models) {
        this(List.of(models), false);
    }

    /**
     * Create parser with context models.
     * 
     * @param models
     * @param isVertical
     */
    public Parser(List models, boolean isVertical) {
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

                    try {
                        replace.append(I.transform(resolve(variable.toString()), String.class));
                    } finally {
                        // clear variable info
                        variable = new StringBuilder();
                    }
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
            verticalize(replace);
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
        throw new Error("Can't resolve the variable {$" + name + "}. Please implement the custom " + Variable.class + " class.");
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

        List<String> expressions = parse(expression);
        if (expressions.isEmpty()) {
            return "";
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
     * Parse the expression.
     * 
     * @param expression
     * @return
     */
    private List<String> parse(String expression) {
        List<String> parts = new ArrayList();
        boolean sequencial = false;
        StringBuilder part = new StringBuilder();

        for (int i = 0, length = expression.length(); i < length; i++) {
            char c = expression.charAt(i);
            switch (c) {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                sequencial = false;
                break; // ignore space

            case '.':
                sequencial = false;
                if (i + 1 != length && Character
                        .isDigit(expression.charAt(i + 1)) && (0 <= i - 1 && Character.isDigit(expression.charAt(i - 1)))) {
                    part.append(c);
                } else {
                    parts.add(part.toString());
                    part.setLength(0);
                }
                break;

            case '+':
            case '-':
            case '*':
            case '/':
            case '%':
            case '#':
            case '&':
            case '!':
            case '=':
            case '<':
            case '>':
            case '?':
            case '@':
                if (sequencial) {
                    // do nothing
                } else {
                    sequencial = true;
                    parts.add(part.toString());
                    part.setLength(0);
                }
                part.append(c);
                break;

            default:
                sequencial = false;
                part.append(c);
                break;
            }
        }
        parts.add(part.toString());
        return parts;
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

        if (index == 0 && expression.charAt(0) == '$') {
            return resolve(expressions, 1, resolveBuiltinVariable(expression.substring(1)));
        }

        for (int i = 0; i < resolvers.size(); i++) {
            if (resolverTypes.get(i).isInstance(value) || value instanceof Model) {
                ExpressionResolver resolver = resolvers.get(i);
                Matcher matcher = resolver.match(expression);

                if (matcher.matches()) {
                    return resolve(expressions, index + 1, extractor.extract(resolver, matcher, value));
                }
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
            List<String> parametersText;

            if (start == -1 && end == -1) {
                // without parameter
                name = expression;
                parametersText = Collections.EMPTY_LIST;
            } else if (start != -1 && end != -1) {
                // with parameter
                name = expression.substring(0, start);
                parametersText = Arrays.stream(expression.substring(start + 1, end).split(",")).map(String::strip).toList();
            } else {
                throw errorInVariableResolve(value, expression);
            }

            for (Method method : model.type.getMethods()) {
                // exclude void type
                if (method.getReturnType() == void.class) {
                    continue;
                }

                if (method.getName().equals(name) && method.getParameterCount() == parametersText.size()) {
                    Object[] params = new Object[parametersText.size()];

                    for (int i = 0; i < parametersText.size(); i++) {
                        params[i] = I.transform(parametersText.get(i), method.getParameterTypes()[i]);
                    }
                    method.setAccessible(true);
                    return resolve(expressions, index + 1, extractor.extract(method, params, value));
                }
            }
            throw errorInVariableResolve(value, expression);
        } catch (ExpressionException e) {
            throw e;
        } catch (Exception e) {
            ExpressionException error = errorInVariableResolve(value, expression);
            error.addSuppressed(e);
            return error;
        }
    }

    /**
     * Describe the error in detail.
     * 
     * @param model
     * @param expression
     * @return
     */
    private ExpressionException errorInVariableResolve(Object model, String expression) {
        Class type = model instanceof Model m ? m.type : model.getClass();

        return new ExpressionException("Class [" + type.getName() + "] can't resolve the expression [" + expression + "].");
    }

    /**
     * Convert text for vertical alignment.
     * 
     * @param text
     */
    static void verticalize(StringBuilder text) {
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
            case '1':
            case '１':
                text.setCharAt(i, '一');
                break;
            case '2':
            case '２':
                text.setCharAt(i, '二');
                break;
            case '3':
            case '３':
                text.setCharAt(i, '三');
                break;
            case '4':
            case '４':
                text.setCharAt(i, '四');
                break;
            case '5':
            case '５':
                text.setCharAt(i, '五');
                break;
            case '6':
            case '６':
                text.setCharAt(i, '六');
                break;
            case '7':
            case '７':
                text.setCharAt(i, '七');
                break;
            case '8':
            case '８':
                text.setCharAt(i, '八');
                break;
            case '9':
            case '９':
                text.setCharAt(i, '九');
                break;
            case '0':
            case '０':
                text.setCharAt(i, '〇');
                break;
            }
        }
    }
}