package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when a WebAssembly memory access operation fails.
 *
 * <p>This exception is thrown for out-of-bounds memory reads/writes, misaligned accesses, or
 * attempts to access memory that has been freed or is otherwise unavailable.
 *
 * @since 1.1.0
 */
public class MemoryAccessException extends WasmRuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new memory access exception with the specified message.
   *
   * @param message the error message describing the access failure
   */
  public MemoryAccessException(final String message) {
    super(message);
  }

  /**
   * Creates a new memory access exception with the specified message and cause.
   *
   * @param message the error message describing the access failure
   * @param cause the underlying cause
   */
  public MemoryAccessException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
