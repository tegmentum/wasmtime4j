package ai.tegmentum.wasmtime4j.compilation;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monitors system performance metrics for adaptive compilation decisions.
 *
 * <p>This class provides real-time monitoring of system resources including CPU utilization,
 * memory pressure, and compilation overhead to help make informed tier transition decisions.
 *
 * @since 1.0.0
 */
public final class SystemPerformanceMonitor {

  private final OperatingSystemMXBean osBean;
  private final MemoryMXBean memoryBean;

  // Cached metrics with timestamps
  private final AtomicReference<CachedMetric<Double>> cachedCpuUtilization;
  private final AtomicReference<CachedMetric<Double>> cachedMemoryPressure;
  private final AtomicLong lastUpdateTime;

  // Cache validity duration (1 second)
  private static final long CACHE_VALIDITY_MS = 1000;

  // Compilation overhead tracking
  private final AtomicLong totalCompilationTime;
  private final AtomicLong totalExecutionTime;

  /**
   * Creates a new system performance monitor.
   */
  public SystemPerformanceMonitor() {
    this.osBean = ManagementFactory.getOperatingSystemMXBean();
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.cachedCpuUtilization = new AtomicReference<>();
    this.cachedMemoryPressure = new AtomicReference<>();
    this.lastUpdateTime = new AtomicLong(0);
    this.totalCompilationTime = new AtomicLong(0);
    this.totalExecutionTime = new AtomicLong(0);
  }

  /**
   * Gets current CPU utilization (0.0 to 1.0).
   *
   * @return CPU utilization percentage
   */
  public double getCpuUtilization() {
    final long currentTime = System.currentTimeMillis();
    final CachedMetric<Double> cached = cachedCpuUtilization.get();

    // Return cached value if still valid
    if (cached != null && (currentTime - cached.timestamp) < CACHE_VALIDITY_MS) {
      return cached.value;
    }

    // Calculate new CPU utilization
    final double cpuUtilization = calculateCpuUtilization();
    cachedCpuUtilization.set(new CachedMetric<>(cpuUtilization, currentTime));

    return cpuUtilization;
  }

  /**
   * Gets current memory pressure (0.0 to 1.0).
   *
   * @return memory pressure percentage
   */
  public double getMemoryPressure() {
    final long currentTime = System.currentTimeMillis();
    final CachedMetric<Double> cached = cachedMemoryPressure.get();

    // Return cached value if still valid
    if (cached != null && (currentTime - cached.timestamp) < CACHE_VALIDITY_MS) {
      return cached.value;
    }

    // Calculate new memory pressure
    final double memoryPressure = calculateMemoryPressure();
    cachedMemoryPressure.set(new CachedMetric<>(memoryPressure, currentTime));

    return memoryPressure;
  }

  /**
   * Calculates CPU utilization using available system metrics.
   */
  private double calculateCpuUtilization() {
    try {
      // Try to get process CPU load first
      if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
        final com.sun.management.OperatingSystemMXBean sunOsBean =
            (com.sun.management.OperatingSystemMXBean) osBean;

        final double processCpuLoad = sunOsBean.getProcessCpuLoad();
        if (processCpuLoad >= 0.0 && processCpuLoad <= 1.0) {
          return processCpuLoad;
        }

        // Fall back to system CPU load
        final double systemCpuLoad = sunOsBean.getSystemCpuLoad();
        if (systemCpuLoad >= 0.0 && systemCpuLoad <= 1.0) {
          return systemCpuLoad;
        }
      }

      // Fall back to load average estimation
      final double loadAverage = osBean.getSystemLoadAverage();
      if (loadAverage >= 0.0) {
        final int availableProcessors = osBean.getAvailableProcessors();
        return Math.min(loadAverage / availableProcessors, 1.0);
      }

    } catch (final Exception e) {
      // Fall back to conservative estimate
    }

