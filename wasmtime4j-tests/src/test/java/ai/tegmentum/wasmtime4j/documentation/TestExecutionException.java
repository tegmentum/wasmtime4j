/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Exception thrown when test execution fails due to system or framework issues.
 *
 * <p>This exception indicates errors in the test execution infrastructure
 * rather than individual test failures.
 *
 * @since 1.0.0
 */
public class TestExecutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new test execution exception with the specified message.
     *
     * @param message the detail message
     */
    public TestExecutionException(final String message) {
        super(message);
    }

    /**
     * Creates a new test execution exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public TestExecutionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new test execution exception with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public TestExecutionException(final Throwable cause) {
        super(cause);
    }
}