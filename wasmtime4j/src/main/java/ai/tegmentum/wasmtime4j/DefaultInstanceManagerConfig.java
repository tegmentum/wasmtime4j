package ai.tegmentum.wasmtime4j;

import java.time.Duration;

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
