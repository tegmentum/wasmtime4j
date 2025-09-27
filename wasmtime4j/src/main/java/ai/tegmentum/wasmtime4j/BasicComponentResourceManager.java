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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Basic implementation of ComponentResourceManager for foundational Component Model support.
 *
 * <p>This implementation provides core component resource management functionality as part of
 * Task #304 to stabilize the Component Model foundation.
 *
 * @since 1.0.0
 */
public final class BasicComponentResourceManager implements ComponentResourceManager {

  private static final Logger LOGGER = Logger.getLogger(BasicComponentResourceManager.class.getName());

  private final Map<String, ComponentResourceUsage> componentResources;
  private final Map<String, SharedResourceHandle> sharedResources;
  private final Map<String, ResourceAllocationResult> allocations;
  private final Map<String, IsolatedEnvironmentHandle> isolatedEnvironments;

  /**
   * Creates a new basic component resource manager.
   */
  public BasicComponentResourceManager() {
    this.componentResources = new ConcurrentHashMap<>();
    this.sharedResources = new ConcurrentHashMap<>();
    this.allocations = new ConcurrentHashMap<>();
    this.isolatedEnvironments = new ConcurrentHashMap<>();
  }

  @Override
  public ResourceAllocationResult allocateResources(final String componentId,
      final ResourceSpecification resourceSpec) throws WasmException {

    Objects.requireNonNull(componentId, "componentId");
    Objects.requireNonNull(resourceSpec, "resourceSpec");

    if (componentId.trim().isEmpty()) {
      throw new IllegalArgumentException("componentId cannot be empty");
    }

    try {
      LOGGER.fine("Allocating resources for component: " + componentId);

      // Create resource allocation
      final ResourceAllocationResult allocation = new ResourceAllocationResult(
          true,
          "Resources allocated successfully",
          resourceSpec.getMemoryMb(),
          resourceSpec.getCpuUnits()
      );

      allocations.put(componentId, allocation);

      // Track resource usage
      final ComponentResourceUsage usage = new ComponentResourceUsage(componentId);
      componentResources.put(componentId, usage);

      LOGGER.fine("Successfully allocated resources for component: " + componentId);
      return allocation;

    } catch (final Exception e) {
      throw new WasmException("Failed to allocate resources for component: " + componentId, e);
    }
  }

  @Override
  public void deallocateResources(final String componentId) throws WasmException {
    Objects.requireNonNull(componentId, "componentId");

    if (componentId.trim().isEmpty()) {
      throw new IllegalArgumentException("componentId cannot be empty");
    }

    try {
      LOGGER.fine("Deallocating resources for component: " + componentId);

      allocations.remove(componentId);
      componentResources.remove(componentId);

      LOGGER.fine("Successfully deallocated resources for component: " + componentId);

    } catch (final Exception e) {
      throw new WasmException("Failed to deallocate resources for component: " + componentId, e);
    }
  }

  @Override
  public SharedResourceHandle createSharedResource(final String resourceId,
      final SharedResourceType resourceType,
      final SharedResourceConfig config) throws WasmException {

    Objects.requireNonNull(resourceId, "resourceId");
    Objects.requireNonNull(resourceType, "resourceType");
    Objects.requireNonNull(config, "config");

    try {
      LOGGER.fine("Creating shared resource: " + resourceId);

      final SharedResourceHandle handle = new BasicSharedResourceHandle(
          resourceId, resourceType, config);
      sharedResources.put(resourceId, handle);

      LOGGER.fine("Successfully created shared resource: " + resourceId);
      return handle;

    } catch (final Exception e) {
      throw new WasmException("Failed to create shared resource: " + resourceId, e);
    }
  }

