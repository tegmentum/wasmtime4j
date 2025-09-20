package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Comprehensive resource usage monitoring for WebAssembly operations.
 *
 * <p>This interface provides detailed metrics about system resource consumption including memory,
 * CPU, threads, I/O, and network usage. Resource usage can be captured as instantaneous snapshots
 * or measured over time windows.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Capture current resource usage
 * ResourceUsage usage = ResourceUsage.capture();
 * System.out.println("Heap usage: " + usage.getMemoryUsage().getHeapUtilization());
 * System.out.println("CPU usage: " + usage.getCpuUsage().getCpuUtilization());
 *
 * // Capture over a time window
 * ResourceUsage windowUsage = ResourceUsage.capture(Duration.ofSeconds(30));
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ResourceUsage {

  /**
   * Captures current resource usage as an instantaneous snapshot.
   *
   * @return current resource usage snapshot
   */
  static ResourceUsage capture() {
    throw new UnsupportedOperationException("Implementation must be provided by runtime factory");
  }

  /**
   * Captures resource usage measured over a specified time window.
   *
   * <p>This method will block for the specified duration to collect accurate measurements of
   * resource consumption rates.
   *
   * @param window the time window to measure over
   * @return resource usage measured over the window
   * @throws IllegalArgumentException if window is null or negative
   */
  static ResourceUsage capture(final Duration window) {
    throw new UnsupportedOperationException("Implementation must be provided by runtime factory");
  }

  /**
   * Gets memory usage information.
   *
   * @return memory usage details
   */
  MemoryUsage getMemoryUsage();

  /**
   * Gets memory usage broken down by type.
   *
   * @return map of memory type to usage details
   */
  Map<String, MemoryUsage> getMemoryUsageByType();

  /**
   * Gets CPU usage information.
   *
   * @return CPU usage details
   */
  CpuUsage getCpuUsage();

  /**
   * Gets CPU usage broken down by core.
   *
   * @return map of core ID to CPU usage details
   */
  Map<Integer, CpuUsage> getCpuUsageByCore();

  /**
   * Gets thread usage information.
   *
   * @return thread usage details
   */
  ThreadUsage getThreadUsage();

  /**
   * Gets thread usage broken down by thread pool.
   *
   * @return map of pool name to thread usage details
   */
  Map<String, ThreadUsage> getThreadUsageByPool();

  /**
   * Gets I/O usage information.
   *
   * @return I/O usage details
   */
  IoUsage getIoUsage();

  /**
   * Gets network usage information if networking is enabled.
   *
   * @return network usage details, or empty if networking disabled
   */
  Optional<NetworkUsage> getNetworkUsage();

  /**
   * Gets the timestamp when this resource usage was captured.
   *
   * @return capture timestamp
   */
  Instant getCaptureTime();

  /**
   * Gets the measurement window duration.
   *
   * <p>Returns Duration.ZERO for instantaneous snapshots.
   *
   * @return measurement window duration
   */
  Duration getMeasurementWindow();

  /**
   * Checks if the system is under resource pressure.
   *
   * <p>Returns true if any resource (memory, CPU, I/O) is highly utilized.
   *
   * @return true if system is under pressure
   */
  default boolean isUnderResourcePressure() {
    return getMemoryUsage().isMemoryPressure()
        || getCpuUsage().isHighCpuUsage()
        || getIoUsage().isHighIoLoad();
  }

  /**
   * Gets the overall resource utilization score.
   *
   * <p>The score is calculated from memory, CPU, and I/O utilization. Score ranges from 0.0 (idle)
   * to 1.0 (fully utilized).
   *
   * @return overall resource utilization (0.0-1.0)
   */
  default double getOverallUtilization() {
    final double memoryUtil = getMemoryUsage().getHeapUtilization();
    final double cpuUtil = getCpuUsage().getCpuUtilization();
    final double ioUtil = getIoUsage().getUtilization();

    return (memoryUtil + cpuUtil + ioUtil) / 3.0;
  }

  /**
   * Gets extended resource metrics as a map.
   *
   * <p>This method provides access to platform-specific or implementation-specific resource metrics
   * that may not be covered by the standard interface methods.
   *
   * @return map of extended resource metrics
   */
  Map<String, Object> getExtendedMetrics();
}
