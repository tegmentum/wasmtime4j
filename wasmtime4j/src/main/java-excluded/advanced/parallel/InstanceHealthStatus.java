package ai.tegmentum.wasmtime4j.parallel;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Health status for a pooled WebAssembly instance.
 *
 * <p>Provides detailed information about the health and availability of an instance
 * in a pool, including any issues or warnings.
 *
 * @since 1.0.0
 */
public final class InstanceHealthStatus {

  private final String instanceId;
  private final HealthState healthState;
  private final Instant lastChecked;
  private final List<String> issues;
  private final List<String> warnings;
  private final Optional<String> lastError;
  private final long consecutiveFailures;
  private final boolean isAvailable;

  private InstanceHealthStatus(final String instanceId,
                               final HealthState healthState,
                               final Instant lastChecked,
                               final List<String> issues,
                               final List<String> warnings,
                               final String lastError,
                               final long consecutiveFailures,
                               final boolean isAvailable) {
    this.instanceId = Objects.requireNonNull(instanceId);
    this.healthState = Objects.requireNonNull(healthState);
    this.lastChecked = Objects.requireNonNull(lastChecked);
    this.issues = List.copyOf(issues);
    this.warnings = List.copyOf(warnings);
    this.lastError = Optional.ofNullable(lastError);
    this.consecutiveFailures = consecutiveFailures;
    this.isAvailable = isAvailable;
  }

  /**
   * Creates a healthy instance status.
   *
   * @param instanceId the instance identifier
   * @return healthy status
   */
  public static InstanceHealthStatus healthy(final String instanceId) {
    return new InstanceHealthStatus(instanceId, HealthState.HEALTHY, Instant.now(),
                                    List.of(), List.of(), null, 0, true);
  }

  /**
   * Creates an unhealthy instance status.
   *
   * @param instanceId the instance identifier
   * @param issues list of issues
   * @param lastError the last error message
   * @param consecutiveFailures number of consecutive failures
   * @return unhealthy status
   */
  public static InstanceHealthStatus unhealthy(final String instanceId,
                                               final List<String> issues,
                                               final String lastError,
                                               final long consecutiveFailures) {
    return new InstanceHealthStatus(instanceId, HealthState.UNHEALTHY, Instant.now(),
                                    issues, List.of(), lastError, consecutiveFailures, false);
  }

  /**
   * Creates a degraded instance status.
   *
   * @param instanceId the instance identifier
   * @param warnings list of warnings
   * @return degraded status
   */
  public static InstanceHealthStatus degraded(final String instanceId,
                                              final List<String> warnings) {
    return new InstanceHealthStatus(instanceId, HealthState.DEGRADED, Instant.now(),
                                    List.of(), warnings, null, 0, true);
  }

  /**
   * Gets the instance identifier.
   *
   * @return instance ID
   */
  public String getInstanceId() {
    return instanceId;
  }

  /**
   * Gets the health state.
   *
   * @return health state
   */
  public HealthState getHealthState() {
    return healthState;
  }

  /**
   * Gets when this status was last checked.
   *
   * @return last check timestamp
   */
  public Instant getLastChecked() {
    return lastChecked;
  }

  /**
   * Gets the list of issues.
   *
   * @return list of issues
   */
  public List<String> getIssues() {
    return issues;
  }

  /**
   * Gets the list of warnings.
   *
   * @return list of warnings
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets the last error message.
   *
   * @return last error, or empty if no error
   */
  public Optional<String> getLastError() {
    return lastError;
  }

  /**
   * Gets the number of consecutive failures.
   *
   * @return consecutive failures count
   */
  public long getConsecutiveFailures() {
    return consecutiveFailures;
  }

  /**
   * Checks if the instance is available for use.
   *
   * @return true if available
   */
  public boolean isAvailable() {
    return isAvailable;
  }

  /**
   * Checks if the instance is healthy.
   *
   * @return true if healthy
   */
  public boolean isHealthy() {
    return healthState == HealthState.HEALTHY;
  }

  /**
   * Checks if the instance has any issues.
   *
   * @return true if issues exist
   */
  public boolean hasIssues() {
    return !issues.isEmpty();
  }

  /**
   * Checks if the instance has any warnings.
   *
   * @return true if warnings exist
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Health states for instances.
   */
  public enum HealthState {
    /** Instance is fully functional */
    HEALTHY,

    /** Instance has minor issues but is still usable */
    DEGRADED,

    /** Instance is not functional */
    UNHEALTHY,

    /** Health status is unknown or being determined */
    UNKNOWN
  }

  @Override
  public String toString() {
    return String.format("InstanceHealthStatus{id='%s', state=%s, available=%s, issues=%d, warnings=%d}",
        instanceId, healthState, isAvailable, issues.size(), warnings.size());
  }
}