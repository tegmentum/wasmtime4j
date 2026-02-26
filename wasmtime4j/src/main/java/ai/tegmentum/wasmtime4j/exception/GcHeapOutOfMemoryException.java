package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when the GC heap runs out of memory.
 *
 * <p>This exception is thrown when a WebAssembly GC allocation (struct, array, etc.) fails because
 * the GC heap has been exhausted. This can happen when the heap is full and the collector cannot
 * reclaim enough space.
 *
 * @since 1.1.0
 */
public class GcHeapOutOfMemoryException extends WasmRuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new GC heap out of memory exception with the specified message.
   *
   * @param message the error message describing the allocation failure
   */
  public GcHeapOutOfMemoryException(final String message) {
    super(message);
  }

  /**
   * Creates a new GC heap out of memory exception with the specified message and cause.
   *
   * @param message the error message describing the allocation failure
   * @param cause the underlying cause
   */
  public GcHeapOutOfMemoryException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
