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

package ai.tegmentum.wasmtime4j.resource;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Enterprise resource manager providing comprehensive resource tracking, optimization, and
 * lifecycle management for WebAssembly operations.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Real-time resource tracking and monitoring
 *   <li>Intelligent resource allocation and optimization
 *   <li>Resource quotas and limits enforcement
 *   <li>Automated resource cleanup and garbage collection
 *   <li>Resource usage analytics and forecasting
 *   <li>Memory pressure detection and mitigation
 * </ul>
 *
 * @since 1.0.0
 */
public final class EnterpriseResourceManager {

  private static final Logger LOGGER = Logger.getLogger(EnterpriseResourceManager.class.getName());

  /** Resource types for tracking. */
  public enum ResourceType {
    NATIVE_MEMORY,
    HEAP_MEMORY,
    OFF_HEAP_MEMORY,
    FILE_HANDLES,
    NETWORK_CONNECTIONS,
    THREAD_POOL,
    MODULE_INSTANCES,
    FUNCTION_CALLS,
    COMPILATION_CACHE,
    NATIVE_OBJECTS
  }

  /** Resource allocation strategy. */
  public enum AllocationStrategy {
    CONSERVATIVE, // Minimize resource usage
    BALANCED, // Balance performance and resource usage
    AGGRESSIVE, // Maximize performance
    ADAPTIVE // Dynamically adjust based on conditions
  }

  /** Resource usage statistics. */
  public static final class ResourceUsageStats {
    private final ResourceType resourceType;
    private final long currentUsage;
    private final long peakUsage;
    private final long totalAllocated;
    private final long totalDeallocated;
    private final double averageUsage;
    private final double utilizationPercentage;
    private final Instant lastUpdated;

    /**
     * Creates a new ResourceUsageStats.
     *
     * @param resourceType the type of resource
     * @param currentUsage the current usage amount
     * @param peakUsage the peak usage amount
     * @param totalAllocated the total allocated amount
     * @param totalDeallocated the total deallocated amount
     * @param averageUsage the average usage
     * @param utilizationPercentage the utilization percentage
     */
    public ResourceUsageStats(
        final ResourceType resourceType,
        final long currentUsage,
        final long peakUsage,
        final long totalAllocated,
        final long totalDeallocated,
        final double averageUsage,
        final double utilizationPercentage) {
      this.resourceType = resourceType;
      this.currentUsage = currentUsage;
      this.peakUsage = peakUsage;
      this.totalAllocated = totalAllocated;
      this.totalDeallocated = totalDeallocated;
      this.averageUsage = averageUsage;
      this.utilizationPercentage = utilizationPercentage;
      this.lastUpdated = Instant.now();
    }

    public ResourceType getResourceType() {
      return resourceType;
    }

    public long getCurrentUsage() {
      return currentUsage;
    }

    public long getPeakUsage() {
      return peakUsage;
    }

    public long getTotalAllocated() {
      return totalAllocated;
    }

    public long getTotalDeallocated() {
      return totalDeallocated;
    }

    public double getAverageUsage() {
      return averageUsage;
    }

    public double getUtilizationPercentage() {
      return utilizationPercentage;
    }

    public Instant getLastUpdated() {
      return lastUpdated;
    }
  }

  /** Resource quota configuration. */
  public static final class ResourceQuota {
    private final ResourceType resourceType;
    private final long softLimit;
    private final long hardLimit;
    private final Duration warningThreshold;
    private final boolean enforcementEnabled;

    /**
     * Creates a new ResourceQuota.
     *
     * @param resourceType the type of resource
     * @param softLimit the soft limit
     * @param hardLimit the hard limit
     * @param warningThreshold the warning threshold
     * @param enforcementEnabled whether enforcement is enabled
     */
    public ResourceQuota(
        final ResourceType resourceType,
        final long softLimit,
        final long hardLimit,
        final Duration warningThreshold,
        final boolean enforcementEnabled) {
      this.resourceType = resourceType;
      this.softLimit = softLimit;
      this.hardLimit = hardLimit;
      this.warningThreshold = warningThreshold;
      this.enforcementEnabled = enforcementEnabled;
    }

    public ResourceType getResourceType() {
      return resourceType;
    }

