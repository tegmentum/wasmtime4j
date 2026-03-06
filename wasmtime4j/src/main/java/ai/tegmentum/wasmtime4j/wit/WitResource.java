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
package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import java.util.Objects;

/**
 * Unified resource WIT value that bridges type metadata with handle lifecycle.
 *
 * <p>WitResource combines a {@link ComponentResourceHandle} with resource type metadata and
 * ownership semantics, providing a single type that can represent both owned and borrowed resource
 * references. It can be converted to {@link WitOwn} or {@link WitBorrow} for use with the WIT value
 * marshalling system.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create an owned resource
 * WitResource ownedRes = WitResource.own("wasi:io/streams/input-stream", 42);
 *
 * // Create a borrowed resource
 * WitResource borrowedRes = WitResource.borrow("wasi:io/streams/input-stream", 42);
 *
 * // Convert from an existing handle
 * WitResource fromHandle = WitResource.fromHandle(existingHandle);
 *
 * // Convert to WitOwn/WitBorrow for marshalling
 * WitOwn own = ownedRes.toOwn();
 * WitBorrow borrow = borrowedRes.toBorrow();
 * }</pre>
 *
 * @since 1.1.0
 */
public final class WitResource extends WitValue {

  private final ComponentResourceHandle handle;
  private final String resourceTypeName;
  private final boolean owned;

  private WitResource(
      final ComponentResourceHandle handle, final String resourceTypeName, final boolean owned) {
    super(WitType.resource(resourceTypeName, resourceTypeName));
    this.handle = Objects.requireNonNull(handle, "handle cannot be null");
    this.resourceTypeName = Objects.requireNonNull(resourceTypeName, "resourceTypeName");
    this.owned = owned;
  }

  /**
   * Creates an owned resource.
   *
   * @param resourceTypeName the resource type name (e.g., "wasi:io/streams/input-stream")
   * @param index the resource handle index (u32 as signed int)
   * @return a new owned WitResource
   */
  public static WitResource own(final String resourceTypeName, final int index) {
    final ComponentResourceHandle handle = ComponentResourceHandle.own(resourceTypeName, index);
    return new WitResource(handle, resourceTypeName, true);
  }

  /**
   * Creates an owned resource with a native handle ID.
   *
   * @param resourceTypeName the resource type name
   * @param handle the native handle ID (u64)
   * @return a new owned WitResource
   */
  public static WitResource ownWithNativeHandle(final String resourceTypeName, final long handle) {
    final ComponentResourceHandle crh =
        ComponentResourceHandle.ownWithNativeHandle(resourceTypeName, 0, handle);
    return new WitResource(crh, resourceTypeName, true);
  }

  /**
   * Creates a borrowed resource.
   *
   * @param resourceTypeName the resource type name (e.g., "wasi:io/streams/input-stream")
   * @param index the resource handle index (u32 as signed int)
   * @return a new borrowed WitResource
   */
  public static WitResource borrow(final String resourceTypeName, final int index) {
    final ComponentResourceHandle handle = ComponentResourceHandle.borrow(resourceTypeName, index);
    return new WitResource(handle, resourceTypeName, false);
  }

  /**
   * Creates a borrowed resource with a native handle ID.
   *
   * @param resourceTypeName the resource type name
   * @param handle the native handle ID (u64)
   * @return a new borrowed WitResource
   */
  public static WitResource borrowWithNativeHandle(
      final String resourceTypeName, final long handle) {
    final ComponentResourceHandle crh =
        ComponentResourceHandle.borrowWithNativeHandle(resourceTypeName, 0, handle);
    return new WitResource(crh, resourceTypeName, false);
  }

  /**
   * Creates a WitResource from an existing {@link ComponentResourceHandle}.
   *
   * <p>The ownership semantics (owned vs. borrowed) are determined from the handle.
   *
   * @param handle the existing resource handle
   * @return a new WitResource wrapping the handle
   */
  public static WitResource fromHandle(final ComponentResourceHandle handle) {
    Objects.requireNonNull(handle, "handle cannot be null");
    return new WitResource(handle, handle.getResourceType(), handle.isOwned());
  }

  /**
   * Gets the underlying {@link ComponentResourceHandle}.
   *
   * @return the resource handle
   */
  public ComponentResourceHandle getHandle() {
    return handle;
  }

  /**
   * Gets the resource type name.
   *
   * @return the resource type name (e.g., "wasi:io/streams/input-stream")
   */
  public String getResourceTypeName() {
    return resourceTypeName;
  }

  /**
   * Returns whether this resource is owned.
   *
   * @return true if this is an owned resource, false if borrowed
   */
  public boolean isOwned() {
    return owned;
  }

  /**
   * Returns whether this resource is borrowed.
   *
   * @return true if this is a borrowed resource, false if owned
   */
  public boolean isBorrowed() {
    return !owned;
  }

  /**
   * Converts this resource to a {@link WitOwn}.
   *
   * <p>If this resource is already owned, the conversion is direct. If it is borrowed, a new owned
   * handle is created with the same index.
   *
   * @return a WitOwn wrapping this resource
   */
  public WitOwn toOwn() {
    if (owned) {
      return WitOwn.fromHandle(handle);
    }
    return WitOwn.of(resourceTypeName, handle.getIndex());
  }

  /**
   * Converts this resource to a {@link WitBorrow}.
   *
   * <p>If this resource is already borrowed, the conversion is direct. If it is owned, a new
   * borrowed handle is created with the same index.
   *
   * @return a WitBorrow wrapping this resource
   */
  public WitBorrow toBorrow() {
    if (!owned) {
      return WitBorrow.fromHandle(handle);
    }
    return WitBorrow.of(resourceTypeName, handle.getIndex());
  }

  @Override
  public Object toJava() {
    return handle;
  }

  @Override
  public void validate() {
    if (handle.getIndex() < 0 && handle.getNativeHandle() < 0) {
      throw new IllegalStateException("Resource handle must have a valid index or native handle");
    }
  }

  @Override
  public String toString() {
    return "WitResource{"
        + (owned ? "own" : "borrow")
        + "("
        + resourceTypeName
        + "), index="
        + handle.getIndex()
        + "}";
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitResource)) {
      return false;
    }
    final WitResource other = (WitResource) obj;
    return owned == other.owned
        && Objects.equals(resourceTypeName, other.resourceTypeName)
        && Objects.equals(handle, other.handle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(owned, resourceTypeName, handle);
  }
}
