/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provides access to the Component Model resource table for managing host resources.
 *
 * <p>A resource table is a mapping from integer indices to host-side objects. When a component
 * creates a resource via a host constructor, the host pushes the underlying object into the
 * resource table and returns the index as the resource handle. When the component calls methods on
 * the resource, the host looks up the object by index.
 *
 * <p>This interface corresponds to Wasmtime's {@code ResourceTable} type, which is stored
 * per-{@link ai.tegmentum.wasmtime4j.Store Store} in the component store data.
 *
 * <p>Implementations of this interface are not expected to be thread-safe. Access should be
 * coordinated through the store's locking mechanisms.
 *
 * @since 1.0.0
 */
public interface ResourceTable {

  /**
   * Pushes a new entry into the resource table and returns the resource handle index.
   *
   * @param entry the host object to store
   * @return the resource handle index that can be used to retrieve or remove the entry
   * @throws WasmException if the entry cannot be pushed (e.g., table is full)
   * @throws IllegalArgumentException if entry is null
   */
  int push(Object entry) throws WasmException;

  /**
   * Gets a resource entry by its handle index.
   *
   * @param <T> the expected type of the resource entry
   * @param index the resource handle index
   * @param clazz the expected class of the entry
   * @return an Optional containing the entry if found, or empty if the index is invalid
   * @throws ClassCastException if the entry is not of the expected type
   * @throws IllegalArgumentException if clazz is null
   */
  <T> Optional<T> get(int index, Class<T> clazz);

  /**
   * Removes a resource entry from the table and returns it.
   *
   * <p>After removal, the index is no longer valid and may be reused for future entries. If the
   * entry has outstanding child resources (created via {@link #pushChild}), the deletion will fail
   * with a {@link ai.tegmentum.wasmtime4j.exception.ResourceTableException} to prevent orphaned
   * children.
   *
   * @param <T> the expected type of the resource entry
   * @param index the resource handle index
   * @param clazz the expected class of the entry
   * @return an Optional containing the removed entry, or empty if the index was invalid
   * @throws WasmException if the entry has outstanding children
   * @throws ClassCastException if the entry is not of the expected type
   * @throws IllegalArgumentException if clazz is null
   */
  <T> Optional<T> delete(int index, Class<T> clazz) throws WasmException;

  /**
   * Checks if a resource entry exists at the given index.
   *
   * @param index the resource handle index
   * @return true if an entry exists at the index
   */
  boolean contains(int index);

  /**
   * Gets the number of entries currently in the resource table.
   *
   * @return the number of entries
   */
  int size();

  /**
   * Checks if the resource table is empty.
   *
   * @return true if the table contains no entries
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Gets the maximum capacity of the resource table.
   *
   * <p>The default maximum capacity is {@link Integer#MAX_VALUE}.
   *
   * @return the maximum number of entries the table can hold
   */
  int maxCapacity();

  /**
   * Sets the maximum capacity of the resource table.
   *
   * <p>If the current size exceeds the new capacity, existing entries are not removed, but new
   * entries will be rejected until the size drops below the limit.
   *
   * @param maxCapacity the maximum number of entries
   * @throws IllegalArgumentException if maxCapacity is less than 1
   */
  void setMaxCapacity(int maxCapacity);

  /**
   * Pushes a new child entry into the resource table under the given parent.
   *
   * <p>The child entry is associated with the parent so that the parent cannot be deleted while it
   * has outstanding children. This mirrors Wasmtime's {@code ResourceTable::push_child} method.
   *
   * @param entry the host object to store as a child resource
   * @param parentHandle the handle of the parent resource
   * @return the resource handle index for the new child entry
   * @throws WasmException if the parent handle is invalid or the table is full
   * @throws IllegalArgumentException if entry is null
   */
  int pushChild(Object entry, int parentHandle) throws WasmException;

  /**
   * Returns the handles of all child resources associated with the given parent.
   *
   * <p>This mirrors Wasmtime's {@code ResourceTable::iter_children} method.
   *
   * @param parentHandle the handle of the parent resource
   * @return a list of child resource handles (empty if no children or parent not found)
   * @throws WasmException if the parent handle is invalid
   */
  List<Integer> iterChildren(int parentHandle) throws WasmException;

  /**
   * Adds an existing resource as a child of an existing parent resource.
   *
   * <p>This allows dynamic reparenting of entries that were originally created without a parent
   * relationship. After this call, the parent cannot be deleted while the child exists.
   *
   * @param childHandle the handle of the child resource
   * @param parentHandle the handle of the parent resource
   * @throws WasmException if either handle is invalid or the child already has a parent
   */
  void addChild(int childHandle, int parentHandle) throws WasmException;

  /**
   * Removes a parent-child relationship without deleting either resource.
   *
   * <p>After this call, the child is no longer associated with the parent, and the parent can be
   * deleted even if the child still exists.
   *
   * @param childHandle the handle of the child resource
   * @param parentHandle the handle of the parent resource
   * @throws WasmException if either handle is invalid or the relationship does not exist
   */
  void removeChild(int childHandle, int parentHandle) throws WasmException;

  /**
   * Creates a new ResourceTable with the specified initial capacity hint.
   *
   * <p>The capacity is used as a hint for the underlying data structure's initial allocation. The
   * table will still grow dynamically as needed. This corresponds to Wasmtime's {@code
   * ResourceTable::with_capacity()}.
   *
   * @param capacity the initial capacity hint
   * @return a new ResourceTable with the specified initial capacity
   * @throws IllegalArgumentException if capacity is negative
   */
  static ResourceTable withCapacity(final int capacity) {
    if (capacity < 0) {
      throw new IllegalArgumentException("capacity cannot be negative, got: " + capacity);
    }
    return new DefaultResourceTable(capacity);
  }

  /**
   * Returns the set of all currently active resource handles in this table.
   *
   * <p>The returned set is a snapshot; modifications to the table after this call are not reflected
   * in the returned set.
   *
   * @return an unmodifiable set of active resource handle indices
   */
  Set<Integer> handles();

  /**
   * Returns an iterator over all entries in this table as handle-to-object pairs.
   *
   * <p>The returned iterator provides a snapshot view of the entries at the time of the call.
   * Modifications to the table after this call may or may not be reflected.
   *
   * @return an iterator over map entries of handle index to resource object
   */
  Iterator<Map.Entry<Integer, Object>> iterEntries();
}
