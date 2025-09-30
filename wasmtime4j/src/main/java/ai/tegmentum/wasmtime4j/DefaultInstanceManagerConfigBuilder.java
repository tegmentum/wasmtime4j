package ai.tegmentum.wasmtime4j;

import java.time.Duration;

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
