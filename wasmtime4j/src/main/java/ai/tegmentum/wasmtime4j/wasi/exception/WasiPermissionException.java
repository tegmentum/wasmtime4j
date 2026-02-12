package ai.tegmentum.wasmtime4j.wasi.exception;

/**
 * Exception thrown when WASI operations are denied due to insufficient permissions.
 *
 * <p>This is an unchecked exception thrown when a WASI operation is attempted but the current
 * security context does not have sufficient permissions to perform the operation, including:
 *
 * <ul>
 *   <li>Sandbox boundary violations
 *   <li>Unauthorized file system access
 *   <li>Environment variable access denied
 *   <li>Dangerous operation attempts
 *   <li>Path traversal attack prevention
 * </ul>
 *
 * <p>This exception extends {@link RuntimeException} to match the behavior of both JNI and Panama
 * module-specific security exception hierarchies, which are unchecked.
 *
 * @since 1.0.0
 */
public class WasiPermissionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new WASI permission exception.
   *
   * @param message the error message
   */
  public WasiPermissionException(final String message) {
    super(message);
  }

  /**
   * Creates a new WASI permission exception with a cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasiPermissionException(final String message, final Throwable cause) {
    super(message, cause);
  }

  @Override
  public String toString() {
    return String.format("WasiPermissionException: %s", getMessage());
  }
}
