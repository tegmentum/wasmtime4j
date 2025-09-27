package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown during WebAssembly runtime operations.
 *
 * <p>This exception is thrown when an error occurs during the execution
 * of WebAssembly code or when runtime-specific operations fail.
 *
 * @since 1.0.0
 */
public class WasmRuntimeException extends WasmException {

  /**
   * Constructs a new runtime exception with the specified detail message.
   *
   * @param message the detail message
   */
  public WasmRuntimeException(final String message) {
    super(message);
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public WasmRuntimeException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new runtime exception with the specified cause.
   *
   * @param cause the cause
   */
  public WasmRuntimeException(final Throwable cause) {
    super(cause);
  }
}