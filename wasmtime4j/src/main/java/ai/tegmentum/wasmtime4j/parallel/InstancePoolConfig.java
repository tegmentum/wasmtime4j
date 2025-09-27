package ai.tegmentum.wasmtime4j.parallel;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for an instance pool.
 *
 * <p>Defines the parameters and limits for managing a pool of WebAssembly instances,
 * including size limits, timeouts, and scaling behavior.
 *
 * @since 1.0.0
 */
public final class InstancePoolConfig {

  private final int minPoolSize;
  private final int maxPoolSize;
  private final int initialPoolSize;
  private final Duration maxIdleTime;
  private final Duration instanceCreationTimeout;
  private final Duration healthCheckInterval;
  private final boolean enableAutoScaling;
  private final double scaleUpThreshold;
  private final double scaleDownThreshold;
  private final int maxConcurrentCreations;
  private final boolean preWarmInstances;

  private InstancePoolConfig(final Builder builder) {
    this.minPoolSize = builder.minPoolSize;
    this.maxPoolSize = builder.maxPoolSize;
    this.initialPoolSize = builder.initialPoolSize;
    this.maxIdleTime = Objects.requireNonNull(builder.maxIdleTime);
    this.instanceCreationTimeout = Objects.requireNonNull(builder.instanceCreationTimeout);
    this.healthCheckInterval = Objects.requireNonNull(builder.healthCheckInterval);
    this.enableAutoScaling = builder.enableAutoScaling;
    this.scaleUpThreshold = builder.scaleUpThreshold;
    this.scaleDownThreshold = builder.scaleDownThreshold;
    this.maxConcurrentCreations = builder.maxConcurrentCreations;
    this.preWarmInstances = builder.preWarmInstances;

    if (minPoolSize < 0) {
      throw new IllegalArgumentException("Minimum pool size cannot be negative");
    }
    if (maxPoolSize < minPoolSize) {
      throw new IllegalArgumentException("Maximum pool size must be >= minimum pool size");
    }
    if (initialPoolSize < minPoolSize || initialPoolSize > maxPoolSize) {
      throw new IllegalArgumentException("Initial pool size must be between min and max pool size");
    }
    if (scaleUpThreshold <= 0 || scaleUpThreshold > 1) {
      throw new IllegalArgumentException("Scale up threshold must be between 0 and 1");
    }
    if (scaleDownThreshold <= 0 || scaleDownThreshold >= scaleUpThreshold) {
      throw new IllegalArgumentException("Scale down threshold must be between 0 and scale up threshold");
    }
  }

  /**
   * Creates a new builder for instance pool configuration.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default configuration.
   *
   * @return default configuration
   */
  public static InstancePoolConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Gets the minimum pool size.
   *
   * @return minimum pool size
   */
  public int getMinPoolSize() {
    return minPoolSize;
  }

  /**
   * Gets the maximum pool size.
   *
   * @return maximum pool size
   */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  /**
   * Gets the initial pool size.
   *
   * @return initial pool size
   */
  public int getInitialPoolSize() {
    return initialPoolSize;
  }

  /**
   * Gets the maximum idle time for instances.
   *
   * @return maximum idle time
   */
  public Duration getMaxIdleTime() {
    return maxIdleTime;
  }

  /**
   * Gets the instance creation timeout.
   *
   * @return creation timeout
   */
  public Duration getInstanceCreationTimeout() {
    return instanceCreationTimeout;
  }

  /**
   * Gets the health check interval.
   *
   * @return health check interval
   */
  public Duration getHealthCheckInterval() {
    return healthCheckInterval;
  }

  /**
   * Checks if auto-scaling is enabled.
   *
   * @return true if auto-scaling is enabled
   */
  public boolean isAutoScalingEnabled() {
    return enableAutoScaling;
  }

  /**
   * Gets the scale-up threshold.
   *
   * @return scale-up threshold (0-1)
   */
  public double getScaleUpThreshold() {
    return scaleUpThreshold;
  }

  /**
   * Gets the scale-down threshold.
   *
   * @return scale-down threshold (0-1)
   */
  public double getScaleDownThreshold() {
    return scaleDownThreshold;
  }

  /**
   * Gets the maximum number of concurrent instance creations.
   *
   * @return maximum concurrent creations
   */
  public int getMaxConcurrentCreations() {
    return maxConcurrentCreations;
  }

  /**
   * Checks if instance pre-warming is enabled.
   *
   * @return true if pre-warming is enabled
   */
  public boolean isPreWarmInstances() {
    return preWarmInstances;
  }

  /**
   * Builder for instance pool configuration.
   */
  public static final class Builder {
    private int minPoolSize = 1;
    private int maxPoolSize = 10;
    private int initialPoolSize = 2;
    private Duration maxIdleTime = Duration.ofMinutes(10);
    private Duration instanceCreationTimeout = Duration.ofSeconds(30);
    private Duration healthCheckInterval = Duration.ofMinutes(1);
    private boolean enableAutoScaling = true;
    private double scaleUpThreshold = 0.8;
    private double scaleDownThreshold = 0.3;
    private int maxConcurrentCreations = 3;
    private boolean preWarmInstances = true;

    public Builder minPoolSize(final int minPoolSize) {
      this.minPoolSize = minPoolSize;
      return this;
    }

    public Builder maxPoolSize(final int maxPoolSize) {
      this.maxPoolSize = maxPoolSize;
      return this;
    }

    public Builder initialPoolSize(final int initialPoolSize) {
      this.initialPoolSize = initialPoolSize;
      return this;
    }

    public Builder maxIdleTime(final Duration maxIdleTime) {
      this.maxIdleTime = Objects.requireNonNull(maxIdleTime);
      return this;
    }

    public Builder instanceCreationTimeout(final Duration instanceCreationTimeout) {
      this.instanceCreationTimeout = Objects.requireNonNull(instanceCreationTimeout);
      return this;
    }

    public Builder healthCheckInterval(final Duration healthCheckInterval) {
      this.healthCheckInterval = Objects.requireNonNull(healthCheckInterval);
      return this;
    }

    public Builder enableAutoScaling(final boolean enableAutoScaling) {
      this.enableAutoScaling = enableAutoScaling;
      return this;
    }

    public Builder scaleUpThreshold(final double scaleUpThreshold) {
      this.scaleUpThreshold = scaleUpThreshold;
      return this;
    }

    public Builder scaleDownThreshold(final double scaleDownThreshold) {
      this.scaleDownThreshold = scaleDownThreshold;
      return this;
    }

    public Builder maxConcurrentCreations(final int maxConcurrentCreations) {
      this.maxConcurrentCreations = maxConcurrentCreations;
      return this;
    }

    public Builder preWarmInstances(final boolean preWarmInstances) {
      this.preWarmInstances = preWarmInstances;
      return this;
    }

    public InstancePoolConfig build() {
      return new InstancePoolConfig(this);
    }
  }

  @Override
  public String toString() {
    return String.format("InstancePoolConfig{min=%d, max=%d, initial=%d, autoScale=%s}",
        minPoolSize, maxPoolSize, initialPoolSize, enableAutoScaling);
  }
}