package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Component resource management interface.
 *
 * <p>This interface provides comprehensive resource management for WebAssembly components,
 * including resource sharing, isolation, limits enforcement, and monitoring.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Resource allocation and deallocation
 *   <li>Resource sharing between components with access control
 *   <li>Resource isolation and sandboxing
 *   <li>Resource limits and quota enforcement
 *   <li>Resource usage monitoring and reporting
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentResourceManager {

  /**
   * Allocates resources for a component.
   *
   * @param componentId the component to allocate resources for
   * @param resourceSpec the resource specification
   * @return resource allocation result
   * @throws WasmException if resource allocation fails
   * @throws IllegalArgumentException if componentId or resourceSpec is null
   */
  ResourceAllocationResult allocateResources(String componentId, ResourceSpecification resourceSpec)
      throws WasmException;

  /**
   * Deallocates resources for a component.
   *
   * @param componentId the component to deallocate resources for
   * @throws WasmException if resource deallocation fails
   * @throws IllegalArgumentException if componentId is null or empty
   */
  void deallocateResources(String componentId) throws WasmException;

  /**
   * Creates a shared resource that can be accessed by multiple components.
   *
   * @param resourceId the unique identifier for the shared resource
   * @param resourceType the type of shared resource
   * @param config the shared resource configuration
   * @return shared resource handle
   * @throws WasmException if shared resource creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  SharedResourceHandle createSharedResource(
      String resourceId, SharedResourceType resourceType, SharedResourceConfig config)
      throws WasmException;

  /**
   * Grants a component access to a shared resource.
   *
   * @param componentId the component to grant access to
   * @param resourceId the shared resource identifier
   * @param accessLevel the level of access to grant
   * @throws WasmException if access cannot be granted
   * @throws IllegalArgumentException if any parameter is null
   */
  void grantResourceAccess(String componentId, String resourceId, ResourceAccessLevel accessLevel)
      throws WasmException;

  /**
   * Revokes a component's access to a shared resource.
   *
   * @param componentId the component to revoke access from
   * @param resourceId the shared resource identifier
   * @throws WasmException if access cannot be revoked
   * @throws IllegalArgumentException if any parameter is null or empty
   */
  void revokeResourceAccess(String componentId, String resourceId) throws WasmException;

  /**
   * Creates an isolated resource environment for a component.
   *
   * @param componentId the component to create isolation for
   * @param isolationConfig the isolation configuration
   * @return isolated environment handle
   * @throws WasmException if isolation cannot be created
   * @throws IllegalArgumentException if any parameter is null
   */
  IsolatedEnvironmentHandle createIsolatedEnvironment(
      String componentId, ResourceIsolationConfig isolationConfig) throws WasmException;

  /**
   * Sets resource limits for a component.
   *
   * @param componentId the component to set limits for
   * @param limits the resource limits to enforce
   * @throws WasmException if limits cannot be set
   * @throws IllegalArgumentException if any parameter is null
   */
  void setResourceLimits(String componentId, ResourceLimits limits) throws WasmException;

  /**
   * Gets the current resource limits for a component.
   *
   * @param componentId the component to get limits for
   * @return the current resource limits
   * @throws IllegalArgumentException if componentId is null or empty
   */
  ResourceLimits getResourceLimits(String componentId);

  /**
   * Gets the current resource usage for a component.
   *
   * @param componentId the component to get usage for
   * @return the current resource usage
   * @throws IllegalArgumentException if componentId is null or empty
   */
  ResourceUsage getResourceUsage(String componentId);

  /**
   * Gets resource usage for all managed components.
   *
   * @return map of component IDs to their resource usage
   */
  Map<String, ResourceUsage> getAllResourceUsage();

  /**
   * Monitors resource usage for a component over time.
   *
   * @param componentId the component to monitor
   * @param monitoringConfig the monitoring configuration
   * @return resource monitoring handle
   * @throws WasmException if monitoring cannot be started
   * @throws IllegalArgumentException if any parameter is null
   */
  ResourceMonitoringHandle startResourceMonitoring(
      String componentId, ResourceMonitoringConfig monitoringConfig) throws WasmException;

  /**
   * Stops resource monitoring for a component.
   *
   * @param monitoringHandle the monitoring handle to stop
   * @throws WasmException if monitoring cannot be stopped
   * @throws IllegalArgumentException if monitoringHandle is null
   */
  void stopResourceMonitoring(ResourceMonitoringHandle monitoringHandle) throws WasmException;

  /**
   * Performs garbage collection on unused resources.
   *
   * @return future that completes when garbage collection is done
   * @throws WasmException if garbage collection fails
   */
  CompletableFuture<ResourceGarbageCollectionResult> performResourceGarbageCollection()
      throws WasmException;

  /**
   * Gets resource statistics for the manager.
   *
   * @return resource manager statistics
   */
  ResourceManagerStatistics getStatistics();

  /**
   * Checks if a component has access to a shared resource.
   *
   * @param componentId the component to check
   * @param resourceId the shared resource identifier
   * @return true if the component has access, false otherwise
   * @throws IllegalArgumentException if any parameter is null or empty
   */
  boolean hasResourceAccess(String componentId, String resourceId);

  /**
   * Gets all shared resources accessible by a component.
   *
   * @param componentId the component to check
   * @return set of accessible shared resource identifiers
   * @throws IllegalArgumentException if componentId is null or empty
   */
  Set<String> getAccessibleResources(String componentId);

  /**
   * Gets all components that have access to a shared resource.
   *
   * @param resourceId the shared resource identifier
   * @return set of component IDs with access
   * @throws IllegalArgumentException if resourceId is null or empty
   */
  Set<String> getResourceAccessors(String resourceId);

  /** Resource specification for component allocation. */
  final class ResourceSpecification {
    private final long maxMemoryBytes;
    private final int maxFileDescriptors;
    private final int maxNetworkConnections;
    private final Duration maxCpuTime;
    private final boolean requiresGpu;
    private final Set<ResourceCapability> requiredCapabilities;

    public ResourceSpecification(
        long maxMemoryBytes,
        int maxFileDescriptors,
        int maxNetworkConnections,
        Duration maxCpuTime,
        boolean requiresGpu,
        Set<ResourceCapability> requiredCapabilities) {
      this.maxMemoryBytes = maxMemoryBytes;
      this.maxFileDescriptors = maxFileDescriptors;
      this.maxNetworkConnections = maxNetworkConnections;
      this.maxCpuTime = maxCpuTime;
      this.requiresGpu = requiresGpu;
      this.requiredCapabilities = requiredCapabilities;
    }

    public long getMaxMemoryBytes() {
      return maxMemoryBytes;
    }

    public int getMaxFileDescriptors() {
      return maxFileDescriptors;
    }

    public int getMaxNetworkConnections() {
      return maxNetworkConnections;
    }

    public Duration getMaxCpuTime() {
      return maxCpuTime;
    }

    public boolean isRequiresGpu() {
      return requiresGpu;
    }

    public Set<ResourceCapability> getRequiredCapabilities() {
      return requiredCapabilities;
    }
  }

  /** Resource allocation result. */
  final class ResourceAllocationResult {
    private final boolean successful;
    private final String allocationId;
    private final ResourceLimits allocatedLimits;
    private final Set<String> warnings;

    public ResourceAllocationResult(
        boolean successful,
        String allocationId,
        ResourceLimits allocatedLimits,
        Set<String> warnings) {
      this.successful = successful;
      this.allocationId = allocationId;
      this.allocatedLimits = allocatedLimits;
      this.warnings = warnings;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getAllocationId() {
      return allocationId;
    }

    public ResourceLimits getAllocatedLimits() {
      return allocatedLimits;
    }

    public Set<String> getWarnings() {
      return warnings;
    }
  }

  /** Shared resource configuration. */
  final class SharedResourceConfig {
    private final int maxConcurrentAccess;
    private final Duration accessTimeout;
    private final boolean persistentAcrossRestarts;
    private final ResourceAccessPolicy accessPolicy;

    public SharedResourceConfig(
        int maxConcurrentAccess,
        Duration accessTimeout,
        boolean persistentAcrossRestarts,
        ResourceAccessPolicy accessPolicy) {
      this.maxConcurrentAccess = maxConcurrentAccess;
      this.accessTimeout = accessTimeout;
      this.persistentAcrossRestarts = persistentAcrossRestarts;
      this.accessPolicy = accessPolicy;
    }

    public int getMaxConcurrentAccess() {
      return maxConcurrentAccess;
    }

    public Duration getAccessTimeout() {
      return accessTimeout;
    }

    public boolean isPersistentAcrossRestarts() {
      return persistentAcrossRestarts;
    }

    public ResourceAccessPolicy getAccessPolicy() {
      return accessPolicy;
    }
  }

  /** Resource isolation configuration. */
  final class ResourceIsolationConfig {
    private final boolean isolateMemory;
    private final boolean isolateFileSystem;
    private final boolean isolateNetwork;
    private final boolean isolateEnvironmentVariables;
    private final Set<String> allowedSystemCalls;

    public ResourceIsolationConfig(
        boolean isolateMemory,
        boolean isolateFileSystem,
        boolean isolateNetwork,
        boolean isolateEnvironmentVariables,
        Set<String> allowedSystemCalls) {
      this.isolateMemory = isolateMemory;
      this.isolateFileSystem = isolateFileSystem;
      this.isolateNetwork = isolateNetwork;
      this.isolateEnvironmentVariables = isolateEnvironmentVariables;
      this.allowedSystemCalls = allowedSystemCalls;
    }

    public boolean isIsolateMemory() {
      return isolateMemory;
    }

    public boolean isIsolateFileSystem() {
      return isolateFileSystem;
    }

    public boolean isIsolateNetwork() {
      return isolateNetwork;
    }

    public boolean isIsolateEnvironmentVariables() {
      return isolateEnvironmentVariables;
    }

    public Set<String> getAllowedSystemCalls() {
      return allowedSystemCalls;
    }
  }

  /** Resource monitoring configuration. */
  final class ResourceMonitoringConfig {
    private final Duration samplingInterval;
    private final boolean monitorMemory;
    private final boolean monitorCpu;
    private final boolean monitorIo;
    private final boolean monitorNetwork;
    private final Duration retentionPeriod;

    public ResourceMonitoringConfig(
        Duration samplingInterval,
        boolean monitorMemory,
        boolean monitorCpu,
        boolean monitorIo,
        boolean monitorNetwork,
        Duration retentionPeriod) {
      this.samplingInterval = samplingInterval;
      this.monitorMemory = monitorMemory;
      this.monitorCpu = monitorCpu;
      this.monitorIo = monitorIo;
      this.monitorNetwork = monitorNetwork;
      this.retentionPeriod = retentionPeriod;
    }

    public Duration getSamplingInterval() {
      return samplingInterval;
    }

    public boolean isMonitorMemory() {
      return monitorMemory;
    }

    public boolean isMonitorCpu() {
      return monitorCpu;
    }

    public boolean isMonitorIo() {
      return monitorIo;
    }

    public boolean isMonitorNetwork() {
      return monitorNetwork;
    }

    public Duration getRetentionPeriod() {
      return retentionPeriod;
    }
  }

  /** Resource usage information. */
  final class ResourceUsage {
    private final long currentMemoryBytes;
    private final int currentFileDescriptors;
    private final int currentNetworkConnections;
    private final Duration currentCpuTime;
    private final double cpuUsagePercent;
    private final Instant lastUpdated;

    public ResourceUsage(
        long currentMemoryBytes,
        int currentFileDescriptors,
        int currentNetworkConnections,
        Duration currentCpuTime,
        double cpuUsagePercent,
        Instant lastUpdated) {
      this.currentMemoryBytes = currentMemoryBytes;
      this.currentFileDescriptors = currentFileDescriptors;
      this.currentNetworkConnections = currentNetworkConnections;
      this.currentCpuTime = currentCpuTime;
      this.cpuUsagePercent = cpuUsagePercent;
      this.lastUpdated = lastUpdated;
    }

    public long getCurrentMemoryBytes() {
      return currentMemoryBytes;
    }

    public int getCurrentFileDescriptors() {
      return currentFileDescriptors;
    }

    public int getCurrentNetworkConnections() {
      return currentNetworkConnections;
    }

    public Duration getCurrentCpuTime() {
      return currentCpuTime;
    }

    public double getCpuUsagePercent() {
      return cpuUsagePercent;
    }

    public Instant getLastUpdated() {
      return lastUpdated;
    }
  }

  /** Resource garbage collection result. */
  final class ResourceGarbageCollectionResult {
    private final int freedMemoryBytes;
    private final int closedFileDescriptors;
    private final int closedNetworkConnections;
    private final int cleanedUpResources;
    private final Duration collectionTime;

    public ResourceGarbageCollectionResult(
        int freedMemoryBytes,
        int closedFileDescriptors,
        int closedNetworkConnections,
        int cleanedUpResources,
        Duration collectionTime) {
      this.freedMemoryBytes = freedMemoryBytes;
      this.closedFileDescriptors = closedFileDescriptors;
      this.closedNetworkConnections = closedNetworkConnections;
      this.cleanedUpResources = cleanedUpResources;
      this.collectionTime = collectionTime;
    }

    public int getFreedMemoryBytes() {
      return freedMemoryBytes;
    }

    public int getClosedFileDescriptors() {
      return closedFileDescriptors;
    }

    public int getClosedNetworkConnections() {
      return closedNetworkConnections;
    }

    public int getCleanedUpResources() {
      return cleanedUpResources;
    }

    public Duration getCollectionTime() {
      return collectionTime;
    }
  }

  /** Resource manager statistics. */
  final class ResourceManagerStatistics {
    private final int totalManagedComponents;
    private final int totalSharedResources;
    private final int totalIsolatedEnvironments;
    private final long totalAllocatedMemory;
    private final int totalActiveMonitors;

    public ResourceManagerStatistics(
        int totalManagedComponents,
        int totalSharedResources,
        int totalIsolatedEnvironments,
        long totalAllocatedMemory,
        int totalActiveMonitors) {
      this.totalManagedComponents = totalManagedComponents;
      this.totalSharedResources = totalSharedResources;
      this.totalIsolatedEnvironments = totalIsolatedEnvironments;
      this.totalAllocatedMemory = totalAllocatedMemory;
      this.totalActiveMonitors = totalActiveMonitors;
    }

    public int getTotalManagedComponents() {
      return totalManagedComponents;
    }

    public int getTotalSharedResources() {
      return totalSharedResources;
    }

    public int getTotalIsolatedEnvironments() {
      return totalIsolatedEnvironments;
    }

    public long getTotalAllocatedMemory() {
      return totalAllocatedMemory;
    }

    public int getTotalActiveMonitors() {
      return totalActiveMonitors;
    }
  }

  /** Handle for shared resources. */
  interface SharedResourceHandle {
    String getResourceId();

    SharedResourceType getResourceType();

    boolean isValid();

    void close() throws WasmException;
  }

  /** Handle for isolated environments. */
  interface IsolatedEnvironmentHandle {
    String getComponentId();

    ResourceIsolationConfig getIsolationConfig();

    boolean isValid();

    void close() throws WasmException;
  }

  /** Handle for resource monitoring. */
  interface ResourceMonitoringHandle {
    String getComponentId();

    ResourceMonitoringConfig getConfig();

    ResourceUsage getCurrentUsage();

    boolean isActive();

    void stop() throws WasmException;
  }

  /** Types of shared resources. */
  enum SharedResourceType {
    MEMORY_POOL,
    FILE_CACHE,
    NETWORK_CONNECTION_POOL,
    CONFIGURATION_STORE,
    CUSTOM
  }

  /** Resource access levels. */
  enum ResourceAccessLevel {
    READ_ONLY,
    READ_WRITE,
    WRITE_ONLY,
    FULL_ACCESS
  }

  /** Resource access policies. */
  enum ResourceAccessPolicy {
    UNRESTRICTED,
    COMPONENT_OWNER_ONLY,
    WHITELIST_BASED,
    ROLE_BASED
  }

  /** Resource capabilities. */
  enum ResourceCapability {
    FILE_SYSTEM_ACCESS,
    NETWORK_ACCESS,
    CRYPTO_OPERATIONS,
    GPU_COMPUTE,
    REAL_TIME_SCHEDULING
  }
}
