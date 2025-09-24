package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Production-ready instance management with pooling, scaling, and health monitoring.
 *
 * <p>The instance manager provides comprehensive instance lifecycle management including:
 *
 * <ul>
 *   <li>Instance pooling and reuse for optimal performance
 *   <li>Automatic scaling based on load patterns
 *   <li>Health monitoring and automatic recovery
 *   <li>Load balancing across instance pools
 *   <li>Instance migration for load distribution
 *   <li>State checkpointing and restoration
 * </ul>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Create instance manager with configuration
 * InstanceManagerConfig config = InstanceManagerConfig.builder()
 *     .poolSize(100)
 *     .scalingEnabled(true)
 *     .healthMonitoringEnabled(true)
 *     .build();
 *
 * InstanceManager manager = InstanceManager.create(engine, config);
 *
 * // Get instance from pool
 * Instance instance = manager.getInstance(module);
 *
 * // Execute function
 * Object result = instance.getFunction("calculate").call(args);
 *
 * // Return instance to pool
 * manager.returnInstance(instance);
 *
 * // Clean shutdown
 * manager.shutdown();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface InstanceManager extends AutoCloseable {

  /**
   * Creates an instance manager with default configuration.
   *
   * @param engine the engine to use for instances
   * @return new instance manager
   * @throws IllegalArgumentException if engine is null
   */
  static InstanceManager create(final Engine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    return create(engine, InstanceManagerConfig.defaultConfig());
  }

  /**
   * Creates an instance manager with custom configuration.
   *
   * @param engine the engine to use for instances
   * @param config the manager configuration
   * @return new instance manager
   * @throws IllegalArgumentException if engine or config is null
   */
  static InstanceManager create(final Engine engine, final InstanceManagerConfig config) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    // Use runtime-specific implementation
    try {
      // First try Panama implementation
      final Class<?> panamaClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaInstanceManager");
      return (InstanceManager)
          panamaClass
              .getDeclaredMethod("create", Engine.class, InstanceManagerConfig.class)
              .invoke(null, engine, config);
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> jniClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniInstanceManager");
        return (InstanceManager)
            jniClass
                .getDeclaredMethod("create", Engine.class, InstanceManagerConfig.class)
                .invoke(null, engine, config);
      } catch (final ClassNotFoundException e2) {
        // No specific implementation found
        throw new RuntimeException(
            "No InstanceManager implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create JNI InstanceManager instance", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create Panama InstanceManager instance", e);
    }
  }

  /**
   * Gets an instance from the pool for the given module.
   *
   * <p>This method attempts to reuse an existing instance from the pool if available, otherwise
   * creates a new instance. The instance is automatically configured with the appropriate linker
   * and store.
   *
   * @param module the module to instantiate
   * @return instance from pool or newly created
   * @throws InstantiationException if instance creation fails
   * @throws IllegalArgumentException if module is null
   */
  Instance getInstance(Module module) throws InstantiationException;

  /**
   * Gets an instance asynchronously from the pool.
   *
   * <p>This method returns immediately with a future that will complete when an instance becomes
   * available. Useful for high-throughput scenarios.
   *
   * @param module the module to instantiate
   * @return future that completes with an instance
   * @throws IllegalArgumentException if module is null
   */
  CompletableFuture<Instance> getInstanceAsync(Module module);

  /**
   * Returns an instance to the pool for reuse.
   *
   * <p>The instance is reset to a clean state and made available for reuse. If the pool is full,
   * the instance may be destroyed.
   *
   * @param instance the instance to return
   * @throws IllegalArgumentException if instance is null or not managed by this manager
   */
  void returnInstance(Instance instance);

  /**
   * Gets an instance with a specific linker configuration.
   *
   * <p>This allows for custom host function binding and WASI configuration per instance request.
   *
   * @param module the module to instantiate
   * @param linker the linker to use for host functions
   * @return configured instance
   * @throws InstantiationException if instance creation fails
   * @throws IllegalArgumentException if module or linker is null
   */
  Instance getInstance(Module module, Linker linker) throws InstantiationException;

  /**
   * Creates a new instance pool for a specific module.
   *
   * <p>This pre-creates instances for the given module to improve response times. The pool size is
   * determined by the manager configuration.
   *
   * @param module the module to pre-instantiate
   * @param poolSize the number of instances to create
   * @return future that completes when pool is ready
   * @throws IllegalArgumentException if module is null or poolSize is not positive
   */
  CompletableFuture<Void> createPool(Module module, int poolSize);

  /**
   * Destroys all instances for a specific module.
   *
   * <p>This is useful when a module is no longer needed and its instances should be cleaned up to
   * free resources.
   *
   * @param module the module whose instances should be destroyed
   * @return number of instances destroyed
   * @throws IllegalArgumentException if module is null
   */
  int destroyPool(Module module);

  /**
   * Gets current instance pool statistics.
   *
   * <p>Provides detailed information about pool usage, scaling decisions, and performance metrics.
   *
   * @return current pool statistics
   */
  InstancePoolStatistics getPoolStatistics();

  /**
   * Gets health status for all managed instances.
   *
   * <p>Returns health information including response times, error rates, and resource usage for
   * monitoring and alerting.
   *
   * @return list of instance health status
   */
  List<InstanceHealthStatus> getInstanceHealth();

  /**
   * Forces health check on all instances.
   *
   * <p>Runs immediate health checks on all pooled instances and removes any that are found to be
   * unhealthy.
   *
   * @return number of unhealthy instances removed
   */
  int performHealthCheck();

  /**
   * Enables or disables automatic scaling.
   *
   * <p>When enabled, the manager automatically creates or destroys instances based on current load
   * and configured scaling policies.
   *
   * @param enabled true to enable automatic scaling
   */
  void setAutoScalingEnabled(boolean enabled);

  /**
   * Checks if automatic scaling is enabled.
   *
   * @return true if automatic scaling is enabled
   */
  boolean isAutoScalingEnabled();

  /**
   * Manually triggers scaling based on target pool size.
   *
   * <p>Adjusts the number of instances in each pool to match the target size. If target is larger
   * than current, new instances are created. If smaller, excess instances are gracefully removed.
   *
   * @param module the module to scale
   * @param targetSize the desired number of instances
   * @return future that completes when scaling is finished
   * @throws IllegalArgumentException if module is null or targetSize is negative
   */
  CompletableFuture<Void> scalePool(Module module, int targetSize);

  /**
   * Migrates instances between pools for load balancing.
   *
   * <p>Moves instances from over-utilized pools to under-utilized ones to improve overall resource
   * distribution.
   *
   * @return number of instances migrated
   */
  int balanceLoad();

  /**
   * Creates a checkpoint of an instance's state.
   *
   * <p>Captures the current state of an instance including memory contents and execution state for
   * later restoration.
   *
   * @param instance the instance to checkpoint
   * @return checkpoint data
   * @throws IllegalArgumentException if instance is null or not managed by this manager
   * @throws RuntimeException if checkpointing fails
   */
  InstanceCheckpoint createCheckpoint(Instance instance);

  /**
   * Restores an instance from a checkpoint.
   *
   * <p>Creates a new instance and restores it to the state captured in the checkpoint.
   *
   * @param checkpoint the checkpoint to restore from
   * @return restored instance
   * @throws InstantiationException if restoration fails
   * @throws IllegalArgumentException if checkpoint is null or invalid
   */
  Instance restoreFromCheckpoint(InstanceCheckpoint checkpoint) throws InstantiationException;

  /**
   * Gets the current manager configuration.
   *
   * @return manager configuration
   */
  InstanceManagerConfig getConfig();

  /**
   * Updates the manager configuration.
   *
   * <p>Configuration changes take effect immediately. Some changes may require pool recreation or
   * scaling adjustments.
   *
   * @param config the new configuration
   * @throws IllegalArgumentException if config is null
   */
  void updateConfig(InstanceManagerConfig config);

  /**
   * Gets detailed performance metrics for instance operations.
   *
   * <p>Includes timing data for instance creation, pool operations, scaling decisions, and health
   * checks.
   *
   * @return performance metrics
   */
  InstancePerformanceMetrics getPerformanceMetrics();

  /**
   * Exports manager state and statistics.
   *
   * <p>Provides comprehensive information about the manager's current state for debugging and
   * monitoring purposes.
   *
   * @param format the export format
   * @return exported data as string
   * @throws IllegalArgumentException if format is not supported
   */
  String exportState(ExportFormat format);

  /**
   * Performs maintenance operations on all pools.
   *
   * <p>Includes cleanup of inactive instances, pool compaction, and resource optimization.
   *
   * @return maintenance summary
   */
  MaintenanceSummary performMaintenance();

  /**
   * Initiates graceful shutdown of the instance manager.
   *
   * <p>Stops accepting new requests, completes pending operations, and cleanly destroys all managed
   * instances.
   *
   * @param timeout maximum time to wait for shutdown
   * @return future that completes when shutdown is finished
   */
  CompletableFuture<Void> shutdown(Duration timeout);

  /**
   * Closes the instance manager and releases all resources.
   *
   * <p>This performs an immediate shutdown without waiting for pending operations to complete. Use
   * {@link #shutdown(Duration)} for graceful shutdown.
   */
  @Override
  void close();

  // Nested interfaces and classes for type safety

  /** Configuration for the instance manager. */
  interface InstanceManagerConfig {
    /**
     * Gets the default pool size for new modules.
     *
     * @return default pool size
     */
    int getDefaultPoolSize();

    /**
     * Gets the maximum pool size per module.
     *
     * @return maximum pool size
     */
    int getMaxPoolSize();

    /**
     * Checks if automatic scaling is enabled.
     *
     * @return true if auto-scaling is enabled
     */
    boolean isAutoScalingEnabled();

    /**
     * Gets the scaling threshold percentage.
     *
     * @return scaling threshold (0.0 to 1.0)
     */
    double getScalingThreshold();

    /**
     * Checks if health monitoring is enabled.
     *
     * @return true if health monitoring is enabled
     */
    boolean isHealthMonitoringEnabled();

    /**
     * Gets the health check interval.
     *
     * @return health check interval
     */
    Duration getHealthCheckInterval();

    /**
     * Checks if instance migration is enabled.
     *
     * @return true if migration is enabled
     */
    boolean isMigrationEnabled();

    /**
     * Checks if checkpointing is enabled.
     *
     * @return true if checkpointing is enabled
     */
    boolean isCheckpointingEnabled();

    /**
     * Gets the instance timeout duration.
     *
     * @return instance timeout
     */
    Duration getInstanceTimeout();

    /**
     * Creates a builder for instance manager configuration.
     *
     * @return configuration builder
     */
    static Builder builder() {
      return new DefaultInstanceManagerConfigBuilder();
    }

    /**
     * Gets the default configuration.
     *
     * @return default configuration
     */
    static InstanceManagerConfig defaultConfig() {
      return builder().build();
    }

    /** Builder for instance manager configuration. */
    interface Builder {
      Builder defaultPoolSize(int size);

      Builder maxPoolSize(int size);

      Builder autoScalingEnabled(boolean enabled);

      Builder scalingThreshold(double threshold);

      Builder healthMonitoringEnabled(boolean enabled);

      Builder healthCheckInterval(Duration interval);

      Builder migrationEnabled(boolean enabled);

      Builder checkpointingEnabled(boolean enabled);

      Builder instanceTimeout(Duration timeout);

      InstanceManagerConfig build();
    }
  }

  /** Statistics for instance pools. */
  interface InstancePoolStatistics {
    /**
     * Gets the total number of pools.
     *
     * @return total pools
     */
    int getTotalPools();

    /**
     * Gets the total number of instances across all pools.
     *
     * @return total instances
     */
    int getTotalInstances();

    /**
     * Gets the number of active instances.
     *
     * @return active instances
     */
    int getActiveInstances();

    /**
     * Gets the number of idle instances.
     *
     * @return idle instances
     */
    int getIdleInstances();

    /**
     * Gets the total number of instance retrievals from pools.
     *
     * @return total retrievals
     */
    long getTotalRetrievals();

    /**
     * Gets the number of pool hits (reused instances).
     *
     * @return pool hits
     */
    long getPoolHits();

    /**
     * Gets the number of pool misses (new instances created).
     *
     * @return pool misses
     */
    long getPoolMisses();

    /**
     * Gets the pool hit ratio.
     *
     * @return hit ratio (0.0 to 1.0)
     */
    double getHitRatio();

    /**
     * Gets the average instance creation time.
     *
     * @return average creation time
     */
    Duration getAverageCreationTime();

    /**
     * Gets the number of scaling operations performed.
     *
     * @return scaling operations
     */
    long getScalingOperations();

    /**
     * Gets the number of health check failures.
     *
     * @return health check failures
     */
    long getHealthCheckFailures();

    /**
     * Gets per-module pool statistics.
     *
     * @return map of module to statistics
     */
    Map<Module, ModulePoolStatistics> getPerModuleStatistics();
  }

  /** Health status for a managed instance. */
  interface InstanceHealthStatus {
    /**
     * Gets the instance ID.
     *
     * @return instance ID
     */
    String getInstanceId();

    /**
     * Checks if the instance is healthy.
     *
     * @return true if healthy
     */
    boolean isHealthy();

    /**
     * Gets the last health check timestamp.
     *
     * @return last check time
     */
    java.time.Instant getLastHealthCheck();

    /**
     * Gets the average response time.
     *
     * @return average response time
     */
    Duration getAverageResponseTime();

    /**
     * Gets the error rate percentage.
     *
     * @return error rate (0.0 to 100.0)
     */
    double getErrorRate();

    /**
     * Gets the current memory usage.
     *
     * @return memory usage in bytes
     */
    long getMemoryUsage();

    /**
     * Gets any health issues.
     *
     * @return list of health issues
     */
    List<String> getHealthIssues();
  }

  /** Performance metrics for instance operations. */
  interface InstancePerformanceMetrics {
    /**
     * Gets the total number of instances created.
     *
     * @return instances created
     */
    long getInstancesCreated();

    /**
     * Gets the total number of instances destroyed.
     *
     * @return instances destroyed
     */
    long getInstancesDestroyed();

    /**
     * Gets the average instance creation time.
     *
     * @return average creation time
     */
    Duration getAverageCreationTime();

    /**
     * Gets the average instance destruction time.
     *
     * @return average destruction time
     */
    Duration getAverageDestructionTime();

    /**
     * Gets the total scaling time.
     *
     * @return total scaling time
     */
    Duration getTotalScalingTime();

    /**
     * Gets the number of successful migrations.
     *
     * @return successful migrations
     */
    long getSuccessfulMigrations();

    /**
     * Gets the number of failed migrations.
     *
     * @return failed migrations
     */
    long getFailedMigrations();

    /**
     * Gets the total checkpoint creation time.
     *
     * @return total checkpoint time
     */
    Duration getTotalCheckpointTime();

    /**
     * Gets the manager overhead percentage.
     *
     * @return overhead percentage (0.0 to 100.0)
     */
    double getManagerOverhead();
  }

  /** Statistics for a specific module's pool. */
  interface ModulePoolStatistics {
    /**
     * Gets the module.
     *
     * @return module
     */
    Module getModule();

    /**
     * Gets the current pool size.
     *
     * @return current pool size
     */
    int getCurrentPoolSize();

    /**
     * Gets the target pool size.
     *
     * @return target pool size
     */
    int getTargetPoolSize();

    /**
     * Gets the number of active instances.
     *
     * @return active instances
     */
    int getActiveInstances();

    /**
     * Gets the number of retrievals for this module.
     *
     * @return retrievals
     */
    long getRetrievals();

    /**
     * Gets the last scaling timestamp.
     *
     * @return last scaling time
     */
    java.time.Instant getLastScaling();
  }

  /** Instance checkpoint data. */
  interface InstanceCheckpoint {
    /**
     * Gets the checkpoint ID.
     *
     * @return checkpoint ID
     */
    String getCheckpointId();

    /**
     * Gets the module this checkpoint was created from.
     *
     * @return source module
     */
    Module getModule();

    /**
     * Gets the checkpoint creation timestamp.
     *
     * @return creation timestamp
     */
    java.time.Instant getCreatedAt();

    /**
     * Gets the checkpoint data size.
     *
     * @return data size in bytes
     */
    long getDataSize();

    /**
     * Checks if this checkpoint is valid.
     *
     * @return true if valid
     */
    boolean isValid();
  }

  /** Maintenance operation summary. */
  interface MaintenanceSummary {
    /**
     * Gets the number of instances cleaned up.
     *
     * @return instances cleaned
     */
    int getInstancesCleaned();

    /**
     * Gets the amount of memory freed.
     *
     * @return memory freed in bytes
     */
    long getMemoryFreed();

    /**
     * Gets the number of pools compacted.
     *
     * @return pools compacted
     */
    int getPoolsCompacted();

    /**
     * Gets the maintenance duration.
     *
     * @return maintenance duration
     */
    Duration getMaintenanceDuration();

    /**
     * Gets any issues encountered during maintenance.
     *
     * @return list of issues
     */
    List<String> getIssues();
  }

  /** Export formats for manager state. */
  enum ExportFormat {
    JSON,
    XML,
    CSV,
    YAML
  }
}