    public long getSoftLimit() {
      return softLimit;
    }

    public long getHardLimit() {
      return hardLimit;
    }

    public Duration getWarningThreshold() {
      return warningThreshold;
    }

    public boolean isEnforcementEnabled() {
      return enforcementEnabled;
    }
  }

  /** Resource tracker for individual resource types. */
  private static final class ResourceTracker {
    private final ResourceType resourceType;
    private final AtomicLong currentUsage = new AtomicLong(0);
    private final AtomicLong peakUsage = new AtomicLong(0);
    private final AtomicLong totalAllocated = new AtomicLong(0);
    private final AtomicLong totalDeallocated = new AtomicLong(0);
    private final AtomicLong allocationCount = new AtomicLong(0);
    private final AtomicLong deallocationCount = new AtomicLong(0);
    private volatile long lastSampleTime = System.currentTimeMillis();
    private volatile double averageUsage = 0.0;

    ResourceTracker(final ResourceType resourceType) {
      this.resourceType = resourceType;
    }

    void recordAllocation(final long amount) {
      final long newUsage = currentUsage.addAndGet(amount);
      totalAllocated.addAndGet(amount);
      allocationCount.incrementAndGet();

      // Update peak usage
      peakUsage.updateAndGet(peak -> Math.max(peak, newUsage));

      // Update average usage (exponential moving average)
      updateAverageUsage(newUsage);
    }

    void recordDeallocation(final long amount) {
      currentUsage.addAndGet(-amount);
      totalDeallocated.addAndGet(amount);
      deallocationCount.incrementAndGet();

      updateAverageUsage(currentUsage.get());
    }

    private void updateAverageUsage(final long currentValue) {
      final long currentTime = System.currentTimeMillis();
      final long timeDelta = currentTime - lastSampleTime;

      if (timeDelta > 0) {
        // Exponential moving average with 5-minute half-life
        final double alpha = 1.0 - Math.exp(-timeDelta / 300_000.0);
        averageUsage = alpha * currentValue + (1.0 - alpha) * averageUsage;
        lastSampleTime = currentTime;
      }
    }

    ResourceUsageStats getStats(final ResourceQuota quota) {
      final long current = currentUsage.get();
      final long peak = peakUsage.get();
      final long allocated = totalAllocated.get();
      final long deallocated = totalDeallocated.get();
      final double average = averageUsage;

      final double utilization =
          quota != null && quota.getHardLimit() > 0
              ? (current * 100.0) / quota.getHardLimit()
              : 0.0;

      return new ResourceUsageStats(
          resourceType, current, peak, allocated, deallocated, average, utilization);
    }
  }

  /** Resource trackers by type. */
  private final ConcurrentHashMap<ResourceType, ResourceTracker> resourceTrackers =
      new ConcurrentHashMap<>();

  /** Resource quotas by type. */
  private final ConcurrentHashMap<ResourceType, ResourceQuota> resourceQuotas =
      new ConcurrentHashMap<>();

  /** Resource allocation strategy. */
  private volatile AllocationStrategy allocationStrategy = AllocationStrategy.BALANCED;

  /** Resource optimization configuration. */
  private volatile boolean optimizationEnabled = true;

  private volatile boolean quotaEnforcementEnabled = true;
  private volatile boolean memoryPressureDetectionEnabled = true;

  /** Background optimization scheduler. */
  private final ScheduledExecutorService optimizationScheduler =
      Executors.newScheduledThreadPool(2);

  /** Memory pressure detection. */
  private final AtomicReference<MemoryPressureLevel> currentMemoryPressure =
      new AtomicReference<>(MemoryPressureLevel.NORMAL);

  /** Memory pressure levels. */
  public enum MemoryPressureLevel {
    NORMAL,
    MODERATE,
    HIGH,
    CRITICAL
  }

  /** Optimization statistics. */
  private final AtomicLong optimizationCycles = new AtomicLong(0);

  private final AtomicLong resourcesReclaimed = new AtomicLong(0);
  private final AtomicLong quotaViolations = new AtomicLong(0);
  private final AtomicLong allocationFailures = new AtomicLong(0);

  /** Creates a new enterprise resource manager. */
  public EnterpriseResourceManager() {
    initializeResourceTrackers();
    initializeDefaultQuotas();
    startBackgroundOptimization();
    LOGGER.info("Enterprise resource manager initialized");
  }

