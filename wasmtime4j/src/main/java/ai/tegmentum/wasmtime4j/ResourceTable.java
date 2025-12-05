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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.ResourceException;
import java.util.Optional;

/**
 * Low-level resource table for Component Model resource handles.
 *
 * <p>A resource table is a handle table that maps integer indices to resource objects. This is the
 * fundamental mechanism by which the Component Model manages resources passed between the host and
 * WebAssembly components.
 *
 * <p>The table supports:
 *
 * <ul>
 *   <li>Allocating new handles for resources
 *   <li>Looking up resources by handle
 *   <li>Freeing handles when resources are destroyed
 *   <li>Resource reuse through handle recycling
 * </ul>
 *
 * @param <T> the type of resources stored in this table
 * @since 1.0.0
 */
public interface ResourceTable<T> {

  /**
   * Pushes a new resource into the table and returns its handle.
   *
   * @param resource the resource to store
   * @return the handle for the stored resource
   * @throws ResourceException if the table is full or resource is null
   */
  int push(T resource) throws ResourceException;

  /**
   * Gets the resource at the given handle without removing it.
   *
   * @param handle the resource handle
   * @return the resource, or empty if handle is invalid or freed
   */
  Optional<T> get(int handle);

  /**
   * Gets the resource at the given handle, throwing if not found.
   *
   * @param handle the resource handle
   * @return the resource
   * @throws ResourceException if handle is invalid or freed
   */
  T getOrThrow(int handle) throws ResourceException;

  /**
   * Removes and returns the resource at the given handle.
   *
   * @param handle the resource handle
   * @return the removed resource, or empty if handle is invalid or freed
   */
  Optional<T> delete(int handle);

  /**
   * Removes and returns the resource at the given handle, throwing if not found.
   *
   * @param handle the resource handle
   * @return the removed resource
   * @throws ResourceException if handle is invalid or freed
   */
  T deleteOrThrow(int handle) throws ResourceException;

  /**
   * Checks if a handle is valid and points to a live resource.
   *
   * @param handle the resource handle
   * @return true if the handle is valid and the resource exists
   */
  boolean contains(int handle);

  /**
   * Gets the number of active resources in the table.
   *
   * @return the number of active resources
   */
  int size();

  /**
   * Checks if the table is empty.
   *
   * @return true if the table has no active resources
   */
  boolean isEmpty();

  /**
   * Gets the capacity of the table.
   *
   * @return the maximum number of resources that can be stored
   */
  int capacity();

  /**
   * Clears all resources from the table without calling destructors.
   *
   * <p>This is useful for bulk cleanup when the destructor will be called elsewhere.
   */
  void clear();
}
