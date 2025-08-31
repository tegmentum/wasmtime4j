package ai.tegmentum.wasmtime4j.panama.wasi.exception;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;

/**
 * Exception thrown when WASI operations fail.
 *
 * <p>This exception is thrown when WASI (WebAssembly System Interface) operations encounter errors
 * during execution. It provides information about the specific operation that failed and the
 * underlying error condition.
 *
 * <p>Common scenarios that trigger this exception:
 *
 * <ul>
 *   <li>Invalid parameters passed to WASI operations
 *   <li>System resource unavailable (insufficient memory, file handles, etc.)
 *   <li>Permission denied for requested operation
 *   <li>I/O errors during file or network operations
 *   <li>Timeout or interrupt conditions
 * </ul>
 *
 * @since 1.0.0
 */
public class WasiException extends PanamaException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new WASI exception with the specified detail message.
   *
   * @param message the detail message explaining the error condition
   */
  public WasiException(final String message) {
    super(message);
  }

  /**
   * Constructs a new WASI exception with the specified detail message and cause.
   *
   * @param message the detail message explaining the error condition
   * @param cause the underlying cause of this exception
   */
  public WasiException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new WASI exception with the specified cause.
   *
   * @param cause the underlying cause of this exception
   */
  public WasiException(final Throwable cause) {
    super(cause);
  }
}
