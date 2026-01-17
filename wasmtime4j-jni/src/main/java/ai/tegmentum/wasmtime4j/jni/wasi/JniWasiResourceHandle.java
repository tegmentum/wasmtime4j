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

package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle;
import java.util.Objects;

/**
 * JNI implementation of WasiResourceHandle.
 *
 * <p>This class provides a handle to a WASI resource that can be passed between components.
 *
 * @since 1.0.0
 */
public final class JniWasiResourceHandle implements WasiResourceHandle {

  private final long resourceId;
  private final String resourceType;
  private final WasiResource resource;
  private volatile WasiInstance owner;
  private volatile boolean valid;

  /**
   * Creates a new WASI resource handle.
   *
   * @param resource the underlying WASI resource
   * @throws IllegalArgumentException if resource is null
   */
  public JniWasiResourceHandle(final WasiResource resource) {
    this.resource = Objects.requireNonNull(resource, "resource cannot be null");
    this.resourceId = resource.getId();
    this.resourceType = resource.getType();
    this.owner = resource.getOwner();
    this.valid = resource.isValid();
  }

  /**
   * Creates a new WASI resource handle with explicit values.
   *
   * @param resourceId the resource ID
   * @param resourceType the resource type
   * @param owner the owning instance
   */
  public JniWasiResourceHandle(
      final long resourceId, final String resourceType, final WasiInstance owner) {
    this.resourceId = resourceId;
    this.resourceType = Objects.requireNonNull(resourceType, "resourceType cannot be null");
    this.resource = null;
    this.owner = owner;
    this.valid = true;
  }

  @Override
  public long getResourceId() {
    return resourceId;
  }

  @Override
  public String getResourceType() {
    return resourceType;
  }

  @Override
  public WasiInstance getOwner() {
    return owner;
  }

  @Override
  public boolean isValid() {
    if (!valid) {
      return false;
    }
    if (resource != null) {
      return resource.isValid();
    }
    return true;
  }

  /**
   * Gets the underlying WASI resource.
   *
   * @return the underlying resource, or null if not available
   */
  public WasiResource getResource() {
    return resource;
  }

  /**
   * Sets the owner of this resource handle.
   *
   * @param newOwner the new owner
   */
  void setOwner(final WasiInstance newOwner) {
    this.owner = newOwner;
  }

  /** Invalidates this handle. */
  void invalidate() {
    this.valid = false;
  }

  @Override
  public String toString() {
    return "JniWasiResourceHandle{"
        + "resourceId="
        + resourceId
        + ", resourceType='"
        + resourceType
        + '\''
        + ", valid="
        + valid
        + '}';
  }
}
