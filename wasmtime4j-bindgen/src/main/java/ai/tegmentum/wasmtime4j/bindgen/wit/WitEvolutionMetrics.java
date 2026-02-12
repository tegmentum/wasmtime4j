package ai.tegmentum.wasmtime4j.bindgen.wit;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Metrics collected during WIT interface evolution operations.
 *
 * <p>This class provides comprehensive metrics about interface evolution operations, including
 * performance data, success rates, and detailed analysis results.
 *
 * @since 1.0.0
 */
public final class WitEvolutionMetrics {

  private final Duration evolutionDuration;
  private final int typesAnalyzed;
  private final int functionsAnalyzed;
  private final int adaptersCreated;
  private final int validationChecks;
  private final double compatibilityScore;
  private final long memoryUsed;
  private final Instant startTime;
  private final Instant endTime;
  private final boolean successful;
  private final Map<String, Object> detailedMetrics;

  /**
   * Creates evolution metrics.
   *
   * @param evolutionDuration duration of evolution operation
   * @param typesAnalyzed number of types analyzed
   * @param functionsAnalyzed number of functions analyzed
   * @param adaptersCreated number of adapters created
   * @param validationChecks number of validation checks performed
   * @param compatibilityScore compatibility score (0.0 to 1.0)
   * @param memoryUsed memory used in bytes
   * @param startTime operation start time
   * @param endTime operation end time
   * @param successful whether operation was successful
   * @param detailedMetrics additional detailed metrics
   */
  public WitEvolutionMetrics(
      final Duration evolutionDuration,
      final int typesAnalyzed,
      final int functionsAnalyzed,
      final int adaptersCreated,
      final int validationChecks,
      final double compatibilityScore,
      final long memoryUsed,
      final Instant startTime,
      final Instant endTime,
      final boolean successful,
      final Map<String, Object> detailedMetrics) {
    this.evolutionDuration =
        Objects.requireNonNull(evolutionDuration, "evolutionDuration must not be null");
    this.typesAnalyzed = Math.max(0, typesAnalyzed);
    this.functionsAnalyzed = Math.max(0, functionsAnalyzed);
    this.adaptersCreated = Math.max(0, adaptersCreated);
    this.validationChecks = Math.max(0, validationChecks);
    this.compatibilityScore = Math.max(0.0, Math.min(1.0, compatibilityScore));
    this.memoryUsed = Math.max(0, memoryUsed);
    this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
    this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
    this.successful = successful;
    this.detailedMetrics =
        Map.copyOf(Objects.requireNonNull(detailedMetrics, "detailedMetrics must not be null"));
  }

  /**
   * Creates an empty metrics instance.
   *
   * @return empty metrics
   */
  public static WitEvolutionMetrics empty() {
    final Instant now = Instant.now();
    return new WitEvolutionMetrics(Duration.ZERO, 0, 0, 0, 0, 0.0, 0, now, now, false, Map.of());
  }

  /**
   * Creates metrics for a successful operation.
   *
   * @param evolutionDuration operation duration
   * @param typesAnalyzed types analyzed
   * @param functionsAnalyzed functions analyzed
   * @param adaptersCreated adapters created
   * @param compatibilityScore compatibility score
   * @return successful metrics
   */
  public static WitEvolutionMetrics success(
      final Duration evolutionDuration,
      final int typesAnalyzed,
      final int functionsAnalyzed,
      final int adaptersCreated,
      final double compatibilityScore) {
    final Instant end = Instant.now();
    final Instant start = end.minus(evolutionDuration);
    return new WitEvolutionMetrics(
        evolutionDuration,
        typesAnalyzed,
        functionsAnalyzed,
        adaptersCreated,
        typesAnalyzed + functionsAnalyzed,
        compatibilityScore,
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
        start,
        end,
        true,
        Map.of());
  }

  /**
   * Gets the evolution operation duration.
   *
   * @return evolution duration
   */
  public Duration getEvolutionDuration() {
    return evolutionDuration;
  }

  /**
   * Gets the number of types analyzed.
   *
   * @return types analyzed count
   */
  public int getTypesAnalyzed() {
    return typesAnalyzed;
  }

