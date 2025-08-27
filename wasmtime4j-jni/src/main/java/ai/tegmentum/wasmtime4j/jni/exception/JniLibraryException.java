package ai.tegmentum.wasmtime4j.jni.exception;

/**
 * Exception thrown when there are issues with native library loading or initialization.
 *
 * <p>This exception is thrown when:
 * <ul>
 *   <li>Native library cannot be found or loaded</li>
 *   <li>Native library is incompatible with the current platform</li>
 *   <li>Native library fails to initialize properly</li>
 *   <li>Required native methods are missing from the library</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniLibraryException extends JniException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new JNI library exception with the specified message.
   *
   * @param message the error message
   */
  public JniLibraryException(final String message) {
    super(message);
  }

  /**
   * Creates a new JNI library exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public JniLibraryException(final String message, final Throwable cause) {
    super(message, cause);
  }
}