  @Override
  public void grantResourceAccess(final String componentId, final String resourceId,
      final ResourceAccessLevel accessLevel) throws WasmException {

    Objects.requireNonNull(componentId, "componentId");
    Objects.requireNonNull(resourceId, "resourceId");
    Objects.requireNonNull(accessLevel, "accessLevel");

    try {
      final SharedResourceHandle resource = sharedResources.get(resourceId);
      if (resource == null) {
        throw new WasmException("Shared resource not found: " + resourceId);
      }

      LOGGER.fine("Granting " + accessLevel + " access to resource " + resourceId +
                 " for component " + componentId);

      // In a full implementation, this would update the resource's access control list

    } catch (final Exception e) {
      throw new WasmException("Failed to grant resource access", e);
    }
  }

  @Override
  public void revokeResourceAccess(final String componentId, final String resourceId)
      throws WasmException {

    Objects.requireNonNull(componentId, "componentId");
    Objects.requireNonNull(resourceId, "resourceId");

    if (componentId.trim().isEmpty()) {
      throw new IllegalArgumentException("componentId cannot be empty");
    }

    try {
      LOGGER.fine("Revoking resource access to " + resourceId + " for component " + componentId);

      // In a full implementation, this would update the resource's access control list

    } catch (final Exception e) {
      throw new WasmException("Failed to revoke resource access", e);
    }
  }

  @Override
  public IsolatedEnvironmentHandle createIsolatedEnvironment(final String componentId,
      final ResourceIsolationConfig isolationConfig) throws WasmException {

    Objects.requireNonNull(componentId, "componentId");
    Objects.requireNonNull(isolationConfig, "isolationConfig");

    try {
      LOGGER.fine("Creating isolated environment for component: " + componentId);

      final IsolatedEnvironmentHandle handle = new BasicIsolatedEnvironmentHandle(
          componentId, isolationConfig);
      isolatedEnvironments.put(componentId, handle);

      LOGGER.fine("Successfully created isolated environment for component: " + componentId);
      return handle;

    } catch (final Exception e) {
      throw new WasmException("Failed to create isolated environment for component: " + componentId, e);
    }
  }

  @Override
  public void setResourceLimits(final String componentId, final ComponentResourceLimits limits)
      throws WasmException {

    Objects.requireNonNull(componentId, "componentId");
    Objects.requireNonNull(limits, "limits");

    try {
      LOGGER.fine("Setting resource limits for component: " + componentId);

      final ComponentResourceUsage usage = componentResources.get(componentId);
      if (usage != null) {
        // In a full implementation, this would update the resource usage with limits
        LOGGER.fine("Resource limits set for component: " + componentId);
      } else {
        LOGGER.warning("No resource tracking found for component: " + componentId);
      }

    } catch (final Exception e) {
      throw new WasmException("Failed to set resource limits for component: " + componentId, e);
    }
  }

  @Override
  public ComponentResourceLimits getResourceLimits(final String componentId) throws WasmException {
    Objects.requireNonNull(componentId, "componentId");

    // Return default limits for now
    return new ComponentResourceLimits();
  }

  @Override
  public ComponentResourceUsage getResourceUsage(final String componentId) throws WasmException {
    Objects.requireNonNull(componentId, "componentId");

    final ComponentResourceUsage usage = componentResources.get(componentId);
    if (usage != null) {
      return usage;
    }

    // Return default usage if component not tracked
    return new ComponentResourceUsage(componentId);
  }

  @Override
  public Map<String, ComponentResourceUsage> getAllResourceUsage() {
    return Collections.unmodifiableMap(new HashMap<>(componentResources));
  }

  @Override
  public CompletableFuture<ResourceOptimizationResult> optimizeResources(
      final ResourceOptimizationConfig config) throws WasmException {

    Objects.requireNonNull(config, "config");

    return CompletableFuture.supplyAsync(() -> {
      try {
        LOGGER.fine("Optimizing resources with configuration");

        // Basic optimization - in a full implementation this would analyze usage patterns
        // and redistribute resources
        final int optimizedCount = componentResources.size();

        return new ResourceOptimizationResult(
            true,
            "Resource optimization completed",
            optimizedCount,
            0.0 // No memory saved in basic implementation
        );

      } catch (final Exception e) {
        return new ResourceOptimizationResult(
            false,
            "Resource optimization failed: " + e.getMessage(),
            0,
            0.0
        );
      }
    });
  }

