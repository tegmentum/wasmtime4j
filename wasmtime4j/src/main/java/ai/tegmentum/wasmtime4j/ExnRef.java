package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.gc.ExnType;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.type.HeapType;
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

  /**
   * Creates a new ExnRef from a tag and field values.
   *
   * <p>The field values must match the tag's type signature. Type codes are: 0=I32, 1=I64, 2=F32,
   * 3=F64.
   *
   * @param store the store context
   * @param tag the exception tag
   * @param fields the field values for the exception payload
   * @return a new ExnRef instance
   * @throws WasmException if creation fails
   * @throws IllegalArgumentException if store, tag, or fields is null
   */
  static ExnRef create(final Store store, final Tag tag, final WasmValue... fields)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (tag == null) {
      throw new IllegalArgumentException("tag cannot be null");
    }
    if (fields == null) {
      throw new IllegalArgumentException("fields cannot be null");
    }
    return WasmRuntimeFactory.create().createExnRef(store, tag, fields);
  }

  /**
   * Creates an ExnRef from a raw integer representation.
   *
   * <p>This is a low-level API used for typed function calls and ValRaw operations. The raw value
   * is a GC heap index. A raw value of 0 indicates a null reference and returns null.
   *
   * @param store the store context
   * @param raw the raw u32 representation
   * @return a new ExnRef instance, or null if raw is 0
   * @throws WasmException if creation fails
   * @throws IllegalArgumentException if store is null
   */
  static ExnRef fromRaw(final Store store, final long raw) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    return WasmRuntimeFactory.create().exnRefFromRaw(store, raw);
  }

  /**
   * Converts this ExnRef to its raw integer representation.
   *
   * <p>This is a low-level API used for typed function calls and ValRaw operations.
   *
   * @param store the store context
   * @return the raw u32 representation as a long
   * @throws WasmException if conversion fails
   * @throws IllegalArgumentException if store is null
   */
  long toRaw(Store store) throws WasmException;

  /**
   * Checks if this ExnRef matches the given heap type.
   *
   * <p>This performs a subtype check against the specified heap type.
   *
   * @param store the store context
   * @param heapType the heap type to check against
   * @return true if this ExnRef matches the given type
   * @throws WasmException if the check fails
   * @throws IllegalArgumentException if store or heapType is null
   */
  boolean matchesTy(Store store, HeapType heapType) throws WasmException;
}
