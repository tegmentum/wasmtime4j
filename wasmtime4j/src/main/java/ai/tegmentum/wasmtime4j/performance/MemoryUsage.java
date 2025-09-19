package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.util.Objects;

/**
 * Detailed memory usage information for the Java Virtual Machine and native components.
 *
 * <p>This class provides comprehensive memory statistics including heap usage, non-heap usage,
 * direct memory, native memory, and garbage collection metrics.
 *
 * @since 1.0.0
 */
public final class MemoryUsage {
  private final long heapUsed;
  private final long heapCommitted;
  private final long heapMax;
  private final long nonHeapUsed;
  private final long nonHeapCommitted;
  private final long directMemoryUsed;
  private final long nativeMemoryUsed;
  private final long gcCount;
  private final Duration gcTime;

  /**
   * Creates a memory usage record.
   *
   * @param heapUsed bytes of heap memory currently used
   * @param heapCommitted bytes of heap memory committed by JVM
   * @param heapMax maximum heap memory bytes available
   * @param nonHeapUsed bytes of non-heap memory currently used
   * @param nonHeapCommitted bytes of non-heap memory committed by JVM
   * @param directMemoryUsed bytes of direct memory currently used
   * @param nativeMemoryUsed bytes of native memory currently used
   * @param gcCount total number of garbage collections
   * @param gcTime cumulative time spent in garbage collection
   */
  public MemoryUsage(
      final long heapUsed,
      final long heapCommitted,
      final long heapMax,
      final long nonHeapUsed,
      final long nonHeapCommitted,
      final long directMemoryUsed,
      final long nativeMemoryUsed,
      final long gcCount,
      final Duration gcTime) {
    this.heapUsed = Math.max(0, heapUsed);
    this.heapCommitted = Math.max(0, heapCommitted);
    this.heapMax = Math.max(0, heapMax);
    this.nonHeapUsed = Math.max(0, nonHeapUsed);
    this.nonHeapCommitted = Math.max(0, nonHeapCommitted);
    this.directMemoryUsed = Math.max(0, directMemoryUsed);
    this.nativeMemoryUsed = Math.max(0, nativeMemoryUsed);
    this.gcCount = Math.max(0, gcCount);
    this.gcTime = Objects.requireNonNull(gcTime, "gcTime cannot be null");

    if (gcTime.isNegative()) {
      throw new IllegalArgumentException("gcTime cannot be negative: " + gcTime);
    }
  }

  /**
   * Gets the amount of heap memory currently used.
   *
   * @return heap memory used in bytes
   */
  public long getHeapUsed() {
    return heapUsed;
  }

  /**
   * Gets the amount of heap memory committed by the JVM.
   *
   * @return heap memory committed in bytes
   */
  public long getHeapCommitted() {
    return heapCommitted;
  }

  /**
   * Gets the maximum heap memory available.
   *
   * @return maximum heap memory in bytes
   */
  public long getHeapMax() {
    return heapMax;
  }

  /**
   * Gets the amount of non-heap memory currently used.
   *
   * @return non-heap memory used in bytes
   */
  public long getNonHeapUsed() {
    return nonHeapUsed;
  }

  /**
   * Gets the amount of non-heap memory committed by the JVM.
   *
   * @return non-heap memory committed in bytes
   */
  public long getNonHeapCommitted() {
    return nonHeapCommitted;
  }

  /**
   * Gets the amount of direct memory currently used.
   *
   * @return direct memory used in bytes
   */
  public long getDirectMemoryUsed() {
    return directMemoryUsed;
  }

  /**
   * Gets the amount of native memory currently used.
   *
   * @return native memory used in bytes
   */
  public long getNativeMemoryUsed() {
    return nativeMemoryUsed;
  }

  /**
   * Gets the total number of garbage collections.
   *
   * @return garbage collection count
   */
  public long getGcCount() {
    return gcCount;
  }

  /**
   * Gets the cumulative time spent in garbage collection.
   *
   * @return garbage collection time
   */
  public Duration getGcTime() {
    return gcTime;
  }

