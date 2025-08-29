package ai.tegmentum.wasmtime4j.panama.exception;

/**
 * Base exception for Panama FFI implementation errors.
 *
 * <p>This is the root exception class for all Panama-specific errors in the WebAssembly runtime.
 * It provides a common base for exception handling across the Panama implementation.
 *
 * @since 1.0.0
 */
public class PanamaException extends Exception {

  /**
   * Creates a new Panama exception with the specified message.
   *
   * @param message the detail message
   */
  public PanamaException(final String message) {
    super(message);
  }

  /**
   * Creates a new Panama exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  public PanamaException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new Panama exception with the specified cause.
   *
   * @param cause the underlying cause
   */
  public PanamaException(final Throwable cause) {
    super(cause);
  }
}