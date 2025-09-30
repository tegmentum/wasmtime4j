package ai.tegmentum.wasmtime4j.parallel;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Configuration for parallel WebAssembly execution operations.
 *
 * <p>This class provides settings for controlling parallel execution behavior,
 * thread management, load balancing, and fault tolerance.
 *
 * @since 1.0.0
 */
public final class ParallelExecutionConfig {

  private int maxParallelInstances = Runtime.getRuntime().availableProcessors() * 2;
  private int maxParallelCalls = Runtime.getRuntime().availableProcessors() * 4;
  private Duration executionTimeout = Duration.ofMinutes(10);
  private Executor defaultExecutor = ForkJoinPool.commonPool();
  private LoadBalancingStrategy loadBalancingStrategy = LoadBalancingStrategy.ROUND_ROBIN;
  private boolean enableFaultTolerance = true;
  private int maxRetryAttempts = 3;
  private Duration retryDelay = Duration.ofSeconds(1);
  private boolean enableAdaptiveScaling = true;
  private double scaleUpThreshold = 0.8;
  private double scaleDownThreshold = 0.3;
  private Duration scalingCooldown = Duration.ofMinutes(1);
  private int minInstancePoolSize = 2;
  private int maxInstancePoolSize = 50;
  private boolean enableResourceMonitoring = true;
  private long maxMemoryPerInstance = 128L * 1024 * 1024; // 128 MB
  private long maxTotalMemory = 2L * 1024 * 1024 * 1024; // 2 GB
  private boolean enableInstanceReuse = true;
  private Duration instanceIdleTimeout = Duration.ofMinutes(5);
  private boolean enableProgressTracking = true;
  private Duration progressUpdateInterval = Duration.ofSeconds(1);

  /** Creates a new parallel execution configuration with default settings. */
  public ParallelExecutionConfig() {
    // Default configuration
  }

  /**
   * Sets the maximum number of parallel instances allowed.
   *
   * @param maxInstances maximum parallel instances
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if maxInstances is less than 1
   */
  public ParallelExecutionConfig setMaxParallelInstances(final int maxInstances) {
    if (maxInstances < 1) {
      throw new IllegalArgumentException("Maximum parallel instances must be at least 1");
    }
    this.maxParallelInstances = maxInstances;
    return this;
  }

  /**
   * Sets the maximum number of parallel function calls allowed.
   *
   * @param maxCalls maximum parallel calls
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if maxCalls is less than 1
   */
  public ParallelExecutionConfig setMaxParallelCalls(final int maxCalls) {
    if (maxCalls < 1) {
      throw new IllegalArgumentException("Maximum parallel calls must be at least 1");
    }
    this.maxParallelCalls = maxCalls;
    return this;
  }

  /**
   * Sets the execution timeout for parallel operations.
   *
   * @param timeout execution timeout
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  public ParallelExecutionConfig setExecutionTimeout(final Duration timeout) {
    if (timeout == null || timeout.isNegative()) {
      throw new IllegalArgumentException("Execution timeout must be positive");
    }
    this.executionTimeout = timeout;
    return this;
  }

  /**
   * Sets the default executor for parallel operations.
   *
   * @param executor the default executor
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if executor is null
   */
  public ParallelExecutionConfig setDefaultExecutor(final Executor executor) {
    if (executor == null) {
      throw new IllegalArgumentException("Default executor cannot be null");
    }
    this.defaultExecutor = executor;
    return this;
  }

  /**
   * Sets the load balancing strategy.
   *
   * @param strategy load balancing strategy
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if strategy is null
   */
  public ParallelExecutionConfig setLoadBalancingStrategy(final LoadBalancingStrategy strategy) {
    if (strategy == null) {
      throw new IllegalArgumentException("Load balancing strategy cannot be null");
    }
    this.loadBalancingStrategy = strategy;
    return this;
  }

  /**
   * Enables or disables fault tolerance.
   *
   * @param enabled true to enable fault tolerance
   * @return this configuration for method chaining
   */
  public ParallelExecutionConfig setFaultTolerance(final boolean enabled) {
    this.enableFaultTolerance = enabled;
    return this;
  }

