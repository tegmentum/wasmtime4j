package ai.tegmentum.wasmtime4j.wasi;

/**
 * Snapshot performance metrics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SnapshotPerformanceMetrics {

  /**
   * Gets the snapshot creation time in milliseconds.
   *
   * @return creation time
   */
  long getCreationTime();

  /**
   * Gets the snapshot restore time in milliseconds.
   *
   * @return restore time
   */
  long getRestoreTime();

  /**
   * Gets the snapshot size in bytes.
   *
   * @return snapshot size
   */
  long getSnapshotSize();

  /**
   * Gets the compression ratio.
   *
   * @return compression ratio (0.0-1.0)
   */
  double getCompressionRatio();

  /**
   * Gets the throughput in MB/s.
   *
   * @return throughput
   */
  double getThroughput();

  /**
   * Gets the memory usage during snapshot operations.
   *
   * @return memory usage in bytes
   */
  long getMemoryUsage();

  /**
   * Gets the I/O operations count.
   *
   * @return I/O operations count
   */
  long getIoOperations();

  /**
   * Gets the average operation latency in microseconds.
   *
   * @return average latency
   */
  double getAverageLatency();

  /**
   * Gets snapshot validation metrics.
   *
   * @return validation metrics
   */
  ValidationMetrics getValidationMetrics();

  /** Validation metrics interface. */
  interface ValidationMetrics {
    /**
     * Gets the validation time in milliseconds.
     *
     * @return validation time
     */
    long getValidationTime();

    /**
     * Gets the number of validation checks performed.
     *
     * @return check count
     */
    int getValidationChecks();

    /**
     * Checks if validation passed.
     *
     * @return true if validation passed
     */
    boolean isValid();

    /**
     * Gets validation error count.
     *
     * @return error count
     */
    int getErrorCount();
  }
}