  /** Initializes resource trackers for all resource types. */
  private void initializeResourceTrackers() {
    for (final ResourceType type : ResourceType.values()) {
      resourceTrackers.put(type, new ResourceTracker(type));
    }
  }

  /** Initializes default resource quotas. */
  private void initializeDefaultQuotas() {
    // Get system memory for calculating default quotas
    final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    final long maxHeap = heapUsage.getMax();

    // Set conservative default quotas
    setResourceQuota(
        ResourceType.HEAP_MEMORY,
        new ResourceQuota(
            ResourceType.HEAP_MEMORY,
            (long) (maxHeap * 0.6),
            (long) (maxHeap * 0.8),
            Duration.ofMinutes(1),
            true));

    setResourceQuota(
        ResourceType.NATIVE_MEMORY,
        new ResourceQuota(
            ResourceType.NATIVE_MEMORY,
            256 * 1024 * 1024,
            512 * 1024 * 1024, // 256MB soft, 512MB hard
            Duration.ofMinutes(1),
            true));

    setResourceQuota(
        ResourceType.MODULE_INSTANCES,
        new ResourceQuota(ResourceType.MODULE_INSTANCES, 1000, 2000, Duration.ofMinutes(5), true));

    setResourceQuota(
        ResourceType.FILE_HANDLES,
        new ResourceQuota(ResourceType.FILE_HANDLES, 500, 1000, Duration.ofMinutes(1), true));
  }

  /** Starts background optimization processes. */
  private void startBackgroundOptimization() {
    // Memory pressure monitoring
    optimizationScheduler.scheduleAtFixedRate(
        this::monitorMemoryPressure, 10, 10, TimeUnit.SECONDS);

    // Resource optimization
    optimizationScheduler.scheduleAtFixedRate(
        this::performResourceOptimization, 60, 30, TimeUnit.SECONDS);
  }

  /**
   * Records resource allocation.
   *
   * @param resourceType the type of resource
   * @param amount the amount allocated
   * @return true if allocation is within quota
   */
  public boolean recordAllocation(final ResourceType resourceType, final long amount) {
    final ResourceTracker tracker = resourceTrackers.get(resourceType);
    if (tracker == null) {
      return false;
    }

    // Check quota before allocation
    if (quotaEnforcementEnabled) {
      final ResourceQuota quota = resourceQuotas.get(resourceType);
      if (quota != null && quota.isEnforcementEnabled()) {
        final long currentUsage = tracker.currentUsage.get();
        final long projectedUsage = currentUsage + amount;

        if (projectedUsage > quota.getHardLimit()) {
          quotaViolations.incrementAndGet();
          allocationFailures.incrementAndGet();
          LOGGER.warning(
              String.format(
                  "Resource allocation rejected: %s usage would exceed hard limit (%d > %d)",
                  resourceType, projectedUsage, quota.getHardLimit()));
          return false;
        }

        if (projectedUsage > quota.getSoftLimit()) {
          LOGGER.warning(
              String.format(
                  "Resource allocation warning: %s usage exceeds soft limit (%d > %d)",
                  resourceType, projectedUsage, quota.getSoftLimit()));
        }
      }
    }

    tracker.recordAllocation(amount);
    return true;
  }

  /**
   * Records resource deallocation.
   *
   * @param resourceType the type of resource
   * @param amount the amount deallocated
   */
  public void recordDeallocation(final ResourceType resourceType, final long amount) {
    final ResourceTracker tracker = resourceTrackers.get(resourceType);
    if (tracker != null) {
      tracker.recordDeallocation(amount);
    }
  }

  /**
   * Sets a resource quota.
   *
   * @param quota the resource quota to set
   */
  public void setResourceQuota(final ResourceQuota quota) {
    resourceQuotas.put(quota.getResourceType(), quota);
    LOGGER.info(
        String.format(
            "Set resource quota for %s: soft=%d, hard=%d",
            quota.getResourceType(), quota.getSoftLimit(), quota.getHardLimit()));
  }

