package ai.tegmentum.wasmtime4j.diagnostics;

import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * A snapshot of WebAssembly performance metrics and system state at a specific point in time.
 *
 * <p>This class captures comprehensive performance information including memory usage, operation
 * statistics, garbage collection impact, and error handling overhead. It provides both raw
 * metrics and formatted reporting capabilities.
 *
 * <p>Usage example:
 * <pre>{@code
 * PerformanceSnapshot snapshot = performanceDiagnostics.captureSnapshot();
 * System.out.println(snapshot.getFormattedReport());
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PerformanceSnapshot {

  // Capture metadata
  private Instant captureTime;

  // Memory information
  private MemoryUsage heapMemoryUsage;
  private MemoryUsage nonHeapMemoryUsage;

  // Runtime information
  private long uptime;

  // Thread information
  private int threadCount;
  private int daemonThreadCount;
  private int peakThreadCount;

  // Garbage collection information
  private long totalGcCollections;
  private long totalGcTime;

  // Operation statistics
  private Map<String, OperationStatistics> operationStatistics;
  private int activeOperationCount;

  // Error handling performance
  private double averageErrorHandlingOverhead;
  private long totalErrorHandlingOperations;

  /**
   * Gets the time when this snapshot was captured.
   *
   * @return the capture time
   */
  public Instant getCaptureTime() {
    return captureTime;
  }

  /**
   * Sets the time when this snapshot was captured.
   *
   * @param captureTime the capture time
   */
  public void setCaptureTime(final Instant captureTime) {
    this.captureTime = captureTime;
  }

  /**
   * Gets the heap memory usage at capture time.
   *
   * @return the heap memory usage
   */
  public MemoryUsage getHeapMemoryUsage() {
    return heapMemoryUsage;
  }

  /**
   * Sets the heap memory usage.
   *
   * @param heapMemoryUsage the heap memory usage
   */
  public void setHeapMemoryUsage(final MemoryUsage heapMemoryUsage) {
    this.heapMemoryUsage = heapMemoryUsage;
  }

  /**
   * Gets the non-heap memory usage at capture time.
   *
   * @return the non-heap memory usage
   */
  public MemoryUsage getNonHeapMemoryUsage() {
    return nonHeapMemoryUsage;
  }

  /**
   * Sets the non-heap memory usage.
   *
   * @param nonHeapMemoryUsage the non-heap memory usage
   */
  public void setNonHeapMemoryUsage(final MemoryUsage nonHeapMemoryUsage) {
    this.nonHeapMemoryUsage = nonHeapMemoryUsage;
  }

  /**
   * Gets the JVM uptime in milliseconds.
   *
   * @return the uptime
   */
  public long getUptime() {
    return uptime;
  }

  /**
   * Sets the JVM uptime.
   *
   * @param uptime the uptime in milliseconds
   */
  public void setUptime(final long uptime) {
    this.uptime = uptime;
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
   * Sets the current thread count.
   *
   * @param threadCount the thread count
   */
  public void setThreadCount(final int threadCount) {
    this.threadCount = threadCount;
  }

  /**
   * Gets the current daemon thread count.
   *
   * @return the daemon thread count
   */
  public int getDaemonThreadCount() {
    return daemonThreadCount;
  }

  /**
   * Sets the current daemon thread count.
   *
   * @param daemonThreadCount the daemon thread count
   */
  public void setDaemonThreadCount(final int daemonThreadCount) {
    this.daemonThreadCount = daemonThreadCount;
  }

  /**
   * Gets the peak thread count since JVM start.
   *
   * @return the peak thread count
   */
  public int getPeakThreadCount() {
    return peakThreadCount;
  }

  /**
   * Sets the peak thread count.
   *
   * @param peakThreadCount the peak thread count
   */
  public void setPeakThreadCount(final int peakThreadCount) {
    this.peakThreadCount = peakThreadCount;
  }

  /**
   * Gets the total number of garbage collection cycles.
   *
   * @return the total GC collections
   */
  public long getTotalGcCollections() {
    return totalGcCollections;
  }

  /**
   * Sets the total number of garbage collection cycles.
   *
   * @param totalGcCollections the total GC collections
   */
  public void setTotalGcCollections(final long totalGcCollections) {
    this.totalGcCollections = totalGcCollections;
  }

  /**
   * Gets the total time spent in garbage collection in milliseconds.
   *
   * @return the total GC time
   */
  public long getTotalGcTime() {
    return totalGcTime;
  }

  /**
   * Sets the total time spent in garbage collection.
   *
   * @param totalGcTime the total GC time in milliseconds
   */
  public void setTotalGcTime(final long totalGcTime) {
    this.totalGcTime = totalGcTime;
  }

  /**
   * Gets the operation statistics by operation type.
   *
   * @return the operation statistics map
   */
  public Map<String, OperationStatistics> getOperationStatistics() {
    return operationStatistics;
  }

  /**
   * Sets the operation statistics.
   *
   * @param operationStatistics the operation statistics map
   */
  public void setOperationStatistics(final Map<String, OperationStatistics> operationStatistics) {
    this.operationStatistics = operationStatistics;
  }

  /**
   * Gets the number of active operations at capture time.
   *
   * @return the active operation count
   */
  public int getActiveOperationCount() {
    return activeOperationCount;
  }

  /**
   * Sets the number of active operations.
   *
   * @param activeOperationCount the active operation count
   */
  public void setActiveOperationCount(final int activeOperationCount) {
    this.activeOperationCount = activeOperationCount;
  }

  /**
   * Gets the average error handling overhead in milliseconds.
   *
   * @return the average error handling overhead
   */
  public double getAverageErrorHandlingOverhead() {
    return averageErrorHandlingOverhead;
  }

  /**
   * Sets the average error handling overhead.
   *
   * @param averageErrorHandlingOverhead the average error handling overhead in milliseconds
   */
  public void setAverageErrorHandlingOverhead(final double averageErrorHandlingOverhead) {
    this.averageErrorHandlingOverhead = averageErrorHandlingOverhead;
  }

  /**
   * Gets the total number of error handling operations.
   *
   * @return the total error handling operations
   */
  public long getTotalErrorHandlingOperations() {
    return totalErrorHandlingOperations;
  }

  /**
   * Sets the total number of error handling operations.
   *
   * @param totalErrorHandlingOperations the total error handling operations
   */
  public void setTotalErrorHandlingOperations(final long totalErrorHandlingOperations) {
    this.totalErrorHandlingOperations = totalErrorHandlingOperations;
  }

  /**
   * Gets the heap memory utilization percentage.
   *
   * @return the heap utilization percentage (0-100)
   */
  public double getHeapUtilizationPercentage() {
    if (heapMemoryUsage == null || heapMemoryUsage.getMax() <= 0) {
      return 0.0;
    }
    return (double) heapMemoryUsage.getUsed() / heapMemoryUsage.getMax() * 100.0;
  }

  /**
   * Gets the non-heap memory utilization percentage.
   *
   * @return the non-heap utilization percentage (0-100), or -1 if max is undefined
   */
  public double getNonHeapUtilizationPercentage() {
    if (nonHeapMemoryUsage == null || nonHeapMemoryUsage.getMax() <= 0) {
      return -1.0;
    }
    return (double) nonHeapMemoryUsage.getUsed() / nonHeapMemoryUsage.getMax() * 100.0;
  }

  /**
   * Gets the garbage collection overhead percentage.
   *
   * @return the GC overhead percentage (0-100), or 0 if uptime is 0
   */
  public double getGcOverheadPercentage() {
    if (uptime == 0) {
      return 0.0;
    }
    return (double) totalGcTime / uptime * 100.0;
  }

  /**
   * Generates a formatted performance report.
   *
   * @return a formatted string containing performance metrics
   */
  public String getFormattedReport() {
    final StringBuilder report = new StringBuilder();

    // Header
    report.append("=== WebAssembly Performance Snapshot ===\n");
    if (captureTime != null) {
      report.append("Captured: ").append(captureTime.toString()).append("\n");
    }
    report.append("\n");

    // System Information
    report.append("--- System Information ---\n");
    report.append(String.format("Uptime: %d ms (%.1f minutes)\n", uptime, uptime / 60000.0));
    report.append(String.format("Threads: %d active, %d daemon, %d peak\n",
        threadCount, daemonThreadCount, peakThreadCount));
    report.append("\n");

    // Memory Information
    report.append("--- Memory Usage ---\n");
    if (heapMemoryUsage != null) {
      report.append(String.format("Heap: %s / %s (%.1f%% used)\n",
          formatMemory(heapMemoryUsage.getUsed()),
          formatMemory(heapMemoryUsage.getMax()),
          getHeapUtilizationPercentage()));
    }
    if (nonHeapMemoryUsage != null) {
      report.append(String.format("Non-Heap: %s", formatMemory(nonHeapMemoryUsage.getUsed())));
      if (nonHeapMemoryUsage.getMax() > 0) {
        report.append(String.format(" / %s (%.1f%% used)",
            formatMemory(nonHeapMemoryUsage.getMax()),
            getNonHeapUtilizationPercentage()));
      }
      report.append("\n");
    }
    report.append("\n");

    // Garbage Collection
    report.append("--- Garbage Collection ---\n");
    report.append(String.format("Collections: %d total, %d ms total time (%.2f%% overhead)\n",
        totalGcCollections, totalGcTime, getGcOverheadPercentage()));
    if (totalGcCollections > 0) {
      report.append(String.format("Average per collection: %.1f ms\n",
          (double) totalGcTime / totalGcCollections));
    }
    report.append("\n");

    // Operation Statistics
    report.append("--- Operation Statistics ---\n");
    report.append(String.format("Active operations: %d\n", activeOperationCount));
    if (operationStatistics != null && !operationStatistics.isEmpty()) {
      operationStatistics.forEach((type, stats) -> {
        if (stats.hasOperations()) {
          report.append(String.format("%s: %d ops, avg %.1f ms, %.1f ops/sec\n",
              type, stats.getOperationCount(), stats.getAverageDuration(),
              stats.getOperationsPerSecond()));
        }
      });
    } else {
      report.append("No completed operations recorded\n");
    }
    report.append("\n");

    // Error Handling Performance
    report.append("--- Error Handling Performance ---\n");
    if (totalErrorHandlingOperations > 0) {
      report.append(String.format("Error operations: %d total, %.3f ms average overhead\n",
          totalErrorHandlingOperations, averageErrorHandlingOverhead));
    } else {
      report.append("No error handling operations recorded\n");
    }

    return report.toString();
  }

  /**
   * Generates a compact one-line summary of key metrics.
   *
   * @return a compact summary string
   */
  public String getCompactSummary() {
    return String.format(
        "Memory: %.1f%% heap, Threads: %d, GC: %.2f%% overhead, Ops: %d active, Errors: %d handled",
        getHeapUtilizationPercentage(),
        threadCount,
        getGcOverheadPercentage(),
        activeOperationCount,
        totalErrorHandlingOperations
    );
  }

  private String formatMemory(final long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else if (bytes < 1024 * 1024 * 1024) {
      return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    } else {
      return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
  }
}