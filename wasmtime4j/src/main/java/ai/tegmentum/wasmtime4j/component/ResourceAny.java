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

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents an opaque resource handle in the WebAssembly Component Model.
 *
 * <p>This interface corresponds to Wasmtime's {@code ResourceAny} type, which is a dynamically
 * typed resource handle that can represent any Component Model resource. Resources are handle-based
 * types that enable host-managed objects to be passed across component boundaries.
 *
 * <p>A {@code ResourceAny} can be either:
 *
 * <ul>
 *   <li><b>owned</b>: The holder is responsible for the resource's lifecycle. When the resource is
 *       no longer needed, {@link #resourceDrop(Store)} must be called.
 *   <li><b>borrowed</b>: The holder has temporary access to the resource. The resource remains
 *       valid only for the duration of the call that provided it.
 * </ul>
 *
 * <p>Resource handles are typically obtained from component function calls that return resource
 * types, or created by host functions that implement resource constructors.
 *
 * @since 1.0.0
 */
public interface ResourceAny {

  /**
   * Gets the type identifier for this resource.
   *
   * <p>The type ID uniquely identifies the resource type within a component instantiation. Two
   * resource handles with the same type ID belong to the same resource type definition.
   *
   * @return the resource type identifier
   */
  int getTypeId();

  /**
   * Checks if this is an owned resource handle.
   *
   * <p>Owned handles transfer resource ownership. The caller is responsible for calling {@link
   * #resourceDrop(Store)} when the resource is no longer needed.
   *
   * @return true if this is an owned handle
   */
  boolean isOwned();

  /**
   * Checks if this is a borrowed resource handle.
   *
   * <p>Borrowed handles provide temporary access. The resource remains valid only for the duration
   * of the call context that produced this handle.
   *
   * @return true if this is a borrowed handle
   */
  boolean isBorrowed();

  /**
   * Drops this resource handle, releasing the underlying resource.
   *
   * <p>This method must be called for owned resource handles when they are no longer needed. For
   * host-defined resources, this triggers the destructor callback. For borrowed handles, calling
   * this method is an error.
   *
   * @param store the store that contains this resource
   * @throws WasmException if the resource cannot be dropped
   * @throws IllegalStateException if called on a borrowed handle
   */
  void resourceDrop(Store store) throws WasmException;

  /**
   * Gets the native handle ID for this resource in the resource registry.
   *
   * @return the native handle ID
   */
  long getNativeHandle();
}
