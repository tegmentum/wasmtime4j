package ai.tegmentum.wasmtime4j.parallel;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Result of a cleanup operation performed on an instance pool.
 *
 * <p>Contains information about instances that were cleaned up, removed, or
 * encountered issues during the cleanup process.
 *
 * @since 1.0.0
 */
public final class PoolCleanupResult {

  private final boolean successful;
  private final int instancesExamined;
  private final int instancesRemoved;
  private final int instancesRepaired;
  private final List<String> removedInstanceIds;
  private final List<String> repairedInstanceIds;
  private final List<String> cleanupErrors;
  private final List<String> cleanupWarnings;
  private final Duration cleanupDuration;
  private final Instant cleanupTimestamp;
  private final long memoryFreedBytes;

  private PoolCleanupResult(final boolean successful,
                            final int instancesExamined,
                            final int instancesRemoved,
                            final int instancesRepaired,
                            final List<String> removedInstanceIds,
                            final List<String> repairedInstanceIds,
                            final List<String> cleanupErrors,
                            final List<String> cleanupWarnings,
                            final Duration cleanupDuration,
                            final Instant cleanupTimestamp,
                            final long memoryFreedBytes) {
    this.successful = successful;
    this.instancesExamined = instancesExamined;
    this.instancesRemoved = instancesRemoved;
    this.instancesRepaired = instancesRepaired;
    this.removedInstanceIds = List.copyOf(removedInstanceIds);
    this.repairedInstanceIds = List.copyOf(repairedInstanceIds);
    this.cleanupErrors = List.copyOf(cleanupErrors);
    this.cleanupWarnings = List.copyOf(cleanupWarnings);
    this.cleanupDuration = Objects.requireNonNull(cleanupDuration);
    this.cleanupTimestamp = Objects.requireNonNull(cleanupTimestamp);
    this.memoryFreedBytes = memoryFreedBytes;
  }

  /**
   * Creates a new builder for pool cleanup results.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a successful cleanup result with no actions taken.
   *
   * @param instancesExamined number of instances examined
   * @param cleanupDuration time taken for cleanup
   * @return successful result
   */
  public static PoolCleanupResult noActionNeeded(final int instancesExamined,
                                                 final Duration cleanupDuration) {
    return builder()
        .successful(true)
        .instancesExamined(instancesExamined)
        .cleanupDuration(cleanupDuration)
        .build();
  }

  /**
   * Checks if the cleanup was successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the number of instances examined during cleanup.
   *
   * @return instances examined
   */
  public int getInstancesExamined() {
    return instancesExamined;
  }

  /**
   * Gets the number of instances removed during cleanup.
   *
   * @return instances removed
   */
  public int getInstancesRemoved() {
    return instancesRemoved;
  }

  /**
   * Gets the number of instances repaired during cleanup.
   *
   * @return instances repaired
   */
  public int getInstancesRepaired() {
    return instancesRepaired;
  }

  /**
   * Gets the IDs of instances that were removed.
   *
   * @return list of removed instance IDs
   */
  public List<String> getRemovedInstanceIds() {
    return removedInstanceIds;
  }

  /**
   * Gets the IDs of instances that were repaired.
   *
   * @return list of repaired instance IDs
   */
  public List<String> getRepairedInstanceIds() {
    return repairedInstanceIds;
  }

  /**
   * Gets any errors that occurred during cleanup.
   *
   * @return list of cleanup errors
   */
  public List<String> getCleanupErrors() {
    return cleanupErrors;
  }

  /**
   * Gets any warnings that occurred during cleanup.
   *
   * @return list of cleanup warnings
   */
  public List<String> getCleanupWarnings() {
    return cleanupWarnings;
  }

  /**
   * Gets the duration of the cleanup operation.
   *
   * @return cleanup duration
   */
  public Duration getCleanupDuration() {
    return cleanupDuration;
  }

  /**
   * Gets when the cleanup was performed.
   *
   * @return cleanup timestamp
   */
  public Instant getCleanupTimestamp() {
    return cleanupTimestamp;
  }

  /**
   * Gets the amount of memory freed during cleanup.
   *
   * @return memory freed in bytes
   */
  public long getMemoryFreedBytes() {
    return memoryFreedBytes;
  }

  /**
   * Checks if any instances were affected by cleanup.
   *
   * @return true if instances were removed or repaired
   */
  public boolean hasChanges() {
    return instancesRemoved > 0 || instancesRepaired > 0;
  }

  /**
   * Checks if any errors occurred during cleanup.
   *
   * @return true if errors exist
   */
  public boolean hasErrors() {
    return !cleanupErrors.isEmpty();
  }

  /**
   * Checks if any warnings occurred during cleanup.
   *
   * @return true if warnings exist
   */
  public boolean hasWarnings() {
    return !cleanupWarnings.isEmpty();
  }

  /**
   * Builder for pool cleanup results.
   */
  public static final class Builder {
    private boolean successful = true;
    private int instancesExamined = 0;
    private int instancesRemoved = 0;
    private int instancesRepaired = 0;
    private List<String> removedInstanceIds = List.of();
    private List<String> repairedInstanceIds = List.of();
    private List<String> cleanupErrors = List.of();
    private List<String> cleanupWarnings = List.of();
    private Duration cleanupDuration = Duration.ZERO;
    private Instant cleanupTimestamp = Instant.now();
    private long memoryFreedBytes = 0;

    public Builder successful(final boolean successful) {
      this.successful = successful;
      return this;
    }

    public Builder instancesExamined(final int instancesExamined) {
      this.instancesExamined = instancesExamined;
      return this;
    }

    public Builder instancesRemoved(final int instancesRemoved) {
      this.instancesRemoved = instancesRemoved;
      return this;
    }

    public Builder instancesRepaired(final int instancesRepaired) {
      this.instancesRepaired = instancesRepaired;
      return this;
    }

    public Builder removedInstanceIds(final List<String> removedInstanceIds) {
      this.removedInstanceIds = Objects.requireNonNull(removedInstanceIds);
      return this;
    }

    public Builder repairedInstanceIds(final List<String> repairedInstanceIds) {
      this.repairedInstanceIds = Objects.requireNonNull(repairedInstanceIds);
      return this;
    }

    public Builder cleanupErrors(final List<String> cleanupErrors) {
      this.cleanupErrors = Objects.requireNonNull(cleanupErrors);
      return this;
    }

    public Builder cleanupWarnings(final List<String> cleanupWarnings) {
      this.cleanupWarnings = Objects.requireNonNull(cleanupWarnings);
      return this;
    }

    public Builder cleanupDuration(final Duration cleanupDuration) {
      this.cleanupDuration = Objects.requireNonNull(cleanupDuration);
      return this;
    }

    public Builder cleanupTimestamp(final Instant cleanupTimestamp) {
      this.cleanupTimestamp = Objects.requireNonNull(cleanupTimestamp);
      return this;
    }

    public Builder memoryFreedBytes(final long memoryFreedBytes) {
      this.memoryFreedBytes = memoryFreedBytes;
      return this;
    }

    public PoolCleanupResult build() {
      return new PoolCleanupResult(successful, instancesExamined, instancesRemoved, instancesRepaired,
                                   removedInstanceIds, repairedInstanceIds, cleanupErrors, cleanupWarnings,
                                   cleanupDuration, cleanupTimestamp, memoryFreedBytes);
    }
  }

  @Override
  public String toString() {
    return String.format("PoolCleanupResult{successful=%s, examined=%d, removed=%d, repaired=%d, errors=%d}",
        successful, instancesExamined, instancesRemoved, instancesRepaired, cleanupErrors.size());
  }
}