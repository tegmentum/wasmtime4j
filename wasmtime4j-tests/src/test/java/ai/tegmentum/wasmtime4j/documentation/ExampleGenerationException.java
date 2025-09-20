/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Exception thrown when code example generation fails.
 *
 * <p>This exception indicates errors in the example generation process,
 * such as template processing failures or code generation issues.
 *
 * @since 1.0.0
 */
public class ExampleGenerationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new example generation exception with the specified message.
     *
     * @param message the detail message
     */
    public ExampleGenerationException(final String message) {
        super(message);
    }

    /**
     * Creates a new example generation exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ExampleGenerationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new example generation exception with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ExampleGenerationException(final Throwable cause) {
        super(cause);
    }
}