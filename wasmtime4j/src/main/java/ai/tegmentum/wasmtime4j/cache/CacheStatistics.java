package ai.tegmentum.wasmtime4j.cache;

import java.time.Duration;
import java.time.Instant;

/**
 * Statistics about module cache usage and performance.
 *
 * <p>CacheStatistics provide insights into cache behavior including hit/miss ratios, eviction
 * counts, and performance metrics. These statistics are useful for monitoring cache effectiveness
 * and tuning cache parameters.
 *
 * @since 1.0.0
 */
public interface CacheStatistics {

  /**
   * Gets the total number of cache requests (hits + misses).
   *
   * @return the total request count
   */
  long getRequestCount();

  /**
   * Gets the number of successful cache hits.
   *
   * <p>A cache hit occurs when a requested module is found in the cache and is still valid.
   *
   * @return the hit count
   */
  long getHitCount();

  /**
   * Gets the number of cache misses.
   *
   * <p>A cache miss occurs when a requested module is not found in the cache or is invalid.
   *
   * @return the miss count
   */
  long getMissCount();

  /**
   * Gets the cache hit rate as a percentage.
   *
   * <p>The hit rate is calculated as hitCount / (hitCount + missCount) * 100. A higher hit rate
   * indicates better cache effectiveness.
   *
   * @return the hit rate as a percentage (0.0 to 100.0)
   */
  double getHitRate();

  /**
   * Gets the cache miss rate as a percentage.
   *
   * <p>The miss rate is calculated as 100 - hitRate.
   *
   * @return the miss rate as a percentage (0.0 to 100.0)
   */
  double getMissRate();

  /**
   * Gets the number of entries that have been evicted from the cache.
   *
   * <p>Evictions occur when the cache reaches capacity limits or when entries expire.
   *
   * @return the eviction count
   */
  long getEvictionCount();

  /**
   * Gets the current number of entries in the cache.
   *
   * @return the current cache size
   */
  long getCacheSize();

  /**
   * Gets the maximum size limit of the cache.
   *
   * @return the maximum cache size, or -1 if unlimited
   */
  long getMaxCacheSize();

  /**
   * Gets the estimated memory usage of the cache in bytes.
   *
   * @return the estimated memory usage
   */
  long getMemoryUsage();

  /**
   * Gets the total number of entries that have been stored in the cache.
   *
   * <p>This includes both current entries and those that have been evicted.
   *
   * @return the total put count
   */
  long getPutCount();

  /**
   * Gets the total number of entries that have been removed from the cache.
   *
   * <p>This includes both explicit removals and evictions.
   *
   * @return the total removal count
   */
  long getRemovalCount();

  /**
   * Gets the average time taken for cache operations.
   *
   * @return the average operation time
   */
  Duration getAverageOperationTime();

  /**
   * Gets the timestamp when statistics collection started.
   *
   * @return the statistics start time
   */
  Instant getStatisticsStartTime();

  /**
   * Gets the total time that statistics have been collected.
   *
   * @return the total collection duration
   */
  Duration getCollectionDuration();

  /**
   * Gets the number of cache maintenance operations performed.
   *
   * @return the maintenance operation count
   */
  long getMaintenanceCount();

  /**
   * Gets the number of integrity check failures.
   *
   * <p>Integrity failures occur when cached modules fail validation checks.
   *
   * @return the integrity failure count
   */
  long getIntegrityFailureCount();

  /**
   * Resets all statistics to zero.
   *
   * <p>This method clears all accumulated statistics and restarts collection from the current
   * time.
   */
  void reset();

  /**
   * Creates a snapshot of the current statistics.
   *
   * <p>The snapshot represents the state at the time this method is called and will not change
   * as new operations occur.
   *
   * @return an immutable snapshot of current statistics
   */
  CacheStatistics snapshot();

  /**
   * Gets a formatted string representation of key statistics.
   *
   * @return a formatted summary of statistics
   */
  String toSummaryString();
}