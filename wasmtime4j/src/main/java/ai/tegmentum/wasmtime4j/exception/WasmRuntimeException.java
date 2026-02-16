package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown for WebAssembly runtime errors.
 *
 * <p>This exception is thrown for general runtime failures including memory operations, function
 * calls, type mismatches, component errors, interface errors, engine configuration errors, store
 * errors, concurrency errors, and internal errors.
 *
 * @since 1.0.0
 */
public class WasmRuntimeException extends WasmException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new runtime exception with the specified message.
   *
   * @param message the error message describing the runtime failure
   */
  public WasmRuntimeException(final String message) {
    super(message);
  }

  /**
   * Creates a new runtime exception with the specified message and cause.
   *
   * @param message the error message describing the runtime failure
   * @param cause the underlying cause
   */
  public WasmRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
