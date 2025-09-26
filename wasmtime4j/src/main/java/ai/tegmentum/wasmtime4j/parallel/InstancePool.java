package ai.tegmentum.wasmtime4j.parallel;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Pool of WebAssembly instances for parallel execution and load balancing.
 *
 * <p>This interface provides instance pooling capabilities with automatic scaling,
 * health monitoring, and resource management for efficient parallel execution.
 *
 * @since 1.0.0
 */
public interface InstancePool extends AutoCloseable {

  /**
   * Acquires an available instance from the pool.
   *
   * @return CompletableFuture containing an available instance
   */
  CompletableFuture<PooledInstance> acquire();

  /**
   * Acquires an instance with a timeout.
   *
   * @param timeout maximum wait time for an available instance
   * @return CompletableFuture containing an available instance
   */
  CompletableFuture<PooledInstance> acquire(Duration timeout);

  /**
   * Returns an instance to the pool after use.
   *
   * @param instance the instance to return
   * @return CompletableFuture that completes when the instance is returned
   */
  CompletableFuture<Void> release(PooledInstance instance);

  /**
   * Gets the current number of available instances in the pool.
   *
   * @return available instances count
   */
  int getAvailableCount();

  /**
   * Gets the current number of instances in use.
   *
   * @return in-use instances count
   */
  int getInUseCount();

  /**
   * Gets the total number of instances in the pool.
   *
   * @return total instances count
   */
  int getTotalCount();

  /**
   * Gets the maximum pool size.
   *
   * @return maximum pool size
   */
  int getMaxSize();

  /**
   * Gets the minimum pool size.
   *
   * @return minimum pool size
   */
  int getMinSize();

  /**
   * Checks if the pool can scale up (create more instances).
   *
   * @return true if scaling up is possible
   */
  boolean canScaleUp();

  /**
   * Checks if the pool can scale down (remove instances).
   *
   * @return true if scaling down is possible
   */
  boolean canScaleDown();

  /**
   * Manually scales the pool to the specified size.
   *
   * @param targetSize target pool size
   * @return CompletableFuture that completes when scaling is done
   */
  CompletableFuture<ScalingResult> scaleTo(int targetSize);

  /**
   * Scales the pool up by the specified amount.
   *
   * @param additionalInstances number of instances to add
   * @return CompletableFuture that completes when scaling is done
   */
  CompletableFuture<ScalingResult> scaleUp(int additionalInstances);

  /**
   * Scales the pool down by the specified amount.
   *
   * @param instancesToRemove number of instances to remove
   * @return CompletableFuture that completes when scaling is done
   */
  CompletableFuture<ScalingResult> scaleDown(int instancesToRemove);

  /**
   * Enables or disables automatic scaling based on load.
   *
   * @param enabled true to enable automatic scaling
   * @return this pool for method chaining
   */
  InstancePool setAutoScaling(boolean enabled);

  /**
   * Gets the current pool statistics.
   *
   * @return pool statistics
   */
  InstancePoolStatistics getStatistics();

  /**
   * Gets the pool configuration.
   *
   * @return pool configuration
   */
  InstancePoolConfig getConfig();

  /**
   * Updates the pool configuration.
   *
   * @param config new configuration
   * @return CompletableFuture that completes when configuration is updated
   */
  CompletableFuture<Void> updateConfig(InstancePoolConfig config);

  /**
   * Gets the health status of all instances in the pool.
   *
   * @return list of instance health statuses
   */
  List<InstanceHealthStatus> getHealthStatuses();

  /**
   * Performs a health check on all instances in the pool.
   *
   * @return CompletableFuture containing health check results
   */
  CompletableFuture<PoolHealthCheckResult> performHealthCheck();

  /**
   * Removes unhealthy instances from the pool.
   *
   * @return CompletableFuture containing the cleanup result
   */
  CompletableFuture<PoolCleanupResult> removeUnhealthyInstances();

  /**
   * Warms up the pool by creating and initializing instances.
   *
   * @return CompletableFuture that completes when warmup is done
   */
  CompletableFuture<Void> warmup();

  /**
   * Preloads instances with the specified module.
   *
   * @param module module to preload
   * @param count number of instances to preload
   * @return CompletableFuture that completes when preloading is done
   */
  CompletableFuture<Void> preload(Module module, int count);

  /**
   * Sets a callback for pool events (scaling, health changes, etc.).
   *
   * @param callback event callback
   * @return this pool for method chaining
   */
  InstancePool setEventCallback(Consumer<InstancePoolEvent> callback);

  /**
   * Gets the current load factor of the pool (0.0 - 1.0).
   *
   * @return current load factor
   */
  double getLoadFactor();

  /**
   * Gets the average response time for instances in the pool.
   *
   * @return average response time
   */
  Duration getAverageResponseTime();

  /**
   * Gets the time when the pool was created.
   *
   * @return pool creation time
   */
  Instant getCreationTime();

  /**
   * Gets the pool uptime.
   *
   * @return uptime duration
   */
  Duration getUptime();

  /**
   * Drains the pool by preventing new acquisitions and waiting for current operations to complete.
   *
   * @param timeout maximum time to wait for draining
   * @return CompletableFuture that completes when pool is drained
   */
  CompletableFuture<Void> drain(Duration timeout);

  /**
   * Checks if the pool is currently draining.
   *
   * @return true if the pool is draining
   */
  boolean isDraining();

  /**
   * Checks if the pool is closed.
   *
   * @return true if the pool is closed
   */
  boolean isClosed();

  /**
   * Closes the pool and releases all resources.
   */
  @Override
  void close();
}