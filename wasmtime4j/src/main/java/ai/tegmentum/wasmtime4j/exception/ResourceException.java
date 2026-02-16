package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown for WebAssembly resource management errors.
 *
 * <p>This exception is thrown when resource operations fail, such as resource allocation,
 * deallocation, or access errors in the WebAssembly component model.
 *
 * @since 1.0.0
 */
public class ResourceException extends WasmException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new resource exception with the specified message.
   *
   * @param message the error message describing the resource failure
   */
  public ResourceException(final String message) {
    super(message);
  }

  /**
   * Creates a new resource exception with the specified message and cause.
   *
   * @param message the error message describing the resource failure
   * @param cause the underlying cause
   */
  public ResourceException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
