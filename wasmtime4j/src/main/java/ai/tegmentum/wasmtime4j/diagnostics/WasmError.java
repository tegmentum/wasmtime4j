package ai.tegmentum.wasmtime4j.diagnostics;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Comprehensive WebAssembly error representation with diagnostics.
 *
 * <p>This interface provides detailed error information including diagnostics, context, and
 * recovery suggestions for WebAssembly runtime errors.
 *
 * @since 1.0.0
 */
public interface WasmError {

  /** Error severity levels for classification and handling. */
  enum Severity {
    /** Informational messages */
    INFO,
    /** Warning conditions that don't prevent execution */
    WARNING,
    /** Error conditions that prevent normal operation */
    ERROR,
    /** Critical conditions that require immediate attention */
    CRITICAL,
    /** Fatal conditions that cause termination */
    FATAL
  }

  /** Error categories for systematic classification. */
  enum Category {
    /** Compilation and validation errors */
    COMPILATION,
    /** Runtime execution errors */
    RUNTIME,
    /** WebAssembly trap conditions */
    TRAP,
    /** Engine configuration errors */
    ENGINE,
    /** Module loading errors */
    MODULE,
    /** Memory access errors */
    MEMORY,
    /** Function call errors */
    FUNCTION,
    /** WASI interface errors */
    WASI,
    /** Resource management errors */
    RESOURCE,
    /** Security policy violations */
    SECURITY,
    /** Performance threshold violations */
    PERFORMANCE,
    /** Network and I/O errors */
    IO,
    /** Unknown or unclassified errors */
    UNKNOWN
  }

  /**
   * Gets the unique error identifier.
   *
   * @return the error ID
   */
  String getErrorId();

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  String getMessage();

  /**
   * Gets the error category.
   *
   * @return the error category
   */
  Category getCategory();

  /**
   * Gets the error severity level.
   *
   * @return the severity level
   */
  Severity getSeverity();

  /**
   * Gets the timestamp when the error occurred.
   *
   * @return the error timestamp
   */
  Instant getTimestamp();

  /**
   * Gets the error context information.
   *
   * @return the error context
   */
  ErrorContext getContext();

  /**
   * Gets the error diagnostics.
   *
   * @return the error diagnostics
   */
  ErrorDiagnostics getDiagnostics();

  /**
   * Gets the WebAssembly stack trace if available.
   *
   * @return the stack trace, or empty if not available
   */
  Optional<WasmStackTrace> getStackTrace();

  /**
   * Gets recovery suggestions for this error.
   *
   * @return list of recovery suggestions
   */
  List<String> getRecoverySuggestions();

  /**
   * Gets additional properties associated with this error.
   *
   * @return map of additional properties
   */
  Map<String, Object> getProperties();

  /**
   * Gets the underlying cause if available.
   *
   * @return the underlying cause, or empty if not available
   */
  Optional<Throwable> getCause();

  /**
   * Checks if this error is recoverable.
   *
   * @return true if the error is recoverable
   */
  boolean isRecoverable();

  /**
   * Checks if this error indicates a security violation.
   *
   * @return true if the error indicates a security violation
   */
  boolean isSecurityViolation();

  /**
   * Checks if this error should be retried.
   *
   * @return true if the error should be retried
   */
  boolean shouldRetry();

  /**
   * Gets the recommended retry delay in milliseconds.
   *
   * @return the retry delay, or empty if retry is not recommended
   */
  Optional<Long> getRetryDelayMs();

  /**
   * Creates a builder for constructing WasmError instances.
   *
   * @return a new error builder
   */
  static WasmErrorBuilder builder() {
    return new WasmErrorBuilder();
  }
}
