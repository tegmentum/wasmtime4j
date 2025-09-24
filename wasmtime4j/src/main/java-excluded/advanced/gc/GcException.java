package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.exception.RuntimeException;

/**
 * Exception thrown by WebAssembly GC operations.
 *
 * <p>Indicates errors in garbage collection operations such as invalid type casts, field access
 * violations, or heap management failures.
 *
 * @since 1.0.0
 */
public class GcException extends RuntimeException {

  /**
   * Creates a new GC exception with the specified message.
   *
   * @param message the error message
   */
  public GcException(final String message) {
    super(message);
  }

  /**
   * Creates a new GC exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public GcException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new GC exception with the specified cause.
   *
   * @param cause the underlying cause
   */
  public GcException(final Throwable cause) {
    super(cause);
  }
}
