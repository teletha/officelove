/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officelove.expression;

import java.lang.reflect.Method;
import java.util.regex.Matcher;

import kiss.model.Model;
import kiss.model.Property;

class ModelExtractor implements Extractor<Model> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Model model(Model object) {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Model extract(Model model, Property property, Model object) {
        return property.model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Model extract(Variable variable, String name) {
        return Model.of(variable.apply(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Model extract(ExpressionResolver resolver, Matcher matcher, Model value) {
        return Model.of(Model.collectParameters(resolver.getClass(), ExpressionResolver.class)[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Model extract(Method method, Object[] params, Model object) throws Exception {
        return Model.of(method.getReturnType());
    }
}
