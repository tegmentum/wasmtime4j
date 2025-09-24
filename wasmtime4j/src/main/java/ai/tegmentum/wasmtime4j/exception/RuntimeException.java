package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown during WebAssembly execution.
 *
 * <p>This exception is thrown when errors occur during WebAssembly function execution, including
 * traps, out-of-fuel conditions, and other runtime errors. For specific trap conditions, consider
 * using {@link TrapException} which provides more detailed trap information.
 *
 * <p>Runtime exceptions provide context about execution failures:
 *
 * <ul>
 *   <li>General runtime error information
 *   <li>Function execution context
 *   <li>Recovery suggestions
 *   <li>Error categorization for handling
 * </ul>
 *
 * @since 1.0.0
 */
public class RuntimeException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** The specific runtime error type. */
  private final RuntimeErrorType errorType;

  /** Function name where error occurred (if available). */
  private final String functionName;

  /** Recovery suggestion for this runtime error. */
  private final String recoverySuggestion;

  /** Enumeration of WebAssembly runtime error types. */
  public enum RuntimeErrorType {
    /** WebAssembly trap occurred during execution. */
    TRAP("WebAssembly trap occurred"),
    /** Function execution failed. */
    FUNCTION_EXECUTION_FAILED("Function execution failed"),
    /** Host function call failed. */
    HOST_FUNCTION_FAILED("Host function call failed"),
    /** Memory access violation. */
    MEMORY_ACCESS_VIOLATION("Memory access violation"),
    /** Stack overflow or underflow. */
    STACK_ERROR("Stack overflow or underflow"),
    /** Execution timeout exceeded. */
    TIMEOUT("Execution timeout exceeded"),
    /** Resource exhaustion during execution. */
    RESOURCE_EXHAUSTED("Resource exhausted during execution"),
    /** Interrupt signal received. */
    INTERRUPTED("Execution was interrupted"),
    /** Unknown runtime error. */
    UNKNOWN("Unknown runtime error");

    private final String description;

    RuntimeErrorType(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this runtime error type.
     *
     * @return the error type description
     */
    public String getDescription() {
      return description;
    }
  }

  /**
   * Creates a new runtime exception with the specified message.
   *
   * @param message the error message describing the runtime failure
   */
  public RuntimeException(final String message) {
    this(RuntimeErrorType.UNKNOWN, message, null, null);
  }

  /**
   * Creates a new runtime exception with the specified message and cause.
   *
   * @param message the error message describing the runtime failure
   * @param cause the underlying cause
   */
  public RuntimeException(final String message, final Throwable cause) {
    this(RuntimeErrorType.UNKNOWN, message, null, cause);
  }

  /**
   * Creates a new runtime exception with the specified error type and message.
   *
   * @param errorType the specific runtime error type
   * @param message the error message describing the runtime failure
   */
  public RuntimeException(final RuntimeErrorType errorType, final String message) {
    this(errorType, message, null, null);
  }

  /**
   * Creates a new runtime exception with detailed error information.
   *
   * @param errorType the specific runtime error type
   * @param message the error message describing the runtime failure
   * @param functionName function where error occurred (may be null)
   * @param cause the underlying cause (may be null)
   */
  public RuntimeException(
      final RuntimeErrorType errorType,
      final String message,
      final String functionName,
      final Throwable cause) {
    super(formatMessage(errorType, message, functionName), cause);
    this.errorType = errorType != null ? errorType : RuntimeErrorType.UNKNOWN;
    this.functionName = functionName;
    this.recoverySuggestion = generateRecoverySuggestion(this.errorType);
  }

  /**
   * Gets the specific runtime error type.
   *
   * @return the runtime error type
   */
  public RuntimeErrorType getErrorType() {
    return errorType;
  }

  /**
   * Gets the function name where the error occurred.
   *
   * @return the function name, or null if not available
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets a recovery suggestion for this runtime error.
   *
   * @return the recovery suggestion
   */
  public String getRecoverySuggestion() {
    return recoverySuggestion;
  }

  /**
   * Checks if this runtime error represents a trap condition.
   *
   * @return true if this is a trap error, false otherwise
   */
  public boolean isTrapError() {
    return errorType == RuntimeErrorType.TRAP;
  }

  /**
   * Checks if this runtime error is related to function execution.
   *
   * @return true if this is a function execution error, false otherwise
   */
  public boolean isFunctionError() {
    return errorType == RuntimeErrorType.FUNCTION_EXECUTION_FAILED
        || errorType == RuntimeErrorType.HOST_FUNCTION_FAILED;
  }

  /**
   * Checks if this runtime error is related to memory issues.
   *
   * @return true if this is a memory error, false otherwise
   */
  public boolean isMemoryError() {
    return errorType == RuntimeErrorType.MEMORY_ACCESS_VIOLATION
        || errorType == RuntimeErrorType.STACK_ERROR;
  }

  /**
   * Checks if this runtime error is related to resource exhaustion.
   *
   * @return true if this is a resource error, false otherwise
   */
  public boolean isResourceError() {
    return errorType == RuntimeErrorType.RESOURCE_EXHAUSTED
        || errorType == RuntimeErrorType.TIMEOUT;
  }

  /**
   * Formats the exception message with runtime error details.
   *
   * @param errorType the runtime error type
   * @param message the base message
   * @param functionName the function name
   * @return the formatted message
   */
  private static String formatMessage(
      final RuntimeErrorType errorType, final String message, final String functionName) {
    if (message == null || message.isEmpty()) {
      throw new IllegalArgumentException("Message cannot be null or empty");
    }

    final StringBuilder sb = new StringBuilder();

    if (errorType != null) {
      sb.append("[").append(errorType.name()).append("] ");
    }

    sb.append(message);

    if (functionName != null && !functionName.isEmpty()) {
      sb.append(" (function: ").append(functionName).append(")");
    }

    return sb.toString();
  }

  /**
   * Generates a recovery suggestion based on the runtime error type.
   *
   * @param errorType the runtime error type
   * @return a recovery suggestion
   */
  private static String generateRecoverySuggestion(final RuntimeErrorType errorType) {
    switch (errorType) {
      case TRAP:
        return "Check WebAssembly code for trap conditions (see TrapException for details)";
      case FUNCTION_EXECUTION_FAILED:
        return "Review function implementation and input parameters";
      case HOST_FUNCTION_FAILED:
        return "Check host function implementation and error handling";
      case MEMORY_ACCESS_VIOLATION:
        return "Verify memory access patterns and bounds checking";
      case STACK_ERROR:
        return "Reduce recursion depth or increase stack limits";
      case TIMEOUT:
        return "Increase execution timeout or optimize code performance";
      case RESOURCE_EXHAUSTED:
        return "Increase resource limits or optimize resource usage";
      case INTERRUPTED:
        return "Handle interruption gracefully or adjust execution context";
      case UNKNOWN:
      default:
        return "Review WebAssembly execution context and error logs";
    }
  }
}
