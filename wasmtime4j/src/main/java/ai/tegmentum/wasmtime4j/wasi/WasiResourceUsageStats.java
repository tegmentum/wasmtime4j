package ai.tegmentum.wasmtime4j.wasi;

import java.util.Map;

/**
 * Resource usage statistics for WASI components.
 *
 * @since 1.0.0
 */
public interface WasiResourceUsageStats {

  /**
   * Gets the total number of resources created.
   *
   * @return total resource creation count
   */
  long getTotalResourcesCreated();

  /**
   * Gets the current number of active resources.
   *
   * @return current active resource count
   */
  int getCurrentResourceCount();

  /**
   * Gets the peak number of simultaneous resources.
   *
   * @return peak resource count
   */
  int getPeakResourceCount();

  /**
   * Gets resource counts by type.
   *
   * @return map of resource types to current counts
   */
  Map<String, Integer> getResourceCountsByType();

  /**
   * Gets resource creation counts by type.
   *
   * @return map of resource types to total creation counts
   */
  Map<String, Long> getResourceCreationsByType();
}