  /**
   * Gets the number of functions analyzed.
   *
   * @return functions analyzed count
   */
  public int getFunctionsAnalyzed() {
    return functionsAnalyzed;
  }

  /**
   * Gets the number of adapters created.
   *
   * @return adapters created count
   */
  public int getAdaptersCreated() {
    return adaptersCreated;
  }

  /**
   * Gets the number of validation checks performed.
   *
   * @return validation checks count
   */
  public int getValidationChecks() {
    return validationChecks;
  }

  /**
   * Gets the compatibility score (0.0 to 1.0).
   *
   * @return compatibility score
   */
  public double getCompatibilityScore() {
    return compatibilityScore;
  }

  /**
   * Gets the memory used during evolution in bytes.
   *
   * @return memory used
   */
  public long getMemoryUsed() {
    return memoryUsed;
  }

  /**
   * Gets the operation start time.
   *
   * @return start time
   */
  public Instant getStartTime() {
    return startTime;
  }

  /**
   * Gets the operation end time.
   *
   * @return end time
   */
  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Checks if the operation was successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets detailed metrics.
   *
   * @return detailed metrics map
   */
  public Map<String, Object> getDetailedMetrics() {
    return detailedMetrics;
  }

  /**
   * Gets the evolution throughput (items per second).
   *
   * @return evolution throughput
   */
  public double getEvolutionThroughput() {
    final long totalItems = typesAnalyzed + functionsAnalyzed;
    final double seconds = evolutionDuration.toNanos() / 1_000_000_000.0;
    return seconds > 0 ? totalItems / seconds : 0.0;
  }

  /**
   * Gets the adapter creation rate (adapters per second).
   *
   * @return adapter creation rate
   */
  public double getAdapterCreationRate() {
    final double seconds = evolutionDuration.toNanos() / 1_000_000_000.0;
    return seconds > 0 ? adaptersCreated / seconds : 0.0;
  }

  /**
   * Gets the validation efficiency (checks per second).
   *
   * @return validation efficiency
   */
  public double getValidationEfficiency() {
    final double seconds = evolutionDuration.toNanos() / 1_000_000_000.0;
    return seconds > 0 ? validationChecks / seconds : 0.0;
  }

  /**
   * Gets memory usage per item analyzed.
   *
   * @return memory usage per item in bytes
   */
  public long getMemoryPerItem() {
    final long totalItems = typesAnalyzed + functionsAnalyzed;
    return totalItems > 0 ? memoryUsed / totalItems : 0;
  }

  /**
   * Gets a specific detailed metric.
   *
   * @param key metric key
   * @param type expected type
   * @param <T> metric type
   * @return metric value if present and of correct type
   */
  @SuppressWarnings("unchecked")
  public <T> java.util.Optional<T> getDetailedMetric(final String key, final Class<T> type) {
    final Object value = detailedMetrics.get(key);
    if (value != null && type.isInstance(value)) {
      return java.util.Optional.of((T) value);
    }
    return java.util.Optional.empty();
  }

  /**
   * Creates a new metrics builder.
   *
   * @return metrics builder
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
    final WitEvolutionMetrics that = (WitEvolutionMetrics) obj;
    return typesAnalyzed == that.typesAnalyzed
        && functionsAnalyzed == that.functionsAnalyzed
        && adaptersCreated == that.adaptersCreated
        && validationChecks == that.validationChecks
        && Double.compare(that.compatibilityScore, compatibilityScore) == 0
        && memoryUsed == that.memoryUsed
        && successful == that.successful
        && Objects.equals(evolutionDuration, that.evolutionDuration)
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime)
        && Objects.equals(detailedMetrics, that.detailedMetrics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        evolutionDuration,
        typesAnalyzed,
        functionsAnalyzed,
        adaptersCreated,
        validationChecks,
        compatibilityScore,
        memoryUsed,
        startTime,
        endTime,
        successful,
        detailedMetrics);
  }

  @Override
  public String toString() {
    return "WitEvolutionMetrics{"
        + "evolutionDuration="
        + evolutionDuration
        + ", typesAnalyzed="
        + typesAnalyzed
        + ", functionsAnalyzed="
        + functionsAnalyzed
        + ", adaptersCreated="
        + adaptersCreated
        + ", validationChecks="
        + validationChecks
        + ", compatibilityScore="
        + compatibilityScore
        + ", successful="
        + successful
        + '}';
  }

  /** Builder for WitEvolutionMetrics. */
  public static final class Builder {
    private Duration evolutionDuration = Duration.ZERO;
    private int typesAnalyzed = 0;
    private int functionsAnalyzed = 0;
    private int adaptersCreated = 0;
    private int validationChecks = 0;
    private double compatibilityScore = 0.0;
    private long memoryUsed = 0;
    private Instant startTime = Instant.now();
    private Instant endTime = Instant.now();
    private boolean successful = false;
    private final Map<String, Object> detailedMetrics = new java.util.HashMap<>();