/** Default implementation of InstanceManagerConfig.Builder. */
final class DefaultInstanceManagerConfigBuilder
    implements InstanceManager.InstanceManagerConfig.Builder {
  private int defaultPoolSize = 10;
  private int maxPoolSize = 100;
  private boolean autoScalingEnabled = true;
  private double scalingThreshold = 0.8;
  private boolean healthMonitoringEnabled = true;
  private Duration healthCheckInterval = Duration.ofMinutes(1);
  private boolean migrationEnabled = true;
  private boolean checkpointingEnabled = false;
  private Duration instanceTimeout = Duration.ofMinutes(5);

  @Override
  public InstanceManager.InstanceManagerConfig.Builder defaultPoolSize(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Default pool size must be positive");
    }
    this.defaultPoolSize = size;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig.Builder maxPoolSize(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Max pool size must be positive");
    }
    this.maxPoolSize = size;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig.Builder autoScalingEnabled(final boolean enabled) {
    this.autoScalingEnabled = enabled;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig.Builder scalingThreshold(final double threshold) {
    if (threshold < 0.0 || threshold > 1.0) {
      throw new IllegalArgumentException("Scaling threshold must be between 0.0 and 1.0");
    }
    this.scalingThreshold = threshold;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig.Builder healthMonitoringEnabled(
      final boolean enabled) {
    this.healthMonitoringEnabled = enabled;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig.Builder healthCheckInterval(
      final Duration interval) {
    if (interval == null || interval.isNegative()) {
      throw new IllegalArgumentException("Health check interval must be positive");
    }
    this.healthCheckInterval = interval;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig.Builder migrationEnabled(final boolean enabled) {
    this.migrationEnabled = enabled;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig.Builder checkpointingEnabled(final boolean enabled) {
    this.checkpointingEnabled = enabled;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig.Builder instanceTimeout(final Duration timeout) {
    if (timeout == null || timeout.isNegative()) {
      throw new IllegalArgumentException("Instance timeout must be positive");
    }
    this.instanceTimeout = timeout;
    return this;
  }

  @Override
  public InstanceManager.InstanceManagerConfig build() {
    if (defaultPoolSize > maxPoolSize) {
      throw new IllegalArgumentException("Default pool size cannot exceed max pool size");
    }

    return new DefaultInstanceManagerConfig(
        defaultPoolSize,
        maxPoolSize,
        autoScalingEnabled,
        scalingThreshold,
        healthMonitoringEnabled,
        healthCheckInterval,
        migrationEnabled,
        checkpointingEnabled,
        instanceTimeout);
  }
}

/** Default implementation of InstanceManagerConfig. */
final class DefaultInstanceManagerConfig implements InstanceManager.InstanceManagerConfig {
  private final int defaultPoolSize;
  private final int maxPoolSize;
  private final boolean autoScalingEnabled;
  private final double scalingThreshold;
  private final boolean healthMonitoringEnabled;
  private final Duration healthCheckInterval;
  private final boolean migrationEnabled;
  private final boolean checkpointingEnabled;
  private final Duration instanceTimeout;

  DefaultInstanceManagerConfig(
      final int defaultPoolSize,
      final int maxPoolSize,
      final boolean autoScalingEnabled,
      final double scalingThreshold,
      final boolean healthMonitoringEnabled,
      final Duration healthCheckInterval,
      final boolean migrationEnabled,
      final boolean checkpointingEnabled,
      final Duration instanceTimeout) {
    this.defaultPoolSize = defaultPoolSize;
    this.maxPoolSize = maxPoolSize;
    this.autoScalingEnabled = autoScalingEnabled;
    this.scalingThreshold = scalingThreshold;
    this.healthMonitoringEnabled = healthMonitoringEnabled;
    this.healthCheckInterval = healthCheckInterval;
    this.migrationEnabled = migrationEnabled;
    this.checkpointingEnabled = checkpointingEnabled;
    this.instanceTimeout = instanceTimeout;
  }

  @Override
  public int getDefaultPoolSize() {
    return defaultPoolSize;
  }

  @Override
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  @Override
  public boolean isAutoScalingEnabled() {
    return autoScalingEnabled;
  }

  @Override
  public double getScalingThreshold() {
    return scalingThreshold;
  }

  @Override
  public boolean isHealthMonitoringEnabled() {
    return healthMonitoringEnabled;
  }

  @Override
  public Duration getHealthCheckInterval() {
    return healthCheckInterval;
  }

  @Override
  public boolean isMigrationEnabled() {
    return migrationEnabled;
  }

  @Override
  public boolean isCheckpointingEnabled() {
    return checkpointingEnabled;
  }

  @Override
  public Duration getInstanceTimeout() {
    return instanceTimeout;
  }
}
