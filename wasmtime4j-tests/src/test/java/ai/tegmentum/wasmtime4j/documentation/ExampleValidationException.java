/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Exception thrown when code example validation fails.
 *
 * <p>This exception indicates that generated examples fail to compile
 * or execute correctly, indicating issues with the examples or API.
 *
 * @since 1.0.0
 */
public class ExampleValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new example validation exception with the specified message.
     *
     * @param message the detail message
     */
    public ExampleValidationException(final String message) {
        super(message);
    }

    /**
     * Creates a new example validation exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ExampleValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new example validation exception with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ExampleValidationException(final Throwable cause) {
        super(cause);
    }
}