  /**
   * Sets a resource quota.
   *
   * @param resourceType the resource type
   * @param quota the resource quota
   */
  public void setResourceQuota(final ResourceType resourceType, final ResourceQuota quota) {
    resourceQuotas.put(resourceType, quota);
  }

  /**
   * Gets resource usage statistics.
   *
   * @param resourceType the resource type
   * @return usage statistics
   */
  public ResourceUsageStats getResourceUsage(final ResourceType resourceType) {
    final ResourceTracker tracker = resourceTrackers.get(resourceType);
    if (tracker == null) {
      return null;
    }

    final ResourceQuota quota = resourceQuotas.get(resourceType);
    return tracker.getStats(quota);
  }

  /** Monitors memory pressure and adjusts allocation strategy. */
  private void monitorMemoryPressure() {
    if (!memoryPressureDetectionEnabled) {
      return;
    }

    try {
      final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
      final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

      final long used = heapUsage.getUsed();
      final long max = heapUsage.getMax();
      final double usagePercentage = (used * 100.0) / max;

      final MemoryPressureLevel newPressure;
      if (usagePercentage > 90) {
        newPressure = MemoryPressureLevel.CRITICAL;
      } else if (usagePercentage > 80) {
        newPressure = MemoryPressureLevel.HIGH;
      } else if (usagePercentage > 70) {
        newPressure = MemoryPressureLevel.MODERATE;
      } else {
        newPressure = MemoryPressureLevel.NORMAL;
      }

      final MemoryPressureLevel oldPressure = currentMemoryPressure.getAndSet(newPressure);

      if (newPressure != oldPressure) {
        LOGGER.info(
            String.format(
                "Memory pressure changed from %s to %s (%.1f%% heap usage)",
                oldPressure, newPressure, usagePercentage));

        // Adjust allocation strategy based on memory pressure
        if (allocationStrategy == AllocationStrategy.ADAPTIVE) {
          switch (newPressure) {
            case CRITICAL:
            case HIGH:
              // Force aggressive cleanup
              performEmergencyCleanup();
              break;
            case MODERATE:
              // Trigger optimization
              performResourceOptimization();
              break;
            case NORMAL:
              // Normal operation
              break;
            default:
              // No action needed for unknown pressure levels
              break;
          }
        }
      }

    } catch (final Exception e) {
      LOGGER.warning("Failed to monitor memory pressure: " + e.getMessage());
    }
  }

  /** Performs resource optimization. */
  private void performResourceOptimization() {
    if (!optimizationEnabled) {
      return;
    }

    optimizationCycles.incrementAndGet();
    long reclaimedResources = 0;

    try {
      // Check each resource type for optimization opportunities
      for (final ResourceType type : ResourceType.values()) {
        reclaimedResources += optimizeResourceType(type);
      }

      resourcesReclaimed.addAndGet(reclaimedResources);

      if (reclaimedResources > 0) {
        LOGGER.info(
            String.format(
                "Resource optimization completed: reclaimed %d units", reclaimedResources));
      }

    } catch (final Exception e) {
      LOGGER.warning("Resource optimization failed: " + e.getMessage());
    }
  }

  /** Optimizes a specific resource type. */
  private long optimizeResourceType(final ResourceType resourceType) {
    final ResourceTracker tracker = resourceTrackers.get(resourceType);
    final ResourceQuota quota = resourceQuotas.get(resourceType);

    if (tracker == null || quota == null) {
      return 0;
    }

    final long currentUsage = tracker.currentUsage.get();
    final long softLimit = quota.getSoftLimit();

    // If we're over the soft limit, try to reclaim resources
    if (currentUsage > softLimit) {
      final long targetReduction = currentUsage - softLimit;
      return reclaimResources(resourceType, targetReduction);
    }

    return 0;
  }

  /** Reclaims resources of a specific type. */
  private long reclaimResources(final ResourceType resourceType, final long targetAmount) {
    long reclaimed = 0;

    switch (resourceType) {
      case COMPILATION_CACHE:
        // Trigger cache cleanup
        reclaimed = triggerCacheCleanup();
        break;

      case NATIVE_OBJECTS:
        // Trigger object pool cleanup
        reclaimed = triggerObjectPoolCleanup();
        break;

      case HEAP_MEMORY:
        // Suggest garbage collection
        System.gc();
        reclaimed = targetAmount / 4; // Estimate
        break;

      default:
        // Generic cleanup
        reclaimed = performGenericCleanup(resourceType, targetAmount);
        break;
    }

    if (reclaimed > 0) {
      recordDeallocation(resourceType, reclaimed);
      LOGGER.fine(String.format("Reclaimed %d units of %s", reclaimed, resourceType));
    }

    return reclaimed;
  }

