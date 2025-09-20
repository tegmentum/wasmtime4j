package ai.tegmentum.wasmtime4j.wasi.impl;

import ai.tegmentum.wasmtime4j.wasi.WasiResourceUsageStats;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of WasiResourceUsageStats for tracking resource usage metrics.
 *
 * <p>This implementation provides thread-safe statistics collection for WASI resource usage
 * including creation/release counts, access patterns, and performance metrics.
 *
 * @since 1.0.0
 */
public final class WasiResourceUsageStatsImpl implements WasiResourceUsageStats {

  private final Instant startTime = Instant.now();
  private final ConcurrentHashMap<String, AtomicLong> resourceCreationCounts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> resourceReleaseCounts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> resourceAccessCounts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Instant> lastAccessTimes = new ConcurrentHashMap<>();

  private final AtomicLong totalResourcesCreated = new AtomicLong(0);
  private final AtomicLong totalResourcesReleased = new AtomicLong(0);
  private final AtomicLong totalResourceAccesses = new AtomicLong(0);
  private final AtomicLong totalMemoryUsed = new AtomicLong(0);

  @Override
  public long getTotalResourcesCreated() {
    return totalResourcesCreated.get();
  }

  @Override
  public long getTotalResourcesReleased() {
    return totalResourcesReleased.get();
  }

  @Override
  public long getCurrentActiveResources() {
    return totalResourcesCreated.get() - totalResourcesReleased.get();
  }

  @Override
  public long getTotalResourceAccesses() {
    return totalResourceAccesses.get();
  }

  @Override
  public Map<String, Long> getResourceCreationCountsByType() {
    final Map<String, Long> result = new HashMap<>();
    for (final Map.Entry<String, AtomicLong> entry : resourceCreationCounts.entrySet()) {
      result.put(entry.getKey(), entry.getValue().get());
    }
    return Collections.unmodifiableMap(result);
  }

  @Override
  public Map<String, Long> getResourceAccessCountsByName() {
    final Map<String, Long> result = new HashMap<>();
    for (final Map.Entry<String, AtomicLong> entry : resourceAccessCounts.entrySet()) {
      result.put(entry.getKey(), entry.getValue().get());
    }
    return Collections.unmodifiableMap(result);
  }

  @Override
  public long getTotalMemoryUsage() {
    return totalMemoryUsed.get();
  }

  @Override
  public Duration getTotalUptime() {
    return Duration.between(startTime, Instant.now());
  }

  @Override
  public double getAverageResourceLifetime() {
    final long released = totalResourcesReleased.get();
    if (released == 0) {
      return 0.0;
    }

    // This is a simplified calculation - in a real implementation you'd track
    // actual lifetimes of individual resources
    final Duration uptime = getTotalUptime();
    return uptime.toMillis() / (double) released;
  }

  @Override
  public long getPeakMemoryUsage() {
    // For now, return current usage as peak - in real implementation this would
    // track the actual peak over time
    return totalMemoryUsed.get();
  }

  @Override
  public Map<String, Object> getCustomMetrics() {
    final Map<String, Object> metrics = new HashMap<>();
    metrics.put("start_time", startTime);
    metrics.put("resource_types_seen", resourceCreationCounts.size());
    metrics.put("most_accessed_resource", getMostAccessedResource());
    return Collections.unmodifiableMap(metrics);
  }

  @Override
  public void reset() {
    resourceCreationCounts.clear();
    resourceReleaseCounts.clear();
    resourceAccessCounts.clear();
    lastAccessTimes.clear();

    totalResourcesCreated.set(0);
    totalResourcesReleased.set(0);
    totalResourceAccesses.set(0);
    totalMemoryUsed.set(0);
  }

  /**
   * Records that a resource was created.
   *
   * @param resourceType the type of resource created
   */
  public void recordResourceCreated(final String resourceType) {
    totalResourcesCreated.incrementAndGet();
    resourceCreationCounts.computeIfAbsent(resourceType, k -> new AtomicLong(0)).incrementAndGet();
  }

  /**
   * Records that a resource was released.
   *
   * @param resourceName the name of the resource released
   */
  public void recordResourceReleased(final String resourceName) {
    totalResourcesReleased.incrementAndGet();

    // Remove from access tracking since resource is no longer available
    resourceAccessCounts.remove(resourceName);
    lastAccessTimes.remove(resourceName);
  }

  /**
   * Records that a resource was accessed.
   *
   * @param resourceName the name of the resource accessed
   */
  public void recordResourceAccessed(final String resourceName) {
    totalResourceAccesses.incrementAndGet();
    resourceAccessCounts.computeIfAbsent(resourceName, k -> new AtomicLong(0)).incrementAndGet();
    lastAccessTimes.put(resourceName, Instant.now());
  }

  /**
   * Records memory usage change.
   *
   * @param deltaBytes the change in memory usage (positive for allocation, negative for deallocation)
   */
  public void recordMemoryUsage(final long deltaBytes) {
    totalMemoryUsed.addAndGet(deltaBytes);
  }

  /**
   * Gets the most accessed resource name.
   *
   * @return the name of the most accessed resource, or null if none
   */
  private String getMostAccessedResource() {
    String mostAccessed = null;
    long maxAccesses = 0;

    for (final Map.Entry<String, AtomicLong> entry : resourceAccessCounts.entrySet()) {
      final long accesses = entry.getValue().get();
      if (accesses > maxAccesses) {
        maxAccesses = accesses;
        mostAccessed = entry.getKey();
      }
    }

    return mostAccessed;
  }
}