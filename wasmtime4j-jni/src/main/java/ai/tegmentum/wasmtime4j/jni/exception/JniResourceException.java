package ai.tegmentum.wasmtime4j.jni.exception;

/**
 * Exception thrown when there are issues with native resource management.
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
public final class JniResourceException extends JniException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new JNI resource exception with the specified message.
   *
   * @param message the error message
   */
  public JniResourceException(final String message) {
    super(message);
  }

  /**
   * Creates a new JNI resource exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public JniResourceException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new JNI resource exception with the specified message and native error code.
   *
   * @param message the error message
   * @param nativeErrorCode the native error code
   */
  public JniResourceException(final String message, final int nativeErrorCode) {
    super(message, nativeErrorCode);
  }

  /**
   * Creates a new JNI resource exception with all parameters.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param nativeErrorCode the native error code
   */
  public JniResourceException(
      final String message, final Throwable cause, final int nativeErrorCode) {
    super(message, cause, nativeErrorCode);
  }
}
