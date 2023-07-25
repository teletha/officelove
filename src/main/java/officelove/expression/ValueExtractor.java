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

class ValueExtractor implements Extractor<Object> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Model model(Object object) {
        return Model.of(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object extract(Model model, Property property, Object object) {
        return model.get(object, property);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object extract(Variable variable, String name) {
        return variable.apply(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object extract(ExpressionResolver resolver, Matcher matcher, Object value) {
        return resolver.resolve(matcher, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object extract(Method method, Object[] params, Object object) throws Exception {
        return method.invoke(object, params);
    }
}