    private Builder() {
      // Private constructor
    }

    /**
     * Sets the evolution duration.
     *
     * @param duration evolution duration
     * @return builder instance
     */
    public Builder evolutionDuration(final Duration duration) {
      this.evolutionDuration = Objects.requireNonNull(duration, "duration must not be null");
      return this;
    }

    /**
     * Sets the number of types analyzed.
     *
     * @param count types analyzed count
     * @return builder instance
     */
    public Builder typesAnalyzed(final int count) {
      this.typesAnalyzed = Math.max(0, count);
      return this;
    }

    /**
     * Sets the number of functions analyzed.
     *
     * @param count functions analyzed count
     * @return builder instance
     */
    public Builder functionsAnalyzed(final int count) {
      this.functionsAnalyzed = Math.max(0, count);
      return this;
    }

    /**
     * Sets the number of adapters created.
     *
     * @param count adapters created count
     * @return builder instance
     */
    public Builder adaptersCreated(final int count) {
      this.adaptersCreated = Math.max(0, count);
      return this;
    }

    /**
     * Sets the number of validation checks.
     *
     * @param count validation checks count
     * @return builder instance
     */
    public Builder validationChecks(final int count) {
      this.validationChecks = Math.max(0, count);
      return this;
    }

    /**
     * Sets the compatibility score.
     *
     * @param score compatibility score (0.0 to 1.0)
     * @return builder instance
     */
    public Builder compatibilityScore(final double score) {
      this.compatibilityScore = Math.max(0.0, Math.min(1.0, score));
      return this;
    }

    /**
     * Sets the memory used.
     *
     * @param bytes memory used in bytes
     * @return builder instance
     */
    public Builder memoryUsed(final long bytes) {
      this.memoryUsed = Math.max(0, bytes);
      return this;
    }

    /**
     * Sets the start time.
     *
     * @param time start time
     * @return builder instance
     */
    public Builder startTime(final Instant time) {
      this.startTime = Objects.requireNonNull(time, "time must not be null");
      return this;
    }

    /**
     * Sets the end time.
     *
     * @param time end time
     * @return builder instance
     */
    public Builder endTime(final Instant time) {
      this.endTime = Objects.requireNonNull(time, "time must not be null");
      return this;
    }

    /**
     * Sets the success status.
     *
     * @param success whether operation was successful
     * @return builder instance
     */
    public Builder successful(final boolean success) {
      this.successful = success;
      return this;
    }

    /**
     * Adds a detailed metric.
     *
     * @param key metric key
     * @param value metric value
     * @return builder instance
     */
    public Builder detailedMetric(final String key, final Object value) {
      this.detailedMetrics.put(
          Objects.requireNonNull(key, "key must not be null"),
          Objects.requireNonNull(value, "value must not be null"));
      return this;
    }

    /**
     * Builds the metrics instance.
     *
     * @return evolution metrics
     */
    public WitEvolutionMetrics build() {
      return new WitEvolutionMetrics(
          evolutionDuration,
          typesAnalyzed,
          functionsAnalyzed,
          adaptersCreated,
          validationChecks,
          compatibilityScore,
          memoryUsed,
          startTime,
          endTime,
          successful,
          detailedMetrics);
    }
  }
}
