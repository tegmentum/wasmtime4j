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
  private final long memoryFootprint;
  private final long functionsPrecompiled;
  private final long totalFunctions;
  private final long instancesCreated;
  private final Duration averageInstantiationTime;
  private final long memoryPoolSize;
  private final boolean poolingEnabled;

  private PreInstantiationStatistics(final Builder builder) {
    this.creationTime = builder.creationTime;
    this.preparationTime = builder.preparationTime;
    this.memoryFootprint = builder.memoryFootprint;
    this.functionsPrecompiled = builder.functionsPrecompiled;
    this.totalFunctions = builder.totalFunctions;
    this.instancesCreated = builder.instancesCreated;
    this.averageInstantiationTime = builder.averageInstantiationTime;
    this.memoryPoolSize = builder.memoryPoolSize;
    this.poolingEnabled = builder.poolingEnabled;
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
   * Gets the memory footprint of the pre-instantiated module.
   *
   * @return memory footprint in bytes
   */
  public long getMemoryFootprint() {
    return memoryFootprint;
  }

  /**
   * Gets the number of functions that were precompiled.
   *
   * @return number of precompiled functions
   */
  public long getFunctionsPrecompiled() {
    return functionsPrecompiled;
  }

  /**
   * Gets the total number of functions in the module.
   *
   * @return total number of functions
   */
  public long getTotalFunctions() {
    return totalFunctions;
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
   * Gets the size of the memory pool used for fast instantiation.
   *
   * @return memory pool size in bytes
   */
  public long getMemoryPoolSize() {
    return memoryPoolSize;
  }

  /**
   * Checks if pooling was enabled for this pre-instantiation.
   *
   * @return true if pooling was enabled
   */
  public boolean isPoolingEnabled() {
    return poolingEnabled;
  }

  /**
   * Calculates the function precompilation ratio.
   *
   * @return precompilation ratio (0.0 to 1.0)
   */
  public double getFunctionPrecompilationRatio() {
    return totalFunctions > 0 ? (double) functionsPrecompiled / totalFunctions : 0.0;
  }

  /**
   * Calculates the preparation efficiency (instances created per second of preparation time).
   *
   * @return preparation efficiency
   */
  public double getPreparationEfficiency() {
    if (preparationTime.isZero() || instancesCreated == 0) {
      return 0.0;
    }
    return (double) instancesCreated / preparationTime.toMillis() * 1000.0;
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
    return memoryFootprint == that.memoryFootprint
        && functionsPrecompiled == that.functionsPrecompiled
        && totalFunctions == that.totalFunctions
        && instancesCreated == that.instancesCreated
        && memoryPoolSize == that.memoryPoolSize
        && poolingEnabled == that.poolingEnabled
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(preparationTime, that.preparationTime)
        && Objects.equals(averageInstantiationTime, that.averageInstantiationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        creationTime,
        preparationTime,
        memoryFootprint,
        functionsPrecompiled,
        totalFunctions,
        instancesCreated,
        averageInstantiationTime,
        memoryPoolSize,
        poolingEnabled);
  }

  @Override
  public String toString() {
    return "PreInstantiationStatistics{"
        + "creationTime="
        + creationTime
        + ", preparationTime="
        + preparationTime
        + ", memoryFootprint="
        + memoryFootprint
        + ", functionsPrecompiled="
        + functionsPrecompiled
        + ", totalFunctions="
        + totalFunctions
        + ", instancesCreated="
        + instancesCreated
        + ", averageInstantiationTime="
        + averageInstantiationTime
        + ", memoryPoolSize="
        + memoryPoolSize
        + ", poolingEnabled="
        + poolingEnabled
        + '}';
  }

  /** Builder for PreInstantiationStatistics. */
  public static final class Builder {
    private Instant creationTime = Instant.now();
    private Duration preparationTime = Duration.ZERO;
    private long memoryFootprint = 0;
    private long functionsPrecompiled = 0;
    private long totalFunctions = 0;
    private long instancesCreated = 0;
    private Duration averageInstantiationTime = Duration.ZERO;
    private long memoryPoolSize = 0;
    private boolean poolingEnabled = false;

    private Builder() {}

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

    public Builder memoryFootprint(final long memoryFootprint) {
      this.memoryFootprint = memoryFootprint;
      return this;
    }

    public Builder functionsPrecompiled(final long functionsPrecompiled) {
      this.functionsPrecompiled = functionsPrecompiled;
      return this;
    }

    public Builder totalFunctions(final long totalFunctions) {
      this.totalFunctions = totalFunctions;
      return this;
    }

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

    public Builder memoryPoolSize(final long memoryPoolSize) {
      this.memoryPoolSize = memoryPoolSize;
      return this;
    }

    public Builder poolingEnabled(final boolean poolingEnabled) {
      this.poolingEnabled = poolingEnabled;
      return this;
    }

    public PreInstantiationStatistics build() {
      return new PreInstantiationStatistics(this);
    }
  }
}
