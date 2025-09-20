package ai.tegmentum.wasmtime4j;

import java.util.Objects;

/**
 * Performance metrics for WebAssembly memory operations tracking.
 *
 * <p>This class provides detailed performance metrics for memory operations, enabling
 * performance analysis, optimization, and monitoring in enterprise applications.
 * All metrics are collected with minimal overhead and provide actionable insights.
 *
 * @since 1.0.0
 */
public final class MemoryPerformanceMetrics {

  private final long totalOperations;
  private final long totalOperationTimeNanos;
  private final long minOperationTimeNanos;
  private final long maxOperationTimeNanos;
  private final double avgOperationTimeNanos;
  private final long totalBytesTransferred;
  private final double throughputBytesPerSecond;
  private final long cacheHits;
  private final long cacheMisses;
  private final long collectionTimestamp;
  private final long bulkOperations;
  private final long singleOperations;

  /**
   * Creates new memory performance metrics.
   *
   * @param totalOperations total number of operations performed
   * @param totalOperationTimeNanos total time spent in operations (nanoseconds)
   * @param minOperationTimeNanos minimum operation time (nanoseconds)
   * @param maxOperationTimeNanos maximum operation time (nanoseconds)
   * @param avgOperationTimeNanos average operation time (nanoseconds)
   * @param totalBytesTransferred total bytes read/written
   * @param throughputBytesPerSecond current throughput in bytes per second
   * @param cacheHits number of cache hits
   * @param cacheMisses number of cache misses
   * @param collectionTimestamp when these metrics were collected (milliseconds since epoch)
   * @param bulkOperations number of bulk operations performed
   * @param singleOperations number of single-byte operations performed
   * @throws IllegalArgumentException if any count parameter is negative
   */
  public MemoryPerformanceMetrics(
      final long totalOperations,
      final long totalOperationTimeNanos,
      final long minOperationTimeNanos,
      final long maxOperationTimeNanos,
      final double avgOperationTimeNanos,
      final long totalBytesTransferred,
      final double throughputBytesPerSecond,
      final long cacheHits,
      final long cacheMisses,
      final long collectionTimestamp,
      final long bulkOperations,
      final long singleOperations) {
    if (totalOperations < 0) {
      throw new IllegalArgumentException("totalOperations cannot be negative: " + totalOperations);
    }
    if (totalOperationTimeNanos < 0) {
      throw new IllegalArgumentException(
          "totalOperationTimeNanos cannot be negative: " + totalOperationTimeNanos);
    }
    if (totalBytesTransferred < 0) {
      throw new IllegalArgumentException(
          "totalBytesTransferred cannot be negative: " + totalBytesTransferred);
    }
    if (cacheHits < 0) {
      throw new IllegalArgumentException("cacheHits cannot be negative: " + cacheHits);
    }
    if (cacheMisses < 0) {
      throw new IllegalArgumentException("cacheMisses cannot be negative: " + cacheMisses);
    }
    if (bulkOperations < 0) {
      throw new IllegalArgumentException("bulkOperations cannot be negative: " + bulkOperations);
    }
    if (singleOperations < 0) {
      throw new IllegalArgumentException("singleOperations cannot be negative: " + singleOperations);
    }

    this.totalOperations = totalOperations;
    this.totalOperationTimeNanos = totalOperationTimeNanos;
    this.minOperationTimeNanos = minOperationTimeNanos;
    this.maxOperationTimeNanos = maxOperationTimeNanos;
    this.avgOperationTimeNanos = avgOperationTimeNanos;
    this.totalBytesTransferred = totalBytesTransferred;
    this.throughputBytesPerSecond = throughputBytesPerSecond;
    this.cacheHits = cacheHits;
    this.cacheMisses = cacheMisses;
    this.collectionTimestamp = collectionTimestamp;
    this.bulkOperations = bulkOperations;
    this.singleOperations = singleOperations;
  }

  /**
   * Gets the total number of memory operations performed.
   *
   * @return the total operation count
   */
  public long getTotalOperations() {
    return totalOperations;
  }

  /**
   * Gets the total time spent in memory operations.
   *
   * @return the total operation time in nanoseconds
   */
  public long getTotalOperationTimeNanos() {
    return totalOperationTimeNanos;
  }

  /**
   * Gets the minimum time for a single operation.
   *
   * @return the minimum operation time in nanoseconds
   */
  public long getMinOperationTimeNanos() {
    return minOperationTimeNanos;
  }

  /**
   * Gets the maximum time for a single operation.
   *
   * @return the maximum operation time in nanoseconds
   */
  public long getMaxOperationTimeNanos() {
    return maxOperationTimeNanos;
  }

  /**
   * Gets the average time per operation.
   *
   * @return the average operation time in nanoseconds
   */
  public double getAvgOperationTimeNanos() {
    return avgOperationTimeNanos;
  }

  /**
   * Gets the total number of bytes transferred (read + written).
   *
   * @return the total bytes transferred
   */
  public long getTotalBytesTransferred() {
    return totalBytesTransferred;
  }

  /**
   * Gets the current memory throughput in bytes per second.
   *
   * @return the throughput in bytes per second
   */
  public double getThroughputBytesPerSecond() {
    return throughputBytesPerSecond;
  }