  /**
   * Sets the maximum number of retry attempts for failed operations.
   *
   * @param maxRetries maximum retry attempts
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if maxRetries is negative
   */
  public ParallelExecutionConfig setMaxRetryAttempts(final int maxRetries) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("Maximum retry attempts cannot be negative");
    }
    this.maxRetryAttempts = maxRetries;
    return this;
  }

  /**
   * Sets the delay between retry attempts.
   *
   * @param delay retry delay
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if delay is null or negative
   */
  public ParallelExecutionConfig setRetryDelay(final Duration delay) {
    if (delay == null || delay.isNegative()) {
      throw new IllegalArgumentException("Retry delay must be positive");
    }
    this.retryDelay = delay;
    return this;
  }

  /**
   * Enables or disables adaptive scaling based on load.
   *
   * @param enabled true to enable adaptive scaling
   * @return this configuration for method chaining
   */
  public ParallelExecutionConfig setAdaptiveScaling(final boolean enabled) {
    this.enableAdaptiveScaling = enabled;
    return this;
  }

  /**
   * Sets the scale-up threshold for adaptive scaling.
   *
   * @param threshold scale-up threshold (0.0 - 1.0)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if threshold is not between 0 and 1
   */
  public ParallelExecutionConfig setScaleUpThreshold(final double threshold) {
    if (threshold < 0.0 || threshold > 1.0) {
      throw new IllegalArgumentException("Scale-up threshold must be between 0.0 and 1.0");
    }
    this.scaleUpThreshold = threshold;
    return this;
  }

  /**
   * Sets the scale-down threshold for adaptive scaling.
   *
   * @param threshold scale-down threshold (0.0 - 1.0)
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if threshold is not between 0 and 1
   */
  public ParallelExecutionConfig setScaleDownThreshold(final double threshold) {
    if (threshold < 0.0 || threshold > 1.0) {
      throw new IllegalArgumentException("Scale-down threshold must be between 0.0 and 1.0");
    }
    this.scaleDownThreshold = threshold;
    return this;
  }

  /**
   * Sets the cooldown period for scaling operations.
   *
   * @param cooldown scaling cooldown period
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if cooldown is null or negative
   */
  public ParallelExecutionConfig setScalingCooldown(final Duration cooldown) {
    if (cooldown == null || cooldown.isNegative()) {
      throw new IllegalArgumentException("Scaling cooldown must be positive");
    }
    this.scalingCooldown = cooldown;
    return this;
  }

  /**
   * Sets the minimum instance pool size.
   *
   * @param minSize minimum pool size
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if minSize is less than 1
   */
  public ParallelExecutionConfig setMinInstancePoolSize(final int minSize) {
    if (minSize < 1) {
      throw new IllegalArgumentException("Minimum instance pool size must be at least 1");
    }
    this.minInstancePoolSize = minSize;
    return this;
  }

  /**
   * Sets the maximum instance pool size.
   *
   * @param maxSize maximum pool size
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if maxSize is less than minimum
   */
  public ParallelExecutionConfig setMaxInstancePoolSize(final int maxSize) {
    if (maxSize < minInstancePoolSize) {
      throw new IllegalArgumentException("Maximum pool size must be at least minimum pool size");
    }
    this.maxInstancePoolSize = maxSize;
    return this;
  }

  /**
   * Enables or disables resource monitoring.
   *
   * @param enabled true to enable resource monitoring
   * @return this configuration for method chaining
   */
  public ParallelExecutionConfig setResourceMonitoring(final boolean enabled) {
    this.enableResourceMonitoring = enabled;
    return this;
  }

  /**
   * Sets the maximum memory per instance.
   *
   * @param memoryBytes maximum memory per instance in bytes
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if memoryBytes is negative
   */
  public ParallelExecutionConfig setMaxMemoryPerInstance(final long memoryBytes) {
    if (memoryBytes < 0) {
      throw new IllegalArgumentException("Maximum memory per instance cannot be negative");
    }
    this.maxMemoryPerInstance = memoryBytes;
    return this;
  }

  /**
   * Sets the maximum total memory for all instances.
   *
   * @param memoryBytes maximum total memory in bytes
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if memoryBytes is negative
   */
  public ParallelExecutionConfig setMaxTotalMemory(final long memoryBytes) {
    if (memoryBytes < 0) {
      throw new IllegalArgumentException("Maximum total memory cannot be negative");
    }
    this.maxTotalMemory = memoryBytes;
    return this;
  }

  /**
   * Enables or disables instance reuse.
   *
   * @param enabled true to enable instance reuse
   * @return this configuration for method chaining
   */
  public ParallelExecutionConfig setInstanceReuse(final boolean enabled) {
    this.enableInstanceReuse = enabled;
    return this;
  }

  /**
   * Sets the idle timeout for instance reuse.
   *
   * @param timeout idle timeout
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  public ParallelExecutionConfig setInstanceIdleTimeout(final Duration timeout) {
    if (timeout == null || timeout.isNegative()) {
      throw new IllegalArgumentException("Instance idle timeout must be positive");
    }
    this.instanceIdleTimeout = timeout;
    return this;
  }

  /**
   * Enables or disables progress tracking.
   *
   * @param enabled true to enable progress tracking
   * @return this configuration for method chaining
   */
  public ParallelExecutionConfig setProgressTracking(final boolean enabled) {
    this.enableProgressTracking = enabled;
    return this;
  }

  /**
   * Sets the progress update interval.
   *
   * @param interval progress update interval
   * @return this configuration for method chaining
   * @throws IllegalArgumentException if interval is null or negative
   */
  public ParallelExecutionConfig setProgressUpdateInterval(final Duration interval) {
    if (interval == null || interval.isNegative()) {
      throw new IllegalArgumentException("Progress update interval must be positive");
    }
    this.progressUpdateInterval = interval;
    return this;
  }

  // Getters

  public int getMaxParallelInstances() {
    return maxParallelInstances;
  }

  public int getMaxParallelCalls() {
    return maxParallelCalls;
  }

  public Duration getExecutionTimeout() {
    return executionTimeout;
  }

  public Executor getDefaultExecutor() {
    return defaultExecutor;
  }

  public LoadBalancingStrategy getLoadBalancingStrategy() {
    return loadBalancingStrategy;
  }

  public boolean isFaultToleranceEnabled() {
    return enableFaultTolerance;
  }

  public int getMaxRetryAttempts() {
    return maxRetryAttempts;
  }

  public Duration getRetryDelay() {
    return retryDelay;
  }

  public boolean isAdaptiveScalingEnabled() {
    return enableAdaptiveScaling;
  }

  public double getScaleUpThreshold() {
    return scaleUpThreshold;
  }

  public double getScaleDownThreshold() {
    return scaleDownThreshold;
  }

  public Duration getScalingCooldown() {
    return scalingCooldown;
  }

  public int getMinInstancePoolSize() {
    return minInstancePoolSize;
  }

  public int getMaxInstancePoolSize() {
    return maxInstancePoolSize;
  }

  public boolean isResourceMonitoringEnabled() {
    return enableResourceMonitoring;
  }

  public long getMaxMemoryPerInstance() {
    return maxMemoryPerInstance;
  }

  public long getMaxTotalMemory() {
    return maxTotalMemory;
  }

  public boolean isInstanceReuseEnabled() {
    return enableInstanceReuse;
  }

  public Duration getInstanceIdleTimeout() {
    return instanceIdleTimeout;
  }

  public boolean isProgressTrackingEnabled() {
    return enableProgressTracking;
  }

  public Duration getProgressUpdateInterval() {
    return progressUpdateInterval;
  }

  /**
   * Creates a default parallel execution configuration.
   *
   * @return default configuration
   */
  public static ParallelExecutionConfig defaultConfig() {
    return new ParallelExecutionConfig();
  }

  /**
   * Creates a high-throughput parallel execution configuration.
   *
   * @return high-throughput configuration
   */
  public static ParallelExecutionConfig highThroughput() {
    return new ParallelExecutionConfig()
        .setMaxParallelInstances(Runtime.getRuntime().availableProcessors() * 4)
        .setMaxParallelCalls(Runtime.getRuntime().availableProcessors() * 8)
        .setLoadBalancingStrategy(LoadBalancingStrategy.LEAST_CONNECTIONS)
        .setAdaptiveScaling(true)
        .setInstanceReuse(true)
        .setMaxInstancePoolSize(100);
  }

  /**
   * Creates a low-resource parallel execution configuration.
   *
   * @return low-resource configuration
   */
  public static ParallelExecutionConfig lowResource() {
    return new ParallelExecutionConfig()
        .setMaxParallelInstances(2)
        .setMaxParallelCalls(4)
        .setAdaptiveScaling(false)
        .setResourceMonitoring(false)
        .setProgressTracking(false)
        .setMaxMemoryPerInstance(32L * 1024 * 1024) // 32 MB
        .setMaxInstancePoolSize(5);
  }

  @Override
  public String toString() {
    return String.format(
        "ParallelExecutionConfig{instances=%d, calls=%d, strategy=%s, faultTolerance=%s}",
        maxParallelInstances, maxParallelCalls, loadBalancingStrategy, enableFaultTolerance);
  }
}