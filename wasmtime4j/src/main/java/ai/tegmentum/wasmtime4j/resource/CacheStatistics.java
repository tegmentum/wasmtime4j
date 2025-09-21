package ai.tegmentum.wasmtime4j.resource;

import java.time.Instant;

/**
 * Statistics and metrics for cache operations and performance.
 *
 * <p>CacheStatistics provides comprehensive information about cache performance, hit/miss ratios,
 * eviction patterns, and resource utilization to support monitoring and tuning.
 *
 * @since 1.0.0
 */
public final class CacheStatistics {

  private final long totalRequests;
  private final long hitCount;
  private final long missCount;
  private final long loadCount;
  private final long loadErrorCount;
  private final long evictionCount;
  private final long expirationCount;
  private final long totalLoadTime;
  private final long averageLoadTime;
  private final long maxLoadTime;
  private final int currentSize;
  private final int maxSize;
  private final long estimatedMemoryUsage;
  private final long maxMemoryUsage;
  private final double hitRate;
  private final double missRate;
  private final double loadErrorRate;
  private final Instant lastAccess;
  private final Instant lastLoad;
  private final Instant lastEviction;
  private final Instant statisticsTimestamp;

  private CacheStatistics(final Builder builder) {
    this.totalRequests = builder.totalRequests;
    this.hitCount = builder.hitCount;
    this.missCount = builder.missCount;
    this.loadCount = builder.loadCount;
    this.loadErrorCount = builder.loadErrorCount;
    this.evictionCount = builder.evictionCount;
    this.expirationCount = builder.expirationCount;
    this.totalLoadTime = builder.totalLoadTime;
    this.averageLoadTime = builder.averageLoadTime;
    this.maxLoadTime = builder.maxLoadTime;
    this.currentSize = builder.currentSize;
    this.maxSize = builder.maxSize;
    this.estimatedMemoryUsage = builder.estimatedMemoryUsage;
    this.maxMemoryUsage = builder.maxMemoryUsage;
    this.hitRate = calculateRate(hitCount, totalRequests);
    this.missRate = calculateRate(missCount, totalRequests);
    this.loadErrorRate = calculateRate(loadErrorCount, loadCount);
    this.lastAccess = builder.lastAccess;
    this.lastLoad = builder.lastLoad;
    this.lastEviction = builder.lastEviction;
    this.statisticsTimestamp = builder.statisticsTimestamp;
  }

  /**
   * Creates a new statistics builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the total number of cache requests.
   *
   * @return the total request count
   */
  public long getTotalRequests() {
    return totalRequests;
  }

  /**
   * Gets the number of cache hits.
   *
   * @return the hit count
   */
  public long getHitCount() {
    return hitCount;
  }

  /**
   * Gets the number of cache misses.
   *
   * @return the miss count
   */
  public long getMissCount() {
    return missCount;
  }

  /**
   * Gets the number of cache loads.
   *
   * @return the load count
   */
  public long getLoadCount() {
    return loadCount;
  }

  /**
   * Gets the number of load errors.
   *
   * @return the load error count
   */
  public long getLoadErrorCount() {
    return loadErrorCount;
  }

  /**
   * Gets the number of evictions.
   *
   * @return the eviction count
   */
  public long getEvictionCount() {
    return evictionCount;
  }

  /**
   * Gets the number of expirations.
   *
   * @return the expiration count
   */
  public long getExpirationCount() {
    return expirationCount;
  }

  /**
   * Gets the total load time in milliseconds.
   *
   * @return the total load time
   */
  public long getTotalLoadTime() {
    return totalLoadTime;
  }

  /**
   * Gets the average load time in milliseconds.
   *
   * @return the average load time
   */
  public long getAverageLoadTime() {
    return averageLoadTime;
  }

  /**
   * Gets the maximum load time in milliseconds.
   *
   * @return the maximum load time
   */
  public long getMaxLoadTime() {
    return maxLoadTime;
  }

  /**
   * Gets the current cache size.
   *
   * @return the current size
   */
  public int getCurrentSize() {
    return currentSize;
  }

  /**
   * Gets the maximum cache size.
   *
   * @return the maximum size
   */
  public int getMaxSize() {
    return maxSize;
  }