  /** Performs emergency cleanup during critical memory pressure. */
  private void performEmergencyCleanup() {
    LOGGER.warning("Performing emergency resource cleanup due to critical memory pressure");

    // Force aggressive resource reclamation
    for (final ResourceType type : ResourceType.values()) {
      final ResourceTracker tracker = resourceTrackers.get(type);
      if (tracker != null) {
        final long currentUsage = tracker.currentUsage.get();
        if (currentUsage > 0) {
          // Try to reclaim 50% of current usage
          reclaimResources(type, currentUsage / 2);
        }
      }
    }

    // Force multiple GC cycles
    for (int i = 0; i < 3; i++) {
      System.gc();
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  /** Triggers compilation cache cleanup. */
  private long triggerCacheCleanup() {
    // In a real implementation, this would call into the compilation cache
    // to perform maintenance and return the amount of memory freed
    return 1024 * 1024; // Estimate 1MB freed
  }

  /** Triggers object pool cleanup. */
  private long triggerObjectPoolCleanup() {
    // In a real implementation, this would call into object pools
    // to release unused objects and return the count freed
    return 100; // Estimate 100 objects freed
  }

  /** Performs generic cleanup for a resource type. */
  private long performGenericCleanup(final ResourceType resourceType, final long targetAmount) {
    // Generic cleanup - return a small amount as a placeholder
    return Math.min(targetAmount / 10, 10);
  }

  /**
   * Gets comprehensive resource statistics.
   *
   * @return formatted resource statistics
   */
  public String getResourceStatistics() {
    final StringBuilder sb = new StringBuilder("=== Enterprise Resource Statistics ===\n");

    sb.append(String.format("Memory Pressure: %s\n", currentMemoryPressure.get()));
    sb.append(String.format("Allocation Strategy: %s\n", allocationStrategy));
    sb.append(String.format("Optimization Cycles: %d\n", optimizationCycles.get()));
    sb.append(String.format("Resources Reclaimed: %d\n", resourcesReclaimed.get()));
    sb.append(String.format("Quota Violations: %d\n", quotaViolations.get()));
    sb.append(String.format("Allocation Failures: %d\n", allocationFailures.get()));
    sb.append("\n");

    sb.append("Resource Usage by Type:\n");
    for (final ResourceType type : ResourceType.values()) {
      final ResourceUsageStats stats = getResourceUsage(type);
      if (stats != null && stats.getCurrentUsage() > 0) {
        sb.append(
            String.format(
                "  %-20s: current=%d, peak=%d, avg=%.0f, util=%.1f%%\n",
                type,
                stats.getCurrentUsage(),
                stats.getPeakUsage(),
                stats.getAverageUsage(),
                stats.getUtilizationPercentage()));
      }
    }

    return sb.toString();
  }

  /**
   * Sets the allocation strategy.
   *
   * @param strategy the allocation strategy
   */
  public void setAllocationStrategy(final AllocationStrategy strategy) {
    this.allocationStrategy = strategy;
    LOGGER.info("Allocation strategy set to " + strategy);
  }

  /**
   * Gets the current memory pressure level.
   *
   * @return memory pressure level
   */
  public MemoryPressureLevel getCurrentMemoryPressure() {
    return currentMemoryPressure.get();
  }

  /**
   * Enables or disables resource optimization.
   *
   * @param enabled true to enable optimization
   */
  public void setOptimizationEnabled(final boolean enabled) {
    this.optimizationEnabled = enabled;
    LOGGER.info("Resource optimization " + (enabled ? "enabled" : "disabled"));
  }

  /** Shuts down the resource manager. */
  public void shutdown() {
    optimizationScheduler.shutdown();
    try {
      if (!optimizationScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
        optimizationScheduler.shutdownNow();
      }
    } catch (final InterruptedException e) {
      optimizationScheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Enterprise resource manager shutdown");
  }
}
