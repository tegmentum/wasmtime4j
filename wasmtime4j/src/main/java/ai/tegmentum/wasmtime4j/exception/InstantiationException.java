package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WebAssembly instance creation fails.
 *
 * <p>This exception is thrown when a WebAssembly instance cannot be created due to instance-level
 * errors such as invalid configuration or resource allocation failures.
 *
 * @since 1.0.0
 */
public class InstantiationException extends WasmException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new instantiation exception with the specified message.
   *
   * @param message the error message describing the instantiation failure
   */
  public InstantiationException(final String message) {
    super(message);
  }

  /**
   * Creates a new instantiation exception with the specified message and cause.
   *
   * @param message the error message describing the instantiation failure
   * @param cause the underlying cause
   */
  public InstantiationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
