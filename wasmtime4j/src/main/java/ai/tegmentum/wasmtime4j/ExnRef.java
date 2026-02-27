package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.gc.ExnType;
import ai.tegmentum.wasmtime4j.memory.Tag;
import java.util.List;

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
   * Gets the value of a specific field in this exception's payload.
   *
   * @param store the store context
   * @param index the zero-based field index
   * @return the field value as a WasmValue
   * @throws WasmException if field retrieval fails or index is out of bounds
   * @throws IllegalArgumentException if store is null or index is negative
   */
  WasmValue field(Store store, int index) throws WasmException;

  /**
   * Gets all field values in this exception's payload.
   *
   * @param store the store context
   * @return an unmodifiable list of all field values
   * @throws WasmException if field retrieval fails
   * @throws IllegalArgumentException if store is null
   */
  List<WasmValue> fields(Store store) throws WasmException;

  /**
   * Gets the exception type of this reference.
   *
   * <p>The returned type describes the structure of this exception's payload fields.
   *
   * @param store the store context
   * @return the ExnType describing this exception
   * @throws WasmException if type retrieval fails
   * @throws IllegalArgumentException if store is null
   */
  ExnType ty(Store store) throws WasmException;

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
