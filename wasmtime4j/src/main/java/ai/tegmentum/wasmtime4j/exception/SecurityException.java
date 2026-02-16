package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown for WebAssembly security-related errors.
 *
 * <p>This exception is thrown when security constraints are violated, such as unauthorized access to
 * resources or violation of sandboxing policies.
 *
 * @since 1.0.0
 */
public class SecurityException extends WasmException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new security exception with the specified message.
   *
   * @param message the error message describing the security failure
   */
  public SecurityException(final String message) {
    super(message);
  }

  /**
   * Creates a new security exception with the specified message and cause.
   *
   * @param message the error message describing the security failure
   * @param cause the underlying cause
   */
  public SecurityException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
