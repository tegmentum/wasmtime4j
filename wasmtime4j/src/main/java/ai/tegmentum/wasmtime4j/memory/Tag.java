package ai.tegmentum.wasmtime4j.memory;

import ai.tegmentum.wasmtime4j.exception.WasmException;

import ai.tegmentum.wasmtime4j.Store;

import ai.tegmentum.wasmtime4j.WasmValueType;

import ai.tegmentum.wasmtime4j.type.TagType;

import ai.tegmentum.wasmtime4j.type.FunctionType;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

/**
 * Represents a WebAssembly exception tag.
 *
 * <p>Tags are used in the WebAssembly exception handling proposal to define types of exceptions
 * that can be thrown and caught. Each tag has an associated {@link TagType} that describes the
 * payload values that can be attached to exceptions of this type.
 *
 * <p>Tags are bound to a specific {@link Store} and can be used to:
 *
 * <ul>
 *   <li>Create exception references via {@link ExnRef}
 *   <li>Match against exceptions in catch blocks
 *   <li>Import/export exception types between modules
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a tag for exceptions carrying an error code
 * FunctionType funcType = FunctionType.create(List.of(WasmValueType.I32), List.of());
 * TagType tagType = TagType.create(funcType);
 * Tag errorTag = Tag.create(store, tagType);
 *
 * // Later, the tag can be used to throw or catch exceptions
 * }</pre>
 *
 * @since 1.0.0
 */
public interface Tag {

  /**
   * Creates a new tag with the specified type in the given store.
   *
   * @param store the store to create the tag in
   * @param tagType the type descriptor for this tag
   * @return a new Tag instance
   * @throws WasmException if tag creation fails
   * @throws IllegalArgumentException if store or tagType is null
   */
  static Tag create(final Store store, final TagType tagType) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (tagType == null) {
      throw new IllegalArgumentException("tagType cannot be null");
    }
    return WasmRuntimeFactory.create().createTag(store, tagType);
  }

  /**
   * Returns the type of this tag.
   *
   * @param store the store that owns this tag
   * @return the TagType descriptor
   * @throws WasmException if retrieval fails
   * @throws IllegalArgumentException if store is null
   */
  TagType getType(Store store) throws WasmException;

  /**
   * Determines reference equality between two tags.
   *
   * <p>Two tags are equal if they refer to the same underlying tag definition. This is reference
   * equality, not structural equality.
   *
   * @param other the other tag to compare
   * @param store the store context for comparison
   * @return true if the tags are the same reference
   * @throws WasmException if comparison fails
   */
  boolean equals(Tag other, Store store) throws WasmException;

  /**
   * Gets the native handle for this tag.
   *
   * <p>This method is intended for internal use by the runtime implementations.
   *
   * @return the native handle
   */
  long getNativeHandle();
}
