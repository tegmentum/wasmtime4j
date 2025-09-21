package ai.tegmentum.wasmtime4j.resource;

import java.time.Instant;
import java.util.Map;

/**
 * Comprehensive resource metrics and statistics.
 *
 * <p>ResourceMetrics provides detailed information about resource usage, performance
 * characteristics, and operational metrics for monitoring and optimization purposes.
 *
 * @since 1.0.0
 */
public final class ResourceMetrics {

  private final Instant timestamp;
  private final long totalMemoryUsage;
  private final long maxMemoryUsage;
  private final int totalResourceCount;
  private final int activeResourceCount;
  private final int idleResourceCount;
  private final Map<ResourceType, Integer> resourceCountByType;
  private final Map<ResourceType, Long> memoryUsageByType;
  private final double cpuUsage;
  private final long garbageCollectionCount;
  private final long garbageCollectionTime;
  private final long nativeMemoryUsage;
  private final int threadCount;
  private final double systemLoadAverage;
  private final Map<String, Object> customMetrics;

  private ResourceMetrics(final Builder builder) {
    this.timestamp = builder.timestamp;
    this.totalMemoryUsage = builder.totalMemoryUsage;
    this.maxMemoryUsage = builder.maxMemoryUsage;
    this.totalResourceCount = builder.totalResourceCount;
    this.activeResourceCount = builder.activeResourceCount;
    this.idleResourceCount = builder.idleResourceCount;
    this.resourceCountByType = Map.copyOf(builder.resourceCountByType);
    this.memoryUsageByType = Map.copyOf(builder.memoryUsageByType);
    this.cpuUsage = builder.cpuUsage;
    this.garbageCollectionCount = builder.garbageCollectionCount;
    this.garbageCollectionTime = builder.garbageCollectionTime;
    this.nativeMemoryUsage = builder.nativeMemoryUsage;
    this.threadCount = builder.threadCount;
    this.systemLoadAverage = builder.systemLoadAverage;
    this.customMetrics = Map.copyOf(builder.customMetrics);
  }

  /**
   * Creates a new metrics builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the timestamp when these metrics were collected.
   *
   * @return the collection timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the total memory usage in bytes.
   *
   * @return the total memory usage
   */
  public long getTotalMemoryUsage() {
    return totalMemoryUsage;
  }

  /**
   * Gets the maximum memory usage in bytes.
   *
   * @return the maximum memory usage
   */
  public long getMaxMemoryUsage() {
    return maxMemoryUsage;
  }

  /**
   * Gets the total number of resources.
   *
   * @return the total resource count
   */
  public int getTotalResourceCount() {
    return totalResourceCount;
  }

  /**
   * Gets the number of active resources.
   *
   * @return the active resource count
   */
  public int getActiveResourceCount() {
    return activeResourceCount;
  }

  /**
   * Gets the number of idle resources.
   *
   * @return the idle resource count
   */
  public int getIdleResourceCount() {
    return idleResourceCount;
  }

  /**
   * Gets the resource count by type.
   *
   * @return map of resource types to their counts
   */
  public Map<ResourceType, Integer> getResourceCountByType() {
    return resourceCountByType;
  }

  /**
   * Gets the memory usage by resource type.
   *
   * @return map of resource types to their memory usage in bytes
   */
  public Map<ResourceType, Long> getMemoryUsageByType() {
    return memoryUsageByType;
  }

  /**
   * Gets the CPU usage as a percentage (0.0 to 1.0).
   *
   * @return the CPU usage
   */
  public double getCpuUsage() {
    return cpuUsage;
  }

  /**
   * Gets the garbage collection count.
   *
   * @return the GC count
   */
  public long getGarbageCollectionCount() {
    return garbageCollectionCount;
  }

  /**
   * Gets the total garbage collection time in milliseconds.
   *
   * @return the GC time
   */
  public long getGarbageCollectionTime() {
    return garbageCollectionTime;
  }

  /**
   * Gets the native memory usage in bytes.
   *
   * @return the native memory usage
   */
  public long getNativeMemoryUsage() {
    return nativeMemoryUsage;
  }

  /**
   * Gets the current thread count.
   *
   * @return the thread count
   */
  public int getThreadCount() {
    return threadCount;
  }

  /**
   * Gets the system load average.
   *
   * @return the system load average
   */
  public double getSystemLoadAverage() {
    return systemLoadAverage;
  }

  /**
   * Gets custom metrics.
   *
   * @return map of custom metric names to values
   */
  public Map<String, Object> getCustomMetrics() {
    return customMetrics;
  }

  /**
   * Calculates the memory utilization as a percentage (0.0 to 1.0).
   *
   * @return the memory utilization
   */
  public double getMemoryUtilization() {
    if (maxMemoryUsage == 0) {
      return 0.0;
    }
    return (double) totalMemoryUsage / maxMemoryUsage;
  }

