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
import java.util.Optional;

/**
 * Provides access to the Component Model resource table for managing host resources.
 *
 * <p>A resource table is a mapping from integer indices to host-side objects. When a component
 * creates a resource via a host constructor, the host pushes the underlying object into the
 * resource table and returns the index as the resource handle. When the component calls methods
 * on the resource, the host looks up the object by index.
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
   * <p>After removal, the index is no longer valid and may be reused for future entries.
   *
   * @param <T> the expected type of the resource entry
   * @param index the resource handle index
   * @param clazz the expected class of the entry
   * @return an Optional containing the removed entry, or empty if the index was invalid
   * @throws ClassCastException if the entry is not of the expected type
   * @throws IllegalArgumentException if clazz is null
   */
  <T> Optional<T> delete(int index, Class<T> clazz);

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
}
