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

import java.util.Objects;

/**
 * Represents a Component Model resource handle.
 *
 * <p>Resources are handle-based types in the Component Model that allow host-managed objects to be
 * passed to and from WebAssembly components. There are two kinds of resource handles:
 *
 * <ul>
 *   <li><b>own</b>: An owned handle that transfers ownership. When the component is done with it,
 *       the resource destructor is called.
 *   <li><b>borrow</b>: A borrowed handle that does not transfer ownership. The resource must remain
 *       valid for the duration of the call.
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentResourceHandle {

  /**
   * Gets the resource type name.
   *
   * @return the resource type name
   */
  String getResourceType();

  /**
   * Gets the resource handle index.
   *
   * @return the handle index
   */
  int getIndex();

  /**
   * Checks if this is an owned handle.
   *
   * @return true if this is an owned handle
   */
  boolean isOwned();

  /**
   * Checks if this is a borrowed handle.
   *
   * @return true if this is a borrowed handle
   */
  boolean isBorrowed();

  /**
   * Gets the underlying host object if this handle was created from a host resource.
   *
   * @param <T> the expected type of the host object
   * @param clazz the class of the expected type
   * @return the host object
   * @throws IllegalStateException if the handle does not reference a host resource
   * @throws ClassCastException if the host object is not of the expected type
   */
  <T> T getHostObject(Class<T> clazz);

  /**
   * Gets the native resource handle identifier, if backed by a native resource.
   *
   * <p>This returns the unique identifier used to track this resource in the native resource
   * registry. For resources created from pure Java (via {@link #own} or {@link #borrow}), this
   * returns -1 indicating no native backing.
   *
   * @return the native resource handle ID, or -1 if not backed by native resource
   * @since 1.0.0
   */
  default long getNativeHandle() {
    return -1;
  }

  /**
   * Creates an owned resource handle.
   *
   * @param resourceType the resource type name
   * @param index the handle index
   * @return a new owned handle
   */
  static ComponentResourceHandle own(final String resourceType, final int index) {
    return new Impl(resourceType, index, true, null);
  }

  /**
   * Creates a borrowed resource handle.
   *
   * @param resourceType the resource type name
   * @param index the handle index
   * @return a new borrowed handle
   */
  static ComponentResourceHandle borrow(final String resourceType, final int index) {
    return new Impl(resourceType, index, false, null);
  }

  /**
   * Creates an owned resource handle wrapping a host object.
   *
   * @param resourceType the resource type name
   * @param index the handle index
   * @param hostObject the host object to wrap
   * @return a new owned handle with host object
   */
  static ComponentResourceHandle ownWithHost(
      final String resourceType, final int index, final Object hostObject) {
    return new Impl(resourceType, index, true, hostObject);
  }

  /**
   * Creates a borrowed resource handle wrapping a host object.
   *
   * @param resourceType the resource type name
   * @param index the handle index
   * @param hostObject the host object to wrap
   * @return a new borrowed handle with host object
   */
  static ComponentResourceHandle borrowWithHost(
      final String resourceType, final int index, final Object hostObject) {
    return new Impl(resourceType, index, false, hostObject);
  }

  /** Default implementation of ComponentResourceHandle. */
  final class Impl implements ComponentResourceHandle {
    private final String resourceType;
    private final int index;
    private final boolean owned;
    private final Object hostObject;

    Impl(final String resourceType, final int index, final boolean owned, final Object hostObject) {
      if (resourceType == null) {
        throw new IllegalArgumentException("Resource type cannot be null");
      }
      this.resourceType = resourceType;
      this.index = index;
      this.owned = owned;
      this.hostObject = hostObject;
    }

    @Override
    public String getResourceType() {
      return resourceType;
    }

    @Override
    public int getIndex() {
      return index;
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
    @SuppressWarnings("unchecked")
    public <T> T getHostObject(final Class<T> clazz) {
      if (hostObject == null) {
        throw new IllegalStateException("This handle does not reference a host resource");
      }
      return clazz.cast(hostObject);
    }

    @Override
    public String toString() {
      final String kind = owned ? "own" : "borrow";
      return kind + "<" + resourceType + ">(" + index + ")";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ComponentResourceHandle)) {
        return false;
      }
      final ComponentResourceHandle other = (ComponentResourceHandle) obj;
      return index == other.getIndex()
          && owned == other.isOwned()
          && resourceType.equals(other.getResourceType());
    }

    @Override
    public int hashCode() {
      return Objects.hash(resourceType, index, owned);
    }
  }
}
