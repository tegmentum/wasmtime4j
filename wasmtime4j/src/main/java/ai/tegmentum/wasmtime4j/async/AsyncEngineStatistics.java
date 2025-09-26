package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for asynchronous WebAssembly engine operations.
 *
 * <p>This class tracks performance metrics and usage patterns for async
 * compilation operations, providing insights for optimization and monitoring.
 *
 * @since 1.0.0
 */
public final class AsyncEngineStatistics {

  private final AtomicLong totalCompilations = new AtomicLong(0);
  private final AtomicLong successfulCompilations = new AtomicLong(0);
  private final AtomicLong failedCompilations = new AtomicLong(0);
  private final AtomicLong cancelledCompilations = new AtomicLong(0);
  private final AtomicLong totalBytesProcessed = new AtomicLong(0);
  private final AtomicLong totalCompilationTime = new AtomicLong(0);
  private final AtomicInteger activeCompilations = new AtomicInteger(0);
  private final AtomicInteger peakActiveCompilations = new AtomicInteger(0);
  private final AtomicLong cacheHits = new AtomicLong(0);
  private final AtomicLong cacheMisses = new AtomicLong(0);
  private final AtomicLong peakMemoryUsage = new AtomicLong(0);
  private final AtomicDouble averageThreadUtilization = new AtomicDouble(0.0);

  private final Instant startTime;
  private volatile Instant lastCompilationTime;

  /** Creates new async engine statistics. */
  public AsyncEngineStatistics() {
    this.startTime = Instant.now();
    this.lastCompilationTime = startTime;
  }

  /**
   * Records the start of a compilation operation.
   */
  public void recordCompilationStart() {
    final int active = activeCompilations.incrementAndGet();
    peakActiveCompilations.updateAndGet(peak -> Math.max(peak, active));
    totalCompilations.incrementAndGet();
  }

  /**
   * Records the successful completion of a compilation operation.
   *
   * @param bytesProcessed number of bytes processed
   * @param compilationTime time taken for compilation
   * @param threadUtilization thread utilization during compilation
   */
  public void recordCompilationSuccess(
      final long bytesProcessed,
      final Duration compilationTime,
      final double threadUtilization) {
    activeCompilations.decrementAndGet();
    successfulCompilations.incrementAndGet();
    totalBytesProcessed.addAndGet(bytesProcessed);
    totalCompilationTime.addAndGet(compilationTime.toNanos());
    updateAverageThreadUtilization(threadUtilization);
    lastCompilationTime = Instant.now();
  }

  /**
   * Records a failed compilation operation.
   */
  public void recordCompilationFailure() {
    activeCompilations.decrementAndGet();
    failedCompilations.incrementAndGet();
    lastCompilationTime = Instant.now();
  }

  /**
   * Records a cancelled compilation operation.
   */
  public void recordCompilationCancellation() {
    activeCompilations.decrementAndGet();
    cancelledCompilations.incrementAndGet();
    lastCompilationTime = Instant.now();
  }

  /**
   * Records a cache hit.
   */
  public void recordCacheHit() {
    cacheHits.incrementAndGet();
  }

  /**
   * Records a cache miss.
   */
  public void recordCacheMiss() {
    cacheMisses.incrementAndGet();
  }

  /**
   * Updates the peak memory usage.
   *
   * @param memoryUsage current memory usage in bytes
   */
  public void updatePeakMemoryUsage(final long memoryUsage) {
    peakMemoryUsage.updateAndGet(peak -> Math.max(peak, memoryUsage));
  }

  private void updateAverageThreadUtilization(final double utilization) {
    // Simple exponential moving average
    final double currentAverage = averageThreadUtilization.get();
    final double alpha = 0.1; // Smoothing factor
    final double newAverage = alpha * utilization + (1 - alpha) * currentAverage;
    averageThreadUtilization.set(newAverage);
  }

  /**
   * Gets the total number of compilation operations started.
   *
   * @return total compilations count
   */
  public long getTotalCompilations() {
    return totalCompilations.get();
  }

  /**
   * Gets the number of successful compilation operations.
   *
   * @return successful compilations count
   */
  public long getSuccessfulCompilations() {
    return successfulCompilations.get();
  }

  /**
   * Gets the number of failed compilation operations.
   *
   * @return failed compilations count
   */
  public long getFailedCompilations() {
    return failedCompilations.get();
  }

  /**
   * Gets the number of cancelled compilation operations.
   *
   * @return cancelled compilations count
   */
  public long getCancelledCompilations() {
    return cancelledCompilations.get();
  }

  /**
   * Gets the total bytes processed across all compilations.
   *
   * @return total bytes processed
   */
  public long getTotalBytesProcessed() {
    return totalBytesProcessed.get();
  }

  /**
   * Gets the total compilation time across all operations.
   *
   * @return total compilation time
   */
  public Duration getTotalCompilationTime() {
    return Duration.ofNanos(totalCompilationTime.get());
  }

