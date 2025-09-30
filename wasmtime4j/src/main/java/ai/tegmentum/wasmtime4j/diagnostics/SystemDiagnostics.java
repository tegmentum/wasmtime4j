package ai.tegmentum.wasmtime4j.diagnostics;

/**
 * System diagnostics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SystemDiagnostics {

  /**
   * Gets the system metrics.
   *
   * @return system metrics
   */
  SystemMetrics getSystemMetrics();

  /**
   * Gets the memory usage information.
   *
   * @return memory usage
   */
  MemoryUsage getMemoryUsage();

  /**
   * Gets the CPU usage percentage.
   *
   * @return CPU usage as percentage
   */
  double getCpuUsage();

  /**
   * Gets the thread count.
   *
   * @return number of threads
   */
  int getThreadCount();

  /** System metrics interface. */
  interface SystemMetrics {
    /**
     * Gets the uptime in milliseconds.
     *
     * @return uptime
     */
    long getUptime();

    /**
     * Gets the total memory in bytes.
     *
     * @return total memory
     */
    long getTotalMemory();

    /**
     * Gets the available memory in bytes.
     *
     * @return available memory
     */
    long getAvailableMemory();
  }

  /** Memory usage interface. */
  interface MemoryUsage {
    /**
     * Gets the used memory in bytes.
     *
     * @return used memory
     */
    long getUsedMemory();

    /**
     * Gets the allocated memory in bytes.
     *
     * @return allocated memory
     */
    long getAllocatedMemory();

    /**
     * Gets the peak memory usage in bytes.
     *
     * @return peak memory usage
     */
    long getPeakMemory();
  }
}
