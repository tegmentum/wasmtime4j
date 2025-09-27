package ai.tegmentum.wasmtime4j.parallel;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Result of a pool scaling operation.
 *
 * <p>Contains information about the outcome of scaling an instance pool,
 * including the number of instances added or removed and any issues encountered.
 *
 * @since 1.0.0
 */
public final class ScalingResult {

  private final boolean successful;
  private final ScalingAction action;
  private final int instancesAffected;
  private final int targetSize;
  private final int actualSize;
  private final Duration scalingTime;
  private final Instant timestamp;
  private final List<String> warnings;
  private final List<String> errors;

  private ScalingResult(final boolean successful,
                        final ScalingAction action,
                        final int instancesAffected,
                        final int targetSize,
                        final int actualSize,
                        final Duration scalingTime,
                        final Instant timestamp,
                        final List<String> warnings,
                        final List<String> errors) {
    this.successful = successful;
    this.action = Objects.requireNonNull(action);
    this.instancesAffected = instancesAffected;
    this.targetSize = targetSize;
    this.actualSize = actualSize;
    this.scalingTime = Objects.requireNonNull(scalingTime);
    this.timestamp = Objects.requireNonNull(timestamp);
    this.warnings = List.copyOf(warnings);
    this.errors = List.copyOf(errors);
  }

  /**
   * Creates a successful scaling result.
   *
   * @param action the scaling action performed
   * @param instancesAffected number of instances added or removed
   * @param targetSize the target pool size
   * @param actualSize the actual pool size after scaling
   * @param scalingTime time taken for the scaling operation
   * @return successful scaling result
   */
  public static ScalingResult success(final ScalingAction action,
                                      final int instancesAffected,
                                      final int targetSize,
                                      final int actualSize,
                                      final Duration scalingTime) {
    return new ScalingResult(true, action, instancesAffected, targetSize, actualSize,
                             scalingTime, Instant.now(), List.of(), List.of());
  }

  /**
   * Creates a successful scaling result with warnings.
   *
   * @param action the scaling action performed
   * @param instancesAffected number of instances added or removed
   * @param targetSize the target pool size
   * @param actualSize the actual pool size after scaling
   * @param scalingTime time taken for the scaling operation
   * @param warnings any warnings encountered
   * @return successful scaling result with warnings
   */
  public static ScalingResult success(final ScalingAction action,
                                      final int instancesAffected,
                                      final int targetSize,
                                      final int actualSize,
                                      final Duration scalingTime,
                                      final List<String> warnings) {
    return new ScalingResult(true, action, instancesAffected, targetSize, actualSize,
                             scalingTime, Instant.now(), warnings, List.of());
  }

  /**
   * Creates a failed scaling result.
   *
   * @param action the scaling action attempted
   * @param targetSize the target pool size
   * @param actualSize the actual pool size
   * @param scalingTime time taken before failure
   * @param errors the errors encountered
   * @return failed scaling result
   */
  public static ScalingResult failure(final ScalingAction action,
                                      final int targetSize,
                                      final int actualSize,
                                      final Duration scalingTime,
                                      final List<String> errors) {
    return new ScalingResult(false, action, 0, targetSize, actualSize,
                             scalingTime, Instant.now(), List.of(), errors);
  }

  /**
   * Checks if the scaling operation was successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the scaling action that was performed.
   *
   * @return scaling action
   */
  public ScalingAction getAction() {
    return action;
  }

  /**
   * Gets the number of instances affected by the scaling operation.
   *
   * @return number of instances added or removed
   */
  public int getInstancesAffected() {
    return instancesAffected;
  }

  /**
   * Gets the target pool size.
   *
   * @return target size
   */
  public int getTargetSize() {
    return targetSize;
  }

  /**
   * Gets the actual pool size after scaling.
   *
   * @return actual size
   */
  public int getActualSize() {
    return actualSize;
  }

  /**
   * Gets the time taken for the scaling operation.
   *
   * @return scaling time
   */
  public Duration getScalingTime() {
    return scalingTime;
  }

  /**
   * Gets the timestamp when scaling completed.
   *
   * @return timestamp
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets any warnings from the scaling operation.
   *
   * @return list of warnings
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets any errors from the scaling operation.
   *
   * @return list of errors
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Checks if there were any warnings.
   *
   * @return true if warnings exist
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Checks if there were any errors.
   *
   * @return true if errors exist
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Scaling actions for instance pools.
   */
  public enum ScalingAction {
    /** Added instances to the pool */
    SCALE_UP,

    /** Removed instances from the pool */
    SCALE_DOWN,

    /** No scaling was needed */
    NO_ACTION
  }

  @Override
  public String toString() {
    return String.format("ScalingResult{action=%s, affected=%d, target=%d, actual=%d, successful=%s}",
        action, instancesAffected, targetSize, actualSize, successful);
  }
}