  /**
   * Calculates the resource utilization as a percentage (0.0 to 1.0).
   *
   * @return the resource utilization
   */
  public double getResourceUtilization() {
    if (totalResourceCount == 0) {
      return 0.0;
    }
    return (double) activeResourceCount / totalResourceCount;
  }

  /**
   * Gets the memory usage for a specific resource type.
   *
   * @param type the resource type
   * @return the memory usage for the type, or 0 if not found
   * @throws IllegalArgumentException if type is null
   */
  public long getMemoryUsage(final ResourceType type) {
    if (type == null) {
      throw new IllegalArgumentException("Resource type cannot be null");
    }
    return memoryUsageByType.getOrDefault(type, 0L);
  }

  /**
   * Gets the resource count for a specific resource type.
   *
   * @param type the resource type
   * @return the resource count for the type, or 0 if not found
   * @throws IllegalArgumentException if type is null
   */
  public int getResourceCount(final ResourceType type) {
    if (type == null) {
      throw new IllegalArgumentException("Resource type cannot be null");
    }
    return resourceCountByType.getOrDefault(type, 0);
  }

  /** Builder for creating resource metrics. */
  public static final class Builder {
    private Instant timestamp = Instant.now();
    private long totalMemoryUsage;
    private long maxMemoryUsage;
    private int totalResourceCount;
    private int activeResourceCount;
    private int idleResourceCount;
    private Map<ResourceType, Integer> resourceCountByType = Map.of();
    private Map<ResourceType, Long> memoryUsageByType = Map.of();
    private double cpuUsage;
    private long garbageCollectionCount;
    private long garbageCollectionTime;
    private long nativeMemoryUsage;
    private int threadCount;
    private double systemLoadAverage;
    private Map<String, Object> customMetrics = Map.of();

    private Builder() {}

    public Builder withTimestamp(final Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder withTotalMemoryUsage(final long totalMemoryUsage) {
      this.totalMemoryUsage = totalMemoryUsage;
      return this;
    }

    public Builder withMaxMemoryUsage(final long maxMemoryUsage) {
      this.maxMemoryUsage = maxMemoryUsage;
      return this;
    }

    public Builder withTotalResourceCount(final int totalResourceCount) {
      this.totalResourceCount = totalResourceCount;
      return this;
    }

    public Builder withActiveResourceCount(final int activeResourceCount) {
      this.activeResourceCount = activeResourceCount;
      return this;
    }

    public Builder withIdleResourceCount(final int idleResourceCount) {
      this.idleResourceCount = idleResourceCount;
      return this;
    }

    public Builder withResourceCountByType(final Map<ResourceType, Integer> resourceCountByType) {
      this.resourceCountByType = resourceCountByType != null ? resourceCountByType : Map.of();
      return this;
    }

    public Builder withMemoryUsageByType(final Map<ResourceType, Long> memoryUsageByType) {
      this.memoryUsageByType = memoryUsageByType != null ? memoryUsageByType : Map.of();
      return this;
    }

    public Builder withCpuUsage(final double cpuUsage) {
      this.cpuUsage = cpuUsage;
      return this;
    }

    public Builder withGarbageCollectionCount(final long garbageCollectionCount) {
      this.garbageCollectionCount = garbageCollectionCount;
      return this;
    }

    public Builder withGarbageCollectionTime(final long garbageCollectionTime) {
      this.garbageCollectionTime = garbageCollectionTime;
      return this;
    }

    public Builder withNativeMemoryUsage(final long nativeMemoryUsage) {
      this.nativeMemoryUsage = nativeMemoryUsage;
      return this;
    }

    public Builder withThreadCount(final int threadCount) {
      this.threadCount = threadCount;
      return this;
    }

    public Builder withSystemLoadAverage(final double systemLoadAverage) {
      this.systemLoadAverage = systemLoadAverage;
      return this;
    }

    public Builder withCustomMetrics(final Map<String, Object> customMetrics) {
      this.customMetrics = customMetrics != null ? customMetrics : Map.of();
      return this;
    }

    public ResourceMetrics build() {
      return new ResourceMetrics(this);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "ResourceMetrics{timestamp=%s, totalMemoryUsage=%d, totalResourceCount=%d, "
            + "activeResourceCount=%d, memoryUtilization=%.2f%%, resourceUtilization=%.2f%%, "
            + "cpuUsage=%.2f%%, threadCount=%d, systemLoadAverage=%.2f}",
        timestamp,
        totalMemoryUsage,
        totalResourceCount,
        activeResourceCount,
        getMemoryUtilization() * 100,
        getResourceUtilization() * 100,
        cpuUsage * 100,
        threadCount,
        systemLoadAverage);
  }
}
