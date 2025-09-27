package ai.tegmentum.wasmtime4j.exception;

/**
 * Base exception for Wasmtime-specific errors.
 *
 * <p>This exception represents errors that originate from the Wasmtime
 * WebAssembly runtime, including compilation failures, execution errors,
 * and resource management issues.
 *
 * @since 1.0.0
 */
public class WasmtimeException extends WasmException {

  private final ErrorCode errorCode;
  private final String nativeStackTrace;

  /**
   * Creates a new Wasmtime exception.
   *
   * @param message the error message
   */
  public WasmtimeException(final String message) {
    this(message, null, ErrorCode.UNKNOWN, null);
  }

  /**
   * Creates a new Wasmtime exception with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasmtimeException(final String message, final Throwable cause) {
    this(message, cause, ErrorCode.UNKNOWN, null);
  }

  /**
   * Creates a new Wasmtime exception with an error code.
   *
   * @param message the error message
   * @param errorCode the Wasmtime error code
   */
  public WasmtimeException(final String message, final ErrorCode errorCode) {
    this(message, null, errorCode, null);
  }

  /**
   * Creates a new Wasmtime exception with full details.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param errorCode the Wasmtime error code
   * @param nativeStackTrace the native stack trace from Wasmtime
   */
  public WasmtimeException(final String message,
                           final Throwable cause,
                           final ErrorCode errorCode,
                           final String nativeStackTrace) {
    super(message, cause);
    this.errorCode = errorCode != null ? errorCode : ErrorCode.UNKNOWN;
    this.nativeStackTrace = nativeStackTrace;
  }

  /**
   * Gets the Wasmtime error code.
   *
   * @return the error code
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Gets the native stack trace from Wasmtime, if available.
   *
   * @return the native stack trace, or null if not available
   */
  public String getNativeStackTrace() {
    return nativeStackTrace;
  }

  /**
   * Checks if this exception has a native stack trace.
   *
   * @return true if native stack trace is available, false otherwise
   */
  public boolean hasNativeStackTrace() {
    return nativeStackTrace != null && !nativeStackTrace.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[").append(errorCode).append("]");
    if (getMessage() != null) {
      sb.append(": ").append(getMessage());
    }
    return sb.toString();
  }

  /**
   * Enumeration of Wasmtime error codes.
   */
  public enum ErrorCode {
    /** Unknown or unspecified error */
    UNKNOWN,

    /** Module compilation failed */
    COMPILATION_FAILED,

    /** Module instantiation failed */
    INSTANTIATION_FAILED,

    /** Function call failed */
    FUNCTION_CALL_FAILED,

    /** Memory access violation */
    MEMORY_ACCESS_VIOLATION,

    /** Stack overflow */
    STACK_OVERFLOW,

    /** Trap occurred during execution */
    TRAP,

    /** Resource exhaustion */
    RESOURCE_EXHAUSTED,

    /** Invalid configuration */
    INVALID_CONFIGURATION,

    /** Unsupported operation */
    UNSUPPORTED_OPERATION,

    /** Native library error */
    NATIVE_LIBRARY_ERROR,

    /** Threading error */
    THREADING_ERROR,

    /** WASI error */
    WASI_ERROR,

    /** Validation error */
    VALIDATION_ERROR,

    /** Linking error */
    LINKING_ERROR,

    /** Fuel exhausted */
    FUEL_EXHAUSTED,

    /** Timeout */
    TIMEOUT,

    /** Interrupted */
    INTERRUPTED
  }
}