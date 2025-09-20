/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Exception thrown when API parity validation detects violations or fails.
 *
 * <p>This exception indicates critical parity violations between JNI and Panama
 * implementations that prevent successful validation.
 *
 * @since 1.0.0
 */
public class ParityValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new parity validation exception with the specified message.
     *
     * @param message the detail message
     */
    public ParityValidationException(final String message) {
        super(message);
    }

    /**
     * Creates a new parity validation exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public ParityValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new parity validation exception with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ParityValidationException(final Throwable cause) {
        super(cause);
    }
}