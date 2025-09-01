package ai.tegmentum.wasmtime4j.wasi;

import java.time.Duration;

/**
 * Statistics for WASI resource usage.
 *
 * @since 1.0.0
 */
public interface WasiResourceStats {

  /**
   * Gets the number of times this resource has been accessed.
   *
   * @return access count
   */
  long getAccessCount();

  /**
   * Gets the total time this resource has been in use.
   *
   * @return total usage time
   */
  Duration getTotalUsageTime();

  /**
   * Gets the number of operations performed on this resource.
   *
   * @return operation count
   */
  long getOperationCount();

  /**
   * Gets the number of errors encountered with this resource.
   *
   * @return error count
   */
  long getErrorCount();
}
