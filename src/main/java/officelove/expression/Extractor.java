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

public interface Extractor<R> {

    /**
     * Model extractor.
     * 
     * @param object
     * @return
     */
    Model model(R object);

    /**
     * Property based value extractor.
     * 
     * @param model
     * @param property
     * @param object
     * @return
     */
    R extract(Model model, Property property, R object);

    /**
     * Builtin variable based value extractor.
     * 
     * @param variable
     * @param name
     * @return
     */
    R extract(Variable variable, String name);

    /**
     * {@link ExpressionResolver} based value extractor.
     * 
     * @param matcher
     * @param value
     * @return
     */
    R extract(ExpressionResolver resolver, Matcher matcher, R value);

    /**
     * Method based balue extractor.
     * 
     * @param method
     * @param params
     * @param object
     * @return
     */
    R extract(Method method, Object[] params, R object) throws Exception;
}
