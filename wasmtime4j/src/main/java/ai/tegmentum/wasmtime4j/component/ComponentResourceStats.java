package ai.tegmentum.wasmtime4j.component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Statistics for component resource usage and management.
 *
 * <p>ComponentResourceStats provides detailed metrics about resource creation, usage, lifecycle
 * management, and cleanup within a component instance. This includes both component-model resources
 * and system resources used by the component.
 *
 * @since 1.0.0
 */
public interface ComponentResourceStats {

  /**
   * Gets the total number of resources created.
   *
   * <p>Returns the cumulative count of all resource instances created by this component instance
   * since creation or last reset.
   *
   * @return total resource creation count
   */
  long getTotalResourcesCreated();

  /**
   * Gets the number of resources currently active.
   *
   * <p>Returns the count of resource instances that are currently alive and managed by this
   * component instance.
   *
   * @return current active resource count
   */
  int getCurrentActiveResources();

  /**
   * Gets the peak number of active resources.
   *
   * <p>Returns the maximum number of resources that were simultaneously active at any point during
   * the component instance's lifetime.
   *
   * @return peak active resource count
   */
  int getPeakActiveResources();

  /**
   * Gets the total number of resources destroyed.
   *
   * <p>Returns the cumulative count of resource instances that have been explicitly destroyed or
   * garbage collected.
   *
   * @return total resource destruction count
   */
  long getTotalResourcesDestroyed();

  /**
   * Gets the total time spent in resource operations.
   *
   * <p>Returns the cumulative time spent creating, managing, and destroying resources.
   *
   * @return total resource operation time
   */
  Duration getTotalResourceOperationTime();

  /**
   * Gets the average resource lifetime.
   *
   * <p>Returns the mean duration between resource creation and destruction for resources that have
   * completed their lifecycle.
   *
   * @return average resource lifetime
   */
  Duration getAverageResourceLifetime();

  /**
   * Gets resource statistics by type.
   *
   * <p>Returns detailed statistics broken down by resource type, including creation counts, active
   * counts, and lifecycle metrics for each resource type.
   *
   * @return map of resource type names to their statistics
   */
  Map<String, ComponentResourceTypeStats> getResourceStatsByType();

  /**
   * Gets the number of resource leaks detected.
   *
   * <p>Returns the count of resources that were not properly cleaned up and had to be garbage
   * collected or forcibly destroyed.
   *
   * @return resource leak count
   */
  long getResourceLeakCount();

  /**
   * Gets the total memory used by active resources.
   *
   * <p>Returns the current memory usage of all active resource instances managed by this component.
   *
   * @return total resource memory usage in bytes
   */
  long getTotalResourceMemoryUsage();

  /**
   * Gets the average memory usage per resource.
   *
   * <p>Returns the mean memory usage across all currently active resource instances.
   *
   * @return average memory usage per resource in bytes
   */
  long getAverageResourceMemoryUsage();

  /**
   * Gets the number of resource recycling operations.
   *
   * <p>Returns the count of times resources were recycled or reused rather than creating new
   * instances.
   *
   * @return resource recycling count
   */
  long getResourceRecyclingCount();

  /**
   * Gets the resource creation rate.
   *
   * <p>Returns the current rate of resource creation measured over a recent time window.
   *
   * @return resource creation rate per second
   */
  double getResourceCreationRate();

  /**
   * Gets the resource destruction rate.
   *
   * <p>Returns the current rate of resource destruction measured over a recent time window.
   *
   * @return resource destruction rate per second
   */
  double getResourceDestructionRate();

  /**
   * Gets the timestamp of the first resource creation.
   *
   * <p>Returns the time when the first resource was created by this component instance, or null if
   * no resources have been created.
   *
   * @return first resource creation timestamp, or null if none created
   */
  Instant getFirstResourceCreationTime();

  /**
   * Gets the timestamp of the most recent resource operation.
   *
   * <p>Returns the time when the most recent resource operation (creation, destruction, or
   * modification) occurred.
   *
   * @return last resource operation timestamp, or null if no operations
   */
  Instant getLastResourceOperationTime();

  /**
   * Resets all resource statistics to zero.
   *
   * <p>Clears all accumulated statistics while preserving information about currently active
   * resources. Timestamps are reset and counters are cleared.
   */
  void reset();
}
