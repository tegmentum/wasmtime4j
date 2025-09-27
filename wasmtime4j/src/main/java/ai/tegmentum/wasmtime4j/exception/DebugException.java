package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception for WebAssembly debugging-related errors.
 *
 * <p>This exception is thrown when debugging operations fail, including
 * breakpoint management, step execution, variable inspection, and debug
 * session management.
 *
 * @since 1.0.0
 */
public class DebugException extends WasmException {

  private final DebugErrorType errorType;
  private final String debugContext;

  /**
   * Creates a new debug exception.
   *
   * @param message the error message
   */
  public DebugException(final String message) {
    this(message, null, DebugErrorType.UNKNOWN, null);
  }

  /**
   * Creates a new debug exception with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public DebugException(final String message, final Throwable cause) {
    this(message, cause, DebugErrorType.UNKNOWN, null);
  }

  /**
   * Creates a new debug exception with an error type.
   *
   * @param message the error message
   * @param errorType the debug error type
   */
  public DebugException(final String message, final DebugErrorType errorType) {
    this(message, null, errorType, null);
  }

  /**
   * Creates a new debug exception with full details.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param errorType the debug error type
   * @param debugContext additional debug context information
   */
  public DebugException(final String message,
                        final Throwable cause,
                        final DebugErrorType errorType,
                        final String debugContext) {
    super(message, cause);
    this.errorType = errorType != null ? errorType : DebugErrorType.UNKNOWN;
    this.debugContext = debugContext;
  }

  /**
   * Gets the debug error type.
   *
   * @return the error type
   */
  public DebugErrorType getErrorType() {
    return errorType;
  }

  /**
   * Gets the debug context information.
   *
   * @return the debug context, or null if not available
   */
  public String getDebugContext() {
    return debugContext;
  }

  /**
   * Checks if this exception has debug context information.
   *
   * @return true if debug context is available, false otherwise
   */
  public boolean hasDebugContext() {
    return debugContext != null && !debugContext.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[").append(errorType).append("]");
    if (getMessage() != null) {
      sb.append(": ").append(getMessage());
    }
    if (hasDebugContext()) {
      sb.append(" (context: ").append(debugContext).append(")");
    }
    return sb.toString();
  }

  /**
   * Enumeration of debug error types.
   */
  public enum DebugErrorType {
    /** Unknown or unspecified debug error */
    UNKNOWN,

    /** Breakpoint operation failed */
    BREAKPOINT_ERROR,

    /** Step execution failed */
    STEP_ERROR,

    /** Variable inspection failed */
    VARIABLE_INSPECTION_ERROR,

    /** Stack frame access failed */
    STACK_FRAME_ERROR,

    /** Debug session management error */
    SESSION_ERROR,

    /** Source mapping error */
    SOURCE_MAPPING_ERROR,

    /** Symbol lookup error */
    SYMBOL_ERROR,

    /** Watch expression error */
    WATCH_ERROR,

    /** Debug target connection error */
    CONNECTION_ERROR,

    /** Invalid debug state */
    INVALID_STATE,

    /** Debug protocol error */
    PROTOCOL_ERROR,

    /** Debugging not supported */
    NOT_SUPPORTED,

    /** Debug timeout */
    TIMEOUT
  }
}