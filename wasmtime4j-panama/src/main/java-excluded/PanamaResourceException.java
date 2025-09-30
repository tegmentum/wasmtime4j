package ai.tegmentum.wasmtime4j.panama.exception;

/**
 * Exception thrown when there are issues with native resource management in Panama FFI.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>Native resource allocation fails
 *   <li>Native resource deallocation fails
 *   <li>Native resource is accessed after being freed
 *   <li>Native resource limits are exceeded
 *   <li>Memory management operations fail
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaResourceException extends PanamaException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new Panama resource exception with the specified message.
   *
   * @param message the error message
   */
  public PanamaResourceException(final String message) {
    super(message);
  }

  /**
   * Creates a new Panama resource exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public PanamaResourceException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new Panama resource exception with the specified message and native error code.
   *
   * @param message the error message
   * @param nativeErrorCode the native error code
   */
  public PanamaResourceException(final String message, final int nativeErrorCode) {
    super(message, nativeErrorCode);
  }

  /**
   * Creates a new Panama resource exception with the specified message, cause, and native error code.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param nativeErrorCode the native error code
   */
  public PanamaResourceException(final String message, final Throwable cause, final int nativeErrorCode) {
    super(message, cause, nativeErrorCode);
  }
}