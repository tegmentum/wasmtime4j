/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Exception thrown when documentation generation or analysis fails.
 *
 * <p>This exception indicates errors in the documentation generation process, such as analysis
 * failures, file access issues, or validation errors.
 *
 * @since 1.0.0
 */
public class DocumentationException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new documentation exception with the specified message.
   *
   * @param message the detail message
   */
  public DocumentationException(final String message) {
    super(message);
  }

  /**
   * Creates a new documentation exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public DocumentationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new documentation exception with the specified cause.
   *
   * @param cause the cause of the exception
   */
  public DocumentationException(final Throwable cause) {
    super(cause);
  }
}