  /**
   * Gets the estimated memory usage in bytes.
   *
   * @return the estimated memory usage
   */
  public long getEstimatedMemoryUsage() {
    return estimatedMemoryUsage;
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
   * Gets the cache hit rate (0.0 to 1.0).
   *
   * @return the hit rate
   */
  public double getHitRate() {
    return hitRate;
  }

  /**
   * Gets the cache miss rate (0.0 to 1.0).
   *
   * @return the miss rate
   */
  public double getMissRate() {
    return missRate;
  }

  /**
   * Gets the load error rate (0.0 to 1.0).
   *
   * @return the load error rate
   */
  public double getLoadErrorRate() {
    return loadErrorRate;
  }

  /**
   * Gets the timestamp of the last cache access.
   *
   * @return the last access time, or null if none
   */
  public Instant getLastAccess() {
    return lastAccess;
  }

  /**
   * Gets the timestamp of the last cache load.
   *
   * @return the last load time, or null if none
   */
  public Instant getLastLoad() {
    return lastLoad;
  }

  /**
   * Gets the timestamp of the last eviction.
   *
   * @return the last eviction time, or null if none
   */
  public Instant getLastEviction() {
    return lastEviction;
  }

  /**
   * Gets the timestamp when these statistics were collected.
   *
   * @return the statistics timestamp
   */
  public Instant getStatisticsTimestamp() {
    return statisticsTimestamp;
  }

  /**
   * Calculates the cache utilization as a percentage (0.0 to 1.0).
   *
   * @return the cache utilization
   */
  public double getUtilization() {
    if (maxSize == 0) {
      return 0.0;
    }
    return (double) currentSize / maxSize;
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
    return (double) estimatedMemoryUsage / maxMemoryUsage;
  }

  /**
   * Calculates the request rate (requests per second).
   *
   * @param timeWindowSeconds the time window in seconds
   * @return the request rate
   */
  public double getRequestRate(final long timeWindowSeconds) {
    if (timeWindowSeconds <= 0) {
      return 0.0;
    }
    return (double) totalRequests / timeWindowSeconds;
  }

  /**
   * Calculates the eviction rate (evictions per second).
   *
   * @param timeWindowSeconds the time window in seconds
   * @return the eviction rate
   */
  public double getEvictionRate(final long timeWindowSeconds) {
    if (timeWindowSeconds <= 0) {
      return 0.0;
    }
    return (double) evictionCount / timeWindowSeconds;
  }

  /**
   * Checks if the cache performance is healthy.
   *
   * <p>A cache is considered healthy if: - Hit rate is above 70% - Load error rate is below 5% -
   * Utilization is below 90%
   *
   * @return true if the cache performance is healthy
   */
  public boolean isHealthy() {
    return hitRate >= 0.7 && loadErrorRate <= 0.05 && getUtilization() <= 0.9;
  }

  private static double calculateRate(final long numerator, final long denominator) {
    if (denominator == 0) {
      return 0.0;
    }
    return (double) numerator / denominator;
  }

  /** Builder for creating cache statistics. */
  public static final class Builder {
    private long totalRequests;
    private long hitCount;
    private long missCount;
    private long loadCount;
    private long loadErrorCount;
    private long evictionCount;
    private long expirationCount;
    private long totalLoadTime;
    private long averageLoadTime;
    private long maxLoadTime;
    private int currentSize;
    private int maxSize;
    private long estimatedMemoryUsage;
    private long maxMemoryUsage;
    private Instant lastAccess;
    private Instant lastLoad;
    private Instant lastEviction;
    private Instant statisticsTimestamp = Instant.now();

    private Builder() {}

    public Builder withTotalRequests(final long totalRequests) {
      this.totalRequests = totalRequests;
      return this;
    }

    public Builder withHitCount(final long hitCount) {
      this.hitCount = hitCount;
      return this;
    }

    public Builder withMissCount(final long missCount) {
      this.missCount = missCount;
      return this;
    }

    public Builder withLoadCount(final long loadCount) {
      this.loadCount = loadCount;
      return this;
    }

    public Builder withLoadErrorCount(final long loadErrorCount) {
      this.loadErrorCount = loadErrorCount;
      return this;
    }

    public Builder withEvictionCount(final long evictionCount) {
      this.evictionCount = evictionCount;
      return this;
    }

    public Builder withExpirationCount(final long expirationCount) {
      this.expirationCount = expirationCount;
      return this;
    }

    public Builder withTotalLoadTime(final long totalLoadTime) {
      this.totalLoadTime = totalLoadTime;
      return this;
    }

    public Builder withAverageLoadTime(final long averageLoadTime) {
      this.averageLoadTime = averageLoadTime;
      return this;
    }

    public Builder withMaxLoadTime(final long maxLoadTime) {
      this.maxLoadTime = maxLoadTime;
      return this;
    }

    public Builder withCurrentSize(final int currentSize) {
      this.currentSize = currentSize;
      return this;
    }

    public Builder withMaxSize(final int maxSize) {
      this.maxSize = maxSize;
      return this;
    }

    public Builder withEstimatedMemoryUsage(final long estimatedMemoryUsage) {
      this.estimatedMemoryUsage = estimatedMemoryUsage;
      return this;
    }

    public Builder withMaxMemoryUsage(final long maxMemoryUsage) {
      this.maxMemoryUsage = maxMemoryUsage;
      return this;
    }

    public Builder withLastAccess(final Instant lastAccess) {
      this.lastAccess = lastAccess;
      return this;
    }

    public Builder withLastLoad(final Instant lastLoad) {
      this.lastLoad = lastLoad;
      return this;
    }

    public Builder withLastEviction(final Instant lastEviction) {
      this.lastEviction = lastEviction;
      return this;
    }

    public Builder withStatisticsTimestamp(final Instant statisticsTimestamp) {
      this.statisticsTimestamp = statisticsTimestamp;
      return this;
    }

    public CacheStatistics build() {
      return new CacheStatistics(this);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "CacheStatistics{totalRequests=%d, hitCount=%d, missCount=%d, "
            + "hitRate=%.2f%%, missRate=%.2f%%, loadErrorRate=%.2f%%, "
            + "currentSize=%d, maxSize=%d, utilization=%.2f%%, "
            + "evictionCount=%d, averageLoadTime=%dms, timestamp=%s}",
        totalRequests,
        hitCount,
        missCount,
        hitRate * 100,
        missRate * 100,
        loadErrorRate * 100,
        currentSize,
        maxSize,
        getUtilization() * 100,
        evictionCount,
        averageLoadTime,
        statisticsTimestamp);
  }
}
