package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.memory.Tag;

/**
 * Represents a WebAssembly exception reference.
 *
 * <p>ExnRef is a rooted reference to an exception that can be thrown or caught within WebAssembly
 * execution. Exception references carry:
 *
 * <ul>
 *   <li>A {@link Tag} identifying the exception type
 *   <li>Payload values matching the tag's type signature
 * </ul>
 *
 * <p>Exception references are part of the WebAssembly exception handling proposal and enable
 * structured error handling between WebAssembly and host code.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Check if store has a pending exception
 * if (store.hasPendingException()) {
 *     ExnRef exn = store.takePendingException();
 *     Tag tag = exn.getTag(store);
 *     // Handle based on tag type
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ExnRef {

  /**
   * Gets the tag associated with this exception.
   *
   * <p>The tag identifies the type of this exception and determines what payload values it carries.
   *
   * @param store the store context
   * @return the Tag that identifies the exception type
   * @throws WasmException if retrieval fails
   * @throws IllegalArgumentException if store is null
   */
  Tag getTag(Store store) throws WasmException;

  /**
   * Gets the native handle for this exception reference.
   *
   * <p>This method is intended for internal use by the runtime implementations.
   *
   * @return the native handle
   */
  long getNativeHandle();

  /**
   * Checks if this exception reference is still valid.
   *
   * <p>Exception references can become invalid if their owning store is closed or if they are
   * explicitly invalidated.
   *
   * @return true if this reference is valid and can be used
   */
  boolean isValid();
}
