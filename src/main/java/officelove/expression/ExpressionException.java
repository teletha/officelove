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

@SuppressWarnings("serial")
public class ExpressionException extends RuntimeException {

    /**
     * Build exception for expression.
     * 
     * @param error
     */
    public ExpressionException(String error) {
        super(error);
    }
}