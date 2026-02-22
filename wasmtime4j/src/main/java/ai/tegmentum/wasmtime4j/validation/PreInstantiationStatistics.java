package ai.tegmentum.wasmtime4j.validation;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Statistics for pre-instantiation operations.
 *
 * <p>PreInstantiationStatistics provides information about the performance and resource usage of
 * pre-instantiation operations that prepare modules for fast instantiation.
 *
 * @since 1.0.0
 */
public final class PreInstantiationStatistics {

  private final Instant creationTime;
  private final Duration preparationTime;
  private final long instancesCreated;
  private final Duration averageInstantiationTime;

  private PreInstantiationStatistics(final Builder builder) {
    this.creationTime = builder.creationTime;
    this.preparationTime = builder.preparationTime;
    this.instancesCreated = builder.instancesCreated;
    this.averageInstantiationTime = builder.averageInstantiationTime;
  }

  /**
   * Gets the time when the InstancePre was created.
   *
   * @return creation time
   */
  public Instant getCreationTime() {
    return creationTime;
  }

  /**
   * Gets the time taken to prepare the module for pre-instantiation.
   *
   * @return preparation time
   */
  public Duration getPreparationTime() {
    return preparationTime;
  }

  /**
   * Gets the number of instances created from this InstancePre.
   *
   * @return number of instances created
   */
  public long getInstancesCreated() {
    return instancesCreated;
  }

  /**
   * Gets the average time to create instances from this InstancePre.
   *
   * @return average instantiation time
   */
  public Duration getAverageInstantiationTime() {
    return averageInstantiationTime;
  }

  /**
   * Creates a new builder for PreInstantiationStatistics.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final PreInstantiationStatistics that = (PreInstantiationStatistics) obj;
    return instancesCreated == that.instancesCreated
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(preparationTime, that.preparationTime)
        && Objects.equals(averageInstantiationTime, that.averageInstantiationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(creationTime, preparationTime, instancesCreated, averageInstantiationTime);
  }

  @Override
  public String toString() {
    return "PreInstantiationStatistics{"
        + "creationTime="
        + creationTime
        + ", preparationTime="
        + preparationTime
        + ", instancesCreated="
        + instancesCreated
        + ", averageInstantiationTime="
        + averageInstantiationTime
        + '}';
  }

  /** Builder for PreInstantiationStatistics. */
  public static final class Builder {
    private Instant creationTime = Instant.now();
    private Duration preparationTime = Duration.ZERO;
    private long instancesCreated = 0;
    private Duration averageInstantiationTime = Duration.ZERO;

    private Builder() {}

    /**
     * Sets the creation time.
     *
     * @param creationTime the creation time
     * @return this builder
     */
    public Builder creationTime(final Instant creationTime) {
      this.creationTime = Objects.requireNonNull(creationTime, "Creation time cannot be null");
      return this;
    }

    /**
     * Sets the preparation time for pre-instantiation.
     *
     * @param preparationTime the time spent preparing for instantiation
     * @return this builder
     */
    public Builder preparationTime(final Duration preparationTime) {
      this.preparationTime =
          Objects.requireNonNull(preparationTime, "Preparation time cannot be null");
      return this;
    }

    /**
     * Sets the number of instances created.
     *
     * @param instancesCreated the number of instances created
     * @return this builder
     */
    public Builder instancesCreated(final long instancesCreated) {
      this.instancesCreated = instancesCreated;
      return this;
    }

    /**
     * Sets the average time taken for instantiation.
     *
     * @param averageInstantiationTime the average instantiation time
     * @return this builder
     */
    public Builder averageInstantiationTime(final Duration averageInstantiationTime) {
      this.averageInstantiationTime =
          Objects.requireNonNull(
              averageInstantiationTime, "Average instantiation time cannot be null");
      return this;
    }

    /**
     * Builds the PreInstantiationStatistics instance.
     *
     * @return a new PreInstantiationStatistics instance
     */
    public PreInstantiationStatistics build() {
      return new PreInstantiationStatistics(this);
    }
  }
}
