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
