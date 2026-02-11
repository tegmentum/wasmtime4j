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
 * Represents a WIT owned resource handle value.
 *
 * <p>In the WebAssembly Component Model, {@code own<T>} represents an owned handle to a resource.
 * When a component receives an owned handle, it takes ownership of the resource. When the handle is
 * dropped or transferred, the resource's destructor is called (if defined).
 *
 * <p>Owned handles are typically used for:
 *
 * <ul>
 *   <li>Returning newly created resources from functions
 *   <li>Transferring resource ownership between components
 *   <li>Passing resources that should be consumed by the callee
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
 * open-file: func(path: string) -> own<file-handle>;
 * }</pre>
 *
 * @since 1.0.0
 * @see WitBorrow
 * @see ComponentResourceHandle
 */
public final class WitOwn extends WitValue {

  private final ComponentResourceHandle handle;

  /**
   * Creates a new WIT owned resource handle value.
   *
   * @param resourceType the WIT resource type
   * @param handle the underlying resource handle
   * @throws IllegalArgumentException if handle is not owned
   */
  private WitOwn(final WitType resourceType, final ComponentResourceHandle handle) {
    super(resourceType);
    if (handle == null) {
      throw new IllegalArgumentException("Resource handle cannot be null");
    }
    if (!handle.isOwned()) {
      throw new IllegalArgumentException("Handle must be owned for WitOwn");
    }
    this.handle = handle;
    validate();
  }

  /**
   * Creates a WIT owned resource handle value.
   *
   * @param resourceType the resource type name
   * @param index the handle index
   * @return a new WitOwn value
   */
  public static WitOwn of(final String resourceType, final int index) {
    final ComponentResourceHandle handle = ComponentResourceHandle.own(resourceType, index);
    final WitType witType = WitType.resource(resourceType, resourceType);
    return new WitOwn(witType, handle);
  }

  /**
   * Creates a WIT owned resource handle value wrapping a host object.
   *
   * @param resourceType the resource type name
   * @param index the handle index
   * @param hostObject the host object to wrap
   * @return a new WitOwn value with host object
   */
  public static WitOwn ofWithHost(
      final String resourceType, final int index, final Object hostObject) {
    final ComponentResourceHandle handle =
        ComponentResourceHandle.ownWithHost(resourceType, index, hostObject);
    final WitType witType = WitType.resource(resourceType, resourceType);
    return new WitOwn(witType, handle);
  }

  /**
   * Creates a WIT owned resource handle value from an existing handle.
   *
   * @param handle the owned resource handle
   * @return a new WitOwn value
   * @throws IllegalArgumentException if handle is not owned
   */
  public static WitOwn fromHandle(final ComponentResourceHandle handle) {
    if (handle == null) {
      throw new IllegalArgumentException("Handle cannot be null");
    }
    if (!handle.isOwned()) {
      throw new IllegalArgumentException("Handle must be owned for WitOwn");
    }
    final WitType witType = WitType.resource(handle.getResourceType(), handle.getResourceType());
    return new WitOwn(witType, handle);
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
        "WitOwn{resourceType='%s', index=%d}", handle.getResourceType(), handle.getIndex());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitOwn)) {
      return false;
    }
    final WitOwn other = (WitOwn) obj;
    return handle.equals(other.handle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), handle);
  }
}
