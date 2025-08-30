package ai.tegmentum.wasmtime4j.panama.wasi.exception;

/**
 * Exception thrown when WASI operations are denied due to insufficient permissions in Panama FFI
 * implementation.
 *
 * <p>This exception is thrown when a WASI operation is attempted but the current security context
 * does not have sufficient permissions to perform the operation.
 *
 * @since 1.0.0
 */
public class WasiPermissionException extends WasiFileSystemException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new WASI permission exception.
   *
   * @param message the error message
   */
  public WasiPermissionException(final String message) {
    super(message, "EACCES");
  }

  /**
   * Creates a new WASI permission exception with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasiPermissionException(final String message, final Throwable cause) {
    super(message, "EACCES", cause);
  }

  @Override
  public String toString() {
    return String.format("WasiPermissionException: %s", getMessage());
  }
}
