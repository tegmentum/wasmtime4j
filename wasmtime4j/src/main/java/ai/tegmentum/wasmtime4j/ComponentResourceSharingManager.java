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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced component resource sharing and isolation system.
 *
 * <p>The ComponentResourceSharingManager provides sophisticated resource management capabilities
 * for components including:
 *
 * <ul>
 *   <li>Shared resource pools between components
 *   <li>Resource isolation and sandboxing
 *   <li>Component-specific resource quotas
 *   <li>Cross-component resource communication
 *   <li>Resource lifecycle management
 *   <li>Dynamic resource allocation and deallocation
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentResourceSharingManager extends AutoCloseable {

  /**
   * Gets the resource manager identifier.
   *
   * @return the manager ID
   */
  String getId();

  /**
   * Gets the resource manager configuration.
   *
   * @return the configuration
   */
  ResourceSharingConfig getConfiguration();

  /**
   * Creates a shared resource pool for components.
   *
   * @param poolName the name of the resource pool
   * @param resourceType the type of resources in the pool
   * @param poolConfig pool configuration
   * @return the created resource pool
   * @throws WasmException if pool creation fails
   */
  ResourcePool createResourcePool(
      String poolName, ResourceType resourceType, ResourcePoolConfig poolConfig)
      throws WasmException;

  /**
   * Gets an existing resource pool.
   *
   * @param poolName the name of the resource pool
   * @return the resource pool, if it exists
   */
  Optional<ResourcePool> getResourcePool(String poolName);

  /**
   * Removes a resource pool and releases all its resources.
   *
   * @param poolName the name of the resource pool to remove
   * @throws WasmException if pool removal fails
   */
  void removeResourcePool(String poolName) throws WasmException;

  /**
   * Allocates resources to a component from a shared pool.
   *
   * @param component the component requesting resources
   * @param poolName the name of the resource pool
   * @param allocationRequest the resource allocation request
   * @return future containing the resource allocation result
   * @throws WasmException if allocation fails
   */
  CompletableFuture<ResourceAllocation> allocateResources(
      Component component, String poolName, ResourceAllocationRequest allocationRequest)
      throws WasmException;

  /**
   * Deallocates resources from a component back to the shared pool.
   *
   * @param allocation the resource allocation to release
   * @return future that completes when deallocation is finished
   * @throws WasmException if deallocation fails
   */
  CompletableFuture<Void> deallocateResources(ResourceAllocation allocation) throws WasmException;

  /**
   * Sets up resource isolation for a component.
   *
   * @param component the component to isolate
   * @param isolationConfig isolation configuration
   * @return the resource isolation context
   * @throws WasmException if isolation setup fails
   */
  ResourceIsolationContext setupResourceIsolation(
      Component component, ResourceIsolationConfig isolationConfig) throws WasmException;

  /**
   * Removes resource isolation for a component.
   *
   * @param component the component to remove isolation from
   * @throws WasmException if isolation removal fails
   */
  void removeResourceIsolation(Component component) throws WasmException;

  /**
   * Sets resource quotas for a component.
   *
   * @param component the component to set quotas for
   * @param quotas the resource quotas to apply
   * @throws WasmException if quota setting fails
   */
  void setResourceQuotas(Component component, ResourceQuotas quotas) throws WasmException;

  /**
   * Gets the current resource quotas for a component.
   *
   * @param component the component to check
   * @return the component's resource quotas
   */
  Optional<ResourceQuotas> getResourceQuotas(Component component);

  /**
   * Updates resource quotas for a component.
   *
   * @param component the component to update quotas for
   * @param quotaUpdates the quota updates to apply
   * @throws WasmException if quota update fails
   */
  void updateResourceQuotas(Component component, ResourceQuotaUpdates quotaUpdates)
      throws WasmException;

  /**
   * Gets current resource usage for a component.
   *
   * @param component the component to check
   * @return the component's current resource usage
   */
  ResourceUsage getResourceUsage(Component component);

  /**
   * Sets up cross-component resource communication.
   *
   * @param sourceComponent the source component
   * @param targetComponent the target component
   * @param communicationConfig communication configuration
   * @return the resource communication channel
   * @throws WasmException if communication setup fails
   */
  ResourceCommunicationChannel setupResourceCommunication(
      Component sourceComponent,
      Component targetComponent,
      ResourceCommunicationConfig communicationConfig)
      throws WasmException;

  /**
   * Removes cross-component resource communication.
   *
   * @param channel the communication channel to remove
   * @throws WasmException if communication removal fails
   */
  void removeResourceCommunication(ResourceCommunicationChannel channel) throws WasmException;

  /**
   * Monitors resource usage and sends alerts on threshold breaches.
   *
   * @param component the component to monitor
   * @param monitoringConfig monitoring configuration
   * @return the resource monitor
   * @throws WasmException if monitoring setup fails
   */
  ResourceMonitor startResourceMonitoring(
      Component component, ResourceMonitoringConfig monitoringConfig) throws WasmException;

  /**
   * Stops resource monitoring for a component.
   *
   * @param monitor the resource monitor to stop
   * @throws WasmException if monitoring stop fails
   */
  void stopResourceMonitoring(ResourceMonitor monitor) throws WasmException;

  /**
   * Performs garbage collection on unused resources.
   *
   * @param gcConfig garbage collection configuration
   * @return garbage collection result
   * @throws WasmException if garbage collection fails
   */
  ResourceGarbageCollectionResult performGarbageCollection(ResourceGCConfig gcConfig)
      throws WasmException;

  /**
   * Creates a resource snapshot for backup or migration.
   *
   * @param component the component to snapshot
   * @param snapshotConfig snapshot configuration
   * @return the resource snapshot
   * @throws WasmException if snapshot creation fails
   */
  ResourceSnapshot createResourceSnapshot(
      Component component, ResourceSnapshotConfig snapshotConfig) throws WasmException;

  /**
   * Restores resources from a snapshot.
   *
   * @param component the component to restore resources for
   * @param snapshot the resource snapshot to restore from
   * @return future that completes when restoration is finished
   * @throws WasmException if restoration fails
   */
  CompletableFuture<Void> restoreResourceSnapshot(
      Component component, ResourceSnapshot snapshot) throws WasmException;

  /**
   * Gets all active resource pools.
   *
   * @return list of active resource pools
   */
  List<ResourcePool> getActiveResourcePools();

  /**
   * Gets resource manager statistics.
   *
   * @return resource manager statistics
   */
  ResourceSharingStatistics getStatistics();

  /**
   * Sets a resource event listener for monitoring resource events.
   *
   * @param listener the resource event listener
   */
  void setResourceEventListener(ResourceEventListener listener);

  /** Removes the resource event listener. */
  void removeResourceEventListener();

  /**
   * Starts the resource manager services.
   *
   * @throws WasmException if startup fails
   */
  void start() throws WasmException;

  /**
   * Stops the resource manager services.
   *
   * @throws WasmException if shutdown fails
   */
  void stop() throws WasmException;

  @Override
  void close();

  /** Resource pool for sharing resources between components. */
  interface ResourcePool {
    String getName();

    ResourceType getResourceType();

    ResourcePoolConfig getConfiguration();

    int getTotalCapacity();

    int getAvailableCapacity();

    int getUsedCapacity();

    List<ResourceAllocation> getActiveAllocations();

    ResourcePoolStatistics getStatistics();

    boolean isHealthy();
  }

  /** Resource allocation assigned to a component. */
  interface ResourceAllocation {
    String getAllocationId();

    Component getComponent();

    String getPoolName();

    ResourceType getResourceType();

    int getAllocatedAmount();

    Instant getAllocationTime();

    Optional<Instant> getExpirationTime();

    ResourceAllocationStatus getStatus();

    Map<String, Object> getResourceProperties();

    void release() throws WasmException;
  }

  /** Resource isolation context for a component. */
  interface ResourceIsolationContext {
    String getContextId();

    Component getComponent();

    ResourceIsolationLevel getIsolationLevel();

    Set<ResourceType> getIsolatedResources();

    Map<String, Object> getIsolationProperties();

    boolean isActive();
  }

  /** Resource quotas for limiting component resource usage. */
  interface ResourceQuotas {
    Map<ResourceType, ResourceQuota> getQuotas();

    boolean hasQuotaFor(ResourceType resourceType);

    ResourceQuota getQuota(ResourceType resourceType);

    boolean isWithinQuotas(ResourceUsage usage);

    List<QuotaViolation> checkViolations(ResourceUsage usage);
  }

  /** Individual resource quota specification. */
  interface ResourceQuota {
    ResourceType getResourceType();

    long getMaxAmount();

    Optional<Long> getWarningThreshold();

    Duration getTimeWindow();

    QuotaEnforcementPolicy getEnforcementPolicy();

    boolean isExceeded(long currentUsage);
  }

  /** Resource usage information for a component. */
  interface ResourceUsage {
    Component getComponent();

    Instant getMeasurementTime();

    Map<ResourceType, ResourceUsageEntry> getUsageByType();

    long getTotalMemoryUsage();

    long getTotalCpuTime();

    int getActiveConnections();

    long getFileSystemUsage();

    double getResourceEfficiency();
  }

  /** Resource usage entry for a specific resource type. */
  interface ResourceUsageEntry {
    ResourceType getResourceType();

    long getCurrentUsage();

    long getPeakUsage();

    long getAverageUsage();

    long getTotalUsage();

    Duration getMeasurementPeriod();
  }

  /** Resource communication channel between components. */
  interface ResourceCommunicationChannel {
    String getChannelId();

    Component getSourceComponent();

    Component getTargetComponent();

    ResourceCommunicationType getType();

    boolean isActive();

    long getMessageCount();

    void sendMessage(ResourceMessage message) throws WasmException;

    void close() throws WasmException;
  }

  /** Resource monitoring system for a component. */
  interface ResourceMonitor {
    String getMonitorId();

    Component getComponent();

    ResourceMonitoringConfig getConfiguration();

    ResourceUsage getCurrentUsage();

    List<ResourceAlert> getActiveAlerts();

    ResourceMonitoringStatistics getStatistics();

    void setThreshold(ResourceType resourceType, long threshold);

    void pause();

    void resume();

    void stop();
  }

  /** Resource alert for threshold breaches. */
  interface ResourceAlert {
    String getAlertId();

    ResourceType getResourceType();

    AlertSeverity getSeverity();

    String getMessage();

    long getCurrentValue();

    long getThresholdValue();

    Instant getTriggeredTime();

    boolean isActive();

    void acknowledge();
  }

  /** Resource snapshot for backup/restore operations. */
  interface ResourceSnapshot {
    String getSnapshotId();

    Component getComponent();

    Instant getCreationTime();

    Map<ResourceType, byte[]> getResourceData();

    long getTotalSize();

    String getChecksum();

    ResourceSnapshotMetadata getMetadata();
  }

  /** Resource garbage collection result. */
  interface ResourceGarbageCollectionResult {
    long getFreedMemory();

    int getCollectedObjects();

    Duration getCollectionTime();

    List<String> getCollectedResourceIds();

    Map<ResourceType, Long> getFreedByType();

    boolean wasSuccessful();
  }

  /** Resource manager statistics. */
  interface ResourceSharingStatistics {
    int getTotalResourcePools();

    int getActiveAllocations();

    long getTotalManagedMemory();

    long getAvailableMemory();

    double getMemoryUtilization();

    int getActiveMonitors();

    long getTotalAlerts();

    Map<ResourceType, Long> getAllocationsByType();

    double getAverageAllocationTime();

    double getResourceSharingEfficiency();

    int getCrossComponentCommunications();
  }

  /** Resource event listener interface. */
  interface ResourceEventListener {
    void onResourceAllocated(ResourceAllocation allocation);

    void onResourceDeallocated(ResourceAllocation allocation);

    void onResourceThresholdExceeded(
        Component component, ResourceType resourceType, long currentUsage, long threshold);

    void onResourceQuotaExceeded(Component component, QuotaViolation violation);

    void onResourcePoolCreated(ResourcePool pool);

    void onResourcePoolDestroyed(String poolName);

    void onResourceIsolationSetup(Component component, ResourceIsolationContext context);

    void onResourceCommunicationEstablished(ResourceCommunicationChannel channel);
  }

  // Enums and supporting types
  /** Types of resources that can be shared between components. */
  enum ResourceType {
    MEMORY,
    CPU,
    NETWORK,
    FILE_SYSTEM,
    THREADS,
    SOCKETS,
    HANDLES,
    CUSTOM
  }

  /** Status of resource allocation operations. */
  enum ResourceAllocationStatus {
    ACTIVE,
    EXPIRED,
    RELEASED,
    FAILED
  }

  /** Resource isolation levels for component sandboxing. */
  enum ResourceIsolationLevel {
    NONE,
    BASIC,
    MODERATE,
    STRONG,
    COMPLETE
  }

  /** Quota enforcement policies for resource limit violations. */
  enum QuotaEnforcementPolicy {
    WARN_ONLY,
    THROTTLE,
    REJECT,
    TERMINATE
  }

  /** Types of resource communication between components. */
  enum ResourceCommunicationType {
    MESSAGE_PASSING,
    SHARED_MEMORY,
    EVENT_DRIVEN,
    STREAM
  }

  /** Severity levels for resource monitoring alerts. */
  enum AlertSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
  }

  /** Quota violation information. */
  interface QuotaViolation {
    ResourceType getResourceType();

    long getCurrentUsage();

    long getQuotaLimit();

    double getViolationPercentage();

    Instant getViolationTime();

    QuotaEnforcementPolicy getEnforcementAction();
  }

  /** Resource message for component communication. */
  interface ResourceMessage {
    String getMessageId();

    ResourceMessageType getType();

    byte[] getPayload();

    Map<String, Object> getHeaders();

    Instant getTimestamp();

    Optional<Duration> getTimeToLive();
  }

  /** Types of resource messages for component communication. */
  enum ResourceMessageType {
    DATA,
    CONTROL,
    HEARTBEAT,
    ERROR
  }

  // Configuration interfaces
  /** Configuration for resource sharing manager. */
  interface ResourceSharingConfig {
    boolean isSharedPoolsEnabled();

    boolean isResourceIsolationEnabled();

    boolean isResourceMonitoringEnabled();

    Duration getDefaultAllocationTimeout();

    int getMaxConcurrentAllocations();
  }

  /** Configuration for resource pools. */
  interface ResourcePoolConfig {
    int getInitialCapacity();

    int getMaxCapacity();

    boolean isDynamicResizing();

    Duration getResourceTimeout();

    ResourceSharingPolicy getSharingPolicy();
  }

  /** Resource sharing policies for pool allocation. */
  enum ResourceSharingPolicy {
    EXCLUSIVE,
    SHARED,
    ROUND_ROBIN,
    PRIORITY_BASED
  }

  /** Request for resource allocation from a pool. */
  interface ResourceAllocationRequest {
    ResourceType getResourceType();

    int getRequestedAmount();

    Duration getRequestTimeout();

    int getPriority();

    Map<String, Object> getRequirements();
  }

  /** Configuration for component resource isolation. */
  interface ResourceIsolationConfig {
    ResourceIsolationLevel getLevel();

    Set<ResourceType> getIsolatedTypes();

    boolean isStrictIsolation();

    Map<String, Object> getIsolationParameters();
  }

  /** Updates to resource quotas for a component. */
  interface ResourceQuotaUpdates {
    Map<ResourceType, Long> getUpdatedLimits();

    Map<ResourceType, Long> getUpdatedWarningThresholds();

    boolean isIncrementalUpdate();
  }

  /** Configuration for cross-component resource communication. */
  interface ResourceCommunicationConfig {
    ResourceCommunicationType getType();

    int getBufferSize();

    Duration getTimeout();

    boolean isSecureChannel();

    Map<String, Object> getChannelProperties();
  }

  /** Configuration for resource monitoring system. */
  interface ResourceMonitoringConfig {
    Set<ResourceType> getMonitoredTypes();

    Duration getSamplingInterval();

    Map<ResourceType, Long> getThresholds();

    boolean isRealTimeMonitoring();

    AlertSeverity getMinAlertSeverity();
  }

  /** Configuration for resource garbage collection. */
  interface ResourceGCConfig {
    boolean isAggressiveCollection();

    Duration getMaxPauseTime();

    Set<ResourceType> getTargetTypes();

    double getUtilizationThreshold();
  }

  /** Configuration for resource snapshot creation. */
  interface ResourceSnapshotConfig {
    Set<ResourceType> getIncludedTypes();

    boolean isCompressed();

    boolean includeMetadata();

    String getCompressionAlgorithm();
  }

  /** Metadata for resource snapshots. */
  interface ResourceSnapshotMetadata {
    String getVersion();

    Instant getCreationTime();

    Component getSourceComponent();

    Map<String, Object> getProperties();

    String getChecksum();
  }

  /** Statistics for resource pool performance. */
  interface ResourcePoolStatistics {
    String getPoolName();

    long getTotalAllocations();

    long getSuccessfulAllocations();

    long getFailedAllocations();

    double getUtilizationRate();

    Duration getAverageAllocationTime();

    Instant getLastAllocation();
  }

  /** Statistics for resource monitoring activity. */
  interface ResourceMonitoringStatistics {
    Component getComponent();

    Duration getMonitoringDuration();

    long getTotalSamples();

    long getAlertsTriggered();

    Map<ResourceType, Long> getPeakUsageByType();

    double getAverageUtilization();
  }
}
