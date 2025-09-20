package ai.tegmentum.wasmtime4j.cache.impl;

import ai.tegmentum.wasmtime4j.cache.CacheStatistics;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe implementation of CacheStatistics.
 *
 * <p>This implementation uses atomic operations to track cache statistics in a thread-safe manner
 * without requiring synchronization.
 *
 * @since 1.0.0
 */
public final class CacheStatisticsImpl implements CacheStatistics {

  private final boolean enabled;
  private final AtomicLong hitCount = new AtomicLong(0);
  private final AtomicLong missCount = new AtomicLong(0);
  private final AtomicLong evictionCount = new AtomicLong(0);
  private final AtomicLong putCount = new AtomicLong(0);
  private final AtomicLong removalCount = new AtomicLong(0);
  private final AtomicLong maintenanceCount = new AtomicLong(0);
  private final AtomicLong integrityFailureCount = new AtomicLong(0);
  private final AtomicLong totalOperationTime = new AtomicLong(0);
  private final AtomicLong operationCount = new AtomicLong(0);
  private volatile Instant statisticsStartTime;

  /**
   * Creates a new CacheStatisticsImpl.
   *
   * @param enabled whether statistics collection is enabled
   */
  public CacheStatisticsImpl(final boolean enabled) {
    this.enabled = enabled;
    this.statisticsStartTime = Instant.now();
  }

  @Override
  public long getRequestCount() {
    return enabled ? hitCount.get() + missCount.get() : 0;
  }

  @Override
  public long getHitCount() {
    return enabled ? hitCount.get() : 0;
  }

  @Override
  public long getMissCount() {
    return enabled ? missCount.get() : 0;
  }

  @Override
  public double getHitRate() {
    if (!enabled) {
      return 0.0;
    }

    final long hits = hitCount.get();
    final long total = hits + missCount.get();
    return total == 0 ? 0.0 : (double) hits / total * 100.0;
  }

  @Override
  public double getMissRate() {
    return 100.0 - getHitRate();
  }

  @Override
  public long getEvictionCount() {
    return enabled ? evictionCount.get() : 0;
  }

  @Override
  public long getCacheSize() {
    // This would be set externally by the cache implementation
    return 0;
  }

  @Override
  public long getMaxCacheSize() {
    // This would be set externally by the cache implementation
    return -1;
  }

  @Override
  public long getMemoryUsage() {
    // This would be calculated externally by the cache implementation
    return 0;
  }

  @Override
  public long getPutCount() {
    return enabled ? putCount.get() : 0;
  }

  @Override
  public long getRemovalCount() {
    return enabled ? removalCount.get() : 0;
  }

  @Override
  public Duration getAverageOperationTime() {
    if (!enabled) {
      return Duration.ZERO;
    }

    final long operations = operationCount.get();
    if (operations == 0) {
      return Duration.ZERO;
    }

    final long averageNanos = totalOperationTime.get() / operations;
    return Duration.ofNanos(averageNanos);
  }

  @Override
  public Instant getStatisticsStartTime() {
    return statisticsStartTime;
  }

  @Override
  public Duration getCollectionDuration() {
    return Duration.between(statisticsStartTime, Instant.now());
  }

  @Override
  public long getMaintenanceCount() {
    return enabled ? maintenanceCount.get() : 0;
  }

  @Override
  public long getIntegrityFailureCount() {
    return enabled ? integrityFailureCount.get() : 0;
  }

  @Override
  public void reset() {
    if (enabled) {
      hitCount.set(0);
      missCount.set(0);
      evictionCount.set(0);
      putCount.set(0);
      removalCount.set(0);
      maintenanceCount.set(0);
      integrityFailureCount.set(0);
      totalOperationTime.set(0);
      operationCount.set(0);
      statisticsStartTime = Instant.now();
    }
  }

  @Override
  public CacheStatistics snapshot() {
    return new CacheStatisticsSnapshot(
        enabled,
        hitCount.get(),
        missCount.get(),
        evictionCount.get(),
        putCount.get(),
        removalCount.get(),
        maintenanceCount.get(),
        integrityFailureCount.get(),
        totalOperationTime.get(),
        operationCount.get(),
        statisticsStartTime);
  }

  @Override
  public String toSummaryString() {
    if (!enabled) {
      return "Statistics disabled";
    }

    return String.format(
        "CacheStatistics{requests=%d, hits=%d, misses=%d, hitRate=%.2f%%, "
            + "evictions=%d, puts=%d, removals=%d, avgOpTime=%s, uptime=%s}",
        getRequestCount(),
        getHitCount(),
        getMissCount(),
        getHitRate(),
        getEvictionCount(),
        getPutCount(),
        getRemovalCount(),
        getAverageOperationTime(),
        getCollectionDuration());
  }

  /** Records a cache hit. */
  public void recordHit() {
    if (enabled) {
      hitCount.incrementAndGet();
    }
  }

  /** Records a cache miss. */
  public void recordMiss() {
    if (enabled) {
      missCount.incrementAndGet();
    }
  }