  /**
   * Gets the throughput in megabytes per second for easier reading.
   *
   * @return the throughput in MB/s
   */
  public double getThroughputMbPerSecond() {
    return throughputBytesPerSecond / (1024.0 * 1024.0);
  }

  /**
   * Gets the number of cache hits.
   *
   * @return the cache hit count
   */
  public long getCacheHits() {
    return cacheHits;
  }

  /**
   * Gets the number of cache misses.
   *
   * @return the cache miss count
   */
  public long getCacheMisses() {
    return cacheMisses;
  }

  /**
   * Gets the cache hit ratio as a percentage.
   *
   * @return the cache hit ratio between 0.0 and 1.0
   */
  public double getCacheHitRatio() {
    final long totalCacheAccesses = cacheHits + cacheMisses;
    return totalCacheAccesses == 0 ? 0.0 : (double) cacheHits / totalCacheAccesses;
  }

  /**
   * Gets the timestamp when these metrics were collected.
   *
   * @return the collection timestamp in milliseconds since epoch
   */
  public long getCollectionTimestamp() {
    return collectionTimestamp;
  }

  /**
   * Gets the number of bulk operations performed.
   *
   * @return the bulk operation count
   */
  public long getBulkOperations() {
    return bulkOperations;
  }

  /**
   * Gets the number of single-byte operations performed.
   *
   * @return the single operation count
   */
  public long getSingleOperations() {
    return singleOperations;
  }

  /**
   * Gets the ratio of bulk operations to total operations.
   *
   * @return the bulk operation ratio between 0.0 and 1.0
   */
  public double getBulkOperationRatio() {
    return totalOperations == 0 ? 0.0 : (double) bulkOperations / totalOperations;
  }

  /**
   * Gets the average bytes per operation.
   *
   * @return the average bytes per operation
   */
  public double getAvgBytesPerOperation() {
    return totalOperations == 0 ? 0.0 : (double) totalBytesTransferred / totalOperations;
  }

  /**
   * Gets the operations per second rate.
   *
   * @return the operations per second
   */
  public double getOperationsPerSecond() {
    final double totalTimeSeconds = totalOperationTimeNanos / 1_000_000_000.0;
    return totalTimeSeconds == 0.0 ? 0.0 : totalOperations / totalTimeSeconds;
  }

  /**
   * Gets the total operation time in milliseconds for easier reading.
   *
   * @return the total operation time in milliseconds
   */
  public double getTotalOperationTimeMillis() {
    return totalOperationTimeNanos / 1_000_000.0;
  }

  /**
   * Gets the average operation time in microseconds for easier reading.
   *
   * @return the average operation time in microseconds
   */
  public double getAvgOperationTimeMicros() {
    return avgOperationTimeNanos / 1_000.0;
  }

  /**
   * Calculates the performance efficiency score based on various metrics.
   *
   * <p>This is a normalized score between 0.0 and 1.0 that considers throughput,
   * cache hit ratio, and bulk operation usage to provide an overall performance indicator.
   *
   * @return the performance efficiency score between 0.0 and 1.0
   */
  public double getPerformanceEfficiencyScore() {
    final double cacheScore = getCacheHitRatio();
    final double bulkScore = getBulkOperationRatio();
    final double throughputScore =
        Math.min(1.0, getThroughputMbPerSecond() / 1000.0); // Normalize to 1GB/s max

    return (cacheScore + bulkScore + throughputScore) / 3.0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MemoryPerformanceMetrics that = (MemoryPerformanceMetrics) obj;
    return totalOperations == that.totalOperations
        && totalOperationTimeNanos == that.totalOperationTimeNanos
        && minOperationTimeNanos == that.minOperationTimeNanos
        && maxOperationTimeNanos == that.maxOperationTimeNanos
        && Double.compare(that.avgOperationTimeNanos, avgOperationTimeNanos) == 0
        && totalBytesTransferred == that.totalBytesTransferred
        && Double.compare(that.throughputBytesPerSecond, throughputBytesPerSecond) == 0
        && cacheHits == that.cacheHits
        && cacheMisses == that.cacheMisses
        && collectionTimestamp == that.collectionTimestamp
        && bulkOperations == that.bulkOperations
        && singleOperations == that.singleOperations;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalOperations,
        totalOperationTimeNanos,
        minOperationTimeNanos,
        maxOperationTimeNanos,
        avgOperationTimeNanos,
        totalBytesTransferred,
        throughputBytesPerSecond,
        cacheHits,
        cacheMisses,
        collectionTimestamp,
        bulkOperations,
        singleOperations);
  }

  @Override
  public String toString() {
    return "MemoryPerformanceMetrics{"
        + "totalOperations="
        + totalOperations
        + ", totalTime="
        + String.format("%.2fms", getTotalOperationTimeMillis())
        + ", avgTime="
        + String.format("%.2fμs", getAvgOperationTimeMicros())
        + ", throughput="
        + String.format("%.2f MB/s", getThroughputMbPerSecond())
        + ", cacheHitRatio="
        + String.format("%.1f%%", getCacheHitRatio() * 100)
        + ", bulkRatio="
        + String.format("%.1f%%", getBulkOperationRatio() * 100)
        + ", efficiency="
        + String.format("%.1f%%", getPerformanceEfficiencyScore() * 100)
        + '}';
  }
}