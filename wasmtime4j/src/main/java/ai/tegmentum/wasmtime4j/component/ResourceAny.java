/*
 * Copyright 2025 Tegmentum AI
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
   * <p>This method must be called for <b>all</b> resource handles — both owned and borrowed — when
   * they are no longer needed. For host-defined resources, this triggers the destructor callback
   * registered during resource definition.
   *
   * <p>This method executes synchronously. The Wasmtime API also provides an async variant ({@code
   * resource_drop_async}) which is not currently supported. If async resource dropping is needed,
   * perform the drop in a virtual thread or executor.
   *
   * @param store the store that contains this resource
   * @throws WasmException if the resource cannot be dropped
   */
  void resourceDrop(Store store) throws WasmException;

  /**
   * Asynchronously drops this resource handle, releasing the underlying resource.
   *
   * <p>This is the async variant of {@link #resourceDrop(Store)}. It requires the engine to have
   * been created with async support enabled. The default implementation wraps the synchronous
   * {@link #resourceDrop(Store)} call in a {@link java.util.concurrent.CompletableFuture}.
   *
   * @param store the store that contains this resource
   * @return a CompletableFuture that completes when the resource has been dropped
   * @since 1.1.0
   */
  default java.util.concurrent.CompletableFuture<Void> resourceDropAsync(final Store store) {
    return java.util.concurrent.CompletableFuture.runAsync(
        () -> {
          try {
            resourceDrop(store);
          } catch (final WasmException e) {
            throw new java.util.concurrent.CompletionException(e);
          }
        });
  }

  /**
   * Gets the u32 representation value for this resource in the given store.
   *
   * <p>This corresponds to Wasmtime's {@code ResourceAny::resource_rep()} which returns the
   * underlying representation (typically a table index or host object reference) for this resource
   * handle.
   *
   * @param store the store context
   * @return the resource representation value
   * @throws WasmException if the operation fails
   * @since 1.1.0
   */
  int resourceRep(Store store) throws WasmException;

  /**
   * Gets the native handle ID for this resource in the resource registry.
   *
   * @return the native handle ID
   */
  long getNativeHandle();

  /**
   * Creates a new owned resource handle with the given type and representation.
   *
   * <p>This corresponds to Wasmtime's {@code ResourceAny::resource_new()} which creates a new
   * resource handle in the component model's resource table.
   *
   * @param store the store context
   * @param typeId the resource type identifier
   * @param rep the u32 representation value for the resource
   * @return a new owned ResourceAny handle
   * @throws WasmException if the resource cannot be created
   * @throws IllegalArgumentException if store is null
   * @since 1.1.0
   */
  static ResourceAny resourceNew(final Store store, final int typeId, final int rep)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    return new DefaultResourceAny(typeId, true, rep, 0, null);
  }

  /**
   * Creates a new resource handle with the given native registry handle ID and lifecycle callback.
   *
   * <p>This factory is used by runtime implementations (JNI/Panama) when a component function
   * returns a resource value. The native handle ID corresponds to an entry in the native resource
   * registry, and the lifecycle callback enables native resource_drop and resource_rep calls.
   *
   * @param typeId the resource type identifier
   * @param owned whether this is an owned handle
   * @param rep the u32 representation value
   * @param nativeHandle the native registry handle ID
   * @param lifecycle the lifecycle callback for native operations, or null for host-only resources
   * @return a new ResourceAny handle
   * @since 1.1.0
   */
  static ResourceAny fromNative(
      final int typeId,
      final boolean owned,
      final int rep,
      final long nativeHandle,
      final ResourceLifecycleCallback lifecycle) {
    return new DefaultResourceAny(typeId, owned, rep, nativeHandle, lifecycle);
  }

  /**
   * Callback interface for native resource lifecycle operations.
   *
   * <p>Runtime implementations (JNI/Panama) provide this to enable native resource_drop and
   * resource_rep calls through the underlying Wasmtime store.
   *
   * @since 1.1.0
   */
  interface ResourceLifecycleCallback {

    /**
     * Drops the resource in the native store.
     *
     * @param nativeHandle the native registry handle ID
     * @throws WasmException if the drop fails
     */
    void drop(long nativeHandle) throws WasmException;
  }

  /** Default implementation of ResourceAny for host-side resource management. */
  final class DefaultResourceAny implements ResourceAny {

    private final int typeId;
    private final boolean owned;
    private final int rep;
    private final long nativeHandle;
    private final ResourceLifecycleCallback lifecycle;
    private volatile boolean dropped;

    DefaultResourceAny(
        final int typeId,
        final boolean owned,
        final int rep,
        final long nativeHandle,
        final ResourceLifecycleCallback lifecycle) {
      this.typeId = typeId;
      this.owned = owned;
      this.rep = rep;
      this.nativeHandle = nativeHandle;
      this.lifecycle = lifecycle;
      this.dropped = false;
    }

    @Override
    public int getTypeId() {
      return typeId;
    }

    @Override
    public boolean isOwned() {
      return owned;
    }

    @Override
    public boolean isBorrowed() {
      return !owned;
    }

    @Override
    public void resourceDrop(final Store store) throws WasmException {
      if (dropped) {
        throw new WasmException("Resource has already been dropped");
      }
      dropped = true;
      if (lifecycle != null && nativeHandle != 0) {
        lifecycle.drop(nativeHandle);
      }
    }

    @Override
    public int resourceRep(final Store store) throws WasmException {
      if (dropped) {
        throw new WasmException("Resource has been dropped");
      }
      return rep;
    }

    @Override
    public long getNativeHandle() {
      return nativeHandle != 0 ? nativeHandle : rep;
    }

    @Override
    public String toString() {
      return "ResourceAny{typeId="
          + typeId
          + ", owned="
          + owned
          + ", rep="
          + rep
          + ", nativeHandle="
          + nativeHandle
          + "}";
    }
  }
}