  /** Records an eviction. */
  public void recordEviction() {
    if (enabled) {
      evictionCount.incrementAndGet();
    }
  }

  /** Records a put operation. */
  public void recordPut() {
    if (enabled) {
      putCount.incrementAndGet();
    }
  }

  /** Records a removal operation. */
  public void recordRemoval() {
    if (enabled) {
      removalCount.incrementAndGet();
    }
  }

  /** Records a maintenance operation. */
  public void recordMaintenanceOperation() {
    if (enabled) {
      maintenanceCount.incrementAndGet();
    }
  }

  /** Records an integrity failure. */
  public void recordIntegrityFailure() {
    if (enabled) {
      integrityFailureCount.incrementAndGet();
    }
  }

  /**
   * Records the time taken for an operation.
   *
   * @param operationTimeNanos the operation time in nanoseconds
   */
  public void recordOperationTime(final long operationTimeNanos) {
    if (enabled) {
      totalOperationTime.addAndGet(operationTimeNanos);
      operationCount.incrementAndGet();
    }
  }

  /** Immutable snapshot of cache statistics. */
  private static final class CacheStatisticsSnapshot implements CacheStatistics {
    private final boolean enabled;
    private final long hitCount;
    private final long missCount;
    private final long evictionCount;
    private final long putCount;
    private final long removalCount;
    private final long maintenanceCount;
    private final long integrityFailureCount;
    private final long totalOperationTime;
    private final long operationCount;
    private final Instant statisticsStartTime;
    private final Instant snapshotTime;

    CacheStatisticsSnapshot(
        final boolean enabled,
        final long hitCount,
        final long missCount,
        final long evictionCount,
        final long putCount,
        final long removalCount,
        final long maintenanceCount,
        final long integrityFailureCount,
        final long totalOperationTime,
        final long operationCount,
        final Instant statisticsStartTime) {
      this.enabled = enabled;
      this.hitCount = hitCount;
      this.missCount = missCount;
      this.evictionCount = evictionCount;
      this.putCount = putCount;
      this.removalCount = removalCount;
      this.maintenanceCount = maintenanceCount;
      this.integrityFailureCount = integrityFailureCount;
      this.totalOperationTime = totalOperationTime;
      this.operationCount = operationCount;
      this.statisticsStartTime = statisticsStartTime;
      this.snapshotTime = Instant.now();
    }

    @Override
    public long getRequestCount() {
      return enabled ? hitCount + missCount : 0;
    }

    @Override
    public long getHitCount() {
      return enabled ? hitCount : 0;
    }

    @Override
    public long getMissCount() {
      return enabled ? missCount : 0;
    }

    @Override
    public double getHitRate() {
      if (!enabled) {
        return 0.0;
      }

      final long total = hitCount + missCount;
      return total == 0 ? 0.0 : (double) hitCount / total * 100.0;
    }

    @Override
    public double getMissRate() {
      return 100.0 - getHitRate();
    }

    @Override
    public long getEvictionCount() {
      return enabled ? evictionCount : 0;
    }

    @Override
    public long getCacheSize() {
      return 0; // Would be set by cache implementation
    }

    @Override
    public long getMaxCacheSize() {
      return -1; // Would be set by cache implementation
    }

    @Override
    public long getMemoryUsage() {
      return 0; // Would be calculated by cache implementation
    }

    @Override
    public long getPutCount() {
      return enabled ? putCount : 0;
    }

    @Override
    public long getRemovalCount() {
      return enabled ? removalCount : 0;
    }

    @Override
    public Duration getAverageOperationTime() {
      if (!enabled || operationCount == 0) {
        return Duration.ZERO;
      }

      final long averageNanos = totalOperationTime / operationCount;
      return Duration.ofNanos(averageNanos);
    }

    @Override
    public Instant getStatisticsStartTime() {
      return statisticsStartTime;
    }

    @Override
    public Duration getCollectionDuration() {
      return Duration.between(statisticsStartTime, snapshotTime);
    }

    @Override
    public long getMaintenanceCount() {
      return enabled ? maintenanceCount : 0;
    }

    @Override
    public long getIntegrityFailureCount() {
      return enabled ? integrityFailureCount : 0;
    }

    @Override
    public void reset() {
      throw new UnsupportedOperationException("Cannot reset a statistics snapshot");
    }

    @Override
    public CacheStatistics snapshot() {
      return this; // Already a snapshot
    }

    @Override
    public String toSummaryString() {
      if (!enabled) {
        return "Statistics disabled";
      }

      return String.format(
          "CacheStatistics{requests=%d, hits=%d, misses=%d, hitRate=%.2f%%, "
              + "evictions=%d, puts=%d, removals=%d, avgOpTime=%s, uptime=%s}",
          getRequestCount(),
          getHitCount(),
          getMissCount(),
          getHitRate(),
          getEvictionCount(),
          getPutCount(),
          getRemovalCount(),
          getAverageOperationTime(),
          getCollectionDuration());
    }
  }
}
