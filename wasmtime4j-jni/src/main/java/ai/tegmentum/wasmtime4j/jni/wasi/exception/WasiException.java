package ai.tegmentum.wasmtime4j.jni.wasi.exception;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;

/**
 * Base exception for WASI system operation failures.
 *
 * <p>This exception serves as the base for all WASI-related errors and provides comprehensive error
 * information including:
 *
 * <ul>
 *   <li>WASI error codes (errno values)
 *   <li>System operation context
 *   <li>File paths or resource information
 *   <li>Underlying native error details
 * </ul>
 *
 * <p>The exception hierarchy maps native WASI errors to appropriate Java exception types for better
 * error handling and debugging.
 *
 * @since 1.0.0
 */
public class WasiException extends JniException {

  private static final long serialVersionUID = 1L;

  /** The WASI error code (errno value). */
  private final WasiErrorCode errorCode;

  /** The system operation that failed. */
  private final String operation;

  /** The resource (file path, descriptor, etc.) associated with the error. */
  private final String resource;

  /**
   * Creates a new WASI exception with the specified error information.
   *
   * @param message the error message
   * @param errorCode the WASI error code
   * @param operation the system operation that failed
   * @param resource the resource associated with the error
   */
  public WasiException(
      final String message,
      final WasiErrorCode errorCode,
      final String operation,
      final String resource) {
    super(formatMessage(message, errorCode, operation, resource));
    this.errorCode = errorCode;
    this.operation = operation;
    this.resource = resource;
  }

  /**
   * Creates a new WASI exception with the specified error information and cause.
   *
   * @param message the error message
   * @param errorCode the WASI error code
   * @param operation the system operation that failed
   * @param resource the resource associated with the error
   * @param cause the underlying cause
   */
  public WasiException(
      final String message,
      final WasiErrorCode errorCode,
      final String operation,
      final String resource,
      final Throwable cause) {
    super(formatMessage(message, errorCode, operation, resource), cause);
    this.errorCode = errorCode;
    this.operation = operation;
    this.resource = resource;
  }

  /**
   * Creates a new WASI exception with the specified message and error code.
   *
   * @param message the error message
   * @param errorCode the WASI error code
   */
  public WasiException(final String message, final WasiErrorCode errorCode) {
    this(message, errorCode, null, null);
  }

  /**
   * Creates a new WASI exception with the specified error code and operation.
   *
   * @param errorCode the WASI error code
   * @param operation the system operation that failed
   */
  public WasiException(final WasiErrorCode errorCode, final String operation) {
    this(errorCode.getDescription(), errorCode, operation, null);
  }

  /**
   * Creates a new WASI exception with the specified error code, operation, and resource.
   *
   * @param errorCode the WASI error code
   * @param operation the system operation that failed
   * @param resource the resource associated with the error
   */
  public WasiException(
      final WasiErrorCode errorCode, final String operation, final String resource) {
    this(errorCode.getDescription(), errorCode, operation, resource);
  }

  /**
   * Gets the WASI error code.
   *
   * @return the WASI error code
   */
  public WasiErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Gets the system operation that failed.
   *
   * @return the operation name
   */
  public String getOperation() {
    return operation;
  }

  /**
   * Gets the resource associated with the error.
   *
   * @return the resource identifier, or null if not applicable
   */
  public String getResource() {
    return resource;
  }

  /**
   * Checks if this exception represents a file system error.
   *
   * @return true if this is a file system error, false otherwise
   */
  public boolean isFileSystemError() {
    return errorCode != null && errorCode.isFileSystemError();
  }

  /**
   * Checks if this exception represents a network error.
   *
   * @return true if this is a network error, false otherwise
   */
  public boolean isNetworkError() {
    return errorCode != null && errorCode.isNetworkError();
  }

  /**
   * Checks if this exception represents a permission error.
   *
   * @return true if this is a permission error, false otherwise
   */
  public boolean isPermissionError() {
    return errorCode != null && errorCode.isPermissionError();
  }

  /**
   * Checks if this exception represents a resource limit error.
   *
   * @return true if this is a resource limit error, false otherwise
   */
  public boolean isResourceLimitError() {
    return errorCode != null && errorCode.isResourceLimitError();
  }

  /**
   * Checks if the operation can be retried.
   *
   * @return true if the operation can be retried, false otherwise
   */
  public boolean isRetryable() {
    return errorCode != null && errorCode.isRetryable();
  }

  /**
   * Formats the exception message with error details.
   *
   * @param message the base message
   * @param errorCode the error code
   * @param operation the operation
   * @param resource the resource
   * @return the formatted message
   */
  private static String formatMessage(
      final String message,
      final WasiErrorCode errorCode,
      final String operation,
      final String resource) {
    final StringBuilder sb = new StringBuilder();

    if (message != null && !message.isEmpty()) {
      sb.append(message);
    } else if (errorCode != null) {
      sb.append(errorCode.getDescription());
    } else {
      sb.append("WASI system operation failed");
    }

    if (operation != null) {
      sb.append(" (operation: ").append(operation).append(")");
    }

    if (resource != null) {
      sb.append(" (resource: ").append(resource).append(")");
    }

    if (errorCode != null) {
      sb.append(" (errno: ").append(errorCode.getErrno()).append(")");
    }

    return sb.toString();
  }

  /**
   * Creates a new WASI exception with just a message.
   *
   * @param message the error message
   */
  public WasiException(final String message) {
    this(message, WasiErrorCode.EIO, "operation", "resource");
  }
}