    // Conservative default if unable to measure
    return 0.5;
  }

  /**
   * Calculates memory pressure based on heap usage.
   */
  private double calculateMemoryPressure() {
    try {
      final long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
      final long heapMax = memoryBean.getHeapMemoryUsage().getMax();

      if (heapMax > 0) {
        return Math.min((double) heapUsed / heapMax, 1.0);
      }

      // Fall back to committed memory if max is not available
      final long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
      if (heapCommitted > 0) {
        return Math.min((double) heapUsed / heapCommitted, 1.0);
      }

    } catch (final Exception e) {
      // Fall back to conservative estimate
    }

    // Conservative default if unable to measure
    return 0.3;
  }

  /**
   * Records compilation time for overhead calculation.
   *
   * @param compilationTimeMs compilation time in milliseconds
   */
  public void recordCompilationTime(final long compilationTimeMs) {
    totalCompilationTime.addAndGet(compilationTimeMs);
  }

  /**
   * Records execution time for overhead calculation.
   *
   * @param executionTimeMs execution time in milliseconds
   */
  public void recordExecutionTime(final long executionTimeMs) {
    totalExecutionTime.addAndGet(executionTimeMs);
  }

  /**
   * Gets current compilation overhead ratio (0.0 to 1.0+).
   *
   * @return compilation overhead ratio
   */
  public double getCompilationOverhead() {
    final long compTime = totalCompilationTime.get();
    final long execTime = totalExecutionTime.get();

    if (execTime == 0) {
      return compTime > 0 ? 1.0 : 0.0;
    }

    return (double) compTime / execTime;
  }

  /**
   * Determines if the system is under high load.
   *
   * @return true if system is under high load
   */
  public boolean isHighLoad() {
    return getCpuUtilization() > 0.8 || getMemoryPressure() > 0.8;
  }

  /**
   * Determines if the system is under low load.
   *
   * @return true if system is under low load
   */
  public boolean isLowLoad() {
    return getCpuUtilization() < 0.3 && getMemoryPressure() < 0.3;
  }

  /**
   * Gets system load category.
   *
   * @return load category
   */
  public LoadCategory getLoadCategory() {
    final double cpuUtil = getCpuUtilization();
    final double memoryPressure = getMemoryPressure();

    if (cpuUtil > 0.8 || memoryPressure > 0.8) {
      return LoadCategory.HIGH;
    } else if (cpuUtil > 0.5 || memoryPressure > 0.5) {
      return LoadCategory.MEDIUM;
    } else {
      return LoadCategory.LOW;
    }
  }

  /**
   * Resets compilation overhead tracking.
   */
  public void resetOverheadTracking() {
    totalCompilationTime.set(0);
    totalExecutionTime.set(0);
  }

  /**
   * Gets comprehensive performance metrics.
   *
   * @return performance metrics
   */
  public SystemPerformanceMetrics getMetrics() {
    return new SystemPerformanceMetrics(
        getCpuUtilization(),
        getMemoryPressure(),
        getCompilationOverhead(),
        getLoadCategory(),
        System.currentTimeMillis()
    );
  }

  /**
   * System load categories.
   */
  public enum LoadCategory {
    LOW,
    MEDIUM,
    HIGH
  }

  /**
   * Comprehensive system performance metrics.
   */
  public static final class SystemPerformanceMetrics {
    private final double cpuUtilization;
    private final double memoryPressure;
    private final double compilationOverhead;
    private final LoadCategory loadCategory;
    private final long timestamp;

    public SystemPerformanceMetrics(final double cpuUtilization,
                                    final double memoryPressure,
                                    final double compilationOverhead,
                                    final LoadCategory loadCategory,
                                    final long timestamp) {
      this.cpuUtilization = cpuUtilization;
      this.memoryPressure = memoryPressure;
      this.compilationOverhead = compilationOverhead;
      this.loadCategory = loadCategory;
      this.timestamp = timestamp;
    }

    public double getCpuUtilization() { return cpuUtilization; }
    public double getMemoryPressure() { return memoryPressure; }
    public double getCompilationOverhead() { return compilationOverhead; }
    public LoadCategory getLoadCategory() { return loadCategory; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
      return String.format(
          "SystemMetrics{cpu=%.1f%%, memory=%.1f%%, compOverhead=%.1f%%, load=%s}",
          cpuUtilization * 100,
          memoryPressure * 100,
          compilationOverhead * 100,
          loadCategory
      );
    }
  }

  /**
   * Cached metric with timestamp for cache invalidation.
   */
  private static final class CachedMetric<T> {
    final T value;
    final long timestamp;

    CachedMetric(final T value, final long timestamp) {
      this.value = value;
      this.timestamp = timestamp;
    }
  }
}