package ai.tegmentum.wasmtime4j.diagnostics;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specialized logger for WebAssembly error handling and diagnostics.
 *
 * <p>This class provides enhanced logging capabilities specifically designed for WebAssembly
 * operations, including error categorization, performance metrics, and diagnostic context
 * preservation.
 *
 * <p>Usage example:
 * <pre>{@code
 * ErrorLogger logger = ErrorLogger.getLogger("ModuleCompilation");
 * logger.logCompilationError(exception, moduleBytes.length, compilationStartTime);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ErrorLogger {

  private static final Map<String, ErrorLogger> LOGGERS = new ConcurrentHashMap<>();

  private final Logger julLogger;
  private final String category;
  private final ErrorMetrics metrics;

  private ErrorLogger(final String category) {
    this.category = category;
    this.julLogger = Logger.getLogger("ai.tegmentum.wasmtime4j.error." + category);
    this.metrics = new ErrorMetrics();
  }

  /**
   * Gets or creates an error logger for the specified category.
   *
   * @param category the error category (e.g., "Compilation", "Runtime", "WASI")
   * @return the error logger instance
   * @throws IllegalArgumentException if category is null or empty
   */
  public static ErrorLogger getLogger(final String category) {
    if (category == null || category.trim().isEmpty()) {
      throw new IllegalArgumentException("Category cannot be null or empty");
    }
    return LOGGERS.computeIfAbsent(category, ErrorLogger::new);
  }

  /**
   * Logs a compilation error with performance metrics.
   *
   * @param exception the compilation exception
   * @param moduleSize the size of the WebAssembly module in bytes
   * @param startTime the compilation start time
   */
  public void logCompilationError(final WasmException exception, final long moduleSize,
      final Instant startTime) {
    final long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
    metrics.recordCompilationError(duration, moduleSize);

    julLogger.log(Level.SEVERE,
        "WebAssembly compilation failed: module_size={0} bytes, duration={1}ms, error={2}",
        new Object[]{moduleSize, duration, exception.getMessage()});

    if (julLogger.isLoggable(Level.FINE)) {
      julLogger.log(Level.FINE, "Compilation error details", exception);
    }
  }

  /**
   * Logs a runtime error with execution context.
   *
   * @param exception the runtime exception
   * @param functionName the function being executed (if known)
   * @param stackDepth the current stack depth
   */
  public void logRuntimeError(final WasmException exception, final String functionName,
      final int stackDepth) {
    metrics.recordRuntimeError();

    final String func = (functionName != null) ? functionName : "unknown";
    julLogger.log(Level.SEVERE,
        "WebAssembly runtime error: function={0}, stack_depth={1}, error={2}",
        new Object[]{func, stackDepth, exception.getMessage()});

    if (julLogger.isLoggable(Level.FINE)) {
      julLogger.log(Level.FINE, "Runtime error details", exception);
    }
  }

  /**
   * Logs a validation error with module context.
   *
   * @param exception the validation exception
   * @param moduleName the module name (if available)
   * @param section the WebAssembly section being validated
   */
  public void logValidationError(final WasmException exception, final String moduleName,
      final String section) {
    metrics.recordValidationError();

    final String module = (moduleName != null) ? moduleName : "unnamed";
    final String sect = (section != null) ? section : "unknown";
    julLogger.log(Level.WARNING,
        "WebAssembly validation failed: module={0}, section={1}, error={2}",
        new Object[]{module, sect, exception.getMessage()});

    if (julLogger.isLoggable(Level.FINE)) {
      julLogger.log(Level.FINE, "Validation error details", exception);
    }
  }

  /**
   * Logs a resource management error.
   *
   * @param exception the resource exception
   * @param resourceType the type of resource (e.g., "Memory", "Handle")
   * @param resourceId the resource identifier
   */
  public void logResourceError(final WasmException exception, final String resourceType,
      final String resourceId) {
    metrics.recordResourceError();

    julLogger.log(Level.SEVERE,
        "WebAssembly resource error: type={0}, id={1}, error={2}",
        new Object[]{resourceType, resourceId, exception.getMessage()});

    if (julLogger.isLoggable(Level.FINE)) {
      julLogger.log(Level.FINE, "Resource error details", exception);
    }
  }

  /**
   * Logs a security violation.
   *
   * @param exception the security exception
   * @param context the security context
   * @param attemptedAction the action that was attempted
   */
  public void logSecurityViolation(final WasmException exception, final String context,
      final String attemptedAction) {
    metrics.recordSecurityError();

    julLogger.log(Level.SEVERE,
        "WebAssembly security violation: context={0}, action={1}, error={2}",
        new Object[]{context, attemptedAction, exception.getMessage()});

    if (julLogger.isLoggable(Level.FINE)) {
      julLogger.log(Level.FINE, "Security violation details", exception);
    }
  }

  /**
   * Logs performance diagnostics information.
   *
   * @param operation the operation being measured
   * @param duration the operation duration in milliseconds
   * @param metadata additional metadata about the operation
   */
  public void logPerformanceDiagnostics(final String operation, final long duration,
      final Map<String, Object> metadata) {
    if (!julLogger.isLoggable(Level.INFO)) {
      return;
    }

    final StringBuilder message = new StringBuilder();
    message.append("Performance: operation=").append(operation)
        .append(", duration=").append(duration).append("ms");

    if (metadata != null && !metadata.isEmpty()) {
      metadata.forEach((key, value) ->
          message.append(", ").append(key).append("=").append(value));
    }

    julLogger.info(message.toString());
  }

  /**
   * Logs error recovery information.
   *
   * @param originalError the original error that occurred
   * @param recoveryStrategy the recovery strategy used
   * @param recoverySuccess whether recovery was successful
   */
  public void logErrorRecovery(final WasmException originalError, final String recoveryStrategy,
      final boolean recoverySuccess) {
    final Level level = recoverySuccess ? Level.INFO : Level.WARNING;
    final String status = recoverySuccess ? "successful" : "failed";

    julLogger.log(level,
        "Error recovery {0}: strategy={1}, original_error={2}",
        new Object[]{status, recoveryStrategy, originalError.getMessage()});
  }

  /**
   * Gets the error metrics for this logger category.
   *
   * @return the error metrics
   */
  public ErrorMetrics getMetrics() {
    return metrics;
  }

  /**
   * Gets the logger category.
   *
   * @return the category
   */
  public String getCategory() {
    return category;
  }

  /**
   * Checks if the underlying JUL logger would log a record at the specified level.
   *
   * @param level the logging level
   * @return true if loggable at the specified level
   */
  public boolean isLoggable(final Level level) {
    return julLogger.isLoggable(level);
  }

  /**
   * Sets the logging level for this error logger.
   *
   * @param level the logging level
   */
  public void setLevel(final Level level) {
    julLogger.setLevel(level);
  }
}