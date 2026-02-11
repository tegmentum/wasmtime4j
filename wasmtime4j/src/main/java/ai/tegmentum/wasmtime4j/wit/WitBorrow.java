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

package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.ComponentResourceHandle;
import java.util.Objects;

/**
 * Represents a WIT borrowed resource handle value.
 *
 * <p>In the WebAssembly Component Model, {@code borrow<T>} represents a borrowed handle to a
 * resource. When a component receives a borrowed handle, it can use the resource but does not take
 * ownership. The resource must remain valid for the duration of the call.
 *
 * <p>Borrowed handles are typically used for:
 *
 * <ul>
 *   <li>Passing resources to functions that only need to read or use them temporarily
 *   <li>Implementing methods on resources where the resource remains owned by the caller
 *   <li>Avoiding unnecessary ownership transfers for performance
 * </ul>
 *
 * <p>Example WIT definition:
 *
 * <pre>{@code
 * resource file-handle {
 *   constructor(path: string);
 *   read: func(bytes: u32) -> list<u8>;
 *   close: func();
 * }
 *
 * // Takes a borrowed handle - file-handle remains owned by caller
 * read-all: func(file: borrow<file-handle>) -> list<u8>;
 * }</pre>
 *
 * @since 1.0.0
 * @see WitOwn
 * @see ComponentResourceHandle
 */
public final class WitBorrow extends WitValue {

  private final ComponentResourceHandle handle;

  /**
   * Creates a new WIT borrowed resource handle value.
   *
   * @param resourceType the WIT resource type
   * @param handle the underlying resource handle
   * @throws IllegalArgumentException if handle is owned
   */
  private WitBorrow(final WitType resourceType, final ComponentResourceHandle handle) {
    super(resourceType);
    if (handle == null) {
      throw new IllegalArgumentException("Resource handle cannot be null");
    }
    if (handle.isOwned()) {
      throw new IllegalArgumentException("Handle must be borrowed for WitBorrow");
    }
    this.handle = handle;
    validate();
  }

  /**
   * Creates a WIT borrowed resource handle value.
   *
   * @param resourceType the resource type name
   * @param index the handle index
   * @return a new WitBorrow value
   */
  public static WitBorrow of(final String resourceType, final int index) {
    final ComponentResourceHandle handle = ComponentResourceHandle.borrow(resourceType, index);
    final WitType witType = WitType.resource(resourceType, resourceType);
    return new WitBorrow(witType, handle);
  }

  /**
   * Creates a WIT borrowed resource handle value wrapping a host object.
   *
   * @param resourceType the resource type name
   * @param index the handle index
   * @param hostObject the host object to wrap
   * @return a new WitBorrow value with host object
   */
  public static WitBorrow ofWithHost(
      final String resourceType, final int index, final Object hostObject) {
    final ComponentResourceHandle handle =
        ComponentResourceHandle.borrowWithHost(resourceType, index, hostObject);
    final WitType witType = WitType.resource(resourceType, resourceType);
    return new WitBorrow(witType, handle);
  }

  /**
   * Creates a WIT borrowed resource handle value from an existing handle.
   *
   * @param handle the borrowed resource handle
   * @return a new WitBorrow value
   * @throws IllegalArgumentException if handle is owned
   */
  public static WitBorrow fromHandle(final ComponentResourceHandle handle) {
    if (handle == null) {
      throw new IllegalArgumentException("Handle cannot be null");
    }
    if (handle.isOwned()) {
      throw new IllegalArgumentException("Handle must be borrowed for WitBorrow");
    }
    final WitType witType = WitType.resource(handle.getResourceType(), handle.getResourceType());
    return new WitBorrow(witType, handle);
  }

  /**
   * Gets the underlying resource handle.
   *
   * @return the resource handle
   */
  public ComponentResourceHandle getHandle() {
    return handle;
  }

  /**
   * Gets the resource type name.
   *
   * @return the resource type name
   */
  public String getResourceType() {
    return handle.getResourceType();
  }

  /**
   * Gets the handle index.
   *
   * @return the handle index
   */
  public int getIndex() {
    return handle.getIndex();
  }

  /**
   * Gets the host object if this handle was created with one.
   *
   * @param <T> the expected type of the host object
   * @param clazz the class of the expected type
   * @return the host object
   * @throws IllegalStateException if the handle does not reference a host resource
   * @throws ClassCastException if the host object is not of the expected type
   */
  public <T> T getHostObject(final Class<T> clazz) {
    return handle.getHostObject(clazz);
  }

  @Override
  public ComponentResourceHandle toJava() {
    return handle;
  }

  @Override
  protected void validate() {
    // The handle index must be non-negative
    if (handle.getIndex() < 0) {
      throw new IllegalArgumentException(
          "Resource handle index cannot be negative: " + handle.getIndex());
    }
  }

  @Override
  public String toString() {
    return String.format(
        "WitBorrow{resourceType='%s', index=%d}", handle.getResourceType(), handle.getIndex());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitBorrow)) {
      return false;
    }
    final WitBorrow other = (WitBorrow) obj;
    return handle.equals(other.handle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), handle);
  }
}