  /**
   * Gets the number of currently active compilations.
   *
   * @return active compilations count
   */
  public int getActiveCompilations() {
    return activeCompilations.get();
  }

  /**
   * Gets the peak number of concurrent compilations.
   *
   * @return peak active compilations
   */
  public int getPeakActiveCompilations() {
    return peakActiveCompilations.get();
  }

  /**
   * Gets the number of cache hits.
   *
   * @return cache hits count
   */
  public long getCacheHits() {
    return cacheHits.get();
  }

  /**
   * Gets the number of cache misses.
   *
   * @return cache misses count
   */
  public long getCacheMisses() {
    return cacheMisses.get();
  }

  /**
   * Gets the peak memory usage during operations.
   *
   * @return peak memory usage in bytes
   */
  public long getPeakMemoryUsage() {
    return peakMemoryUsage.get();
  }

  /**
   * Gets the average thread utilization.
   *
   * @return average thread utilization (0.0 - 1.0)
   */
  public double getAverageThreadUtilization() {
    return averageThreadUtilization.get();
  }

  /**
   * Gets the time when statistics collection started.
   *
   * @return start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the time of the last compilation operation.
   *
   * @return last compilation time
   */
  public Instant getLastCompilationTime() {
    return lastCompilationTime;
  }

  /**
   * Calculates the success rate of compilation operations.
   *
   * @return success rate (0.0 - 1.0)
   */
  public double getSuccessRate() {
    final long total = getTotalCompilations();
    return total > 0 ? (double) getSuccessfulCompilations() / total : 0.0;
  }

  /**
   * Calculates the cache hit rate.
   *
   * @return cache hit rate (0.0 - 1.0)
   */
  public double getCacheHitRate() {
    final long totalCacheAccesses = getCacheHits() + getCacheMisses();
    return totalCacheAccesses > 0 ? (double) getCacheHits() / totalCacheAccesses : 0.0;
  }

  /**
   * Calculates the average compilation time.
   *
   * @return average compilation time
   */
  public Duration getAverageCompilationTime() {
    final long successful = getSuccessfulCompilations();
    return successful > 0
        ? Duration.ofNanos(totalCompilationTime.get() / successful)
        : Duration.ZERO;
  }

  /**
   * Calculates the average compilation throughput in bytes per second.
   *
   * @return average throughput
   */
  public double getAverageThroughput() {
    final Duration avgTime = getAverageCompilationTime();
    final long successful = getSuccessfulCompilations();
    if (avgTime.isZero() || successful == 0) {
      return 0.0;
    }

    final double avgBytes = (double) getTotalBytesProcessed() / successful;
    final double seconds = avgTime.toNanos() / 1_000_000_000.0;
    return avgBytes / seconds;
  }

  /**
   * Gets the uptime of the statistics collection.
   *
   * @return uptime duration
   */
  public Duration getUptime() {
    return Duration.between(startTime, Instant.now());
  }

  /**
   * Creates a snapshot of current statistics as a map.
   *
   * @return statistics snapshot
   */
  public Map<String, Object> snapshot() {
    return Map.of(
        "totalCompilations", getTotalCompilations(),
        "successfulCompilations", getSuccessfulCompilations(),
        "failedCompilations", getFailedCompilations(),
        "cancelledCompilations", getCancelledCompilations(),
        "activeCompilations", getActiveCompilations(),
        "peakActiveCompilations", getPeakActiveCompilations(),
        "successRate", getSuccessRate(),
        "cacheHitRate", getCacheHitRate(),
        "averageThroughput", getAverageThroughput(),
        "averageThreadUtilization", getAverageThreadUtilization(),
        "uptime", getUptime().toString());
  }

  /**
   * Resets all statistics to initial values.
   */
  public void reset() {
    totalCompilations.set(0);
    successfulCompilations.set(0);
    failedCompilations.set(0);
    cancelledCompilations.set(0);
    totalBytesProcessed.set(0);
    totalCompilationTime.set(0);
    activeCompilations.set(0);
    peakActiveCompilations.set(0);
    cacheHits.set(0);
    cacheMisses.set(0);
    peakMemoryUsage.set(0);
    averageThreadUtilization.set(0.0);
    lastCompilationTime = Instant.now();
  }

  @Override
  public String toString() {
    return String.format(
        "AsyncEngineStatistics{total=%d, success=%.1f%%, active=%d, throughput=%.2f KB/s}",
        getTotalCompilations(),
        getSuccessRate() * 100,
        getActiveCompilations(),
        getAverageThroughput() / 1024.0);
  }

  private static class AtomicDouble {
    private volatile double value;

    public AtomicDouble(final double initialValue) {
      this.value = initialValue;
    }

    public double get() {
      return value;
    }

    public void set(final double newValue) {
      this.value = newValue;
    }
  }
}