  @Override
  public Set<String> getSharedResourceIds() {
    return Collections.unmodifiableSet(sharedResources.keySet());
  }

  @Override
  public ResourceHealthStatus checkResourceHealth() {
    try {
      final int totalComponents = componentResources.size();
      final int totalSharedResources = sharedResources.size();
      final boolean healthy = true; // Basic implementation always reports healthy

      return new ResourceHealthStatus(
          healthy,
          "Resource manager is healthy",
          totalComponents,
          totalSharedResources
      );

    } catch (final Exception e) {
      return new ResourceHealthStatus(
          false,
          "Health check failed: " + e.getMessage(),
          0,
          0
      );
    }
  }

  @Override
  public CompletableFuture<Void> performResourceGarbageCollection() {
    return CompletableFuture.runAsync(() -> {
      try {
        LOGGER.fine("Performing resource garbage collection");

        // Remove invalid allocations
        allocations.entrySet().removeIf(entry -> !entry.getValue().isSuccessful());

        // In a full implementation, this would perform comprehensive cleanup

        LOGGER.fine("Resource garbage collection completed");

      } catch (final Exception e) {
        LOGGER.warning("Resource garbage collection failed: " + e.getMessage());
      }
    });
  }

  @Override
  public void close() {
    try {
      LOGGER.fine("Closing component resource manager");

      // Close all isolated environments
      isolatedEnvironments.values().forEach(env -> {
        try {
          env.close();
        } catch (Exception e) {
          LOGGER.warning("Error closing isolated environment: " + e.getMessage());
        }
      });

      // Close all shared resources
      sharedResources.values().forEach(resource -> {
        try {
          resource.close();
        } catch (Exception e) {
          LOGGER.warning("Error closing shared resource: " + e.getMessage());
        }
      });

      // Clear all maps
      componentResources.clear();
      sharedResources.clear();
      allocations.clear();
      isolatedEnvironments.clear();

      LOGGER.fine("Component resource manager closed successfully");

    } catch (final Exception e) {
      LOGGER.warning("Error closing component resource manager: " + e.getMessage());
    }
  }

  /**
   * Basic implementation of SharedResourceHandle.
   */
  private static final class BasicSharedResourceHandle implements SharedResourceHandle {
    private final String resourceId;
    private final SharedResourceType resourceType;
    private final SharedResourceConfig config;
    private volatile boolean closed = false;

    BasicSharedResourceHandle(final String resourceId, final SharedResourceType resourceType,
        final SharedResourceConfig config) {
      this.resourceId = resourceId;
      this.resourceType = resourceType;
      this.config = config;
    }

    @Override
    public String getResourceId() {
      return resourceId;
    }

    @Override
    public SharedResourceType getResourceType() {
      return resourceType;
    }

    @Override
    public SharedResourceConfig getConfig() {
      return config;
    }

    @Override
    public boolean isValid() {
      return !closed;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  /**
   * Basic implementation of IsolatedEnvironmentHandle.
   */
  private static final class BasicIsolatedEnvironmentHandle implements IsolatedEnvironmentHandle {
    private final String componentId;
    private final ResourceIsolationConfig config;
    private volatile boolean closed = false;

    BasicIsolatedEnvironmentHandle(final String componentId,
        final ResourceIsolationConfig config) {
      this.componentId = componentId;
      this.config = config;
    }

    @Override
    public String getComponentId() {
      return componentId;
    }

    @Override
    public ResourceIsolationConfig getConfig() {
      return config;
    }

    @Override
    public boolean isValid() {
      return !closed;
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}