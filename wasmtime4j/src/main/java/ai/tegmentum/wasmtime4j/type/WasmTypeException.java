package ai.tegmentum.wasmtime4j.type;

/**
 * Exception thrown when a WebAssembly type error occurs.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>Type mismatch between expected and actual value types
 *   <li>Invalid type conversion attempts
 *   <li>Incompatible function signature calls
 *   <li>Table or memory type mismatches
 *   <li>Global type constraint violations
 * </ul>
 *
 * @since 1.0.0
 */
public class WasmTypeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new WebAssembly type exception with the specified message.
   *
   * @param message the error message
   */
  public WasmTypeException(final String message) {
    super(message);
  }

  /**
   * Creates a new WebAssembly type exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasmTypeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
