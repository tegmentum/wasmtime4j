package ai.tegmentum.wasmtime4j.parallel;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a health check performed on an instance pool.
 *
 * <p>Contains information about the overall health of the pool and individual
 * instance health statuses.
 *
 * @since 1.0.0
 */
public final class PoolHealthCheckResult {

  private final boolean overallHealthy;
  private final int totalInstances;
  private final int healthyInstances;
  private final int degradedInstances;
  private final int unhealthyInstances;
  private final Map<String, InstanceHealthStatus> instanceStatuses;
  private final List<String> poolIssues;
  private final List<String> poolWarnings;
  private final Duration checkDuration;
  private final Instant checkTimestamp;

  private PoolHealthCheckResult(final boolean overallHealthy,
                                final int totalInstances,
                                final int healthyInstances,
                                final int degradedInstances,
                                final int unhealthyInstances,
                                final Map<String, InstanceHealthStatus> instanceStatuses,
                                final List<String> poolIssues,
                                final List<String> poolWarnings,
                                final Duration checkDuration,
                                final Instant checkTimestamp) {
    this.overallHealthy = overallHealthy;
    this.totalInstances = totalInstances;
    this.healthyInstances = healthyInstances;
    this.degradedInstances = degradedInstances;
    this.unhealthyInstances = unhealthyInstances;
    this.instanceStatuses = Map.copyOf(instanceStatuses);
    this.poolIssues = List.copyOf(poolIssues);
    this.poolWarnings = List.copyOf(poolWarnings);
    this.checkDuration = Objects.requireNonNull(checkDuration);
    this.checkTimestamp = Objects.requireNonNull(checkTimestamp);
  }

  /**
   * Creates a new builder for pool health check results.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if the pool is overall healthy.
   *
   * @return true if pool is healthy
   */
  public boolean isOverallHealthy() {
    return overallHealthy;
  }

  /**
   * Gets the total number of instances checked.
   *
   * @return total instances
   */
  public int getTotalInstances() {
    return totalInstances;
  }

  /**
   * Gets the number of healthy instances.
   *
   * @return healthy instances count
   */
  public int getHealthyInstances() {
    return healthyInstances;
  }

  /**
   * Gets the number of degraded instances.
   *
   * @return degraded instances count
   */
  public int getDegradedInstances() {
    return degradedInstances;
  }

  /**
   * Gets the number of unhealthy instances.
   *
   * @return unhealthy instances count
   */
  public int getUnhealthyInstances() {
    return unhealthyInstances;
  }

  /**
   * Gets the health status for each instance.
   *
   * @return map of instance ID to health status
   */
  public Map<String, InstanceHealthStatus> getInstanceStatuses() {
    return instanceStatuses;
  }

  /**
   * Gets pool-level issues.
   *
   * @return list of pool issues
   */
  public List<String> getPoolIssues() {
    return poolIssues;
  }

  /**
   * Gets pool-level warnings.
   *
   * @return list of pool warnings
   */
  public List<String> getPoolWarnings() {
    return poolWarnings;
  }

  /**
   * Gets the duration of the health check.
   *
   * @return check duration
   */
  public Duration getCheckDuration() {
    return checkDuration;
  }

  /**
   * Gets when the health check was performed.
   *
   * @return check timestamp
   */
  public Instant getCheckTimestamp() {
    return checkTimestamp;
  }

  /**
   * Gets the health percentage of the pool.
   *
   * @return health percentage (0-100)
   */
  public double getHealthPercentage() {
    if (totalInstances == 0) return 100.0;
    return (double) healthyInstances / totalInstances * 100.0;
  }

  /**
   * Checks if there are any pool-level issues.
   *
   * @return true if pool issues exist
   */
  public boolean hasPoolIssues() {
    return !poolIssues.isEmpty();
  }

  /**
   * Checks if there are any pool-level warnings.
   *
   * @return true if pool warnings exist
   */
  public boolean hasPoolWarnings() {
    return !poolWarnings.isEmpty();
  }

  /**
   * Builder for pool health check results.
   */
  public static final class Builder {
    private boolean overallHealthy = true;
    private int totalInstances = 0;
    private int healthyInstances = 0;
    private int degradedInstances = 0;
    private int unhealthyInstances = 0;
    private Map<String, InstanceHealthStatus> instanceStatuses = Map.of();
    private List<String> poolIssues = List.of();
    private List<String> poolWarnings = List.of();
    private Duration checkDuration = Duration.ZERO;
    private Instant checkTimestamp = Instant.now();

    public Builder overallHealthy(final boolean overallHealthy) {
      this.overallHealthy = overallHealthy;
      return this;
    }

    public Builder totalInstances(final int totalInstances) {
      this.totalInstances = totalInstances;
      return this;
    }

    public Builder healthyInstances(final int healthyInstances) {
      this.healthyInstances = healthyInstances;
      return this;
    }

    public Builder degradedInstances(final int degradedInstances) {
      this.degradedInstances = degradedInstances;
      return this;
    }

    public Builder unhealthyInstances(final int unhealthyInstances) {
      this.unhealthyInstances = unhealthyInstances;
      return this;
    }

    public Builder instanceStatuses(final Map<String, InstanceHealthStatus> instanceStatuses) {
      this.instanceStatuses = Objects.requireNonNull(instanceStatuses);
      return this;
    }

    public Builder poolIssues(final List<String> poolIssues) {
      this.poolIssues = Objects.requireNonNull(poolIssues);
      return this;
    }

    public Builder poolWarnings(final List<String> poolWarnings) {
      this.poolWarnings = Objects.requireNonNull(poolWarnings);
      return this;
    }

    public Builder checkDuration(final Duration checkDuration) {
      this.checkDuration = Objects.requireNonNull(checkDuration);
      return this;
    }

    public Builder checkTimestamp(final Instant checkTimestamp) {
      this.checkTimestamp = Objects.requireNonNull(checkTimestamp);
      return this;
    }

    public PoolHealthCheckResult build() {
      return new PoolHealthCheckResult(overallHealthy, totalInstances, healthyInstances,
                                       degradedInstances, unhealthyInstances, instanceStatuses,
                                       poolIssues, poolWarnings, checkDuration, checkTimestamp);
    }
  }

  @Override
  public String toString() {
    return String.format("PoolHealthCheckResult{healthy=%s, total=%d, healthy=%d, degraded=%d, unhealthy=%d}",
        overallHealthy, totalInstances, healthyInstances, degradedInstances, unhealthyInstances);
  }
}