  /**
   * Gets the heap utilization as a ratio (0.0 to 1.0).
   *
   * @return heap utilization ratio
   */
  public double getHeapUtilization() {
    return heapMax > 0 ? (double) heapUsed / heapMax : 0.0;
  }

  /**
   * Gets the total memory used (heap + non-heap + direct + native).
   *
   * @return total memory used in bytes
   */
  public long getTotalMemoryUsed() {
    return heapUsed + nonHeapUsed + directMemoryUsed + nativeMemoryUsed;
  }

  /**
   * Checks if the system is under memory pressure.
   *
   * <p>Returns true if heap utilization exceeds 80%.
   *
   * @return true if under memory pressure
   */
  public boolean isMemoryPressure() {
    return getHeapUtilization() > 0.8;
  }

  /**
   * Checks if garbage collection is frequent.
   *
   * <p>Returns true if more than 5% of time is spent in GC (requires uptime context).
   *
   * @param uptime the total uptime to compare GC time against
   * @return true if GC is frequent
   */
  public boolean isFrequentGarbageCollection(final Duration uptime) {
    if (uptime.isZero()) {
      return false;
    }
    final double gcRatio = (double) gcTime.toNanos() / uptime.toNanos();
    return gcRatio > 0.05; // More than 5% of time in GC
  }

  /**
   * Gets the memory efficiency score (0.0 to 1.0).
   *
   * <p>Higher scores indicate better memory efficiency (low GC overhead, good utilization).
   *
   * @param uptime the total uptime for GC ratio calculation
   * @return memory efficiency score
   */
  public double getEfficiencyScore(final Duration uptime) {
    double score = 1.0;

    // Penalize high heap utilization
    final double heapUtil = getHeapUtilization();
    if (heapUtil > 0.9) {
      score -= 0.4;
    } else if (heapUtil > 0.8) {
      score -= 0.2;
    }

    // Penalize frequent GC
    if (!uptime.isZero()) {
      final double gcRatio = (double) gcTime.toNanos() / uptime.toNanos();
      if (gcRatio > 0.1) {
        score -= 0.4;
      } else if (gcRatio > 0.05) {
        score -= 0.2;
      }
    }

    return Math.max(0.0, score);
  }

  /**
   * Formats memory size in human-readable format.
   *
   * @param bytes the number of bytes
   * @return formatted size string
   */
  public static String formatBytes(final long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    final int unit = 1024;
    final int exp = (int) (Math.log(bytes) / Math.log(unit));
    final String pre = "KMGTPE".charAt(exp - 1) + "";
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MemoryUsage that = (MemoryUsage) obj;
    return heapUsed == that.heapUsed &&
        heapCommitted == that.heapCommitted &&
        heapMax == that.heapMax &&
        nonHeapUsed == that.nonHeapUsed &&
        nonHeapCommitted == that.nonHeapCommitted &&
        directMemoryUsed == that.directMemoryUsed &&
        nativeMemoryUsed == that.nativeMemoryUsed &&
        gcCount == that.gcCount &&
        Objects.equals(gcTime, that.gcTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(heapUsed, heapCommitted, heapMax, nonHeapUsed, nonHeapCommitted,
        directMemoryUsed, nativeMemoryUsed, gcCount, gcTime);
  }

  @Override
  public String toString() {
    return String.format(
        "MemoryUsage{heapUsed=%s, heapCommitted=%s, heapMax=%s, " +
        "nonHeapUsed=%s, directMemoryUsed=%s, nativeMemoryUsed=%s, " +
        "gcCount=%d, gcTime=%s, utilization=%.1f%%}",
        formatBytes(heapUsed), formatBytes(heapCommitted), formatBytes(heapMax),
        formatBytes(nonHeapUsed), formatBytes(directMemoryUsed), formatBytes(nativeMemoryUsed),
        gcCount, gcTime, getHeapUtilization() * 100);
  }
}