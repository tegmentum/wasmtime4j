package ai.tegmentum.wasmtime4j.exception;

/**
 * Base class for all WASI-related exceptions.
 *
 * <p>This is the root exception class for all errors that can occur during WASI operations,
 * including component instantiation, resource management, system calls, and configuration.
 *
 * <p>WASI exceptions provide additional context beyond basic WebAssembly exceptions:
 *
 * <ul>
 *   <li>WASI operation context (system call, component operation, etc.)
 *   <li>Resource information (file paths, handles, etc.)
 *   <li>Error categorization for recovery strategies
 *   <li>Retry guidance for transient errors
 * </ul>
 *
 * <p>This class serves as the public API exception base and abstracts implementation-specific
 * error details from JNI and Panama runtimes into a unified error handling interface.
 *
 * @since 1.0.0
 */
public class WasiException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** The WASI operation that failed. */
  private final String operation;

  /** The resource (file path, handle, etc.) associated with the error. */
  private final String resource;

  /** Whether the operation can potentially be retried. */
  private final boolean retryable;

  /** The error category for handling strategies. */
  private final ErrorCategory category;

  /**
   * Error categories for WASI exceptions to guide error handling strategies.
   */
  public enum ErrorCategory {
    /** File system related errors (I/O, permissions, paths). */
    FILE_SYSTEM,
    /** Network related errors (connections, timeouts). */
    NETWORK,
    /** Permission and security related errors. */
    PERMISSION,
    /** Resource limit errors (memory, file handles, etc.). */
    RESOURCE_LIMIT,
    /** Component model specific errors. */
    COMPONENT,
    /** Configuration and setup errors. */
    CONFIGURATION,
    /** General system errors. */
    SYSTEM
  }

  /**
   * Creates a new WASI exception with the specified message.
   *
   * @param message the error message
   */
  public WasiException(final String message) {
    this(message, null, null, false, ErrorCategory.SYSTEM);
  }

  /**
   * Creates a new WASI exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasiException(final String message, final Throwable cause) {
    this(message, null, null, false, ErrorCategory.SYSTEM, cause);
  }

  /**
   * Creates a new WASI exception with detailed error information.
   *
   * @param message the error message
   * @param operation the WASI operation that failed
   * @param resource the resource associated with the error (may be null)
   * @param retryable whether the operation can be retried
   * @param category the error category
   */
  public WasiException(
      final String message,
      final String operation,
      final String resource,
      final boolean retryable,
      final ErrorCategory category) {
    super(formatMessage(message, operation, resource));
    this.operation = operation;
    this.resource = resource;
    this.retryable = retryable;
    this.category = category;
  }

  /**
   * Creates a new WASI exception with detailed error information and cause.
   *
   * @param message the error message
   * @param operation the WASI operation that failed
   * @param resource the resource associated with the error (may be null)
   * @param retryable whether the operation can be retried
   * @param category the error category
   * @param cause the underlying cause
   */
  public WasiException(
      final String message,
      final String operation,
      final String resource,
      final boolean retryable,
      final ErrorCategory category,
      final Throwable cause) {
    super(formatMessage(message, operation, resource), cause);
    this.operation = operation;
    this.resource = resource;
    this.retryable = retryable;
    this.category = category;
  }

  /**
   * Gets the WASI operation that failed.
   *
   * @return the operation name, or null if not specified
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
   * Checks if the operation can potentially be retried.
   *
   * @return true if the operation can be retried, false otherwise
   */
  public boolean isRetryable() {
    return retryable;
  }

  /**
   * Gets the error category for this exception.
   *
   * @return the error category
   */
  public ErrorCategory getCategory() {
    return category;
  }

  /**
   * Checks if this exception represents a file system error.
   *
   * @return true if this is a file system error, false otherwise
   */
  public boolean isFileSystemError() {
    return category == ErrorCategory.FILE_SYSTEM;
  }

  /**
   * Checks if this exception represents a network error.
   *
   * @return true if this is a network error, false otherwise
   */
  public boolean isNetworkError() {
    return category == ErrorCategory.NETWORK;
  }

  /**
   * Checks if this exception represents a permission error.
   *
   * @return true if this is a permission error, false otherwise
   */
  public boolean isPermissionError() {
    return category == ErrorCategory.PERMISSION;
  }

  /**
   * Checks if this exception represents a resource limit error.
   *
   * @return true if this is a resource limit error, false otherwise
   */
  public boolean isResourceLimitError() {
    return category == ErrorCategory.RESOURCE_LIMIT;
  }

  /**
   * Checks if this exception represents a component model error.
   *
   * @return true if this is a component error, false otherwise
   */
  public boolean isComponentError() {
    return category == ErrorCategory.COMPONENT;
  }

  /**
   * Checks if this exception represents a configuration error.
   *
   * @return true if this is a configuration error, false otherwise
   */
  public boolean isConfigurationError() {
    return category == ErrorCategory.CONFIGURATION;
  }

  /**
   * Formats the exception message with error details.
   *
   * @param message the base message
   * @param operation the operation
   * @param resource the resource
   * @return the formatted message
   */
  private static String formatMessage(
      final String message, final String operation, final String resource) {
    if (message == null || message.isEmpty()) {
      throw new IllegalArgumentException("Message cannot be null or empty");
    }

    final StringBuilder sb = new StringBuilder(message);

    if (operation != null && !operation.isEmpty()) {
      sb.append(" (operation: ").append(operation).append(")");
    }

    if (resource != null && !resource.isEmpty()) {
      sb.append(" (resource: ").append(resource).append(")");
    }

    return sb.toString();
  }
}