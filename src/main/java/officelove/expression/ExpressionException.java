/*
 * Copyright (C) 2024 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.expression;

import java.util.Objects;

@SuppressWarnings("serial")
public class ExpressionException extends RuntimeException {

    /** The file location. */
    private String location;

    /**
     * Build exception for expression.
     * 
     * @param error
     */
    public ExpressionException(String error) {
        super(error);
    }

    /**
     * Set the location of invalid file.
     */
    public ExpressionException location(String location) {
        this.location = Objects.requireNonNull(location);

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        if (location == null) {
            return super.getMessage();
        } else {
            return location + "\n" + super.getMessage();
        }
    }
}